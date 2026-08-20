#Requires -Version 5.1
#Requires -RunAsAdministrator
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'lib\GestPovCommon.ps1')
Assert-GestPovAdministrator
$P = Get-GestPovPaths
$pg = Get-GestPovService $P.PgServiceName
if ($pg -and $pg.Status -ne 'Running') { Start-Service $P.PgServiceName }
$app = Get-GestPovService $P.AppServiceName
if ($app -and $app.Status -ne 'Running') { Start-Service $P.AppServiceName }
Write-Host "Services demarres (ou deja actifs)."
