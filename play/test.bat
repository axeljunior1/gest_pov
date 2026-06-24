@echo off
setlocal
cd /d "%~dp0"
echo Lancement des tests Playwright...
call npm.cmd test
endlocal
