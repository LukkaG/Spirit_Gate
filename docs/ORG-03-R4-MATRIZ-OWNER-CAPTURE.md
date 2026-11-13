# ORG-03-R4 — Matriz de owner e captura

## Fronteiras e assinaturas

| Entrada/tipo | Visibilidade | Papel e autoridade |
| --- | --- | --- |
| RpgSessionWorld.bindCaptureOwner(Thread, IntPredicate) | pacote multiplayer | Registro único pelo LocalSimulationServer antes do primeiro tick; não é autenticação |
| RpgSessionWorld.characterCaptureRequest(int) | pacote multiplayer | Resolve Session real no owner e guarda expectativa opaca |
| RpgSessionWorld.captureCharacter(CharacterCaptureRequest) | pacote multiplayer | Valida expectativa/presença/contexto e devolve dados imutáveis ou recusa |
| CharacterCaptureRequest | pacote; campos privados finais | Retém fonte/Session/conteúdo/região/domínio; sem getter de mundo ou sessão |
| CharacterCaptureResult/Metadata/Status | pacote multiplayer | Somente valores e diagnóstico; nenhum objeto interno mutável |
| RpgCharacterObservation.read(Thread,int,RpgWorld,RpgSkillEngine,Map) | pública Java, entre pacotes internos | Ponte de leitura R3; recebe referências do coordenador confiável, não autentica por si só |
| CharacterReadState e Restrictions | tipos públicos imutáveis | Mesmos dados R3, agora legíveis pelo coordenador multiplayer |
| QuestService.Ledger/ObjectiveRead | tipos públicos imutáveis | Ledger R3, sem mudança de contagem/concessão |
| RpgSkillEngine.TimingStatus/SkillDeadline/TimingRead | tipos públicos imutáveis | Timing R3 com presença real e IDs de catálogo |
| Métodos de leitura R3 e CharacterReadContext | continuam de pacote RPG | Sem getters públicos adicionais em RpgWorld/engine |
| SessionSimulation | inalterada | Não recebe comando, callback ou snapshot novo |

O fato de tipos serem públicos em Java não cria mensagem de protocolo. Nenhum
resultado de captura é passado a Protocol.write, controles de peer, snapshots
TCP, renderer ou cliente. A ponte não concede posse por receber um Thread:
é o coordenador que valida a admissão real e fornece as referências privadas.

## Ponto serializado e ciclo de vida

| Situação | Verificação/resultado | Evidência |
| --- | --- | --- |
| Owner real | Registro no executor antes do primeiro tick e comparação `Thread == captureOwner` | Ensaios TCP e wrongThreadMissingSourceAndInvalidRequestReturnOnlyDiagnostics |
| Sem owner registrado | WRONG_OWNER, sem ler mundo | Mesmo teste |
| Participação ativa | Session atual + peer/connections/closed/departures + revisão sem kick | realOwnerCopiesBothParticipantsWithoutChangingPrivateOrObserverState, apps6/7 |
| Remoção já drenada | Session ausente e pedido antigo: REMOVED | disconnectBetweenDepartureDrainAndCaptureIsRejectedThenRemoved |
| Peer fechado após departures | DISCONNECTING antes da cópia | Mesmo teste, socket real fechado enquanto fixture controla a ordem |
| Presença perdida na guarda final | Descarta toda observação: DISCONNECTING | disappearanceAtFinalPresenceCheckDiscardsWholeObservationInLaboratory; guarda controlada, não corrida TCP alegada |
| Pedido ausente/inválido | INVALID_REQUEST/ABSENT sem dados | Teste de thread/pedido inválido |
| Outro Session no mesmo ID | REPLACED, mesmo se UUID legado coincidir | sameNumericIdWithAnotherSessionCannotReuseOldRequestInLaboratory |
| Mundo substituído pela revisão | Pedido com Session anterior recusado | absentReadyActiveAndRemovedSkillSessionsRemainDistinct |
| Conteúdo diferente | Identidade do LiveRevision atual difere: CONTENT_CHANGED | domainRegionFreezeAndContentRevisionRejectStaleRequests |
| Região/domínio alterados | REGION_CHANGED/DOMAIN_CHANGED | Mesmo teste |
| Coordenador congelado | TRANSITION | Mesmo teste |
| Binding real de skill divergente | SKILL_BINDING_MISMATCH sem limpar Session | staleSkillRegionIsRefusedWithoutCleanupUntilNormalStepRemovesIt |
| Skill ausente | Observação capturada com timing ABSENT e deadlines null | Teste de sessão ausente/pronta/ativa/removida |
| Skills prontas | Timing PRESENT com cinco prazos reais zero | Mesmo teste; cast recusado por falta de alvo cria Session pelo caminho real |
| Skills removidas pelo fluxo normal | Nova observação ABSENT, sem cinco zeros inventados | Mesmo teste e teste de região/step |
| Repetição | Mesmos valores e snapshots antes/depois, tick estável | Testes TCP de pureza e medição |

