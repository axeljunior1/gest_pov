#Requires -Version 5.1
<#
  Tests unitaires secrets DPAPI (CurrentUser — pas besoin d'admin).
  Usage : powershell -File desktop/scripts/server/tests/Test-GestPovSecrets.ps1
#>
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$lib = Join-Path $PSScriptRoot '..\lib'
. (Join-Path $lib 'GestPovSecrets.ps1')
. (Join-Path $lib 'GestPovCommon.ps1')

$tmp = Join-Path $env:TEMP ("gestpov-secret-test-" + [guid]::NewGuid())
New-Item -ItemType Directory -Path $tmp | Out-Null
$file = Join-Path $tmp 'secrets.dpapi'

try {
    $original = @{
        dbAppPassword = New-GestPovPassword 16
        jwtSecret     = 'not-a-real-jwt-just-test-value-32chars!!'
    }
    Protect-GestPovSecrets -Secrets $original -Path $file -Scope CurrentUser
    if (-not (Test-Path $file)) { throw 'fichier non cree' }
    $raw = [System.IO.File]::ReadAllBytes($file)
    $asText = [System.Text.Encoding]::UTF8.GetString($raw)
    if ($asText.Contains($original.dbAppPassword)) {
        throw 'le mot de passe apparait en clair dans le fichier DPAPI'
    }
    $round = Unprotect-GestPovSecrets -Path $file -Scope CurrentUser
    if ($round.dbAppPassword -ne $original.dbAppPassword) { throw 'round-trip password KO' }
    if ($round.jwtSecret -ne $original.jwtSecret) { throw 'round-trip jwt KO' }
    Write-Host '[OK] DPAPI CurrentUser round-trip'
    Write-Host '[OK] secret absent du blob brut'
} finally {
    Remove-Item $tmp -Recurse -Force -ErrorAction SilentlyContinue
}

$pwd = New-GestPovPassword 24
if ($pwd.Length -ne 24) { throw 'longueur mot de passe' }
Write-Host '[OK] New-GestPovPassword'

$paths = Get-GestPovPaths
if ($paths.DbName -ne 'gest_pov') { throw 'DbName' }
if ($paths.DbAppUser -ne 'gest_pov_app') { throw 'DbAppUser' }
Write-Host '[OK] Get-GestPovPaths'
Write-Host 'Tous les tests secrets/paths OK.'
exit 0
