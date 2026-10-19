# TASK-003 - Implementação do Vigor Físico

Spirit Gate | 18/10/2026 | Java 17 e Maven

## Resultado e parâmetros provisórios

Jogadores criados por PlayerFactory recebem 100/100 pontos de vigor. A arena exibe a barra amarela e o valor numérico. A tecla E executa um esforço temporário de custo 20, sem dano. A regeneração é de 15 pontos/s, limitada ao máximo. Os valores são provisórios e não representam balanceamento definitivo.

Ataque básico e movimentação não gastam vigor. Nenhuma skill de combate, stamina de XP, integração de rede ou alteração de banco foi introduzida. Nenhuma dependência foi adicionada.

## Preparação e proposta

Foram inspecionados PlayerFactory, componentes, Position, DebugInput, DebugWorld, DebugSnapshot, DebugRuntime, DebugLauncher, DebugPanel e MovementReplayTest antes de modificar arquivos. O comando inicial mvn clean test -q passou com os 15 testes existentes.

A proposta apresentada separou armazenamento e invariantes no componente, consumo e regeneração no serviço, entrada por transição no mundo e leitura da barra por snapshot. O processamento permanece na thread proprietária da simulação, sem mutações de entidades no EDT.

## Arquitetura do domínio

StaminaComponent implementa Component, armazena valores float e inicia cheio. O construtor padrão usa 100; o construtor com máximo explícito exige valor finito e positivo. Não há setter de saldo ou máximo. tryConsume valida custo e só subtrai se houver saldo. restore valida quantidade e limita a recuperação ao máximo. Custos ou quantidades negativos, NaN e infinitos lançam IllegalArgumentException antes de qualquer mutação.

StaminaService recebe GameEntity com HealthComponent e StaminaComponent. tryConsume recusa entidade morta, inclusive custo zero, e delega o consumo ao componente. O chamador executa a ação apenas se o método retornar true. regenerate valida dt finito e não negativo, recusa mortos e aplica 15 * dt. O cálculo intermediário usa double e saturação para evitar overflow com deltas finitos grandes. Delta zero não altera o recurso.

O componente é armazenamento protegido por invariantes numéricas; elegibilidade por vida pertence ao serviço. Ações do jogo devem usar StaminaService. O serviço não agenda tarefas, não cria threads e não depende do protótipo, Swing, PostgreSQL ou Kryonet.

## Ordem do tick e entrada

DebugWorld valida dt antes de alterar estado, incluindo rejeição explícita de NaN. Mantém atualização dos efeitos e cooldowns existentes; se morto, retorna. Para jogador vivo, regenera antes das ações, detecta a transição de E, processa movimento, ataque básico e inimigos na ordem anterior. Ao final, executa o esforço somente se houver nova pressão e o jogador continuar vivo.

O custo é verificado antes da execução do esforço; sucesso e insuficiência publicam mensagens no log. A ação provisória é somente essa confirmação e o consumo, sem dano ou mudança de movimento. Se ocorrer morte no tick, o esforço não sobrescreve a mensagem de derrota.

Segurar E não repete consumo. Uma recusa por saldo insuficiente também exige soltar e pressionar novamente, mesmo que o saldo se recupere. O runtime sincroniza o estado de E ao criar o mundo no reinício: a arena volta a 100/100, sem gasto automático por tecla mantida. O construtor antigo de cinco booleanos de DebugInput continua disponível e define effort=false.

DebugSnapshot acrescenta somente stamina e maxStamina. DebugPanel lê esses campos, desenha a barra e mostra o controle E. O domínio não depende do formato visual. As configurações de velocidade, colisão, hitboxes e dano foram preservadas.

## Replay e equivalência do comportamento anterior

Os três baselines originais de MovementReplayTest foram mantidos. Antes de acrescentar campos ao snapshot, uma projeção explícita dos campos antigos foi comparada por igualdade textual com o toString original em todos os 1.800 ticks; o teste passou. Após a mudança, o replay aplica hash à mesma projeção, ignorando apenas os campos novos de vigor. Nenhum baseline foi recalculado ou teste eliminado.

A projeção inclui atores, obstáculos, efeitos, HP, derrotas, morte, ticks, mensagem, direção, cooldown e corte. O teste continua complementar às asserções de comportamento, e permanece sensível à apresentação textual dos campos antigos. Mudanças futuras precisam de justificativa, nunca de atualização automática dos hashes.

