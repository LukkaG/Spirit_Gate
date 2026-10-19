"""Consolida evidencias reais da expansao; nunca executa rede ou altera saves."""
from pathlib import Path
import argparse
import hashlib
import json
import re
import zipfile
from gerar_pdf_task002 import gerar_pdf

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / 'docs'
TITLES = [
    'Auditoria da base 0.36.0-rc1', 'Entrada RPG separada', 'Identidade e criacao local',
    'Registro de conteudo', 'Save V1 e backup', 'Itens e raridade', 'Inventario transacional',
    'Equipamento e atributos', 'Pocoes e recarga', 'Loot e ledger de geracoes',
    'XP e niveis', 'Guardian e Mystic', 'Inimigos e respawn', 'IA PvE', 'Chefe com aviso e fases',
    'NPCs e interacao', 'Dialogos com escolhas', 'Diario e estados de quest',
    'Objetivos e recompensas', 'Comerciante e moedas', 'Portais seguros', 'Tres regioes',
    'Baus e recursos', 'Ciclo de iluminacao', 'Masmorra e retorno', 'Menu e selecao de personagem',
    'UI de inventario/equipamento', 'Diario/mapa/objetivos', 'Hotbar e atalhos',
    'Audio e preferencias; marco offline B', 'Protocolo binario V1', 'Servidor localhost headless',
    'Duas sessoes e reconexao', 'Cliente visual e interpolacao', 'LAN explicita; dois processos locais',
    'Combate e habilidades autoritativas', 'Chefe cooperativo e recompensa',
    'Falhas de rede e desconexoes', 'Integracao e regressao de backup', 'Candidatos privados e pacotes',
]

def digest(path):
    result = hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            result.update(block)
    return result.hexdigest()

def read(name):
    return json.loads((DOCS / name).read_text(encoding='utf-8-sig'))

def source_files():
    files = []
    for directory in (ROOT / 'server/src', ROOT / 'tools'):
        files += [p for p in directory.rglob('*') if p.is_file() and '__pycache__' not in p.parts]
    files += [p for p in ROOT.iterdir() if p.is_file() and (p.suffix in ('.cmd', '.md') or p.name == '.gitignore')]
    files.append(ROOT / 'server/pom.xml')
    return sorted(files)

