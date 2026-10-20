"""Auditoria real de pacote World Studio; sem extrair nem publicar conteudo."""
from pathlib import Path,PurePosixPath
import hashlib,json,zipfile,sys,io

package=Path(sys.argv[1]).resolve()
manifest=json.loads(Path(str(package)+'.manifest.json').read_text(encoding='utf-8-sig'))
expected={'SpiritGateWorldStudio/'+row['path']:row for row in manifest}
with zipfile.ZipFile(package) as archive:
    names={n.replace('\\','/'):n for n in archive.namelist() if not n.endswith('/')}
    assert set(names)==set(expected)
    for name,raw in names.items():
        assert not PurePosixPath(name).is_absolute() and '..' not in PurePosixPath(name).parts
        assert not any(p in name.lower().split('/') for p in ('saves','studio-projects','.git','.aws','.codex'))
        data=archive.read(raw);assert len(data)==expected[name]['bytes'];assert hashlib.sha256(data).hexdigest()==expected[name]['sha256']
    assert 'SpiritGateWorldStudio/runtime/bin/java.exe' in names and 'SpiritGateWorldStudio/SpiritGateWorldStudio.exe' in names
    assert 'SpiritGateWorldStudio/assets/maps/studio_tutorial.sgmap' in names
    with zipfile.ZipFile(io.BytesIO(archive.read(names['SpiritGateWorldStudio/app/world-studio.jar']))) as jar:
        assert 'com/projetok/studio/WorldStudio.class' in jar.namelist()
        assert not any('DatabaseManager' in n or 'multiplayer/' in n or n.endswith('Test.class') for n in jar.namelist())
        for n in jar.namelist():
            if n.endswith('.class'):assert int.from_bytes(jar.read(n)[6:8],'big')==61
sha=hashlib.sha256(package.read_bytes()).hexdigest();assert sha==Path(str(package)+'.sha256').read_text().strip()
report={'package':str(package),'bytes':package.stat().st_size,'sha256':sha,'files':len(names),'pngs':sum(n.endswith('.png') for n in names),'javaBundled':True,'personalDataIncluded':False,'databaseOrCoopRequired':False}
print(json.dumps(report,indent=2))
if len(sys.argv)>2:Path(sys.argv[2]).write_text(json.dumps(report,indent=2),encoding='utf-8')
