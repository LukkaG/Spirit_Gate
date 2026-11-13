@echo off
REM Sem argumentos inicia o atendimento automatico; Status e Parar controlam o monitor.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\monitorar-erros.ps1" %*
exit /b %ERRORLEVEL%
