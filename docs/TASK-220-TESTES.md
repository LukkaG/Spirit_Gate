# TASK-220 - testes

GameInputRoutingTest: eventos AWT com foco real, repetição, texto, captura, modalidades, isolamento de outra janela, quarentena e retorno após derrota. GamePanelsIntegrationTest: arraste, reabertura, descarte e dois clientes.

Suíte Java: 698 executados, 673 aprovados, 25 abortos gráficos, zero falhas e zero ignorados. Rodada GUI: 13 classes, 36 execuções aprovadas, zero abortos e falhas. Os totais se sobrepõem.

Evidências: .task-checkpoints/TASK-221-GUI/corujao-viii/ e .task-checkpoints/TASK-214-222/corujao-viii/. Os testes de teclado injetam eventos na fila AWT, com foco e bindings Swing reais. Não comprovam teclado físico.

[Relatório consolidado](CORUJAO-VIII-RELATORIO-FINAL.md) · [PDF](Spirit_Gate_Corujao_VIII_Relatorio_Completo.pdf) · [Roteiro de uso](CORUJAO-VIII-COMO-TESTAR.md).
