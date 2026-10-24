"""Check private ZIP hashes and run packaged runtime/server/two native client processes."""
from pathlib import Path
import hashlib
import json
import socket
import subprocess
import sys
from datetime import datetime, timezone
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def main(metadata):
    packages = json.loads(Path(metadata).read_text(encoding='utf-8'))
    output = ROOT / '.task-checkpoints/TASK-175/corujao-v' / (Path(metadata).stem + '-smoke-' + datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ'))
    output.mkdir()
    for package in packages:
        archive = ROOT / package['path']
        if hashlib.sha256(archive.read_bytes()).hexdigest() != package['sha256']:
            raise RuntimeError('ZIP hash mismatch')
        with zipfile.ZipFile(archive) as z:
            manifest_name = next(n for n in z.namelist() if n.endswith('/manifest-sha256.json'))
            prefix = manifest_name.rsplit('/', 1)[0] + '/'
            manifest = json.loads(z.read(manifest_name))
            for entry in manifest:
                content = z.read(prefix + entry['path'])
                if len(content) != entry['bytes'] or hashlib.sha256(content).hexdigest() != entry['sha256']:
                    raise RuntimeError('Manifest mismatch: ' + entry['path'])
            forbidden = [n for n in z.namelist() if any(part.lower() in ('saves', '.git', '.env', 'studio-projects', 'src') for part in Path(n).parts)]
            if forbidden:
                raise RuntimeError('Unexpected private files: ' + repr(forbidden))
    host = ROOT / packages[0]['image']
    guest = ROOT / packages[1]['image']
    with socket.socket() as reserve:
        reserve.bind(('127.0.0.1', 0))
        port = reserve.getsockname()[1]
    def java(image):
        return str(image / 'runtime/bin/java.exe')
    with (output / 'runtime.log').open('w', encoding='utf-8') as log:
        subprocess.run([java(host), '-version'], cwd=host, stdout=log, stderr=subprocess.STDOUT, check=True)
    clients = []
    logs = []
    server = subprocess.Popen([java(host), '-cp', 'app/spiritgate.jar', 'com.projetok.multiplayer.LocalSimulationServer', '--combat', '--cosmetics', '--port', str(port), '--run-seconds', '30'], cwd=host, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    try:
        first = server.stdout.readline()
        if 'experimental em' not in first:
            raise RuntimeError('Packaged server did not start: ' + first)
        for label, image in (('a', host), ('b', guest)):
            log = (output / ('client-' + label + '.log')).open('w', encoding='utf-8')
            logs.append(log)
            capture = output / ('client-' + label + '.png')
            clients.append(subprocess.Popen([java(image), '-cp', 'app/spiritgate.jar', 'com.projetok.multiplayer.RpgNetworkClientLauncher', '127.0.0.1', str(port), 'Jogador ' + label.upper(), str(capture), '--combat', '--cosmetics'], cwd=image, stdout=log, stderr=subprocess.STDOUT))
        codes = [process.wait(timeout=30) for process in clients]
        remaining, _ = server.communicate(timeout=35)
        (output / 'server.log').write_text(first + remaining, encoding='utf-8')
        sidecars = [json.loads((output / ('client-' + label + '.png.json')).read_text(encoding='utf-8')) for label in ('a', 'b')]
        # Existing launcher sidecar uses actors count under the historical "players" key.
        # Application 3 adds the shared enemy, so three actors means two players + one enemy.
        if codes != [0, 0] or server.returncode != 0 or any(s['players'] != 3 or s['appearances'] != 2 for s in sidecars):
            raise RuntimeError('Native package smoke failed: ' + repr((codes, server.returncode, sidecars)))
        result = {'zipAndManifestVerified': True, 'serverExitCode': server.returncode, 'clientExitCodes': codes, 'nativeProcesses': 3, 'eachClientActors': 3, 'actualPlayers': 2, 'sharedEnemies': 1, 'sidecarPlayersKeyCountsActors': True, 'radminRemoteTested': False}
        (output / 'result.json').write_text(json.dumps(result, indent=2), encoding='utf-8')
        print(json.dumps(result, indent=2))
    finally:
        for process in clients:
            if process.poll() is None:
                process.terminate()
                process.wait(timeout=5)
        if server.poll() is None:
            server.terminate()
            server.wait(timeout=5)
        for log in logs:
            log.close()


if __name__ == '__main__':
    main(sys.argv[1])
