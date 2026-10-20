@echo off
cd /d "%~dp0"
"%~dp0runtime\bin\java.exe" -Dspiritgate.rpg.experimentalSprites=false "-Djpackage.app-path=%~dp0SpiritGateRPG.exe" -cp "%~dp0app\spirit-gate-rpg.jar" com.projetok.prototype.rpg.RpgLauncher
