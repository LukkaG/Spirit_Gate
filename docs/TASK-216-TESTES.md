# TASK-216 - testes

GamePanelsIntegrationTest: arquivo isolado, Cancelar sem alteração, Aplicar com persistência e remapeamento explícito, captura de SPACE e funcionamento em três resoluções. Regressões de áudio/preferências existentes na suíte completa.

Suíte Java: 698 executados, 673 aprovados, 25 abortos gráficos, zero falhas e zero ignorados. Rodada GUI: 13 classes, 36 execuções aprovadas, zero abortos e falhas. Os totais se sobrepõem.

Evidências: .task-checkpoints/TASK-221-GUI/corujao-viii/ e .task-checkpoints/TASK-214-222/corujao-viii/. Os testes de teclado injetam eventos na fila AWT, com foco e bindings Swing reais. Não comprovam teclado físico.

[Relatório consolidado](CORUJAO-VIII-RELATORIO-FINAL.md) · [PDF](Spirit_Gate_Corujao_VIII_Relatorio_Completo.pdf) · [Roteiro de uso](CORUJAO-VIII-COMO-TESTAR.md).
