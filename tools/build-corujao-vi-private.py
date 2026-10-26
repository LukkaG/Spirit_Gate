"""Pacotes privados novos no checkpoint 198; sem feed, publicação ou alteração de dist."""
from pathlib import Path
import runpy, shutil, json, zipfile
root=Path.cwd()
h=runpy.run_path(str(root/'.task-checkpoints/corujao-vi-session-20261025T221534Z-be629798/session.py'))
out=h['checkpoint']('198')
build=out/'build';build.mkdir()
classes=build/'classes';classes.mkdir()
sources=sorted((root/'server/src/main/java').rglob('*.java'))
args=build/'sources.args'
args.write_text('\n'.join('"'+p.as_posix()+'"' for p in sources),encoding='utf-8')
h['write'](out/'compiled-sources.json',h['sources']())
h['command'](out,'compile',['javac','--release','17','-encoding','UTF-8','-d',str(classes),'@'+str(args)],root,180)
shutil.copytree(root/'server/src/main/resources',classes,dirs_exist_ok=True)
jar=build/'spiritgate.jar'
h['command'](out,'jar',['jar','--create','--file',str(jar),'-C',str(classes),'com/projetok','-C',str(classes),'rpg'],root)
runtime=build/'runtime'
h['command'](out,'jlink',['jlink','--add-modules','java.base,java.desktop,java.logging,jdk.random','--strip-debug','--no-man-pages','--no-header-files','--compress=2','--output',str(runtime)],root,180)
packages=out/'packages';packages.mkdir()
results=[]
for role in ('Anfitriao','Convidado'):
    image=build/role;image.mkdir();app=image/'app';app.mkdir();shutil.copyfile(jar,app/'spiritgate.jar')
    dest=build/('native-'+role)
    command=['jpackage','--type','app-image','--dest',str(dest),'--name','SpiritGate','--input',str(app),'--main-jar','spiritgate.jar','--main-class','com.projetok.prototype.rpg.RpgLauncher','--runtime-image',str(runtime),'--app-version','0.198.0','--vendor','Spirit Gate']
    if role=='Convidado':command+=['--java-options','-Dspiritgate.guest=true']
    h['command'](out,'jpackage-'+role,command,root,180)
    shutil.copytree(dest/'SpiritGate',image,dirs_exist_ok=True)
    for relative in ('assets/dev/player-base','assets/dev/generated/corujao-iv-v1'):
        shutil.copytree(root/relative,image/relative)
    docs=image/'docs';docs.mkdir()
    for name in ('TASK-180-LIVE-PUBLISHING.md','TASK-180-HOMOLOGACAO.md','TASK-184-RADMIN-ROTEIRO.md','TASK-197-RELATORIO-FINAL.md'):
        shutil.copyfile(root/'docs'/name,docs/name)
    (image/'LEIA-ME.txt').write_text('Spirit Gate Corujao VI — privado experimental, '+role+'\nExtraia tudo e abra SpiritGate.exe. Java 17 integrado. Atualize ambos os papeis juntos.\nStudio autorizado apenas no anfitriao local. Leia docs/TASK-180-LIVE-PUBLISHING.md.\nRadmin fisico PENDENTE: seguir docs/TASK-184-RADMIN-ROTEIRO.md.\nSem contas publicas ou persistencia sintetica integrada ao launcher. Nenhum save pessoal incluido.\n',encoding='utf-8')
    files=sorted(p for p in image.rglob('*') if p.is_file())
    allowed_roots={'app','runtime','assets','docs'}
    allowed_top={'SpiritGate.exe','LEIA-ME.txt'}
    forbidden={'.git','.env','saves','studio-projects','src','tools','target','credentials','online-synthetic'}
    for path in files:
        parts=path.relative_to(image).parts
        if (len(parts)>1 and parts[0] not in allowed_roots) or (len(parts)==1 and parts[0] not in allowed_top):raise RuntimeError('Fora da allowlist: '+str(path))
        if any(part.lower() in forbidden or part.lower().startswith('online-synthetic-') for part in parts):raise RuntimeError('Conteudo privado: '+str(path))
        asset_manifests={'assets/dev/generated/corujao-iv-v1/manifest.properties','runtime/conf/logging.properties','runtime/conf/net.properties','runtime/conf/sound.properties','runtime/lib/psfontj2d.properties'}
        if path.suffix.lower() in {'.java','.py','.ps1','.pdb','.jfr','.log'} or (path.suffix.lower()=='.properties' and path.relative_to(image).as_posix() not in asset_manifests):raise RuntimeError('Fonte/ferramenta solta no pacote: '+str(path))
    with zipfile.ZipFile(jar) as archive:
        names=archive.namelist()
        if any('Task194' in n or 'Task195' in n or 'Task196' in n or n.endswith('.java') for n in names):raise RuntimeError('Teste/fonte no jar')
    manifest=[dict(path=p.relative_to(image).as_posix(),bytes=p.stat().st_size,sha256=h['sha'](p)) for p in files]
    h['write'](image/'manifest-sha256.json',manifest)
    archive=packages/('SpiritGate-CorujaoVI-TASK198-0.198.0-dev-'+role+'-'+out.name+'.zip')
    with zipfile.ZipFile(archive,'x',zipfile.ZIP_DEFLATED) as zipped:
        for path in files+[image/'manifest-sha256.json']:zipped.write(path,'SpiritGateCorujaoVI-'+role+'/'+path.relative_to(image).as_posix())
    digest=h['sha'](archive)
    with archive.with_suffix('.zip.sha256').open('x',encoding='ascii') as stream:stream.write(digest+'\n')
    results.append(dict(role=role,path=str(archive.relative_to(root)),image=str(image.relative_to(root)),sha256=digest,files=len(files)+1,jar_sha256=h['sha'](jar)))
h['write'](out/'packages.json',results)
print(json.dumps(results,indent=2),flush=True)
