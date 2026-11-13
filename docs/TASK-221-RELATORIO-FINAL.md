# TASK-221 - Regressões

31/10/2026. Autorização explícita para avançar nas tarefas restantes.

Estado: Implementada e validada localmente; aceite visual/físico pendente.

Compilação e suíte Java completa, rodada GUI serializada, casos de inventário/missões/combate e validação com servidor e dois clientes. Launcher, offline, reconexão, cancelamento e Studio cobertos pelas regressões existentes. Evidências preservadas em checkpoints novos.

Arquivos principais: Task214To220UiTest.java, GameInputRoutingTest.java, GamePanelsIntegrationTest.java.

Validação: Resultados consolidados abaixo; a suíte GUI cobre os abortos gráficos da execução headless. Não somar rodadas como testes únicos.

Suíte Java: 698 executados, 673 aprovados, 25 abortos gráficos, zero falhas e zero ignorados. Rodada GUI: 13 classes, 36 execuções aprovadas, zero abortos e falhas. Os totais se sobrepõem.

Limites: revisão visual humana, teclado físico e Radmin entre máquinas pendentes. Escopo visual moderno na partida V6; offline/Coop preservados.

[Relatório consolidado](CORUJAO-VIII-RELATORIO-FINAL.md) · [PDF](Spirit_Gate_Corujao_VIII_Relatorio_Completo.pdf) · [Roteiro de uso](CORUJAO-VIII-COMO-TESTAR.md).
