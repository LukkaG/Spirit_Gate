# ORG-03-R2 — Matriz de extração do estado

## Legenda e fontes

Todos os caminhos abreviados abaixo são relativos a
`server/src/main/java/com/projetok/`. A matriz cobre os componentes do record
RecoverableCharacterSnapshot e seus valores internos. Métodos citados descrevem
o checkout; APIs futuras são propostas, não alterações efetuadas.

- **C**: leitura disponível sem mudança de produção; **A**: conversão/adaptador
  necessário; **N**: depende de API interna nova; **P**: política pendente;
  **U**: informação não existe no contrato atual.
- **Owner**: ponto serializado da simulação, não reader/writer TCP nem EDT.
- Um campo C/A não torna seu bloco completo: HP é legível, mas Resources não
  pode ser construído sem a fração de regeneração. Não converter ausência em 0.

| Sigla | Arquivo e âncora de leitura |
| --- | --- |
| RW | [RpgWorld](../server/src/main/java/com/projetok/rpg/RpgWorld.java): networkState, progressView, domainProgression, inventory, equippedItems, abilityView, authoredQuestViews, save/snapshot, manualAttributes |
| RS | [RpgSessionWorld](../server/src/main/java/com/projetok/multiplayer/RpgSessionWorld.java): Session, joinCharacter, leave, attributesSnapshotFor/energySnapshotFor, contentHash |
| LS | [LocalSimulationServer](../server/src/main/java/com/projetok/multiplayer/LocalSimulationServer.java): owner coop-world, commands, departures, start |
| RE | [RpgSkillEngine](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java): Session.ready, state, remove/clear |
| AQ | [AuthoredQuestSession](../server/src/main/java/com/projetok/rpg/AuthoredQuestSession.java): progress, flags, views/copy |
| QS | [QuestService](../server/src/main/java/com/projetok/rpg/QuestService.java): state/event/complete, View, Definition |
| INV | [InventoryService](../server/src/main/java/com/projetok/rpg/InventoryService.java): slots, capacity, snapshot |
| DP | [DomainProgression](../server/src/main/java/com/projetok/rpg/DomainProgression.java): record, read/writeTo |
| SE | [SharedRpgEncounter](../server/src/main/java/com/projetok/rpg/SharedRpgEncounter.java): participants, hit, leave |

## Identidade, versão e captura

| Campo DTO | Origem/dono | Leitura disponível | Thread | Tipo atual | Tipo exigido | Conversão | Precisão | Dependências | MP/classificação | Risco de inconsistência | API futura mínima |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| characterId.value | CI em RW; RS admissão | networkState.identity.id | Owner | UUID efêmero online | CharacterId(UUID estável) | Não converter efêmero em permanente | Bits exatos; sem continuidade | posse/admissão | P/U; ID sintético só no lab | Mesma pessoa reconecta com outro UUID | Política/identidade estável separada |
| schemaVersion.value | Contrato R1 | Constante da versão do formato | Adaptador no owner | Ausente no gameplay | int conhecido | Atribuir versão do contrato, não wire | Sem perda | versão R1 | A | Confundir schema com aplicação6/7 | Nenhuma para versão do DTO |
| stateRevision.value | Autoridade de personagem | Nenhuma revisão durável por personagem | Owner futuro | Ausente | long monotônico | Não usar tick/sequence | Não aplicável | CAS/geração | U/N | Snapshot antigo sobrescrever novo | Revisão explícita do estado |
| contentHash.value | RS.content | contentHash() | Owner | String hash | ContentHash | Wrapping | Exata | revisão ativa | C/A em RS; N em RW isolado | Ler hash novo e personagem antigo | Agregado coordenado com revisão |
| rulesVersion.value | Curvas/catálogos separados | Não há versão consolidada | Owner | Ausente | RulesVersion(String) | Não inferir do hash de mapa | Sem política | regras e catálogos | U/P | Mudar curva/itens sem registrar versão | Versão de regras aprovada |
| capture.worldEpoch.value | RS.instance/instância de mundo | instance é UUID privado | Owner | UUID, não long durável | WorldEpoch(long) | UUID não cabe por truncamento | Conversão seria indevida | época/autoridade | U/P | Reutilizar prazo/ID após restart | Época explicitamente definida |
| capture.sourceTick.value | RW.ticks | networkState.ticks | Owner | long | SourceTick(long) | Wrapping | Exata | mesmo mundo/instante | C/A, testado | Misturar tick RW, RS e clock RE | Captura agregada com base temporal |
| capture.capturedAt | Amostrador confiável futuro | Não há sampler de captura no runtime | Owner | Não disponível como dado RW | Instant | Timestamp explícito, não hora do cliente | Política pendente | relógio/época | P; horário sintético no lab | Instante não vinculado ao tick | Provedor de tempo aprovado |
| capture.clockOrigin/clockVersion | Contrato temporal | Tick local conhecido, continuidade não | Owner | Clock local de processo | enum/int | Declarar unidade/origem sem avançar | Não prova prazo durável | relógio/R1 | P | Interpretar tick antigo no processo novo | Política de continuidade/restart |
| identity.name | CI | networkState.identity.name | Owner | String normalizada | String | Cópia | Exata | identidade do mesmo RW | C/A observado | Misturar nome de outra admissão | Agregado ligado à participação |
| identity.race | CI.archetype/race | networkState.identity.race | Owner | String canônica | String | Cópia canônica | Exata | CharacterRaces | C/A observado | Domínio de outra raça | Nenhuma leitura nova |
| identity.appearance | CI | networkState.identity.appearance | Owner | azure/amber | String | Cópia | Exata | identidade local | C/A observado | Confundir com roupa/cabelo | Nenhuma leitura nova |
| identity.hair/clothing | RS.Session.appearance | appearances() parcial por ator; Session privada | Owner | CosmeticCatalog.Selection | Strings de catálogo | Relacionar ao mesmo id/contexto | Exata se agregado | raça/catálogo | N/A; não há fonte em RW | Inventar none ou usar outro ator | Agregado com cosméticos da participação |

