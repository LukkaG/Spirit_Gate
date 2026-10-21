@echo off
setlocal
REM Entrada principal do cliente; compila fontes alteradas sem Maven ou rede.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\launch-local.ps1" -Mode Rpg %*
set "SPIRIT_GATE_EXIT=%ERRORLEVEL%"
if not "%SPIRIT_GATE_EXIT%"=="0" pause
exit /b %SPIRIT_GATE_EXIT%
