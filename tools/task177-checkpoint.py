"""Finalize auditable deltas against the user's dirty TASK-176 baseline, without staging."""
from pathlib import Path
import subprocess
import hashlib
import difflib
import json

ROOT=Path(__file__).resolve().parents[1]
CP=ROOT/'.task-checkpoints/TASK-177/network-collision'
before=json.loads((CP/'initial-sha256.json').read_text())
initial=CP/'initial-source'
initial.mkdir(exist_ok=True)
status=(CP/'initial-status.txt').read_text().splitlines()
for line in status:
    if line.startswith(' M '):
        relative=line[3:]
        output=initial/relative
        output.parent.mkdir(parents=True,exist_ok=True)
        output.write_bytes(subprocess.check_output(['git','show','HEAD:'+relative],cwd=ROOT))
subprocess.run(['git','apply','--directory='+initial.relative_to(ROOT).as_posix(),str(CP/'initial.diff')],cwd=ROOT,check=True)
deltas=CP/'file-diffs'
deltas.mkdir(exist_ok=True)
changed=[]
for path in sorted((ROOT/'server/src').rglob('*')):
    if not path.is_file():continue
    relative=path.relative_to(ROOT).as_posix()
    old_hash=before.get(str(path.relative_to(ROOT))) or before.get(relative)
    digest=hashlib.sha256(path.read_bytes()).hexdigest()
    if digest==old_hash:continue
    baseline=initial/relative
    if not baseline.exists():
        result=subprocess.run(['git','show','HEAD:'+relative],cwd=ROOT,capture_output=True)
        if result.returncode==0:
            baseline.parent.mkdir(parents=True,exist_ok=True)
            baseline.write_bytes(result.stdout)
    old=baseline.read_text(encoding='utf-8') if baseline.exists() else ''
    if old_hash and hashlib.sha256(baseline.read_bytes()).hexdigest()!=old_hash:
        # Git may normalize CRLF; compare normalized text before claiming a content mismatch.
        normalized=baseline.read_bytes().replace(b'\n',b'\r\n')
        if hashlib.sha256(normalized).hexdigest()!=old_hash:
            raise RuntimeError('Baseline hash mismatch: '+relative)
    new=path.read_text(encoding='utf-8')
    delta=''.join(difflib.unified_diff(old.splitlines(True),new.splitlines(True),fromfile='initial/'+relative,tofile='final/'+relative))
    (deltas/(relative.replace('/','__')+'.diff')).write_text(delta,encoding='utf-8')
    changed.append(dict(path=relative,initialSha256=old_hash,finalSha256=digest))
assets=[]
for path in (ROOT/'assets/dev/player-base').iterdir():
    if path.is_file():
        relative=path.relative_to(ROOT)
        expected=before.get(str(relative)) or before.get(relative.as_posix())
        digest=hashlib.sha256(path.read_bytes()).hexdigest()
        if expected!=digest:raise RuntimeError('Original asset changed: '+str(relative))
        assets.append(dict(path=relative.as_posix(),sha256=digest))
(CP/'final-deltas.json').write_text(json.dumps(changed,indent=2),encoding='utf-8')
extra=[]
for path in sorted(list((ROOT/'docs').glob('TASK-177*.md'))+list((ROOT/'tools').glob('*177*'))):
    if not path.is_file():continue
    relative=path.relative_to(ROOT).as_posix()
    content=path.read_text(encoding='utf-8')
    delta=''.join(difflib.unified_diff([],content.splitlines(True),fromfile='initial/'+relative,tofile='final/'+relative))
    (deltas/(relative.replace('/','__')+'.diff')).write_text(delta,encoding='utf-8')
    extra.append(dict(path=relative,sha256=hashlib.sha256(path.read_bytes()).hexdigest()))
(CP/'task-docs-tools-sha256.json').write_text(json.dumps(extra,indent=2),encoding='utf-8')
(CP/'original-sprites-verified.json').write_text(json.dumps(assets,indent=2),encoding='utf-8')
(CP/'final-status.txt').write_bytes(subprocess.check_output(['git','status','--short'],cwd=ROOT))
(CP/'final-working-tree.diff').write_bytes(subprocess.check_output(['git','diff','--binary'],cwd=ROOT))
print('Source deltas:',len(changed),'original assets verified:',len(assets))
