"""Documenta um commit: resumo, inventario e diff integral de codigo no PDF."""
from __future__ import annotations

import argparse
from collections import Counter
from datetime import date
import hashlib
import json
from pathlib import Path
import subprocess
import textwrap
from xml.sax.saxutils import escape

from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, Preformatted,
)

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / 'docs'
STEM = 'ALTERACOES-DE-CODIGO-RACAS-LAUNCHER'
PDF = DOCS / 'Spirit_Gate_Alteracoes_de_Codigo.pdf'
INK = colors.HexColor('#18283B')
ACCENT = colors.HexColor('#167A87')


def git(*args: str) -> bytes:
    return subprocess.check_output(['git', *args], cwd=ROOT)


def group(path: str) -> str:
    if path.startswith('server/src/main/java/'):
        return 'Java de producao'
    if path.startswith('server/src/test/java/'):
        return 'Testes Java'
    if path.startswith('server/src/main/resources/'):
        return 'Recursos e regras'
    if path.startswith('tools/'):
        return 'Ferramentas e empacotamento'
    if path.endswith('.cmd'):
        return 'Inicializadores Windows'
    if path == 'server/pom.xml':
        return 'Dependencias Maven'
    return 'Documentacao e evidencias'


class Report(SimpleDocTemplate):
    def afterFlowable(self, flowable):
        if isinstance(flowable, Paragraph) and getattr(flowable, 'bookmark', None):
            key, title, level = flowable.bookmark
            self.canv.bookmarkPage(key)
            self.canv.addOutlineEntry(title, key, level=level)


