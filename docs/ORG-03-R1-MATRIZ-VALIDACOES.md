# ORG-03-R1 — Matriz de validações

## Modelo implementado

`RecoverableCharacterSnapshot` contém blocos obrigatórios de identidade/revisões,
captura, progressão/reset, atributos, bolsa, equipamentos, moedas, missões,
recibos, watermarks, direitos pendentes, recursos, cooldowns, localização e
restrições. A lista de parâmetros do record é o schema explícito; não há Map
de flags genéricas, Object, callback, socket ou campo de execução.

Records distinguem `CharacterId(UUID)`, `SchemaVersion`, `StateRevision`,
`ContentHash`, `RulesVersion`, `WorldEpoch` e `SourceTick`. `CharacterId.parse`
exige UUID canônico; um UUID não prova posse. Timestamp é Instant fornecido,
não `Instant.now()`. `lastReset` e `spawnRef` são opcionais; blocos e coleções
obrigatórios não são preenchidos com defaults. Coleção presente é cópia imutável;
elemento nulo, excesso de tamanho ou precisão causam rejeição no construtor.
Blocos nulos podem ser construídos para o validador retornar diagnóstico estruturado.

`CharacterSnapshotValidator.Result` carrega `List<Violation(code,path)>` e
`Set<PendingCheck>`. Nenhuma consulta retorna inventário candidato instalável.
A chamada é determinística, com acumulador local. Resultado pode ter violações
e pendências ao mesmo tempo. Sem violações significa somente estrutura aceita
pelas verificações disponíveis, **nunca ativação autorizada**.

## Cobertura dos 30 requisitos

Métodos abaixo pertencem a `CharacterSnapshotValidatorTest`; parametrizações
ampliam o total para 47 casos, sem somar os casos como requisitos novos.

| Nº | Caso exigido | Regra/evidência principal |
| --- | --- | --- |
| 1 | Completo válido | `completeSnapshotIsStructurallyValidButNeverAuthorizesActivation`: sem violações, quatro pendências obrigatórias |
| 2 | Coleções imutáveis | `collectionsAndResultsAreImmutable`: bolsa, objetivos, recibos, deadlines e resultado |
| 3 | Alteração externa | `externalListsAndContextMapsCannotChangeConstructedData`: listas originais e índice do contexto copiados |
| 4 | Bloco ausente | `everyRequiredTopLevelBlockIsRejectedWhenAbsent`: cada componente obrigatório; também ausência de coleção interna |
| 5 | Versão incompatível | `incompatibleSchemaIsRejected`: schema diferente de1 |
| 6 | UUID inválido/ausente | `malformedOrMissingUuidIsRejected`: null, texto inválido e forma abreviada |
| 7 | Revisão inválida | `negativeStaleAndOverflowingRevisionsAreRejected`: -1, revisão não superior e MAX_VALUE |
| 8 | XP inválida | `xpOutsideCurrentTechnicalBoundsIsRejected`: negativo, acima de1000000 e MAX_VALUE |
| 9 | Domínio incompatível | `incompatibleDomainOrHalfUnitsAreRejectedWithoutChoosingOrAwarding`: raça, none com XP e XP negativa |
| 10 | Atributos negativos | `negativeAttributesAreRejected` |
| 11 | Orçamento/checkpoint | `budgetAndGrantCheckpointCannotConcedeExtraPoints`: soma longa, nível derivado e gasto/concessão coerentes |
| 12 | Slot duplicado | `duplicateSlotIndicesAreRejectedEvenWithDifferentItems`: verifica índices antes de converter em Map |
| 13 | Quantidade inválida | `invalidStackQuantitiesAreRejected`: 0, -1,100,MAX_VALUE; catálogo complementa limite |
| 14 | Item igual legítimo | `sameItemInTwoDifferentSlotsIsLegitimate` |
| 15 | Equipamento incompatível | `incompatibleEquipmentAndUnknownItemsAreRejected`; slot repetido em teste adicional |
| 16 | Moedas/overflow | `negativeOrOverflowingCoinsAreRejected`: long candidato, faixa técnica atual |
| 17 | Missão duplicada | `duplicateQuestsCannotHideBehindDifferentVersions`: mesmo namespace/ID com versão diferente também recusa |
| 18 | Objetivo inválido | `invalidDuplicateAndExcessiveObjectivesAreRejected`: índice, alvo/tipo, quantidade e definição |
| 19 | Recibo conflitante | `duplicateOrConflictingReceiptsAreRejected`: ID duplicado e mesma concessão com ID novo |
| 20 | Loot pendente duplicado | `duplicateLootAndDanglingEconomicReferencesAreRejected`: ID, concessão/item e referência |
| 21 | Cooldown ausente | `missingCooldownCannotBecomeZero`: canal removido ou lista ausente |
| 22 | Deadline inválido | `invalidDeadlinesAreRejectedAndDecimalPrecisionIsPreserved` e teste de MAX_VALUE |
| 23 | HP/energia | `resourcesDoNotPermitDeadWithHpOrExcessEnergy`: vida, máximo, NaN e energia absoluta |
| 24 | Localização | `invalidLocationsAreRejectedWithoutMovingAnyBody`: região, finitude e limites injetados |
| 25 | Restrição obrigatória ausente | `absentPenaltyEvidenceNeverMeansNoPenalty`: evidência externa exige ROOT; UNKNOWN não vira limpo |
| 26 | Intangibilidade | `intangibilityAndExecutableStateCannotBeRepresented`: enum recusa INTANGIBLE e schema não tem execução |
| 27 | Outro contexto | `snapshotFromOtherContentRulesOrEpochIsRejected` |
| 28 | Repetição determinística | `repeatedValidationIsDeterministicAndDoesNotAwardOrAlterAnything`: 30 consultas iguais |
| 29 | Sem efeitos externos | Mesmo teste conserva saldos/objetivos/recursos; `immutableContractClassesHaveNoLinksToRuntimeOrExternalIo` verifica bytecode dos tipos novos |
| 30 | Sem instalação parcial | `rejectionDoesNotPartiallyInstallOrMutateAnExistingInventory`: inventário externo preservado e candidato não alterado |

