#Requires -Version 5.1
<#
.SYNOPSIS
  Stub documentaire : invocation jpackage pour GestPOV-Client-Setup.exe (Phase H).
  Ne remplace PAS build-client-package.ps1 / build-offline-package.ps1.

.DESCRIPTION
  Verifie la presence de jpackage (et rappelle WiX pour --type exe sous Windows).
  Si les outils manquent, affiche le message et quitte avec code 0 pour ne pas
  casser les pipelines qui appelleraient ce stub par erreur.

  Usage futur (quand JDK + WiX sont installes) :
    .\build-exe-stub.ps1 -ReallyBuild

  Sans -ReallyBuild : affiche la commande documentee et sort.
#>
[CmdletBinding()]
param(
    [string]$RepoRoot,
    [string]$AppVersion = '1.0.0',
    [switch]$ReallyBuild
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if (-not $RepoRoot) {
    $RepoRoot = Resolve-Path (Join-Path $PSScriptRoot '..\..\..')
}

Write-Host '=== Gest POV - stub jpackage (.exe) ==='
Write-Host "Repo : $RepoRoot"
Write-Host ''
Write-Host 'Packages dossier (source de verite USB) :'
Write-Host '  desktop\scripts\build\build-offline-package.ps1'
Write-Host '  desktop\scripts\build\build-client-package.ps1'
Write-Host 'Roadmap : desktop\docs\phase-h.md'
Write-Host ''

function Find-Jpackage {
    $candidates = @()
    if ($env:JAVA_HOME) {
        $candidates += (Join-Path $env:JAVA_HOME 'bin\jpackage.exe')
    }
    $cmd = Get-Command jpackage -ErrorAction SilentlyContinue
    if ($cmd) { $candidates += $cmd.Source }
    foreach ($p in $candidates) {
        if ($p -and (Test-Path $p)) { return $p }
    }
    return $null
}

$jpackage = Find-Jpackage
$wixHint = 'WiX Toolset (candle/light) - requis par jpackage Windows pour --type exe'

function Write-JpackageExample {
    Write-Host 'Exemple d invocation documentee (client) une fois les outils presents :'
    Write-Host '  jpackage \'
    Write-Host '    --type exe \'
    Write-Host '    --name GestPOV-Client \'
    Write-Host '    --app-version 1.0.0 \'
    Write-Host '    --input PATH_TO_JAR_DIR \'
    Write-Host '    --main-jar gest-pov-desktop.jar \'
    Write-Host '    --main-class com.gestpov.desktop.GestPovDesktopApp \'
    Write-Host '    --dest desktop\client-package\build\exe \'
    Write-Host '    --win-dir-chooser \'
    Write-Host '    --win-shortcut \'
    Write-Host '    --win-menu'
}

if (-not $jpackage) {
    Write-Host 'jpackage introuvable (installez un JDK 17+ avec jpackage).'
    Write-Host "Aussi requis : $wixHint"
    Write-Host ''
    Write-JpackageExample
    Write-Host ''
    Write-Host 'Stub : sortie sans erreur (outils manquants). Aucun .exe produit.'
    exit 0
}

Write-Host "jpackage : $jpackage"
Write-Host "Rappel   : $wixHint"
Write-Host ''

$dest = Join-Path $RepoRoot 'desktop\client-package\build\exe'
$jarDir = Join-Path $RepoRoot 'desktop\client\target'
$jar = Get-ChildItem $jarDir -Filter 'gest-pov-desktop*.jar' -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notlike '*sources*' -and $_.Name -notlike '*javadoc*' } |
    Select-Object -First 1

$mainJarName = 'gest-pov-desktop.jar'
if ($jar) { $mainJarName = $jar.Name }

$docArgs = @(
    '--type', 'exe',
    '--name', 'GestPOV-Client',
    '--app-version', $AppVersion,
    '--input', $jarDir,
    '--main-jar', $mainJarName,
    '--main-class', 'com.gestpov.desktop.GestPovDesktopApp',
    '--dest', $dest,
    '--win-dir-chooser',
    '--win-shortcut',
    '--win-menu'
)

Write-Host 'Commande documentee :'
Write-Host ('  jpackage ' + ($docArgs -join ' '))
Write-Host ''

if (-not $ReallyBuild) {
    Write-Host 'Mode documentation (defaut). Relancer avec -ReallyBuild pour tenter jpackage.'
    Write-Host 'Prealable recommande : build-client-package.ps1 (ou mvn package client).'
    exit 0
}

if (-not $jar) {
    Write-Host "JAR client introuvable dans $jarDir - lancez d abord build-client-package.ps1."
    exit 0
}

New-Item -ItemType Directory -Path $dest -Force | Out-Null
Write-Host 'Tentative jpackage (peut echouer sans WiX)...'
& $jpackage @docArgs
if ($LASTEXITCODE -ne 0) {
    Write-Host 'jpackage a echoue (souvent WiX manquant). Packages dossier inchanges.'
    exit 0
}

Write-Host "OK - voir $dest"
exit 0
