# Histórico técnico

## 18/10/2026 - TASK-011: candidata 0.11.0-rc1 e rodada encerrada

Build limpo final com 202 testes aprovados; pacote Windows x64 reconstruído com
código das TASK-007 a TASK-010, Java 17 integrado, kit, manifesto e SHA-256.
Auditoria comparou 122 classes byte a byte e recusou cópia adulterada,
preservando o original. O EXE abriu fora dos fontes em pasta com espaços, com
PATH sem Java, usando jvm.dll do runtime integrado; fechou com código 0.
Captura real inspecionada, sem publicação.

ZIP de 29.359.239 bytes; SHA-256:
7484f90f5de5ca0475c8d73a918a8868b20a089d5a3fb3a12dd2e40f5fc19d01.
Homologação em outro Windows e revisão humana por tarefa pendentes. Inspeção
de sockets indisponível no ambiente; limitações registradas. Sem TASK-012,
push ou alterações de banco/rede.

Entrega: [TASK-011](TASK-011.md), [consolidado](RELATORIO-FINAL-RODADA.md), PDFs,
ZIP em dist, changelog RC, kit do tester, testes/logs, diffs e evidências JSON/PNG.

## 18/10/2026 - TASK-010: regressão determinística

Treze testes novos combinam runtime/fila/bindings reais, recursos, dash, retorno
a WASD, Freeze/ataque, morte/R, colisão, índice e extremos de float. O tick foi
extraído internamente sem mudar agendador ou ordem, com fábrica de mundo controlada.
Novos testes não usam sleep/JFrame. Os 26 arquivos originais de teste estão intactos.

Bug de texto do HUD foi reproduzido antes da correção: saldo abaixo do custo
arredondado para 35/35 ou55/55. Corrigido só o contador redundante no aviso.
`mvn clean test` e repetição offline: 202 testes, zero falhas/erros/ignorados,
mesma contagem/resultados por classe e baselines históricos preservados.

Entrega: [TASK-010](TASK-010.md), PDF, testes, logs de reprodução/correção e diff.

## 18/10/2026 - TASK-009: feedback contextual de habilidades

HUD recebe views imutáveis de estado/recarga/vigor e feedback breve de cast.
Simulação calcula todos os valores; painel apenas desenha. Step.blocked relata
redução real antes da recuperação, sem modificar movimento nem confundir arbusto
com parede. Campos e mensagens históricos do replay preservados.

`mvn clean test`: base 177, final 189 testes, zero falhas/erros/ignorados.
Doze testes novos de mapeamento, integração e renderização; captura de quatro
estados inspecionada. Homologação visual humana permanece pendente.

Entrega: [TASK-009](TASK-009.md), PDF, resultados, logs, diff e captura.

## 18/10/2026 - TASK-008: cenário demonstrativo de colisão leve

Runtime usa DebugWorld.demo() com dois arbustos e um poste SOFT_OBSTACLE.
Geometria e metadados visuais compartilham os mesmos retângulos; H mostra hitboxes.
Construtores históricos mantêm o mapa original e os baselines de replay.

`mvn clean test`: base 171, final 177 testes, zero falhas/erros/ignorados.
Seis testes novos e captura renderizada inspecionada. Domínio e testes anteriores
intactos. A falha inicial de limpeza do jpackage foi resolvida normalizando apenas
atributos da saída gerada; a base foi repetida. Homologação jogando pendente.

Entrega: [TASK-008](TASK-008.md), PDF, testes, logs, diff e captura do demo.

## 18/10/2026 - TASK-007: pacote Windows portátil

Base local TASK-006.1 confirmada com teste de parede do usuário e 170 testes.
Build jpackage app-image x64 com runtime Java 17 reduzido e jdk.random explícito.
JAR restrito a domain/prototype, sem bibliotecas do servidor/banco; manifest,
inventário e SHA-256. Scripts não removem saídas nem sobrescrevem versões.

