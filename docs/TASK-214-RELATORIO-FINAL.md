# TASK-214 - Inventário em grid

31/10/2026. Autorização explícita para avançar nas tarefas restantes.

Estado: Implementada e validada localmente; aceite visual/físico pendente.

Slots com moldura, ícones do sistema visual existente, quantidade, nome, raridade, seleção e descrição. Busca por nome, categorias Armadura/Armas/Itens de drop/Todos e organização por categoria/nome. Consumíveis, materiais e itens de missão ficam em Itens de drop. Organizar altera somente a visualização local; equipar, consumir, desequipar e recolher continuam enviando pedidos ao servidor.

Arquivos principais: NetworkInventoryPanel.java, GameHudStyle.java.

Validação: Task214To220UiTest: filtros, quantidades, organização, busca vazia, sincronização e equipar pelo grid com dois clientes TCP e inventários privados. GamePanelsIntegrationTest: painel real, foco e três resoluções.

Suíte Java: 698 executados, 673 aprovados, 25 abortos gráficos, zero falhas e zero ignorados. Rodada GUI: 13 classes, 36 execuções aprovadas, zero abortos e falhas. Os totais se sobrepõem.

Limites: revisão visual humana, teclado físico e Radmin entre máquinas pendentes. Escopo visual moderno na partida V6; offline/Coop preservados.

[Relatório consolidado](CORUJAO-VIII-RELATORIO-FINAL.md) · [PDF](Spirit_Gate_Corujao_VIII_Relatorio_Completo.pdf) · [Roteiro de uso](CORUJAO-VIII-COMO-TESTAR.md).


## HUD compacta e ficha — 01/11/2026

Evolução 0.224.0-private: ícones menores sem caixas permanentes, detalhes de item ao clicar, Personagem separado de Habilidades, equipamentos e distribuição de Força/Inteligência/Agilidade no servidor. Força usa a fórmula existente; INT/AGI ficam reservadas às próximas técnicas, conforme indicado na interface. Pontos e progresso são de sessão. Validação: 682 testes gerais aprovados e 49 execuções GUI aprovadas, sem falhas. [Relatório e pacotes](HUD-COMPACTA-RELATORIO.md) · [PDF](Spirit_Gate_HUD_Compacta_Relatorio.pdf). As versões e contagens anteriores acima permanecem históricas.

