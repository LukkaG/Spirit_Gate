@echo off
setlocal
for %%I in ("%~dp0..") do set "SKILLS_PREVIEW_ROOT=%%~fI"
set "SKILLS_PREVIEW_CLASSES=%SKILLS_PREVIEW_ROOT%\.task-checkpoints\SKILLS40\preview-classes-v2"
pushd "%SKILLS_PREVIEW_ROOT%" || exit /b 1

echo [1/3] Compilando as fontes locais com JDK 17, sem Maven ou rede...
call powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%SKILLS_PREVIEW_ROOT%\tools\compile-local.ps1"
if errorlevel 1 goto failed
if not exist "%SKILLS_PREVIEW_CLASSES%" mkdir "%SKILLS_PREVIEW_CLASSES%"
if errorlevel 1 goto failed

echo [2/3] Fontes compiladas. Preparando a ferramenta de treino...
javac --release 17 -encoding UTF-8 -cp "%SKILLS_PREVIEW_ROOT%\server\target\launcher\classes" -d "%SKILLS_PREVIEW_CLASSES%" "%SKILLS_PREVIEW_ROOT%\tools\SkillsLocalPreview.java"
if errorlevel 1 goto failed
echo [3/3] Abrindo a janela "Spirit Gate - Revisar habilidades"...
echo Escolha o kit nessa janela para iniciar a partida local.
java -cp "%SKILLS_PREVIEW_CLASSES%;%SKILLS_PREVIEW_ROOT%\server\target\launcher\classes" SkillsLocalPreview "%SKILLS_PREVIEW_ROOT%"
if errorlevel 1 goto failed
popd
exit /b 0

:failed
echo A previa nao foi iniciada ou foi encerrada com erro. Consulte a mensagem acima.
echo E necessario ter JDK 17 ou superior no PATH. Maven e cache local nao sao necessarios.
pause
popd
exit /b 1
