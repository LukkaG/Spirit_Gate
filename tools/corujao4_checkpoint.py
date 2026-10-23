"""Checkpoints incrementais recuperáveis, sem copiar saves pessoais ou releases."""
from pathlib import Path
import difflib
import hashlib
import json
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def inventory():
    names = subprocess.check_output(
        ['git', 'ls-files', '--cached', '--others', '--exclude-standard', '-z'], cwd=ROOT
    ).decode('utf-8').split('\0')
    return {n: hashlib.sha256((ROOT/n).read_bytes()).hexdigest() for n in names
            if n and (ROOT/n).is_file() and not n.startswith(('saves/', 'dist/', '.task-checkpoints/'))}


def checkpoint(task, phase):
    destination = ROOT/'.task-checkpoints'/f'TASK-{task}'/'corujao-iv'
    destination.mkdir(parents=True, exist_ok=True)
    target = destination/f'{phase}.json'
    if target.exists():
        raise ValueError(f'Checkpoint existente: {target}')
    baseline = ROOT/'.task-checkpoints/TASK-134/corujao-iv'
    base = json.loads((baseline/'before.json').read_text(encoding='utf-8'))['files']
    hashes = inventory()
    changed = [n for n, h in hashes.items() if base.get(n) != h]
    with zipfile.ZipFile(destination/f'{phase}.zip', 'w', zipfile.ZIP_DEFLATED) as archive:
        for name in changed:
            archive.write(ROOT/name, name)
    metadata = {'head': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT).decode().strip(),
                'files': hashes, 'removedFromBase': sorted(set(base)-set(hashes)),
                'baseArchive': '../../TASK-134/corujao-iv/before.zip',
                'restore': 'Extrair baseArchive em pasta nova, remover removedFromBase, sobrepor este ZIP. Não restaurar sobre saves pessoais.',
                'archiveSha256': hashlib.sha256((destination/f'{phase}.zip').read_bytes()).hexdigest()}
    target.write_text(json.dumps(metadata, indent=2, ensure_ascii=False), encoding='utf-8')
    if phase == 'after':
        before = json.loads((destination/'before.json').read_text(encoding='utf-8'))
        diff = []
        with zipfile.ZipFile(baseline/'before.zip') as base_zip, zipfile.ZipFile(destination/'before.zip') as overlay:
            for name in sorted(set(before['files']) | set(hashes)):
                if before['files'].get(name) == hashes.get(name):
                    continue
                if Path(name).suffix not in ('.java', '.py', '.md', '.json', '.ps1', '.properties', '.cmd'):
                    continue
                previous = overlay.read(name) if name in overlay.namelist() else base_zip.read(name) if name in base_zip.namelist() else b''
                current = (ROOT/name).read_bytes() if name in hashes else b''
                diff.extend(difflib.unified_diff(previous.decode('utf-8', errors='replace').splitlines(True),
                    current.decode('utf-8', errors='replace').splitlines(True), fromfile='before/'+name, tofile='after/'+name))
        (destination/'isolated.diff').write_text(''.join(diff), encoding='utf-8')
    print(f'TASK-{task}: {phase}, {len(hashes)} hashes, {len(changed)} arquivos sobre a base')


if __name__ == '__main__':
    checkpoint(*sys.argv[1:])