Testes extras cobrem budgets técnicos sem truncamento, mortes preservadas,
recibo de outro personagem, watermarks repetidos, overflow de data de reset,
captura futura/negativa, políticas desconhecidas, descritores adversos sem dano
e identificadores internos nulos recusados com diagnóstico, sem exceção de consulta.

## Reutilização e limites da pureza

| Regra | Reuso real | Fronteira |
| --- | --- | --- |
| Identidade/raça | CharacterIdentity e CharacterRaces | Objeto temporário apenas valida; nome não é normalizado silenciosamente |
| XP/nível | `RpgProgression.level` da instância injetada | Não chama builtin/award; não executa level-up |
| Domínio | Construtor DomainProgression, Domains e INITIATION_LEVEL | validateFor carrega builtin e não é usado; relação raça/nível é conferida explicitamente |
| Atributos | StatsComponent e LevelingRules.pointsForLevel | Acrescenta não negativos de STR/INT/AGI; soma do orçamento, sem applyLevelUp |
| Energia | EnergyRules.maximum | Não regenera, não converte fração em refill |
| Bolsa | ItemCatalog pré-carregado e InventoryService.restore puro | Índices verificados antes da conversão; restore cria só cópia local, sem inventário vivo |
| Equipamento | ItemCatalog.category/slot/minimumLevel | Não chama equip/unequip; faz conferência dos metadados necessários |
| Missões | QuestService.Definition/Objective imutáveis no contexto | Não cria QuestService nem chama complete/refresh/reward na validação |
| Skills | RpgSkillCatalog.forDomain | Consulta IDs reais; sem skill executor e sem concessão |

Catálogos de raça/domínio/skill consultados são declarativos em memória.
ItemCatalog/curva/definições de missão devem ser carregados fora da chamada.
Não há relógio global, RNG, gravação, conexão ou leitura de arquivo em validate.
O teste de bytecode cobre dependências diretas novas; revisão dos métodos usados
complementa esse teste, que não é prova universal sobre qualquer alteração futura.

## Regras de referência e precisão

