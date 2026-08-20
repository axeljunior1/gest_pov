@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"

title Gest POV - Diagnostic serveur
echo.
echo === Gest POV Health Check ===
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\server\health-check.ps1" %*
set ERR=%ERRORLEVEL%
echo.
if %ERR% neq 0 (
  echo [ECHEC] code %ERR%
) else (
  echo [OK] Controles termines.
)
pause
exit /b %ERR%
