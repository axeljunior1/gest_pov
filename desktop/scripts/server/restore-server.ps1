#Requires -Version 5.1
#Requires -RunAsAdministrator
<#
.SYNOPSIS
  Restaure un dump cree par backup-server.ps1.
  Arrete le serveur applicatif, restore, redemarre.
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$BackupDir
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'lib\GestPovCommon.ps1')
. (Join-Path $PSScriptRoot 'lib\GestPovSecrets.ps1')
Assert-GestPovAdministrator
$P = Get-GestPovPaths
$log = $P.BackupLog
$dump = Join-Path $BackupDir 'postgres.dump'
if (-not (Test-Path $dump)) { throw "postgres.dump introuvable dans $BackupDir" }

$sec = Unprotect-GestPovSecrets -Path $P.SecretsFile -Scope LocalMachine
Write-GestPovLog -LogFile $log -Message "=== restore-server.ps1 ==="

$app = Get-GestPovService $P.AppServiceName
if ($app -and $app.Status -eq 'Running') {
    Stop-Service $P.AppServiceName
    Write-GestPovLog -LogFile $log -Message "Service application arrete."
}

$pgRestore = Join-Path $P.PostgresDir 'bin\pg_restore.exe'
$env:PGPASSWORD = $sec.dbAppPassword
try {
    & $pgRestore -h 127.0.0.1 -p 5432 -U $sec.dbAppUser -d $sec.dbName --clean --if-exists $dump
    if ($LASTEXITCODE -ne 0) { throw "pg_restore a echoue ($LASTEXITCODE)." }
} finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}

$licZip = Join-Path $BackupDir 'license.zip'
if (Test-Path $licZip) {
    New-Item -ItemType Directory -Path $P.LicenseDir -Force | Out-Null
    Expand-Archive -Path $licZip -DestinationPath $P.LicenseDir -Force
}
$upZip = Join-Path $BackupDir 'uploads.zip'
if (Test-Path $upZip) {
    New-Item -ItemType Directory -Path $P.UploadsDir -Force | Out-Null
    Expand-Archive -Path $upZip -DestinationPath $P.UploadsDir -Force
}

if ($app) { Start-Service $P.AppServiceName }
Write-GestPovLog -LogFile $log -Message "Restore terminee."
Write-Host "Restauration terminee. Lancez health-check.ps1"
