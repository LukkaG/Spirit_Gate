# Corujão VI — relatório final

Veredito final da TASK-200: **VALIDADA COM RESSALVAS no laboratório local**. Homologação física Radmin e teste do atualizador sob política autorizada continuam pendentes. Nenhum lançamento foi realizado. Branch beco, HEAD ad66caed14e3f94bc369f7edd9861900d5a68636; alterações preexistentes preservadas.

## Regressão final executada

TASK-200: seis testes específicos da confirmação negativa/faults aprovados; Java headless 613 executados, 603 aprovados, dez abortos GUI, zero falhas/skips; Java gráfico 613/613, zero abortos/falhas/skips; Python 33/33 com parser Java recém-compilado. Rehash integral de 31.629 arquivos sem divergência e dois ZIPs/jars conferidos novamente. Atualizador PowerShell abortado pela política vigente, zero checks, sem bypass. Evidência final: `.task-checkpoints/TASK-200/corujao-vi/run-20261026T002631Z-cdcb97d1`, verdict.json e logs/commands.

## Resultado funcional

A confirmação falsa da TASK-186 foi corrigida no código principal: accepted deve ser verdadeiro e hash deve corresponder, mantendo validação de fase e recuperação existentes. Teste negativo reproduzido antes da correção e repetido na regressão final; uma recusa não confirma candidata, não avança ponteiro e recupera os clientes. Nenhuma classe da proposta substituiu a implementação.

Também foram corrigidos bugs reproduzidos: saída de conexão recusada não cancela barreira/reserva; fila de conteúdo reserva capacidade para um Pong legítimo; encerramento resolve futures pendentes sem executar trabalho após close. Métricas locais limitadas e JSON sem credenciais foram acrescentados. Testes exercitam clientes V1–V4, ACKs atrasados/duplicados/alheios, reconnect, falhas de staging/ativação/ponteiro, peers lentos, flood limitado e processo interrompido.

ADR-193 aprovado formalmente pelo responsável, com hash da proposta e decisão em checkpoint separado. 194–196 implementam somente laboratório sintético: UUID de backup validado, CAS estrito, lock conflitante como erro tratado, retry de snapshot inteiro idêntico na base+1, handles/grants privados e efêmeros, uma posse por registro local, restart em outro processo e recompensas persistidas sem duplicação nos replays descritos. Sem integração ao launcher, contas públicas ou saves pessoais.

197 confirmou falta/adulteração e limites de PNG com catálogo preinstalado. 198 gerou dois ZIPs privados novos, allowlist e manifests, Java integrado, self-check nativo e smoke em três processos. 199 mostrou Studio/Preview separado, DRAFT sem ativação, publish, movimento/PvE/portal, rollback e nova conexão com hash consistente em janelas/renderizadores reais. Ver inventário [de pacotes](CORUJAO-VI-PACOTES.md).

## Estados das tarefas

| TASK | Estado e evidência |
| --- | --- |
| 181 | VALIDADA COM RESSALVAS; inventário técnico, revisão humana preservada |
| 182 | VALIDADA COM RESSALVAS; baseline Java/Python, Maven/atualizador ambientais |
| 183 | VALIDADA; proxy TCP local e cenários limitados, sem Radmin físico |
| 184 | VALIDADA COM RESSALVAS documental; Radmin físico PENDENTE |
| 185 | VALIDADA; conclusão anterior preservada, reconexão novamente na regressão |
| 186 | VALIDADA APÓS CORREÇÃO; negativo e três faults repetidos na 200 |
| 187 | VALIDADA COM RESSALVAS; backpressure real e golden bytes |
| 188 | VALIDADA APÓS CORREÇÃO; admissão e partida de não participante |
| 189 | VALIDADA COM RESSALVAS; JSON limitado/JFR sanitizado |
| 190 | VALIDADA COM RESSALVAS após correção; soak 300,09s |
| 191 | VALIDADA APÓS CORREÇÃO; shutdown/restart e Studio real |
| 192 | VALIDADA COM RESSALVAS; modelo de ameaças e negativos |
| 193 | VALIDADA; auditoria/contrato e aprovação formal do ADR |
| 194 | VALIDADA COM RESSALVAS; seis específicos, 89 RPG, 138 multiplayer + um GUI abortado em headless |
| 195 | VALIDADA COM RESSALVAS; três específicos, 92 RPG, 138 multiplayer + um GUI abortado em headless |
| 196 | VALIDADA COM RESSALVAS; três específicos; 610 Java gráficos e 33 Python; Gate D |
| 197 | VALIDADA; três específicos; Studio 70 aprovados + oito GUI abortados em headless |
| 198 | VALIDADA COM RESSALVAS; dois pacotes/launchers verificados, três processos exit 0 |
| 199 | VALIDADA COM RESSALVAS; prova integrada e cinco capturas reais |
| 200 | VALIDADA COM RESSALVAS; 613 Java gráficos, 33 Python, 31.629 hashes; sem TASK-201 |

