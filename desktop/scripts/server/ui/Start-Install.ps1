#Requires -Version 5.1
<#
.SYNOPSIS
  Point d'entree installation serveur (double-clic via 01-Installer.cmd).
  Gere l'elevation UAC correctement — ne pas mettre cette logique dans un .bat UTF-8.
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

function Get-PackageRootFromUi {
    # ui\ -> server\ -> scripts\ -> package root
    return (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
}

if (-not $PackageRoot) {
    $PackageRoot = Get-PackageRootFromUi
}

if (-not (Test-GestPovAdmin)) {
    Write-Host ''
    Write-Host '[Gest POV] Elevation Administrateur (une seule demande UAC)...'
    $arg = "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`" -PackageRoot `"$PackageRoot`""
    Start-Process -FilePath 'powershell.exe' -Verb RunAs -ArgumentList $arg -WorkingDirectory $PackageRoot | Out-Null
    exit 0
}

$install = Join-Path $PSScriptRoot '..\install-server.ps1'
if (-not (Test-Path $install)) {
    throw "install-server.ps1 introuvable : $install"
}

Write-Host ''
Write-Host '========================================'
Write-Host '  Gest POV Serveur - INSTALLATION'
Write-Host '========================================'
Write-Host ''
Write-Host "Package : $PackageRoot"
Write-Host ''

try {
    & $install -PackageRoot $PackageRoot
    $code = if ($null -ne $LASTEXITCODE -and $LASTEXITCODE -ne 0) { $LASTEXITCODE } else { 0 }
} catch {
    Write-Host "[ECHEC] $($_.Exception.Message)" -ForegroundColor Red
    $code = 1
}

Write-Host ''
if ($code -ne 0) {
    Write-Host "[ECHEC] Installation code $code" -ForegroundColor Red
    Write-Host 'Ensuite : 02-Menu.cmd puis choix 4 Diagnostic.'
} else {
    Write-Host '[OK] Installation terminee.' -ForegroundColor Green
    Write-Host 'Prochaine etape : double-clic 02-Menu.cmd puis 4) Diagnostic.'
}
Write-Host ''
Read-Host 'Appuyez sur Entree pour fermer'
exit $code
