@echo off
setlocal
rem Encaminha os argumentos ao build PowerShell e preserva o codigo de erro.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0build-windows-portable.ps1" %*
exit /b %ERRORLEVEL%
