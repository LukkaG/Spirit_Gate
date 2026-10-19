# Colisões com deslizamento lento

18/10/2026 - Melhoria de sensação de movimento solicitada após TASK-005.

## Comportamento

O jogador e os mobs agora tentam escorregar lentamente pelo contorno de outro
corpo circular ou por uma quina externa quando o passo inteiro fica bloqueado.
A intenção é projetada na tangente do contato, com fator de atrito de 0,45.
O auxílio nunca supera 45% da distância da caminhada solicitada e fica mais
lento quando a direção aponta quase de frente para o bloqueador. É mais lento
que manter uma diagonal livre. O limite adicional por passo é um quarto do raio.

Movimento livre e deslizamento normal X/Y pelas paredes conservam a velocidade
anterior. O auxílio só entra se os dois eixos não produzirem deslocamento.
Raios, alcance e hitboxes de ataque não foram reduzidos. Outros atores não são
empurrados, incluindo mobs congelados.

Um contato frontal perfeitamente simétrico não escolhe sozinho um lado.
Paredes retas e cantos internos sem saída continuam bloqueando. Não há expulsão
automática de uma origem já sobreposta. Isso evita desvio arbitrário ao segurar
uma tecla diretamente contra uma parede ou um ator alinhado.

## Resolução e segurança geométrica

CollisionService.resolveSliding mantém a prioridade X antes de Y. Cada trecho
normal é verificado por inteiro para não atravessar a pequena borda curva de
um círculo, mesmo quando as duas extremidades estejam livres. Se o passo ficar
totalmente bloqueado, o serviço identifica corpos/quinas no percurso, projeta
a intenção na tangente e tenta um deslocamento reduzido.

A projeção precisa avançar na direção solicitada; não pode recuar apenas para
produzir algum movimento. O percurso tangencial inteiro também é validado
contra todos os bloqueadores e limites. Há até quatro tentativas de redução
do passo. Entre alternativas válidas, prevalece o maior progresso na intenção,
com desempate geométrico estável. O índice espacial atualiza apenas o ator movido.

Não há sorteio, nova thread ou dependência gráfica nesse cálculo. Freeze continua
sendo verificado em MovementService antes de qualquer resposta à colisão.
Ataques, vigor, Criostase e probabilidades não foram alterados.

## Políticas e regressão histórica

MovementService.Response permite selecionar STRICT ou SOFT_SLIDE. O construtor
antigo do serviço mantém STRICT por compatibilidade de API. DebugWorld usa
SOFT_SLIDE por padrão, para jogador e mobs, inclusive ao reiniciar a arena.
O construtor de DebugWorld com resposta explícita permite comparar os dois modos.

CollisionService.resolve conserva o comportamento discreto anterior.
resolveContinuous continua sendo a operação específica de varredura por eixo,
sem assistência tangencial. Para a caminhada da arena, o caminho usado é
resolveSliding. Deslocamentos grandes de dash devem continuar usando a API
contínua apropriada; esta tarefa não implementa dash ou busca de caminho.

Esta mudança altera intencionalmente trajetórias antes bloqueadas. Portanto,
MovementReplayTest agora instancia explicitamente o modo STRICT para conservar
a referência histórica. Seus três hashes não foram recalculados. Os novos testes
verificam o comportamento SOFT_SLIDE diretamente, evitando usar o replay antigo
para exigir que a melhoria reproduza o travamento anterior.

## Validação

Estado inicial: mvn clean test -q, 105 testes aprovados.
Estado final: mvn clean test -q, 118 testes aprovados, sem falhas, erros ou ignorados.

SoftSlideCollisionTest acrescenta 13 testes: círculo, quatro espelhamentos,
quinas externas, limite de velocidade, 24 passos de escape com amostragem do
percurso, contato frontal, paredes/limites/cantos internos, passagem fechada,
origem sobreposta, atualização do índice sem empurrar, entradas numéricas
inválidas, Freeze e preservação de movimento livre e da prioridade X/Y.

Um desses testes identificou o caso de pontas livres com travessia do contorno
curvo; a validação por segmento no novo modo corrige esse problema. A versão
final passa no teste, sem relaxar sua separação entre corpos.

A execução incremental intermediária encontrou artefatos internos de classe
incompletos no target. A compilação limpa foi executada para a validação final.
Não houve sessão interativa da janela nesta etapa; o fator inicial de 45% deve
ser avaliado pela sensação de jogo. Não se alterou o balanceamento de combate.

## Arquivos

- server/src/main/java/com/projetok/domain/movement/CollisionService.java.
- server/src/main/java/com/projetok/domain/movement/MovementService.java.
- server/src/main/java/com/projetok/prototype/DebugWorld.java.
- server/src/test/java/com/projetok/domain/movement/SoftSlideCollisionTest.java (novo).
- server/src/test/java/com/projetok/prototype/MovementReplayTest.java (seleção explícita de STRICT).
- README-PROTOTIPO.md e docs/HISTORICO-TECNICO.md.
- Este relatório, DESLIZAMENTO-testes.txt e DESLIZAMENTO.diff.

O ajuste de intensidade fica em CollisionService.SOFT_SLIDE_FACTOR. Valores
maiores tornam o contorno mais rápido; 0,45 é o valor inicial desta entrega.

Para testar manualmente: executar rodar-prototipo.cmd, aproximar-se ligeiramente
desalinhado de um mob ou de uma quina externa e manter a direção contra o contato.
O personagem deve contornar lentamente e retomar a velocidade normal ao liberar
o caminho. Conferir também diagonal livre, parede reta e mob congelado.
