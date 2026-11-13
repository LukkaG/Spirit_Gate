# ORG-03-R2 — Testes e evidências

## Resultado final

11/11/2026, Temurin 17.0.20.1, Maven 3.10.0, Windows 10. Rodada dirigida final:
**270 casos em 28 classes aprovados, zero falhas/erros/skips**. Inclui todos os
47 casos R1, 21 novos de captura e regressões de Player/progressão/inventário/
missões/habilidades. Os 21 novos também estão na suíte geral.

Geral headless: **1136 casos reportados em 275 classes; 1079 executados e
aprovados, 57 excluídos, zero falhas/erros**. Tempo Maven final: 2 min 17 s.
Não somar rodadas como casos únicos e não interpretar exclusões como homologação
de Swing, foco, hotkeys, teclado físico, Radmin ou launcher nativo.

## Comandos, da raiz

Casos novos e contrato anterior:

```powershell
mvn -o -f server/pom.xml '-Dmaven.repo.local=.maven-repository' '-Dtest=CharacterSnapshotCaptureTest,CharacterSnapshotValidatorTest' test
```

Regressão dirigida final:

```powershell
mvn -o -f server/pom.xml '-Dmaven.repo.local=.maven-repository' '-Dtest=CharacterSnapshotCaptureTest,CharacterSnapshotValidatorTest,CharacterIdentityTest,DomainProgressionTest,RpgWorldTest,LocalSaveStoreTest,CharacterSavesTest,InventoryServiceTest,EquipmentRpgTest,EnergyRulesTest,AttributeCombatRulesTest,QuestBookTest,QuestRewardsTest,QuestLifecycleTest,SyntheticOnlineRepositoryTest,Task194PersistenceTest,Task195SessionOwnershipTest,Task196ResumeTest,Task225R3RecoveryLabTest,SessionCharacterSelectionTest,Task201CharacterSessionTest,AttributeAllocationTest,RpgSkillEngineTest,SkillProtocolTest,Task207InventoryTest,Task208ProgressionTest,Task209AbilitiesTest,Task210QuestJourneyTest' '-Dsurefire.runOrder=alphabetical' test
```

Suíte geral:

```powershell
mvn -o -f server/pom.xml '-Dmaven.repo.local=.maven-repository' '-Djava.awt.headless=true' test
```

Compilação release17 ocorreu no lifecycle; sem instalação, dependência nova ou
cópia manual de resources. Fixtures R2 usam executor único, cenas em memória e
catálogos reais; não há socket, cliente ou save no protótipo. Os testes anteriores
podem usar TCP loopback e @TempDir, sem conta ou save pessoal.

## Rodadas e falha intermediária preservada

| Log em temp/org03-r2 | Casos | Falhas | Erros | Skips | Resultado |
| --- | ---: | ---: | ---: | ---: | --- |
| new-tests.txt | 68 | 0 | 20 | 0 | Erros de preparo da fixture; R1 passou |
| new-tests-corrected.txt | 68 | 0 | 0 | 0 | Aprovado após ajuste somente na fixture nova |
| directed.txt | 270 | 0 | 0 | 0 | Aprovado, 19, 039s Maven |
| full-headless.txt | 1136 | 0 | 0 | 57 | Aprovado nos1079 executados |
| removed-active-session.txt | 1 | 0 | 0 | 0 | Caso de retirada reforçado com habilidade e recarga ativas |
| full-headless-final.txt | 1136 | 0 | 0 | 57 | Versão final, incluindo o caso reforçado |

O portal estava ao lado do personagem e interact() priorizou viagem antes de
acceptQuest. Corrigido o preparo para usar interactSyntheticNpc(), API existente
para NPC sem priorizar portal. Nenhuma asserção, regra, limite físico ou personagem
de produção foi alterado. A identidade da fixture também foi tornada explicitamente
determinística por construtor validado, sem UUID derivado de peer.

Cada rodada arquiva summary.json/XMLs somente das classes Running correspondentes.
O erro de fixture é registrado em logs/analises/ORG-03-R2-fixture-portal.md.
Após a primeira suíte geral, o teste de retirada foi reforçado: confirma recarga
ativa antes de remove, observa os zeros do engine fora da captura e exige que
o exportador recuse a fonte retirada, sem candidato. O caso passou isolado e
a suíte geral foi repetida sobre essa versão final. Não mudou o capturador.

## Cobertura dos 20 cenários pedidos

Métodos citados pertencem a CharacterSnapshotCaptureTest. **Parcial/pendente**
significa que a informação integral não está disponível, não um teste verde
de uma exportação inexistente.

