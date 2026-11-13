# ORG-03-R5 — Testes e evidências

## Ambiente e comandos

11/11/2026, Windows, JDK Temurin 17.0.20.1, Maven 3.10.0, release 17.
Sem instalação, dependências novas ou downloads; todos os comandos usam `-o`.

Novos casos, da raiz:

```powershell
mvn -o -f server/pom.xml '-Dmaven.repo.local=.maven-repository' '-Dtest=EconomicLaboratoryTest' test
```

Regressão dirigida:

```powershell
mvn -o -f server/pom.xml '-Dmaven.repo.local=.maven-repository' '-Dtest=EconomicLaboratoryTest,SyntheticOnlineRepositoryTest,Task194PersistenceTest,Task195SessionOwnershipTest,Task196ResumeTest,Task225R3RecoveryLabTest,QuestLifecycleTest,QuestRewardsTest,QuestBookTest,AuthoredQuestRuntimeTest,LootServiceTest,InventoryServiceTest,DomainProgressionTest,Task206SharedLootTest,CharacterSnapshotValidatorTest,CharacterSnapshotCaptureTest,CharacterReadModelsTest,OwnerCharacterCaptureTest,RpgCharacterObservationTest' '-Dsurefire.runOrder=alphabetical' test
```

Geral:

```powershell
mvn -o -f server/pom.xml '-Dmaven.repo.local=.maven-repository' '-Djava.awt.headless=true' test
```

## Rodadas

Logs, XMLs arquivados e summaries estão em `temp/org03-r5`.

| Rodada | Resultado |
| --- | --- |
| new-tests.txt | 56 casos iniciais aprovados, zero falhas/erros/exclusões |
| directed.txt | 241 casos em 19 classes aprovados, zero falhas/erros/exclusões; 62 casos econômicos nessa versão |
| new-tests-final.txt | 63 casos econômicos finais aprovados, zero falhas/erros/exclusões |
| full-headless.txt | 1253 casos em 279 classes: 1196 executados/aprovados, 57 excluídos, zero falhas/erros; 2 min 12 s Maven |

O último reforço verifica pacote sintético parcialmente acomodável: todos os
itens ficam pendentes e um claim sem espaço não publica a primeira inserção.
Também foram endurecidas referências obrigatórias da imagem. Nenhum fonte de
produção mudou. Não somar as rodadas como testes únicos: são 63 casos novos.

## Falhas controladas exigidas

Todos os métodos abaixo pertencem a EconomicLaboratoryTest.

| Cenário | Evidência |
| --- | --- |
| Commit aceito | questBalancesDeliveryReceiptAndRevisionCommitTogether |
| Commit rejeitado | wrongEpochAndRevisionAreRefused; requestedValueMustMatchTrustedResolvedOffer |
| Estado inválido antes de commit | unknownCatalogItemAndIncompleteQuestOpeningAreRefused; malformedIdsAndNullReferencesFailBeforeAnyCommit |
| Falha durante preparação | controlledFailuresNeverExposePartialReward, parâmetro PREPARATION |
| Resposta perdida após commit | lostResponseRequiresLookupAndDoesNotMakeCommitDisappear |
| Duas solicitações simultâneas | twoConcurrentOperationsHaveExactlyOneCasWinner: 12 disputas reais de duas threads |
| Duplicata econômica | simultaneousDifferentIdsForOneGrantCannotDuplicateEconomy; casos da matriz de recibos |
| Concessão parcialmente representada | DROP_RECEIPT/CORRUPT_RECEIPT recusados antes do CAS; partiallyFittingSyntheticBundleIsEntirelyPendingAndClaimRemainsAtomic |
| Limite de inventário | fullQuestInventoryRejectsRewardDeliveryAndReceiptTogether; claimWithoutSpacePreservesWholePendingRight |
| Overflow moedas/XP/domínio | resourceOverflowDoesNotPartiallyCompleteQuest; numericOverflowInAuthorizedTermsFailsClosed |
| Personagem errado | wrongBeneficiaryIsRefusedBeforeAnyEconomicChange; sameOperationIdCannotCrossBeneficiaries |
| Época errada | wrongEpochAndRevisionAreRefused; oldOfferCannotBecomeNewEpochLootByChangingRequestContext |
| Revisão errada/antiga | latePreparedCallbackCannotOverwriteNewRevision; replayingOldReceiptDoesNotRollBackLaterBalances |
| Recibo conflitante | sameIdDifferentOriginIsConflict; conflictingReceiptFingerprintIsNotAcceptedAsHistory |
| Falha de reconciliação | failedReconciliationRemainsUncertainAndNeverRetriesBlindly |
| Reinicialização simulada do coordenador | coordinatorReinitializationOnlyRetainsSameInMemoryLedger |
| Retenção insuficiente | receiptCapacityRejectsNewCommandsWithoutEviction; losingReceiptOrDedupIndexMakesHistoryIncomplete |
| Pendência sem origem | pendingWithoutSourceOrReceiptCannotBeConstructedOrImported |

