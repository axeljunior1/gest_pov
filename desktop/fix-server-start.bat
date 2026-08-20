@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"

set "PKG=desktop\server-package\build\GestPOV-Server-Offline"
if not exist "%PKG%\fix-server-start.bat" (
  echo [ERREUR] fix-server-start.bat introuvable dans %PKG%
  pause
  exit /b 1
)

call "%PKG%\fix-server-start.bat"
