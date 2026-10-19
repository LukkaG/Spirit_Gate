# TASK-024 - Mapas V2 ampliados

Objetivo: aceitar mapas maiores preservando V1, colisões e reinício.
Base aprovada: TASK-023, 223 JUnit e 22 Python. Situação: ver evidências finais
TASK-024-qualidade.json e TASK-024-testes.txt, que são a autoridade de aprovação.

Diagnóstico: LocalArenaMap limitava dimensões ao demo; mundo, Criostase e painel
usavam constantes. ArenaViewport já calculava câmera para mundo maior.
Decisão: constantes históricas continuam definindo o canvas e o mapa padrão;
dimensões do mapa governam Space, alvo da Criostase, câmera e desenho do mundo.
DebugRuntime já usa fábrica no reinício e não precisou ser substituído.

Implementação: schema 1 permanece estritamente 960x640; schema 2 reservado a mapas
ampliados, mínimo 960x640 e máximo 8192x8192. Isso impede que apenas trocar a
versão de um mapa histórico valide silenciosamente outro contrato. Máximo 4096
visuais e 4096 físicos, também no construtor. Spawn, borda, finitude, IDs,
categorias e sobreposições continuam validados. Memória visual depende de tiles
32x32 por objeto, não de uma imagem com toda a área do mundo.

Arquivos: LocalArenaMap.java, DebugWorld.java, DebugPanel.java e LargeMapTest.java.
Testes novos: V2 1920x1280, V1 ampliado inválido, limite excessivo, NaN, contagem,
dash versus sólido em posição além da arena histórica, câmera e R determinístico.
Suíte completa: 226 JUnit e 22 Python aprovados na primeira execução; a repetição
após corrigir também o clipping do ataque está registrada nos mesmos logs finais.
Nenhuma fórmula de combate, custo, duração, FIFO ou expectativa histórica alterada.

Reprodução: python tools/qualidade.py --offline --task 024
Checkpoint inicial .task-checkpoints/TASK-024-before.json; diff e checkpoint final
gerados somente depois da validação. Retomada: TASK-025.
Limites: mobs mantêm seus três spawns históricos; não foi introduzida autoria
de entidades. Sem arte final ou homologação humana. O render ainda percorre todos
os objetos; otimização de culling pertence à próxima tarefa.

## Resultado final observado
Validação local aprovada: 226 JUnit e 22 Python; zero falhas.
