"""Audit TASK-178 deltas against the complete dirty TASK-177 baseline; never publish."""
from pathlib import Path
import difflib
import hashlib
import json
import shutil
import subprocess

ROOT=Path(__file__).resolve().parents[1]
CP=ROOT/'.task-checkpoints/TASK-178/collision-fix'
initial=json.loads((CP/'initial-sha256.json').read_text())
diffs=CP/'file-diffs';diffs.mkdir(exist_ok=True)
changed=[]
for path in sorted((ROOT/'server/src').rglob('*')):
    if not path.is_file():continue
    relative=path.relative_to(ROOT).as_posix();digest=hashlib.sha256(path.read_bytes()).hexdigest()
    if initial.get(relative)==digest:continue
    before=CP/'initial-source'/relative
    old=before.read_text(encoding='utf-8') if before.exists() else ''
    new=path.read_text(encoding='utf-8')
    delta=''.join(difflib.unified_diff(old.splitlines(True),new.splitlines(True),fromfile='TASK177/'+relative,tofile='TASK178/'+relative))
    (diffs/(relative.replace('/','__')+'.diff')).write_text(delta,encoding='utf-8')
    changed.append(dict(path=relative,initialSha256=initial.get(relative),finalSha256=digest))
for path in sorted(list((ROOT/'docs').glob('TASK-178*.md'))+list((ROOT/'tools').glob('*178*'))):
    if not path.is_file():continue
    relative=path.relative_to(ROOT).as_posix()
    (diffs/(relative.replace('/','__')+'.diff')).write_text(''.join(difflib.unified_diff([],path.read_text(encoding='utf-8').splitlines(True),fromfile='initial/'+relative,tofile='final/'+relative)),encoding='utf-8')
protected=[p for p in initial if p.startswith('assets/dev/player-base/') or p.endswith('/Protocol.java') or p.endswith('/RpgSave.java') or p.endswith('/LocalSaveStore.java')]
for relative in protected:assert hashlib.sha256((ROOT/relative).read_bytes()).hexdigest()==initial[relative],relative
assert subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT)==(CP/'initial-head.txt').read_bytes()
assert 'JUnit: 514 executed, 514 passed, 0 aborted, 0 failures, 0 skipped' in (CP/'gui-tests.log').read_text(encoding='utf-8')
shutil.copyfile(ROOT/'server/target/local-tests/run-wkdhrdmi/test-results.log',CP/'headless-tests.log')
native_metadata=sorted(CP.glob('packages-*.json'))[-1]
packages=json.loads(native_metadata.read_text())
native_smoke=sorted(CP.glob(native_metadata.stem+'-smoke-*/result.json'))[-1]
native_result=json.loads(native_smoke.read_text());assert native_result['zipAndManifestVerified'] and native_result['clientExitCodes']==[0,0]
updatable=json.loads((ROOT/'dist/SpiritGate-Atualizavel-Radmin-0.178.0/integracao.json').read_text())
update_smoke=sorted((ROOT/'.task-checkpoints/radmin-atualizavel').glob('smoke-*/result.json'))[-1]
update_result=json.loads(update_smoke.read_text());assert update_result['packagedClients']==2 and update_result['legacyMigration']
update_evidence=json.loads((update_smoke.parent/'anfitriao/SpiritGate-Atualizavel/versions'/updatable['manifestSha256']/'installed-manifest.json').read_text())
assert update_evidence['version']=='0.178.0-radmin'
for package in packages:assert hashlib.sha256((ROOT/package['path']).read_bytes()).hexdigest()==package['sha256']
for package in updatable['packages']:assert hashlib.sha256(Path(package['archive']).read_bytes()).hexdigest()==package['sha256']
summary=dict(task='TASK-178',tests=dict(guiPassed=514,headlessPassed=505,headlessAborted=9,failures=0),
             sourceDeltas=changed,protectedHashesVerified=len(protected),nativePackages=packages,
             nativeSmoke=dict(path=str(native_smoke.relative_to(ROOT)),**native_result),
             updatablePackages=updatable['packages'],updatableSmoke=dict(path=str(update_smoke.relative_to(ROOT)),**update_result),
             geometry=dict(playerRadius=12,enemyRadius=19,sceneryHalfWidth=22,sceneryHalfHeight=12,
                           originalLeftContact=368,newLeftContact=358,originalAlphaOverlap=7.5,newAlphaGap=2.5,
                           rawSolid=[380,180,120,120],projectionHeight=48,supportY=300),
             captures=['before-left.png','after-left.png','before-right.png','after-right.png','before-front.png','after-front.png','before-behind.png','after-behind.png','network-debug-0.png','network-debug-1.png'],
             limitations=['Original referenced screenshot absent from attachment','Radmin physical two-computer test pending','Human visual approval pending'],pushed=False,published=False)
(CP/'validation-summary.json').write_text(json.dumps(summary,indent=2),encoding='utf-8')
(CP/'final-status.txt').write_bytes(subprocess.check_output(['git','status','--short'],cwd=ROOT))
print(json.dumps({k:summary[k] for k in ('task','tests','protectedHashesVerified','geometry','limitations')},indent=2))
