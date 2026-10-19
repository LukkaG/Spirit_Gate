"""Autoria/revisao apenas; reutiliza o contrato e inspetor TASK-013. Nunca aprova arte."""
import argparse
import copy
import hashlib
import html
import json
import os
from pathlib import Path
from urllib.parse import quote
from separar_sprites import write_png
from validar_assets import CONTRACT, DIRECTIONS, inspect, read_png


def frames():
    result = []
    for action, count in list(CONTRACT.items()) + [('KO', 3)]:
        for direction in (('FRONT',) if action == 'KO' else DIRECTIONS):
            for index in range(count):
                slot = len(result)
                result.append(dict(state=action, direction=direction, index=index,
                                   x=(slot % 8)*64, y=(slot // 8)*64, width=64, height=64,
                                   durationMs=100, pivot=[32, 48]))
    return result


def animation_preview(entry, src):
    """Prévia CSS por estado/direção, com recortes originais e passos de 100 ms."""
    groups = {}
    for frame in entry.get('frames', []):
        groups.setdefault((frame['state'], frame['direction']), []).append(frame)
    output = []
    prefix = hashlib.sha256(json.dumps(entry.get('frames',[]),sort_keys=True).encode()).hexdigest()[:12]
    for index, ((state, direction), sequence) in enumerate(groups.items()):
        sequence = sorted(sequence, key=lambda f:f['index'])
        steps = ''.join(f"{i*100/len(sequence):.6f}%{{background-position:-{f['x']}px -{f['y']}px}}"
                        for i,f in enumerate(sequence))
        safe = html.escape(f'{state}.{direction}')
        output.append(f'<figure><figcaption>{safe}</figcaption><style>@keyframes anim{prefix}_{index}{{{steps}}}</style>'
                      f'<div class="animation" style="width:64px;height:64px;background-image:url(&quot;{src}&quot;);'
                      f'animation:anim{prefix}_{index} {len(sequence)*.1}s steps(1,end) infinite;image-rendering:pixelated"></div></figure>')
    return ''.join(output)


def compare(before, after, output):
    """Compara versões sem alterar imagens e sem conceder aprovação artística."""
    def describe(path):
        w,h,pixels=read_png(path)
        return {'file':path.name,'width':w,'height':h,'transparent':pixels[3::4].count(0),
                'partial':sum(0<a<255 for a in pixels[3::4]),
                'sha256':hashlib.sha256(path.read_bytes()).hexdigest()}
    a,b=describe(before),describe(after)
    result={'before':a,'after':b,'sameBytes':a['sha256']==b['sha256'],'artApproved':False}
    with output.open('x',encoding='utf-8') as stream:
        json.dump(result,stream,ensure_ascii=False,indent=2)
    return result


def template(output):
    output.mkdir(parents=True, exist_ok=False)
    write_png(output/'template-vazio.png', 512, 512, bytearray(512*512*4))
    entry = dict(path='template-vazio.png', kind='character', grid=64, frames=frames(),
                 pivot=[32, 48], offset=[0, 0], layer='actors',
                 origin='TEMPLATE VAZIO - NAO E ARTE APROVADA', review={})
    (output/'manifest.json').write_text(json.dumps({'schemaVersion': 1, 'assets': [entry]}, indent=2), encoding='utf-8')
    review(output, output/'manifest.json', output/'revisao')


def aseprite(png, metadata, output):
    """Importa --format json-array, sem trim/rotate/pack, nomes ACTION.DIRECTION.INDEX."""
    data = json.loads(metadata.read_text(encoding='utf-8'))
    exported = data.get('frames')
    expected = frames()
    if not isinstance(exported, list) or len(exported) != 59:
        raise ValueError('Aseprite exige json-array com exatamente 59 frames')
    w, h, _ = read_png(png)
    if data.get('meta', {}).get('size') != {'w': w, 'h': h}:
        raise ValueError('dimensoes Aseprite divergentes')
    for source, target in zip(exported, expected):
        name = f"{target['state']}.{target['direction']}.{target['index']}"
        if source.get('filename') != name or source.get('rotated') is not False or source.get('trimmed') is not False:
            raise ValueError('ordem/direcao, rotacao ou trim incompativel: ' + name)
        if source.get('duration') != 100 or source.get('sourceSize') != {'w':64, 'h':64} or source.get('spriteSourceSize') != {'x':0,'y':0,'w':64,'h':64}:
            raise ValueError('duracao/origem/tamanho incompativel')
        rect = source['frame']
        target.update(x=rect['x'], y=rect['y'], width=rect['w'], height=rect['h'])
    entry = dict(path=png.name, kind='character', grid=64, frames=expected, pivot=[32,48],
                 offset=[0,0], layer='actors', origin='Aseprite: ' + metadata.name, review={})
    result, _ = inspect(png.parent, entry)
    if result['errors']:
        raise ValueError('; '.join(result['errors']))
    if output.exists():
        raise FileExistsError(output)
    output.write_text(json.dumps({'schemaVersion':1, 'assets':[entry]}, indent=2), encoding='utf-8')


def review(root, manifest, output):
    """Le fontes sem modifica-las; relatorio/preview fora dos assets de producao."""
    output = output.resolve()
    production = Path(__file__).resolve().parents[1]/'assets'
    if output.is_relative_to(production):
        raise ValueError('saida de revisao deve ficar fora de assets')
    output.mkdir(parents=True, exist_ok=False)
    entries = json.loads(manifest.read_text(encoding='utf-8-sig'))['assets']
    results, sections = [], []
    for entry in entries:
        result, _ = inspect(root, copy.deepcopy(entry))
        # A ferramenta nao promove nem exporta atlas, mesmo se um SHA constar no manifest.
        result['reviewToolApprovesArt'] = False
        expected = frames()
        actual = entry.get('frames', [])
        if entry['kind'] == 'character':
            if [(f['state'],f['direction'],f['index']) for f in actual] != [(f['state'],f['direction'],f['index']) for f in expected]:
                result['errors'].append('ordem ou direcao incoerente')
            if any(f.get('pivot',entry.get('pivot')) != entry.get('pivot') or f.get('durationMs',100) != 100 for f in actual):
                result['errors'].append('pivot/duracao por frame inconsistente')
        result['status'] = 'REPROVADO' if result['errors'] else 'PENDENTE_REVISAO_HUMANA'
        results.append(result)
        src = html.escape(quote(Path(os.path.relpath((root/entry['path']).resolve(),output)).as_posix()), quote=True)
        labels = ' | '.join(html.escape(f"{i}: {f['state']}.{f['direction']}.{f['index']} pivot={f.get('pivot',entry.get('pivot'))}") for i,f in enumerate(actual))
        markers=[]
        for i,f in enumerate(actual):
            pivot=f.get('pivot',entry.get('pivot',[0,0]))
            x,y=f['x']+pivot[0],f['y']+pivot[1]
            markers.append(f"<text x='{f['x']+2}' y='{f['y']+9}' fill='red' font-size='8'>{i}</text><path d='M{x-4},{y}h8 M{x},{y-4}v8' stroke='magenta'/>")
        overlay=f"<svg style='position:absolute;inset:0' width='{result.get('width',0)}' height='{result.get('height',0)}'>{''.join(markers)}</svg>"
        sections.append(f"<section><h2>{html.escape(entry['path'])}</h2><p>{result['sha256']} — {result['status']}</p><div class='sheet'><img src='{src}'>{overlay}</div><p>{labels}</p>{animation_preview(entry,src)}<pre>{html.escape(json.dumps(result,ensure_ascii=False,indent=2))}</pre></section>")
    (output/'relatorio.json').write_text(json.dumps({'schemaVersion':1,'artApproved':False,'assets':results},ensure_ascii=False,indent=2),encoding='utf-8')
    (output/'preview.html').write_text("<!doctype html><meta charset='utf-8'><title>Revisao humana - sem aprovacao automatica</title><style>body{font:14px sans-serif;background:#ddd}section{border:1px solid #444;padding:12px}.sheet{width:max-content;background:repeating-conic-gradient(#aaa 0% 25%,#eee 0% 50%) 0/16px 16px;position:relative}.sheet:after{content:'';position:absolute;inset:0;background:linear-gradient(90deg,#f008 1px,transparent 1px),linear-gradient(#f008 1px,transparent 1px);background-size:64px 64px;pointer-events:none}img{display:block;image-rendering:pixelated;max-width:none}pre{white-space:pre-wrap}</style><h1>REVISAO: nenhuma arte aprovada</h1><p>Grade 64px. Indices em ordem abaixo de cada imagem; pivot e alfa no relatorio. Originais preservados.</p>" + ''.join(sections),encoding='utf-8')
    return results


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest='command', required=True)
    t = sub.add_parser('template'); t.add_argument('output', type=Path)
    r = sub.add_parser('review')
    for name in ('root','manifest','output'): r.add_argument(name,type=Path)
    a = sub.add_parser('aseprite')
    for name in ('png','metadata','output'): a.add_argument(name,type=Path)
    c = sub.add_parser('compare')
    for name in ('before','after','output'): c.add_argument(name,type=Path)
    args = parser.parse_args()
    if args.command == 'template': template(args.output)
    elif args.command == 'review': review(args.root,args.manifest,args.output)
    elif args.command == 'compare': compare(args.before,args.after,args.output)
    else: aseprite(args.png,args.metadata,args.output)
