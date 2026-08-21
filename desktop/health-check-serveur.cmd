@echo off
cd /d "%~dp0"
set "PKG=desktop\dist\GestPOV-Server-Offline"
if not exist "%PKG%\02-Menu.cmd" set "PKG=desktop\server-package\build\GestPOV-Server-Offline"
if not exist "%PKG%\02-Menu.cmd" (
  echo [ERREUR] Package serveur introuvable.
  pause
  exit /b 1
)
start "" "%~dp0%PKG%\02-Menu.cmd"
