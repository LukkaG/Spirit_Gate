"""Recorta um atlas Aseprite RGBA estático, sem dependências externas.

Uso: python tools/separar_sprites.py [arquivo.aseprite] [pasta_saida]
Não redimensiona pixels. Exporta .aseprite, PNG e manifesto de coordenadas.
"""
import argparse
import json
import struct as st
import zlib
from pathlib import Path


def chunk(kind, data):
    return st.pack('<IH', len(data) + 6, kind) + data


def read_ase(path):
    data = path.read_bytes()
    _, magic, frames, width, height, depth = st.unpack_from('<I5H', data)
    if magic != 0xA5E0 or frames != 1 or depth != 32:
        raise ValueError('Esperado Aseprite RGBA de um frame; arquivo original preservado.')
    result = bytearray(width * height * 4)
    count = st.unpack_from('<H', data, 134)[0]
    if count == 65535:
        count = st.unpack_from('<I', data, 140)[0]
    pos = 144
    cels = []
    for _ in range(count):
        size, kind = st.unpack_from('<IH', data, pos)
        body = data[pos + 6:pos + size]
        if kind == 0x2004:
            flags, layer_type = st.unpack_from('<HH', body)
            if layer_type != 0 or not flags & 1 or body[12] != 255:
                raise ValueError('Use uma camada normal visível com opacidade 255.')
        if kind == 0x2005:
            layer, x, y, opacity, celtype = st.unpack_from('<HhhBH', body)
            if opacity != 255 or celtype not in (0, 2):
                raise ValueError('Cel vinculado ou opacidade parcial não suportados.')
            cw, ch = st.unpack_from('<HH', body, 16)
            raw = zlib.decompress(body[20:]) if celtype == 2 else body[20:]
            if len(raw) != cw * ch * 4:
                raise ValueError('Dados RGBA inválidos.')
            cels.append((x, y, cw, ch, raw))
        pos += size
    if len(cels) != 1:
        raise ValueError('Esperado um único cel: achate uma cópia do atlas antes de executar.')
    x, y, cw, ch, raw = cels[0]
    for row in range(ch):
        if not 0 <= y + row < height:
            continue
        left, right = max(0, x), min(width, x + cw)
        if right > left:
            dst = ((y + row) * width + left) * 4
            src = (row * cw + left - x) * 4
            result[dst:dst + (right-left)*4] = raw[src:src + (right-left)*4]
    return width, height, result


def components(width, height, pixels):
    mask = bytearray(pixels[3::4])
    found = []
    for start in range(width * height):
        if not mask[start]:
            continue
        stack = [start]
        mask[start] = 0
        points = []
        x0 = x1 = start % width
        y0 = y1 = start // width
        while stack:
            p = stack.pop()
            points.append(p)
            x, y = p % width, p // width
            x0, x1 = min(x0, x), max(x1, x)
            y0, y1 = min(y0, y), max(y1, y)
            for yy in range(max(0, y-1), min(height, y+2)):
                for xx in range(max(0, x-1), min(width, x+2)):
                    q = yy * width + xx
                    if mask[q]:
                        mask[q] = 0
                        stack.append(q)
        found.append({'box': [x0, y0, x1+1, y1+1], 'points': points})
    return found


def write_ase(path, width, height, pixels):
    name = path.stem.encode('utf-8')
    layer = st.pack('<6HB', 1, 0, 0, 0, 0, 0, 255) + bytes(3) + st.pack('<H', len(name)) + name
    cel = st.pack('<HhhBH', 0, 0, 0, 255, 2) + bytes(7) + st.pack('<HH', width, height) + zlib.compress(pixels)
    chunks = chunk(0x2004, layer) + chunk(0x2005, cel)
    frame = st.pack('<IHHHHI', len(chunks)+16, 0xF1FA, 2, 100, 0, 0) + chunks
    header = bytearray(128)
    st.pack_into('<I5HI', header, 0, 128+len(frame), 0xA5E0, 1, width, height, 32, 1)
    st.pack_into('<I', header, 18, 100)
    header[34:36] = bytes([1, 1])
    path.write_bytes(header + frame)


def write_png(path, w, h, pixels):
    def part(kind, body):
        return st.pack('>I', len(body)) + kind + body + st.pack('>I', zlib.crc32(kind+body))
    rows = b''.join(b'\0' + pixels[y*w*4:(y+1)*w*4] for y in range(h))
    path.write_bytes(b'\x89PNG\r\n\x1a\n' + part(b'IHDR', st.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0)) + part(b'IDAT', zlib.compress(rows)) + part(b'IEND', b''))


