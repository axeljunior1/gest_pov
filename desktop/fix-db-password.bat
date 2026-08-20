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

title Gest POV - Sync mot de passe DB
echo.
echo === Sync mot de passe PostgreSQL + redemarrage ===
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\server\fix-db-password.ps1" %*
set ERR=%ERRORLEVEL%
echo.
if %ERR% neq 0 (
  echo [ECHEC] code %ERR%
  echo Details : C:\ProgramData\GestPOV\logs\fix-db-password.log
) else (
  echo [OK] Serveur joignable sur http://127.0.0.1:8080
)
pause
exit /b %ERR%
