#Requires -Version 5.1
#Requires -RunAsAdministrator
<#
.SYNOPSIS
  Fix Spring config path (trailing slash) and restart GestPOV-Server.
  Run if the service stops with "Unable to load config data from file:.../config"
#>
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$launcher = 'C:\Program Files\GestPOV\server\start-backend.ps1'
if (-not (Test-Path $launcher)) {
    throw "Gest POV server not installed: $launcher missing."
}

$content = Get-Content $launcher -Raw
$fixed = $content -replace "\`$cfg = 'file:' \+ \(\`$P\.ConfigDir -replace '\\\\','/'\)", "`$cfg = 'file:' + (`$P.ConfigDir -replace '\\','/') + '/'"
if ($fixed -eq $content) {
    Write-Host "Script already fixed or unexpected format - check the `$cfg line manually."
} else {
    Set-Content -Path $launcher -Value $fixed -Encoding UTF8
    Write-Host "start-backend.ps1 fixed."
}

Write-Host "Stopping GestPOV-Server..."
Stop-Service GestPOV-Server -Force -ErrorAction SilentlyContinue

Write-Host "Starting GestPOV-Server..."
Start-Service GestPOV-Server
Start-Sleep -Seconds 5

$svc = Get-Service GestPOV-Server
Write-Host "GestPOV-Server status: $($svc.Status)"

if ($svc.Status -ne 'Running') {
    Write-Host "See C:\ProgramData\GestPOV\logs\GestPOV-Server.out.log"
    exit 1
}

try {
    $r = Invoke-WebRequest -Uri 'http://127.0.0.1:8080/api/discovery' -UseBasicParsing -TimeoutSec 30
    Write-Host "Discovery OK ($($r.StatusCode))"
} catch {
    Write-Host "Discovery still failing: $($_.Exception.Message)"
    Write-Host "Stop any other backend (npm run dev:backend) using port 8080."
    exit 1
}

Write-Host "Done. Launch the Desktop client and use 127.0.0.1:8080"
