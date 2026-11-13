# TASK-225 — Testes, capturas e limites

Referência: 03/11/2026, JDK 17.0.20.1, Maven 3.10.0 e cache local existente. Nenhuma dependência instalada. Checkpoint `.task-checkpoints/CORUJAO-IX/TASK-225/`.

## Resultado das rodadas

| Rodada | Casos | Falhas / erros | Ignorados | Evidência |
|---|---:|---:|---:|---|
| Reprodução anterior à correção visual | 6 | 6 / 0 | 0 | `presentation-before.log`, `presentation-before-reports/` |
| Primeira focalizada, novos testes + apresentação/protocolo existentes | 89 | 1 / 0 | 0 | `focused-first.log`; única falha de fixture A-04, descrita abaixo |
| A-04 após corrigir fixture | 12 | 0 / 0 | 0 | `a04-fixture-fixed.log` |
| Alvo lateral + sequência raster | 1 | 0 / 0 | 0 | `moving-target.log` |
| Suíte completa headless | 987 | 2 / 0 | 54 | `full-headless.log`, `full-headless-reports/`, `full-headless-summary.json` |
| Complemento gráfico, 26 classes | 106 | 1 / 0 | 0 | `gui.log`, `gui-reports/`; ausência de foco AWT em um caso |
| Repetição somente do caso de foco | 1 | 0 / 0 | 0 | `gui-focus-rerun.log`, `gui-focus-rerun-reports/` |
| Missão anterior, isolamento da falha | 1 | 1 / 0 | 0 | `task210-isolated.log`, XML arquivado |
| Progressão anterior, isolamento da falha | 2 | 1 / 0 | 0 | `task208-isolated.log`, XML arquivado |

**Os 45 novos casos da TASK-225 passaram na suíte completa. A suíte global não está aprovada.** A comparação com os relatórios da TASK-224 não deve ocultar a evolução do checkout entre as entregas. XMLs são arquivados por rodada; não somar execuções repetidas como casos adicionais aprovados.

Agregado pelo XML mais recente de cada classe: **987 casos em 257 classes; 985 aprovados, duas falhas, zero erros e zero skips remanescentes**. As 54 verificações ignoradas em headless foram exercitadas no complemento gráfico. A aprovação focalizada posterior de `GamePanelsIntegrationTest` substitui somente seu resultado de foco; não apaga a ocorrência da rodada inicial nem as falhas de progressão/missão. Agregado em `final-reports/` e `verification.json`.

## Comandos executados

Comando Maven a partir de `server/`, usando somente o cache:

```powershell
mvn test -o `
  "-Dmaven.repo.local=C:/Users/lucas/Documents/GitHub/Projeto-K/.maven-repository" `
  "-Dspiritgate.logs.dir=C:/Users/lucas/Documents/GitHub/Projeto-K/.task-checkpoints/CORUJAO-IX/TASK-225/test-errors" `
  "-Djava.awt.headless=true"
