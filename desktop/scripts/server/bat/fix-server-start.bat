@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"

net session >nul 2>&1
if errorlevel 1 (
  echo.
  echo [Gest POV] Mode Administrateur requis. Relance...
  powershell -NoProfile -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
  exit /b 1
)

title Gest POV - Reparation demarrage
echo.
echo === Reparation + demarrage GestPOV-Server ===
echo.
echo Si le port 8080 est occupe, arretez d'abord npm run dev:backend.
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\server\fix-server-start.ps1" %*
set ERR=%ERRORLEVEL%
echo.
if %ERR% neq 0 (
  echo [ECHEC] code %ERR%
) else (
  echo [OK] Serveur pret : http://127.0.0.1:8080
  echo Admin : C:\ProgramData\GestPOV\config\INITIAL_ADMIN.txt
)
pause
exit /b %ERR%
