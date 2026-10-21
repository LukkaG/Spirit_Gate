"""Verifica ZIP/manifesto, Java integrado e assets sem sobrescrever auditorias antigas."""
import hashlib
import io
import json
from pathlib import Path, PurePosixPath
import sys
import zipfile

package = Path(sys.argv[1])
destination = Path(sys.argv[2])
manifest = json.loads(Path(str(package)+'.manifest.json').read_text(encoding='utf-8-sig'))
expected = {'SpiritGateRPG/'+row['path']: row for row in manifest}
with zipfile.ZipFile(package) as archive:
    names = {name.replace('\\','/'): name for name in archive.namelist() if not name.endswith('/')}
    assert set(names) == set(expected)
    for name, raw in names.items():
        path = PurePosixPath(name)
        assert not path.is_absolute() and '..' not in path.parts
        assert not any(part.lower() in {'saves','data','.git','.aws','.codex'} for part in path.parts)
        content = archive.read(raw)
        assert len(content) == expected[name]['bytes']
        assert hashlib.sha256(content).hexdigest() == expected[name]['sha256']
    assert 'SpiritGateRPG/runtime/bin/java.exe' in names
    assert 'SpiritGateRPG/SpiritGateRPG.exe' in names
    source = Path('server/src/main/resources')
    with zipfile.ZipFile(io.BytesIO(archive.read(names['SpiritGateRPG/app/spirit-gate-rpg.jar']))) as jar:
        assert not any(n.endswith('Test.class') or n.startswith('com/projetok/multiplayer/') for n in jar.namelist())
        for file in (source/'rpg').rglob('*'):
            if file.is_file():assert jar.read(file.relative_to(source).as_posix()) == file.read_bytes()
    for file in Path('assets/dev/player-base').glob('*.png'):
        assert archive.read(names['SpiritGateRPG/assets/dev/player-base/'+file.name]) == file.read_bytes()
sha = hashlib.sha256(package.read_bytes()).hexdigest()
assert sha == Path(str(package)+'.sha256').read_text().strip()
destination.write_text(json.dumps({'package':str(package),'bytes':package.stat().st_size,'sha256':sha,'filesVerified':len(names),'resourcesMatchSource':True,'javaBundled':True,'personalSavesIncluded':False,'published':False},indent=2),encoding='utf-8')
print('PASS', len(names), sha)