## Outras garantias exercitadas

CAS entre beneficiários distintos preserva ambos os commits; a disputa da mesma
revisão produz uma escrita e uma recusa, sem last-write-wins. Duas intenções
iguais simultâneas produzem uma escrita e um replay do mesmo recibo.

READY sem recibo não é entregue. Quest builtin e autorada confirmam prêmio e
entrega juntos. Quest de coleta revalida e consome itens apenas no commit;
READY com inventário insuficiente é recusada. Domínio não escolhido não ganha
maestria inventada. Slots, direitos, mapas de recibos e termos são imutáveis.

Mudança de admissão/época invalida callbacks anteriores. Novo coordenador pode
consultar histórico autorizado sem reaplicar uma intenção antiga. Perda da
memória é testada separadamente: um ledger novo não encontra o commit anterior;
o teste não faz ressubmissão cega nem alega continuidade durável.

O RNG contado é usado pela fixture para resolver LootService antes das operações.
Retries e consultas não aumentam sua contagem. Fingerprints ignoram ordem de
iteração dos mapas e preservam índices/quantidades reais.

## Isolamento e limites

Os novos testes não iniciam servidor, banco, launcher ou processo externo.
Serviços reais trabalham apenas com inventários descartáveis. Aberturas são
sintéticas; bolsas de 1/2 slots tornam falhas determinísticas, sem reduzir a
bolsa do jogo. O pacote de dois itens testa atomicidade, sem editar drops.

Os laboratórios anteriores ainda usam seus diretórios temporários/processos
e os testes R4 usam TCP loopback. Isso não integra R5 a eles. Nenhuma conta,
credencial, save pessoal ou arquivo econômico persistente foi criado pela R5.

CAS em memória, pontos de falha controlados e recriação de Coordinator não
comprovam durabilidade de processo ou armazenamento. A auditoria não é assinatura
contra administrador malicioso. Não há teste de migração nem importação.

Exclusões gráficas da suíte geral são registradas separadamente em
`temp/org03-r5/headless-exclusions.json`, com classe/método/motivo. Não representam
homologação de Swing, Radmin físico ou launcher nativo.


## Preservação final

3760 arquivos anteriores conferidos; somente o documento principal mudou conforme o adendo autorizado. Nenhuma alteração de produção, ausência ou diferença inesperada. 6886 evidências conferidas após restauração; dois TXT A-04 regenerados foram arquivados e restaurados individualmente. Ver `temp/org03-r5/validation.json`.

## Exclusões gráficas separadas

57 casos nas classes abaixo; métodos e motivos constam no JSON e XMLs finais.

| Classe | Excluídos |
| --- | ---: |
| CompactHudCharacterGuiTest | 1 |
| CompactWorldEditorTest | 1 |
| GameInputRoutingTest | 4 |
| GamePanelsIntegrationTest | 1 |
| GrabDashRefinementGuiTest | 5 |
| InGameMapEditorTest | 2 |
| NetworkSkillGuiTest | 1 |
| QuestBuilderTest | 1 |
| RadminLauncherUiTest | 1 |
| SentryRefinementGuiTest | 1 |
| SessionGameReconnectCloseTest | 3 |
| SkillAimInputRoutingTest | 1 |
| SkillImpactRefinementGuiTest | 5 |
| SkillInputRefinementGuiTest | 2 |
| SkillLifecycleGuiTest | 3 |
| SkillSlashRefinementGuiTest | 4 |
| StudioEmbeddedGameTest | 1 |
| StudioGestureSafetyTest | 1 |
| StudioUsabilityTest | 6 |
| TargetCombatGuiTest | 1 |
| Task191LifecycleTest | 1 |
| Task201RadminCancellationTest | 6 |
| Task201SessionWindowFlowTest | 1 |
| Task212PlayableUiTest | 1 |
| Task213HudSwingTest | 1 |
| Task224CastingGuiTest | 2 |
