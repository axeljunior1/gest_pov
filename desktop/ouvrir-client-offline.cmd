@echo off
cd /d "%~dp0"
set "PKG=desktop\dist\GestPOV-Client-Offline"
if not exist "%PKG%\01-Installer.cmd" set "PKG=desktop\client-package\build\GestPOV-Client-Offline"
if not exist "%PKG%\01-Installer.cmd" (
  echo [ERREUR] Package client introuvable. Lancez construire-installateurs.bat
  pause
  exit /b 1
)
start "" "%~dp0%PKG%"
echo Use: 01-Installer.cmd or 02-Menu.cmd or GestPOV-Client.cmd
pause
