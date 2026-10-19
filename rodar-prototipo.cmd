@echo off
REM Spirit Gate - inicializador didatico da arena visual para Windows.
REM Mantem a execucao dentro da pasta server onde esta o pom.xml.
setlocal
cd /d "%~dp0server"

REM 1. Verifica se o Maven esta configurado no PATH.
where mvn >nul 2>nul
if errorlevel 1 (
    echo ERRO: Maven nao encontrado. Execute mvn -version e revise o PATH.
    pause
    exit /b 1
)

REM 2. Executa os testes JUnit 5 antes de abrir a janela.
echo [Spirit Gate] Compilando e executando testes...
call mvn test
if errorlevel 1 (
    echo.
    echo ERRO: Testes ou compilacao falharam. Veja o primeiro ERROR no terminal.
    pause
    exit /b 1
)

REM 3. Inicia o prototipo local (independente do banco de dados).
echo.
echo [Spirit Gate] Abrindo a arena 2D...
call mvn exec:java -Dexec.mainClass=com.projetok.prototype.DebugLauncher
if errorlevel 1 (
    echo ERRO: Falha ao abrir a janela. Verifique a mensagem acima.
    pause
    exit /b 1
)
endlocal
