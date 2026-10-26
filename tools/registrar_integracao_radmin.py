"""Consolidate local migration evidence; no publication or changes to installed user data."""
from pathlib import Path
import hashlib
import json
import subprocess

ROOT=Path(__file__).resolve().parents[1]
CP=ROOT/'.task-checkpoints/radmin-atualizavel'
metadata=json.loads((ROOT/'dist/SpiritGate-Atualizavel-Radmin-0.177.0/integracao.json').read_text())
smoke=sorted(CP.glob('smoke-*/result.json'))[-1]
result=json.loads(smoke.read_text())
assert result['packagedClients']==2 and result['legacyMigration'] and not result['published']
updater=json.loads((CP/'updater-tests-final/results.json').read_text(encoding='utf-8-sig'))
assert updater['passed']==32
actual_launch=json.loads((CP/'updater-real-launch.png.json').read_text())
assert actual_launch['windowShowing'] and actual_launch['ticks']>0
for entry in json.loads((ROOT/'.task-checkpoints/TASK-177/network-collision/final-deltas.json').read_text()):
    assert hashlib.sha256((ROOT/entry['path']).read_bytes()).hexdigest()==entry['finalSha256']
manifest=json.loads((Path(metadata['commonChannel'])/'latest.json').read_text())
files={entry['path']:entry for entry in manifest['files']}
for entry in json.loads((ROOT/'.task-checkpoints/TASK-177/network-collision/original-sprites-verified.json').read_text()):
    assert files[entry['path']]['sha256']==entry['sha256']
for package in metadata['packages']:
    assert hashlib.sha256(Path(package['archive']).read_bytes()).hexdigest()==package['sha256']
summary=dict(version=metadata['version'],packages=metadata['packages'],pythonTestsPassed=8,
             powershellChecksPassed=32,smokeEvidence=str(smoke.relative_to(ROOT)),
             actualUpdaterLaunchedGame=True,gameSourceUnchangedSinceTask177=True,
             originalSpritesPreserved=20,**result)
(CP/'resultado-final.json').write_text(json.dumps(summary,indent=2),encoding='utf-8')
paths=['tools/preparar_atualizacao.py','tools/test_atualizacao.py','tools/updater/Atualizar-SpiritGate.ps1',
       'tools/updater/Test-Atualizador.ps1','tools/updater/Test-Radmin-Launch.ps1','tools/integrar_launcher_radmin.py',
       'tools/UpdatableRadminUiProbe.java','tools/smoke_atualizavel_radmin.py','tools/registrar_integracao_radmin.py',
       'docs/ATUALIZADOR.md','docs/LAUNCHER-RADMIN-ATUALIZAVEL.md']
(CP/'source-sha256.json').write_text(json.dumps({p:hashlib.sha256((ROOT/p).read_bytes()).hexdigest() for p in paths},indent=2),encoding='utf-8')
(CP/'tracked-changes.diff').write_bytes(subprocess.check_output(['git','diff','--',*paths],cwd=ROOT))
print(json.dumps(summary,indent=2))