No protótipo, o bloco Identity fica nulo: o nome/raça/aparência legíveis aparecem
em IdentityObservation, sem fabricar cabelo/roupa. CharacterId, época, hash,
revisão, regras e horário são **metadados declaradamente sintéticos** do controlador.
Não constituem prova de identidade permanente ou contexto autenticado.

## Progressão, atributos e economia

| Campo DTO | Origem/dono | Leitura disponível | Thread | Tipo atual | Tipo exigido | Conversão | Precisão | Dependências | MP/classificação | Risco de inconsistência | API futura mínima |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| progression.xp | RW.xp | progressView.xp | Owner | long | long | Cópia | Exata | curva/domain no mesmo tick | C/A testado | Prêmio entre leituras | Leitura no owner agregado |
| progression.domain | RW.domain | domainProgression.domain | Owner | String | String | Cópia | Exata | raça/nível | C/A testado | Troca/reset entre cópias | Agregado |
| progression.domainXpHalfUnits | DP | domainProgression.xpHalfUnits | Owner | long | long | Sem double | Exata | XP de domínio | C/A testado com morte real | Reexecutar award para reconstruir | Nenhuma operação econômica |
| progression.lastReset | DP | domainProgression.lastReset | Owner | LocalDate ou null | LocalDate opcional | Cópia | Exata | política de reset | C/A leitura; reset ativo não testado | Inventar data/saldo | Política para recuperar reset |
| progression.resetQuestComplete | DP | domainProgression.resetQuestComplete | Owner | boolean | boolean | Cópia | Exata | contrato de missão/reset | C/A leitura | Confundir flag com pagamento | Recibos futuros, não novo getter |
| attributes.mode | RW.manualStatsEnabled | Campo privado | Owner | boolean | AttributeMode | Não inferir de bônus zero | Ambígua pelos getters efetivos | regras de atributo | N/P | Captura habilitar o modo | Read-only do modo bruto |
| attributes.strength/intelligence | RW.manualStats | manualAttributes altera/habilita | Owner | ints em StatsComponent | ints | Não inverter bônus de dano | Bônus perde informação | orçamento/nível | N | Concessão lazy durante export | Cópia do record bruto sem materialização |
| attributes.agility/energy | RW.manualStats | skillAgility()/trainedEnergy() efetivos | Owner | int efetivo/treinado | investimentos int | Só observação parcial | Zero pode significar modo desabilitado | manualStatsEnabled | C observado; N para bloco | Confundir efetivo com bruto | RawStats com modo |
| attributes.unspentPoints | RW.manualStats | Apenas consulta lazy mutável | Owner | int | int | Não recalcular concessões | Exata só pelo campo bruto | LevelingRules | N | Reatribuir pontos | Export read-only do saldo |
| attributes.grantedThroughLevel | RW.manualStats | Campo do record via getter lazy | Owner | int | int checkpoint | Não derivar do nível atual | Checkpoint real pode divergir | XP e concessão | N | Criar concessão nova | Export read-only do checkpoint |
| inventory.capacity | INV | capacity() | Owner | int, 20 em RW | int | Cópia | Exata | mesma bolsa | C/A testado | Mudar capacidade para fixture | Nenhuma |
| inventory.slots.index/itemId/quantity | INV.Stack[] | slots() copia array | Owner | posições com Stack/null | List<Slot> | Omitir vazios mantendo índice/capacidade | Exata | equipamento/loot | C/A testado, inclusive índice19 | Dupla contagem se equip ocorrer no meio | Captura agregada no owner |
| equipment.slot/itemId | RW.equipment | equippedItems() | Owner | Map<String,String> | List<Equipment> | Ordenar/copy, sem equip() | Exata | bolsa e nível | C/A testado | Somar item equipado à bolsa | Agregado, não operação de instalação |
| wallet.coins | RW.coins | networkState.coins | Owner | int | long | Alargamento exato | Exata | prêmio/transação | C/A testado com 5 moedas reais | Premiar novamente na exportação | Nenhuma concessão |