`mvn clean test`: 171 testes, zero falhas/erros/ignorados. EXE extraído em pasta
com espaços fora dos fontes abriu com PATH sem Java/Maven, foi capturado e fechou
normalmente. Smoke acompanha o processo filho do launcher. Observação de sockets
indisponível no ambiente; outro Windows sem Java permanece pendente de homologação.

Entrega: [TASK-007](TASK-007.md), PDF, testes, log, diff e evidências JSON/PNG.

## 18/10/2026 - TASK-006.1: colisão seletiva preparada para revisão

Adicionadas CollisionCategory e CollisionPolicy. A mesma geometria de CollisionService
filtra atores e obstáculos leves na varredura seletiva, conservando paredes e limites.
Construtores e overloads antigos continuam SOLID; DebugWorld seleciona explicitamente
PHASE_THROUGH_ACTORS_AND_SOFT para o Passo Relâmpago. WASD e obstáculos antigos mantidos.
NPCs sem HealthComponent também podem ter corpo sólido, sem exigir regras de combate.

Ao terminar, o dash procura a saída normalmente livre mais próxima do destino pela
trajetória real X/Y, incluindo deslizamentos; a correção atualiza o índice espacial.
Paredes impedem o retorno. Se corpos móveis cobrirem toda a rota, o relógio termina,
WASD aguarda uma mudança do espaço e a busca é repetida sem novo consumo/cooldown.
Essa espera pode persistir se os bloqueadores não se moverem; não há empurrão ou i-frames.
Snapshot publica a espera e rastros seguem todos os segmentos, inclusive o recuo.

Estado inicial: **147 testes aprovados**. Final: `mvn clean test`, BUILD SUCCESS,
**169 testes, zero falhas/erros/ignorados**, incluindo 22 novos. Testes anteriores,
MovementReplayTest e seus baselines preservados; teste de direção zero do usuário intacto.
Captura de dois quadros renderizados inspecionada, sem sessão interativa humana da janela.

O teste `devePreservarBloqueioDeParedeNoDashSeletivo` foi reservado ao usuário e
não foi implementado. Fixtures e API real estão no relatório. Aguardar essa
contribuição, conferência jogando e revisão técnica; TASK-007 não iniciada.

Entrega: [relatório e API](TASK-006-1.md), [PDF](Spirit_Gate_TASK-006-1.pdf),
[testes](TASK-006-1-testes.txt), [captura](TASK-006-1-colisao-seletiva.png)
e [diff](TASK-006-1.diff).

## 18/10/2026 - TASK-006: Passo Relâmpago e participação validados

Implementado LightningStepService com direção do mouse, custo 35, cooldown 4 s e até 150 px ao longo de 0,25 s. O primeiro deslocamento ocorre no tick após o lançamento, usando moveContinuous e a colisão X/Y existente. WASD é suspenso em cada tick iniciado com dash, inclusive o último fracionário. Colisões consomem o tempo sem compensar distância depois. Sem dano, status ou invulnerabilidade.

Shift usa a mesma fila de SkillCastCommand que Q, com despacho FIFO e saldo compartilhado. Bindings contemplam modificador Shift e os controles anteriores. R limpa dash, rastros e cooldown sem repetir Shift mantido. Snapshot publica rastros imutáveis amarelos de 0,18 s e o painel apenas desenha.

Validação inicial: 118 testes. Primeira entrega: 146 testes. Após a contribuição do usuário: `mvn clean test`, BUILD SUCCESS, **147 testes sem falhas ou erros**. Testes e baselines anteriores intactos. Captura renderizada inspecionada; sessão interativa da janela não realizada.

O usuário escreveu LightningStepZeroDirectionTest.deveRejeitarDashSemDirecao. Revisão confirmou ZERO_DIRECTION, vigor e posição preservados, cooldown zerado e dash inativo. O teste passou na suíte completa e seu código foi preservado integralmente. A participação reservada está concluída; não houve correção obrigatória. Relatório, PDF, resumo e diff foram atualizados.

