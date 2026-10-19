"""Nomes descritivos para os 55 recortes do atlas Karakura v3."""
import json
import struct
from pathlib import Path

NAMES = '''grama
terra
grama-terra-transicao-01
grama-terra-transicao-02
asfalto
asfalto-faixa-central
asfalto-faixa-lateral-direita
faixa-de-pedestres
calcada-borda-horizontal
calcada-pavimento
canal-margem-esquerda
agua-ondas-01
agua-ondas-02
canal-margem-superior
muro-canal-horizontal
muro-canal-estreito-01
muro-canal-estreito-02
muro-canal-esquina-01
grama-terra-transicao-03
grama-terra-transicao-04
grama-terra-transicao-05
grama-terra-transicao-06
asfalto-variacao
asfalto-faixa-inferior
asfalto-faixa-lateral-esquerda
asfalto-faixa-esquina-direita
calcada-meio-fio
asfalto-faixa-esquina-esquerda
calcada-esquina
canal-margem-superior-ondas
canal-esquina-01
canal-esquina-02
muro-canal-variacao
muro-canal-escada-metalica
muro-canal-escadaria
muro-canal-esquina-02
arbusto-01
arbusto-flores-brancas-01
arbusto-flores-brancas-02
arbusto-flores-rosas
arbusto-02
cerca-viva
cerca-viva-flores-brancas
cerca-viva-flores-rosas
casa-telhado-azul-01
casa-telhado-cinza
arvore-01
arvore-02
arvore-03
arvore-cerejeira-01
arvore-cerejeira-02
predio-residencial-01
predio-residencial-02
loja
casa-telhado-azul-02'''.splitlines()


def rename_layers(path, mapping):
    data = path.read_bytes()
    header = bytearray(data[:128])
    count = struct.unpack_from('<H', data, 134)[0]
    pos = 144
    chunks = []
    for _ in range(count):
        size, kind = struct.unpack_from('<IH', data, pos)
        body = data[pos+6:pos+size]
        if kind == 0x2004:
            length = struct.unpack_from('<H', body, 16)[0]
            old = body[18:18+length].decode('utf-8')
            name = mapping.get(old, old).encode('utf-8')
            body = body[:16] + struct.pack('<H', len(name)) + name + body[18+length:]
        chunks.append(struct.pack('<IH', len(body)+6, kind) + body)
        pos += size
    payload = b''.join(chunks)
    frame = bytearray(data[128:144])
    struct.pack_into('<I', frame, 0, len(payload)+16)
    struct.pack_into('<I', header, 0, len(payload)+144)
    path.write_bytes(header + frame + payload)


def main():
    folder = Path(__file__).resolve().parents[1]/'assets/sprites/karakura-v3-extraidos-2026-10-18'
    manifest_path = folder/'manifest.json'
    manifest = json.loads(manifest_path.read_text(encoding='utf-8'))
    mapping = {f'sprite-{i:03d}': name for i, name in enumerate(NAMES, 1)}
    assert len(NAMES) == len(set(NAMES)) == len(manifest['sprites']) == 55
    # Verifica todos os destinos antes de alterar qualquer arquivo.
    for item in manifest['sprites']:
        old = item['name']
        if old not in mapping:
            continue
        for ext in ('.aseprite', '.png'):
            source, target = folder/(old+ext), folder/(mapping[old]+ext)
            if not source.exists() or target.exists():
                raise FileExistsError(f'Origem ausente ou destino existente: {source} -> {target}')
    for item in manifest['sprites']:
        old = item['name']
        if old not in mapping:
            continue
        new = mapping[old]
        rename_layers(folder/(old+'.aseprite'), mapping)
        for ext in ('.aseprite', '.png'):
            (folder/(old+ext)).rename(folder/(new+ext))
        item['original_name'] = old
        item['name'] = new
    rename_layers(folder/'karakura-v3-camadas.aseprite', mapping)
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding='utf-8')
    # Confirma pares, nomes das camadas e integridade dos pixels recortados.
    from separar_sprites import read_ase
    w, h, original = read_ase(Path(manifest['source']))
    total = 0
    for item in manifest['sprites']:
        sw, sh, pixels = read_ase(folder/(item['name']+'.aseprite'))
        assert (sw, sh) == (item['width'], item['height'])
        assert (folder/(item['name']+'.png')).exists()
        for y in range(sh):
            for x in range(sw):
                p = (y*sw+x)*4
                if pixels[p+3]:
                    q = ((y+item['y'])*w+x+item['x'])*4
                    assert pixels[p:p+4] == original[q:q+4]
                    total += 1
    assert total == sum(bool(a) for a in original[3::4])
    print('55 sprites renomeados em PNG e Aseprite; manifesto e camadas atualizados. Pixels verificados.')


if __name__ == '__main__':
    main()
