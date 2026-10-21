@echo off
setlocal
if "%~1"=="--geometrico" (
  powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\launch-local.ps1" -Mode Sprites -Geometric
) else if "%~1"=="" (
  powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\launch-local.ps1" -Mode Sprites
) else (
  echo Uso: testar-rpg-sprites.cmd [--geometrico]
  exit /b 2
)
exit /b %ERRORLEVEL%
