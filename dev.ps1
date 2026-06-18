# Redémarrage rapide backend (8080) + frontend (5173)
param(
    [switch]$StopOnly
)

$ErrorActionPreference = "SilentlyContinue"
$Root = $PSScriptRoot
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.12"

function Stop-PortListener {
    param([int]$Port)
    $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($conn) {
        Stop-Process -Id $conn.OwningProcess -Force -ErrorAction SilentlyContinue
    }
}

Write-Host "Arrêt des services..." -ForegroundColor Yellow
Stop-PortListener 8080
Stop-PortListener 5173
Start-Sleep -Milliseconds 400

if ($StopOnly) {
    Write-Host "Services arrêtés." -ForegroundColor Green
    exit 0
}

Write-Host "Démarrage PostgreSQL..." -ForegroundColor Cyan
& "$Root\db.ps1"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Démarrage backend + frontend..." -ForegroundColor Cyan

$backendCmd = "Set-Location '$Root\backend'; `$env:JAVA_HOME='$env:JAVA_HOME'; if (-not `$env:SPRING_DATASOURCE_URL) { `$env:SPRING_DATASOURCE_URL='jdbc:postgresql://127.0.0.1:5432/erp_products' }; if (-not `$env:SPRING_DATASOURCE_USERNAME) { `$env:SPRING_DATASOURCE_USERNAME='erp_user' }; if (-not `$env:SPRING_DATASOURCE_PASSWORD) { `$env:SPRING_DATASOURCE_PASSWORD='ErpProd2026!' }; `$env:SPRING_DEVTOOLS_RESTART_ENABLED='false'; `$env:SPRING_PROFILES_ACTIVE='dev'; mvn -q -DskipTests spring-boot:run"
$frontendCmd = "Set-Location '$Root\frontend'; npm run dev -- --host"

Start-Process powershell -ArgumentList "-NoExit", "-NoProfile", "-Command", $backendCmd | Out-Null
Start-Process powershell -ArgumentList "-NoExit", "-NoProfile", "-Command", $frontendCmd | Out-Null

Write-Host "Backend  -> http://localhost:8080" -ForegroundColor Green
Write-Host "Frontend -> http://localhost:5173" -ForegroundColor Green
Write-Host "PostgreSQL -> localhost:5432 / erp_products (erp_user)" -ForegroundColor Green
Write-Host "Utilisez .\dev.ps1 -StopOnly pour tout arrêter." -ForegroundColor DarkGray
