# TASK-215 - Diário de missões

31/10/2026. Autorização explícita para avançar nas tarefas restantes.

Estado: Implementada e validada localmente; aceite visual/físico pendente.

Painel vertical com lista selecionável, estado, progresso confirmado, resumo do objetivo e recompensa já existentes. A seleção é preservada na atualização. Aceitar e entregar refletem o estado confirmado; distância do NPC e validade continuam verificadas pelo servidor. Não foram criadas missões ou recompensas.

Arquivos principais: NetworkQuestPanel.java, RpgNetworkClientLauncher.java.

Validação: Task214To220UiTest: AVAILABLE/ACTIVE/READY/COMPLETED/UNAVAILABLE. GamePanelsIntegrationTest e Task212PlayableUiTest: integração real. Regressões de jornada e protocolo na suíte completa.

Suíte Java: 698 executados, 673 aprovados, 25 abortos gráficos, zero falhas e zero ignorados. Rodada GUI: 13 classes, 36 execuções aprovadas, zero abortos e falhas. Os totais se sobrepõem.

Limites: revisão visual humana, teclado físico e Radmin entre máquinas pendentes. Escopo visual moderno na partida V6; offline/Coop preservados.

[Relatório consolidado](CORUJAO-VIII-RELATORIO-FINAL.md) · [PDF](Spirit_Gate_Corujao_VIII_Relatorio_Completo.pdf) · [Roteiro de uso](CORUJAO-VIII-COMO-TESTAR.md).
