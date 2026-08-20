#Requires -Version 5.1
<#
.SYNOPSIS
  Diagnostic serveur Gest POV. N'affiche aucun secret.
#>
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Continue'

. (Join-Path $PSScriptRoot 'lib\GestPovCommon.ps1')
$P = Get-GestPovPaths
$failed = 0

function Show-Check([string]$Name, [bool]$Ok, [string]$Detail = '') {
    $tag = if ($Ok) { '[OK]' } else { '[FAIL]' }
    if (-not $Ok) { $script:failed++ }
    $line = if ($Detail) { "$tag $Name - $Detail" } else { "$tag $Name" }
    Write-Host $line
}

function Show-Warn([string]$Name, [string]$Detail) {
    Write-Host "[WARN] $Name - $Detail"
}

$pg = Get-GestPovService $P.PgServiceName
Show-Check 'PostgreSQL service' ($null -ne $pg -and $pg.Status -eq 'Running') $(if ($pg) { $pg.Status } else { 'absent' })

$dbOk = $false
$pgIsReady = Join-Path $P.PostgresDir 'bin\pg_isready.exe'
if (Test-Path $pgIsReady) {
    & $pgIsReady -h 127.0.0.1 -p 5432 | Out-Null
    $dbOk = ($LASTEXITCODE -eq 0)
}
Show-Check 'Database (pg_isready localhost)' $dbOk

$app = Get-GestPovService $P.AppServiceName
Show-Check 'Gest POV Server service' ($null -ne $app -and $app.Status -eq 'Running') $(if ($app) { $app.Status } else { 'absent' })

$liveOk = $false
try {
    $r = Invoke-WebRequest -Uri "http://127.0.0.1:$($P.ApiPort)/actuator/health/liveness" -UseBasicParsing -TimeoutSec 5
    $liveOk = ($r.StatusCode -eq 200)
} catch { }
Show-Check 'API liveness' $liveOk

$discOk = $false
$serverId = ''
$version = ''
try {
    $r = Invoke-WebRequest -Uri "http://127.0.0.1:$($P.ApiPort)/api/discovery" -UseBasicParsing -TimeoutSec 5
    $discOk = ($r.StatusCode -eq 200 -and $r.Content -match 'GEST_POV')
    if ($discOk) {
        $j = $r.Content | ConvertFrom-Json
        $serverId = $j.serverId
        $version = $j.version
    }
} catch { }
Show-Check 'Discovery' $discOk
Show-Check 'Server ID' (-not [string]::IsNullOrWhiteSpace($serverId)) $(if ($serverId) { $serverId } else { 'inconnu' })
if ($version) { Show-Check 'Version' $true $version }

$fileId = ''
if (Test-Path $P.ServerIdFile) { $fileId = (Get-Content $P.ServerIdFile -Raw).Trim() }
if ($fileId -and $serverId -and $fileId -ne $serverId) {
    Show-Warn 'Server ID' 'fichier config different de /api/discovery'
}

$licOk = $false
$licDetail = 'indisponible'
try {
    $r = Invoke-WebRequest -Uri "http://127.0.0.1:$($P.ApiPort)/api/license/status" -UseBasicParsing -TimeoutSec 5
    if ($r.StatusCode -eq 200) {
        $j = $r.Content | ConvertFrom-Json
        $licOk = [bool]$j.valid
        $licDetail = if ($j.valid) { 'valide' } else { [string]$j.reason }
    }
} catch { }
if ($licOk) { Show-Check 'Licence' $true $licDetail } else { Show-Warn 'Licence' $licDetail }

$free = Get-GestPovDiskFreeGb
if ($null -eq $free) {
    Show-Warn 'Disque' 'impossible a lire'
} elseif ($free -lt 1) {
    Show-Warn 'Disque' "$free Go libres"
} else {
    Show-Check 'Disque C:' $true "$free Go libres"
}

$backups = @()
if (Test-Path $P.BackupsDir) {
    $backups = Get-ChildItem $P.BackupsDir -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending
}
if ($backups.Count -eq 0) {
    Show-Warn 'Backup' 'aucune sauvegarde recente'
} else {
    Show-Check 'Backup recent' $true $backups[0].Name
}

Write-Host ""
if ($failed -gt 0) {
    Write-Host "Resultat : $failed controle(s) en echec."
    exit 1
}
Write-Host "Resultat : tous les controles critiques sont OK."
exit 0
