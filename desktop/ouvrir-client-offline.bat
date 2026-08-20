@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"

set "PKG=desktop\client-package\build\GestPOV-Client-Offline"
if not exist "%PKG%\GestPOV-Client.bat" (
  echo [ERREUR] Package client introuvable : %PKG%
  echo Lancez d'abord : desktop\scripts\build\build-client-package.ps1
  pause
  exit /b 1
)

echo Ouverture du package client...
start "" "%~dp0%PKG%"
echo.
echo Double-cliquez : install-client.bat puis GestPOV-Client.bat
pause
