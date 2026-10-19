"""Inventaria PNGs sem dependencias; aprovacao humana e metadados sao obrigatorios.

Uso: python tools/validar_assets.py --root assets --manifest assets/manifest-assets.json
      --report docs/TASK-013-assets.json --atlas docs/TASK-013-atlas.png
PNG suportado na validacao de pixels: RGB/RGBA, 8 bits, sem entrelacamento.
Outros formatos permanecem PENDENTE, sem ativacao automatica.
"""
import argparse
import hashlib
import json
from pathlib import Path
import struct
import zlib
from separar_sprites import write_png

CONTRACT = {'IDLE': 1, 'WALK': 4, 'RUN': 4, 'PUNCH': 3, 'HIT': 2}
DIRECTIONS = ('FRONT', 'BACK', 'LEFT', 'RIGHT')


def read_png(path):
    """Confere CRC, tamanho e filtros PNG antes de devolver pixels RGBA."""
    data = path.read_bytes()
    if data[:8] != b'\x89PNG\r\n\x1a\n':
        raise ValueError('assinatura PNG invalida')
    pos, compressed, header = 8, bytearray(), None
    ended = False
    while pos < len(data):
        size, kind = struct.unpack_from('>I4s', data, pos)
        body = data[pos+8:pos+8+size]
        crc, = struct.unpack_from('>I', data, pos+8+size)
        if zlib.crc32(kind + body) & 0xffffffff != crc:
            raise ValueError('CRC PNG invalido')
        if kind == b'IHDR':
            header = struct.unpack('>IIBBBBB', body)
        elif kind == b'IDAT':
            compressed.extend(body)
        elif kind == b'IEND':
            ended = True
            break
        pos += size + 12
    if not ended or header is None:
        raise ValueError('PNG incompleto')
    w, h, depth, color, compression, filtering, interlace = header
    if w <= 0 or h <= 0 or w*h > 32_000_000:
        raise ValueError('dimensoes excessivas ou vazias')
    if depth != 8 or color not in (2, 6) or compression or filtering or interlace:
        raise NotImplementedError('revisao manual: PNG fora de RGB/RGBA 8 bits nao entrelacado')
    bpp = 4 if color == 6 else 3
    stride = w*bpp
    raw = zlib.decompress(compressed)
    if len(raw) != h*(stride+1):
        raise ValueError('tamanho de pixels inconsistente')
    previous, pixels = bytearray(stride), bytearray()
    for y in range(h):
        offset = y*(stride+1)
        filter_type = raw[offset]
        row = bytearray(raw[offset+1:offset+1+stride])
        if filter_type > 4:
            raise ValueError('filtro PNG invalido')
        for x in range(stride) if filter_type else ():
            left = row[x-bpp] if x >= bpp else 0
            up, corner = previous[x], previous[x-bpp] if x >= bpp else 0
            if filter_type == 4:
                p = left + up - corner
                distances = (abs(p-left), abs(p-up), abs(p-corner))
                value = (left, up, corner)[distances.index(min(distances))]
            else:
                value = (0, left, up, (left+up)//2)[filter_type]
            row[x] = (row[x] + value) & 255
        if bpp == 4:
            pixels.extend(row)
        else:
            for x in range(0, stride, 3):
                pixels.extend(row[x:x+3]); pixels.append(255)
        previous = row
    return w, h, pixels


def inspect(root, entry):
    """Distingue verificacao mecanica de aprovacao artistica; nunca promove candidato."""
    result = dict(entry)
    errors, warnings = [], []
    relative = Path(entry['path'])
    path = (root / relative).resolve()
    if relative.is_absolute() or not path.is_relative_to(root.resolve()) or (root / relative).is_symlink():
        raise ValueError('caminho de asset inseguro')
    result['sha256'] = hashlib.sha256(path.read_bytes()).hexdigest()
    pixels = None
    try:
        w, h, pixels = read_png(path)
        result.update(width=w, height=h, alpha={'transparent': pixels[3::4].count(0),
                      'partial': sum(0 < a < 255 for a in pixels[3::4])})
        grid = entry['grid']
        if grid not in (32, 64) or w % grid or h % grid:
            errors.append('dimensoes fora da grade pretendida')
        frames = entry.get('frames', [])
        seen, boxes = set(), []
        for frame in frames:
            x, y, fw, fh = (frame[k] for k in ('x', 'y', 'width', 'height'))
            key = (frame['state'], frame['direction'], frame['index'])
            if key in seen:
                errors.append('frame duplicado')
            seen.add(key)
            if fw != grid or fh != grid or x < 0 or y < 0 or x % grid or y % grid or x+fw > w or y+fh > h:
                errors.append('coordenadas de frame inconsistentes')
                continue
            if any(x < bx+bw and x+fw > bx and y < by+bh and y+fh > by for bx, by, bw, bh in boxes):
                errors.append('frames sobrepostos')
            boxes.append((x, y, fw, fh))
            if entry['kind'] == 'character':
                border = [pixels[(py*w+px)*4+3] for py in range(y,y+fh) for px in range(x,x+fw)
                          if px in (x,x+fw-1) or py in (y,y+fh-1)]
                if any(border):
                    errors.append('conteudo toca borda: corte ou fundo acidental')
                if not any(pixels[(py*w+px)*4+3] for py in range(y,y+fh) for px in range(x,x+fw)):
                    errors.append('frame vazio')
        if entry['kind'] == 'character':
            expected = {(s,d,i) for s,n in CONTRACT.items() for d in DIRECTIONS for i in range(n)}
            expected |= {('KO','FRONT',i) for i in range(3)}
            if grid != 64 or seen != expected:
                errors.append('contrato de 59 frames incompleto')
            if not result['alpha']['transparent']:
                errors.append('fundo opaco em personagem transparente')
            if result['alpha']['partial']:
                errors.append('alfa parcial: revisar antialiasing/fundo irregular')
        if not frames:
            warnings.append('recortes e linhas de corte exigem revisao no Aseprite')
        if entry.get('pivot') is None or entry.get('offset') is None or not entry.get('layer') or not entry.get('origin'):
            errors.append('metadados obrigatorios ausentes')
        else:
            for field in ('pivot','offset'):
                value=entry[field]
                if not isinstance(value,list) or len(value)!=2 or any(type(v) is not int for v in value):
                    errors.append('pivot/offset inconsistente')
                elif field=='pivot' and any(v<0 or v>grid for v in value):
                    errors.append('pivot fora do frame')
                elif field=='offset' and any(abs(v)>4096 for v in value):
                    errors.append('offset excessivo: limite 4096 por eixo')
    except NotImplementedError as error:
        warnings.append(str(error))
    except (ValueError, TypeError, KeyError, struct.error, zlib.error) as error:
        errors.append(str(error))
    approved = entry.get('review', {}).get('approvedSha256') == result['sha256']
    if not approved:
        warnings.append('aprovacao visual humana vinculada ao SHA ausente')
    result.update(errors=sorted(set(errors)), warnings=warnings,
                  status='REPROVADO' if errors else 'VALIDADO' if approved and pixels is not None else 'PENDENTE')
    return result, pixels


def run(root, manifest, report, atlas):
    """Produz inventario deterministico e atlas exclusivo de arquivos validados."""
    if manifest.exists():
        config = json.loads(manifest.read_text(encoding='utf-8'))
    else:
        config = {'schemaVersion': 1, 'characterContract': {'frameSize':64, 'totalFrames':59,
                  'directions':DIRECTIONS, 'states':CONTRACT, 'KO':{'FRONT':3}}, 'assets':[]}
    known = {e['path']:e for e in config['assets']}
    for path in sorted(root.rglob('*.png')):
        name = path.relative_to(root).as_posix()
        if name not in known:
            known[name] = {'path':name, 'kind':'candidate', 'grid':32, 'frames':[],
                           'pivot':[0,0], 'offset':[0,0], 'layer':'scenario',
                           'origin':'checkout existente; revisar assets/README.md', 'status':'PENDENTE'}
    config['assets'] = [known[k] for k in sorted(known)]
    manifest.write_text(json.dumps(config, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    results, valid = [], []
    for entry in config['assets']:
        result, pixels = inspect(root, entry)
        results.append(result)
        if result['status'] == 'VALIDADO':
            valid.append((result,pixels))
    report.write_text(json.dumps({'schemaVersion':1, 'assets':results,
                      'validatedCount':len(valid), 'atlasGenerated':bool(valid)}, ensure_ascii=False, indent=2)+'\n',encoding='utf-8')
    if valid:
        w, h = max(r['width'] for r,p in valid), sum(r['height'] for r,p in valid)
        merged, y = bytearray(w*h*4), 0
        for r,p in valid:
            for row in range(r['height']):
                merged[((y+row)*w)*4:((y+row)*w+r['width'])*4] = p[row*r['width']*4:(row+1)*r['width']*4]
            y += r['height']
        write_png(atlas,w,h,merged)
        # Ponte Java Properties: somente personagens aprovados e ligados ao SHA.
        # O PNG permanece na origem; o caminho e relativo ao manifesto exportado.
        import os
        for r,p in valid:
            if r['kind'] != 'character':
                continue
            destination = report.parent / (Path(r['path']).stem + '-validated.properties')
            values = {'status':'VALIDADO', 'sha256':r['sha256'],
                      'image':os.path.relpath(root/r['path'],destination.parent).replace('\\','/'),
                      'pivotX':r['pivot'][0], 'pivotY':r['pivot'][1],
                      'offsetX':r['offset'][0], 'offsetY':r['offset'][1]}
            for f in r['frames']:
                key=f"{f['state']}.{f['direction']}.{f['index']}"
                values[key]=f"{f['x']},{f['y']},{f['width']},{f['height']}"
            destination.write_text('\n'.join(f'{k}={v}' for k,v in sorted(values.items()))+'\n',encoding='utf-8')
    elif atlas.exists():
        raise ValueError('atlas antigo presente sem assets validados; escolha saida nova')
    print(f"Inventario: {len(results)} PNGs; validados: {len(valid)}")


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root',type=Path,default=Path('assets'))
    parser.add_argument('--manifest',type=Path,default=Path('assets/manifest-assets.json'))
    parser.add_argument('--report',type=Path,default=Path('docs/TASK-013-assets.json'))
    parser.add_argument('--atlas',type=Path,default=Path('docs/TASK-013-atlas.png'))
    args=parser.parse_args()
    run(args.root,args.manifest,args.report,args.atlas)
