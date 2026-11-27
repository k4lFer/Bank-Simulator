param(
    [string]$BaseUrl = 'http://localhost:8888',
    [string]$MysqlContainer = 'bank-mysql'
)

# Black-box E2E tests for Fase 2 (saga orquestada).
# Requiere el stack levantado con app.saga.orchestrated=true (valor por defecto).
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
        return (& docker exec -e MYSQL_PWD=rootpass $MysqlContainer mysql -uroot -N -B -e $query 2>$null |
            Select-Object -First 1)
    } finally {
        $ErrorActionPreference = $previous
    }
}

function Wait-Transfer([string]$transferId, [string]$token) {
    for ($i = 0; $i -lt 45; $i++) {
        Start-Sleep -Milliseconds 700
        $result = Api 'GET' "/api/transfers/$transferId" $null $token
        $status = $result.Body.data.status
        if ($status -in @('COMPLETED', 'REJECTED', 'COMPENSATED', 'FAILED')) { return $result.Body.data }
    }
    return $null
}

function UuidSql([string]$value) {
    return "UNHEX(REPLACE('$value','-',''))"
}

Write-Host "== Setup: usuario con dos cuentas =="
$email = "e2efase2$(Get-Random)@test.com"
$registration = @{
    firstName = 'E2E'; lastName = 'Saga'; email = $email; password = 'pass123'
    phone = '999999999'; dateOfBirth = '1990-01-01'; address = 'Av 1'
    idDocument = '12345678'; occupation = 'Dev'
}
$registered = Api 'POST' '/api/auth/register' $registration
Check 'registro de usuario' ($registered.Body.success -eq $true)
$login = Api 'POST' '/api/auth/login' @{ email = $email; password = 'pass123' }
$token = $login.Body.data.accessToken

$card = (Api 'POST' '/api/cards/issue' @{ pin4 = '1234'; pin6 = '123456' } $token).Body.data
$accountA = (Api 'POST' '/api/accounts/create' @{ currency = 'PEN'; cardId = $card.id } $token).Body.data
$accountB = (Api 'POST' '/api/accounts/create' @{ currency = 'PEN'; cardId = $card.id } $token).Body.data
$deposit = Api 'POST' '/api/accounts/deposit' @{
    accountNumber = $accountA.accountNumber; currency = 'PEN'; pin4 = '1234'; amount = 500
} $token
Check 'deposito inicial de 500' ($deposit.Body.success -eq $true)

Write-Host "== Fase 2.3/2.4: internal happy path usa comandos =="
$key = [guid]::NewGuid().ToString()
$internalBody = @{
    fromAccount = $accountA.accountNumber; toAccount = $accountB.accountNumber
    amount = 200; currency = 'PEN'; description = 'E2E Fase 2 internal'; pin4 = '1234'; cardId = $card.id
}
$created = Api 'POST' '/api/transfers/internal' $internalBody $token @{ 'Idempotency-Key' = $key }
Check 'creacion de transferencia' ($created.Body.success -eq $true -and $created.Body.data.status -eq 'PENDING')
$transferId = $created.Body.data.transferId
$final = Wait-Transfer $transferId $token
Check 'saga orquestada termina COMPLETED' ($null -ne $final -and $final.status -eq 'COMPLETED') "status=$($final.status)"

$types = Sql "SELECT GROUP_CONCAT(DISTINCT e.event_type ORDER BY e.event_type) FROM bank_transfers.transfers_events e JOIN bank_transfers.transfers t ON t.id=e.transfer_id WHERE t.transfer_id=$(UuidSql $transferId);"
Check 'outbox contiene TransferDebitCommand y TransferCreditCommand' ($types -eq 'TransferCreditCommand,TransferDebitCommand') "types=$types"
$commandInbox = [int](Sql "SELECT COUNT(*) FROM bank_accounts.processed_events WHERE consumer='accounts-commands';")
Check 'accounts registra comandos en su inbox' ($commandInbox -ge 2) "count=$commandInbox"

Write-Host "== Fase 2.5/2.6: destino externo invalido compensa =="
$externalBody = @{
    fromAccount = $accountA.accountNumber; toAccount = '9999999999'
    amount = 100; currency = 'PEN'; description = 'E2E Fase 2 compensate'
}
$externalKey = [guid]::NewGuid().ToString()
$external = Api 'POST' '/api/transfers/external' $externalBody $token @{ 'Idempotency-Key' = $externalKey }
Check 'creacion de transferencia externa' ($external.Body.success -eq $true)
$externalId = $external.Body.data.transferId
$externalFinal = Wait-Transfer $externalId $token
Check 'fallo de credito termina COMPENSATED' ($null -ne $externalFinal -and $externalFinal.status -eq 'COMPENSATED') "status=$($externalFinal.status)"

$externalTypes = Sql "SELECT GROUP_CONCAT(DISTINCT e.event_type ORDER BY e.event_type) FROM bank_transfers.transfers_events e JOIN bank_transfers.transfers t ON t.id=e.transfer_id WHERE t.transfer_id=$(UuidSql $externalId);"
Check 'outbox contiene debito, credito fallido y compensacion' ($externalTypes -eq 'TransferCompensateCommand,TransferCreditCommand,TransferDebitCommand') "types=$externalTypes"

$ledgerTypes = Sql "SELECT GROUP_CONCAT(entry_type ORDER BY entry_type) FROM bank_ledger.ledger_entries WHERE transfer_id=$(UuidSql $externalId) AND account_number='$($accountA.accountNumber)';"
Check 'ledger registra DR y REV (sin CR)' ($ledgerTypes -eq 'DR,REV') "types=$ledgerTypes"
$reversals = [int](Sql "SELECT COUNT(*) FROM bank_accounts.account_movements WHERE transfer_id=$(UuidSql $externalId) AND type='REVERSAL';")
Check 'accounts persiste un movimiento REVERSAL' ($reversals -eq 1) "count=$reversals"

$balanceA = (Api 'GET' "/api/accounts/$($accountA.id)" $null $token).Body.data.balance
Check 'saldo origen devuelto a 300 tras compensacion' ([decimal]$balanceA -eq 300) "A=$balanceA"

$compensatedNotifications = [int](Sql "SELECT COUNT(*) FROM bank_notifications.notifications WHERE type='TRANSFER_COMPENSATED' AND related_account='$($accountA.accountNumber)';")
Check 'notificacion TRANSFER_COMPENSATED generada' ($compensatedNotifications -ge 1) "count=$compensatedNotifications"

Write-Host "== Fase 2.8: replay idempotente no duplica compensacion =="
$replay = Api 'POST' '/api/transfers/external' $externalBody $token @{ 'Idempotency-Key' = $externalKey }
Check 'replay devuelve el mismo transferId' ($replay.Body.data.transferId -eq $externalId)
$reversalsAfter = [int](Sql "SELECT COUNT(*) FROM bank_accounts.account_movements WHERE transfer_id=$(UuidSql $externalId) AND type='REVERSAL';")
Check 'replay no duplica el REVERSAL' ($reversalsAfter -eq 1) "count=$reversalsAfter"

Write-Host ""
if ($failures.Count -eq 0) {
    Write-Host 'RESULTADO: todas las pruebas E2E de Fase 2 pasaron.'
    exit 0
}
Write-Host "RESULTADO: $($failures.Count) fallos: $($failures -join ', ')"
exit 1