def main():
    root = Path(__file__).resolve().parents[1]
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('input', nargs='?', type=Path, default=root/'assets/tilesets/karakura-environment-v3-blend.aseprite')
    parser.add_argument('output', nargs='?', type=Path, default=root/'assets/sprites/karakura-v3-separated')
    parser.add_argument('--min-pixels', type=int, default=150, help='Área mínima para um sprite independente')
    parser.add_argument('--fragment-distance', type=int, default=24, help='Distância máxima para anexar pétalas e fragmentos')
    args = parser.parse_args()
    w, h, pixels = read_ase(args.input)
    parts = components(w, h, pixels)
    large = [p for p in parts if len(p['points']) >= args.min_pixels]
    small = [p for p in parts if len(p['points']) < args.min_pixels]
    if not large:
        raise ValueError('Nenhum sprite encontrado.')
    # Ilhas internas (por exemplo, o piso dentro de uma borda em L)
    # pertencem ao mesmo objeto mesmo sem conexão de pixels.
    nested = set()
    for i, part in enumerate(large):
        x0, y0, x1, y1 = part['box']
        containers = [j for j, other in enumerate(large) if j != i
                      and other['box'][0] <= x0 and other['box'][1] <= y0
                      and other['box'][2] >= x1 and other['box'][3] >= y1
                      and len(other['points']) > len(part['points'])]
        if containers:
            # Escolhe o maior recipiente para evitar cadeias de anexação.
            parent = max(containers, key=lambda j: len(large[j]['points']))
            large[parent]['points'].extend(part['points'])
            nested.add(i)
    large = [part for i, part in enumerate(large) if i not in nested]
    original_boxes = [p['box'][:] for p in large]
    unassigned = []
    for part in small:
        x0, y0, x1, y1 = part['box']
        def distance(box):
            a, b, c, d = box
            return max(a-x1, x0-c, 0)**2 + max(b-y1, y0-d, 0)**2
        idx = min(range(len(large)), key=lambda i: distance(original_boxes[i]))
        if distance(original_boxes[idx]) <= args.fragment_distance**2:
            large[idx]['points'].extend(part['points'])
            a, b, c, d = large[idx]['box']
            large[idx]['box'] = [min(a,x0), min(b,y0), max(c,x1), max(d,y1)]
        else:
            unassigned.append(part)
    # Fragmentos distantes são preservados como arquivos próprios, nunca descartados.
    large.extend(unassigned)
    large.sort(key=lambda p: (p['box'][1]//32, p['box'][0]))
    args.output.mkdir(parents=True, exist_ok=True)
    manifest = []
    atlas_chunks = []
    rebuilt = bytearray(len(pixels))
    for i, part in enumerate(large, 1):
        x0, y0, x1, y1 = part['box']
        sw, sh = x1-x0, y1-y0
        crop = bytearray(sw*sh*4)
        for p in part['points']:
            target = ((p//w-y0)*sw + p%w-x0)*4
            crop[target:target+4] = pixels[p*4:p*4+4]
            rebuilt[p*4:p*4+4] = pixels[p*4:p*4+4]
        name = f'sprite-{i:03d}'
        for ext in ('.aseprite', '.png'):
            if (args.output/(name+ext)).exists():
                raise FileExistsError('A pasta já contém recortes; escolha outra pasta para preservar os existentes.')
        write_ase(args.output/(name+'.aseprite'), sw, sh, crop)
        write_png(args.output/(name+'.png'), sw, sh, crop)
        layer_name = name.encode('utf-8')
        atlas_chunks.append(chunk(0x2004, st.pack('<6HB', 1, 0, 0, 0, 0, 0, 255) + bytes(3) + st.pack('<H', len(layer_name)) + layer_name))
        atlas_chunks.append(chunk(0x2005, st.pack('<HhhBH', i-1, x0, y0, 255, 2) + bytes(7) + st.pack('<HH', sw, sh) + zlib.compress(crop)))
        rw, rh, check = read_ase(args.output/(name+'.aseprite'))
        assert (rw, rh, check) == (sw, sh, crop), 'Falha no round-trip Aseprite'
        manifest.append({'name': name, 'x': x0, 'y': y0, 'width': sw, 'height': sh, 'visible_pixels': len(part['points'])})
    assert all(pixels[i:i+4] == rebuilt[i:i+4] for i in range(0,len(pixels),4) if pixels[i+3]), 'Pixels visíveis perdidos'
    (args.output/'manifest.json').write_text(json.dumps({'source':str(args.input), 'canvas':[w,h], 'sprites':manifest}, indent=2), encoding='utf-8')
    write_png(args.output/'atlas-verificacao.png', w, h, rebuilt)
    chunks_data = b''.join(atlas_chunks)
    frame = st.pack('<IHHHHI', len(chunks_data)+16, 0xF1FA, len(atlas_chunks), 100, 0, 0) + chunks_data
    header = bytearray(128)
    st.pack_into('<I5HI', header, 0, len(frame)+128, 0xA5E0, 1, w, h, 32, 1)
    st.pack_into('<H', header, 18, 100)
    header[34:36] = bytes([1, 1])
    (args.output/'karakura-v3-camadas.aseprite').write_bytes(header + frame)
    print(f'{len(large)} sprites exportados; {len(small)-len(unassigned)} fragmentos anexados; {len(unassigned)} fragmentos isolados preservados.')
    print('Round-trip Aseprite e preservação de todos os pixels visíveis: OK')
    for item in manifest:
        print(item['name'], item['x'], item['y'], item['width'], item['height'])


if __name__ == '__main__':
    main()
