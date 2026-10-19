"""Staging explicito de um personagem aprovado por SHA; sem alteracao da fonte."""
import argparse
import json
from pathlib import Path
import shutil
from validar_assets import inspect


def stage(root, manifest, output):
    config=json.loads(manifest.read_text(encoding='utf-8-sig'))
    if config.get('packageVersion') != 1 or len(config.get('assets',[])) != 1:
        raise ValueError('pacote visual v1 exige exatamente um personagem')
    asset_id=config.get('assetId','')
    import re
    if not re.fullmatch('[a-z][a-z0-9._-]{0,63}',asset_id):
        raise ValueError('assetId estavel invalido')
    entry=config['assets'][0]
    if entry.get('kind') != 'character' or not entry.get('review',{}).get('origin','').strip():
        raise ValueError('personagem e origem da revisao obrigatorios')
    result,_=inspect(root,entry)
    if result['status'] != 'VALIDADO':
        raise ValueError('arte nao aprovada ou invalida: '+ '; '.join(result['errors']+result['warnings']))
    if any(f.get('durationMs',100)!=100 or f.get('pivot',entry['pivot'])!=entry['pivot'] for f in entry['frames']):
        raise ValueError('pivot/duracao por frame incompativel com runtime')
    # Pacote duravel usa nomes fixos, sem caminhos externos e sem fontes pendentes.
    output.mkdir(parents=True,exist_ok=False)
    shutil.copyfile(root/entry['path'],output/'character.png')
    entry=dict(entry,path='character.png')
    config=dict(config,assets=[entry])
    (output/'review.json').write_text(json.dumps(config,ensure_ascii=False,indent=2),encoding='utf-8')
    values=dict(status='VALIDADO',packageVersion=1,assetId=asset_id,image='character.png',
                sha256=result['sha256'],approvedSha256=result['sha256'],
                reviewOrigin=entry['review']['origin'],frameSize=64,tileSize=32,
                pivotX=entry['pivot'][0],pivotY=entry['pivot'][1],offsetX=entry['offset'][0],offsetY=entry['offset'][1])
    for frame in entry['frames']:
        values[f"{frame['state']}.{frame['direction']}.{frame['index']}"]=','.join(str(frame[k]) for k in ('x','y','width','height'))
    # Properties: escapar controles, separadores e unicode evita reinterpretacao.
    def escape(value):
        return str(value).replace('\\','\\\\').replace('\n','\\n').replace('\r','\\r').replace('=','\\=').replace(':','\\:')
    (output/'character.properties').write_text('\n'.join(f'{k}={escape(v)}' for k,v in sorted(values.items()))+'\n',encoding='utf-8')


if __name__ == '__main__':
    p=argparse.ArgumentParser(description=__doc__)
    for name in ('root','manifest','output'): p.add_argument(name,type=Path)
    a=p.parse_args(); stage(a.root,a.manifest,a.output)