def generate(revision: str, publication_date: date):
    commit = git('rev-parse', '--verify', revision + '^{commit}').decode().strip()
    parent = git('rev-parse', commit + '^').decode().strip()
    statuses = {}
    for entry in git('diff', '--no-renames', '--name-status', parent, commit).decode('utf-8').splitlines():
        status, path = entry.split('\t', 1)
        statuses[path] = status
    inventory = []
    for entry in git('diff', '--no-renames', '--numstat', parent, commit).decode('utf-8').splitlines():
        added, removed, path = entry.split('\t', 2)
        inventory.append(dict(path=path, status=statuses[path], group=group(path), added=added, removed=removed))
    code = [item for item in inventory if item['group'] != 'Documentacao e evidencias']
    paths = [item['path'] for item in code]
    patch = git('diff', '--no-ext-diff', '--no-renames', '--no-color', '--full-index',
                '--src-prefix=a/', '--dst-prefix=b/', parent, commit, '--', *paths)
    patch_path = DOCS / (STEM + '.patch')
    patch_path.write_bytes(patch)
    counts = Counter(item['group'] for item in inventory)
    day = publication_date.strftime('%d/%m/%Y')
    summary = f'''# Spirit Gate — alterações de código

Relatório técnico da entrega de raças, domínios e launcher. Emitido em {day}.

## 1. Escopo e referência

Este relatório descreve o commit {commit} ("adicao das racas") em comparação com seu pai {parent}. O diff é extraído diretamente do Git, não reconstruído a partir da conversa.

Foram identificados {len(inventory)} arquivos alterados: {len(code)} arquivos de código, configuração, scripts e testes, mais {counts['Documentacao e evidencias']} documentos e evidências. O inventário inclui arquivos adicionados, modificados e removidos. O anexo do PDF contém o diff integral dos {len(code)} arquivos técnicos, com as linhas adicionadas e removidas e o contexto original.

O PDF de raças e domínios é o documento de design. Este é o documento de implementação. O patch separado preserva os mesmos bytes do diff para consulta por ferramentas. Linhas longas do anexo são quebradas apenas para caber na página.

## 2. Modelo de personagem e persistência

CharacterRaces centraliza Reaper, Human, Aetheri e Spirit e seus fundamentos iniciais. CharacterIdentity passa a validar raças atuais; a criação rejeita Guardian/Mystic como novas opções, enquanto o construtor permite sua normalização ao ler identidades antigas. O acesso race() foi acrescentado e o componente archetype permanece por compatibilidade do snapshot.

Domains cadastra dez caminhos com raça, descrição e técnicas para prévia: quatro de Reaper, dois humanos, dois de arco Aetheri e dois de trevas Spirit. DomainProgression mantém um único domínio, a escolha a partir do nível 15, XP em meias unidades, data do reset e conclusão da quest. Não permite alternância normal entre domínios.

O save V2 grava race explicitamente. LocalSaveStore aceita V1, migra Guardian para Reaper e Mystic para Human e registra a migração, preservando identidade, atributos-base, XP, itens, equipamento e quests. RpgSave valida compatibilidade racial e nível mínimo do domínio. CharacterSaves apresenta os nomes das raças no catálogo.

Antes da escolha, não se acumula XP de domínio. Depois dela, a implementação recebe metade da recompensa geral no domínio. A proporção foi confirmada para o mob de nível 1; seu uso em quests continua provisório. Existe armazenamento de XP de domínio, mas ainda não existe cálculo do nível de domínio nem requisitos finais por técnica.

## 3. Combate, progressão e reset

RpgWorld inicia sem domínio: Reaper recebe small_sword de poder adicional zero, Human/Aetheri começam com socos e Spirit com feixe luminoso básico. Qualquer raça pode optar por socos sem ganhar especialização marcial. Feixe básico de Spirit não concede domínio de luz.

O ataque básico de Aetheri passa a usar alcance de 280 pixels ao escolher bow_long e 140 em bow_short. Os demais ataques iniciais usam 82 pixels para Reaper, 60 para socos e 180 para Spirit. Esses valores são provisórios. As árvores e habilidades elementais finais não estão implementadas pelas prévias.

Shunpo ofensivo e defensivo exigem nível 20 em todas as raças e compartilham recarga. O ofensivo valida alvo visível, alcance e destino; o defensivo recua e cria uma janela de esquiva. O estado de recarga, vigor, direção e esquiva é preservado ao salvar. RpgSecondaryAbility foi adaptado aos metadados do Shunpo defensivo; os antigos ataques de arquétipo foram retirados do fluxo atual.

Parâmetros provisórios do Shunpo: custo 25 de vigor, recarga de 90 ticks, alcance ofensivo 240 pixels, recuo defensivo 70 pixels e janela de 6 ticks, com simulação de 30 Hz. Apenas o requisito de nível 20 está fechado pelo design.

EnemyService adiciona nível explícito, com compatibilidade para definições antigas. Os mobs atuais são tratados como nível 1 e recompensam 3 XP geral e 1,5 de domínio após a escolha. Níveis de mob acima de 1 são recusados enquanto suas recompensas não forem definidas. A curva geral continua provisória; cap=0 retira o teto de design, mantendo 1.000.000 XP como proteção técnica do save.

DomainResetPolicy define 35 Spirit Coins, equivalentes a R$ 35. DomainProgression exige quest concluída, saldo suficiente e um mês de calendário desde o último reset; zera o domínio e suas técnicas, preservando a base. A validação é uma regra de domínio, sem checkout ou débito financeiro real integrado. O cliente informa essa indisponibilidade. Conteúdo da quest e serviço de carteira continuam pendentes; o intervalo por personagem é uma interpretação provisória.

Foram corrigidos o evento de morte repetido em alvo já morto, a redução indevida de XP em recompensa e o alvo oculto mais próximo impedindo ataque a outro visível. A atribuição de XP geral/domínio valida as duas parcelas antes de alterar o estado, evitando atualização parcial em caso de overflow.

## 4. Interface e World Studio

CharacterCreation troca os dois arquétipos por quatro raças, apresenta o início como pequena alma/aprendiz e informa a ausência de domínio. CharacterSelection mostra raça, domínio e XP fracionária. RpgMenu remove a entrada de arena do menu atual sem remover o motor que sustenta outras ferramentas.

DomainPreview adiciona demonstrações Java2D animadas dos dez caminhos e lista técnicas de conceito. A consulta não concede XP nem desbloqueios. A escolha é habilitada apenas no nível 15 e sem domínio atual. Foi corrigido um problema de primeiro layout que podia ocultar a cena da prévia.

RpgLauncher integra prévias, escolha, salvamento e mensagens de reset, e atualiza a barra de ações para Shunpo e socos. RpgHud, RpgCombatStatus e GettingStarted refletem raça, domínio, XP e nível mínimo do Shunpo. RpgPlayerSprites, RpgVisualState e RpgCombatFeedback diferenciam feixes, arco e socos por eventos reais, sem criar regras de combate no desenho.

StudioExport e StudioPlaytest passam a criar Reaper. Foi removido o vínculo duplicado da tecla F no playtest. Editor, colisão, movimento e testes de mapa continuam preservados.

## 5. Launcher, distribuição e limpeza

Os cinco inicializadores Windows usam o fluxo compartilhado. compile-local.ps1 verifica JDK 17+, hashes de fontes/recursos/saídas e recompila para --release 17 quando necessário. A saída é isolada em server/target/launcher/classes. Uma falha de compilação impede o uso silencioso de classes antigas.

launch-local.ps1 seleciona cliente RPG, sprites, editor ou mapa. Os scripts portáteis de RPG, sprites e Studio compilam as fontes atuais e ajustam módulos do runtime ao JDK disponível. O empacotamento RPG aceita reutilização de runtime previamente validado por binários e módulos.

LauncherSelfCheck e a opção --verify-install verificam quatro raças, dez domínios, bloqueios iniciais e ciclo do save V2 sem janela nem personagens pessoais. run-local-tests.py executa JUnit Jupiter com dependências já presentes no cache, sem Maven ou rede. gerar_pdf_dominios.py gera o PDF de design.

Foram removidos KServer, Kclient, Network, DatabaseManager, Main, Archetypes e archetypes.properties, além das dependências Kryonet e PostgreSQL do POM. Permanecem o motor compartilhado, os testes úteis e o World Studio. A limpeza não apagou saves pessoais nem repositórios históricos de evidências.

O laboratório web anterior foi restrito ao proprietário por uma operação externa de Sites. Essa alteração de acesso está registrada na documentação, mas não é uma alteração de código no patch. O conector não oferecia exclusão definitiva.

## 6. Testes e evidências da entrega

Os registros da entrega em 21/10/2026 informam 191 fontes Java e 144 arquivos de teste compilados, 413 testes Java aprovados, 6 ignorados/abortados e zero falhas. A suíte Python registrou 30 testes aprovados. Esses números documentam a validação anterior; a geração deste PDF não reexecutou as suítes.

Os testes alterados e novos cobrem criação e seleção, prévias e layout, XP fracionária, bloqueio de escolha/troca, reset e calendário, preservação da base, migração V1/V2, Shunpo e recursos, alvo visível, evento de morte e compatibilidade com Studio. Os probes visuais e de jornada foram atualizados às novas raças e ações.

VALIDACAO-LAUNCHER-RACAS.json registra pacote 1.0.0-rc1 com Java integrado, 148 arquivos conferidos por tamanho/SHA-256, extração nova em caminho com espaços e retorno 0 do executável. O resultado informa quatro raças, dez domínios, saveSchema=2 e paymentEnabled=false. O compilador foi javac 24.0.2 com alvo Java 17.

Naquela sessão, a execução Java precisou ocorrer fora das restrições do sandbox por bloqueio de leitura de classpath/java.security. Isso não equivale a homologação em outro Windows nem a lançamento comercial.

## 7. Limites e próximos trabalhos

Ainda faltam o percurso jogável da pequena alma, árvores e skills finais, cálculo do nível de domínio, requisitos de técnicas, conteúdo da quest de reset, carteira e pagamentos reais, balanceamento e homologação de lançamento. As prévias não executam as skills finais no mundo de combate.

Controle poder atingir aliados da mesma party está definido no design, mas depende das skills e da integração multiplayer. A luz não ganha vantagem automática contra trevas. O cliente atual permanece local; o pacote validado é um candidato de desenvolvimento.

## 8. Inventário completo

A = adicionado; M = modificado; D = removido. + e - representam linhas do diff. Arquivos binários usam o marcador do Git. As dez entradas de documentação aparecem aqui por rastreabilidade; seu conteúdo completo não integra o anexo de código.
'''
    inventory_md = '\n'.join(f"- [{item['status']}] {item['path']} (+{item['added']} / -{item['removed']})" for item in inventory)
    (DOCS / (STEM + '.md')).write_text(summary + '\n' + inventory_md + '\n', encoding='utf-8')

    font_dir = Path('C:/Windows/Fonts')
    for name, filename in [('BodySG','arial.ttf'),('BoldSG','arialbd.ttf'),('CodeSG','consola.ttf')]:
        pdfmetrics.registerFont(TTFont(name, str(font_dir / filename)))
    styles = getSampleStyleSheet()
    styles.add(ParagraphStyle(name='SGBody', fontName='BodySG', fontSize=9.3, leading=13.5, spaceAfter=7, textColor=INK))
    styles.add(ParagraphStyle(name='SGTitle', parent=styles['SGBody'], fontName='BoldSG', fontSize=24, leading=29, spaceAfter=14))
    styles.add(ParagraphStyle(name='SGHeading', parent=styles['SGBody'], fontName='BoldSG', fontSize=14, leading=19, spaceBefore=12, spaceAfter=8, textColor=ACCENT, keepWithNext=True))
    styles.add(ParagraphStyle(name='SGFile', parent=styles['SGBody'], fontName='BoldSG', fontSize=9, leading=12, spaceBefore=10, keepWithNext=True))
    styles.add(ParagraphStyle(name='SGCell', parent=styles['SGBody'], fontSize=7.5, leading=10, spaceAfter=0))
    styles.add(ParagraphStyle(name='SGCode', fontName='CodeSG', fontSize=7, leading=9.2, spaceAfter=5, textColor=INK))
    story = []
    section = 0
    for line in summary.splitlines():
        if line.startswith('# '):
            story.append(Paragraph(escape(line[2:]), styles['SGTitle']))
        elif line.startswith('## '):
            section += 1
            heading = Paragraph(escape(line[3:]), styles['SGHeading'])
            heading.bookmark = (f'section-{section}', line[3:], 0)
            story.append(heading)
        elif line.strip():
            story.append(Paragraph(escape(line), styles['SGBody']))

    for category in counts:
        story.append(Paragraph(escape(f'{category} — {counts[category]} arquivos'), styles['SGHeading']))
        rows = [['Estado','Arquivo','+','-']]
        for item in inventory:
            if item['group'] == category:
                rows.append([item['status'], item['path'], item['added'], item['removed']])
        table = Table([[Paragraph(escape(str(cell)),styles['SGCell']) for cell in row] for row in rows],
                      colWidths=[36,365,36,36], repeatRows=1, hAlign='LEFT')
        table.setStyle(TableStyle([
            ('BACKGROUND',(0,0),(-1,0),colors.HexColor('#D5E7EB')),
            ('ROWBACKGROUNDS',(0,1),(-1,-1),[colors.HexColor('#F0F5F7'),colors.white]),
            ('VALIGN',(0,0),(-1,-1),'TOP'),
            ('TOPPADDING',(0,0),(-1,-1),5),('BOTTOMPADDING',(0,0),(-1,-1),5),
        ]))
        story.extend([table,Spacer(1,8)])

    story.append(PageBreak())
    title = Paragraph('Anexo — diff integral de código', styles['SGHeading'])
    title.bookmark = ('code-appendix','Anexo — diff integral de código',0)
    story.append(title)
    story.append(Paragraph('Cada bloco reproduz o diff do Git. + adiciona; - remove; espaço indica contexto. '
                           'As quebras de linhas longas são somente de apresentação. Consulte o arquivo .patch para os bytes originais.',styles['SGBody']))
    chunks = patch.decode('utf-8').split('diff --git ')
    if len(chunks)-1 != len(code):
        raise RuntimeError('Contagem de arquivos do anexo diverge do inventário.')
    wrapper = textwrap.TextWrapper(width=112, expand_tabs=True, replace_whitespace=False,
                                   drop_whitespace=False, break_long_words=True, break_on_hyphens=False)
    for index, chunk in enumerate(chunks[1:],1):
        item = code[index-1]
        heading = Paragraph(escape(f"{index:02d}. [{item['status']}] {item['path']}"),styles['SGFile'])
        heading.bookmark=(f'file-{index}',item['path'],1)
        story.append(heading)
        lines=[]
        for line in ('diff --git '+chunk).splitlines():
            lines.extend(wrapper.wrap(line.expandtabs(4)) or [''])
        for start in range(0,len(lines),40):
            block=lines[start:start+40]
            if any(pdfmetrics.stringWidth(line,'CodeSG',7)>499 for line in block):
                raise RuntimeError('Linha ultrapassa a largura do PDF.')
            story.append(Preformatted('\n'.join(block),styles['SGCode']))

    page_count=0
    def decorate(canvas, doc):
        nonlocal page_count
        page_count=doc.page
        canvas.saveState();canvas.setStrokeColor(ACCENT);canvas.setLineWidth(1.5)
        canvas.line(48,806,547,806)
        canvas.setFillColor(INK);canvas.setFont('BodySG',8)
        canvas.drawString(48,819,'SPIRIT GATE / RELATÓRIO TÉCNICO E DIFF')
        canvas.drawString(48,27,f'{commit[:7]} • {day}')
        canvas.drawRightString(547,27,f'Página {doc.page}')
        canvas.restoreState()
    document=Report(str(PDF),pagesize=A4,leftMargin=48,rightMargin=48,topMargin=52,bottomMargin=48,
                    title='Spirit Gate — alterações de código e diff completo',author='Spirit Gate')
    document.build(story,onFirstPage=decorate,onLaterPages=decorate)
    manifest=dict(commit=commit,parent=parent,date=publication_date.isoformat(),
                  totalChangedFiles=len(inventory),codeFiles=len(code),categories=dict(counts),files=inventory,
                  patchSha256=hashlib.sha256(patch).hexdigest(),pdfPages=page_count,
                  pdfSha256=hashlib.sha256(PDF.read_bytes()).hexdigest(),
                  testsRerun=False,validationSource='docs/ENTREGA-RACAS-LAUNCHER.md; docs/VALIDACAO-LAUNCHER-RACAS.json')
    (DOCS/(STEM+'.json')).write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(dict(pdf=str(PDF),pages=page_count,codeFiles=len(code),totalChangedFiles=len(inventory),bytes=PDF.stat().st_size),ensure_ascii=False))


if __name__ == '__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--revision',default='07accf0')
    parser.add_argument('--date',type=date.fromisoformat,required=True)
    args=parser.parse_args()
    generate(args.revision,args.date)
