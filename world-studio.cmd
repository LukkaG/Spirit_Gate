@echo off
setlocal
cd /d "%~dp0"
if not exist "server\target\classes\com\projetok\studio\WorldStudio.class" (
  echo Compile primeiro: python tools/qualidade.py --offline --task studio-local
  exit /b 1
)
java -cp "server\target\classes" com.projetok.studio.WorldStudio --project "%~dp0."
