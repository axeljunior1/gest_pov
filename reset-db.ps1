# Reset base ERP — purge metier ou reset complet PostgreSQL
param(
    [ValidateSet('demo', 'full')]
    [string]$Mode = 'demo',
    [switch]$SeedDemo,
    [switch]$SkipBackend
)

$ErrorActionPreference = 'Stop'
$Root = $PSScriptRoot
$DockerDir = Join-Path $Root 'docker'
$BackendDir = Join-Path $Root 'backend'
$ResetSql = Join-Path $Root 'scripts\reset-demo.sql'
$ResetToken = if ($env:APP_RESET_TOKEN) { $env:APP_RESET_TOKEN } else { 'dev-reset-token-change-me' }
$AdminEmail = 'admin@erp.local'
$AdminPassword = 'ErpAdmin2026!'

function Wait-Postgres {
    Write-Host 'Attente PostgreSQL...' -ForegroundColor Yellow
    for ($i = 0; $i -lt 60; $i++) {
        $health = docker inspect --format='{{.State.Health.Status}}' erp-postgres 2>$null
        if ($health -eq 'healthy') {
            Write-Host 'PostgreSQL pret.' -ForegroundColor Green
            return
        }
        Start-Sleep -Seconds 1
    }
    throw 'PostgreSQL non pret apres 60s'
}

function Invoke-DemoSqlReset {
    if (-not (Test-Path $ResetSql)) {
        throw "Script introuvable: $ResetSql"
    }
    Write-Host 'Purge donnees metier (SQL)...' -ForegroundColor Cyan
    Get-Content $ResetSql | docker exec -i erp-postgres psql -U erp_user -d erp_products -v ON_ERROR_STOP=1 | Out-Null
    Write-Host 'Donnees metier purgees.' -ForegroundColor Green
}

function Invoke-FlywayMigrate {
    Write-Host 'Migration Flyway (schema)...' -ForegroundColor Cyan
    Push-Location $BackendDir
    try {
        mvn -q flyway:migrate
        if ($LASTEXITCODE -ne 0) { throw 'flyway:migrate a echoue' }
    } finally {
        Pop-Location
    }
    Write-Host 'Schema Flyway a jour.' -ForegroundColor Green
}

function Start-BackendBootstrap {
    Write-Host 'Demarrage backend (seed referentiel + Flyway baseline)...' -ForegroundColor Cyan
    $env:SPRING_PROFILES_ACTIVE = 'dev'
    Push-Location $BackendDir
    try {
        $proc = Start-Process -FilePath 'mvn' -ArgumentList @(
            '-q', '-DskipTests', 'spring-boot:run',
            '-Dspring-boot.run.profiles=dev',
            '-Dspring-boot.run.jvmArguments=-Dspring.main.web-application-type=none'
        ) -PassThru -NoNewWindow
        $deadline = (Get-Date).AddSeconds(120)
        while ((Get-Date) -lt $deadline) {
            if ($proc.HasExited) {
                throw "Backend arrete prematurement (code $($proc.ExitCode))"
            }
            Start-Sleep -Seconds 2
        }
        Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
        Write-Host 'Referentiel systeme initialise.' -ForegroundColor Green
    } finally {
        Pop-Location
    }
}

function Invoke-SeedDemoApi {
    Write-Host 'Chargement jeu de demo via API...' -ForegroundColor Cyan
    $loginBody = @{ email = $AdminEmail; password = $AdminPassword } | ConvertTo-Json
    try {
        $login = Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/auth/login' `
            -ContentType 'application/json' -Body $loginBody -TimeoutSec 10
    } catch {
        Write-Warning 'Backend HTTP indisponible — lancez .\dev.ps1 puis: Invoke-RestMethod POST /api/admin/seed-demo'
        return
    }
    $token = $login.token
    $headers = @{
        Authorization = "Bearer $token"
        'X-Reset-Token' = $ResetToken
    }
    Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/admin/seed-demo' -Headers $headers -TimeoutSec 30 | Out-Null
    Write-Host 'Jeu de demo charge (DEMO-EAU-1L).' -ForegroundColor Green
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker requis pour reset-db.ps1'
}

if ($Mode -eq 'full') {
    Write-Host '=== Reset COMPLET (volume PostgreSQL) ===' -ForegroundColor Magenta
    Set-Location $DockerDir
    docker compose down -v
    docker compose up -d postgres
    Set-Location $Root
    Wait-Postgres
    Invoke-FlywayMigrate
    if (-not $SkipBackend) {
        Start-BackendBootstrap
    } else {
        Write-Host 'Skip backend: lancez .\dev.ps1 manuellement pour le referentiel.' -ForegroundColor Yellow
    }
} else {
    Write-Host '=== Reset METIER (referentiel conserve) ===' -ForegroundColor Magenta
    & (Join-Path $Root 'db.ps1')
    Wait-Postgres
    Invoke-DemoSqlReset
}

if ($SeedDemo) {
    if ($Mode -eq 'full' -and -not $SkipBackend) {
        Write-Host 'Seed demo: demarrez .\dev.ps1 puis relancez avec -Mode demo -SeedDemo, ou API manuelle.' -ForegroundColor Yellow
    } else {
        Invoke-SeedDemoApi
    }
}

Write-Host ''
Write-Host 'Termine.' -ForegroundColor Green
Write-Host '  Mode       : ' $Mode
Write-Host '  Seed demo  : ' $SeedDemo
Write-Host '  Token admin: ' $ResetToken
