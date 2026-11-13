# TASK-225-R1 — Testes e evidências

Referência: 03/11/2026, branch `beco`, HEAD `6d72c65`. Java 17, Maven offline e cache existente. Checkpoint `.task-checkpoints/CORUJAO-IX/TASK-225-R1/`; nenhuma instalação ou acesso a banco.

## Rodadas focalizadas

| Rodada | Casos | Falhas / erros | Skips | Evidência |
|---|---:|---:|---:|---|
| Baseline de progressão/missão/resistência | 6 | 2 / 0 | 0 | `baseline-regressions.log`, `baseline-regressions-reports/` |
| Fixtures corrigidas + resistência preservada | 6 | 0 / 0 | 0 | `fixtures-first.log`, `fixtures-first-reports/` |
| Primeira compilação dos novos testes A-04 | Não executados | Erro de compilação de teste | — | `a04-first.log` |
| A-04 após ajuste de compilação | 12 | 0 / 0 | 0 | `a04-compiled.log`, `a04-compiled-reports/` |
| Primeira suíte geral R1 | 999 | 0 / 3 | 54 | `full-headless.log`, `full-headless-reports/`; classe sintética de teste ausente |
| VisualPackage após reconstruir artefatos | 4 | 0 / 0 | 0 | `visual-package-rebuilt.log`, XML preservado |
| Suíte geral reconstruída | 999 | 0 / 0 | 54 | `full-headless-rebuilt.log`, `full-headless-rebuilt-reports/` |
| Complemento Swing, 26 classes | 106 | 0 / 0 | 0 | `gui.log`, `gui-reports/` |
| Missão após reforço final de moedas/replay | 1 | 0 / 0 | 0 | `quest-coins-final.log`, XML preservado |

**Agregado final pelo XML mais recente de cada classe: 999 casos em 259 classes, todos aprovados, zero falhas, erros ou skips remanescentes.** Não somar rodadas repetidas. Os 54 skips headless foram exercitados no complemento Swing. `final-reports/` e `verification.json` preservam o agregado. Os 45 casos originais da TASK-225 e os 12 novos da R1 passaram.

O erro intermediário era uma chamada inexistente `Protocol.Actor.alive()` em teste novo. Foi substituída pela verificação de `hp()>0`, de acordo com o DTO real. Nenhuma produção foi alterada para contornar essa falha. A cena TCP de reconexão usa mob distante para reduzir interferência da IA no spawn; é preparação do cenário, não prova de um bug de admissão.

A primeira suíte geral teve três erros `NoClassDefFoundError: VisualPackageTest$1`. O bytecode do switch de enum referenciava essa classe sintética, ausente do diretório compilado. A origem da remoção é indeterminada; não se atribui automaticamente a uma ferramenta. Foram recompilados os 269 fontes de teste com `javac --release 17 -encoding UTF-8`, classpath extraído do XML e lista de argumentos preservada em `recompile-tests.args`, sem apagar arquivos/usar clean. A classe voltou, os quatro casos passaram e a suíte inteira foi repetida. O fonte `VisualPackageTest.java` permaneceu igual ao baseline. [Análise](../logs/analises/20261103-task225-r1-compilacao-testes.md).

## Comandos e limites de execução

A partir da raiz:

```powershell
python .task-checkpoints/CORUJAO-IX/TASK-225-R1/run-maven.py baseline-regressions `
  '-Djava.awt.headless=true' '-Dtest=Task208ProgressionTest,Task210QuestJourneyTest,EnemyDurabilityTest'

python .task-checkpoints/CORUJAO-IX/TASK-225-R1/run-maven.py fixtures-first `
  '-Djava.awt.headless=true' '-Dtest=Task208ProgressionTest,Task210QuestJourneyTest,EnemyDurabilityTest'

python .task-checkpoints/CORUJAO-IX/TASK-225-R1/run-maven.py a04-compiled `
  '-Djava.awt.headless=true' '-Dtest=Task225R1MaterializationTest,Task225R1PhaseObserversTest'