def reports():
    quality = read('TASK-076-qualidade.json')
    assert quality['exitCode'] == 0
    smoke = read('TASK-076-smoke.json')
    assert smoke['automatedApproved'] and smoke['serverExitCode'] == 0
    legacy_smoke = read('TASK-076-arena-smoke.json')
    assert legacy_smoke['bundledJvmVerified'] and legacy_smoke['exitCode'] == 0
    packages = [read('TASK-076-auditoria-rpg.json'), read('TASK-076-auditoria-coop.json')]
    assert all(p['automatedApproved'] for p in packages)
    baseline = json.loads((ROOT / '.task-checkpoints/TASK-037-before.json').read_text(encoding='utf-8'))
    changed = []
    for path in source_files():
        name = path.relative_to(ROOT).as_posix()
        try:
            text = path.read_text(encoding='utf-8-sig')
        except UnicodeError:
            continue
        if baseline.get(name) != text:
            changed.append({'path': name, 'change': 'modified' if name in baseline else 'added', 'sha256': digest(path)})
    legacy_domain = all((ROOT / name).read_text(encoding='utf-8-sig') == text for name, text in baseline.items()
                        if name.startswith('server/src/main/java/com/projetok/domain/') and name.endswith('.java'))
    legacy_tests = all((ROOT / name).read_text(encoding='utf-8-sig') == text for name, text in baseline.items()
                       if name.startswith('server/src/test/') and name.endswith('.java'))
    original = ROOT / 'dist/SpiritGate-0.36.0-rc1-20261019T042237Z-a006aab1-portable.zip'
    original_hash = digest(original)
    assert original_hash == '511d59412598be73395391e21a6cfb2315f4855c714059086abc4d02e46cff31'
    assert legacy_domain and legacy_tests
    tasks = []
    for number, title in enumerate(TITLES, 37):
        evidence = read(f'TASK-{number:03}-qualidade.json')
        assert evidence['exitCode'] == 0
        tasks.append({'task': f'{number:03}', 'title': title,
                      'status': 'parcial: homologacao LAN entre maquinas pendente' if number == 71 else
                                'parcial: sem transicoes de regiao online' if number == 74 else 'concluida: validacao automatizada',
                      'tests': evidence['checks']})
    inventory = {'schemaVersion': 1, 'date': '2026-10-19', 'sourceChanges': changed,
                 'tasks': tasks, 'packages': packages, 'baselineSha256': original_hash,
                 'legacyDomainUnchanged': legacy_domain, 'legacyTestsUnchanged': legacy_tests,
                 'finalChecks': quality['checks'], 'smoke': smoke, 'legacyArenaSmoke': legacy_smoke}
    (DOCS / 'INVENTARIO-ALTERACOES-TASK-037_076.json').write_text(json.dumps(inventory, ensure_ascii=False, indent=2), encoding='utf-8')
    table = '\n'.join(f"| TASK-{t['task']} | {t['title']} | {'Parcial' if t['task'] in ('071','074') else 'Validada'} |" for t in tasks)
    report = f'''# Relatorio final - Spirit Gate TASK-037 a TASK-076

Data local: 19/10/2026, America/Sao_Paulo. Base real: TASK-036 / 0.36.0-rc1, sem Git. O checkpoint 21 citado inicialmente ja tinha sido sucedido pelas TASK-022 a 036 neste checkout; elas nao foram refeitas.

## Resultado principal: campanha RPG offline jogavel

RPG 0.66.1-rc1 com Java integrado. Iniciar por jogar-rpg-offline.cmd ou dist/RPG-Offline-0.66.1-rc1/SpiritGateRPG/SpiritGateRPG.exe. Menu, personagens por UUID, Guardian/Mystic, aparencia, vila, bosque, santuario, duas missoes, inimigos, XP/niveis, moedas, inventario de 20 slots, equipamento, pocoes, loot, comercio, baus/recursos, dois sentinelas e chefe final. Save/backup/retomada, diario, mapa, hotbar, atalhos e audio/preferencias integrados.

O personagem percorre vila, conversa com a Ancia, aceita Ecos no bosque, derrota inimigos, recebe loot/recompensa, compra/equipa itens, aceita Santuario, enfrenta guardas/chefe, salva, reabre e entrega a reliquia. A campanha inicial inteira foi automatizada no dominio para os dois arquetipos, sem injecao de XP/moeda/item/posicao. A interface real foi aberta no executavel; isso nao representa homologacao humana de todo o percurso.

## Validacao e preservacao

- Baseline reproduzida: 241 Java + 25 Python, zero falhas.
- Final: {quality['checks']['java']['totals']['tests']} Java + {quality['checks']['python']['tests']} Python, zero falhas/erros/skips Java. Sao 56 testes Java adicionais.
- Suite completa executada em cada etapa; resultados individuais em TASK-NNN-testes.txt e logs Java/Python. Falhas intermediarias foram preservadas em logs separados.
- Stress offline: 20000 ticks com recargas periodicas e comparacao de estado. Rede: 2500 ticks com perda/atraso/repeticao/reordenacao e seed fixa.
- Dois processos Java de clientes locais validados; combate por TCP real derrotou chefe e concedeu exatamente 20 moedas a cada participante uma vez.
- Smokes finais usam os executaveis/runtime dos pacotes: RPG com janela visivel e 62 ticks; dois clientes graficos com IDs distintos, dois jogadores e servidor encerrado com codigo 0.
- Capturas de componentes Swing de janelas em execucao foram inspecionadas. Desktop gerou imagem preta neste ambiente; essa imagem nao foi aceita como homologacao visual. Nenhuma captura falsa foi criada.
- Dominio legado e TODOS os testes existentes antes da rodada permanecem textualmente iguais. Renderer compartilhado ganhou opcoes desativadas por padrao; testes historicos continuam verdes. Arena continua no menu e em distribuicoes anteriores.
- ZIP original 0.36.0-rc1 preservado, SHA-256 {original_hash}.
- Nova arena de compatibilidade 0.36.1-compat-rc1 compilada/empacotada e auditada; EXE aberto com PATH sem Java externo, JVM integrada comprovada e encerramento 0. Evidencia TASK-076-arena-smoke.json.

## Ajustes relevantes encontrados na integracao

1. Inventario/equipamento/comercio/recompensas usam validacao antes do commit, sem perda/duplicacao em operacoes recusadas. Loot cheio fica pendente; limite atingido impede derrota sem recompensa.
2. Recargas, direcao, estados de inimigos/respawn, quests/objetos/regioes e vigor persistem. Loot padrao usa seed por spawn/geracao, mantendo continuidade apos reload.
3. IO e audio ficam fora da simulacao. Save captura comandos pendentes na thread proprietaria e pausa ao voltar ao menu; erro preserva sessao.
4. Regressao de backup reproduzida ANTES da correcao: Properties sintaticamente valido com flag corrompida podia substituir backup bom. Leitura agora valida restauracao semantica antes de copiar. Teste CorruptBackupProtectionTest e log TASK-075-regressao-backup-antes.log documentam a correcao.
5. HUD passou a mostrar nomes reais, atalhos efetivos e aparencias, retirando rotulos de debug do RPG. Dialogos usam grade para manter escolhas acessiveis.
6. Builders/auditor da arena excluem explicitamente prototype/rpg; pacotes separados nao carregam classes de banco/Kryonet, testes ou arte reprovada.
7. Rede fecha handshakes pendentes, rejeita sequencias antigas, expira movimento ausente e limita conexoes/filas/payloads. Servidor nao reinicia apos close; bind malsucedido e limpo.
8. Primeiro smoke PowerShell confundiu codigo de saida indisponivel com falha; handles foram preservados antes da espera e a nova execucao comprovou codigo 0. Logs anteriores mantidos. Primeiro bot TCP foi ajustado para flanquear e esperar os dois participantes, respeitando colisao e regra de contribuicao.

## Coop experimental separado

0.76.0-rc1: servidor TCP headless, localhost por padrao, duas sessoes, snapshots 10 Hz, cliente visual e interpolacao apenas de apresentacao. Movimento/dano/vigor/cooldown/colisao resolvidos pelo servidor. Passo Relampago e Criostase usam os servicos reais existentes. Chefe compartilhado, Freeze e recompensa por participacao, sem importar saves offline.

Limites: um mapa online; sem portais online ou persistencia de contas. LAN privada existe somente com opcao explicita; NAO houve teste entre computadores. Sem autenticacao/TLS de producao. O rodape generico do renderer coop ainda contem uma dica da campanha offline; controles/servidor corretos aparecem no painel lateral. Esse experimento nao e MMORPG completo e nao deve substituir o pacote offline.

Nao houve acesso a banco real, migracao, alteracao de credenciais, push, upload, publicacao, porta publica, firewall global ou servico do sistema. Sprites pendentes/reprovados continuam fora das distribuicoes de jogo. Tons foram sintetizados localmente, sem assets de licenca desconhecida.

## Situacao por tarefa

Validada significa implementacao e automacao local aprovadas, sem alegar homologacao humana. 38 etapas validadas; 071 e 074 parciais nos pontos explicitados.

| Tarefa | Entrega | Situacao |
| --- | --- | --- |
{table}

O marco A foi exercitado em OfflineCycleTest na TASK-058: portais da TASK-057 eram necessarios ao ciclo entre regioes. O pacote integrado foi criado apos essa dependencia, em vez de declarar fluxo completo prematuramente na TASK-056. O marco B foi exercitado na TASK-066; a revisao final de backup gerou o patch 0.66.1-rc1.

## Pacotes e documentos

- Offline: dist/{packages[0]['package']}
- SHA-256 offline: {packages[0]['sha256']}
- Coop: dist/{packages[1]['package']}
- SHA-256 coop: {packages[1]['sha256']}
- Manifestes e arquivos .sha256 acompanham cada ZIP; auditorias TASK-076-auditoria-rpg/coop.json conferem todos os arquivos internos.
- Relatorios por tarefa: TASK-NNN.md, Spirit_Gate_TASK-NNN.pdf, TASK-NNN-testes.txt, TASK-NNN.diff e logs.
- Inventario: INVENTARIO-ALTERACOES-TASK-037_076.json, {len(changed)} arquivos de codigo/scripts/recursos adicionados ou ajustados, com hashes.
- Roteiros: ROTEIRO-RPG-OFFLINE.md e ROTEIRO-COOP-EXPERIMENTAL.md; incluidos nos respectivos pacotes.
- ZIP documental final: dist/SpiritGate-Documentos-TASK-037_076-r2.zip. Versoes anteriores dos checkpoints/documentos foram preservadas.
- Checkpoints JSON antes/depois em .task-checkpoints; arquivos recuperaveis da base e da entrega final consolidados sem caches/target gerados. Assets pendentes entram somente no checkpoint de desenvolvimento, nunca no jogo.

## Avaliacao humana ainda necessaria

Jogar o roteiro completo na interface, testar redimensionamento/atalhos/audio em outro Windows e experimentar duas maquinas numa LAN privada configurada. A campanha tem arte geometrica, tres regioes compactas, duas missoes e um chefe; e uma base offline funcional para testes privados. Nao foram aprovados novos sprites nem release publica.
'''
    final = DOCS / 'RELATORIO-FINAL-TASK-037_076.md'
    final.write_text(report, encoding='utf-8')
    gerar_pdf(final, DOCS / 'Spirit_Gate_Relatorio_Final_TASK-037_076.pdf', 'Expansao TASK-037 a TASK-076')
    continuity = '''# Continuidade - 19/10/2026

Ultima etapa executada: TASK-076, candidatos privados validados automaticamente. TASK-071 parcial quanto a LAN entre maquinas; TASK-074 parcial quanto a transicoes online inexistentes. Nao declarar multiplayer completo.

Principal entrega: RPG offline 0.66.1-rc1. Abrir jogar-rpg-offline.cmd. Saves por UUID junto ao EXE em saves/characters; preferencias/controles separados. Menu salva antes de sair. Arena antiga preservada.

Coop 0.76.0-rc1 e separado; iniciar servidor manualmente. Nunca importar saves offline nem iniciar LAN automaticamente.

Ultimo checkpoint: .task-checkpoints/TASK-076-after.json e checkpoint final ZIP recuperavel. Antes de nova alteracao, criar novo checkpoint sem sobrescrever os existentes.

Comandos de verificacao (na raiz Projeto-K):

python tools/qualidade.py --offline --task retomada-expansao
python tools/consolidar_expansao.py reports

Consultar auditorias e hashes em docs/TASK-076-auditoria-*.json e sidecars dos ZIPs. Build local offline: powershell -File tools/build-rpg-portable.ps1 -Version NOVA-VERSAO. Build coop separado: tools/build-coop-portable.ps1. Nao reutilizar versao nem substituir artefatos anteriores.

Pendencias: homologacao humana em outro Windows; duas maquinas LAN; portais online; ampliar conteudo e autoria dos mapas. Rodape do cliente coop ainda tem dica generica da campanha offline. Nenhuma TASK-077 autorizada; nao iniciar automaticamente.
'''
    (DOCS / 'CONTINUIDADE-TASK-037_076.md').write_text(continuity, encoding='utf-8')
    roadmap = '''# Roadmap apos TASK-076 - sujeito a nova autorizacao

1. Homologar campanha offline pela UI em outro Windows; corrigir apenas falhas reproduzidas antes de ampliar escopo.
2. Melhorar autoria visual das tres regioes, acessibilidade/localizacao e arte somente apos aprovacao explicita dos assets.
3. Ampliar NPCs, missoes, equipamentos e encontros mantendo catalogos/saves versionados e transacoes atomicas.
4. Homologar LAN entre duas maquinas privadas. Nenhuma exposicao publica ou mudanca de firewall automatica.
5. Projetar transicoes e ciclo de vida de regioes online antes de compartilhar persistencia. Definir contrato de contas, seguranca e dados sem reutilizar login legado inseguro.
6. Completar apresentacao de habilidades/rodape do cliente coop e telemetria local de falhas; manter pacote offline independente.
7. Considerar transporte/autenticacao de producao somente com desenho e auditoria dedicados. Nao alegar seguranca de producao do prototipo.

Nenhuma destas acoes inicia TASK-077 sem nova instrucao.
'''
    (DOCS / 'ROADMAP-APOS-TASK-076.md').write_text(roadmap, encoding='utf-8')
    (DOCS / 'TASK-076-preservacao.json').write_text(json.dumps({'legacyDomainUnchanged': legacy_domain, 'legacyTestsUnchanged': legacy_tests, 'baselineZipSha256': original_hash}, indent=2), encoding='utf-8')
    print(json.dumps({'tasks': len(tasks), 'sourceChanges': len(changed), 'java': quality['checks']['java']['totals'], 'python': quality['checks']['python']['tests']}))

