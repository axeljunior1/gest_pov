@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0client"

title Gest POV Desktop - Dev
echo.
echo === Lancement client Desktop (dev) ===
echo Serveur attendu : http://127.0.0.1:8080
echo.

where mvn >nul 2>&1
if errorlevel 1 (
  echo [ERREUR] Maven ^(mvn^) introuvable dans le PATH.
  pause
  exit /b 1
)

mvn -q javafx:run
set ERR=%ERRORLEVEL%
if %ERR% neq 0 pause
exit /b %ERR%
