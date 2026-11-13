"""Prepara o relatório HTML ilustrado a partir do catálogo/quadros exportados pelo Java.

O PDF é impresso pelo Edge local, sem dependências Python adicionais ou acesso externo.
"""
from pathlib import Path
import base64
import html
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / 'docs/skills-report'
OUTPUT = ROOT / 'docs/Spirit_Gate_Guia_Ilustrado_das_40_Skills.html'
KITS = {'water': 'Água', 'wind': 'Vento', 'thunder': 'Raio', 'fire': 'Fogo',
        'spirit': 'Spirit', 'aetheri': 'Aetheri', 'light': 'Luz', 'martial': 'Artista Marcial'}
STYLE = """
@page { size:A4; margin:20mm; }
* { box-sizing:border-box; } body { margin:0; color:#183347; background:#e7edf2; font-family:Arial,sans-serif; }
.page { width:170mm; height:257mm; position:relative; background:white; margin:16px auto; padding:0; break-after:page; }
.page:last-child { break-after:auto; }
header { color:#247c90; font-size:11px; letter-spacing:2px; padding:12px 0; border-bottom:2px solid #35bad0; }
h1 { font-size:35px; line-height:1.1; color:#102c40; margin:22px 0 15px; } h2 { font-size:20px; margin:22px 0 10px; }
p { font-size:14px; line-height:1.55; margin:10px 0; } .lead { font-size:18px; line-height:1.5; }
.tag { color:#247c90; font-weight:bold; font-size:13px; margin-top:21px; } .hint { padding:13px 16px; border-left:4px solid #29aac2; background:#edf7f9; }
.stats { display:grid; grid-template-columns:repeat(4,1fr); gap:8px; margin:18px 0; }
.stat { background:#edf2f6; padding:13px 9px; font-size:12px; } .stat b { display:block; font-size:19px; margin-top:7px; }
.frames { background:#0b2030; border-radius:7px; display:grid; grid-template-columns:repeat(3,1fr); padding:10px; gap:8px; height:172px; }
.frames img { width:100%; height:150px; object-fit:contain; image-rendering:pixelated; }
.caption { font-size:11px; color:#526a79; margin:7px 0 14px; } .note { font-size:12px; color:#526a79; }
footer { position:absolute; bottom:0; width:100%; border-top:1px solid #b8d3dc; padding:11px 0; font-size:10px; color:#526a79; display:flex; justify-content:space-between; }
.cover { background:#0b2030; color:#d5edf4; padding:34px; } .cover h1 { color:white; font-size:54px; margin:80px 0 20px; }
.cover .lead { color:#99dae6; } .cover footer { width:calc(100% - 68px); color:#99dae6; }
.kitlist { display:grid; grid-template-columns:1fr 1fr; gap:12px; margin:35px 0; font-size:17px; }
@media print { body { background:white; } .page { margin:0; } }
"""


def image(path):
    return 'data:image/png;base64,' + base64.b64encode(path.read_bytes()).decode('ascii')


def document(pages):
    return '<!doctype html><html lang="pt-BR"><meta charset="utf-8"><title>Spirit Gate — Guia das 40 skills</title><style>' + STYLE + '</style><body>' + ''.join(pages) + '</body></html>'