```

As rodadas focalizadas acrescentaram, respectivamente, `-Dtest=Task225ProjectilePresentationTest`, `-Dtest=Task225*,NetworkSkillPresentationTest,SkillProtocolTest`, `-Dtest=Task225MaterializationTest`, `-Dtest=Task225MovingProjectileVisualTest`, `-Dtest=Task210QuestJourneyTest` e `-Dtest=Task208ProgressionTest`. Argumentos com vírgulas foram passados entre aspas no PowerShell.

O complemento gráfico usou `-Djava.awt.headless=false` e as 26 classes da lista preservada em `gui-tests.txt`; a repetição usou `-Dtest=GamePanelsIntegrationTest`. Inclui foco/input, hotkeys, HUD, lifecycle de janelas, launcher local e World Studio. O teste denominado Radmin nessa seleção continua sendo local.

Suíte geral e rodadas finais usam o wrapper de evidência `run-maven.py` no checkpoint, com limite externo de 600 segundos e encerramento da árvore do processo em caso de timeout. Os testes novos de física, A-04 e TCP também têm limites JUnit e laços finitos. Nenhum timeout foi necessário para encerrar a investigação.

## Matriz dos cenários

| Cenário | Evidência realmente executada | Resultado / limite |
|---|---|---|
| Alvo parado e móvel lateral | `Task225MovingProjectileVisualTest`, `RpgSkillEngineTest`, TCP novo | Contato real respeitado; curva intermediária visual difere, medição abaixo. |
| Dois inimigos alinhados / corpos próximos | `Task225CollisionTest`, regressões do engine | Primeiro contato ordenado por fração do trecho e ID no empate; alvo selecionado não atravessa outro corpo. |
| Parede prévia e alvo que se move para trás de parede | Cinco dirigidos no teste de colisão; frontal TCP | Cast target bloqueado por LOS; após lançamento o cenário limita o voo. |
| Obstáculo fino | Parede de uma unidade, testes de engine/TCP/dashes | Varredura contínua conserva bloqueio; alvo atrás não recebe dano. |
| Borda do mapa | Dash e Body slam isolados; reentrada A-04 | Apoios válidos, sem aumento de hitbox ou teleporte. |
| Morte / geração nova durante voo | `Task225CollisionTest`; request TCP com geração inválida | Execução antiga não atinge o respawn; request inválido não inicia recarga. Morte/geração durante o voo foi testada no engine, não induzida em servidor TCP ativo. |
| Desconexão e viagem durante voo | Dois cenários reais em `Task225ProjectileTcpTest` | Servidor cancela sem dano/contato tardio; observador limpa o voo pelo snapshot sem lançador. |
| Evento repetido / mesmo tick | Novos testes de apresentação, trace e colisão; protocolo existente | Mesmo snapshot não reinicia efeito; mesmo tick não duplica dano; requests repetidos seguem recusados pelo servidor. |
| Três cortes, corrente, duas chuvas, leque e dois combos | `Task225CollisionTest.multipleFamiliesKeepTheirOwnHitBudgetsWhenTicksAreRepeated` | Sete famílias exercitadas dentro de um caso JUnit; leque não gera sete hits na vítima. Chuvas contam pulsos. Corrente conserva 20/15/11/8 no fixture. |
| Água, Passo cortante e relâmpago | Testes isolados de parede/aliado/direção e regressões de dash/renderer | Mobs atravessáveis conforme exceção existente; aliados e cenário sólidos; posição final válida. Não é homologação TCP de todas as direções de cada dash. |
| Body slam | Borda e contagem de impacto isolados; regressões de engine/renderer | Um impacto; deslocamento da vítima conserva sua geometria; atraso de pegada permanece visual. |
| A-04 livre / parede / borda / obstáculos / aliado | `Task225MaterializationTest` | Término normal no tick 32 ou busca de apoio livre, sem atravessar parede. |
| A-04 ausência total artificial / reentrada | Mesmo teste, 180 ticks limitados | Imunidade persistente reproduzida; quatro danos únicos; liberar apoio permite reentrada no tick seguinte. Regra não alterada. |
| A-04 perda/substituição de sessão / região / clear | Engine isolado com mundos reais | Mundo anterior limpo; próxima sessão não herda fase. Queda física de rede não foi simulada nestes casos. |
| A-04 evento terminal perdido | `Task225PhasePresentationTest` | Fusível visual de cinco segundos reproduzido; início repetido não reoculta. Diverge da espera autoritativa extrema. |
| Aplicações 6 e 7 | `Task225ProjectileTcpTest`, `SkillProtocolTest` | Mesmo dano autoritativo; cliente 6 não recebe frames de skills nem envia casts 7. Cliente 7 recebe lançamento/contato. |
| Movimento / offline / Coop / World Studio | Suíte geral e complemento gráfico | Cobertura dos testes existentes; não implica testes físicos nem distribuição. Falhas globais de progressão/missão estão abertas. |

## Reprodução visual A-03

[Galeria de cinco PNGs e interpretação](skills-preview-task225/README.md), [CSV](skills-preview-task225/trajetoria.csv), [JSON](skills-preview-task225/trajetoria.json).

Engine e renderer reais; alvo movido deliberadamente em cenário determinístico. A sonda lê `Projectile.point` por reflexão somente no código de teste e não altera produção. Os PNGs usam os clips originais; um segundo renderer com marcador permite medir o centro efetivamente desenhado sem copiar a fórmula de interpolação.

| Marco | Servidor / apresentação |
|---|---|
| Lançamento, tick 0 | `(140,300)` rumo ao alvo inicialmente em `(340,300)` |
| Último tick antes do contato, tick 10 | Alvo `(340,340)`; projétil real `(315,845;328,374)`; desenho `(320,300)`; desvio **28,676** |
| Primeiro contato, tick 11 / 367 ms no relógio de teste | Alvo `(340,345)`; contato `(321,054;331,959)`; HP 1.000 → 980 |
| Impacto visual recebido | Centro raster `(321,332)`; erro **0,068**, compatível com arredondamento |

O teste não simula a latência/buffer de uma sessão real. Os testes TCP separados comparam o tick do impacto com o evento HIT e os eventos recebidos pelo jogador/observador. A-03 não foi fechado como curva corrigida: a [proposta negociada](TASK-225-PROPOSTA-TRAJETORIA.md) permanece não implementada.

## A-04 e desempenho

[Relatório específico](TASK-225-A04-EVIDENCIAS.md). A grade final possui 36.425 pontos; somando origem e 1.024 sondas radiais, o pior caso sintético examina 37.450 candidatos por chamada. Contagem derivada dos laços, não instrumentação de produção. Três aquecimentos e 15 amostras por rodada, dados brutos no TXT/log; houve variação relevante entre rodadas, incluindo máximo de 31,664 ms na repetição focalizada. Não é benchmark de carga, garantia de FPS nem evidência de exploração no mapa real.

## Ocorrências durante a validação

1. Os seis testes anteriores à correção reproduziram projéteis visuais que permaneciam após ausência/morte do lançador. Foi feita a limpeza em `NetworkSkillPresentation.actors()`, com guarda de tick para snapshot anterior ao lançamento e sem reiniciar sequências. Sete casos finais passaram, incluindo essa proteção temporal.
2. Na primeira investigação A-04, atribuir `enemy.hp=0` diretamente causava respawn imediato (`readyTick=0`) e bloqueava novamente a câmara. A fixture passou a matar pelo ledger `skillDamage`; 12/12 casos aprovados na repetição. Nenhuma regra do engine foi alterada para contornar o teste.
3. A suíte completa e as repetições isoladas falharam em `Task208ProgressionTest.tcpKillAwardsXpOnceAndOtherCharacterCannotSendArbitraryXp` (XP esperado 3, observado 0) e `Task210QuestJourneyTest.realTwoClientSessionCompletesIndividualQuestAndCannotReplayReward` (READY esperado, ACTIVE observado). O piso de HP 10.000 já existia no baseline desta task; os ataques limitados dos testes não matam a vítima. Código envolvido permanece igual ao baseline. Não houve ajuste de HP/XP/missão ou alteração desses testes neste escopo. Análise em `logs/analises/20261103-task225-regressao-task210.md`.
4. O complemento Swing teve uma falha de `GamePanelsIntegrationTest`: `Evento sem componente focado após aguardar a transferência AWT`, em `SwingKeys.post`. A repetição isolada passou sem alterar fonte, teste ou helper. Não foi apresentada como falha do servidor nem omitida do histórico.

## Integridade e limites

Somente `NetworkSkillPresentation.java` foi modificado na produção. Comparação SHA-256 dos arquivos protegidos e contagens finais em `verification.json` no checkpoint. Loader confirmado em sonda JShell: **71 clips, 40 principais** (`clip-count.log`). Sprites, manifestos, catálogo, engine, colisão e protocolo preservados.

Os testes gráficos regeneraram **81 capturas históricas**. As versões desta rodada foram arquivadas em `suite-runtime/`, e os bytes anteriores foram restaurados do baseline. A comparação final dos 2.594 arquivos protegidos encontrou apenas a alteração de produção prevista e os dois documentos de progresso/pendências; nenhum asset ou fonte alheio foi modificado. Hashes dos novos entregáveis em `deliverable-hashes.json`.

Radmin físico, hardware distinto, launcher nativo distribuído, revisão estética humana e benchmark de carga continuam pendentes. O encerramento técnico não pode ser declarado aprovado com as duas regressões globais e o gate A-04 abertos. Nenhuma TASK-226 foi iniciada.
