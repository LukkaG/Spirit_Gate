# TASK-219 - Minimapa e utilitários

31/10/2026. Autorização explícita para avançar nas tarefas restantes.

Estado: Implementada e validada localmente; aceite visual/físico pendente.

Minimapa mantém proporção, mapa recebido, obstáculos, portais e área de câmera, com legenda de jogador/aliados/inimigos e enquadramento ajustado. Atalho remapeado atualiza o tooltip. Perfil/inventário, ações centrais e livro/menu/configurações permanecem separados; o botão do painel aberto ganha destaque.

Arquivos principais: NetworkMinimap.java, NetworkGameHud.java, GameHudStyle.java.

Validação: Task213HudTest: projeções e câmera. Task213HudSwingTest: mostrar/ocultar minimapa, atalhos e layout. Capturas reais nas três resoluções.

Suíte Java: 698 executados, 673 aprovados, 25 abortos gráficos, zero falhas e zero ignorados. Rodada GUI: 13 classes, 36 execuções aprovadas, zero abortos e falhas. Os totais se sobrepõem.

Limites: revisão visual humana, teclado físico e Radmin entre máquinas pendentes. Escopo visual moderno na partida V6; offline/Coop preservados.

[Relatório consolidado](CORUJAO-VIII-RELATORIO-FINAL.md) · [PDF](Spirit_Gate_Corujao_VIII_Relatorio_Completo.pdf) · [Roteiro de uso](CORUJAO-VIII-COMO-TESTAR.md).
