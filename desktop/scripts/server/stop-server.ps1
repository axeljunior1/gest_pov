#Requires -Version 5.1
#Requires -RunAsAdministrator
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'lib\GestPovCommon.ps1')
Assert-GestPovAdministrator
$P = Get-GestPovPaths
$app = Get-GestPovService $P.AppServiceName
if ($app -and $app.Status -eq 'Running') { Stop-Service $P.AppServiceName }
$pg = Get-GestPovService $P.PgServiceName
if ($pg -and $pg.Status -eq 'Running') { Stop-Service $P.PgServiceName }
Write-Host "Services arretes."
