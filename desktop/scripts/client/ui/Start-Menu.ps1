#Requires -Version 5.1
<#
.SYNOPSIS
  Menu client Gest POV (double-clic via 02-Menu.cmd).
#>
[CmdletBinding()]
param(
    [string]$PackageRoot
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Get-ClientPackageRoot {
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

function Start-GestPovClientApp {
    param([string]$Root)
    $cmd = Join-Path $Root 'GestPOV-Client.cmd'
    $bat = Join-Path $Root 'GestPOV-Client.bat'
    if (Test-Path $cmd) {
        Start-Process -FilePath $cmd -WorkingDirectory $Root
        return
    }
    if (Test-Path $bat) {
        Start-Process -FilePath $bat -WorkingDirectory $Root
        return
    }
    throw 'GestPOV-Client.cmd introuvable.'
}

if (-not $PackageRoot) {
    $PackageRoot = Get-ClientPackageRoot
} else {
    $PackageRoot = (Resolve-Path ($PackageRoot.TrimEnd('\'))).Path
}

$installUi = Join-Path $PSScriptRoot 'Start-Install.ps1'

while ($true) {
    Clear-Host
    Write-Host ''
    Write-Host '========================================'
    Write-Host '  Gest POV Client - MENU'
    Write-Host '========================================'
    Write-Host ''
    Write-Host '  1. Installer (utilisateur courant)'
    Write-Host '  2. Installer pour tous les utilisateurs (admin)'
    Write-Host '  3. Lancer Gest POV (sans reinstaller)'
    Write-Host '  0. Quitter'
    Write-Host ''
    $choix = Read-Host 'Votre choix'

    try {
        switch ($choix) {
            '1' {
                Start-Process -FilePath 'powershell.exe' -ArgumentList @(
                    '-NoProfile', '-ExecutionPolicy', 'Bypass',
                    '-File', $installUi,
                    '-PackageRoot', $PackageRoot
                ) -WorkingDirectory $PackageRoot -Wait
            }
            '2' {
                Start-Process -FilePath 'powershell.exe' -ArgumentList @(
                    '-NoProfile', '-ExecutionPolicy', 'Bypass',
                    '-File', $installUi,
                    '-PackageRoot', $PackageRoot,
                    '-AllUsers'
                ) -WorkingDirectory $PackageRoot -Wait
            }
            '3' {
                Write-Host ''
                Write-Host 'Lancement...'
                Start-GestPovClientApp -Root $PackageRoot
            }
            '0' { exit 0 }
            default { Write-Host 'Choix invalide.'; Start-Sleep -Seconds 1; continue }
        }
    } catch {
        Write-Host "[ECHEC] $($_.Exception.Message)" -ForegroundColor Red
        Read-Host 'Appuyez sur Entree'
    }
}
