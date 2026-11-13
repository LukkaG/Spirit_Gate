# ORG-03-R5 — Matriz de recibos e transações

## Tipos implementados somente em src/test

Todos abaixo são membros de EconomicLaboratory no pacote `com.projetok.rpg.economy`.
Não existem importações desses tipos em src/main.

| Tipo | Responsabilidade | Não representa |
| --- | --- | --- |
| BeneficiaryId | Identidade sintética fornecida pela fixture | Posse, conta, CharacterId de produção |
| EconomicOperationId | ID global de intenção nesta instância | Geração, revisão ou concessão |
| GrantId | Concessão autorizada e identidade de direito | Tentativa de comando |
| EconomicRevision | CAS do estado econômico | Tick, conteúdo ou Peer.id |
| AuthorityGeneration | Contexto de admissão do controlador local | Fence durável entre processos |
| WorldEpoch | Época sintética explícita de origem | Clock de personagem ou UUID truncado |
| EconomicSource | Namespace, missão/spawn, definição/versão, geração de mob e concessão | Prova de morte/entrega real por si só |
| RewardTerms / AuthorizedGrant | Resultado pré-resolvido e oferta confiável, ligada ao beneficiário/época | Valor aceito porque foi declarado pelo solicitante |
| EconomicOperation / EconomicAction | Intenção imutável de grant, claim, consumo ou movimento | Snapshot livre para sobrescrever estado |
| ValidatedOperation / Preparation | Plano opaco e cópias aprovadas sobre uma base | Confirmação |
| EconomicState | XP, domínio, moedas, slots, direitos, missões e revisão | Personagem recuperável inteiro |
| EconomicReceipt | Intenção, base, resultado e revisões confirmadas na imagem | Durabilidade ou autenticação por hash |
| EconomicOperationResult | Confirmação, replay, recusa, ausência ou incerteza | Garantia de entrega da resposta |
| LedgerImage | Estados + recibos + índices + watermarks em uma única referência | Banco ou arquivo persistente |
| Coordinator / MemoryLedger | Autoridade do controlador sintético e CAS em memória | Runtime multiplayer ou serviço online |

## Componentes que entram juntos no CAS

| Componente | Proposta validada | Confirmação experimental |
| --- | --- | --- |
| XP | Soma exata e limites antes de preparar recompensa | Resultado absoluto no estado e recibo |
| Domínio | DomainProgression.award, somente quando já escolhido | Meias unidades inteiras, sem escolher domínio novo |
| Moedas | addExact e teto técnico | Sem crédito independente do recibo |
| Inventário | InventoryService.restore/cópia, índices preservados | Mapa imutável, sem referência ao inventário anterior |
| Direito pendente | Origem, GrantId, recibo original e pacote resolvido | Junto da concessão, sem RNG posterior |
| Claim | Confere direito/recibo e espaço para pacote inteiro | Itens inseridos e direito removido juntos; marca de coleta ligada ao novo recibo |
| Missão | READY é pré-requisito, itens de coleta revalidados | COMPLETED ligado ao recibo de entrega correto |
| Recibo | Intenção completa e resultado esperado | Mesmo CAS dos efeitos |
| Revisão | Esperada igual à atual e incremento sem overflow | before→after; nunca last-write-wins |
| Deduplicação | ID inteiro + chave de concessão + origem/contexto | Índices publicados com estado/recibo |
| Watermark | Beneficiário, época, namespace/spawn, maior geração | Não admite geração antiga que ainda não tinha recibo |

Equipamento, HP, energia, cooldowns, posição e execuções de skills não foram
adicionados ao contrato econômico. Consumo/movimento da R5 são ações sobre
inventário de teste; não implementam uso de poção, combate ou equipamento.

## Matriz de origem e duplicação

| Origem/ação | Identidade econômica | Comportamento |
| --- | --- | --- |
| Quest builtin | Beneficiário + namespace builtin + questId | Entrega única, com definição/versão registradas no recibo |
| Quest autorada | Beneficiário + namespace authored + questId | Separada de builtin, mesma fronteira de confirmação |
| Morte de mob | Beneficiário + WorldEpoch + namespace/mapa + spawn + geração | Mesma geração não paga outra vez; geração posterior precisa de oferta autorizada |
| GrantId | Beneficiário + identidade da concessão | Não reaparece sob outro ID de operação |
| Loot direto | Pacote já resolvido no grant de morte | Guardado no resultado; retry não rola tabela |
| Loot pendente | GrantId + recibo do grant + origem completa | Não se reduz a pending:item:quantity |
| Transferência pendente | Beneficiário + GrantId coletado | Um claim confirmado; duplicata não recoloca itens |
| Consumo/movimento | ID da intenção e revisão esperada | Novo ID pode ser nova ação legítima; retry preserva a intenção original |

## Casos de idempotência pedidos

| Nº | Caso | Teste de EconomicLaboratoryTest |
| --- | --- | --- |
| 1 | Mesmo ID/conteúdo | sameIdSameContentReturnsOriginalWithoutRewardOrRngAgain |
| 2 | Mesmo ID divergente | sameIdDifferentOriginIsConflict; sameIdDifferentAmountIsConflict |
| 3 | ID novo, concessão igual | newOperationIdCannotClaimSameConcession |
| 4 | Mesmo mob/geração | changingGrantIdDoesNotRepeatSameSpawnGeneration |
| 5 | Respawn posterior | respawnInLaterGenerationIsAnotherAuthorizedEvent |
| 6 | Missão entregue | questCannotBeDeliveredAgainByAliasGrantOrDefinitionVersion |
| 7 | Prêmio não confirmado | questReadyWithoutConfirmationStillHasNoReceipt |
| 8 | Direito já concedido | pendingRightCannotBeGrantedAgain |
| 9 | Transferência à bolsa | pendingTransferConsumeAndRetryDoNotDuplicateLootOrXp |
| 10 | Resposta perdida | lostResponseRequiresLookupAndDoesNotMakeCommitDisappear |

## Limites da prova

A imagem é confirmada em um processo. O hash é determinístico, não assinatura.
`historyValid` recusa cadeia/indexação/referência incompletas e candidatos com
recibo corrompido, mas não autentica um histórico inteiro reescrito por um
administrador malicioso que controla estado e fingerprints. Não existe API
para instalar essas imagens no runtime ou restaurar um ledger a partir de save.
