"""Autoria offline; validação autoritativa usa LocalArenaMap compilado, sem outro schema."""
import argparse
from pathlib import Path
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]

def safe_path(root, name):
    path = (root / name).resolve()
    if not path.is_relative_to(root.resolve()) or path == root.resolve():
        raise ValueError('Caminho fora da raiz autorizada')
    return path

def validate(path):
    if path.stat().st_size > 1_000_000:
        raise ValueError('Mapa excessivo')
    java = shutil.which('java')
    if not java or not (ROOT / 'server/target/classes/com/projetok/prototype/LocalArenaMap.class').exists():
        raise RuntimeError('Compile antes: python tools/qualidade.py --offline')
    result = subprocess.run([java, '-cp', str(ROOT / 'server/target/classes'),
                             'com.projetok.prototype.LocalArenaMap', str(path)],
                            capture_output=True, text=True, errors='replace', timeout=30)
    if result.returncode:
        raise ValueError(result.stderr.strip())
    return result.stdout.strip()

def create(root, name, width=1920, height=1280):
    path = safe_path(root, name)
    if not 960 <= width <= 8192 or not 640 <= height <= 8192:
        raise ValueError('Dimensões fora dos limites')
    values = dict(schemaVersion=1 if (width,height)==(960,640) else 2,
                  width=width,height=height,spawnX=135,spawnY=340)
    values.update({'physical.count':1,'visual.count':2,
                   'physical.0.id':'boundary','physical.0.x':0,'physical.0.y':0,
                   'physical.0.width':width,'physical.0.height':height,
                   'physical.0.category':'WORLD_BOUNDARY'})
    for i,layer in enumerate((0,6)):
        for key,value in dict(id=f'synthetic-{i}',x=32+i*128,y=32,width=64,height=64,layer=layer,rgb='245342').items():
            values[f'visual.{i}.{key}']=value
    text = '# CENARIO SINTETICO - SEM ARTE APROVADA\n' + ''.join(f'{k}={v}\n' for k,v in sorted(values.items()))
    path.parent.mkdir(parents=True,exist_ok=True)
    with path.open('x',encoding='utf-8') as output:
        output.write(text)
    return path

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root',type=Path,default=ROOT/'docs/mapas')
    sub=parser.add_subparsers(dest='command',required=True)
    c=sub.add_parser('create');c.add_argument('name');c.add_argument('--width',type=int,default=1920);c.add_argument('--height',type=int,default=1280)
    v=sub.add_parser('validate');v.add_argument('name')
    args=parser.parse_args()
    try:
        path=create(args.root,args.name,args.width,args.height) if args.command=='create' else safe_path(args.root,args.name)
        print(validate(path))
    except (OSError,ValueError,RuntimeError,subprocess.TimeoutExpired) as error:
        parser.exit(1,str(error)+'\n')

if __name__=='__main__':
    main()
