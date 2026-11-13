# TASK-225-R2 — Testes e evidências

Referência: 03/11/2026, branch `beco`, HEAD `6d72c65`. Baseline recebido: 999 casos aprovados na R1. Evidências R2 em `.task-checkpoints/CORUJAO-IX/TASK-225-R2/`.

## Rodadas

| Rodada | Casos | Falhas / erros | Skips | Resultado |
|---|---:|---:|---:|---|
| `lab-first` | 13 | 1 / 2 | 0 | Expectativa incorreta após retirar alvo antes de 32; identificador de skill incorreto no teste. |
| `lab-corrected` | 13 | 1 / 1 | 0 | Renderer conservou corretamente a fase do observador; sequência zero foi recusada pelo construtor do DTO. |
| `lab-valid` | 13 | 0 / 0 | 0 | Correções restritas aos novos testes. |
| `lab-repeat` | 25 | 0 / 0 | 0 | R2 e os 12 casos de A-04 da R1. |
| `lab-final` | 26 | 0 / 0 | 0 | R2 ampliada para 14 casos, fila real e desconexão espontânea, mais os 12 da R1. |
| `full-headless` | 1.013 | 0 / 0 | 54 | Regressão completa, BUILD SUCCESS. |
| `gui` | 106 | 0 / 0 | 0 | Complemento gráfico de 26 classes, BUILD SUCCESS. |

Logs e XMLs por rodada são preservados; o resumo de `lab-first` corresponde aos XMLs copiados antes da correção. A suíte não perdeu casos: 999 anteriores + 14 novos = 1.013. Rodadas repetidas não são somadas no agregado por classe.

**Agregado final por XML mais recente de cada classe: 1.013 casos em 261 classes, zero falhas, erros ou skips remanescentes.** Os 54 skips headless foram exercitados no complemento gráfico. Os 14 novos casos passaram também na regressão completa; não se somam as 106 execuções gráficas aos 1.013 casos. `final-reports/` e `verification.json` preservam o resultado.

Integridade final: 291 arquivos de produção e 186 arquivos de skills (152 PNGs) idênticos ao baseline R2. Entre os 2.722 arquivos protegidos, só os dois documentos de progresso/pendências receberam os acréscimos autorizados. As saídas históricas regeneradas foram arquivadas em `suite-generated/` antes de restaurar as cópias recebidas. Checkpoints e relatórios 225/R1 preservados.

## Matriz do modelo isolado — 11 casos

`Task225R2DeadlineLabTest`, timeout de 20 segundos por teste:

| Cenário | Evidência |
|---|---|
| 31, 32, 91, 92, 93 sem apoio | Fase aos 31, quatro HITs aos 32, nenhuma retirada aos 91, tentativa real precedendo retirada candidata aos 92, nenhuma repetição aos 93. |
| Apoio antes de 32/91/92 | Três parametrizações: materializa e permanece admitido. Aos 32 a liberação do fixture retira o alvo antes dos cortes, portanto não há HITs contra esse alvo; aos 91/92 os quatro HITs anteriores permanecem únicos. |
| Apoio após retirada e pedido duplicado | Não ressuscita a sessão; não repete retirada, quatro HITs, XP/inventário ou recompensa. Morte de preparação após retirada não credita o personagem ausente. |
| Runtime sem candidato | Engine real permanece intangível aos 93, comprovando que o timeout não foi ativado. |
| Saída espontânea durante espera | Limpeza aos 60; pedido antigo é recusado até depois de 92. O TCP correspondente é exercitado separadamente. |
| Região/revisão/execução/identidade/outro ID | Cinco parametrizações recusam token antigo sem retirar o outro participante. Região usa viagem real; revisão modela mudança de época com limpeza real do engine; identidade substitui mundo sob mesmo ID somente em memória. Não é publicação real nem nova execução TCP de 92 ticks. |

Mundos e encontro reais, câmara artificial herdada do desenho da R1, participante bloqueador inserido depois do cast. O encontro usa configuração explícita de HP mínimo 1.000 de teste; o piso de produção de 10.000 não é alterado. O alvo é enraizado exclusivamente na preparação. O método de liberação mata o alvo pela API de dano como preparação declarada; essa morte não é apresentada como um quinto corte legítimo da técnica.

## Matriz TCP — três casos

`Task225R2ShutdownTcpLabTest`, timeout de 30 segundos por caso:

