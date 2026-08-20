#Requires -Version 5.1
<#
.SYNOPSIS
  Build editeur : package GestPOV-Client-Offline (Internet autorise ici seulement).
  Le resultat USB n'a besoin d'aucun telechargement.
#>
[CmdletBinding()]
param(
    [string]$RepoRoot,
    [switch]$SkipTests
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if (-not $RepoRoot) {
    $RepoRoot = Resolve-Path (Join-Path $PSScriptRoot '..\..\..')
}

$clientPom = Join-Path $RepoRoot 'desktop\client\pom.xml'
$outRoot = Join-Path $RepoRoot 'desktop\client-package\build\GestPOV-Client-Offline'
if (Test-Path $outRoot) {
    Remove-Item $outRoot -Recurse -Force
}
New-Item -ItemType Directory -Path $outRoot -Force | Out-Null

Write-Host "=== Build package offline Gest POV Client ==="
Write-Host "Repo : $RepoRoot"
Write-Host "Out  : $outRoot"

$mvnArgs = @('-f', $clientPom, 'package', '-Djavafx.platform=win')
if ($SkipTests) { $mvnArgs += '-DskipTests' }
Write-Host "Maven package client..."
& mvn @mvnArgs
if ($LASTEXITCODE -ne 0) { throw "mvn package client a echoue." }

$jarSrc = Join-Path $RepoRoot 'desktop\client\target\gest-pov-desktop-1.0.0-SNAPSHOT.jar'
if (-not (Test-Path $jarSrc)) {
    $hit = Get-ChildItem (Join-Path $RepoRoot 'desktop\client\target') -Filter 'gest-pov-desktop*.jar' |
        Where-Object { $_.Name -notlike '*sources*' -and $_.Name -notlike '*javadoc*' } |
        Select-Object -First 1
    if (-not $hit) { throw "JAR client introuvable dans desktop/client/target." }
    $jarSrc = $hit.FullName
}

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

$appDir = Join-Path $outRoot 'app'
$libDir = Join-Path $appDir 'lib'
$fxDir = Join-Path $appDir 'javafx'
New-Item -ItemType Directory -Path $libDir, $fxDir -Force | Out-Null
Copy-Item $jarSrc (Join-Path $appDir 'gest-pov-desktop.jar') -Force

$depDir = Join-Path $RepoRoot 'desktop\client\target\lib'
if (-not (Test-Path $depDir)) { throw "Dependances Maven introuvables : $depDir" }
Get-ChildItem $depDir -Filter '*.jar' | ForEach-Object {
    if ($_.Name -like 'javafx-*') {
        Copy-Item $_.FullName $fxDir -Force
    } else {
        Copy-Item $_.FullName $libDir -Force
    }
}
if (-not (Get-ChildItem $fxDir -Filter 'javafx-controls*.jar' -ErrorAction SilentlyContinue)) {
    throw "JAR JavaFX Windows manquants. Relancez avec un JDK/Maven capable de resoudre javafx.platform=win."
}

$scriptsDir = Join-Path $outRoot 'scripts'
New-Item -ItemType Directory -Path $scriptsDir -Force | Out-Null
Copy-Item (Join-Path $RepoRoot 'desktop\scripts\client\install-client.ps1') (Join-Path $scriptsDir 'install-client.ps1') -Force

@"
@echo off
setlocal
set DIR=%~dp0
set JAVA=%DIR%runtime\bin\javaw.exe
if not exist "%JAVA%" set JAVA=%DIR%runtime\bin\java.exe
set FX=%DIR%app\javafx
set CP=%DIR%app\gest-pov-desktop.jar
if exist "%DIR%app\lib\*" set CP=%CP%;%DIR%app\lib\*
"%JAVA%" --module-path "%FX%" --add-modules javafx.controls,javafx.fxml -cp "%CP%" com.gestpov.desktop.GestPovDesktopApp
"@ | Set-Content (Join-Path $outRoot 'GestPOV-Client.bat') -Encoding ASCII

@(
    '$here = $PSScriptRoot'
    '& (Join-Path $here ''scripts\install-client.ps1'') -PackageRoot $here @args'
) | Set-Content (Join-Path $outRoot 'install-client.ps1') -Encoding UTF8

@"
@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"
echo.
echo === Installation Gest POV Client ===
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\install-client.ps1" -PackageRoot "%~dp0" %*
set ERR=%ERRORLEVEL%
echo.
if %ERR% neq 0 (echo [ECHEC] code %ERR%) else (echo [OK] Client installe.)
pause
exit /b %ERR%
"@ | Set-Content (Join-Path $outRoot 'install-client.bat') -Encoding ASCII

@"
Gest POV Client - package offline
=================================
Poste caisse / bureau. PAS de PostgreSQL, PAS de Spring Boot.

Prealable : le serveur Gest POV doit deja tourner sur le LAN
(package GestPOV-Server-Offline).

1. Copier ce dossier sur le PC (cle USB).
2. Double-clic : install-client.bat
3. Lancer Gest POV depuis le menu Demarrer, ou :
     GestPOV-Client.bat
4. Premier lancement : decouverte UDP du serveur, puis login.

Config locale : %APPDATA%\GestPOV\client.properties
Compte : celui du serveur (INITIAL_ADMIN.txt ou utilisateur cree).
Licence : s'importe sur le SERVEUR, pas sur le client.
"@ | Set-Content (Join-Path $outRoot 'README.txt') -Encoding UTF8

Write-Host ""
Write-Host "Package pret : $outRoot"
Write-Host "Copiez ce dossier sur une cle USB (postes caisse)."
