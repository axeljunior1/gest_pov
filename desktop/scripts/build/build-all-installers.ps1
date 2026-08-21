#Requires -Version 5.1
<#
.SYNOPSIS
  Construit les deux packages d'installation offline (serveur + client).

.DESCRIPTION
  Enchaine :
    1) GestPOV-Server-Offline  (build-offline-package.ps1)
    2) GestPOV-Client-Offline  (build-client-package.ps1)
  Puis copie/résume les sorties dans desktop\dist\

.PARAMETER SkipTests
  Passe -SkipTests aux builds Maven.

.PARAMETER SkipPostgresDownload
  Ne retélécharge pas PostgreSQL (doit déjà être dans vendor/).

.PARAMETER ServerOnly / ClientOnly
  Ne construit qu'un des deux packages.
#>
[CmdletBinding()]
param(
    [string]$RepoRoot,
    [switch]$SkipTests,
    [switch]$SkipPostgresDownload,
    [switch]$ServerOnly,
    [switch]$ClientOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if (-not $RepoRoot) {
    $RepoRoot = Resolve-Path (Join-Path $PSScriptRoot '..\..\..')
}
$RepoRoot = (Resolve-Path $RepoRoot).Path
$buildDir = Join-Path $RepoRoot 'desktop\scripts\build'
$distDir = Join-Path $RepoRoot 'desktop\dist'
New-Item -ItemType Directory -Path $distDir -Force | Out-Null

$doServer = -not $ClientOnly
$doClient = -not $ServerOnly
if ($ServerOnly -and $ClientOnly) {
    $doServer = $true
    $doClient = $true
}

Write-Host ''
Write-Host '========================================'
Write-Host ' Gest POV — construction installateurs'
Write-Host '========================================'
Write-Host "Repo : $RepoRoot"
Write-Host ''

$serverOut = Join-Path $RepoRoot 'desktop\server-package\build\GestPOV-Server-Offline'
$clientOut = Join-Path $RepoRoot 'desktop\client-package\build\GestPOV-Client-Offline'

if ($doServer) {
    Write-Host '--- [1/2] Package SERVEUR ---'
    $serverParams = @{ RepoRoot = $RepoRoot }
    if ($SkipTests) { $serverParams['SkipTests'] = $true }
    if ($SkipPostgresDownload) { $serverParams['SkipPostgresDownload'] = $true }
    & (Join-Path $buildDir 'build-offline-package.ps1') @serverParams
    if (-not (Test-Path (Join-Path $serverOut '01-Installer.cmd'))) {
        throw "Package serveur incomplet : $serverOut"
    }
}

if ($doClient) {
    Write-Host ''
    Write-Host '--- [2/2] Package CLIENT ---'
    $clientParams = @{ RepoRoot = $RepoRoot }
    if ($SkipTests) { $clientParams['SkipTests'] = $true }
    & (Join-Path $buildDir 'build-client-package.ps1') @clientParams
    if (-not (Test-Path (Join-Path $clientOut '01-Installer.cmd'))) {
        throw "Package client incomplet : $clientOut"
    }
}

# Index lisible dans desktop\dist
$index = @()
$index += 'Gest POV — packages prêts à installer'
$index += '====================================='
$index += ''
$index += 'Ces dossiers sont des installateurs OFFLINE (pas de Setup.exe pour l''instant).'
$index += 'Copiez-les sur une clé USB puis sur les PC cibles.'
$index += ''

if ($doServer -and (Test-Path $serverOut)) {
    $destS = Join-Path $distDir 'GestPOV-Server-Offline'
    Write-Host "Copie serveur -> $destS"
    # /MIR : sync sans Remove-Item (évite verrous pgAdmin / antivirus)
    robocopy $serverOut $destS /MIR /NFL /NDL /NJH /NJS /nc /ns /np /R:1 /W:1 | Out-Null
    if ($LASTEXITCODE -ge 8) { throw "Copie serveur vers dist a échoué (robocopy $LASTEXITCODE)." }
    $index += 'SERVEUR (1 seul PC boutique)'
    $index += "  Dossier : $destS"
    $index += '  Install : double-clic 01-Installer.cmd (UAC)'
    $index += '  Ensuite : 02-Menu.cmd (demarrer / diagnostic / reparer)'
    $index += ''
}

if ($doClient -and (Test-Path $clientOut)) {
    $destC = Join-Path $distDir 'GestPOV-Client-Offline'
    Write-Host "Copie client -> $destC"
    robocopy $clientOut $destC /MIR /NFL /NDL /NJH /NJS /nc /ns /np /R:1 /W:1 | Out-Null
    if ($LASTEXITCODE -ge 8) { throw "Copie client vers dist a échoué (robocopy $LASTEXITCODE)." }
    $index += 'CLIENT (chaque caisse / poste)'
    $index += "  Dossier : $destC"
    $index += '  Install : double-clic 01-Installer.cmd'
    $index += '  Lancer  : GestPOV-Client.cmd ou 02-Menu.cmd'
    $index += ''
}

$index += 'Guide détaillé : desktop\INSTALLATION.md'
$index += ("Généré le : {0:yyyy-MM-dd HH:mm}" -f (Get-Date))
$index -join "`r`n" | Set-Content (Join-Path $distDir 'LIRE-MOI.txt') -Encoding UTF8

Write-Host ''
Write-Host '========================================'
Write-Host ' TERMINÉ — fichiers utiles :'
Write-Host "  $distDir"
Write-Host '  LIRE-MOI.txt'
if ($doServer) { Write-Host "  GestPOV-Server-Offline\  (01-Installer.cmd + 02-Menu.cmd)" }
if ($doClient) { Write-Host "  GestPOV-Client-Offline\  (01-Installer.cmd + 02-Menu.cmd)" }
Write-Host '========================================'
Write-Host ''
Write-Host 'Ouvrir le dossier dist...'
Start-Process explorer.exe $distDir
