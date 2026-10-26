"""Finalize TASK-176 documentation using already-tested native binaries; preserve all ZIPs."""
from pathlib import Path
from datetime import datetime, timezone
import hashlib
import json
import sys
import zipfile
ROOT=Path(__file__).resolve().parents[1]
def main(metadata):
    packages=json.loads(Path(metadata).read_text(encoding='utf-8'))
    stamp=datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    build=ROOT/'server/target'/('task-176-delivery-'+stamp);build.mkdir()
    results=[]
    for package in packages:
        image=build/package['role'];image.mkdir()
        original=ROOT/package['path']
        if hashlib.sha256(original.read_bytes()).hexdigest()!=package['sha256']:raise ValueError('Source hash mismatch')
        with zipfile.ZipFile(original) as archive:
            prefix=next(n for n in archive.namelist() if n.endswith('/manifest-sha256.json')).rsplit('/',1)[0]+'/'
            for entry in archive.infolist():
                relative=entry.filename[len(prefix):]
                if not relative:continue
                target=(image/relative).resolve()
                if not target.is_relative_to(image.resolve()):raise ValueError('Unsafe ZIP path')
                target.parent.mkdir(parents=True,exist_ok=True)
                target.write_bytes(archive.read(entry))
        for doc in (ROOT/'docs').glob('TASK-176*.md'):(image/'docs'/doc.name).write_bytes(doc.read_bytes())
        (image/'LEIA-ME.txt').write_text('Spirit Gate TASK-176 - privado experimental\nExtraia tudo e abra SpiritGate.exe. Java integrado.\nLeia docs/TASK-176-LAUNCHER-RADMIN.md. Radmin remoto pendente de homologacao fisica.\n',encoding='utf-8')
        files=sorted(p for p in image.rglob('*') if p.is_file() and p.name!='manifest-sha256.json')
        manifest=[{'path':p.relative_to(image).as_posix(),'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in files]
        (image/'manifest-sha256.json').write_text(json.dumps(manifest,indent=2),encoding='utf-8')
        output=ROOT/'dist'/('SpiritGate-TASK176-0.176.0-dev-'+package['role']+'-'+stamp+'.zip')
        with zipfile.ZipFile(output,'x',zipfile.ZIP_DEFLATED) as archive:
            for p in files+[image/'manifest-sha256.json']:archive.write(p,'SpiritGate-'+package['role']+'/'+p.relative_to(image).as_posix())
        digest=hashlib.sha256(output.read_bytes()).hexdigest();output.with_suffix('.zip.sha256').write_text(digest+'\n',encoding='ascii')
        results.append({'role':package['role'],'path':str(output.relative_to(ROOT)),'image':str(image.relative_to(ROOT)),'sha256':digest,'files':len(manifest)+1,'binarySource':package['path']})
    checkpoint=ROOT/'.task-checkpoints/TASK-176/launcher-radmin'
    (checkpoint/('packages-'+stamp+'.json')).write_text(json.dumps(results,indent=2),encoding='utf-8')
    (checkpoint/'packages-final.json').write_text(json.dumps(results,indent=2),encoding='utf-8')
    print(json.dumps(results,indent=2))
if __name__=='__main__':main(sys.argv[1])
