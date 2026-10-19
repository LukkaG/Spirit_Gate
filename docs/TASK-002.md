# TASK-002 - Extração de movimentação e colisões

Spirit Gate | 18/10/2026 | Java 17

## Resultado

Movimentação e colisões estão no domínio, sem Swing, DebugInput ou DebugSnapshot. O protótipo fornece entrada e cenário. Nenhuma dependência foi adicionada. O processamento continua na thread única de DebugRuntime.

## Inspeção e decisões

A entrada WASD é convertida em intenção no DebugWorld. As velocidades continuam configuradas no mundo: jogador 215 e mobs 92 unidades/s. Position é um record imutável armazenado em GameEntity; não foi necessário alterar esses tipos.

MovementService normaliza a direção e calcula deslocamento com direção * velocidade * dt. moveDirected recebe direção já normalizada. A multiplicação mantém a ordem original, inclusive na perseguição dos mobs. move aceita deslocamento pronto e delega a resolução.

CollisionService contém a detecção e a resolução X antes de Y. O eixo Y consulta a posição resultante de X. Isso preserva o deslizamento. Space recebe limites, obstáculos retangulares e corpos circulares. As listas são copiadas; posições e HealthComponent permanecem consultados ao vivo na thread da simulação.

Tangência é permitida. O próprio ator e entidades mortas não bloqueiam. Jogador e mobs usam a mesma regra. DebugWorld apenas monta esse contexto e converte obstáculos para o snapshot visual; não duplica geometria.

## Arquivos alterados nesta finalização

- server/src/main/java/com/projetok/domain/movement/MovementService.java: cálculo de deslocamento e delegação, Javadoc em português.
- server/src/main/java/com/projetok/domain/movement/CollisionService.java: resolução por eixos, documentação dos dados, métodos e seções de geometria.
- server/src/main/java/com/projetok/prototype/DebugWorld.java: jogador e mobs usam moveDirected; comentário do contexto em português.
- server/src/test/java/com/projetok/domain/movement/MovementServiceTest.java: deslocamento nos oito sentidos, zero e resolução direta nos limites.
- server/src/test/java/com/projetok/prototype/DebugWorldTest.java: regressão explícita de alcance e cone com animação ativa.
- README-PROTOTIPO.md: responsabilidades e links para a entrega.
- docs/HISTORICO-TECNICO.md: registro da TASK-002.
- docs/TASK-002.md e docs/Spirit_Gate_TASK-002.pdf: decisões e resultados.
- tools/gerar_pdf_task002.py: geração reproduzível do PDF, sem pacotes externos.

## Validação executada

Comando na pasta server: mvn clean test.

Resultado: BUILD SUCCESS. 15 testes, zero falhas, zero erros, zero ignorados. Compilação completa com release 17.

MovementServiceTest: 6 testes. Cobre direção normalizada, deslocamento horizontal, vertical e diagonal, repouso, velocidade e dt, limites nas quatro bordas, tangência, cantos, obstáculos, ordem X/Y, deslizamento, corpos vivos/mortos, exclusão do próprio ator e posições atualizadas.

DebugWorldTest: 8 testes. Cobre configuração, movimento diagonal, obstáculos, IA e dano recebido, dano de 15 e cooldown, alcance e cone, duração/direção do corte e renderização em imagem sem abrir janela.

MovementReplayTest: 1 teste com três sequências de 600 ticks. As impressões digitais do estado visual completo, acumuladas a cada tick, coincidem com as registradas antes da extração inicial. Isso cobre posições de jogador e mobs, direção, HP, cooldown e efeitos ao longo de 1.800 ticks. A comparação por hash é uma regressão complementar; as asserções geométricas verificam comportamentos diretamente.

CombatService, seleção de alvo, alcance, cone, cooldown e desenho do corte não foram modificados nesta finalização. DebugRuntime, DebugLauncher e DebugPanel também permaneceram intactos. Os testes verificam a integração sem janela; não foi realizada inspeção manual interativa da arena nesta etapa.

## Limites preservados e sugestões futuras

A colisão considera somente a posição candidata por eixo, como antes. Grandes deslocamentos podem atravessar obstáculos; varredura contínua seria mudança de mecânica e fica para outra tarefa. O chamador deve controlar o passo de simulação e possuir as entidades. Nenhum serviço cria threads ou implementa multiplayer.

## Reprodução do documento

Na raiz Projeto-K, executar: python tools/gerar_pdf_task002.py. O PDF é gerado deste arquivo usando somente a biblioteca padrão do Python; o PDF anterior do protótipo foi preservado.
