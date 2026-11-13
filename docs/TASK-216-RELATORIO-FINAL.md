# TASK-216 - Configurações

31/10/2026. Autorização explícita para avançar nas tarefas restantes.

Estado: Implementada e validada localmente; aceite visual/físico pendente.

Consolidação do painel integrado do adendo: seções, volume geral, mute, redução de efeitos, resolução, keybinds, captura e validação de conflitos. Aplicar confirma e Cancelar/X/ESC descartam. Preferências suportadas mantêm o formato anterior. Atalhos e resolução são de sessão. A redução de efeitos é lida ao abrir a partida e também limita o deslocamento do dano flutuante. Música e efeitos separados não existem na base e não foram simulados com sliders sem função.

Arquivos principais: NetworkSettingsPanel.java, RpgNetworkClientLauncher.java, NetworkCombatPresentation.java.

Ressalva de escopo: canais separados de música/efeitos permanecem indisponíveis, pois não são suportados pela base atual. A UI entrega somente controles reais.

Validação: GamePanelsIntegrationTest: arquivo isolado, Cancelar sem alteração, Aplicar com persistência e remapeamento explícito, captura de SPACE e funcionamento em três resoluções. Regressões de áudio/preferências existentes na suíte completa.

Suíte Java: 698 executados, 673 aprovados, 25 abortos gráficos, zero falhas e zero ignorados. Rodada GUI: 13 classes, 36 execuções aprovadas, zero abortos e falhas. Os totais se sobrepõem.

Limites: revisão visual humana, teclado físico e Radmin entre máquinas pendentes. Escopo visual moderno na partida V6; offline/Coop preservados.

[Relatório consolidado](CORUJAO-VIII-RELATORIO-FINAL.md) · [PDF](Spirit_Gate_Corujao_VIII_Relatorio_Completo.pdf) · [Roteiro de uso](CORUJAO-VIII-COMO-TESTAR.md).
