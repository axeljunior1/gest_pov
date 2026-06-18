@echo off
setlocal EnableExtensions

rem Lance backend (Spring Boot) + frontend (Vite) sans tests.
rem Equivalent simplifie de dev.ps1 pour invite de commandes Windows.

set "ROOT=%~dp0"
cd /d "%ROOT%"

if not defined JAVA_HOME (
    if exist "C:\Program Files\Java\jdk-17.0.12" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-17.0.12"
    )
)

rem Aligner le backend sur docker/postgres.env (erp-postgres)
if not defined SPRING_DATASOURCE_URL set "SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/erp_products"
if not defined SPRING_DATASOURCE_USERNAME set "SPRING_DATASOURCE_USERNAME=erp_user"
if not defined SPRING_DATASOURCE_PASSWORD set "SPRING_DATASOURCE_PASSWORD=ErpProd2026!"

echo.
echo [1/3] Demarrage PostgreSQL...
powershell -NoProfile -ExecutionPolicy Bypass -File "%ROOT%db.ps1"
if errorlevel 1 (
    echo Echec demarrage PostgreSQL.
    exit /b 1
)

echo.
echo [2/3] Demarrage backend (port 8080, tests ignores)...
start "ERP Backend" /D "%ROOT%backend" cmd /k "set SPRING_PROFILES_ACTIVE=dev && set SPRING_DEVTOOLS_RESTART_ENABLED=false && mvn -q -DskipTests spring-boot:run"

echo [3/3] Demarrage frontend (port 5173)...
start "ERP Frontend" /D "%ROOT%frontend" cmd /k "npm run dev -- --host"

echo.
echo Backend  -^> http://localhost:8080
echo Frontend -^> http://localhost:5173
echo PostgreSQL -^> localhost:5432 / erp_products (erp_user)
echo.
echo Fermez les fenetres "ERP Backend" et "ERP Frontend" pour arreter les services.
echo.

endlocal
