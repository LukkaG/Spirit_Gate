"""Run private native launchers with bounded lifetime and independent evidence."""
from pathlib import Path
import json
import re
import subprocess
import sys
import time


def smoke(image, evidence):
    image, evidence = Path(image).resolve(), Path(evidence).resolve()
    evidence.mkdir(parents=True, exist_ok=False)
    processes, logs = [], []

    def start(executable, arguments, name):
        log = (evidence / (name + '.log')).open('w', encoding='utf-8')
        logs.append(log)
        process = subprocess.Popen([str(image / executable), *arguments], cwd=image,
                                   stdout=log, stderr=subprocess.STDOUT)
        processes.append(process)
        return process

    try:
        server = start('SpiritGateRpgServer.exe', ['--rpg', '--cosmetics', '--bind',
                       '127.0.0.1', '--port', '0', '--run-seconds', '25'], 'server')
        match = None
        for _ in range(100):
            time.sleep(.1)
            match = re.search(r'127\.0\.0\.1:(\d+)', (evidence / 'server.log').read_text())
            if match:
                break
        if not match:
            raise RuntimeError('Server did not announce its loopback endpoint')
        clients = []
        for name, hair, clothing in [('A', 'hair_short', 'tunic'), ('B', 'hair_spiky', 'jacket')]:
            clients.append(start('SpiritGateRpgClient' + name + '.exe',
                ['127.0.0.1', match[1], 'Jogador' + name, str(evidence / ('client-' + name + '.png')),
                 '--cosmetics', '--hair', hair, '--clothing', clothing], 'client-' + name))
        codes = [client.wait(timeout=20) for client in clients]
        snapshots = [json.loads((evidence / ('client-' + name + '.png.json')).read_text()) for name in ('A', 'B')]
        assert all(s['players'] == 2 and s['appearances'] == 2 for s in snapshots), snapshots
        assert snapshots[0]['hair'] != snapshots[1]['hair']
        assert snapshots[0]['race'] != snapshots[1]['race']
        codes.append(server.wait(timeout=30))
        assert codes == [0, 0, 0], codes
        result = dict(serverExited=True, clientProcesses=2, playersObservedA=2,
                      playersObservedB=2, appearanceSynchronized=True, differentRaces=True,
                      differentHair=True, exitCodes=codes, loopbackOnly=True, physicalUiApproved=False)
        (evidence / 'result.json').write_text(json.dumps(result, indent=2), encoding='utf-8')
        print(json.dumps(result))
    finally:
        for process in processes:
            if process.poll() is None:
                process.terminate()
                process.wait(timeout=10)
        for log in logs:
            log.close()


if __name__ == '__main__':
    smoke(*sys.argv[1:])
