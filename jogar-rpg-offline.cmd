@echo off
setlocal
cd /d "%~dp0"
if exist "server\target\classes\com\projetok\prototype\rpg\SpiritGateEntry.class" (
  start "Spirit Gate RPG offline" javaw -cp "server\target\classes" com.projetok.prototype.rpg.RpgLauncher
  exit /b
)
start "Spirit Gate RPG offline" "%~dp0dist\RPG-Offline-0.66.1-rc1\SpiritGateRPG\SpiritGateRPG.exe"
