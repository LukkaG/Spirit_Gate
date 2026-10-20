@echo off
setlocal
"%~dp0runtime\bin\java.exe" -cp "%~dp0app\spirit-gate-coop.jar" com.projetok.multiplayer.NetworkClientLauncher %*