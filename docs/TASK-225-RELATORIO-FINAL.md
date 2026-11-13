# TASK-225 — Relatório final

> **Referência posterior — TASK-225-R1:** o [adendo corretivo](TASK-225-R1-RELATORIO-FINAL.md) saneou as fixtures de progressão/missão preservando a produção e preparou a [ADR A-04](TASK-225-ADR-A04-PROPOSTA.md). O texto e os resultados abaixo conservam a execução original da TASK-225; as duas falhas não foram apagadas do histórico. A-04 continua um gate separado, sem autorização de implementação.

Entrega local de 03/11/2026, branch `beco`, HEAD `6d72c6595dee6dbe4dea832fe6047ee81f51d4a3`. **Auditoria e correção localizada entregues; encerramento técnico bloqueado por duas regressões preexistentes e decisão pendente sobre A-04.** TASK-226 não iniciada.

## Resultado

As dez famílias e as 40 habilidades foram auditadas na [matriz de colisões](TASK-225-MATRIZ-COLISOES.md), incluindo primeiro contato, cenário, múltiplos, investidas e exceções físicas. Os testes não demonstraram necessidade de mudar o cálculo autoritativo de colisão, perfil físico, hitboxes, dano ou contadores.

Foi reproduzida e corrigida uma falha de apresentação: o servidor cancelava a habilidade quando o lançador saía/morria, mas um observador mantinha o projétil desenhado até expirar. `NetworkSkillPresentation.actors()` agora descarta efeitos cujo lançador esteja ausente/morto em snapshot de tick igual ou posterior ao evento. Um snapshot anterior não cancela um lançamento recém-recebido. A sequência monotônica permanece, impedindo recriação por evento repetido. Nenhuma decisão física foi transferida ao cliente.

Os seis casos iniciais dessa falha foram executados antes da correção e falharam; sete casos finais, incluindo a proteção temporal, passaram. Dois cenários TCP reais de saída/viagem confirmaram cancelamento autoritativo sem dano tardio e limpeza visual no observador.

## A-03: reprodução e limite de continuidade

Engine e renderer reais, em cenário controlado com alvo lateral, mediram desvio intermediário máximo de **28,676 unidades**. O contato real ocorreu no tick 11 em `(321,054;331,959)`; o impacto foi desenhado nessa coordenada com erro raster de **0,068**. O HP só caiu no contato autoritativo. [Cinco imagens, CSV e JSON](skills-preview-task225/README.md).

A curva intermediária **permanece diferente**: aplicação 7 não informa alvo/geração, identidade do projétil ou amostras de trajetória. Não foi adivinhado um alvo por proximidade nem alterada a simulação para caber na animação. A [proposta de extensão negociada](TASK-225-PROPOSTA-TRAJETORIA.md) descreve IDs de execução/projétil, amostras e terminais limitados, preservação 6/7 e testes necessários. Ela não foi implementada.

Morte/geração do alvo durante voo cancelam o executor, mas o contrato atual não identifica no cliente qual projétil deveria receber esse cancelamento sem evento terminal. Sua representação pode persistir até o prazo visual. A correção desta entrega cobre o lançador removido/morto, com identidade confirmada no snapshot; não anuncia solução universal de cancelamento por alvo.

## A-04: risco reproduzido, regra preservada

Em câmara artificial sem apoio válido, Quatro relâmpagos manteve fase e imunidade aos **180 ticks**, enquanto o fusível visual expira em cinco segundos. Aplicou somente quatro cortes, sem repetir dano na espera. A reentrada ocorreu no tick seguinte à liberação do apoio. Não foi demonstrado que o mapa distribuído permita forçar essa condição; a reprodução não é uma exploração validada em Radmin.

A busca completa percorre até **37.450 candidatos** no fixture 960 × 640 (contagem derivada da implementação). Foram registradas 15 amostras após três aquecimentos em cada rodada; a variação entre rodadas impede tratar os tempos como garantia de desempenho. A mediana inicial foi 0,943 ms e uma repetição teve máximo de 31,664 ms. [Evidências e proposta de término seguro](TASK-225-A04-EVIDENCIAS.md).

**Decisão:** bloquear continuidade dos gates que dependam de A-04 resolvido. Conforme a restrição explícita do pedido, não mudar imunidade, duração nem materialização sem autorização específica. A proposta sugere avaliar 60 ticks adicionais após os 32 atuais e, persistindo a falta de apoio, retirar a participação pelos caminhos de saída, com readmissão somente em spawn válido. Isso tem custo de experiência e exige revisão/aceite; não foi aplicado.

## Arquivos alterados e entregáveis

Único arquivo de produção alterado:

- `server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java`: limpeza dos efeitos cancelados pelo lifecycle observado; Javadoc em português explica o cuidado com ticks.

Novos arquivos de teste, em `server/src/test/java/com/projetok/`:

