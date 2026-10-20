@echo off
setlocal
cd /d "%~dp0"
if "%~1"=="" (
  echo Uso: testar-mapa-rpg.cmd "pasta-exportada"
  exit /b 1
)
java -cp "server\target\classes" com.projetok.studio.StudioPlaytest "%~1" "%~dp0."