Entrega: [relatório e participação](TASK-006.md), [PDF](Spirit_Gate_TASK-006.pdf), [testes](TASK-006-testes.txt), [captura](TASK-006-passo-relampago.png) e [diff](TASK-006.diff).

## 18/10/2026 - Colisões com deslizamento lento

Adicionada assistência tangencial em bloqueio total por corpos e quinas externas.
SOFT_SLIDE passa a ser a resposta padrão do DebugWorld para jogador e mobs.
Movimento livre e deslizamento X/Y normais mantêm a velocidade; o auxílio usa
fator 0,45 e valida o percurso inteiro, sem reduzir raios ou empurrar atores.
Freeze continua impedindo movimento; combate e recursos não foram alterados.

O modo STRICT preserva o comportamento anterior. MovementReplayTest passou a
selecioná-lo explicitamente, mantendo os três baselines, já que a trajetória nova
é uma mudança intencional. Treze testes novos cobrem o modo SOFT_SLIDE.
Validação final: `mvn clean test -q`, **118 testes aprovados**, sem falhas ou erros.
Sem sessão interativa da janela nesta etapa.

Entrega: [decisões e arquivos](DESLIZAMENTO-COLISOES.md),
[testes](DESLIZAMENTO-testes.txt) e [diff](DESLIZAMENTO.diff).

## 18/10/2026 - TASK-005: Efeitos temporários e Freeze

Criados armazenamento por entidade, serviço de aplicação/atualização de efeitos e ProbabilityService com RandomGenerator injetável. MobFactory anexa componente vazio. Criostase sorteia 30% por dano positivo sobrevivido e aplica Freeze por 1 s; sucesso renova sem acumular, falha não altera. Dano, cinco pulsos, custo, cooldown e alcance foram preservados.

Efeitos antigos avançam antes dos pulsos e comandos. Aplicações novas conservam 1 s inteiro e bloqueiam movimento no próprio tick. MovementService bloqueia mobs congelados nos caminhos discreto, direcionado e contínuo; ataques em alcance continuam ativos. Snapshot publica status por valor em lista separada de ActorView. Painel apenas desenha contorno, tonalidade e legenda.

Validação inicial: 73 testes. Final: `mvn clean test`, BUILD SUCCESS, **105 testes sem falhas ou erros**. Confirmados 0%, 30%, 100%, amostras 0,29/0,30, independência, renovação, expiração, mortos, dano zero/letal, movimento e ataque. Os testes e três baselines antigos foram mantidos. Captura renderizada inspecionada; sessão interativa da janela não realizada.

Entrega: [relatório](TASK-005.md), [PDF](Spirit_Gate_TASK-005.pdf), [testes](TASK-005-testes.txt), [captura](TASK-005-freeze.png) e [diff](TASK-005.diff).

## 18/10/2026 - TASK-004: Criostase

Implementada a primeira habilidade específica: Q captura o alvo do mouse e publica SkillCastCommand imutável na fila. Custo 55 de vigor, cooldown 3 s, clamp de alcance 240, raio 85, duração 2,5 s e cinco pulsos de 7 ICE em 0/0,5/1/1,5/2 s. Sem sexto pulso, congelamento ou lentidão. Paredes não bloqueiam; hitboxes tangentes são incluídas.

CombatService permanece o único caminho de dano e não foi modificado. ICE foi anexado ao final do enum, com afinidade racial padrão 1,0. Zonas persistem no ponto lançado, consultam mobs vivos a cada pulso e são removidas na expiração ou reinício. Snapshot e Java2D publicam/desenham apenas valores; nenhuma nova thread, dependência ou framework genérico foi criado.

Validação inicial: 53 testes. Final: `mvn clean test`, BUILD SUCCESS, **73 testes sem falhas ou erros**. Os três baselines e o código do replay foram preservados. Testes de mouse/Q no EDT e runtime real cobrem toque rápido, repetição, liberação do cooldown e reset. Captura renderizada inspecionada; sessão interativa com JFrame não realizada.

