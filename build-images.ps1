# Build des images Docker (backend + frontend) — Windows PowerShell
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

Write-Host "==> Build monapp-backend:1.0.0" -ForegroundColor Cyan
docker build -t monapp-backend:1.0.0 -f backend/Dockerfile backend
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "==> Build monapp-frontend:1.0.0" -ForegroundColor Cyan
docker build -t monapp-frontend:1.0.0 -f frontend/Dockerfile frontend
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host ""
Write-Host "Images pretes :" -ForegroundColor Green
docker images monapp-backend:1.0.0 monapp-frontend:1.0.0