- `multiplayer/Task225ProjectilePresentationTest.java`: sete casos de ausência, morte, retransmissão e tick antigo.
- `multiplayer/Task225ProjectileTcpTest.java`: cinco casos com servidor real e dois clientes locais, versões 6/7, parede e saída/região.
- `multiplayer/Task225MovingProjectileVisualTest.java`: um caso de medição raster, sequência e contato.
- `multiplayer/Task225PhasePresentationTest.java`: dois casos do fusível/ciclo visual A-04.
- `rpg/Task225CollisionTest.java`: 18 casos; inclui sete famílias múltiplas dentro de um caso, sem inflar contagem.
- `rpg/Task225MaterializationTest.java`: 12 casos de materialização, custo e ciclo de sessão.
- `rpg/Task225ProjectileTrace.java`: helper de observação exclusivamente de teste; não acrescenta casos JUnit.

Documentos: `TASK-225.md`, `TASK-225-TESTES.md`, este relatório, `TASK-225-MATRIZ-COLISOES.md`, `TASK-225-A04-EVIDENCIAS.md`, `TASK-225-PROPOSTA-TRAJETORIA.md`, galeria/medições e atualizações em `CORUJAO-IX-PROGRESSO.md`/`CORUJAO-IX-PENDENCIAS.md`. Diagnóstico das duas regressões em `logs/analises/20261103-task225-regressao-task210.md`.

## Validação e impedimentos

Os **45 novos casos passaram** na suíte completa. A rodada headless executou **987 casos**, com **duas falhas, zero erros e 54 ignorados**. As falhas foram confirmadas por reexecuções isoladas, sem alteração de código:

- `Task208ProgressionTest`: esperava 3 XP, recebeu 0.
- `Task210QuestJourneyTest`: esperava missão READY, recebeu ACTIVE.

Ambos tentam matar um mob com no máximo seis ataques básicos, mas o checkout já impunha piso de **10.000 HP** antes desta task. A classe `EnemyDurabilityTest`, também anterior e aprovada, exige esse piso; o código envolvido coincide com o baseline SHA-256 da TASK-225. A base atual contém 942 casos, diante dos 939 históricos da TASK-224. Não é falha causada pelo renderer nem simples corrida entre snapshots. Não se alteraram HP, XP, missão ou testes anteriores para apresentar resultado verde. É necessária decisão própria sobre a resistência dos inimigos e a intenção dessas fixtures.

Comandos, rodadas gráficas, XML e limites estão em [TASK-225-TESTES.md](TASK-225-TESTES.md). A suíte geral não deve ser anunciada como aprovada. Radmin físico, hardware distinto, launcher nativo distribuído, homologação estética humana e benchmark de carga permanecem pendentes.

Complemento gráfico: **106 execuções**, com uma falha de ausência de foco AWT; o único caso foi repetido e passou sem mudança de código/teste/helper. Agregado mais recente por classe: **987 casos em 257 classes, 985 aprovados, duas falhas, zero erros e zero skips remanescentes**. Os 54 skips headless foram cobertos pelo complemento; a ocorrência de foco inicial permanece registrada.

## Baseline, hashes e compatibilidade

Checkpoint `.task-checkpoints/CORUJAO-IX/TASK-225/`: pedido integral, Git/HEAD/status/diff anterior, 2.594 hashes protegidos e cópias de fontes/documentação/capturas anteriores. Checkpoints 223/224 preservados. Inventário final e hashes dos novos entregáveis em `verification.json` e `deliverable-hashes.json`.

| Fonte modificada | SHA-256 |
|---|---|
| Baseline de `NetworkSkillPresentation.java` | `e7604a426c243ac2dc1824a3a243c21b53106928928155f7847b544459f35737` |
| Depois da correção | `30c8532df0f0ba67ee55b6e43b9a33f1cfa740ed819b203a8eec809392c559d5` |

Foram preservados os 186 arquivos protegidos de skills, incluindo **152 PNGs**; loader confirmou **71 clips / 40 principais**. Engine, catálogo, colisões, movimento, `SkillProtocol`, `Protocol`, `RemoteTimeline`, HUD/hotkeys, preview, launcher, World Studio e saves não receberam mudanças de produção. Offline/Coop conservaram seus caminhos e testes existentes. Aplicações 6/7 foram exercitadas em TCP real local sem mudar serialização, valores ou negociação. Os testes Swing não substituem teclado/hardware/Radmin físicos.

As **81 capturas históricas** regeneradas pelo complemento foram arquivadas no checkpoint e restauradas ao conteúdo anterior. A verificação final dos 2.594 arquivos protegidos encontrou somente o arquivo de produção previsto e os dois documentos de progresso/pendências alterados; zero mudanças inesperadas. Nenhum teste anterior foi modificado nesta task. Novas imagens e dados ficam exclusivamente na galeria da TASK-225.

## Reversão e próximo passo

Reverter somente o bloco novo e seu Javadoc em `NetworkSkillPresentation.actors()`, usando a cópia do checkpoint e comparando mudanças posteriores. Retirar os sete novos arquivos Java e documentos/galeria específicos somente após verificar referências. Progresso/pendências têm cópia anterior. Não há migração nem alteração wire/save a reverter; não usar reset/clean ou restauração global.

Próximo passo sugerido, sem executar: revisar o comportamento extremo A-04 e a decisão sobre piso de HP/fixtures que bloqueiam o encerramento técnico; aprovar uma proposta de protocolo somente se for exigida a curva intermediária exata. Nenhuma TASK-226, commit, push, release, publicação ou build privada foi iniciada.
