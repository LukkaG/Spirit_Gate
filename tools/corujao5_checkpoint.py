"""Consolidate cumulative per-task checkpoints; never fabricate temporal boundaries."""
from pathlib import Path
import difflib
import hashlib
import json
import subprocess
import zipfile
import sys

ROOT = Path(__file__).resolve().parents[1]


def main(phase='after'):
    if phase not in ('after', 'after-verified'):
        raise ValueError('Unknown checkpoint phase')
    base = ROOT / '.task-checkpoints/TASK-160/corujao-v'
    before = json.loads((base / 'before.json').read_text(encoding='utf-8'))
    names = subprocess.check_output(['git', 'ls-files', '--cached', '--others', '--exclude-standard', '-z'], cwd=ROOT).decode().split('\0')
    hashes = {n: hashlib.sha256((ROOT / n).read_bytes()).hexdigest() for n in names if n and (ROOT / n).is_file() and not n.startswith(('saves/', 'dist/', '.task-checkpoints/', 'studio-projects/'))}
    changes = sorted(n for n, h in hashes.items() if before['files'].get(n) != h)
    removed = sorted(set(before['files']) - set(hashes))
    diff = []
    with zipfile.ZipFile(base / 'before.zip') as archive:
        for name in sorted(set(changes) | set(removed)):
            if Path(name).suffix not in ('.java', '.py', '.md', '.json', '.ps1', '.properties', '.cmd', '.sgquest'):
                continue
            previous = archive.read(name) if name in before['files'] else b''
            current = (ROOT / name).read_bytes() if name in hashes else b''
            diff.extend(difflib.unified_diff(previous.decode('utf-8', errors='replace').splitlines(True), current.decode('utf-8', errors='replace').splitlines(True), fromfile='before/' + name, tofile='after/' + name))
    protected = {}
    mismatches = []
    for folder, files in before['protected'].items():
        current = {n: hashlib.sha256((ROOT / n).read_bytes()).hexdigest() if (ROOT / n).is_file() else None for n in files}
        altered = [n for n in files if current[n] != files[n]]
        protected[folder] = {'baselineFiles': len(files), 'unchanged': len(files) - len(altered), 'altered': altered}
        mismatches.extend(altered)
    if mismatches:
        raise RuntimeError('Protected files changed: ' + repr(mismatches))
    for task in range(160, 176):
        target = ROOT / f'.task-checkpoints/TASK-{task}/corujao-v'
        target.mkdir(parents=True, exist_ok=True)
        zip_path = target / (phase + '.zip')
        with zipfile.ZipFile(zip_path, 'x', zipfile.ZIP_DEFLATED) as archive:
            for name in changes:
                archive.write(ROOT / name, name)
        metadata = {'task': task, 'kind': 'cumulative-integrated-consolidation', 'temporalTaskBoundary': False, 'base': '../../TASK-160/corujao-v/before.zip', 'head': before['head'], 'files': hashes, 'removedFromBase': removed, 'changedFromBase': changes, 'archiveSha256': hashlib.sha256(zip_path.read_bytes()).hexdigest(), 'protected': protected, 'restore': 'Extract initial archive into a NEW directory; remove removedFromBase; overlay after.zip. Never restore over personal saves.'}
        (target / (phase + '.json')).write_text(json.dumps(metadata, indent=2), encoding='utf-8')
        (target / (phase + '.diff')).write_text(''.join(diff), encoding='utf-8')
        (target / 'latest.json').write_text(json.dumps({'phase': phase, 'metadata': phase + '.json'}), encoding='utf-8')
        if task != 160:
            (target / 'before-reference.json').write_text(json.dumps({'base': '../../TASK-160/corujao-v/before.zip', 'temporalTaskBoundary': False}), encoding='utf-8')
    output = ROOT / '.task-checkpoints/TASK-175/corujao-v'
    (output / 'protected-final.json').write_text(json.dumps(protected, indent=2), encoding='utf-8')
    for label, tasks in {'A': [160, 164], 'B': [165, 168], 'C': [169, 172], 'D': [173, 175]}.items():
        (output / f'marco-{label}.json').write_text(json.dumps({'range': tasks, 'kind': 'consolidation-index', 'temporalBoundary': False, 'baseHead': before['head'], 'hashes': hashes}, indent=2), encoding='utf-8')
    print(json.dumps({'changed': len(changes), 'removed': removed, 'protected': protected}, indent=2))


if __name__ == '__main__':
    main(*sys.argv[1:])
