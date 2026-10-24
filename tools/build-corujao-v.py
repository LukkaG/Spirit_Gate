"""Build privado por allowlist; runtime novo, sem copiar saves ou releases anteriores."""
from pathlib import Path
from datetime import datetime, timezone
import hashlib
import json
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def main():
    stamp = datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    build = ROOT / 'server/target' / ('corujao-v-' + stamp)
    build.mkdir()
    classes = build / 'classes'
    classes.mkdir()
    sources = sorted((ROOT / 'server/src/main/java').rglob('*.java'))
    args = build / 'sources.args'
    args.write_text('\n'.join('"' + p.as_posix() + '"' for p in sources), encoding='utf-8')
    subprocess.run(['javac', '--release', '17', '-encoding', 'UTF-8', '-d', str(classes), '@' + str(args)], check=True)
    shutil.copytree(ROOT / 'server/src/main/resources', classes, dirs_exist_ok=True)
    jar = build / 'spiritgate.jar'
    subprocess.run(['jar', '--create', '--file', str(jar), '-C', str(classes), 'com/projetok', '-C', str(classes), 'rpg'], check=True)
    runtime = build / 'runtime'
    modules = 'java.base,java.desktop,java.logging'
    if 'jdk.random@' in subprocess.check_output(['java', '--list-modules'], text=True):
        modules += ',jdk.random'
    subprocess.run(['jlink', '--add-modules', modules, '--strip-debug', '--no-man-pages', '--no-header-files', '--compress=2', '--output', str(runtime)], check=True)
    results = []
    for role in ('Anfitriao', 'Convidado'):
        image = build / role
        (image / 'app').mkdir(parents=True)
        shutil.copyfile(jar, image / 'app/spiritgate.jar')
        shutil.copytree(runtime, image / 'runtime')
        for relative in ('assets/dev/player-base', 'assets/dev/generated/corujao-iv-v1'):
            shutil.copytree(ROOT / relative, image / relative)
        docs = image / 'docs'
        docs.mkdir()
        for source in (ROOT / 'docs').glob('CORUJAO-V-*.md'):
            shutil.copyfile(source, docs / source.name)
        (image / 'Conectar.ps1').write_text('''param([switch]$Radmin)
$ErrorActionPreference='Stop'
Set-Location -LiteralPath $PSScriptRoot
$serverIp=Read-Host 'IP do servidor (ENTER = 127.0.0.1)'
if(!$serverIp){$serverIp='127.0.0.1'}
$portText=Read-Host 'Porta (ENTER = 54557)'
if(!$portText){$portText='54557'}
if($serverIp -notmatch '^\\d{1,3}(\\.\\d{1,3}){3}$' -or $portText -notmatch '^\\d{1,5}$'){throw 'IP ou porta invalida'}
$playerName=Read-Host 'Nome sintetico (3 a 24 caracteres)'
$arguments=@('-cp','app/spiritgate.jar','com.projetok.multiplayer.RpgNetworkClientLauncher',$serverIp,$portText,$playerName,'--combat','--cosmetics')
if($Radmin){$arguments+='--radmin'}
& './runtime/bin/java.exe' @arguments
'''.replace('\n+', '\n'), encoding='utf-8')
        for name, options in (('Cliente-local.cmd', ''), ('Cliente-Radmin.cmd', ' -Radmin')):
            (image / name).write_text('@echo off\r\npowershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0Conectar.ps1"' + options + '\r\npause\r\n', encoding='ascii')
        if role == 'Anfitriao':
            (image / 'Hospedar.ps1').write_text('''param([switch]$Radmin)
$ErrorActionPreference='Stop'
Set-Location -LiteralPath $PSScriptRoot
$portText=Read-Host 'Porta (ENTER = 54557)'
if(!$portText){$portText='54557'}
if($portText -notmatch '^\\d{1,5}$'){throw 'Porta invalida'}
$arguments=@('-cp','app/spiritgate.jar','com.projetok.multiplayer.LocalSimulationServer','--combat','--cosmetics','--port',$portText)
if($Radmin){$vpnIp=Read-Host 'IPv4 26.x.x.x do SEU adaptador Radmin';if($vpnIp -notmatch '^26(\\.\\d{1,3}){3}$'){throw 'IPv4 Radmin invalido'};$arguments+=@('--radmin-bind',$vpnIp)}
& './runtime/bin/java.exe' @arguments
'''.replace('\n+', '\n'), encoding='utf-8')
            for name, options in (('Servidor-local.cmd', ''), ('Servidor-Radmin.cmd', ' -Radmin')):
                (image / name).write_text('@echo off\r\npowershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0Hospedar.ps1"' + options + '\r\npause\r\n', encoding='ascii')
            (image / 'Dois-clientes.ps1').write_text('''$ErrorActionPreference='Stop'
Set-Location -LiteralPath $PSScriptRoot
foreach($playerName in @('Jogador A','Jogador B')){
Start-Process -FilePath "$PSScriptRoot/runtime/bin/java.exe" -WorkingDirectory $PSScriptRoot -WindowStyle Hidden -ArgumentList @('-cp','app/spiritgate.jar','com.projetok.multiplayer.RpgNetworkClientLauncher','127.0.0.1','54557',('"'+$playerName+'"'),'--combat','--cosmetics')
}
'''.replace('\n+', '\n'), encoding='utf-8')
            (image / 'Iniciar-dois-clientes.cmd').write_text('@echo off\r\npowershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0Dois-clientes.ps1"\r\n', encoding='ascii')
        (image / 'LEIA-ME.txt').write_text('Spirit Gate Corujao V - privado experimental\nJava integrado. Extraia todo o ZIP.\n' + ('Abra Servidor-local.cmd, depois Iniciar-dois-clientes.cmd.\nRadmin: Servidor-Radmin.cmd e Cliente-Radmin.cmd no anfitriao; Cliente-Radmin.cmd no convidado.\n' if role == 'Anfitriao' else 'Abra Cliente-Radmin.cmd para VPN, ou Cliente-local.cmd para localhost.\n') + 'WASD mover; SPACE atacar; SHIFT/Q Shunpo (nivel minimo 20; perfis padrao nivel 1); P portal; F NPC; E aceitar missao; C concluir; R respawn apos morte.\nLeia docs/CORUJAO-V-RADMIN-MANUAL.md e limitacoes. Teste remoto real PENDENTE. Sem contas ou persistencia online integrada.\n', encoding='utf-8')
        files = sorted(p for p in image.rglob('*') if p.is_file())
        for p in files:
            if any(part.lower() in ('saves', '.git', '.env', 'studio-projects', 'src') for part in p.relative_to(image).parts):
                raise RuntimeError('Arquivo nao autorizado: ' + str(p))
        manifest = [{'path': p.relative_to(image).as_posix(), 'bytes': p.stat().st_size, 'sha256': hashlib.sha256(p.read_bytes()).hexdigest()} for p in files]
        (image / 'manifest-sha256.json').write_text(json.dumps(manifest, indent=2), encoding='utf-8')
        output = ROOT / 'dist' / ('SpiritGate-CorujaoV-0.175.0-dev-' + role + '-' + stamp + '.zip')
        with zipfile.ZipFile(output, 'x', zipfile.ZIP_DEFLATED) as z:
            for p in files + [image / 'manifest-sha256.json']:
                z.write(p, 'SpiritGateCorujaoV-' + role + '/' + p.relative_to(image).as_posix())
        digest = hashlib.sha256(output.read_bytes()).hexdigest()
        output.with_suffix('.zip.sha256').write_text(digest + '\n', encoding='ascii')
        results.append({'role': role, 'path': str(output.relative_to(ROOT)), 'image': str(image.relative_to(ROOT)), 'sha256': digest, 'files': len(files) + 1})
    checkpoint = ROOT / '.task-checkpoints/TASK-175/corujao-v'
    checkpoint.mkdir(parents=True, exist_ok=True)
    (checkpoint / ('packages-' + stamp + '.json')).write_text(json.dumps(results, indent=2), encoding='utf-8')
    print(json.dumps(results, indent=2))


if __name__ == '__main__':
    main()