Gates A/B/C atendidos nas condições e ressalvas de seus relatórios. Gate D atendido estritamente no laboratório aprovado. Gate E consolida validação local e pacotes privados, mantendo pendências humanas/ambientais explícitas; não é autorização de release.

## Compatibilidade e integridade

| Contrato | Resultado |
| --- | --- |
| Envelope V1/opcodes 1–12, aplicações V1/V2/V3 | Preservados; golden bytes e matriz de ingresso |
| Aplicação V4/opcodes 13–15/barreiras | Preservados; confirmação corrigida, sem novo wire/schema |
| MAX_FRAME4096, CHUNK2800, descritor4MiB | Preservados |
| forest/village, rootforest, 960×640, dois jogadores/seis atores | Preservados |
| Assets preinstalados, 128 e 64MiB ativos | Preservados; zero PNG dinâmico |
| Saves pessoais V1/V2, progressão e combate | Fontes e arquivos protegidos preservados neste bloco |
| Administração por referência local Java | Preservada; convidado/jogador não publica |
| Repositório SGO1/payload existente | Formato preservado; somente laboratório sintético |

Após a correção 186, apenas seis arquivos de produção diferem da entrada da sessão 187: LocalSimulationServer, NetworkDiagnostics, RevisionCoordinator, PreviewServer, SyntheticOnlineRepository e novo SyntheticSessionLaboratory. A correção anterior acrescenta LivePublishing a esse inventário. Hash desse arquivo permanece 70215e54170740cb4b9df140f19463b1445f3561411ebacd6dc0ec7964bfea6d. Fontes de protocolo, codecs, RpgWorld e recursos não sofreram mudança por estas etapas. Mudanças antigas já presentes na árvore Git não são atribuídas a este bloco.

31.629 arquivos protegidos foram revalidados integralmente após Gate C e Gate D; conferências intermediárias usam SHA cacheado com size/mtime e rehash de qualquer alteração, declaradas em cada checkpoint. A 200 repete o rehash integral e verifica novamente os novos ZIPs/jars. Nenhuma evidência original ou pacote anterior foi sobrescrito. Branch/HEAD e status completos estão em before/after.json.

## Medições e limites

Soak190: duas épocas comparáveis de aproximadamente150s, oito transações no total e descritor4192250bytes. Tick móvel p95 2,0003→1,4594ms, paint de camadas0,5943→0,5538ms, threads p9522→22 e heap p95 aproximadamente346,22→438,50MiB. Não há alegação de ganho, vazamento ausente ou p95 global de todos os frames: janelas móveis256, GC não normalizado e render parcial documentados.

Prova199: publicação257,664ms, rollback552,432ms, paint p957,526/11,273ms e tick5,917ms. É uma execução gráfica distinta, sem comparação causal com o soak. Hash base/final/reconexão igual em servidor e clientes; candidata d86f77ff50676ac8dfc788aa68c2aacc36f67c7f7b2f0cd2adbeebe0286051a3. Métricas/CSVs/JFR permanecem nos checkpoints de origem.

## Rastreabilidade e pendências

Índice: .task-checkpoints/corujao-vi-session-20261025T221534Z-be629798/progress.jsonl; ponteiros task-N.txt identificam tentativas finais. Logs brutos, commands/cwd/durações/exit codes, manifests, diffs, fontes compiladas e hashes estão nos checkpoints. Tentativas falhas, ajuste de fixtures, abortamentos GUI e montagens recusadas pela allowlist foram preservados e explicados. Não houve teste desabilitado, nova dependência, commit, push, publicação ou alteração de rede/política.

Os diffs finais de 194–197 estão também em TASK-200/verified-diffs, comparando exatamente as duas árvores compiladas e seus manifests SHA. Os registros automáticos anteriores de source-delta usavam inventário da entrada da tarefa anterior; permanecem preservados como contexto, sem substituir essa comparação final entre snapshots compilados.

[Homologação e checklist](CORUJAO-VI-HOMOLOGACAO.md) · [Pendências](CORUJAO-VI-PENDENCIAS.md) · [Progresso por tarefa](CORUJAO-VI-PROGRESSO.md). O próximo passo humano é coletar evidência Radmin A/B e resolver o ambiente do atualizador se desejado; isso não inicia TASK-201 nem integra a persistência sintética ao jogo real.
