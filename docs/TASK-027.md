# TASK-027 - Ferramenta offline de mapas

Objetivo: gerar, inspecionar e validar mapas sem editar grandes Properties.
Base aprovada: TASK-026, 230 JUnit e 22 Python.
Diagnóstico: duplicar o validador em Python criaria divergência entre autoria e
runtime. Decisão: tools/mapas.py gera Properties determinístico e chama o próprio
LocalArenaMap.main em modo de validação, sem Swing ou rede. O relatório lista
dimensões, spawn, contagem por camada e categoria. Mensagens de limites e
sobreposição incluem IDs físicos/visuais. Contrato, spawns, finitude, contagens,
duplicação e chaves continuam sob o validador autoritativo do jogo.

Segurança: raiz explícita --root (padrão docs/mapas), resolve real dos caminhos,
recusa escape, criação exclusiva sem sobrescrita, limite 1 MB e timeout da JVM.
Não instala dependências. Java 17 e classes compiladas são pré-requisitos claros.
Arquivos: LocalArenaMap.java, mapas.py, test_mapas.py, exemplo sintético docs/mapas.
Testes Python executam JVM real, com V1/V2, exportação determinística, NaN,
coordenada negativa, ID duplicado, borda inválida, tamanho e escape de caminho.
Suite completa registrada em TASK-027-qualidade.json e logs; nenhum teste simulado
é declarado como carregamento real. Checkpoints/diff padronizados.

Reprodução: python tools/qualidade.py --offline --task 027
python tools/mapas.py create demo-v2.properties
python tools/mapas.py validate demo-v2.properties
Criação repetida recusa sobrescrita. Cenário sintético não substitui o padrão.
Limites: CLI sem editor gráfico; IDs de spawn fixos da arena histórica; sem
aprovação humana de arte ou teste interativo em outro Windows. Próxima TASK-028.

## Resultado final observado
Validação local aprovada: 230 JUnit e 24 Python; zero falhas.
