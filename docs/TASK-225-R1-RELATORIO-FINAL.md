# TASK-225-R1 — Relatório final

Referência: 03/11/2026, branch `beco`, HEAD `6d72c6595dee6dbe4dea832fe6047ee81f51d4a3`. Adendo da TASK-225, sem iniciar TASK-226.

**R1 entregue localmente: regressões saneadas, produção preservada e investigação/ADR concluídas no escopo autorizado. Agregado final: 999 casos aprovados, zero falhas/erros/skips remanescentes. A-04 permanece bloqueante para a TASK-225 até decisão aprovada e implementação validada.**

## Resultado das regressões

As fixtures de progressão e missão foram corrigidas preservando a finalidade dos testes e o comportamento de produção. A reprodução inicial executou seis casos selecionados e apresentou as duas falhas anteriores. Após o ajuste, os mesmos seis casos passaram: XP, missão e três casos de resistência. Não se reduziu HP de produção, não se atribuiu XP/READY manualmente e não se simulou uma morte para satisfazer as asserções.

O piso de 10.000 HP é uma configuração intencional do encontro multiplayer de conteúdo, documentada no código e exigida por `EnemyDurabilityTest`; continua vigente. Os testes antigos dependiam incidentalmente da fábrica do launcher e tentavam vencer esse piso com até seis golpes básicos. Agora usam uma configuração de teste explícita com os **40 HP oficiais do catálogo**, preparada em memória antes dos sockets, pela API existente de revisão/encontro. O transporte, personagens, combate, IA, ledger, contribuições, XP e missões continuam reais.

`Task208ProgressionTest` observou DEATH/HP zero nos dois clientes, ID/geração iguais, uma recompensa de 3 XP para o contribuinte, nenhuma para o outro jogador e ausência de progressão extra por ataque posterior/alvos inventados. Preserva XP 3, nível 1 e próximo limiar 60.

`Task210QuestJourneyTest` verificou aceitação ACTIVE com progresso zero, DEATH real, READY com objetivo 1/1, recusa de entrega distante, viagem/interação, COMPLETED com uma poção adicional e replay sem nova recompensa. XP, moedas, inventário e missão do observador permaneceram isolados. A revisão final acrescentou asserções próprias de moedas do snapshot, pois elas não pertencem à ficha; o caso foi repetido e passou. Não houve evidência de problema independente de contabilização após a morte real no cenário exercitado.

Esses cenários de catálogo não homologam uma batalha completa contra 10.000 HP nem substituem o teste de durabilidade. A fábrica `LauncherSession.start` permanece coberta pelos outros testes do repositório; ela e todas as fontes de produção estão preservadas.

## Arquivos alterados

Arquivos existentes modificados em `server/src/test/java/com/projetok/multiplayer/`:

- `Task208ProgressionTest.java`: fixture explícita, verificação de morte/reward único, isolamento e tentativas inválidas sem XP arbitrário; asserções originais preservadas.
- `Task210QuestJourneyTest.java`: fixture explícita e verificação completa do objetivo/entrega/replay/isolamento pelo combate real.

Novos arquivos exclusivamente de teste:

- `multiplayer/Task225R1CombatFixture.java`: prepara encontro de catálogo antes da admissão e mata o slime por comandos TCP legítimos, esperando DEATH e HP zero.
- `multiplayer/Task225R1PhaseObserversTest.java`: três casos de observadores serializados, materialização TCP normal e reconexão durante fase.
- `rpg/Task225R1MaterializationTest.java`: nove casos de espera, custo acumulado, lifecycle, morte após limpeza e candidato isolado de retirada aos 92 ticks.

Documentação: `TASK-225-R1.md`, `TASK-225-R1-TESTES.md`, este relatório, `TASK-225-ADR-A04-PROPOSTA.md`, medições em `skills-preview-task225-r1/`, referência posterior no relatório original e atualização de progresso/pendências. A execução histórica da TASK-225 não foi reescrita.

## A-04: investigação ampliada

Os **12 novos casos passaram** na rodada focalizada. A espera sem apoio continua reproduzida; a produção ainda está intangível no tick 92 e aos 180 ticks do cenário de observadores. Quatro danos são aplicados uma única vez. Liberação do apoio permite materialização válida; limpeza de sessão/região/substituição evita herança da fase. Dano comum não mata durante a imunidade; a morte de teste ocorre somente depois de cancelar essa fase.

Dois renderers independentes receberam os mesmos eventos reais por round-trip do protocolo existente, com terminal deliberadamente ausente em um deles. Ambos atingiram o fusível visual de cinco segundos enquanto a simulação ainda esperava. Em TCP real, lançador + observador receberam os mesmos eventos normais e quatro HITs únicos; reconexão removeu ator/eventos antigos e admitiu ID novo, com domínio/recargas iniciais e sem dano tardio.

O limite real é de dois participantes no transporte, sessão e encontro. Não se alegam lançador + dois observadores TCP simultâneos. A demonstração de dois observadores foi isolada; aumentar a capacidade não está autorizado.

A medição adicional somou 60 tentativas consecutivas da busca, em vez de extrapolar uma mediana. Na rodada focalizada, um mundo acumulou **70,934 ms** (maior tentativa 2,022 ms); dois mundos sequenciais acumularam **112,861 ms** em 60 rodadas (maior rodada 3,303 ms). O intervalo mede somente `engine.tick`; não inclui atualização completa dos mundos, transporte ou carga concorrente. Dados brutos e rodadas em [TASK-225-R1-TESTES.md](TASK-225-R1-TESTES.md). Não são garantias de desempenho.

