param(
    [string]$BaseUrl = 'http://localhost:8888',
    [string]$MysqlContainer = 'bank-mysql'
)

# Black-box E2E tests for Fase 0 and Fase 1.
# Requires: docker compose up -d (mysql, kafka and all services) plus nginx on :8888.
$ErrorActionPreference = 'Stop'
$failures = @()

function Check([string]$name, [bool]$condition, [string]$detail = '') {
    if ($condition) {
        Write-Host "  PASS  $name"
    } else {
        Write-Host "  FAIL  $name $detail"
        $script:failures += $name
    }
}

function Api([string]$method, [string]$path, $body = $null, [string]$token = $null, $extraHeaders = @{}) {
    $headers = @{ 'Content-Type' = 'application/json' }
    if ($token) { $headers['Authorization'] = "Bearer $token" }
    foreach ($key in $extraHeaders.Keys) { $headers[$key] = $extraHeaders[$key] }
    $payload = if ($null -eq $body) { $null } else { $body | ConvertTo-Json -Depth 8 -Compress }
    $response = Invoke-WebRequest -Uri "$BaseUrl$path" -Method $method -Headers $headers `
        -Body $payload -TimeoutSec 30 -UseBasicParsing
    return @{
        Body    = if ($response.Content) { $response.Content | ConvertFrom-Json } else { $null }
        Headers = $response.Headers
    }
}

function Sql([string]$query) {
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        return (& docker exec -e MYSQL_PWD=rootpass $MysqlContainer mysql -uroot -N -e $query 2>$null |
            Select-Object -First 1)
    } finally {
        $ErrorActionPreference = $previous
    }
}

function Wait-Transfer([string]$transferId, [string]$token) {
    for ($i = 0; $i -lt 30; $i++) {
        Start-Sleep -Milliseconds 700
        $result = Api 'GET' "/api/transfers/$transferId" $null $token
        $status = $result.Body.data.status
        if ($status -in @('COMPLETED', 'REJECTED', 'COMPENSATED', 'FAILED')) { return $result.Body.data }
    }
    return $null
}

Write-Host "== Fase 1.5: correlation id y login admin =="
$admin = Api 'POST' '/api/auth/login' @{ email = 'admin@bank.com'; password = 'admin123' }
Check 'admin@bank.com / admin123 hace login' ($admin.Body.success -eq $true -and $admin.Body.data.accessToken)
Check 'respuesta incluye X-Correlation-Id' ($null -ne $admin.Headers['X-Correlation-Id'])
$adminToken = $admin.Body.data.accessToken

Write-Host "== Setup: usuario con dos cuentas =="
$email = "e2e$(Get-Random)@test.com"
$registration = @{
    firstName = 'E2E'; lastName = 'Owner'; email = $email; password = 'pass123'
    phone = '999999999'; dateOfBirth = '1990-01-01'; address = 'Av 1'
    idDocument = '12345678'; occupation = 'Dev'
}
$registered = Api 'POST' '/api/auth/register' $registration
Check 'registro de usuario' ($registered.Body.success -eq $true)
$userId = $registered.Body.data
$login = Api 'POST' '/api/auth/login' @{ email = $email; password = 'pass123' }
$token = $login.Body.data.accessToken

$card = (Api 'POST' '/api/cards/issue' @{ pin4 = '1234'; pin6 = '123456' } $token).Body.data
Check 'emision de tarjeta' ($null -ne $card.id)
$accountA = (Api 'POST' '/api/accounts/create' @{ currency = 'PEN'; cardId = $card.id } $token).Body.data
$accountB = (Api 'POST' '/api/accounts/create' @{ currency = 'PEN'; cardId = $card.id } $token).Body.data
Check 'creacion de dos cuentas' ($null -ne $accountA.accountNumber -and $null -ne $accountB.accountNumber)

$deposit = Api 'POST' '/api/accounts/deposit' @{
    accountNumber = $accountA.accountNumber; currency = 'PEN'; pin4 = '1234'; amount = 500
} $token
Check 'deposito inicial de 500' ($deposit.Body.success -eq $true)

Write-Host "== Fase 0/1: transferencia internal happy path =="
$key = [guid]::NewGuid().ToString()
$transferBody = @{
    fromAccount = $accountA.accountNumber; toAccount = $accountB.accountNumber
    amount = 200; currency = 'PEN'; description = 'E2E Fase 0/1'; pin4 = '1234'; cardId = $card.id
}
$created = Api 'POST' '/api/transfers/internal' $transferBody $token @{ 'Idempotency-Key' = $key }
Check 'creacion de transferencia' ($created.Body.success -eq $true -and $created.Body.data.status -eq 'PENDING')
$transferId = $created.Body.data.transferId
$final = Wait-Transfer $transferId $token
Check 'saga termina COMPLETED' ($null -ne $final -and $final.status -eq 'COMPLETED') "status=$($final.status)"

$balanceA = (Api 'GET' "/api/accounts/$($accountA.id)" $null $token).Body.data.balance
$balanceB = (Api 'GET' "/api/accounts/$($accountB.id)" $null $token).Body.data.balance
Check 'saldo origen 300 y destino 200' ([decimal]$balanceA -eq 300 -and [decimal]$balanceB -eq 200) "A=$balanceA B=$balanceB"

Write-Host "== Fase 1.4: idempotencia por Idempotency-Key =="
$replay = Api 'POST' '/api/transfers/internal' $transferBody $token @{ 'Idempotency-Key' = $key }
Check 'replay devuelve el mismo transferId' ($replay.Body.data.transferId -eq $transferId)

Write-Host "== Fase 0.2: fondos insuficientes => REJECTED =="
$poorBody = @{
    fromAccount = $accountA.accountNumber; toAccount = $accountB.accountNumber
    amount = 10000; currency = 'PEN'; description = 'E2E insufficient funds'; pin4 = '1234'; cardId = $card.id
}
$poor = Api 'POST' '/api/transfers/internal' $poorBody $token @{ 'Idempotency-Key' = [guid]::NewGuid().ToString() }
$poorFinal = Wait-Transfer $poor.Body.data.transferId $token
Check 'fondos insuficientes termina REJECTED' ($null -ne $poorFinal -and $poorFinal.status -eq 'REJECTED') "status=$($poorFinal.status)"

Write-Host "== Fase 1.2/1.3/1.4: outbox e inbox en MySQL =="
$sentEvents = [int](Sql "SELECT COUNT(*) FROM bank_accounts.outbox_events WHERE status='SENT' AND event_id IS NOT NULL;")
Check 'outbox de accounts con eventos SENT y event_id' ($sentEvents -gt 0) "count=$sentEvents"
# Fase 2: el orquestador usa 'transfers-saga'; se acepta el consumer anterior para dual-run.
$inboxRows = [int](Sql "SELECT COUNT(*) FROM bank_transfers.processed_events WHERE consumer IN ('transfers-saga','transfers-account');")
Check 'inbox de transfers con eventos deduplicados' ($inboxRows -gt 0) "count=$inboxRows"
$movements = [int](Sql "SELECT COUNT(*) FROM bank_accounts.account_movements;")
Check 'movimientos de accounts persistidos' ($movements -ge 4) "count=$movements"
$notifications = [int](Sql "SELECT COUNT(*) FROM bank_notifications.notifications;")
Check 'notificaciones generadas por la saga' ($notifications -gt 0) "count=$notifications"

Write-Host ""
if ($failures.Count -eq 0) {
    Write-Host 'RESULTADO: todas las pruebas E2E pasaron.'
    exit 0
}
Write-Host "RESULTADO: $($failures.Count) fallos: $($failures -join ', ')"
exit 1
