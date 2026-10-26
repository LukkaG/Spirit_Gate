"""Verify TASK-180 against the dirty entry baseline, artifacts and real TCP proofs."""
from pathlib import Path
import hashlib
import difflib
import json
import shutil
import subprocess
import struct
import zipfile

ROOT=Path(__file__).resolve().parents[1]
CP=ROOT/'.task-checkpoints/TASK-180/live-publishing'
RUN=ROOT/'server/target/local-tests/run-9ap278e_'

def sha(path):
    digest=hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda:stream.read(1024*1024),b''):digest.update(block)
    return digest.hexdigest()

initial=json.loads((CP/'baseline-sha256.json').read_text())
diffs=CP/'file-diffs';diffs.mkdir(exist_ok=True)
paths=list((ROOT/'server/src').rglob('*.java'))+list((ROOT/'docs').glob('TASK-180*.md'))+list((ROOT/'tools').glob('*180*'))
changes=[]
for path in sorted(paths):
    if not path.is_file():continue
    relative=path.relative_to(ROOT).as_posix();digest=sha(path)
    if initial.get(relative)==digest:continue
    before=CP/'baseline'/relative
    assert relative not in initial or before.exists(),relative
    old=before.read_text(encoding='utf-8') if before.exists() else ''
    delta=''.join(difflib.unified_diff(old.splitlines(True),path.read_text(encoding='utf-8').splitlines(True),fromfile='TASK179/'+relative,tofile='TASK180/'+relative))
    (diffs/(relative.replace('/','__')+'.diff')).write_text(delta,encoding='utf-8')
    changes.append(dict(path=relative,before=initial.get(relative),after=digest))
protected=[p for p in initial if p.startswith('assets/dev/player-base/') or p.endswith(('/RpgSave.java','/LocalSaveStore.java','/CharacterFootprint.java','/CollisionService.java','/MovementService.java','/EnemyService.java','/RpgWorld.java','/RpgCollisionProfile.java','/SharedRpgEncounter.java')) or p.startswith('tools/updater/')]
for relative in protected:assert sha(ROOT/relative)==initial[relative],relative
assert subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT).decode().strip()==(CP/'head.txt').read_text().strip()
previous=json.loads((CP/'previous-packages-sha256.json').read_text())
for relative,digest in previous.items():assert sha(ROOT/relative)==digest,relative
headless=(RUN/'test-results.log').read_text(encoding='utf-8')
assert 'JUnit: 558 executed, 549 passed, 9 aborted, 0 failures, 0 skipped' in headless
assert 'JUnit: 558 executed, 558 passed, 0 aborted, 0 failures, 0 skipped' in (CP/'gui-tests.log').read_text(encoding='utf-8')
assert 'Ran 33 tests' in (CP/'python-tests.log').read_text(encoding='utf-8') and '\nOK\n' in (CP/'python-tests.log').read_text(encoding='utf-8')
shutil.copyfile(RUN/'test-results.log',CP/'headless-tests.log')
updater=json.loads((CP/'updater-regression/results.json').read_text(encoding='utf-8-sig'));assert updater['passed']==32
metrics=json.loads((CP/'integrated-probe-final/metrics.json').read_text())
for key in ['realRpgServer','productionRenderer','previewIsolated','beforeSaveUnchanged','draftSaveNotPublished','activationConfirmed','rollbackConfirmed','portalAfterPublication','pveHitAfterPublication','preinstalledAssetPreparedBothClients']:assert metrics[key],key
assert metrics['tcpClients']==2 and metrics['imageTransferBytes']==0 and metrics['historyEntries']==3
metadata=CP/'packages-20261025T063313Z.json';packages=json.loads(metadata.read_text())
smoke=sorted(CP.glob(metadata.stem+'-smoke-*/result.json'))[-1]
native=json.loads(smoke.read_text());assert native['application']==4 and native['publicationConfirmed'] and native['rollbackConfirmed'] and native['guestAdministrativeDenied'] and native['exitCodes']==[0,0,0]
assets=[];class_count=0
for package in packages:
    archive=ROOT/package['path'];assert sha(archive)==package['sha256']
    image=ROOT/package['image']
    # Both builds used the same javac flags. Verify the shipped main class bytes
    # against the exact classes on which the final JUnit suite ran.
    for file in (image/'app').glob('*.jar'):
        with zipfile.ZipFile(file) as jar:
            for name in jar.namelist():
                if not name.endswith('.class'):continue
                assert jar.read(name)==(RUN/'classes'/name).read_bytes(),name
                class_count+=1
    with zipfile.ZipFile(archive) as zipped:
        prefix=next(n for n in zipped.namelist() if n.endswith('/manifest-sha256.json')).rsplit('/',1)[0]+'/'
        manifest=json.loads(zipped.read(prefix+'manifest-sha256.json'))
        for entry in manifest:
            data=zipped.read(prefix+entry['path']);assert len(data)==entry['bytes'] and hashlib.sha256(data).hexdigest()==entry['sha256']
            if package['role']=='Anfitriao' and entry['path'].endswith('.png'):
                assert data[:8]==b'\x89PNG\r\n\x1a\n';width,height=struct.unpack('>II',data[16:24]);assets.append(dict(path=entry['path'],id=hashlib.sha256(entry['path'].encode()).hexdigest(),sha256=entry['sha256'],bytes=len(data),width=width,height=height))
