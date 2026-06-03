param(
    [string]$BootstrapSecret = "change-me-in-production"
)

$email = Read-Host "Email"
$password = Read-Host -AsSecureString "Password"
$BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($password)
$plainPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
[System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($BSTR)

$fname = Read-Host "First name [Admin]"
if (-not $fname) { $fname = "Admin" }

$lname = Read-Host "Last name [User]"
if (-not $lname) { $lname = "User" }

Write-Host "Creando admin $email ..."

$body = @{
    firstName = $fname
    lastName  = $lname
    email     = $email
    password  = $plainPassword
    phone     = ""
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/register-admin" `
        -Method Post `
        -ContentType "application/json" `
        -Headers @{ "X-Bootstrap-Secret" = $BootstrapSecret } `
        -Body $body

    $response | ConvertTo-Json -Depth 10
}
catch {
    Write-Host "Error: $_"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $reader.ReadToEnd() | Write-Host
    }
}
