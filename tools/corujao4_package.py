"""Finaliza pacote privado novo, com exemplos, documentação, inventário e hashes."""
from pathlib import Path
import hashlib
import json
import shutil
import sys
import zipfile

ROOT=Path(__file__).resolve().parents[1]


def package(image, task='149'):
    image=Path(image).resolve()
    if not image.is_relative_to(ROOT/'server/target') or not (image/'SpiritGateWorldStudio.exe').is_file():
        raise ValueError('Imagem fora do build privado autorizado')
    docs=image/'docs';docs.mkdir(exist_ok=True)
    for source in (ROOT/'docs').glob('CORUJAO-IV-*.md'):
        shutil.copyfile(source,docs/source.name)
    for number in range(134,int(task)+1):
        for source in (ROOT/'docs').glob(f'TASK-{number}*.md'):
            shutil.copyfile(source,docs/source.name)
    examples=image/'examples/corujao-iv';examples.mkdir(parents=True,exist_ok=True)
    demo=ROOT/'.task-checkpoints/TASK-149/corujao-iv/demo'
    for source in demo.iterdir():
        if source.is_file() and source.suffix in ('.sgmap','.sgquest','.json','.png'):
            shutil.copyfile(source,examples/source.name)
    if not (examples/'bundle').exists():
        shutil.copytree(demo/'bundle',examples/'bundle')
    if int(task)>=159:
        evidence=image/'examples/multiplayer';evidence.mkdir(parents=True,exist_ok=True)
        for folder in ('TASK-157/corujao-iv/preview','TASK-158/corujao-iv/two-clients','TASK-159/corujao-iv/native-smoke-final'):
            source=ROOT/'.task-checkpoints'/folder
            if source.is_dir():
                target=evidence/source.parts[-3];target.mkdir(exist_ok=True)
                for p in source.iterdir():
                    if p.is_file() and p.suffix in ('.png','.json'):shutil.copyfile(p,target/p.name)
        (image/'Iniciar-multiplayer-local.cmd').write_text('@echo off\ncd /d "%~dp0"\nstart "Servidor RPG local" "SpiritGateRpgServer.exe"\ntimeout /t 2 /nobreak >nul\nstart "Cliente A" "SpiritGateRpgClientA.exe"\nstart "Cliente B" "SpiritGateRpgClientB.exe"\n',encoding='ascii')
        (image/'LEIA-ME.txt').write_text('Spirit Gate - Corujao IV - experimental privado\nExtraia todo o ZIP. Java integrado. Abra SpiritGateWorldStudio.exe para editar ou Jogar-RPG-offline.cmd para jogar offline.\nMultiplayer localhost: abra Iniciar-multiplayer-local.cmd, que inicia servidor e dois clientes em 127.0.0.1:54557. Alternativa: SpiritGateRpgServer.exe, depois SpiritGateRpgClientA.exe e SpiritGateRpgClientB.exe. Aguarde a janela Conectando. WASD movimenta; seletores alteram cabelo e roupa temporarios.\nFeche os dois clientes e a janela de console do servidor ao terminar. Porta ocupada: encerre somente sua instancia anterior ou configure outra porta explicitamente nos tres executaveis.\nMovimento autoritativo e aparencia em camadas; combate RPG compartilhado indisponivel. LAN fisica pendente. Sem contas, persistencia online ou seguranca de producao homologada. Nao expor a internet.\nQuests autoradas executam em memoria; mapas de runtime limitados a 960x640. Saves pessoais nao incluidos. Sprites temporarias sem aprovacao artistica.\nRoteiros e limitacoes: docs/CORUJAO-IV-HOMOLOGACAO-MULTIPLAYER.md e docs/CORUJAO-IV-HOMOLOGACAO.md. Exemplos e capturas em examples/.\n',encoding='utf-8')
    (image/'Jogar-RPG-offline.cmd').write_text('@echo off\ncd /d "%~dp0"\n"runtime\\bin\\java.exe" -cp "app\\world-studio.jar" com.projetok.prototype.rpg.RpgLauncher\n',encoding='ascii')
    files=sorted(p for p in image.rglob('*') if p.is_file())
    forbidden=[str(p.relative_to(image)) for p in files if 'saves' in p.relative_to(image).parts or p.suffix in ('.bak','.tmp')]
    if forbidden:raise ValueError(f'Dados não autorizados no pacote: {forbidden}')
    manifest=[{'path':p.relative_to(image).as_posix(),'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in files]
    output=ROOT/'dist'/('SpiritGate-CorujaoIV-'+image.parents[1].name+'.zip')
    if output.exists():raise ValueError('Não substituir pacote anterior')
    with zipfile.ZipFile(output,'x',zipfile.ZIP_DEFLATED) as archive:
        for p in files:archive.write(p,'SpiritGateCorujaoIV/'+p.relative_to(image).as_posix())
    output.with_suffix('.zip.manifest.json').write_text(json.dumps(manifest,indent=2),encoding='utf-8')
    digest=hashlib.sha256(output.read_bytes()).hexdigest();output.with_suffix('.zip.sha256').write_text(digest+'\n',encoding='ascii')
    (ROOT/f'.task-checkpoints/TASK-{task}/corujao-iv/package.json').write_text(json.dumps({'path':str(output),'sha256':digest,'files':len(files),'personalSavesIncluded':False},indent=2),encoding='utf-8')
    print(output);print(digest)


if __name__=='__main__':package(sys.argv[1],sys.argv[2] if len(sys.argv)>2 else '149')
