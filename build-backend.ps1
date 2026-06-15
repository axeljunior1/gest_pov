# Build du backend Gest_POV — produit le JAR exécutable
param(
    [switch]$SkipTests = $true,
    [switch]$Install
)

$ErrorActionPreference = "Stop"
$Root = $PSScriptRoot
$Backend = Join-Path $Root "backend"

if (-not (Test-Path $Backend)) {
    Write-Error "Dossier backend introuvable : $Backend"
    exit 1
}

if (-not $env:JAVA_HOME) {
    $defaultJdk = "C:\Program Files\Java\jdk-17.0.12"
    if (Test-Path $defaultJdk) {
        $env:JAVA_HOME = $defaultJdk
    }
}

Write-Host "Build backend Gest_POV..." -ForegroundColor Cyan
Set-Location $Backend

$mvnArgs = @("clean", "package")
if ($SkipTests) {
    $mvnArgs += "-DskipTests"
}
if ($Install) {
    $mvnArgs = @("clean", "install")
    if ($SkipTests) { $mvnArgs += "-DskipTests" }
}

$prevEap = $ErrorActionPreference
$ErrorActionPreference = "Continue"
& mvn @mvnArgs 2>&1 | Out-Host
$mvnExit = $LASTEXITCODE
$ErrorActionPreference = $prevEap

if ($mvnExit -ne 0) {
    Write-Host "Echec du build Maven (code $mvnExit)." -ForegroundColor Red
    exit $mvnExit
}

$jar = Get-ChildItem -Path "target" -Filter "gest-pov-backend.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $jar) {
    $jar = Get-ChildItem -Path "target" -Filter "*.jar" -Exclude "*original*" -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
}

if (-not $jar) {
    Write-Host "Build termine mais aucun JAR trouve dans target/." -ForegroundColor Red
    exit 1
}

$sizeMb = [math]::Round($jar.Length / 1MB, 2)
Write-Host ""
Write-Host "JAR pret :" -ForegroundColor Green
Write-Host "  $($jar.FullName)" -ForegroundColor White
Write-Host "  Taille : ${sizeMb} Mo" -ForegroundColor DarkGray
Write-Host ""
Write-Host "Lancer en production (PostgreSQL requis) :" -ForegroundColor Cyan
Write-Host "  java -jar `"$($jar.FullName)`"" -ForegroundColor White
Write-Host ""
Write-Host "Variables utiles : SPRING_PROFILES_ACTIVE, SPRING_DATASOURCE_URL, APP_LICENSE_DATA_DIR" -ForegroundColor DarkGray