O bloco Attributes permanece nulo. Observar AGI e Energy não exporta STR, INT,
pontos ou checkpoint. O teste consulta manualAttributes somente no preparo e
na verificação de uma fixture já inicializada; o capturador não chama esse método.

## Missões, recibos, watermarks e direitos pendentes

| Campo DTO | Origem/dono | Leitura disponível | Thread | Tipo atual | Tipo exigido | Conversão | Precisão | Dependências | MP/classificação | Risco de inconsistência | API futura mínima |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| quests.key.namespace/id/version | RW builtin; AQ autorado; QuestBook | authoredQuestViews só expõe ID | Owner | ID em View; versões de conteúdo separadas | QuestKey | Não inventar versão/namespace por texto | Incompleta | definição da missão | N/P | Misturar duas definições do mesmo ID | Export versionado com origem |
| quests.status | QS.state | authoredQuestViews.state | Owner | enum/flags agregadas | QuestStatus | AVAILABLE não é missão aceita; COMPLETED não é recibo | Parcial | progress/flags | C observação; N para contrato completo | Declarar entrega paga por estado visual | Ledger tipado com entrega/recibo |
| quests.objectives.index/type/target/count | RW.flags qo; AQ.flags/progress | Sem contador individual exportável | Owner | flags + mapas privados | ObjectiveProgress | Não analisar descrição de UI como contador | Total não é vetor de objetivos | QuestDefinition/inventário | N | Total igual com objetivos diferentes | Cópia de contadores/definição |
| quests.deliveryReceiptId | Flag quest-done e histórico ausente | Não existe referência durável | Owner | flag em memória | String de recibo | Não fabricar a partir de flag | Sem garantia econômica | prêmio/commit | U | Duplicação de recompensa | Ledger/recibo novo, não getter suficiente |
| receipts.id/kind/source/beneficiary | Apenas deduplicação local/gerações | Nenhum recibo completo no runtime | Owner | flags/SE.participants | RewardReceipt | Não reinterpretar flag como pagamento | Indisponível | origem econômica/CharacterId | U | Pagar duas vezes ou mudar beneficiário | Projeto de registro econômico |
| receipts.worldEpoch/confirmedRevision/resultHash | Sem commit durável atual | Nenhum | Owner | Ausente | época/revisão/hash | Não derivar de tick/UUID/Map.hashCode | Indisponível | confirmação transacional | U/P | Falso recibo durável | Persistência/recibo em etapa própria |
| watermarks.type/source/epoch/sequence | RW.flags qe; AQ.flags | Campos privados; views não incluem ledger | Owner | String/tipo/fonte/seq | EventWatermark | Decoder tipado fiel, sem replay | Época do evento ainda não definida | contexto da fonte | N/P | Contar kill novamente após restart | Export de ledger e namespace temporal |
| pendingLoot.id/receiptId | Direitos em flags pending sem recibo próprio | Nenhuma identidade de concessão completa | Owner | Sem ID de direito/recibo | PendingEntitlement | Não emitir novos IDs durante captura | Indisponível | recibo econômico | U | Duplicar o mesmo direito | Contrato de concessão/direito |
| pendingLoot.itemId/quantity | RW.flags pending | Sem getter puro do mundo | Owner | flags, até 9999/item | String/int | Leitura tipada sem collectLoot() | Quantidade é exata, origem incompleta | bolsa/flags | N | Coletar/consumir para inspecionar | Export read-only de pendências |

As quatro coleções ficam nulas no candidato parcial, não listas vazias. O
protótipo preserva apenas resumos autorados em Observations, explicitamente
separados de QuestProgress. Builtin não tem nem essa leitura segura: snapshot()
de RW chama save(), que escreve flags. Não se reexecuta missão nem loot para
reconstruir histórico. O laboratório sintético/R3 não é fonte de recibos deste RW.

## Recursos, cooldowns, localização e restrições

