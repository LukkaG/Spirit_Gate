"""Build the isolated Java 17 browser client; never reads personal saves."""
from pathlib import Path
import hashlib,json,shutil,subprocess

root=Path(__file__).resolve().parent
classes=root/'build/classes';classes.mkdir(parents=True,exist_ok=True)
sources=sorted((root/'game-src').rglob('*.java'))
args=root/'build/sources.txt';args.write_text('\n'.join('"'+str(p).replace('\\','/')+'"' for p in sources),encoding='utf-8')
subprocess.run(['javac','--release','17','-encoding','UTF-8','-d',str(classes),'@'+str(args)],check=True)
shutil.copytree(root/'game-resources',classes,dirs_exist_ok=True)
subprocess.run(['jar','--create','--file',str(root/'dist/spirit-gate-web.jar'),'--main-class','com.projetok.prototype.rpg.RpgLauncher','-C',str(classes),'.'],check=True)
files=[p for p in (root/'dist').rglob('*') if p.is_file() and p.name!='build-info.json']
(root/'dist/build-info.json').write_text(json.dumps({'build':'sprites-browser-079','java':17,'defaultScale':0.4,'defaultFrameMillis':220,'files':{p.relative_to(root/'dist').as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in files}},indent=2),encoding='utf-8')
print('Isolated browser JAR built:',(root/'dist/spirit-gate-web.jar').stat().st_size)
