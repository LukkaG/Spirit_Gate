# TASK-220 - Polimento e teclado

31/10/2026. Autorização explícita para avançar nas tarefas restantes.

Estado: Implementada e validada localmente; aceite visual/físico pendente.

Estados de hover, pressão, foco e categoria ativa; contraste de botões desabilitados e barras. Um painel ativo por vez, modalidade das configurações, fechamento consistente e arraste limitado. Textos não enviam gameplay; SPACE não clica no botão focado; ENTER ativa o controle; TAB/SHIFT+TAB navegam. Corrigido retorno R após derrota, sem liberar movimento/ataque. Sincronização de revisão também bloqueia gameplay.

Arquivos principais: GameInputRouter.java, GamePanels.java, GameHudStyle.java, RpgNetworkClientLauncher.java.

Validação: GameInputRoutingTest: eventos AWT com foco real, repetição, texto, captura, modalidades, isolamento de outra janela, quarentena e retorno após derrota. GamePanelsIntegrationTest: arraste, reabertura, descarte e dois clientes.

Suíte Java: 698 executados, 673 aprovados, 25 abortos gráficos, zero falhas e zero ignorados. Rodada GUI: 13 classes, 36 execuções aprovadas, zero abortos e falhas. Os totais se sobrepõem.

Limites: revisão visual humana, teclado físico e Radmin entre máquinas pendentes. Escopo visual moderno na partida V6; offline/Coop preservados.

[Relatório consolidado](CORUJAO-VIII-RELATORIO-FINAL.md) · [PDF](Spirit_Gate_Corujao_VIII_Relatorio_Completo.pdf) · [Roteiro de uso](CORUJAO-VIII-COMO-TESTAR.md).


## Evolução posterior: alvo de combate — 01/11/2026

Seleção PvE por TAB e ataque dirigido implementados na build privada 0.223.0-private. O ataque básico vira sem caminhar; o Shunpo ofensivo aproxima do alvo dentro de 240 unidades ou avança 70 na direção atual quando não há alvo. Nenhuma habilidade atual exige alvo obrigatório, conforme confirmação do responsável. TAB conserva navegação dentro dos painéis. [Regras, testes e pacotes](TARGET-COMBATE.md) · [PDF](Spirit_Gate_Target_Combate_Relatorio.pdf). Esta evolução é posterior à consolidação 0.222.0-private registrada acima.