python .task-checkpoints/CORUJAO-IX/TASK-225-R1/run-maven.py full-headless-rebuilt '-Djava.awt.headless=true'
```

O wrapper preservado no checkpoint executa `mvn test -o` em `server/`, com `-Dmaven.repo.local` apontando para `.maven-repository` e `-Dspiritgate.logs.dir` para `TASK-225-R1/test-errors`. Limite externo de 600 segundos, com encerramento da árvore do processo em caso de timeout. Não há Maven concorrente. Os novos casos têm limites JUnit de 20/25 segundos e laços finitos; não dependem de a produção finalmente materializar para encerrar o teste.

O complemento usa `-Djava.awt.headless=false` e `-Dtest` com as 26 classes preservadas em `gui-tests.txt`; o reforço final usa `-Dtest=Task210QuestJourneyTest`. A suíte geral abrangeu o catálogo/engine/colisões/protocolo da auditoria 223, os testes 224/225, combate/recompensas, movimento, offline/Coop e World Studio. Entre as classes aprovadas estão `RpgSkillEngineTest` (103), `NetworkSkillPresentationTest` (39), `SkillProtocolTest` (6), `Task224SkillFeedbackTest` (9) e `EnemyDurabilityTest` (3). Os testes gráficos incluem foco, hotkeys/HUD e ferramentas existentes; nomes Radmin nessa seleção não indicam rede física.

## Progressão: finalidade preservada

`Task208ProgressionTest` passou mantendo XP esperado 3, XP do observador 0, nível 1 e próximo limiar 60. A fixture agora declara um encontro real com os 40 HP oficiais do catálogo, instalado antes da admissão dos sockets por `RevisionCandidate`/`installRevision`, com a mesma revisão, população e política `SYNTHETIC_PARTICIPANTS`. Ela primeiro confirma que a configuração normal de produção continua usando 10.000 HP.

O cliente seleciona o alvo e envia ataques normais por TCP. O helper espera **DEATH**, verifica HP zero nos dois clientes, lançador, vítima e geração 1, eventos iguais e uma única recompensa de 3. Ataque posterior e alvos inventados `xp_999999` nas operações existentes não alteram a progressão. Não existe setter de XP no contrato; essa tentativa de alvo inválido não deve ser apresentada como um novo comando wire de atribuição de XP.

Não se atribuem XP, morte, dano ou HP de entidade viva diretamente nestas fixtures. A inicialização de nível 15 nos mundos isolados de A-04 é preparação exclusiva daquela investigação, como a configuração sintética de nível do servidor; não participa das verificações de XP das TASKs 208/210.

O cenário de catálogo não mede o tempo necessário para matar o mob de treino de 10.000 HP. `EnemyDurabilityTest` conserva seus três casos, e o TCP do A-04 também confirma HP 10.000 sem modificar produção. A mudança remove a dependência incidental de `LauncherSession.start`; outros testes existentes continuam cobrindo o launcher.

## Missão: causa e fluxo real

O baseline falhou em READY/ACTIVE. Com a morte real agora observada, `Task210QuestJourneyTest` confirmou:

1. Aceitação normal: ACTIVE e progresso zero, enquanto o outro jogador permanece AVAILABLE.
2. DEATH da geração correta no ledger compartilhado: progresso 1/1 e estado READY; XP 3 somente para o contribuinte.
3. Entrega fora da Anciã recusada: estado e poções preservados.
4. Viagem/interação legítimas e entrega: COMPLETED e uma poção adicional.
5. Pedido repetido: ficha inteira e moedas do snapshot preservadas, sem recompensa adicional. Moedas não pertencem a `GameplayProtocol.Sheet`, por isso receberam asserção própria na revisão final, aprovada em `quest-coins-final.log`.
6. XP, moedas, inventário e objetivo do outro jogador preservados.

Não houve READY forçado, morte sintética para satisfazer a missão ou remoção de verificações corretas. O sucesso após DEATH real não revelou problema independente de contabilização do objetivo no cenário exercitado. Não é prova de todas as missões ou conteúdos possíveis.

## A-04: matriz adicional

| Cenário | Teste / evidência | Limite |
|---|---|---|
| Área livre | `normalMaterializationAndReleasedSupportKeepFourUniqueCuts` | Fase até 31, término normal aos 32. |
| Aliado bloqueia e depois libera | Mesmo caso, câmara descartável | Espera até 75, quatro cortes únicos; liberação permite apoio válido no tick seguinte. |
| Ausência completa e custo acumulado | `cumulativeConsecutiveSearchesWriteRawEvidenceWithoutPerformanceGuarantees` | 60 tentativas, ticks 32..91, um mundo e dois mundos sequenciais. Não é benchmark de sessões concorrentes. |
| Candidato de prazo total 92 | `isolatedNinetyTwoTickCandidateRemovesParticipationWithoutChangingRuntimePolicy` | Produção ainda intangível no tick 92; remoção candidata ocorre no próprio teste, fora do engine. Não fecha socket nem valida atomicidade de transporte. |
| Remoção, clear, perda, substituição, região | Cinco casos dinâmicos de `lifecycleCleanupDuringBlockedWaitingDoesNotTransferPhase` | Mundos descartáveis em espera bloqueada; sessão/mundo novo não herda fase. |
| Dano fatal / limpeza | `fatalDamageIsIgnoredDuringPhaseAndAcceptedAfterLifecycleCleanup` | Dano comum é ignorado durante a imunidade; morte só após cancelar a fase, sem simular morte natural enquanto imune. |
| Dois observadores com término ausente em um deles | `twoSerializedObserversExposeMissingTerminalAndExistingFiveSecondFuse` | Dois renderers independentes alimentados por round-trip dos DTOs existentes; não são três sockets simultâneos. |
| Materialização normal em TCP | `realCasterAndObserverReceiveNormalMaterializationAndFourUniqueImpacts` | Servidor real, lançador + observador, mesmos eventos de início/término e exatamente quatro HITs. |
| Reconexão durante fase | `reconnectDuringPhaseRemovesOldCasterAndAdmitsANewValidWorld` | Dois sockets; saída remove ator/eventos antigos, nova admissão recebe ID novo, domínio inicial e recargas novas; sem dano tardio. Não é retomada da identidade anterior. |

`LocalSimulationServer`, `RpgSessionWorld` e `SharedRpgEncounter` limitam a participação a dois. Não foi alterada capacidade para forçar lançador + dois observadores TCP simultâneos. Essa parcela do pedido foi investigada por dois renderers serializados e pela sessão TCP de dois participantes, com o limite explícito. A condição extrema sem apoio continua sintética, não demonstrada no mapa distribuído/Radmin.

## Custo cumulativo observado

Dados brutos da rodada mais recente: [a04-consecutive-searches.txt](skills-preview-task225-r1/a04-consecutive-searches.txt). O teste mede somente `engine.tick`; preparação dos mundos e `encounter.step` ficam fora do intervalo. Não há limiar de aprovação por tempo nem aquecimento dedicado da busca nesta rodada. JIT, GC e agendamento afetam os valores.

Rodada focalizada `a04-compiled.log`:

| Cenário | Rodadas | Total observado | Mediana por rodada | Maior rodada |
|---|---:|---:|---:|---:|
| Um mundo, uma busca por rodada | 60 | 70,934 ms | 1,118 ms | 2,022 ms |
| Dois mundos sequenciais, duas buscas por rodada | 60 | 112,861 ms | 1,649 ms | 3,303 ms |

Os totais são somas das amostras reais, não multiplicação de uma mediana. A grade/câmara corresponde ao limite derivado de 37.450 candidatos por tentativa; 60 buscas completas representam até 2.247.000 candidatos por mundo. A política proposta até o tick 92 incluiria 61 buscas, pois a primeira já ocorre no tick 32. Esse candidato não reduz o custo por busca e não foi implementado em produção.

O TXT pode ser regravado por uma nova rodada desta R1; os números da tabela identificam especificamente a focalizada. Logs/XML de cada rodada são preservados. Estes valores não incluem todo o custo do owner, transporte, renderização, várias sessões reais ou latência de rede.

## Preservação e limites de homologação

Comparação final: **291 arquivos protegidos de produção idênticos ao baseline**, 186 arquivos de skills preservados, incluindo 152 PNGs. Sonda do loader confirmou 71 clips/40 principais em `clip-count.log`. Somente as duas fixtures existentes e três documentos autorizados mudaram entre os 2.715 arquivos protegidos; os novos testes/documentos ficam em inventário próprio. Zero mudanças inesperadas. O corpo do relatório original da TASK-225 foi preservado integralmente, acrescentando apenas referência ao adendo.

Foram arquivadas e restauradas **82 capturas históricas** regeneradas pelo complemento e o TXT histórico `skills-preview-task225/a04-measurements.txt`, usando os bytes do baseline R1. Versões da execução atual ficam em `suite-generated/`. O TXT novo desta R1 mantém as medições mais recentes. Checkpoints 223–225 não foram alterados. Comparação e hashes em `verification.json`/`deliverable-hashes.json`.

A correção visual da TASK-225 e o resultado A-03 continuam preservados; nenhum opcode, mensagem, inferência de identidade ou algoritmo de trajetória foi adicionado.

Radmin físico, hardware distinto, launcher nativo distribuído e homologação estética continuam pendentes. Aprovação de regressão não encerra A-04: a [ADR](TASK-225-ADR-A04-PROPOSTA.md) segue proposta, sem decisão ou implementação de produção.