O candidato de 92 ticks está **somente no teste**: após observar que o runtime continua esperando, o cenário retira execução, mundo do mapa de participantes e participação no encontro. Não valida fechamento de socket, atomicidade ou uma política já aprovada. Nenhuma duração, imunidade, materialização ou saída de produção recebeu mudança.

## Decisão arquitetural necessária

A [ADR A-04 proposta](TASK-225-ADR-A04-PROPOSTA.md) compara A (prazo e saída), B (recuperação em apoio validado) e C (espera atual) nos critérios solicitados. Recomenda **A condicionada**, por oferecer término finito sem exigir apoio livre ou inventar coordenada de materialização.

Para autorizar implementação, o responsável precisa aprovar expressamente:

1. Fase normal de 32 ticks mais 60 de tolerância, com última tentativa de materialização no tick relativo 92.
2. Persistindo a falha, retirada coordenada de execução, corpo/sessão/encontro e peer. Remover só a skill não basta; o transporte não pode consultar snapshots privados depois de a sessão ser removida.
3. Readmissão pelo fluxo normal, em spawn revalidado e com possibilidade de recusa. Atualmente ela cria **outro personagem efêmero**, reinicializando progressão, domínio, inventário, moedas, missões e cooldowns. Se preservar identidade/progresso/cooldown for requisito, será necessária decisão e implementação própria antes de aplicar A.
4. Feedback genérico de conexão ou proposta separada para causa estruturada. `Protocol.Error` hoje não apresenta adequadamente uma razão específica após o handshake; nenhum opcode ou nova semântica foi inserido nesta R1.

O pedido desta R1 proíbe implementar essas mudanças sem nova aprovação explícita. A-04 continua um **gate separado e bloqueante**, mesmo quando a regressão de fixtures estiver aprovada. B não garante recuperação se o apoio reservado ficar ocupado; C mantém o risco reproduzido e a busca repetida sem limite.

## Validação, ocorrências e limites

Comandos, contagens por rodada e XML estão em [TASK-225-R1-TESTES.md](TASK-225-R1-TESTES.md). Não houve redução de cobertura ou alteração de asserção correta para produzir verde.

Suíte geral reconstruída: **999 casos, zero falhas/erros, 54 skips headless**. Complemento Swing: **106 execuções, zero falhas/erros/skips**. Reforço final de missão/moedas: um caso aprovado. O agregado mais recente por classe reúne **999 aprovados em 259 classes, sem falhas, erros ou skips remanescentes**, sem somar repetições. As duas regressões não permanecem abertas; A-04 é decisão/risco reproduzido, separado desse resultado. Exploração no mapa distribuído e capacidade de carga concorrente continuam não demonstradas.

Foram preservadas as ocorrências intermediárias: duas falhas do baseline, erro de compilação de teste por método inexistente e três erros de carregamento de `VisualPackageTest$1` na primeira suíte geral. A classe sintética estava ausente da saída compilada e era referenciada pelo switch de enum; sua origem de remoção não foi determinada. A recompilação dos 269 fontes de teste com JDK 17, sem apagar arquivos/usar clean, recuperou a classe. `VisualPackageTest` passou 4/4 isoladamente, com seu fonte idêntico ao baseline. [Análise local](../logs/analises/20261103-task225-r1-compilacao-testes.md).

Nenhum teste Radmin físico, hardware distinto, launcher nativo distribuído, homologação estética ou benchmark de carga foi executado. Não se deve declarar a TASK-225 plenamente homologada pela aprovação de testes locais.

## Baseline e integridade

Checkpoint R1 com Git/status/diff, pedido integral, 2.715 hashes protegidos, 557 fontes/testes e cópias anteriores. Não havia diferença frente aos hashes dos entregáveis finais da TASK-225 no início deste adendo. Todos os checkpoints 223–225 foram preservados.

Comparação final e hashes dos entregáveis ficam em `verification.json`/`deliverable-hashes.json`. A correção visual da TASK-225, os 152 PNGs, manifestos e 71 clips permanecem preservados. A-03 continua documentado com a mesma limitação intermediária; a proposta de trajetória não foi implementada. Produção, aplicações 6/7, autoridade, saves, offline/Coop, launcher, HUD/hotkeys e World Studio não receberam alterações de comportamento neste adendo.

Os **291 arquivos protegidos de produção** são idênticos ao baseline R1, assim como os 186 arquivos de skills. Foram restauradas 82 capturas históricas e a medição TXT original da TASK-225 depois de arquivar a execução atual. Entre os 2.715 arquivos protegidos, apenas as duas fixtures e os três documentos autorizados mudaram; zero alterações inesperadas. O corpo original do relatório da TASK-225 foi conferido integralmente, com somente a referência posterior acrescentada.

## Rollback e próximo passo

Reverter apenas os trechos das duas fixtures e retirar os três novos arquivos Java após conferir referências, usando o baseline R1. Reverter somente a referência posterior no relatório original e as mudanças documentais desta R1, preservando evidências históricas e trabalho posterior. Artefatos de compilação são reconstruíveis; não há formato wire, save ou banco para migrar/reverter. Não usar restauração global/reset/clean.

Próximo passo sugerido: revisar e decidir a alternativa da ADR, incluindo as consequências da readmissão efêmera. Não implementar A-04 nem iniciar TASK-226 automaticamente. Sem commit, push, release, publicação, migração ou instalação.
