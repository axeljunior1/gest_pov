#Requires -Version 5.1
#Requires -RunAsAdministrator
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'lib\GestPovCommon.ps1')
Assert-GestPovAdministrator
$P = Get-GestPovPaths
$app = Get-GestPovService $P.AppServiceName
if ($app) { Restart-Service $P.AppServiceName -Force }
Write-Host "Service $($P.AppServiceName) redemarre (dependance PostgreSQL conservee)."
