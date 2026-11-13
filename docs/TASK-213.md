# TASK-213 — HUD principal do Corujão VIII

Revisão visual posterior aplicada: [adendo obrigatório](CORUJAO-VIII-ADENDO-HUD.md).
Três blocos, hotbar ampliada e barras alinhadas; o logo entre HP e energia foi
retirado. A descrição original abaixo registra a primeira entrega. Captura atual:
[HUD refinada](TASK-213-ADENDO-HUD-1366.png).

Data: 31/10/2026. Branch `beco`, HEAD
`6d72c6595dee6dbe4dea832fe6047ee81f51d4a3`. Implementada e validada localmente.
Escopo desta execução: somente TASK-213, conforme a primeira ação do prompt.
O aceite funcional do Corujão VII foi informado pelo responsável no pedido;
não é apresentado como nova homologação física desta tarefa.

## Auditoria e arquitetura anterior

Foram examinados RpgNetworkClientLauncher, NetworkCombatHud, NetworkHotbar,
NetworkInventoryPanel, NetworkQuestPanel, NetworkProgressLabel, DebugPanel,
ArenaViewport, SpiritGateEntry, SpiritTheme, RpgLauncher, NavigationMap,
PreferencesDialog, RpgPreferences e RpgControls, além dos testes associados.

No multiplayer, o launcher reunia controles no topo, telemetria lateral,
missão inline e barras na parte inferior. O viewport reservava espaço para
essa apresentação histórica. O fluxo atual Radmin usa cliente V6, com recursos
e recargas confirmados pelo servidor. O offline possui NavigationMap, dependente
de RpgRuntime; não pode ser ligado diretamente à sessão TCP sem usar dados errados.

A entrada aprovada usa navy (6,16,29), ciano (112,225,244), texto claro,
gradientes escuros e emblema de portal/losango. Essa linguagem foi reproduzida
em componentes Java2D nativos, sem gerar imagens ou regravar os assets.

## Estratégia aplicada

A nova composição é opt-in para sessões V6. O mundo ocupa a superfície da
janela, sem a reserva lateral/rodapé de debug. A câmera mantém zoom e coordenadas
coerentes por transformação pura; não modifica física, posição ou simulação.
Legados, offline e Coop conservam a apresentação anterior por padrão.

NetworkGameHud sobrepõe os elementos em JLayeredPane:

- Minimapa superior direito, projetado da região e geometria sincronizadas.
  Mantém proporção, mostra câmera, jogador, aliados, inimigos, NPCs e portais
  já recebidos; não revela entidades de outras regiões. M mostra/oculta.
- Identidade do personagem e região no alto à esquerda. Diagnósticos ficam
  ocultos na apresentação normal e continuam disponíveis em F2.
- Barras HP/energia centralizadas na base, valores reais e suavização apenas
  do preenchimento visual, com nome/emblema Spirit Gate no centro.
- Quatro slots para as ações instaladas: ataque racial, Shunpo ofensivo,
  Shunpo defensivo e poção. Seleção, hover/foco, bloqueio de nível e recargas
  usam a ficha existente. Nenhuma habilidade foi inventada para preencher slots.
- Perfil e inventário à esquerda; livro de missões, menu e configurações à
  direita. Botões têm nomes acessíveis e tooltips. J abre missões; ESC abre menu.

O perfil mantém progressão e escolha de domínio existentes. O menu preserva
reconexão, zoom e personalização compatível. A janela de inventário é a anterior.
O livro abre o painel de missão anterior. Configurações reaproveitam o diálogo
existente, com carregamento em worker e sem gravar ao abrir/fechar; não acrescentam
controles fictícios de música ou keybinds. A reformulação dessas telas pertence
às TASKs 214–216, que não foram executadas.

Abrir painéis pela HUD libera o input mantido. As janelas são pertencentes à
partida e fecham com ela. As ações e callbacks de reconexão continuam os mesmos.

## Arquivos

Novos fontes: GameHudStyle, NetworkMinimap e NetworkGameHud, no pacote multiplayer.
Fontes existentes: RpgNetworkClientLauncher, NetworkCombatHud, NetworkHotbar,
ArenaViewport, DebugPanel e PreferencesDialog. Testes novos: Task213HudTest e
Task213HudSwingTest. Task212PlayableUiTest foi adaptado para abrir o diário pelo
livro/J e continuar verificando o painel real, em vez de exigir sua posição antiga.

tools/corujao7_verify.py ganhou seleção explícita do ciclo e raiz de capturas
de teste. O padrão antigo continua válido; regressões não sobrescrevem capturas
históricas de outra tarefa. Não foram instaladas dependências.

## Riscos e limites

Os riscos principais eram redimensionamento, sobreposição, foco e perda de
controles. Foram exercitados com janelas reais em três tamanhos e dois clientes.
O mapa usa marcadores vetoriais de dados, não uma miniatura pintada do cenário.
O zoom mantém letterbox quando a superfície excede o mundo, preservando a escala.

Inventário, diário e configurações ainda têm os conteúdos/estrutura anteriores;
esta entrega muda seu acesso, sem antecipar a reformulação das próximas tarefas.
Não há alteração de protocolo, regras de combate, inventário, balanceamento ou
save. Revisão artística humana e teste Radmin físico não foram realizados aqui.

## Executar e revisar

Use `rodar-prototipo.cmd`, opções Radmin no Portal, com dois personagens.
Esses comandos recompilam os fontes atuais; os EXEs/pacotes anteriores não
receberam esta HUD. Novo pacote do Corujão VIII fica para a TASK-222.

[Captura 1366×768](TASK-213-HUD-1366.png) ·
[Captura 640×480](TASK-213-HUD-640.png).
Resultados em [TASK-213-TESTES.md](TASK-213-TESTES.md).


## Adendo obrigatório de janelas e teclado - 31/10/2026

Implementado no V6: cinco painéis internos translúcidos, roteamento contextual de teclado e configurações com Aplicar/Cancelar. Base das TASKs 214-217 adiantada por determinação do novo adendo; seus escopos completos continuam pendentes. TASK-216 tem implementação parcial de apresentação e controles. Regressão: 695 testes Java (670 aprovados, 25 abortos gráficos), complementada por 33 execuções GUI aprovadas, sem falhas. Teclado físico e revisão visual humana permanecem pendentes. Ver [relatório do adendo](ADENDO-JANELAS-HOTKEYS-RELATORIO.md) e [PDF](Spirit_Gate_Adendo_Janelas_Hotkeys.pdf).
