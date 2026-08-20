@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"

title Gest POV - Import licence
echo.
echo === Import licence Gest POV ===
echo La licence s'installe sur le SERVEUR (pas dans le client Desktop).
echo.

curl.exe -s http://127.0.0.1:8080/api/license/installation-id
echo.
echo.
echo Copiez l'installationId ci-dessus : la licence .lic doit etre emise pour CET ID.
echo.

set /p LICPATH=Chemin complet du fichier .lic : 
if "%LICPATH%"=="" (
  echo Annule.
  pause
  exit /b 1
)
if not exist "%LICPATH%" (
  echo [ERREUR] Fichier introuvable : %LICPATH%
  pause
  exit /b 1
)

echo.
echo Import en cours...
curl.exe -s -X POST http://127.0.0.1:8080/api/license/import -F "file=@%LICPATH%"
echo.
echo.
echo Statut :
curl.exe -s http://127.0.0.1:8080/api/license/status
echo.
echo.
echo Si valid=true, relancez le client Desktop et reconnectez-vous.
pause
