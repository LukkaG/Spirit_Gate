# TASK-020 - Câmera, viewport e entrada

18/10/2026. Plano apresentado antes das edições: preservar mapa 960x640, criar transformação pura, aplicar câmera somente ao mundo, converter mouse no momento do comando e adaptar canvas à janela.

ArenaViewport implementa ida/volta mundo/tela, câmera com origem inteira e limites, zoom inteiro 1 a 4, escala inteira quando cabe e redução proporcional quando a janela é menor. Canvas lógico completo permanece 1260x750 (arena 960x640 e HUD). Letterbox centralizado; interpolação nearest-neighbor. A janela agora permite redimensionamento.

DebugPanel separa desenho do mundo e HUD. Toda geometria, mapa, efeitos, trails, sprites e hitboxes usa a mesma câmera e recorte. HUD acompanha escala do canvas, sem deslocamento pela câmera. Câmera calculada do snapshot, sem timers ou mutação. Padrão zoom=1 mantém origem zero e coordenadas históricas; setCameraZoom habilita zoom de integração/teste, sem novo atalho.

Mouse permanece em tela até Shift/Q: conversão usa câmera atual no momento do comando, mesmo sem novo evento de mouse. HUD e letterbox são alvos inválidos. Física, custos, dano, alcance, FIFO e replays preservados.

Schema de mapa exige 960x640 e foi preservado. Transformação para mundo sintético 1920x1280 testada, sem mapas maiores em produção. Reinício deriva câmera novamente do spawn.

Validação: 223 JUnit e 18 Python, zero falhas finais. Testes: identidade padrão, resize/letterbox, zoom 1 a 4, limites/diagonais e ida/volta em mundo maior, Shift/Q com câmera deslocada e resize, comparação integral dos snapshots incluindo colisões e reinício, render repetido puro e HUD fixo. Evidência TASK-020-camera-sintetica.png.

Primeira execução: fixture nova escolheu alvo fora da região visível em zoom 4, corretamente recusado. Corrigido cenário para alvo visível relativo ao spawn real; preservada comparação integral com comando direto. Log inicial TASK-020-java-primeira-tentativa.log; resultado final TASK-020-java.log. Nenhum teste antigo removido ou alterado.