| Campo DTO | Origem/dono | Leitura disponível | Thread | Tipo atual | Tipo exigido | Conversão | Precisão | Dependências | MP/classificação | Risco de inconsistência | API futura mínima |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| resources.hp/lifeState | RW.hp | networkState.hp | Owner | int; morte hp0 | int/enum | hp0=DEAD, sem respawn | Exata | mesmo RW/tick | C/A observado, inclusive morto | Cura entre leituras | Agregado |
| resources.energyAbsolute | RW.stamina | networkState.stamina | Owner | int absoluto | int | Não usar normalizedEnergy | Exata | atributos/máximo | C/A observado | Normalizar/arredondar ou refill | Agregado |
| resources.regenerationRemainder | RW.energyRegenerationRemainder | Nenhum getter | Owner | double | double | Não assumir zero | Save perde a fração | energia/máximo | N | Mudança da regeneração após recuperar | Read-only do acumulador |
| cooldowns.policyVersion | Política futura | Não homologada como recuperação | Owner | Sem regra de continuidade | int | Versão explícita, não default de gameplay | Semântica pendente | clock/época | P | Reiniciar ou liberar recarga | Aprovação da política |
| deadlines.channel | RW + RE | Canais conhecidos no código | Owner | Quatro básicos +cinco por domínio | enum | Mapear ações, não só slot | Identidade exata | domínio/catálogo | A/P | Slot de outra skill | Envelope de prazo/contexto |
| deadlines.skillId | RpgSkillCatalog/RE.Session.domain | Catálogo puro, sem prova da sessão atual | Owner | ID/slot/domínio | String de ID | Usar ID real associado ao owner | Identidade exata; vínculo ausente | presença RE | N/A | Vincular ready[] a outro domínio | Leitura atômica do vínculo |
| deadline básico | RW.preciseAttackReady/attackReadyTick | abilityView.attackTicks só inteiro | Owner | double e long privados | BigDecimal | Não reconstruir a fração do restante inteiro | Perda se reconstruído | AGI/clock local | N; view parcial testada | Liberação antecipada | Read-only da cadência completa |
| deadline Shunpo/poção | RW.abilityReadyTick/potionReadyTick | abilityView retorna restante clampado | Owner | long privados | BigDecimal | Não inferir deadline expirado como now | Restante perde origem exata | tick de captura | N; view observada | Reset ou mistura de ticks | Prazo absoluto +relógio |
| cinco deadlines de domínio | RE.Session.ready[5] | state(id) retorna inteiros/zeros se ausente | Owner | long[5] privado | cinco deadlines com IDs | Sem pós-remove; não usar zeros como presença | Restante perde prazo histórico | mundo/domínio/região | N | Sessão inexistente parecer pronta | Export presence+binding+ready copiado |
| deadline viagem | RW.portalReadyTick | Sem getter puro específico | Owner | long | BigDecimal | Não usar save() para ler flag | Exata só com accessor | região/portal/tick | N | Viagem liberada por ausência | Leitura do prazo |
| location.regionId/x/y | RW.region/body | networkState | Owner | String,float,float | mesmos tipos | Cópia | Exata nos floats existentes | geometria/revisão | C/A testado | Região nova com posição antiga | Agregado e validação física futura |
| location.spawnRef | Conteúdo/home em RS | home é privado, sem referência persistente | Owner | Position home/map.spawn | String opcional | Não inventar ID de spawn | Não aplicável | conteúdo/política | P/N | Coordenada não equivale a spawnID | Referência de spawn aprovada |
| restrictions.assessment | RW/RE controles e DoTs | Sem inventário completo de restrições | Owner | flags/campos/execução | enum | Ausente não vira ASSESSED vazio | Evidência incompleta | política/status/clock | N/P | Logout limpar penalidade | Export tipado com evidência |
| restrictions.entries.kind/source/until/policy | RW timers; RE dots/controles | skillMovementBlocked é bool misto; prazos privados | Owner | long/float/bool/pending | Restriction | Não copiar execução/DoT, não inventar origem | Bool não preserva tipo/deadline | regra por efeito | N/P | Remover restrição ou reexecutar dano | Descritores somente leitura +política |

Resources, Cooldowns e Restrictions ficam nulos no candidato. Os contadores
inteiros básicos aparecem somente em RemainingView; não são convertidos em
deadline. O engine nem é consultado pelo capturador: não há prova de presença
por state(id), que retorna zeros após remoção. Nenhum prazo é avançado.

## Consistência e limite real do protótipo

Uma chamada captura todos os valores conhecidos no executor único da fixture,
sem tick, I/O ou callback de gameplay no meio. Thread errada/participação local
retirada são recusadas antes de ler o mundo. Identidade efêmera/região/contexto
esperados e NetworkState antes/depois são conferidos. Esses stamps não detectam
sozinhos toda escrita concorrente de XP/inventário no mesmo tick: a prova do
laboratório depende de **confinar todos os escritores ao mesmo owner**.

O resultado não é capturado do LocalClient. Não existe comando wire nem entrada
no servidor vivo. O flag present de Source é ciclo de vida **da fixture**, não
prova de sessão TCP ativa. Corrida real de leave/revisão e captura dentro de
coop-world continua pendente da API coordenada proposta no documento de lacunas.
