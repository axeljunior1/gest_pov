# Demarre PostgreSQL Docker (donnees persistantes volume erp_pgdata)
param(
    [switch]$Stop,
    [switch]$Logs
)

$ErrorActionPreference = "Stop"
$DockerDir = Join-Path $PSScriptRoot "docker"

if ($Logs) {
    Set-Location $DockerDir
    docker compose logs -f postgres
    exit 0
}

if ($Stop) {
    Set-Location $DockerDir
    docker compose down
    Write-Host "PostgreSQL arrete (volume erp_pgdata conserve)." -ForegroundColor Green
    exit 0
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Error "Docker n'est pas installe ou pas dans le PATH."
}

Set-Location $DockerDir
docker compose up -d postgres

Write-Host "Attente PostgreSQL..." -ForegroundColor Yellow
$ready = $false
for ($i = 0; $i -lt 30; $i++) {
    $health = docker inspect --format='{{.State.Health.Status}}' erp-postgres 2>$null
    if ($health -eq "healthy") { $ready = $true; break }
    Start-Sleep -Seconds 1
}

if (-not $ready) {
    Write-Warning "Conteneur demarre mais healthcheck non confirme - voir: docker logs erp-postgres"
} else {
    Write-Host "PostgreSQL pret." -ForegroundColor Green
}

Write-Host ""
Write-Host "=== Connexion base ERP ===" -ForegroundColor Cyan
Write-Host "Conteneur   : erp-postgres"
Write-Host "Host        : localhost"
Write-Host "Port        : 5432"
Write-Host "Base        : erp_products"
Write-Host "Utilisateur : erp_user"
Write-Host "Mot de passe: ErpProd2026!"
Write-Host "URL JDBC    : jdbc:postgresql://localhost:5432/erp_products"
Write-Host "Volume      : erp_pgdata"
Write-Host ""
