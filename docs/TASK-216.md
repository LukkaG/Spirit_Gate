# TASK-216 - Configurações

31/10/2026. Autorização explícita para avançar nas tarefas restantes.

Estado: Implementada e validada localmente; aceite visual/físico pendente.

Consolidação do painel integrado do adendo: seções, volume geral, mute, redução de efeitos, resolução, keybinds, captura e validação de conflitos. Aplicar confirma e Cancelar/X/ESC descartam. Preferências suportadas mantêm o formato anterior. Atalhos e resolução são de sessão. A redução de efeitos é lida ao abrir a partida e também limita o deslocamento do dano flutuante. Música e efeitos separados não existem na base e não foram simulados com sliders sem função.

Arquivos principais: NetworkSettingsPanel.java, RpgNetworkClientLauncher.java, NetworkCombatPresentation.java.

Ressalva de escopo: canais separados de música/efeitos permanecem indisponíveis, pois não são suportados pela base atual. A UI entrega somente controles reais.

[Relatório consolidado](CORUJAO-VIII-RELATORIO-FINAL.md) · [PDF](Spirit_Gate_Corujao_VIII_Relatorio_Completo.pdf) · [Roteiro de uso](CORUJAO-VIII-COMO-TESTAR.md).
