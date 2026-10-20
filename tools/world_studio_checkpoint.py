"""Checkpoints locais recuperaveis da rodada II; nunca inclui saves nos ZIPs."""
from pathlib import Path
import sys, json, hashlib, zipfile, difflib, subprocess

ROOT = Path(__file__).resolve().parent.parent

def files():
    for folder in ('server/src', 'assets', 'tools', 'docs'):
        for p in (ROOT/folder).rglob('*'):
            if p.is_file() and '__pycache__' not in p.parts:
                yield p
    yield from ROOT.glob('*.cmd')

def run(task, phase):
    destination=ROOT/'.task-checkpoints'/('TASK-'+task)/'corujao-ii'
    destination.mkdir(parents=True,exist_ok=True)
    manifest=destination/(phase+'.json')
    if manifest.exists():raise ValueError('Checkpoint existente; nao sobrescrever')
    hashes={}
    with zipfile.ZipFile(destination/(phase+'.zip'),'w',zipfile.ZIP_DEFLATED) as archive:
        for p in files():
            name=p.relative_to(ROOT).as_posix(); data=p.read_bytes()
            hashes[name]=hashlib.sha256(data).hexdigest();archive.writestr(name,data)
    saves={p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in (ROOT/'saves').rglob('*') if p.is_file()}
    manifest.write_text(json.dumps({'files':hashes,'saves':saves,'git':subprocess.run(['git','status','--short'],cwd=ROOT,capture_output=True,text=True).stdout},indent=2),encoding='utf-8')
    if phase=='after':
        before=json.loads((destination/'before.json').read_text())
        changed=[n for n,h in hashes.items() if before['files'].get(n)!=h]
        removed=sorted(set(before['files'])-set(hashes))
        with zipfile.ZipFile(destination/'before.zip') as old,zipfile.ZipFile(destination/'after.zip') as new:
            diff=[]
            for n in changed+removed:
                if n.endswith(('.java','.py','.md','.properties','.cmd','.ps1')):
                    a=old.read(n).decode('utf-8',errors='replace').splitlines(True) if n in old.namelist() else []
                    b=new.read(n).decode('utf-8',errors='replace').splitlines(True) if n in new.namelist() else []
                    diff.extend(difflib.unified_diff(a,b,fromfile='before/'+n,tofile='after/'+n))
            (destination/'isolated.diff').write_text(''.join(diff),encoding='utf-8')
        (destination/'preservation.json').write_text(json.dumps({'changed':changed,'removed':removed,'savesUnchanged':before['saves']==saves},indent=2),encoding='utf-8')
    print(destination,phase,len(hashes))

if __name__=='__main__':run(*sys.argv[1:])
