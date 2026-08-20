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

title Gest POV - Installation serveur
echo.
echo === Installation Gest POV Server ===
echo Dossier : %CD%
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\server\install-server.ps1" -PackageRoot "%~dp0" %*
set ERR=%ERRORLEVEL%
echo.
if %ERR% neq 0 (
  echo [ECHEC] Installation code %ERR%
) else (
  echo [OK] Installation terminee. Lancez health-check.bat
)
pause
exit /b %ERR%
