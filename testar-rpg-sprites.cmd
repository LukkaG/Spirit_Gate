@echo off
setlocal
cd /d "%~dp0"
if not defined SPIRIT_GATE_SPRITE_SCALE set "SPIRIT_GATE_SPRITE_SCALE=0.5"
if not defined SPIRIT_GATE_FRAME_MS set "SPIRIT_GATE_FRAME_MS=250"
if not exist "server\target\classes\com\projetok\prototype\rpg\RpgPlayerSprites.class" (
  echo Compile primeiro: python tools/qualidade.py --offline --task 079-local
  exit /b 1
)
if "%~1"=="--geometrico" (
  java -cp "server\target\classes" com.projetok.prototype.rpg.RpgLauncher
) else if "%~1"=="" (
  java "-Dspiritgate.rpg.spriteScale=%SPIRIT_GATE_SPRITE_SCALE%" "-Dspiritgate.rpg.spriteFrameMillis=%SPIRIT_GATE_FRAME_MS%" -cp "server\target\classes" com.projetok.prototype.rpg.RpgLauncher --experimental-player-sprites
) else (
  echo Uso: testar-rpg-sprites.cmd [--geometrico]
  exit /b 2
)
