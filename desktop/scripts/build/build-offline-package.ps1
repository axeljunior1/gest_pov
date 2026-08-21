#Requires -Version 5.1
<#
.SYNOPSIS
  Build editeur : package GestPOV-Server-Offline (Internet autorise ici seulement).
  Le resultat USB n'a besoin d'aucun telechargement.
#>
[CmdletBinding()]
param(
    [string]$RepoRoot,
    [switch]$SkipTests,
    [switch]$SkipPostgresDownload
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if (-not $RepoRoot) {
    $RepoRoot = Resolve-Path (Join-Path $PSScriptRoot '..\..\..')
}

$vendor = Join-Path $RepoRoot 'desktop\server-package\vendor'
$outRoot = Join-Path $RepoRoot 'desktop\server-package\build\GestPOV-Server-Offline'
New-Item -ItemType Directory -Path $vendor, $outRoot -Force | Out-Null

Write-Host "=== Build package offline Gest POV Server ==="
Write-Host "Repo : $RepoRoot"
Write-Host "Out  : $outRoot"

# 1) JAR
$mvnArgs = @('-f', (Join-Path $RepoRoot 'backend\pom.xml'), 'package')
if ($SkipTests) { $mvnArgs += '-DskipTests' }
Write-Host "Maven package backend..."
& mvn @mvnArgs
if ($LASTEXITCODE -ne 0) { throw "mvn package backend a echoue." }
$jarSrc = Join-Path $RepoRoot 'backend\target\gest-pov-backend.jar'
if (-not (Test-Path $jarSrc)) { throw "JAR introuvable : $jarSrc" }

# 2) Runtime Java (jlink) — jamais utilise chez le client
# Spring Boot 3.2 cible Java 17 : preferer un JDK 17 meme si JAVA_HOME pointe vers 21+.
$javaHome = $env:JAVA_HOME
$jdk17Candidates = @(
    'C:\Program Files\Java\jdk-17',
    'C:\Program Files\Java\jdk-17.0.12',
    'C:\Program Files\Eclipse Adoptium\jdk-17*',
    'C:\Program Files\Microsoft\jdk-17*'
)
foreach ($pattern in $jdk17Candidates) {
    $hit = Get-Item $pattern -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($hit -and (Test-Path (Join-Path $hit.FullName 'bin\jlink.exe'))) {
        $javaHome = $hit.FullName
        Write-Host "JDK 17 retenu pour jlink : $javaHome"
        break
    }
}
if (-not $javaHome) {
    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCmd) {
        $javaHome = (Resolve-Path (Join-Path (Split-Path $javaCmd.Source) '..')).Path
    }
}
if (-not $javaHome) { throw "JAVA_HOME requis sur la machine editeur pour jlink." }
$jlink = Join-Path $javaHome 'bin\jlink.exe'
$runtimeOut = Join-Path $outRoot 'runtime'
if (Test-Path $runtimeOut) { Remove-Item $runtimeOut -Recurse -Force }
if (-not (Test-Path $jlink)) { throw "jlink.exe introuvable dans $javaHome" }
Write-Host "jlink runtime..."
$jmods = Join-Path $javaHome 'jmods'
$jlinkArgs = @(
    '--output', $runtimeOut,
    '--module-path', $jmods,
    '--add-modules', 'ALL-MODULE-PATH',
    '--strip-debug',
    '--no-header-files',
    '--no-man-pages',
    '--compress=2'
)
& $jlink @jlinkArgs
$jlinkOk = ($LASTEXITCODE -eq 0) -and (Test-Path (Join-Path $runtimeOut 'bin\java.exe'))
if (-not $jlinkOk) {
    Write-Host "jlink a echoue - copie JAVA_HOME (fallback)."
    if (Test-Path $runtimeOut) { Remove-Item $runtimeOut -Recurse -Force }
    robocopy $javaHome $runtimeOut /E /XD jmods include src.zip demo sample man /NFL /NDL /NJH /NJS | Out-Null
    if (-not (Test-Path (Join-Path $runtimeOut 'bin\java.exe'))) {
        throw "Impossible de produire un runtime Java."
    }
}

function Get-EditorAsset {
    param(
        [Parameter(Mandatory)][string]$Uri,
        [Parameter(Mandatory)][string]$OutFile
    )
    $curl = Get-Command curl.exe -ErrorAction SilentlyContinue
    if ($curl) {
        & curl.exe -L --fail --retry 3 --retry-delay 2 --connect-timeout 30 --max-time 1800 -o $OutFile $Uri
        if ($LASTEXITCODE -ne 0) { throw "curl.exe a echoue ($LASTEXITCODE) pour $Uri" }
        return
    }
    Invoke-WebRequest -Uri $Uri -OutFile $OutFile -UseBasicParsing
}

# 3) PostgreSQL binaries (cache vendor)
$pgZip = Get-ChildItem $vendor -Filter 'postgresql-*-windows-x64-binaries.zip' -ErrorAction SilentlyContinue |
    Where-Object { $_.Length -gt 1MB } |
    Select-Object -First 1
$pgUrl = 'https://get.enterprisedb.com/postgresql/postgresql-16.6-1-windows-x64-binaries.zip'
if (-not $pgZip -and -not $SkipPostgresDownload) {
    Write-Host "Telechargement PostgreSQL Windows binaries (editeur)..."
    $destZip = Join-Path $vendor 'postgresql-16.6-1-windows-x64-binaries.zip'
    try {
        Get-EditorAsset -Uri $pgUrl -OutFile $destZip
        $pgZip = Get-Item $destZip
    } catch {
        Write-Host "Echec telechargement. Placez le zip dans desktop/server-package/vendor/ (voir vendor/README.md)"
        throw
    }
}
if (-not $pgZip) { throw "Zip PostgreSQL absent dans vendor/." }