(CP/'asset-inventory.json').write_text(json.dumps(assets,indent=2),encoding='utf-8')
updates=json.loads((ROOT/'dist/SpiritGate-Atualizavel-Radmin-0.180.0-entrega/integracao.json').read_text());assert updates['sourceSha256']==packages[0]['sha256'] and not updates['remoteUpdatesConfigured'] and not updates['published']
for package in updates['packages']:assert sha(Path(package['archive']))==package['sha256']
update_smoke=sorted((ROOT/'.task-checkpoints/radmin-atualizavel').glob('smoke-*/result.json'))[-1]
update_result=json.loads(update_smoke.read_text());assert update_result['sentinelSavesPreserved'] and update_result['legacyMigration'] and update_result['packagedClients']==2
subprocess.run(['git','diff','--check'],cwd=ROOT,check=True)
summary=dict(task='TASK-180',localTechnicalValidation=True,radminPhysicalHomologation=False,humanVisualApproval=False,
             tests=dict(javaGuiPassed=558,javaHeadlessPassed=549,javaHeadlessAborted=9,pythonPassed=33,updaterChecks=32,failures=0),
             protectedHashesVerified=len(protected),previousArchivesPreserved=len(previous),shippedClassesMatchingTestedClasses=class_count,
             sourceDeltas=changes,metrics=metrics,syntheticDemoProject='.task-checkpoints/TASK-180/live-publishing/integrated-project-final',
             realServer='RpgSessionWorld via LauncherSession',application=4,protocolEnvelope=1,saveSchemasUnchanged=[1,2],
             nativePackages=packages,nativeSmoke=dict(path=str(smoke.relative_to(ROOT)),**native),updatablePackages=updates['packages'],
             updatableSmoke=dict(path=str(update_smoke.relative_to(ROOT)),**update_result),
             limitations=['Physical two-computer Radmin test pending','Human visual approval pending','Preinstalled assets only, no image transfer','960x640 forest/village, root forest','No power-loss durability claim','Editor triggers/environment animation not published'],
             pushed=False,released=False,remoteFeedConfigured=False,personalSavesModified=False,task181Started=False)
(CP/'validation-summary.json').write_text(json.dumps(summary,indent=2),encoding='utf-8')
(CP/'final-status.txt').write_bytes(subprocess.check_output(['git','status','--short'],cwd=ROOT))
print(json.dumps({k:summary[k] for k in ['task','localTechnicalValidation','tests','protectedHashesVerified','previousArchivesPreserved','shippedClassesMatchingTestedClasses']},indent=2))