## Validação real

Comando final: mvn clean test, executado na pasta server, com compilação release 17. Resultado: BUILD SUCCESS; 30 testes, zero falhas, zero erros e zero ignorados.

- MovementServiceTest: 6 testes existentes preservados.
- DebugWorldTest: 8 testes existentes preservados, incluindo dano, cone, alcance, cooldown e renderização do corte.
- MovementReplayTest: 1 teste com três sequências de 600 ticks e baselines inalterados.
- StaminaServiceTest: 5 testes para valores iniciais, consumo válido/zero/insuficiente, regeneração fracionária, saturação, entradas inválidas e morte.
- StaminaWorldTest: 8 testes para fábrica, compatibilidade de entrada, gasto único, recusa sem consumo, reinício, ataque e movimento com vigor baixo, dano de 15 preservado, morte e dt inválido.
- StaminaInterfaceTest: 2 testes para ligações reais de E no EDT, repetição de teclado, reinício com a thread real da simulação e largura da barra em renderização sem janela.

O resumo Surefire está em TASK-003-testes.txt. A imagem TASK-003-vigor.png foi produzida pelo teste e inspecionada: mostra 80/100 de vigor, barra parcial, tecla E e mensagem de esforço sem sobreposição. Não houve sessão interativa de jogo com JFrame nesta etapa.

## Relação de arquivos alterados ou criados

Domínio:
- server/src/main/java/com/projetok/domain/entity/component/StaminaComponent.java (novo).
- server/src/main/java/com/projetok/domain/resource/StaminaService.java (novo).
- server/src/main/java/com/projetok/domain/factory/PlayerFactory.java.

Protótipo, sob server/src/main/java/com/projetok/prototype/:
- DebugInput.java, DebugSnapshot.java, DebugWorld.java.
- DebugRuntime.java, DebugLauncher.java, DebugPanel.java.

Testes:
- server/src/test/java/com/projetok/domain/resource/StaminaServiceTest.java (novo).
- server/src/test/java/com/projetok/prototype/StaminaWorldTest.java (novo).
- server/src/test/java/com/projetok/prototype/StaminaInterfaceTest.java (novo).
- server/src/test/java/com/projetok/prototype/MovementReplayTest.java.

Documentação e ferramentas:
- README-PROTOTIPO.md e docs/HISTORICO-TECNICO.md.
- docs/TASK-003.md, docs/TASK-003-testes.txt, docs/TASK-003-vigor.png.
- docs/Spirit_Gate_TASK-003.pdf e docs/TASK-003.diff.
- tools/gerar_pdf_task002.py (parâmetro opcional do rodapé; comportamento anterior preservado).
- tools/gerar_pdf_task003.py (novo, reutiliza o gerador sem dependências).

O diff textual é relativo ao estado inicial desta tarefa, capturado antes das alterações. Esta pasta não é um checkout Git acessível; não foi realizado commit ou merge. O patch não contém server/target, dados locais sensíveis ou binários. PDF e PNG são entregues como artefatos separados. O próprio diff não se inclui recursivamente.

## Limitações e próximos passos

O esforço de E é uma ação provisória, sem animação própria e sem efeito de gameplay além do consumo. A regeneração não tem atraso após consumo. Deltas e saldos usam precisão float; a interface mostra uma casa decimal. O máximo é fixo durante a vida do componente, sem progressão ou modificadores nesta tarefa.

A entrada conserva o modelo de estado amostrado por tick do protótipo: uma pressão e soltura completas entre dois ticks podem não ser observadas. Uma futura fila de comandos pode tratar isso junto à evolução da entrada; não há previsão de rede aqui.

MovementService, CollisionService, CombatService, Position, pom.xml e as regras da TASK-001/TASK-002 não foram alterados. Tunelamento, validação numérica da colisão e otimização espacial permanecem fora desta tarefa.

## Reprodução

Na pasta server: mvn clean test. Na pasta Projeto-K: python -B tools/gerar_pdf_task003.py. Para conferência manual, executar rodar-prototipo.cmd, pressionar e segurar E, soltar e pressionar novamente, observar regeneração e insuficiência, testar R com E mantido, mover e atacar com pouco vigor e conferir a derrota.