$pgExtract = Join-Path $vendor 'pgsql-extract'
if (Test-Path $pgExtract) { Remove-Item $pgExtract -Recurse -Force }
New-Item -ItemType Directory -Path $pgExtract -Force | Out-Null
Write-Host "Extraction PostgreSQL..."
$tar = Get-Command tar.exe -ErrorAction SilentlyContinue
if ($tar) {
    & tar.exe -xf $pgZip.FullName -C $pgExtract
    if ($LASTEXITCODE -ne 0) { throw "tar.exe extraction PostgreSQL a echoue." }
} else {
    Expand-Archive -Path $pgZip.FullName -DestinationPath $pgExtract -Force
}
$pgSrc = Get-ChildItem $pgExtract -Directory | Where-Object { Test-Path (Join-Path $_.FullName 'bin\initdb.exe') } | Select-Object -First 1
if (-not $pgSrc) {
    # zip EDB often has pgsql\ at root
    $alt = Join-Path $pgExtract 'pgsql'
    if (Test-Path (Join-Path $alt 'bin\initdb.exe')) { $pgSrc = Get-Item $alt }
}
if (-not $pgSrc) { throw "initdb.exe introuvable dans le zip PostgreSQL." }
$pgOut = Join-Path $outRoot 'postgres'
if (Test-Path $pgOut) { Remove-Item $pgOut -Recurse -Force }
robocopy $pgSrc.FullName $pgOut /E /NFL /NDL /NJH /NJS /nc /ns /np | Out-Null

# 4) WinSW
$winsw = Join-Path $vendor 'WinSW.exe'
if (-not (Test-Path $winsw)) {
    $winswUrl = 'https://github.com/winsw/winsw/releases/download/v2.12.0/WinSW-x64.exe'
    Write-Host "Telechargement WinSW (editeur)..."
    Get-EditorAsset -Uri $winswUrl -OutFile $winsw
}

# 5) Assemble package
$backendOut = Join-Path $outRoot 'backend'
New-Item -ItemType Directory -Path $backendOut, (Join-Path $outRoot 'winsw'), (Join-Path $outRoot 'scripts\server\lib') -Force | Out-Null
Copy-Item $jarSrc (Join-Path $backendOut 'gest-pov-server.jar') -Force
Copy-Item $winsw (Join-Path $outRoot 'winsw\WinSW.exe') -Force

# Scripts serveur + UI (menu / install PowerShell)
$srcScripts = Join-Path $RepoRoot 'desktop\scripts\server'
Copy-Item (Join-Path $srcScripts '*.ps1') (Join-Path $outRoot 'scripts\server') -Force
Copy-Item (Join-Path $srcScripts 'lib\*.ps1') (Join-Path $outRoot 'scripts\server\lib') -Force
$uiOut = Join-Path $outRoot 'scripts\server\ui'
New-Item -ItemType Directory -Path $uiOut -Force | Out-Null
Copy-Item (Join-Path $srcScripts 'ui\*.ps1') $uiOut -Force

$tplDir = Join-Path $outRoot 'config\templates'
New-Item -ItemType Directory -Path $tplDir -Force | Out-Null
$tplSrc = Join-Path $RepoRoot 'desktop\server-package\config\application-desktop-server.yml.template'
if (Test-Path $tplSrc) {
    Copy-Item $tplSrc (Join-Path $tplDir 'application-desktop-server.yml.template') -Force
}

# Lanceurs .cmd ASCII+CRLF uniquement (jamais UTF-8 — bug cmd.exe)
. (Join-Path $srcScripts 'lib\Write-GestPovAsciiCmd.ps1')
Write-GestPovAsciiCmd -Path (Join-Path $outRoot '01-Installer.cmd') -Content @'
@echo off
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\server\ui\Start-Install.ps1"
'@
Write-GestPovAsciiCmd -Path (Join-Path $outRoot '02-Menu.cmd') -Content @'
@echo off
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\server\ui\Start-Menu.ps1"
'@

$lireMoi = @"
Gest POV Serveur - package offline
==================================
Windows 10/11 64-bit. Internet / Java / PostgreSQL / Docker : NON requis.

FICHIERS A UTILISER (racine)
---------------------------
  01-Installer.cmd   Premiere installation (UAC admin)
  02-Menu.cmd        Demarrer, arreter, diagnostic, reparer...
  LIRE-MOI.txt       Ce fichier

Ne touchez PAS au dossier scripts\ (technique).

INSTALLATION
------------
1. Copier ce dossier sur le PC serveur.
2. Double-clic 01-Installer.cmd (accepte UAC).
3. Double-clic 02-Menu.cmd -> 4) Diagnostic.

Admin initial (nouveau site) :
  C:\ProgramData\GestPOV\config\INITIAL_ADMIN.txt

Clients caisses : package GestPOV-Client-Offline.
API : http://127.0.0.1:8080
Firewall : TCP 8080 + UDP 38471
"@
# LIRE-MOI en ASCII aussi (pas de tiret long / accents)
$lireMoiAscii = ($lireMoi -replace '[^\x00-\x7F]', '?')
$enc = [System.Text.Encoding]::ASCII
[System.IO.File]::WriteAllText((Join-Path $outRoot 'LIRE-MOI.txt'), ($lireMoiAscii -replace "`n", "`r`n"), $enc)

Write-Host ""
Write-Host "Package pret : $outRoot"
Write-Host "Utilisez : 01-Installer.cmd  puis  02-Menu.cmd"