Um pedido não serve como prova isolada de presença: cada execução volta a
consultar a Session atual e a condição do peer. Não há cache de resultados,
fila de leituras, pendência registrada no personagem ou identidade persistente.
Pedidos retidos pelo chamador são referências locais descartáveis; depois de
remoção eles apenas permitem obter diagnóstico. Não autorizam restauração.

## Origens temporais

| Valor observado | Fonte | Significado e limite |
| --- | --- | --- |
| participant | ID do coordenador | Conexão/participação temporária, não CharacterId persistente |
| sessionWorldInstance | RpgSessionWorld.instance | UUID real da instância em processo; não WorldEpoch durável |
| sessionTick | RpgSessionWorld.tick | Avanços da sessão, pausados pelo frozen |
| worldTick | RpgWorld.ticks | Avanços daquele personagem; entrada tardia pode divergir da sessão |
| skillEngineClock | RpgSkillEngine.clock | Clock global recebido do coordenador, distinto do relógio do personagem |
| Cinco readyAtTick | Session.ready | Prazos absolutos calculados sobre world.skillTick(), sem expiração na leitura |
| Ataque/Shunpo/poção/viagem | Campos RpgWorld da R3 | Precisão nativa, NaN inicial explícito, sem conversão durável |
| contentRevision/contentHash | LiveRevision.number/hash instalados | Revisão do conteúdo; não StateRevision do personagem |
| durableWorldEpoch | Indisponível | null, sem truncar/reinterpretar UUID |

`lateParticipantKeepsSeparateWorldSessionAndSkillClocks` entra depois de outro
participante, lança uma skill real e compara seu deadline ao tick do personagem;
usar o tick de sessão produziria outro valor. Não se infere equivalência dos
relógios porque valores coincidiram em um ensaio inicial.

## Ordenação, concorrência e limites

Os testes inserem tarefas na fila commands existente. Assim a captura ocorre
na fase de comandos: depois das retiradas já drenadas e antes do step/snapshot
daquele tick. Dois comandos consecutivos observam o mesmo ponto de movimento;
o movimento só aparece após o step e no snapshot seguinte. Um pedido enfileirado
não tem prioridade sobre alterações de conteúdo: sempre revalida a expectativa
quando executado. Revisões processadas em step afetam comandos da próxima rodada.

Trocas de conteúdo nos testes usam prepareRevision/installRevision existentes,
somente em memória, sem publicar mapas ou acionar distribuição. A revisão nova
mantém bundle/hash e incrementa o número, demonstrando que número de conteúdo é
observado sem fabricar revisão de personagem. Não se afirma validação de uma
publicação distribuída completa, rollback administrativo ou comportamento ABA
de reinstalações arbitrárias de objetos privados por código externo.

A fila conserva capacidade 64 e drenagem máxima64; nenhuma fila ou futuro novo
de produção foi criado. O chamador interno deve agendar conforme sua autorização
e orçamento. Não há serviço de polling, rate limit ou SLA de captura implementado.
As medições de testes não são garantia de carga em produção.
