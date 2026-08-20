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

title Gest POV - Demarrage
echo.
echo === Demarrage services Gest POV ===
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\server\start-server.ps1" %*
pause
exit /b %ERRORLEVEL%
