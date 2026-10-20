"""Audita o ZIP experimental e seus hashes sem extrair nem incluir dados pessoais."""
from pathlib import Path,PurePosixPath
import sys,json,hashlib,zipfile,io

package=Path(sys.argv[1]).resolve()
manifest=json.loads(Path(str(package)+'.manifest.json').read_text(encoding='utf-8-sig'))
expected={'SpiritGateRPG/'+row['path']:row for row in manifest}
with zipfile.ZipFile(package) as archive:
    names={name.replace('\\','/'):name for name in archive.namelist() if not name.endswith('/')}
    assert set(names)==set(expected),(set(names)-set(expected),set(expected)-set(names))
    for name,rawname in names.items():
        assert not PurePosixPath(name).is_absolute() and '..' not in PurePosixPath(name).parts
        assert not any(p in name.lower().split('/') for p in ('saves','data','.git','.aws','.codex'))
        data=archive.read(rawname);assert len(data)==expected[name]['bytes']
        assert hashlib.sha256(data).hexdigest()==expected[name]['sha256']
    pngs={name.split('/')[-1]:hashlib.sha256(archive.read(raw)).hexdigest() for name,raw in names.items() if name.endswith('.png')}
    metadata={}
    for line in Path('server/src/main/resources/rpg/experimental-player.properties').read_text().splitlines():
        if line and not line.startswith('#'):
            values=line.split('=',1)[1].split(',');metadata[values[0]]=values[1]
    assert len(pngs)==20 and pngs==metadata
    assert 'SpiritGateRPG/runtime/bin/java.exe' in names
    assert 'SpiritGateRPG/SpiritGateRPG.exe' in names and 'SpiritGateRPG/Abrir-Geometrico.cmd' in names
    with zipfile.ZipFile(io.BytesIO(archive.read(names['SpiritGateRPG/app/spirit-gate-rpg.jar']))) as jar:
        assert not any('multiplayer/' in n or 'DatabaseManager' in n or n.endswith('Test.class') for n in jar.namelist())
        assert 'com/projetok/rpg/RpgSecondaryAbility.class' in jar.namelist()
sha=hashlib.sha256(package.read_bytes()).hexdigest()
assert sha==Path(str(package)+'.sha256').read_text().strip()
report={'package':str(package),'bytes':package.stat().st_size,'sha256':sha,'filesVerified':len(names),'pngsVerified':20,'personalSavesIncluded':False,'javaBundled':True,'geometryFallbackIncluded':True,'humanApproved':False}
print(json.dumps(report,indent=2))
Path('.task-checkpoints/TASK-090/corujao-i/package-audit.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