def generate():
    catalog = {entry.attrib['key']: entry.text or '' for entry in ET.parse(DATA / 'catalog.xml').getroot().findall('entry')}
    pages = []
    def footer(number):
        return f'<footer><span>SPIRIT GATE · REVISÃO LOCAL · 02/11/2026</span><span>{number:02d} / 42</span></footer>'
    pages.append('<section class="page cover"><header>GUIA ILUSTRADO DE COMBATE</header><h1>Spirit<br>Gate.</h1><p class="lead">40 habilidades.<br>Oito estilos de combate.</p><div class="kitlist">' + ''.join('<span>' + name + '</span>' for name in KITS.values()) + '</div><p>Sprites reais do jogo, efeitos, comandos e condições de uso.</p><p class="note" style="color:#b9d9e4">Documento da implementação local atual. Valores provisórios de balanceamento; a apresentação estática mostra quadros dos efeitos, não uma gravação de cada habilidade.</p>' + footer(1) + '</section>')
    pages.append('''<section class="page"><header>ANTES DE COMEÇAR</header><h1>Como ler e usar</h1>
<h2>Movimento e atalhos</h2><p>WASD ou setas movem o personagem. Os cinco slots usam, por padrão, F5, F6, F7, F8 e F9. O número do slot não é o número da tecla: slot 5 corresponde a F9.</p>
<h2>Quatro formas de conjurar</h2><p><b>Alvo:</b> selecione com TAB ou clique. <b>Frontal:</b> smartcast imediato na direção em que o personagem olha, em quatro direções. <b>Área:</b> ajuste com mouse, WASD ou setas e confirme com ENTER ou a mesma tecla; ESC cancela. <b>Em si:</b> ativação direta sem selecionar alvo.</p>
<h2>O que significam os valores</h2><p>Energia e recarga são os valores declarados no catálogo. Alcance e raio são unidades do mundo, não metros. Dano e cura são valores-base: atributos e regras do servidor podem alterar o resultado. Em técnicas com vários pulsos, o valor-base pode se repetir conforme a mecânica indicada.</p>
<h2>Controle e condições</h2><p>Imobilização impede movimento, mas permite ataques e habilidades. Atordoamento impede essas ações também. Prisões e lentidões sem dano são identificadas na ficha. As habilidades são liberadas no nível 15; o treino local começa no nível 20.</p>
<h2>Imagens e versão</h2><p>Os quadros são recortes dos atlas efetivamente carregados pelo jogo, respeitando as sobreposições de arte atuais. Sequência, escala, rotação e composição final são aplicadas pelo cliente. O especial de raio usa a sprite-fonte dos quatro cortes; as imagens não representam suas hitboxes.</p>
<p class="hint">Vento 5 puxa o conjunto marcado por Vento 4 sem alvo. Raio 5 concede invisibilidade, ausência de colisão e imunidade durante sua fase. Sentinela dura quatro segundos e usa o botão de ataque básico para disparar.</p>''' + footer(2) + '</section>')
    for kit in KITS:
        for slot in range(1, 6):
            ident = f'{kit}_{slot}'
            def value(field):
                return html.escape(catalog[f'{ident}.{field}'])
            def strip(suffix=''):
                return '<div class="frames">' + ''.join('<img alt="Quadro da habilidade" src="' + image(DATA / f'{ident}{suffix}-{i}.png') + '">' for i in range(3)) + '</div>'
            page = f'<section class="page"><header>SPIRIT GATE / {KITS[kit].upper()}</header><div class="tag">SLOT {slot} · ATALHO PADRÃO F{slot+4}</div><h1>{value("name")}</h1><p class="lead">{value("description")}</p>'
            page += '<div class="stats">' + ''.join(f'<div class="stat">{label}<b>{value(field)}</b></div>' for label, field in [('Energia','energy'),('Recarga','cooldown'),('Alcance','range'),('Raio','radius')]) + '</div>'
            page += strip() + '<p class="caption">Sprite atual · quadros de formação, desenvolvimento e dissipação.' + (' Fonte visual dos quatro cortes finais.' if ident == 'thunder_5' else '') + '</p>'
            page += '<p class="hint"><b>Como usar</b><br>' + value('activation') + '</p>'
            if catalog[f'{ident}.effects']:
                page += '<p><b>Efeitos:</b> ' + value('effects') + '</p>'
            pattern = catalog[f'{ident}.pattern']
            damage = value('damage'); healing = value('healing')
            if pattern == 'SENTRY':
                detail = 'Fase de 4 s. SPACE dispara flechas contra o alvo selecionado; agilidade aumenta a cadência. Conjurar de novo cancela. Fora da Sentinela, o básico é uma flecha frontal.'
            elif pattern == 'CHECKPOINT_PULL':
                detail = 'Exige contato de Vento 4 nos últimos 5 s. Puxa somente os marcados válidos; cada geração é revalidada. Consome o combo uma vez e respeita paredes. Dano-base: ' + damage + ' por inimigo.'
            elif pattern == 'CIRCULAR_DASH':
                detail = 'Fase de 32 ticks (cerca de 1,07 s). No encerramento, quatro pulsos de ' + damage + ' de dano-base cobrem todo o círculo, respeitando paredes. Os cortes aparecem em sequência; o dano não depende das linhas desenhadas.'
            elif pattern in ['HEAL','AREA_HEAL','REVIVE']:
                detail = 'Cura/vida restaurada-base: ' + healing + '. Dano-base contra mobs: ' + damage + '. Cura em área e revive não causam dano. Cura não ultrapassa a vida máxima; revive exige apoio livre.'
            elif pattern == 'CHAIN':
                detail = 'Quatro impactos, com intervalo aproximado de 0,27 s. Dano-base inicial: ' + damage + '. Sem novo inimigo, repete no atual com redução sucessiva de dano.'
            elif pattern in ['MULTI_HIT','VORTEX','RAIN','CONE_COMBO','CONE_VOLLEY']:
                detail = 'Dano-base por impacto/pulso: ' + damage + '. Quantidade declarada: ' + value('hits') + '. Duração da sequência/área: ' + value('duration') + '. A chuva causa um pulso de área por onda; as flechas visuais não multiplicam esse pulso.' if pattern == 'RAIN' else 'Dano-base por impacto: ' + damage + '. Quantidade declarada: ' + value('hits') + '. Cada acerto depende de alcance, contato e regras da técnica.'
            else:
                detail = 'Dano-base declarado: ' + damage + '. ' + ('A técnica é de controle/suporte e não causa dano direto.' if damage == '0' else 'O servidor valida contato, alcance e obstáculos antes de aplicar o resultado.')
            page += '<p class="note">' + detail + '</p>'
            for suffix, label in [('.impact','Impacto / dissipação'),('.ground','Efeito no chão'),('.travel','Textura de movimento / rastro')]:
                if f'{ident}{suffix}.frames' in catalog:
                    page += strip(suffix) + '<p class="caption">' + label + ' · componente adicional da composição.</p>'
                    break
            page += footer(len(pages)+1) + '</section>'
            pages.append(page)
    assert len(pages) == 42
    OUTPUT.write_text(document(pages), encoding='utf-8')
    for ident, index in [('wind_5',11),('thunder_5',16),('light_3',34)]:
        # Índices pelo catálogo: duas páginas iniciais e cinco habilidades por kit.
        kit, slot = ident.rsplit('_',1); index = 2 + list(KITS).index(kit)*5 + int(slot)-1
        (DATA / f'preview-{ident}.html').write_text(document([pages[index]]),encoding='utf-8')
    print(f'{OUTPUT}: {len(pages)} páginas, 40 fichas ilustradas')


if __name__ == '__main__':
    generate()