- Duas parametrizações: lançador aplicação 7, observador aplicação 6 ou 7. Ambos usam `LocalClient`, `LocalSimulationServer`, coordenadores de revisão e handshake reais, loopback e porta efêmera. Só o renderer é imediato, como nas fixtures anteriores.
- Pedido interno equivalente injetado durante a fase real, com validação de ID, referência do mundo, região, hash e sequência inicial. Pedidos de outro ID, hash antigo e execução inválida são recusados; duplicata após retirada também é recusada.
- Adaptador chama `RpgSessionWorld.leave` no owner `coop-world`, limpa intangibilidade/dash, conserva tombstone em coleção exclusiva de teste e o expõe pelo `disconnectRequested` existente.
- Peer fecha, cliente deixa de estar conectado, `skills()` fica nulo e novos comandos locais são recusados. Uma retirada lógica é contada; o callback posterior `departures -> leave` é observado uma vez e não repete a retirada lógica.
- Zero chamadas de `skillsSnapshotFor`/`gameplaySnapshotFor` com sessão ausente. Observador recebe snapshot sem ator antigo; renderer descarta a fase desse ator. Na aplicação 7, a fase do outro personagem e sua identidade permanecem ativas imediatamente após a retirada.
- Comandos de movimento, skill e gameplay são colocados na **fila real de comandos do servidor antes de leave**, para drenagem no próximo tick, e recusados pelo ID antigo. O wire vincula o ID ao peer: não se inventa um comando de cliente para escolher ID arbitrário.
- Spawn é bloqueado por injeção exclusivamente de teste na geometria de admissão do coordenador. Reconexão é recusada sem corpo parcial; a geometria é restaurada em `finally`. Não altera cenário em arquivo nem colisões de mundos já admitidos.
- Depois, readmissão aceita outro ID e UUID, domínio `none` e cinco recargas zero. Sequência repetida após comando válido é recusada. Observador continua conectado e avança mais de três ticks; servidor não registra falha.
- Terceiro caso fecha espontaneamente o cliente durante a fase. O fluxo existente retira uma vez, limpa flags e invalida o pedido candidato atrasado/duplicado, mantendo o observador.

Em nenhum momento existem três clientes TCP simultâneos. A tentativa de readmissão ocorre depois de o lançador anterior estar desconectado; uma tentativa recusada termina antes da seguinte. A causa estruturada A-04 não existe no wire: o evento observado pelo cliente é encerramento da conexão.

## Reprodução e limites

Na raiz do checkout:

```powershell
python .task-checkpoints/CORUJAO-IX/TASK-225-R2/run-maven.py lab-final `
  '-Djava.awt.headless=true' '-Dtest=Task225R2*,Task225R1PhaseObserversTest,Task225R1MaterializationTest'
python .task-checkpoints/CORUJAO-IX/TASK-225-R2/run-maven.py full-headless '-Djava.awt.headless=true'
$r2GuiCases = (Get-Content -Encoding UTF8 .task-checkpoints/CORUJAO-IX/TASK-225-R2/gui-tests.txt -Raw).Trim()
python .task-checkpoints/CORUJAO-IX/TASK-225-R2/run-maven.py gui '-Djava.awt.headless=false' "-Dtest=$r2GuiCases"
```

O wrapper executa `mvn.cmd test -o` em `server`, cache `.maven-repository` existente e `spiritgate.logs.dir` no checkpoint. Limite externo de 600 segundos, encerrando árvore em timeout. Sem instalações e sem execuções Maven concorrentes. Esperas TCP de até sete segundos, jobs no owner/fila com cinco segundos, avanço simulado finito. O TCP não mede carga nem garante prazo de CPU do owner; demonstra que o observador continua recebendo ticks.

A prova de prazo e a prova de transporte não são uma integração definitiva. Nenhum teste faz o engine de produção emitir pedido aos 92. A prova TCP ocorre durante fase normal com causa equivalente; o estado sem apoio até 92 pertence à câmara isolada. Falta testar o patch final com pedido originado no engine, geração ativa, queries privadas concorrentes, expiração de tombstones e falta real de apoio até 92 por TCP.

Homologação física, Radmin entre computadores, launcher nativo distribuído, carga sustentada e estética humana não foram executadas. ACK/trajetória e TASK-226 não foram iniciados.
