#Requires -Version 5.1
#Requires -RunAsAdministrator
<#
.SYNOPSIS
  Sauvegarde locale PostgreSQL + licence + uploads.
#>
[CmdletBinding()]
param([int]$Keep = 14)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'lib\GestPovCommon.ps1')
. (Join-Path $PSScriptRoot 'lib\GestPovSecrets.ps1')
Assert-GestPovAdministrator
$P = Get-GestPovPaths
New-Item -ItemType Directory -Path $P.LogsDir, $P.BackupsDir -Force | Out-Null
$log = $P.BackupLog
Write-GestPovLog -LogFile $log -Message "=== backup-server.ps1 ==="

if (-not (Test-Path $P.SecretsFile)) { throw "Secrets introuvables. Installation incomplete." }
$sec = Unprotect-GestPovSecrets -Path $P.SecretsFile -Scope LocalMachine

$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$dest = Join-Path $P.BackupsDir $stamp
New-Item -ItemType Directory -Path $dest -Force | Out-Null

$pgDump = Join-Path $P.PostgresDir 'bin\pg_dump.exe'
if (-not (Test-Path $pgDump)) { throw "pg_dump.exe introuvable." }

$env:PGPASSWORD = $sec.dbAppPassword
try {
    $dumpFile = Join-Path $dest 'postgres.dump'
    & $pgDump -h 127.0.0.1 -p 5432 -U $sec.dbAppUser -d $sec.dbName -Fc -f $dumpFile
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path $dumpFile) -or ((Get-Item $dumpFile).Length -lt 32)) {
        throw "pg_dump a echoue ou fichier trop petit."
    }
    Write-GestPovLog -LogFile $log -Message "Dump PostgreSQL OK ($stamp)."
} finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}

if (Test-Path $P.LicenseDir) {
    Compress-Archive -Path (Join-Path $P.LicenseDir '*') -DestinationPath (Join-Path $dest 'license.zip') -Force
}
if (Test-Path $P.UploadsDir) {
    Compress-Archive -Path (Join-Path $P.UploadsDir '*') -DestinationPath (Join-Path $dest 'uploads.zip') -Force -ErrorAction SilentlyContinue
}
if (Test-Path $P.ServerIdFile) {
    Copy-Item $P.ServerIdFile (Join-Path $dest 'server.id') -Force
}

$manifest = @{
    timestamp = $stamp
    dbName    = $sec.dbName
    dumpOk    = $true
    dumpBytes = (Get-Item (Join-Path $dest 'postgres.dump')).Length
} | ConvertTo-Json
Set-Content -Path (Join-Path $dest 'manifest.json') -Value $manifest -Encoding UTF8

@"
Restauration : restore-server.ps1 -BackupDir `"$dest`"
Arreter GestPOV-Server avant restore.
"@ | Set-Content (Join-Path $dest 'README.txt') -Encoding UTF8

$all = Get-ChildItem $P.BackupsDir -Directory | Sort-Object Name -Descending
if ($all.Count -gt $Keep) {
    $all | Select-Object -Skip $Keep | ForEach-Object {
        Remove-Item $_.FullName -Recurse -Force
        Write-GestPovLog -LogFile $log -Message "Rotation : suppression $($_.Name)"
    }
}

Write-Host "Sauvegarde : $dest"
Write-GestPovLog -LogFile $log -Message "Backup termine $dest"
