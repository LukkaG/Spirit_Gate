@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\launch-local.ps1" -Mode Studio %*
exit /b %ERRORLEVEL%
