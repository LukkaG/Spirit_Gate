"""Audita o pacote RPG sem executar seu conteudo e sem acessar rede."""
from pathlib import Path, PurePosixPath
import argparse
import hashlib
import io
import json
import zipfile

def audit(path, kind='rpg'):
    checks = {}
    with zipfile.ZipFile(path) as archive:
        entries = archive.infolist()
        names = [e.filename for e in entries]
        assert len(names) == len(set(names)) and len(names) < 2000
        assert sum(e.file_size for e in entries) < 512 * 1024 * 1024
        for entry in entries:
            p = PurePosixPath(entry.filename)
            assert not p.is_absolute() and '..' not in p.parts and '\\' not in entry.filename
            assert entry.file_size < 128 * 1024 * 1024
            assert not entry.filename.lower().endswith(('.env', '.pem', '.key', '.sql', '.py'))
        checks['zipIntegrity'] = archive.testzip() is None
        jar_name = next(n for n in names if n.endswith('/app/spirit-gate-' + kind + '.jar'))
        with zipfile.ZipFile(io.BytesIO(archive.read(jar_name))) as jar:
            entrypoint = 'com.projetok.prototype.rpg.RpgLauncher' if kind == 'rpg' else 'com.projetok.multiplayer.LocalSimulationServer'
            assert 'Main-Class: ' + entrypoint in jar.read('META-INF/MANIFEST.MF').decode()
            classes = 0
            for name in jar.namelist():
                prefixes = ('META-INF/', 'com/projetok/domain/', 'com/projetok/prototype/') + (('com/projetok/rpg/', 'rpg/') if kind == 'rpg' else ('com/projetok/multiplayer/',))
                assert name.startswith(prefixes)
                assert not any(x in name for x in ('DatabaseManager', 'KServer', 'Kclient', 'Test.class'))
                if name.endswith('.class'):
                    data = jar.read(name)
                    assert data[:4] == b'\xca\xfe\xba\xbe' and int.from_bytes(data[6:8], 'big') <= 61
                    classes += 1
            checks['productionClasses'] = classes
            checks['contentIncluded'] = ('rpg/content.properties' if kind == 'rpg' else 'com/projetok/multiplayer/Protocol.class') in jar.namelist()
        manifest = json.loads(Path(str(path) + '.manifest.json').read_text(encoding='utf-8-sig'))
        root = jar_name.split('/')[0] + '/'
        actual = {n[len(root):] for n in names if not n.endswith('/')}
        assert actual == {item['path'] for item in manifest}
        for item in manifest:
            data = archive.read(root + item['path'])
            assert len(data) == item['bytes'] and hashlib.sha256(data).hexdigest() == item['sha256']
        checks['manifestHashes'] = True
        checks['javaIntegrated'] = any(n.endswith('/runtime/release') for n in names)
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    assert Path(str(path) + '.sha256').read_text().strip() == digest
    return {'schemaVersion': 1, 'package': path.name, 'sha256': digest, 'automatedApproved': True,
            'humanApproved': False, 'checks': checks}

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('zip', type=Path)
    parser.add_argument('report', type=Path)
    parser.add_argument('--kind', choices=['rpg', 'coop'], default='rpg')
    args = parser.parse_args()
    report = audit(args.zip, args.kind)
    args.report.write_text(json.dumps(report, indent=2), encoding='utf-8')
    print(json.dumps(report, indent=2))
