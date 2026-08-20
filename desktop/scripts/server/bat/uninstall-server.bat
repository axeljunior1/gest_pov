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

title Gest POV - Desinstallation
echo.
echo === Desinstallation Gest POV Server ===
echo Les donnees dans C:\ProgramData\GestPOV sont conservees par defaut.
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\server\uninstall-server.ps1" %*
pause
exit /b %ERRORLEVEL%
