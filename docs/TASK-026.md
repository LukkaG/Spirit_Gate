# TASK-026 - Camadas de cenário

Objetivo: separar terreno, decoração, atores, efeitos e HUD.
Base: TASK-025, 228 JUnit e 22 Python aprovados.
Diagnóstico: os descritores já tinham layer, mas todos eram desenhados antes dos
atores. Reutilizado Renderer; drawLayers desenha intervalos, com ordenação estável
por layer e ordem original para empates. 0 é terreno, 1-4 decoração traseira,
5-10 decoração frontal. Atores ficam entre intervalos; ataque e efeitos depois;
HUD continua em coordenadas de canvas. Gelo e rastros permanecem sob os atores
conforme apresentação histórica. Nenhum PNG foi acrescentado.

Tiles sintéticos permanecem 32x32 em cache e são desenhados no retângulo explícito
do descritor, com pivot superior esquerdo e offset zero. Sprites de personagem
mantêm pivots/offsets no carregador existente. Colisão depende só de physical.
Mapas V1 existentes usam camadas traseiras e preservam apresentação.
Arquivos: LocalArenaMap.java, DebugPanel.java, ScenarioLayerTest.java.
Testes positivos, negativos e determinísticos verificam oclusão, empate de layers,
camada inválida e igualdade da simulação com aparências diferentes.
Reprodução: python tools/qualidade.py --offline --task 026
Resultado e logs: TASK-026-qualidade.json, TASK-026-testes.txt, TASK-026-*.log.
Checkpoints e diff padronizados; próximo passo TASK-027.
Limites: sem árvores definitivas ou aprovação artística; homologação humana
visual pendente. Camadas são numéricas por compatibilidade com Properties.

## Resultado final observado
Validação local aprovada: 230 JUnit e 22 Python; zero falhas.