def package():
    for number in range(37, 77):
        for name in (f'TASK-{number:03}.md', f'Spirit_Gate_TASK-{number:03}.pdf', f'TASK-{number:03}-testes.txt', f'TASK-{number:03}.diff'):
            assert (DOCS / name).is_file(), name
        for phase in ('before', 'after'):
            assert (ROOT / f'.task-checkpoints/TASK-{number:03}-{phase}.json').is_file()
    outputs = []
    document_zip = ROOT / 'dist/SpiritGate-Documentos-TASK-037_076-r2.zip'
    assert not document_zip.exists(), 'Pacote documental ja existe'
    with zipfile.ZipFile(document_zip, 'w', zipfile.ZIP_DEFLATED) as out:
        for path in sorted(DOCS.rglob('*')):
            if path.is_file():
                out.write(path, path.relative_to(ROOT).as_posix())
    outputs.append(document_zip)
    baseline = json.loads((ROOT / '.task-checkpoints/TASK-037-before.json').read_text(encoding='utf-8'))
    base_zip = ROOT / '.task-checkpoints/SpiritGate-TASK-037-base-recuperavel.zip'
    if not base_zip.exists():
      with zipfile.ZipFile(base_zip, 'w', zipfile.ZIP_DEFLATED) as out:
        names = set()
        for name, text in baseline.items():
            out.writestr(name, text.encode('utf-8')); names.add(name)
        for path in sorted((ROOT / 'assets').rglob('*')):
            if path.is_file() and path.relative_to(ROOT).as_posix() not in names:
                out.write(path, path.relative_to(ROOT).as_posix())
        for path in sorted(DOCS.iterdir()):
            match = re.search(r'TASK[-_](\d{3})', path.name)
            if path.is_file() and match and int(match[1]) <= 36 and path.relative_to(ROOT).as_posix() not in names:
                out.write(path, path.relative_to(ROOT).as_posix())
        for path in sorted((ROOT / 'dist').glob('SpiritGate-0.36.0-*')):
            if path.is_file(): out.write(path, path.relative_to(ROOT).as_posix())
    outputs.append(base_zip)
    final_zip = ROOT / '.task-checkpoints/SpiritGate-TASK-076-checkpoint-recuperavel-r2.zip'
    assert not final_zip.exists()
    with zipfile.ZipFile(final_zip, 'w', zipfile.ZIP_DEFLATED) as out:
        paths = source_files()
        for folder in ('docs', 'assets'):
            paths += [p for p in (ROOT / folder).rglob('*') if p.is_file()]
        paths += list((ROOT / '.task-checkpoints').glob('TASK-0[3-7][0-9]-*.json'))
        for audit in ('TASK-076-auditoria-rpg.json', 'TASK-076-auditoria-coop.json'):
            name = read(audit)['package']
            paths += [ROOT / 'dist' / name, ROOT / 'dist' / (name+'.manifest.json'), ROOT / 'dist' / (name+'.sha256')]
        for path in sorted(set(paths)):
            out.write(path, path.relative_to(ROOT).as_posix())
        out.writestr('CHECKPOINT-LEIA-ME.txt', 'Fontes, testes, scripts, documentos, assets de desenvolvimento e pacotes finais. Assets pendentes NAO sao assets de producao. Excluidos caches Maven e target gerado; releases anteriores permanecem em dist.\n')
    outputs.append(final_zip)
    manifest = []
    for path in outputs:
        with zipfile.ZipFile(path) as archive:
            assert archive.testzip() is None
            if path != document_zip:
                assert 'server/pom.xml' in archive.namelist(), 'POM de build ausente no checkpoint'
                assert 'server/src/main/java/com/projetok/prototype/DebugLauncher.java' in archive.namelist()
        sha = digest(path)
        Path(str(path)+'.sha256').write_text(sha+'  '+path.name+'\n', encoding='ascii')
        manifest.append({'path': path.relative_to(ROOT).as_posix(), 'bytes': path.stat().st_size, 'sha256': sha})
    (ROOT / 'dist/SpiritGate-Entrega-TASK-037_076-manifest.json').write_text(json.dumps(manifest, indent=2), encoding='utf-8')
    print(json.dumps(manifest, indent=2))

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('action', choices=['reports', 'package'])
    args = parser.parse_args()
    reports() if args.action == 'reports' else package()