Entrega para revisão técnica: [relatório](TASK-004.md), [PDF](Spirit_Gate_TASK-004.pdf), [testes](TASK-004-testes.txt), [captura](TASK-004-criostase.png) e [diff](TASK-004.diff). A próxima task aguarda revisão.

## 18/10/2026 - Pendências após TASK-003

Implementadas as pendências registradas: rejeição de entradas numéricas inválidas no movimento, fila FIFO que preserva transições rápidas entre ticks, varredura contínua opcional e índice espacial para corpos. DebugWorld reutiliza o contexto de colisão e atualiza o índice incrementalmente. A arena preserva a resolução discreta aprovada; a operação contínua fica disponível para deslocamentos rápidos futuros.

Validação inicial: 30 testes. Final: `mvn clean test`, BUILD SUCCESS, **53 testes sem falhas ou erros**. Os três baselines do replay foram mantidos. Testadas mil consultas indexadas contra a busca linear com 400 corpos e contatos de varredura, além da fila concorrente e de toques reais no EDT antes do primeiro tick.

As limitações registradas em documentos anteriores são históricas: a perda de toques rápidos e a aceitação de NaN foram corrigidas; a busca indexada já é usada pela arena; a varredura contínua está implementada como API explícita. A conferência manual da janela e a integração em branch de destino continuam fora da validação automatizada.

Entrega: [decisões e arquivos](PENDENCIAS-IMPLEMENTADAS.md), [testes](PENDENCIAS-testes.txt), [diff](PENDENCIAS.diff) e [PDF](Spirit_Gate_Pendencias_Tecnicas.pdf).

## 18/10/2026 - TASK-003

Adicionados StaminaComponent e StaminaService independentes de interface e rede. PlayerFactory anexa 100/100 de vigor; regeneração provisória de 15/s e esforço E de custo 20, uma vez por pressão, com recusa sem gasto quando insuficiente. Mortos não executam esforço nem regeneram. Reinício restaura vigor e exige nova pressão quando E estiver mantido.

DebugInput mantém o construtor antigo. Snapshot e painel exibem vigor. O runtime continua sendo o único proprietário das entidades. Combate básico permanece gratuito e os serviços de movimento/colisão não foram alterados.

Antes dos campos novos, a projeção dos campos antigos foi conferida em 1.800 ticks contra o snapshot original. Os três baselines do replay foram preservados. Validação inicial: 15 testes; validação final: `mvn clean test`, BUILD SUCCESS, 30 testes sem falhas ou erros. Ligações de E e reinício foram testadas com EDT e thread real da simulação, sem JFrame. Imagem da barra renderizada inspecionada; sessão manual interativa não realizada.

Entrega: [relatório e arquivos](TASK-003.md), [PDF](Spirit_Gate_TASK-003.pdf), [diff textual](TASK-003.diff) e [resultados](TASK-003-testes.txt).

## 18/10/2026 - TASK-002

Concluída a divisão aprovada entre MovementService (direção e deslocamento) e CollisionService (detecção e resolução). DebugWorld coordena os serviços para jogador e mobs, mantendo a simulação em uma thread e os snapshots desacoplados do domínio.

Mantidos os valores e a ordem dos cálculos, colisões discretas por eixo, tangência, deslizamento e exclusão de corpos mortos. Sem dependências novas, exclusões, renomeações ou implementação de multiplayer.

Validação: `mvn clean test`, BUILD SUCCESS, 15 testes sem falhas ou erros. As três sequências de regressão de 600 ticks coincidiram com o comportamento anterior à extração. Testes de combate e corte permanecem ativos. Não houve conferência manual da janela nesta etapa.

Decisões, arquivos e limitações: [relatório TASK-002](TASK-002.md). Documento exportado: [PDF TASK-002](Spirit_Gate_TASK-002.pdf).
