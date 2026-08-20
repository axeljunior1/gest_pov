#Requires -Version 5.1
#Requires -RunAsAdministrator
<#
.SYNOPSIS
  Desinstallation serveur Gest POV.
  Par defaut : services, binaires, firewall. Donnees conservees.
.PARAMETER PurgeData
  Supprime aussi ProgramData (DB, licence, backups, secrets).
#>
[CmdletBinding()]
param(
    [switch]$PurgeData,
    [string]$ConfirmPhrase
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'lib\GestPovCommon.ps1')
Assert-GestPovAdministrator
$P = Get-GestPovPaths
New-Item -ItemType Directory -Path $P.LogsDir -Force | Out-Null
$log = $P.InstallerLog
Write-GestPovLog -LogFile $log -Message "=== uninstall-server.ps1 PurgeData=$PurgeData ==="

$winExe = Join-Path $P.ServerDir 'GestPOV-Server.exe'
if (Test-Path $winExe) {
    try { & $winExe stop } catch { }
    try { & $winExe uninstall } catch { }
}

$app = Get-GestPovService $P.AppServiceName
if ($app) {
    if ($app.Status -eq 'Running') { Stop-Service $P.AppServiceName -Force -ErrorAction SilentlyContinue }
    sc.exe delete $P.AppServiceName | Out-Null
    Write-GestPovLog -LogFile $log -Message "Service $($P.AppServiceName) supprime."
}

$pg = Get-GestPovService $P.PgServiceName
if ($pg) {
    if ($pg.Status -eq 'Running') { Stop-Service $P.PgServiceName -Force -ErrorAction SilentlyContinue }
    $pgctl = Join-Path $P.PostgresDir 'bin\pg_ctl.exe'
    if (Test-Path $pgctl) {
        & $pgctl unregister -N $P.PgServiceName -ErrorAction SilentlyContinue
    }
    sc.exe delete $P.PgServiceName | Out-Null
    Write-GestPovLog -LogFile $log -Message "Service $($P.PgServiceName) supprime."
}

foreach ($rule in @($P.FirewallTcpName, $P.FirewallUdpName)) {
    netsh advfirewall firewall delete rule name="$rule" | Out-Null
}
Write-GestPovLog -LogFile $log -Message "Regles firewall GestPOV supprimees."

if (Test-Path $P.InstallRoot) {
    Remove-Item -LiteralPath $P.InstallRoot -Recurse -Force -ErrorAction SilentlyContinue
    Write-GestPovLog -LogFile $log -Message "Binaires $($P.InstallRoot) supprimes."
}

if ($PurgeData) {
    $ok = $ConfirmPhrase -eq 'PURGE'
    if (-not $ok) {
        Write-Host "ATTENTION : -PurgeData supprime irrevocablement $($P.DataRoot)"
        Write-Host "(base PostgreSQL, licence, uploads, backups, secrets, server.id)."
        $typed = Read-Host "Tapez exactement PURGE pour confirmer (toute autre saisie annule)"
        $ok = $typed -eq 'PURGE'
    }
    if (-not $ok) {
        throw "Purge annulee : confirmation PURGE manquante."
    }
    if (Test-Path $P.DataRoot) {
        Remove-Item -LiteralPath $P.DataRoot -Recurse -Force
        Write-Host "Donnees ProgramData purges."
    }
} else {
    Write-Host "Donnees conservees dans $($P.DataRoot) (licence, backups, PostgreSQL)."
    Write-Host "Purge complete : uninstall-server.ps1 -PurgeData"
}

Write-Host "Desinstallation terminee."
