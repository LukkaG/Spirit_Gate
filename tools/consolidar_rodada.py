"""Consolida somente evidências realmente executadas das TASK-022 a TASK-036."""
from pathlib import Path
import hashlib
import json
import re
import zipfile
from gerar_pdf_task002 import gerar_pdf
from checkpoint_rodada import checkpoint

ROOT=Path(__file__).resolve().parents[1]
DOCS=ROOT/'docs'

def read(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    release=max((ROOT/'dist').glob('SpiritGate-0.36.0-rc1-*-portable.zip'),key=lambda p:p.stat().st_mtime_ns)
    audit=read(DOCS/'TASK-036-auditoria.json')
    smoke=read(DOCS/'TASK-036-smoke.json');map_smoke=read(DOCS/'TASK-036-map-smoke.json')
    assert audit['status']=='PASS'
    assert Path(audit['zip']).name==release.name
    for result in (smoke,map_smoke):
        assert result.get('bundledJvmVerified') and result.get('exitCode')==0 and result.get('windowExitCode')==0
        assert Path(result['zip']).name==release.name
    rows=[('022',223,18)]
    for number in range(23,37):
        task=f'{number:03}'
        result=read(DOCS/f'TASK-{task}-qualidade.json')
        assert result['exitCode']==0,task
        rows.append((task,result['checks']['java']['totals']['tests'],result['checks']['python']['tests']))
    previous=read(ROOT/'.task-checkpoints/TASK-022-before.json')
    historical=[name for name in previous if name.startswith(('server/src/main/java/com/projetok/domain/',
                                                               'server/src/test/java/com/projetok/domain/'))]
    changed=[name for name in historical if (ROOT/name).read_text(encoding='utf-8-sig')!=previous[name]]
    assert not changed
    assets=read(DOCS/'TASK-016-assets.json')['assets']
    art_changes=[a['path'] for a in assets if digest(ROOT/'assets'/a['path'])!=a['sha256']]
    assert not art_changes
    original=ROOT/'dist/SpiritGate-0.21.0-rc1-20261019T001737Z-546ce1c5-portable.zip'
    assert digest(original)=='303f469b87aa0752d6dcd44e21ae5efef78295cf971884976939b1d8ae6cc534'
    preservation={'historicalDomainAndTestFilesCompared':len(historical),'changedHistoricalDomainFiles':changed,
                  'sourcePngCompared':len(assets),'changedSourcePng':art_changes,
                  'baseline021ZipPreserved':True,'baseline021Sha256':digest(original)}
    (DOCS/'TASK-036-preservacao.json').write_text(json.dumps(preservation,indent=2),encoding='utf-8')
    task36=f'''# TASK-036 - Release candidate consolidada

Objetivo: candidata portátil Windows x64 construída dos fontes consolidados.
Base: TASK-035 aprovada, 241 JUnit e 25 Python.
Versão 0.36.0-rc1 sem conflito anterior no diretório dist.
Situação: AUTOMAÇÃO LOCAL APROVADA; homologação humana pendente.

Implementação: versão do diagnóstico consolidada; README/changelog e aviso do
pacote atualizados; kit novo com roteiro por tarefa, modelo de bug, notas e mapa
V2 opcional. Build limpo real com Maven offline, sem SkipMaven; JAR novo criado
somente de domain/prototype; jdeps, jlink, jpackage e JVM Java 17 integrada.
Não foram incluídas artes pendentes, fontes de teste ou configurações privadas.
Revisão final corrigiu o comando do mapa opcional no kit e tornou os caminhos
dos previews HTML relativos, para portabilidade do pacote documental.

Artefato: dist/{release.name}
Bytes: {release.stat().st_size}
SHA-256: {digest(release)}
Auditoria oficial PASS: ZIP original, inventário, whitelist, hashes, PE x64 e
bytecode de todas as classes atuais. Controle negativo e distribuição visual
sintética testados em cópias, sem promoção de arte. Ver logs/JSON TASK-036.
Smokes reais do EXE padrão e --map em extrações distintas fora de Projeto-K,
PATH sem Java externo, JVM integrada comprovada, fechamento normal e códigos 0.
Capturas TASK-036-exe.png e TASK-036-mapa-exe.png são reais, não simulações.
Inspeção de sockets por CIM pode estar indisponível: ver JSON, sem afirmar o
que esse controle não conseguiu medir. Não houve teste em outro computador.

Preservação: {len(historical)} fontes/testes históricos de domínio idênticos;
{len(assets)} PNGs idênticos ao inventário histórico; ZIP 0.21 original intacto.
Todos os releases anteriores mantidos. Sem commit, push, publicação ou upload.
Arquivos modificados/adicionados e inventário estão no relatório final e diff.

Reprodução: python tools/qualidade.py --offline --task 036
Build: tools/build-windows-portable.ps1 -Version 0.36.0-rc1 -MavenRepository
.maven-repository -TesterKitDirectory docs/kit-homologacao-022-036
Auditoria: python tools/auditar_rc.py --zip dist/{release.name} --report docs/re-auditoria.json
Smoke: tools/test-windows-portable.ps1 -ZipPath dist/{release.name}

Limites: sem assinatura do executável, sem arte final aprovada, sem homologação
humana em outro Windows. Não é lançamento público. Próximo passo humano: kit de
homologação por tarefa. Continuidade em docs/CONTINUIDADE-TASK-022_036.md.
'''
    (DOCS/'TASK-036.md').write_text(task36,encoding='utf-8')
    for task,java,python in rows:
        path=DOCS/f'TASK-{task}.md'
        text=path.read_text(encoding='utf-8')
        marker='\n## Resultado final observado\n'
        text=text.split(marker)[0]+marker+f'Validação local aprovada: {java} JUnit e {python} Python; zero falhas.\n'
        path.write_text(text,encoding='utf-8')
        gerar_pdf(path,DOCS/f'Spirit_Gate_TASK-{task}.pdf',f'TASK-{task}')
    descriptions=['Auditoria e baseline','Orquestrador de qualidade','Mapas V2 ampliados',
                  'Culling conservador','Camadas de cenário','Autoria offline de mapas',
                  'Prévia e comparação de sprites','Robustez de pacotes visuais',
                  'Regressões de animação','Foco e mira','Ajuda F1 e HUD',
                  'Feedback de combate','Diagnóstico local F2','Estresse determinístico','Release portátil']
    table='\n'.join(f'| {task} | {description} | Concluída na automação local | {java} | {python} |'
                    for (task,java,python),description in zip(rows,descriptions))
    report=f'''# Relatório final - TASK-022 a TASK-036

Spirit Gate, 19/10/2026 (America/Sao_Paulo). Base: 0.21.0-rc1.
As quinze tarefas foram concluídas na validação automatizada local, em sequência
com checkpoint inicial, suíte completa e documentação por etapa. Nenhuma tarefa
fica bloqueada na implementação. Aprovação artística e homologação humana em outro
computador Windows permanecem pendentes; não são declaradas executadas.

## Entrega

Candidata local: dist/{release.name}
Tamanho: {release.stat().st_size} bytes.
SHA-256: {digest(release)}
Auditoria: docs/TASK-036-auditoria.json. Smokes: TASK-036-smoke.json e
TASK-036-map-smoke.json. Java 17 integrado, EXE PE x64, novo JAR de build limpo.
Último release anterior preservado: {original.name}.
Não houve publicação, upload ou operação Git de commit/push.

## Evolução real dos testes

| TASK | Entrega | Situação | JUnit | Python |
|---|---|---|---:|---:|
{table}

Evolução: 223 para 241 JUnit (+18), 18 para 25 Python (+7).
Todos os resultados finais: zero falhas/erros/ignorados em JUnit; Python OK.
TASK-035 teve um erro inicial de sintaxe no teste novo, corrigido sem alterar
asserções. O log da tentativa recusada foi preservado. A primeira invocação Maven
da baseline falhou ao selecionar o cache; a repetição com argumento completo
entre aspas usou o cache do projeto e passou. Não se ocultaram falhas de gameplay.

## Alterações e decisões

- Mapas: Properties V1 960x640 compatível; V2 reservado a mapas ampliados,
  960x640 até 8192x8192, máximo 4096 elementos por lista, validações físicas
  preservadas. Mundo, Criostase, câmera e clipping usam as dimensões reais.
- Render: culling conservador por clipping Java2D e comparação de pixels;
  camadas 0-4 traseiras e 5-10 frontais, ordenação estável. Sem índice paralelo.
- Autoria: CLI Python gera exportação determinística e reutiliza o validador
  Java autoritativo. Revisão de sprites inclui prévias direcionais e comparação
  de dimensões, alfa e SHA, preservando contrato de 59 frames e revisão humana.
- Assets: carregador único com fallback, limites de arquivo/dimensões, rejeição
  de chave duplicada, caminho externo e offset extremo. Sem IO por frame.
- Entrada/animação: prioridades históricas mantidas, deltas inválidos recusados,
  testes de transição; perda de foco limpa held/mira sem apagar casts FIFO.
- Apresentação: F1 mostra ajuda e valores publicados; impactos/derrotas usam
  efeitos reais com expiração da simulação; listas do snapshot são copiadas.
- Diagnóstico: F2 salva ZIP local explícito com um JSON e campos por whitelist;
  sem nome pessoal, caminho completo, mensagens livres ou transmissão externa.
- Qualidade: comando único offline, logs individuais, JSON/TXT e códigos de
  ambiente/teste/build, sem instalação de dependências ou mudanças globais.

## Preservação e estresse

{len(historical)} arquivos históricos de domínio e seus testes comparados com
checkpoint inicial: nenhuma alteração. {len(assets)} PNGs conferidos contra
inventário histórico: nenhum alterado. ZIP 0.21 original com hash preservado.
Fórmulas, vigor, cooldowns, Freeze/Criostase, SOLID/SOFT, dash seletivo,
recuperação de posição, FIFO e três replays históricos preservados pela suíte.
Estresse: dois runtimes, 6000 ticks cada e 6000 comparações completas, dez R por
runtime, 780 ticks com Freeze, 3000 com gelo e 80 com dash; 4096 visuais em mapa
8192x8192, câmera extrema e 32 repetições de dash sólido. Métricas observadas,
sem limiar temporal para aprovar gameplay. Ver TASK-035-estresse.json.

## Inventário e recuperação

Cada tarefa tem docs/TASK-NNN.md, Spirit_Gate_TASK-NNN.pdf, TASK-NNN-testes.txt,
TASK-NNN.diff e logs. Checkpoints em .task-checkpoints/TASK-NNN-before.json
(conteúdo textual recuperável) e TASK-NNN-after.json (hashes/arquivos alterados).
O diff reconstrói o estado posterior a partir do snapshot anterior. Não existe Git
neste checkout e não foi criado repositório. Inventário completo com hashes em
    docs/INVENTARIO-TASK-022_036.json (fontes locais); o consolidado JSON lista alterações por tarefa.
Snapshots locais conservam fontes; pacote documental inclui metadados e evidências,
separado do ZIP do jogo. Os logs textuais exportados têm caminhos pessoais redigidos.
Continuar por docs/CONTINUIDADE-TASK-022_036.md, sem repetir tarefas aprovadas.

## Reprodução

Na raiz Projeto-K: python tools/qualidade.py --offline --task local
Requer Java 17, Maven, Python e cache .maven-repository existente.
Somente Python: python -m unittest discover -s tools -p "test_*.py" -v
(os testes de autoria de mapas requerem classes Java compiladas).
Mapa: python tools/mapas.py create nome-novo.properties; depois validate nome-novo.properties.
Build: powershell -NoProfile -ExecutionPolicy Bypass -File tools/build-windows-portable.ps1
-Version 0.36.0-rc1 -MavenRepository .maven-repository
-TesterKitDirectory docs/kit-homologacao-022-036
O build cria outra saída única, preservando releases anteriores.
Auditar ZIP: python tools/auditar_rc.py --zip dist/{release.name} --report docs/re-auditoria.json
Executável opcional: SpiritGate.exe --map mapa-demo.properties

## Limitações e roteiro humano por tarefa

Roteiro separado por TASK em docs/kit-homologacao-022-036/ROTEIRO-HOMOLOGACAO.md,
incluído também no jogo. Revisar F1/F2, foco, colisões seletivas, mapa ampliado,
R, camadas e sessão prolongada; em outro Windows sem Java, conferir abertura e SHA.
Capturas do EXE na TASK-036 são reais. Template/autoria e cenários de teste são
sintéticos identificados, sem aprovação artística. Não há sprites definitivos.
EXE não assinado; sem teste Linux ou homologação em outro computador. Inspeção
de sockets por CIM é indisponível neste ambiente; não se declarou medição negativa.
Não há multiplayer, autenticação, migração de banco ou mudança de protocolo.
Zoom inteiro continua API de integração; não foi inventado atalho novo.
Fonte escala com janela e precisa de revisão humana de legibilidade em tamanhos
muito pequenos. Culling conserva efeitos/atlas com offsets e ainda percorre fundo;
não foi alegada eliminação de todo custo fora da viewport. Estresse sintético não
prova ausência de todo vazamento e não substitui sessão humana longa.
'''
    final=DOCS/'RELATORIO-FINAL-TASK-022_036.md';final.write_text(report,encoding='utf-8')
    gerar_pdf(final,DOCS/'Spirit_Gate_Relatorio_Final_TASK-022_036.pdf','TASK-022 a TASK-036')
    (DOCS/'CONTINUIDADE-TASK-022_036.md').write_text(
        '# Continuidade - 19/10/2026\n\nÚltima concluída: TASK-036, automação local aprovada.\n'
        f'Release: dist/{release.name}\nSHA-256: {digest(release)}\n\n'
        'Próximo passo: homologação humana em outro Windows e revisão artística, conforme kit.\n'
        'Nenhuma TASK 022-036 precisa ser reimplementada. Não gerar arte aprovada automaticamente.\n'
        'Para validar checkout: python tools/qualidade.py --offline --task retomada\n'
        'Para auditoria: python tools/auditar_rc.py --zip dist/'+release.name+' --report docs/retomada-auditoria.json\n'
        'Ler RELATORIO-FINAL-TASK-022_036.md e os JSON de evidência antes de nova alteração.\n'
        'Snapshots recuperáveis em .task-checkpoints, sem Git. Antes de nova tarefa, criar novo checkpoint.\n',encoding='utf-8')
    checkpoint('036',True)
    changes={f'{n:03}':read(ROOT/f'.task-checkpoints/TASK-{n:03}-after.json')['changed'] for n in range(22,37)}
    (DOCS/'ALTERACOES-TASK-022_036.json').write_text(json.dumps(changes,indent=2),encoding='utf-8')
    selected=[]
    for path in DOCS.rglob('*'):
        if not path.is_file():continue
        rel=path.relative_to(DOCS).as_posix()
        if (re.match(r'(?:Spirit_Gate_)?TASK-0(?:2[2-9]|3[0-6])(?:[.-]|$)',rel)
            or rel.startswith(('kit-homologacao-022-036/','autoria-028-sintetica/','mapas/'))
            or path.name in ('RELATORIO-FINAL-TASK-022_036.md','Spirit_Gate_Relatorio_Final_TASK-022_036.pdf',
                            'CONTINUIDADE-TASK-022_036.md','ALTERACOES-TASK-022_036.json')):
            selected.append(path)
    selected+=list((ROOT/'.task-checkpoints').glob('TASK-0*-after.json'))
    selected=[p for p in selected if not p.parent.name=='.task-checkpoints' or 22<=int(p.stem.split('-')[1])<=36]
    inventory=[{'path':p.relative_to(ROOT).as_posix(),'sizeBytes':p.stat().st_size,'sha256':digest(p)} for p in sorted(selected)]
    inv=DOCS/'INVENTARIO-TASK-022_036.json';inv.write_text(json.dumps(inventory,indent=2),encoding='utf-8');selected.append(inv)
    destination=ROOT/'dist/SpiritGate-Documentos-TASK-022_036.zip'
    if destination.exists():raise FileExistsError(destination)
    package_inventory=[]
    with zipfile.ZipFile(destination,'x',zipfile.ZIP_DEFLATED) as archive:
        for path in sorted(selected):
            data=path.read_bytes()
            if path.suffix.lower() in ('.md','.log','.txt','.json','.diff','.html','.properties'):
                text=data.decode('utf-16' if data.startswith((b'\xff\xfe',b'\xfe\xff')) else 'utf-8-sig')
                text=re.sub(r'C:(?:\\+|/)Users(?:\\+|/)[^\\/\r\n"]+',r'C:/Users/<USER>',text,flags=re.I)
                data=text.encode('utf-8')
            name=path.relative_to(ROOT).as_posix()
            archive.writestr(name,data)
            package_inventory.append({'path':name,'sizeBytes':len(data),'sha256':hashlib.sha256(data).hexdigest()})
        archive.writestr('INVENTARIO-PACOTE.json',json.dumps(package_inventory,indent=2).encode('utf-8'))
    Path(str(destination)+'.sha256').write_text(digest(destination)+'  '+destination.name+'\n',encoding='utf-8')
    print(destination.name)

if __name__=='__main__':main()
