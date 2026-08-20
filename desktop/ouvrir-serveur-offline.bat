@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"

set "PKG=desktop\server-package\build\GestPOV-Server-Offline"
if not exist "%PKG%\install-server.bat" (
  echo [ERREUR] Package serveur introuvable : %PKG%
  echo Lancez d'abord : desktop\scripts\build\build-offline-package.ps1
  pause
  exit /b 1
)

echo Ouverture du package serveur...
start "" "%~dp0%PKG%"
echo.
echo Double-cliquez dans le dossier ouvert :
echo   install-server.bat      ^(1ere install, admin^)
echo   fix-server-start.bat    ^(reparer + demarrer, admin^)
echo   health-check.bat        ^(diagnostic^)
pause