Missão versionada: namespace builtin/autorado + ID; versão divergente não permite
duas entradas da mesma missão. Objetivos têm índice único, tipo/alvo iguais à
definição e contagem dentro do requisito. READY/DELIVERED exigem objetivos completos;
DELIVERED exige recibo QUEST da origem `namespace:id:version`. ACTIVE não pode
contradizer todos os objetivos completos no catálogo fornecido.

Recibo liga beneficiário ao CharacterId, revisão confirmada ao teto do snapshot,
tipo/origem/época e hash de resultado. IDs e concessões repetidas são recusados,
sem merge. Watermark liga tipo/origem/época e sequência positiva. Não é pagamento.
PendingEntitlement exige recibo LOOT, item conhecido e chave de direito única;
não soma itens, e totais pendentes por item respeitam9999. Essa conferência não
prova saldo histórico nem idempotência durável.

Deadlines são BigDecimal com no máximo40 algarismos e9 casas decimais,
não negativos e abaixo de Long.MAX_VALUE. Não se reduz a precisão para caber:
representação mais precisa é recusada para revisão de formato. Básico, Shunpo,
poção e viagem têm canais próprios; cada skill usa ID do catálogo, nunca só slot.
Sem domínio, são quatro canais; com domínio, quatro mais as cinco skills reais.
Sem aritmética de expiração, conversão de tick para horário ou avanço offline.

## Limites técnicos, não regras de retenção

| Estrutura | Limite deste formato experimental | Fundamento |
| --- | ---: | --- |
| Strings técnicas | 100 caracteres | Orçamento conservador compatível com flags locais de até100; nome continua limitado pela identidade real |
| Bolsa/lista de slots | 100 | Capacidade máxima já aceita pelo InventoryService; RW atual usa20 |
| Equipamentos | 4 | Slots RPG atuais |
| Missões/objetivos | 64 /8 por missão | Contratos atuais de save/autoria |
| Recibos e watermarks | 2048 cada | Budget experimental inspirado no limite de flags; não política final de retenção |
| Direitos pendentes | 256 | Orçamento técnico compatível com teto do catálogo de itens; sem truncar direitos |
| Restrições | 64 | Orçamento de segurança deste formato, sem afirmar quantidade máxima de efeitos no produto |
| Prazos | 9 | Canais atuais do personagem com domínio |

Exceder orçamento recusa, não apaga recibo/item. Antes de integrar, aprovar
retenção/compactação e formato capaz de preservar estado legítimo maior.
Os records não formam ciclos por seus tipos e não carregam credenciais.

## Lacunas de produto e runtime explicitamente pendentes

Sempre pendentes: AUTHORIZATION, WORLD_PLACEMENT, DURABLE_COMMIT e CLOCK_POLICY.
Também podem faltar catálogo, contexto de revisão/conteúdo/época, evidência de
restrições e política de atributos. Lista vazia de restrições só satisfaz uma
exigência estrutural; não comprova captura completa sem fonte confiável.

`AttributeMode.MANUAL_SESSION` usa orçamento atual do RPG sintético sem bônus
racial; UNSPECIFIED não recebe orçamento inventado. ROOT/STUN/SLOW/BLEED/BURN
podem ser descritos, mas não há política aprovada de reativação, dano offline ou
purga. Descritor presente acrescenta RESTRICTION_POLICY. UNKNOWN mantém evidência
pendente. Captura antiga/clock insuficiente nunca recebe autorização de ativação.

Alternativas futuras: reter restrição com deadline e aguardar; recusar recuperação
até expirar sob relógio confiável; ou aprovar uma política específica por efeito.
Nenhuma foi aplicada. Para relógio, continuidade na mesma época e prazo durável
entre reinícios exigem contratos distintos e autorização. Campos opcionais
`campaignFacts`/`derivedChecks` do plano não foram implementados: falta catálogo
fechado de fatos; máximos/nível são calculados, sem segunda autoridade persistida.

Autenticação, banco, clock entre reinícios, spawn ocupado/revisão durante ativação,
captura completa e persistência transacional são testes futuros. A-04 e seu
laboratório R3 não foram modificados; não se recupera execução de Quatro relâmpagos.
