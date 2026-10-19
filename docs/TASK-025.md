# TASK-025 - Culling conservador

Objetivo: evitar desenho fora da viewport sem interferir no mundo.
Base: TASK-024 aprovada, 226 JUnit e 22 Python.
Implementação: ArenaViewport.visibleWorld publica o retângulo visível. Java2D
hitClip reaproveita o clipping transformado para tiles, sólidos, leves e atores
do fallback. Margens incluem sombras, strokes, nome, HP, Freeze e alcance H.
Atlas com offsets arbitrários mantém desenho conservador, sem descarte de ator.
Efeitos permanecem desenhados e recortados pelo Java2D, preservando cruzamentos
de borda. Nenhum índice espacial ou alteração da física foi introduzido.

Diagnóstico: o pipeline já recortava pixels, mas ainda submetia todos os objetos.
Foi acrescentada uma chave de referência para comparar culling de entidades
ligado/desligado. Tiles usam hitClip sempre. Ordem e dados continuam iguais.
Arquivos: ArenaViewport.java, LocalArenaMap.java, DebugPanel.java, CullingTest.java.
Testes: comparação de pixels em mapa 1920x1280, zoom 1 a 4, três tamanhos, hitboxes;
interseções parciais diagonais e retângulo visível. Ver totais reais e resultado em
TASK-025-qualidade.json e TASK-025-testes.txt, logs completos Java/Python.
Comando: python tools/qualidade.py --offline --task 025

Limites: fundo e efeitos ainda percorrem seus elementos; otimização é conservadora,
sem alegar eliminação de todo trabalho fora da câmera. Sem benchmark temporal
rígido ou arte aprovada. Homologação visual humana permanece pendente.
Checkpoint recuperável antes/depois e diff em caminhos padronizados. Próximo:
TASK-026, camadas explícitas. Não há mudanças de regras de gameplay.

## Resultado final observado
Validação local aprovada: 228 JUnit e 22 Python; zero falhas.
