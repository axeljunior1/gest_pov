#Requires -Version 5.1
<#
.SYNOPSIS
  Installation client Gest POV Desktop (package offline).
  Copie le runtime JavaFX + JAR vers un dossier local et cree un raccourci.
#>
[CmdletBinding()]
param(
    [string]$PackageRoot,
    [string]$InstallDir,
    [switch]$AllUsers
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if (-not $PackageRoot) {
    $dir = $PSScriptRoot
    $PackageRoot = $null
    for ($i = 0; $i -lt 6; $i++) {
        if (Test-Path (Join-Path $dir 'app\gest-pov-desktop.jar')) {
            $PackageRoot = $dir
            break
        }
        $parent = Split-Path $dir
        if (-not $parent -or $parent -eq $dir) {
            break
        }
        $dir = $parent
    }
    if (-not $PackageRoot) {
        throw "Package client introuvable. Passez -PackageRoot vers GestPOV-Client-Offline (dossier qui contient app\gest-pov-desktop.jar)."
    }
}
$PackageRoot = (Resolve-Path $PackageRoot).Path

$jar = Join-Path $PackageRoot 'app\gest-pov-desktop.jar'
$java = Join-Path $PackageRoot 'runtime\bin\java.exe'
$fx = Join-Path $PackageRoot 'app\javafx'
if (-not (Test-Path $jar)) { throw "Package client incomplet : app\gest-pov-desktop.jar manquant." }
if (-not (Test-Path $java)) { throw "Package client incomplet : runtime\bin\java.exe manquant." }
if (-not (Get-ChildItem $fx -Filter 'javafx-controls*.jar' -ErrorAction SilentlyContinue)) {
    throw "Package client incomplet : JAR JavaFX manquants."
}

if (-not $InstallDir) {
    if ($AllUsers) {
        $InstallDir = 'C:\Program Files\GestPOV\Client'
    } else {
        $InstallDir = Join-Path $env:LOCALAPPDATA 'Programs\GestPOV-Client'
    }
}

if ($AllUsers) {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($identity)
    if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
        throw "L'installation Tous les utilisateurs (-AllUsers) demande PowerShell Administrateur."
    }
}

Write-Host "Installation Gest POV Client"
Write-Host "Source : $PackageRoot"
Write-Host "Cible  : $InstallDir"

New-Item -ItemType Directory -Path $InstallDir -Force | Out-Null
robocopy $PackageRoot $InstallDir /E /NFL /NDL /NJH /NJS /nc /ns /np | Out-Null
if ($LASTEXITCODE -ge 8) { throw "Copie du package client a echoue (robocopy $LASTEXITCODE)." }

$launcherCmd = Join-Path $InstallDir 'GestPOV-Client.cmd'
$launcherBat = Join-Path $InstallDir 'GestPOV-Client.bat'
if (-not (Test-Path $launcherCmd) -and -not (Test-Path $launcherBat)) {
    throw "GestPOV-Client.cmd introuvable apres copie."
}
$launcher = if (Test-Path $launcherCmd) { $launcherCmd } else { $launcherBat }

$programs = if ($AllUsers) {
    Join-Path $env:PROGRAMDATA 'Microsoft\Windows\Start Menu\Programs'
} else {
    Join-Path $env:APPDATA 'Microsoft\Windows\Start Menu\Programs'
}
New-Item -ItemType Directory -Path $programs -Force | Out-Null
$desktop = [Environment]::GetFolderPath('Desktop')

$shell = New-Object -ComObject WScript.Shell
foreach ($lnkPath in @(
        (Join-Path $programs 'Gest POV Client.lnk'),
        (Join-Path $desktop 'Gest POV Client.lnk')
    )) {
    $shortcut = $shell.CreateShortcut($lnkPath)
    $shortcut.TargetPath = $launcher
    $shortcut.WorkingDirectory = $InstallDir
    $shortcut.Description = 'Gest POV Desktop'
    $shortcut.Save()
}

Write-Host ""
Write-Host "Client installe."
Write-Host "Lancez Gest POV Client depuis le bureau ou le menu Demarrer."
Write-Host "Au premier lancement : selection du serveur LAN, puis login."
Write-Host "Config : $env:APPDATA\GestPOV\client.properties"
