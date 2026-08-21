@echo off
cd /d "%~dp0"
set "PKG=desktop\dist\GestPOV-Server-Offline"
if not exist "%PKG%\01-Installer.cmd" set "PKG=desktop\server-package\build\GestPOV-Server-Offline"
if not exist "%PKG%\01-Installer.cmd" (
  echo [ERREUR] Package serveur introuvable. Lancez construire-installateurs.bat
  pause
  exit /b 1
)
start "" "%~dp0%PKG%"
echo Use only: 01-Installer.cmd then 02-Menu.cmd
pause
