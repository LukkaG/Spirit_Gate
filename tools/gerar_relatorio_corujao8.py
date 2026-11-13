"""Consolida resultados existentes e gera o relatório VIII com capturas reais."""
from pathlib import Path
import hashlib
import json
import re
import shutil
from gerar_relatorio_corujao7 import write_pdf

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / "docs"
CP = ROOT / ".task-checkpoints/TASK-214-222/corujao-viii"
GUI = ROOT / ".task-checkpoints/TASK-221-GUI/corujao-viii"
PACKAGE = ROOT / "dist/SpiritGate-CorujaoVIII-0.222.0-private"

TASKS = {
    214: ("Inventário em grid", "Slots com moldura, ícones do sistema visual existente, quantidade, nome, raridade, seleção e descrição. Busca por nome, categorias Armadura/Armas/Itens de drop/Todos e organização por categoria/nome. Consumíveis, materiais e itens de missão ficam em Itens de drop. Organizar altera somente a visualização local; equipar, consumir, desequipar e recolher continuam enviando pedidos ao servidor.", "NetworkInventoryPanel.java, GameHudStyle.java", "Task214To220UiTest: filtros, quantidades, organização, busca vazia, sincronização e equipar pelo grid com dois clientes TCP e inventários privados. GamePanelsIntegrationTest: painel real, foco e três resoluções."),
    215: ("Diário de missões", "Painel vertical com lista selecionável, estado, progresso confirmado, resumo do objetivo e recompensa já existentes. A seleção é preservada na atualização. Aceitar e entregar refletem o estado confirmado; distância do NPC e validade continuam verificadas pelo servidor. Não foram criadas missões ou recompensas.", "NetworkQuestPanel.java, RpgNetworkClientLauncher.java", "Task214To220UiTest: AVAILABLE/ACTIVE/READY/COMPLETED/UNAVAILABLE. GamePanelsIntegrationTest e Task212PlayableUiTest: integração real. Regressões de jornada e protocolo na suíte completa."),
    216: ("Configurações", "Consolidação do painel integrado do adendo: seções, volume geral, mute, redução de efeitos, resolução, keybinds, captura e validação de conflitos. Aplicar confirma e Cancelar/X/ESC descartam. Preferências suportadas mantêm o formato anterior. Atalhos e resolução são de sessão. A redução de efeitos é lida ao abrir a partida e também limita o deslocamento do dano flutuante. Música e efeitos separados não existem na base e não foram simulados com sliders sem função.", "NetworkSettingsPanel.java, RpgNetworkClientLauncher.java, NetworkCombatPresentation.java", "GamePanelsIntegrationTest: arquivo isolado, Cancelar sem alteração, Aplicar com persistência e remapeamento explícito, captura de SPACE e funcionamento em três resoluções. Regressões de áudio/preferências existentes na suíte completa."),
    217: ("Identidade visual compartilhada", "GameHudStyle compartilha navy/ciano com SpiritTheme e padroniza botões, foco, seleção, caixas de seleção, combos, sliders, listas, rolagem e barras de progresso. A seta do combo é desenhada em Java2D, sem depender de glifo ausente. Os cinco painéis internos possuem título, fechamento e transparência reais. Os três blocos inferiores e reservas sem ações fictícias continuam presentes.", "GameHudStyle.java, GamePanels.java, NetworkInventoryPanel.java, NetworkQuestPanel.java", "Capturas dos cinco painéis nas três resoluções, alfa RGBA e limites verificados em GamePanelsIntegrationTest. Task213HudSwingTest preserva a composição aprovada."),
    218: ("Feedback de combate", "Valores de HP e energia continuam exatos; apresentação suavizada sem mudar o modelo. Hotbar exibe recarga, seleção, bloqueio por nível, falta de energia e falta de poção, com descrições acessíveis. Ataque básico continua possível com energia zero. Arcos de ataque e alvo usam ciano; dano flutuante tem contorno escuro e contraste. Redução de movimento mantém os números estáveis. Estados indisponíveis limpam indicadores antigos.", "NetworkHotbar.java, NetworkCombatHud.java, NetworkCombatPresentation.java, RpgNetworkClientLauncher.java", "Task214To220UiTest: energia zero, recarga real e poções. GamePanelsIntegrationTest: ataque com foco em outros botões e hits observados pelos dois clientes. Task213HudTest e regressões de combate completas."),
    219: ("Minimapa e utilitários", "Minimapa mantém proporção, mapa recebido, obstáculos, portais e área de câmera, com legenda de jogador/aliados/inimigos e enquadramento ajustado. Atalho remapeado atualiza o tooltip. Perfil/inventário, ações centrais e livro/menu/configurações permanecem separados; o botão do painel aberto ganha destaque.", "NetworkMinimap.java, NetworkGameHud.java, GameHudStyle.java", "Task213HudTest: projeções e câmera. Task213HudSwingTest: mostrar/ocultar minimapa, atalhos e layout. Capturas reais nas três resoluções."),
    220: ("Polimento e teclado", "Estados de hover, pressão, foco e categoria ativa; contraste de botões desabilitados e barras. Um painel ativo por vez, modalidade das configurações, fechamento consistente e arraste limitado. Textos não enviam gameplay; SPACE não clica no botão focado; ENTER ativa o controle; TAB/SHIFT+TAB navegam. Corrigido retorno R após derrota, sem liberar movimento/ataque. Sincronização de revisão também bloqueia gameplay.", "GameInputRouter.java, GamePanels.java, GameHudStyle.java, RpgNetworkClientLauncher.java", "GameInputRoutingTest: eventos AWT com foco real, repetição, texto, captura, modalidades, isolamento de outra janela, quarentena e retorno após derrota. GamePanelsIntegrationTest: arraste, reabertura, descarte e dois clientes."),
    221: ("Regressões", "Compilação e suíte Java completa, rodada GUI serializada, casos de inventário/missões/combate e validação com servidor e dois clientes. Launcher, offline, reconexão, cancelamento e Studio cobertos pelas regressões existentes. Evidências preservadas em checkpoints novos.", "Task214To220UiTest.java, GameInputRoutingTest.java, GamePanelsIntegrationTest.java", "Resultados consolidados abaixo; a suíte GUI cobre os abortos gráficos da execução headless. Não somar rodadas como testes únicos."),
    222: ("Pacote privado", "Dois pacotes novos 0.222.0-private, Anfitrião e Convidado, com JAR recompilado e runtime/assets anteriores verificados. Canal comum local, sem feed remoto. Manifest e ZIPs verificados, servidor e dois clientes em três processos com os runtimes distribuídos; interface também aberta a partir do JAR empacotado. Distribuições anteriores preservadas.", "CorujaoEightUiProbe.java, gerar_relatorio_corujao8.py; ferramentas de build/smoke existentes reutilizadas", "package-smoke/result.json e package-ui/result.json; hashes no integracao.json. Não equivale a executar o iniciador EXE ou homologar duas máquinas via Radmin."),
}