| Nº | Cenário | Evidência e limite |
| --- | --- | --- |
| 1 | Leitura coerente | coherentOwnerReadMatchesRuntimeAndRepeatsExactly: mesmo owner, tick/contexto e resultado repetido |
| 2 | Sem alteração | captureDoesNotAdvanceOrAlterAvailableAuthoritativeState: compara valores públicos e estado inicializado antes/depois |
| 3 | Cópia imutável | capturedCopiesRemainImmutableAfterRealInventoryChanges: alterações posteriores na bolsa não mudam a cópia |
| 4 | XP preservada | xpDomainHalfUnitsAndResetMetadataAreReadExactly: morte real, XP e meias unidades exatas |
| 5 | Atributos | effectiveAttributesAreObservedButRawGrantStateIsNotInvented: AGI/Energy efetivos e record de preparo preservados; export bruto/pontos pendente |
| 6 | Itens/quantidades | allTwentySlotsAndQuantitiesKeepTheirActualIndices: capacidade20 e índice19 preservados |
| 7 | Equipamento | equippedSwordIsNotAddedBackToTheBag: espada equipada não reaparece na bolsa |
| 8 | Moedas | actualKillCoinsAreExportedWithoutAnotherReward: 5 moedas reais, repetição não premia |
| 9 | Missões/objetivos | authoredSummariesArePreservedWithoutPretendingToBeObjectiveLedgers: resumo READY real; vetor individual/builtin pendentes |
| 10 | Recibos ausentes | missingReceiptsAndLootRightsAreUnavailableRatherThanEmptyCollections: null/NOT_PRESENT; nenhum recibo fabricado |
| 11 | Loot pendente | Mesmo teste identifica falta de accessor e de referência de concessão; direito completo não capturado, pendente |
| 12 | Precisão de cooldown | roundedBasicCooldownNeverBecomesAnExactDeadline: intervalo real fracionário e view inteira; bloco exato nulo, não conversão com perda |
| 13 | HP/energia | hpAndAbsoluteEnergyStayBelowMaximumWithoutRefill: saldos absolutos preservados; fração privada pendente |
| 14 | Morte | deadCharacterIsNotResurrectedOrRefilled: hp0 continua morto |
| 15 | Intangibilidade | actualLightningPhaseRemainsActiveButIsNotInTheCandidate: cast real do slot5, fase ativa preservada, sem export de execução |
| 16 | Restrição | activeNegativeControlIsNotClearedAndRemainsAPolicyGap: ROOT permanece, sem inferir duração/origem ou limpar |
| 17 | Região/contexto | changedRegionOrControllerContextRefusesTheOldCaptureRequest: divergência anterior recusada; corrida real no meio da captura fica pendente |
| 18 | Sessão retirada | retiredLaboratoryParticipationHasNoCandidateOrZeroedSkillFallback: retirada local real do encontro/engine e flag da fixture; integração RS.leave pendente |
| 19 | Dados incompletos | incompleteCandidateStaysRejectedByR1AndValidationRepeatsWithoutEffects: blocos ausentes mantidos nulos e rejeitados pela R1 |
| 20 | Repetição pura | Mesmo teste e leitura repetida sem mudança; checks de autorização/mundo/durabilidade/clock permanecem pendentes |

Casos adicionais: thread errada recusada antes da leitura, identidade sintética
separada de UUID observado, revisão inválida classificada e constant pool do
capturador sem chamadas de save/snapshot/manualAttributes/tick/cast/equip/loot/
concessão/limpeza ou reflexão de campos privados. São21 testes, não21 cenários
de captura integral. O parser de bytecode lê apenas a classe de teste, fora da captura.

## Preservação

2351 hashes anteriores e checkpoint em bytes de 11 arquivos relevantes; Git
inicial arquivado. Os fontes R1 e toda produção anterior devem permanecer
idênticos. Conferência final em temp/org03-r2/validation.json. A única mudança
permitida em arquivo anterior é o adendo no principal de organização.

6702 PNGs e 6 arquivos textuais de evidências foram preservados antes da suíte.
Dois TXT de medições A-04 foram regenerados pelos testes existentes; as novas
saídas foram arquivadas em generated-this-task e os anteriores restaurados
individualmente. Não são alteração da TASK-225 nem correção de A-04.
Listas: evidence-restored.json e evidence-restored-final.json. Saídas da última
rodada estão separadas em generated-final-round, preservando também a anterior.
Sem limpeza global ou sobrescrita de assets aprovados.

## Limitações

Não se mediu atomicidade de servidor sob carga, captura TCP, revisão/leave
concorrentes, export de ledger completo, recibo durável ou instalação de estado.
Nada foi lido do cliente como autoridade. Sem PostgreSQL, JDBC, migrations,
autenticação, tokens, recuperação/reconexão automática ou save pessoal.
Os cenários de API ausente constam na matriz/lacunas, sem substituir testes por
setters reflexivos. A investigação AWT não foi retomada nesta R2.
