#Requires -Version 5.1
<#
.SYNOPSIS
  Menu serveur Gest POV (double-clic via 02-Menu.cmd).
  Toute la logique est ici en PowerShell — les .cmd ne font que lancer ce fichier.
#>
[CmdletBinding()]
param(
    [string]$PackageRoot
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Test-GestPovAdmin {
    $id = [Security.Principal.WindowsIdentity]::GetCurrent()
    $p = New-Object Security.Principal.WindowsPrincipal($id)
    return $p.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Request-GestPovAdmin {
    param([string]$ScriptPath, [string]$Pkg)
    Write-Host ''
    Write-Host '[Gest POV] Elevation Administrateur (une seule demande UAC)...'
    $arg = "-NoProfile -ExecutionPolicy Bypass -File `"$ScriptPath`" -PackageRoot `"$Pkg`""
    Start-Process -FilePath 'powershell.exe' -Verb RunAs -ArgumentList $arg -WorkingDirectory $Pkg | Out-Null
    exit 0
}

function Invoke-ServerScript {
    param([string]$Name, [object[]]$ArgumentList = @())
    $path = Join-Path $PSScriptRoot "..\$Name"
    if (-not (Test-Path $path)) { throw "Script introuvable : $path" }
    & $path @ArgumentList
}

if (-not $PackageRoot) {
    $PackageRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
}

$scriptsDir = Join-Path $PackageRoot 'scripts\server'
if (-not (Test-Path (Join-Path $scriptsDir 'health-check.ps1'))) {
    # Si lance depuis le repo source (ui/), remonter autrement
    $scriptsDir = Join-Path $PSScriptRoot '..'
}

Set-Location $PackageRoot

while ($true) {
    Clear-Host
    Write-Host ''
    Write-Host '========================================'
    Write-Host '  Gest POV Serveur - MENU'
    Write-Host '========================================'
    Write-Host ''
    Write-Host '  Usage quotidien'
    Write-Host '  ---------------'
    Write-Host '  1. Demarrer'
    Write-Host '  2. Arreter'
    Write-Host '  3. Redemarrer'
    Write-Host '  4. Diagnostic (etat du serveur)'
    Write-Host ''
    Write-Host '  Si quelque chose ne marche pas'
    Write-Host '  ------------------------------'
    Write-Host '  5. Reparrer demarrage'
    Write-Host '  6. Resync mot de passe base de donnees'
    Write-Host ''
    Write-Host '  Desinstallation'
    Write-Host '  ---------------'
    Write-Host '  9. Desinstaller (conserve les donnees)'
    Write-Host ''
    Write-Host '  0. Quitter'
    Write-Host ''
    $choix = Read-Host 'Votre choix'

    $needAdmin = $choix -in @('1', '2', '3', '5', '6', '9')
    if ($needAdmin -and -not (Test-GestPovAdmin)) {
        Request-GestPovAdmin -ScriptPath $PSCommandPath -Pkg $PackageRoot
    }

    try {
        switch ($choix) {
            '1' {
                Write-Host ''; Write-Host '=== Demarrage ==='
                Invoke-ServerScript 'start-server.ps1'
            }
            '2' {
                Write-Host ''; Write-Host '=== Arret ==='
                Invoke-ServerScript 'stop-server.ps1'
            }
            '3' {
                Write-Host ''; Write-Host '=== Redemarrage ==='
                Invoke-ServerScript 'restart-server.ps1'
            }
            '4' {
                Write-Host ''; Write-Host '=== Diagnostic ==='
                Invoke-ServerScript 'health-check.ps1'
            }
            '5' {
                Write-Host ''; Write-Host '=== Reparrer demarrage ==='
                Invoke-ServerScript 'fix-server-start.ps1'
            }
            '6' {
                Write-Host ''; Write-Host '=== Resync mot de passe PostgreSQL ==='
                Invoke-ServerScript 'fix-db-password.ps1'
            }
            '9' {
                Write-Host ''; Write-Host '=== Desinstallation ==='
                Write-Host 'Les donnees dans C:\ProgramData\GestPOV sont conservees.'
                Invoke-ServerScript 'uninstall-server.ps1'
            }
            '0' { exit 0 }
            default { Write-Host 'Choix invalide.'; Start-Sleep -Seconds 1; continue }
        }
    } catch {
        Write-Host "[ECHEC] $($_.Exception.Message)" -ForegroundColor Red
    }

    Write-Host ''
    Read-Host 'Appuyez sur Entree pour revenir au menu'
}