def main():
    log = (CP / "java-full-final.log").read_text(encoding="utf8", errors="replace")
    summaries = re.findall(r"JUnit: (\d+) executed, (\d+) passed, (\d+) aborted, (\d+) failures, (\d+) skipped", log)
    assert summaries, "Suíte final ausente"
    executed, passed, aborted, failed, skipped = map(int, summaries[-1])
    assert failed == skipped == 0
    gui = json.loads((GUI / "verification-gui.json").read_text())
    gui_count = 0
    for test in gui["tests"]:
        assert test["exit"] == 0
        counts = re.search(r"JUnit: (\d+) executed, (\d+) passed, (\d+) aborted, (\d+) failures", test["result"])
        n, ok, abort, failures = map(int, counts.groups())
        assert n == ok and abort == failures == 0
        gui_count += n
    integrity = json.loads((CP / "integrity-final.json").read_text())
    assert not integrity["protected_changed"] and not integrity["source_changed_during_tests"]
    smoke = json.loads((ROOT / ".task-checkpoints/TASK-222/corujao-viii/package-smoke/result.json").read_text())
    packaged_ui = json.loads((ROOT / ".task-checkpoints/TASK-222/corujao-viii/package-ui/result.json").read_text())
    assert smoke["exitCodes"] == [0, 0, 0] and packaged_ui["productionFromJar"]
    integration = json.loads((PACKAGE / "integracao.json").read_text())
    for package in integration["packages"]:
        assert hashlib.sha256(Path(package["archive"]).read_bytes()).hexdigest() == package["sha256"]
    results = f"Suíte Java: {executed} executados, {passed} aprovados, {aborted} abortos gráficos, zero falhas e zero ignorados. Rodada GUI: {len(gui['tests'])} classes, {gui_count} execuções aprovadas, zero abortos e falhas. Os totais se sobrepõem."
    status = "Implementada e validada localmente; aceite visual/físico pendente"
    for task, (title, behavior, files, tests) in TASKS.items():
        base = f"# TASK-{task} - {title}\n\n31/10/2026. Autorização explícita para avançar nas tarefas restantes.\n\nEstado: {status}.\n\n{behavior}\n\nArquivos principais: {files}.\n"
        if task == 216:
            base += "\nRessalva de escopo: canais separados de música/efeitos permanecem indisponíveis, pois não são suportados pela base atual. A UI entrega somente controles reais.\n"
        links = "\n[Relatório consolidado](CORUJAO-VIII-RELATORIO-FINAL.md) · [PDF](Spirit_Gate_Corujao_VIII_Relatorio_Completo.pdf) · [Roteiro de uso](CORUJAO-VIII-COMO-TESTAR.md).\n"
        (DOCS / f"TASK-{task}.md").write_text(base + links, encoding="utf8")
        (DOCS / f"TASK-{task}-TESTES.md").write_text(f"# TASK-{task} - testes\n\n{tests}\n\n{results}\n\nEvidências: .task-checkpoints/TASK-221-GUI/corujao-viii/ e .task-checkpoints/TASK-214-222/corujao-viii/. Os testes de teclado injetam eventos na fila AWT, com foco e bindings Swing reais. Não comprovam teclado físico.\n" + links, encoding="utf8")
        (DOCS / f"TASK-{task}-RELATORIO-FINAL.md").write_text(base + f"\nValidação: {tests}\n\n{results}\n\nLimites: revisão visual humana, teclado físico e Radmin entre máquinas pendentes. Escopo visual moderno na partida V6; offline/Coop preservados.\n" + links, encoding="utf8")
    report = """# Spirit Gate - Corujão VIII
## Relatório completo de implementação e validação local

Data: 31/10/2026. Projeto-K, Java 17, Swing/Java2D, servidor TCP autoritativo.
Branch beco. HEAD 6d72c6595dee6dbe4dea832fe6047ee81f51d4a3.

## Resultado

TASK-213, os dois adendos e as TASKs 214-222 estão integrados nos fontes locais e na build privada 0.222.0-private. A autorização posterior para avançar nas tarefas restantes supera a etapa inicial do plano que limitava a execução à 213. Não houve commit, push, release ou publicação oficial.

A conclusão é técnica e local, com uma adaptação explícita na TASK-216: a base não oferece canais separados de música e efeitos. Foram preservados os controles reais de volume geral e mute, sem simular funcionalidades. A escuta do áudio no dispositivo não foi homologada. Não se declara um novo sistema de áudio multiplayer.

A interface nova pertence à partida multiplayer V6. Launcher, atualizador, offline e Coop mantêm seus fluxos anteriores; World Studio não foi reformulado. Protocolo, movimento, dano, cooldowns, progressão, inventário autoritativo, missões e saves pessoais não foram alterados. Nenhuma dependência nova foi instalada.

## TASK-213 e adendos preservados

Mundo ocupa toda a área do jogo. Minimap superior direito, HP e energia inferiores alinhados, perfil/inventário à esquerda, hotbar no centro, missões/menu/configurações à direita. Quatro ações funcionais e reservas laterais sem ações inventadas; elemento decorativo pesado entre as barras removido.

Os cinco painéis são componentes internos, sem molduras nativas individuais. Têm título e fechamento próprios, navy com alfa real, contorno ciano, arraste limitado e foco restaurado. A janela principal continua com sua moldura normal. Configurações são modais; outros painéis são alternados sem acumular janelas.

O roteador é limitado à janela da partida. TAB navega, ENTER ativa o controle e SPACE ataca apesar do foco em outro botão durante gameplay. Edição de texto, sliders, combos, captura e modalidade não enviam combate/movimento. SHIFT isolado age ao soltar, preservando SHIFT+TAB. Teclas pressionadas não reaparecem após mudança de contexto ou reconexão. Dispatcher/listeners são descartados ao fechar.

"""
    for task, (title, behavior, files, tests) in TASKS.items():
        report += f"## TASK-{task} - {title}\n\n{behavior}\n\nArquivos: {files}.\n\nValidação: {tests}\n\n"
    report += f"## Resultados finais\n\n{results}\n\nCompilação: {gui['sources']} fontes principais e {gui['testFiles']} arquivos de teste. A suíte completa inclui os testes preexistentes de protocolos, inventário, jornada de missão, combate, offline, áudio/preferências e Studio. A rodada GUI inclui launcher, cancelamento, reconexão e ciclo de vida, além dos testes específicos do ciclo.\n\n"
    report += """Foram verificados os cinco painéis em 1366x768, 1000x700 e 640x480, com fechamento acessível, transparência RGBA, arraste nos limites, reabertura sem crescimento de componentes e remoção de listeners. Servidor e dois clientes reais confirmaram hits, movimento e privacidade do inventário. O teste de grid enviou EQUIP e aguardou o estado confirmado; organização/filtros não alteraram a ficha do outro cliente.

Hotbar foi verificada com nível insuficiente, energia zero, recarga e quantidade de poção. Ataque básico não passou a exigir energia. Estados de missão AVAILABLE/ACTIVE/READY/COMPLETED/UNAVAILABLE foram testados com fichas sintéticas para cobrir apresentação; as capturas da partida mostram o estado efetivamente recebido do servidor.

Uma tentativa intermediária falhou na compilação do novo teste por import ausente e ambiguidade de update(null). Outra expectativa dependia da ordem não garantida de um Map no teste de filtros. As fixtures foram corrigidas e a rodada final passou. Essas tentativas não foram contadas como aprovação. Não houve ajuste do servidor para fazer a UI passar.

Comandos principais:

    python tools/run-local-tests.py
    python tools/corujao7_verify.py TASK-221-GUI <classes registradas no verification-gui.json> --gui --cycle corujao-viii
    python tools/build-multiplayer-update.py dist/SpiritGate-CorujaoVII-0.212.0-private/Anfitriao/canal .task-checkpoints/TASK-222/corujao-viii/package-build dist/SpiritGate-CorujaoVIII-0.222.0-private --version 0.222.0-private
    python tools/smoke_corujao7_package.py dist/SpiritGate-CorujaoVIII-0.222.0-private .task-checkpoints/TASK-222/corujao-viii/package-smoke

## Pacotes privados e uso

Extraia integralmente o ZIP de cada papel em pasta distinta e abra SpiritGate-Launcher.exe da raiz. O anfitrião hospeda e o convidado conecta; o convidado continua impedido de hospedar. Não abra o EXE interno em versions. Dados pessoais não foram incluídos. Leia CORUJAO-VIII-COMO-TESTAR.md.

"""
    for package in integration["packages"]:
        report += f"{package['role']}: {Path(package['archive']).relative_to(ROOT).as_posix()}\nSHA-256: {package['sha256']}\n\n"
    report += f"Manifest comum SHA-256: {integration['manifestSha256']}.\n\n"
    report += """O smoke verificou 391 arquivos por papel contra o manifest. Servidor e dois clientes executaram em três processos usando os runtimes e JARs dos pacotes, com códigos de saída zero. A prova gráfica abriu duas janelas a partir do JAR distribuído, carregou os assets empacotados e capturou inventário, missões e configurações, sem janelas nativas secundárias. Não foi executado o iniciador EXE nesta prova.

## Integridade

"""
    report += f"{integrity['protected_count']} arquivos preexistentes protegidos em assets, saves e dist permaneceram idênticos ao início. Os novos pacotes foram acrescentados em diretório próprio. As fontes não mudaram durante a validação final. As exclusões históricas do workspace foram preservadas. Saídas sintéticas geradas pelos testes foram arquivadas; evidências antigas de mapa com timestamps regenerados foram restauradas após guardar as novas saídas.\n\n"
    report += "Arquivos alterados nesta continuação, relativos à raiz:\n\n" + "\n".join("- " + p.replace("\\", "/") for p in integrity["source_changed"] + integrity["source_new"]) + "\n\n"
    report += """Ferramentas: CorujaoEightUiProbe.java e gerar_relatorio_corujao8.py novos; write_pdf de gerar_relatorio_corujao7.py passou a aceitar capturas e identificação opcionais, mantendo os padrões anteriores. Ferramentas de build e smoke reutilizadas. Documentação: três arquivos por TASK 214-222, plano/progresso/pendências/relatório do ciclo, roteiro e capturas reais. O patch incremental e os hashes ficam no checkpoint do ciclo.

## Limitações e próximas recomendações

- Homologação artística e legibilidade humana nas capturas e no jogo pendentes.
- Teclado físico no Windows não foi comprovado. A prova anterior mostrou que Robot não entrega eventos nativos neste ambiente. A cobertura usa EventQueue, KeyEventDispatcher, foco e bindings Swing reais; não chama somente ActionMap para simular o resultado. Não equivale a digitação física.
- Radmin entre dois computadores e execução do iniciador nativo EXE ainda exigem conferência humana. Loopback e interface empacotada foram comprovados localmente.
- Música e efeitos não possuem canais separados na base atual; não foram criados sliders fictícios ou novo backend de áudio. Volume/mute mantêm o contrato de preferências existente. Confirmar áudio ouvido e requisitos futuros antes de expandir esse sistema.
- Atalhos personalizados e resolução são de sessão. Preferências existentes mantêm schema e localização. Progresso multiplayer segue efêmero; reconectar abre nova sessão, sem promessa de recuperação de XP/itens.
- A atualização visual não foi estendida a offline/Coop ou interfaces do Studio; seus fluxos foram preservados e cobertos por regressões.
- Nenhuma medição nova de FPS ou homologação de longa duração foi realizada. A atualização usa o timer existente, sem um timer por slot/painel.

Recomenda-se revisar a build privada com teclado físico e dois computadores, verificar brilho/foco nas três resoluções e registrar o aceite visual antes de qualquer publicação. Não há publicação ou feed remoto configurados nesta entrega.

## Evidências e continuidade

- .task-checkpoints/TASK-214-222/corujao-viii/java-full-final.log
- .task-checkpoints/TASK-221-GUI/corujao-viii/verification-gui.json e ui-regression/
- .task-checkpoints/TASK-214-222/corujao-viii/integrity-final.json e implementation.patch
- .task-checkpoints/TASK-222/corujao-viii/package-build/build-evidence.json
- .task-checkpoints/TASK-222/corujao-viii/package-smoke/result.json
- .task-checkpoints/TASK-222/corujao-viii/package-ui/result.json e capturas
- dist/SpiritGate-CorujaoVIII-0.222.0-private/integracao.json

Os relatórios anteriores da TASK-213 e dos adendos mantêm seus números históricos. O estado atual é o consolidado aqui; não somar rodadas repetidas como testes únicos. Os documentos anteriores de progresso/pendências/relatório foram preservados em before-docs no checkpoint desta continuação.
"""
    (DOCS / "CORUJAO-VIII-RELATORIO-FINAL.md").write_text(report, encoding="utf8")
    pending = "# Corujão VIII - pendências após TASK-222\n\n" + "\n".join(report.split("## Limitações e próximas recomendações\n\n")[1].split("## Evidências")[0].splitlines())
    pending += "\n\n[Relatório final](CORUJAO-VIII-RELATORIO-FINAL.md). Não há tarefa de implementação restante no escopo visual suportado; canais separados de áudio constituem a ressalva da TASK-216. Homologação humana permanece pendente.\n"
    (DOCS / "CORUJAO-VIII-PENDENCIAS.md").write_text(pending, encoding="utf8")
    progress = "# Corujão VIII - progresso final\n\n31/10/2026. Continuação das TASKs 214-222 expressamente autorizada.\n\n| Tarefa | Estado |\n|---|---|\n| 213 e adendos | Implementados e validados localmente |\n"
    for task, (title, *_) in TASKS.items():
        progress += f"| {task} - {title} | {'Entregue no escopo suportado; sem canais separados de áudio' if task == 216 else 'Implementada e validada localmente'} |\n"
    progress += f"\n{results}\n\nDois pacotes privados 0.222.0-private gerados e verificados; nenhuma publicação. Aceite visual, teclado físico e Radmin entre computadores pendentes.\n\n[Relatório](CORUJAO-VIII-RELATORIO-FINAL.md) · [PDF](Spirit_Gate_Corujao_VIII_Relatorio_Completo.pdf) · [Roteiro](CORUJAO-VIII-COMO-TESTAR.md).\n"
    (DOCS / "CORUJAO-VIII-PROGRESSO.md").write_text(progress, encoding="utf8")
    captions = [("inventory-1366", "Inventário em grid: tipos e quantidades confirmadas"), ("inventory-640", "Inventário compacto: categorias e ações existentes"), ("journal-1000", "Diário vertical: missão recebida do servidor"), ("settings-1366", "Configurações integradas: controles reais"), ("settings-640", "Configurações compactas: rolagem e rodapé"), ("client-0-1366", "HUD em três blocos: mundo e recursos reais")]
    captures = []
    for filename, caption in captions:
        source = GUI / "ui-regression" / (filename + ".png")
        target = DOCS / ("CORUJAO-VIII-" + filename + ".png")
        shutil.copy2(source, target)
        captures.append((target, caption))
    pdf = write_pdf(report, DOCS / "Spirit_Gate_Corujao_VIII_Relatorio_Completo.pdf", captures=captures, cycle="VIII")
    (CP / "pdf-verification.json").write_text(json.dumps(pdf, indent=2), encoding="utf8")
    print(json.dumps(pdf, indent=2))


if __name__ == "__main__":
    main()
