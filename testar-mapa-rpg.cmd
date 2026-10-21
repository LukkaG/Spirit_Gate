@echo off
setlocal
if "%~1"=="" (
  echo Uso: testar-mapa-rpg.cmd "pasta-exportada"
  exit /b 2
)
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\launch-local.ps1" -Mode Map -MapFolder "%~1"
exit /b %ERRORLEVEL%
