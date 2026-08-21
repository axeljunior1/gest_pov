#Requires -Version 5.1
<#
.SYNOPSIS
  Installation client Gest POV (double-clic via 01-Installer.cmd).
#>
[CmdletBinding()]
param(
    [string]$PackageRoot,
    [switch]$AllUsers
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Test-GestPovAdmin {
    $id = [Security.Principal.WindowsIdentity]::GetCurrent()
    $p = New-Object Security.Principal.WindowsPrincipal($id)
    return $p.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Get-ClientPackageRoot {
    # ui\ -> client scripts parent chain: scripts\client\ui -> need package root
    # In package: <root>\scripts\ui\Start-Install.ps1  OR <root>\scripts\install-client.ps1
    $dir = $PSScriptRoot
    for ($i = 0; $i -lt 6; $i++) {
        if (Test-Path (Join-Path $dir 'app\gest-pov-desktop.jar')) {
            return (Resolve-Path $dir).Path
        }
        $parent = Split-Path $dir
        if (-not $parent -or $parent -eq $dir) { break }
        $dir = $parent
    }
    throw 'Package client introuvable (app\gest-pov-desktop.jar).'
}

if (-not $PackageRoot) {
    $PackageRoot = Get-ClientPackageRoot
} else {
    $PackageRoot = $PackageRoot.TrimEnd('\')
    $PackageRoot = (Resolve-Path $PackageRoot).Path
}

if ($AllUsers -and -not (Test-GestPovAdmin)) {
    Write-Host ''
    Write-Host '[Gest POV] Elevation Administrateur (install tous utilisateurs)...'
    $arg = "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`" -PackageRoot `"$PackageRoot`" -AllUsers"
    Start-Process -FilePath 'powershell.exe' -Verb RunAs -ArgumentList $arg -WorkingDirectory $PackageRoot | Out-Null
    exit 0
}

$install = Join-Path $PSScriptRoot '..\install-client.ps1'
if (-not (Test-Path $install)) {
    $install = Join-Path $PackageRoot 'scripts\install-client.ps1'
}
if (-not (Test-Path $install)) {
    throw "install-client.ps1 introuvable."
}

Write-Host ''
Write-Host '========================================'
Write-Host '  Gest POV Client - INSTALLATION'
Write-Host '========================================'
Write-Host ''
Write-Host "Package : $PackageRoot"
if ($AllUsers) {
    Write-Host 'Mode    : Tous les utilisateurs (Program Files)'
} else {
    Write-Host 'Mode    : Utilisateur courant (%LOCALAPPDATA%)'
}
Write-Host ''

try {
    if ($AllUsers) {
        & $install -PackageRoot $PackageRoot -AllUsers
    } else {
        & $install -PackageRoot $PackageRoot
    }
    $code = 0
} catch {
    Write-Host "[ECHEC] $($_.Exception.Message)" -ForegroundColor Red
    $code = 1
}

Write-Host ''
if ($code -ne 0) {
    Write-Host "[ECHEC] Installation code $code" -ForegroundColor Red
} else {
    Write-Host '[OK] Client installe.' -ForegroundColor Green
    Write-Host 'Lancez via le raccourci Bureau / menu Demarrer, ou 02-Menu.cmd option 3.'
}
Write-Host ''
Read-Host 'Appuyez sur Entree pour fermer'
exit $code
