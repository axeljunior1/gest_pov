@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"

echo.
echo ========================================
echo  Gest POV — construction installateurs
echo ========================================
echo.
echo Ceci va construire :
echo   1. Package SERVEUR  (PostgreSQL + backend)
echo   2. Package CLIENT   (application caisse)
echo.
echo Duree : plusieurs minutes (Maven + Java + telechargements).
echo Internet requis UNIQUEMENT sur ce PC (editeur).
echo.
pause

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\build\build-all-installers.ps1" -SkipTests
set ERR=%ERRORLEVEL%
echo.
if %ERR% neq 0 (
  echo [ECHEC] code %ERR%
  echo Verifiez JDK 17+, Maven, et la connexion Internet.
  pause
  exit /b %ERR%
)

echo.
echo [OK] Packages dans : desktop\dist\
echo Lisez desktop\INSTALLATION.md pour installer sur les PC.
pause
exit /b 0
