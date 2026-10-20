from pathlib import Path
import hashlib,json,zipfile

root=Path(__file__).resolve().parent
metadata={}
for line in (root/'game-resources/rpg/experimental-player.properties').read_text().splitlines():
    if line and not line.startswith('#'):
        key,value=line.split('=',1);metadata[key]=value.split(',')
assert len(metadata)==12
for row in metadata.values():
    assert hashlib.sha256((root/'dist/assets/dev/player-base'/row[0]).read_bytes()).hexdigest()==row[1]
with zipfile.ZipFile(root/'dist/spirit-gate-web.jar') as archive:
    names=archive.namelist()
    assert 'com/projetok/prototype/rpg/BrowserLauncher.class' in names
    assert not any('DatabaseManager' in name or 'multiplayer/' in name or name.endswith('Test.class') for name in names)
    for name in names:
        if name.endswith('.class'):assert int.from_bytes(archive.read(name)[6:8],'big')==61
snapshot=json.loads((root/'snapshot.json').read_text())
parent=root.parent
desktopVerified=0
for name,sha in snapshot['productionSnapshot'].items():
    source=parent/name
    if source.exists():assert hashlib.sha256(source.read_bytes()).hexdigest()==sha;desktopVerified+=1
print(json.dumps({'originalPngsVerified':12,'jarJavaVersion':17,'desktopProductionFilesUnchanged':desktopVerified}))
