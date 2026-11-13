# ORG-03-R3 — Testes e evidências

## Ambiente e comandos

11/11/2026, Windows, Temurin 17.0.20.1, Maven 3.10.0, compilação release17.
Sem dependências novas, instalação ou downloads: todos os comandos usaram `-o`.

Da raiz, rodada dirigida final:

```powershell
mvn -o -f server/pom.xml '-Dmaven.repo.local=.maven-repository' '-Dtest=CharacterReadModelsTest,CharacterSnapshotCaptureTest,CharacterSnapshotValidatorTest,RpgWorldTest,RpgSkillEngineTest,DomainProgressionTest,InventoryServiceTest,EquipmentRpgTest,EnergyRulesTest,AttributeCombatRulesTest,QuestBookTest,QuestRewardsTest,QuestLifecycleTest,Task195SessionOwnershipTest,Task196ResumeTest,Task207InventoryTest,Task208ProgressionTest,Task209AbilitiesTest,Task210QuestJourneyTest' '-Dsurefire.runOrder=alphabetical' test
```

Suíte geral:

```powershell
mvn -o -f server/pom.xml '-Dmaven.repo.local=.maven-repository' '-Djava.awt.headless=true' test
```

## Rodadas

Logs, XMLs e somatórios ficam em `temp/org03-r3`.

| Rodada | Resultado |
| --- | --- |
| initial-tests.txt | 68 testes R1/R2 aprovados após primeira adaptação |
| read-model-tests.txt | Falha de compilação da fixture nova: quatro nomes de API incorretos; corrigida sem mudança nas regras |
| read-model-tests-corrected.txt | 101 casos aprovados na versão intermediária (33 novos + 21 R2 + 47 R1) |
| directed.txt | 252 casos, 1 erro na fixture nova: dano de 100000 acima do limite; demais 251 passaram |
| directed-final.txt | 252 casos aprovados, zero falhas/erros/exclusões; 19 classes, incluindo 36 casos novos e 23 de captura |
| full-headless.txt | 1174 casos reportados em 276 classes: 1117 aprovados, 57 excluídos, zero falhas/erros |
| full-headless-final.txt | 1174 casos em 276 classes: 1117 aprovados, 57 excluídos, zero falhas/erros; 2 min 27 s Maven |

O teste de morte passou a usar o HP atual, respeitando receiveDamage. Falhas
intermediárias estão preservadas; não se relaxou validação de produção nem
asserção para aceitar comportamento incorreto. O registro local está em
`logs/analises/ORG-03-R3-validacao.md`.

Depois da primeira suíte geral, uma asserção estrutural de teste novo foi
substituída pela verificação de flags/saldo reais após consultas repetidas.
A suíte geral foi repetida sobre essa versão final. Uma correção posterior
somente no texto de Javadoc não altera bytecode/comportamento. Não somar
rodadas como casos únicos: são **38 casos adicionais** sobre o baseline R2
(36 de read models + 2 no adaptador), de 1079 para 1117 executados/aprovados.

## Matriz dos 26 cenários solicitados

Os métodos abaixo pertencem a CharacterReadModelsTest, salvo indicação de
CharacterSnapshotCaptureTest (captura). Os testes usam mundos descartáveis,
catálogos reais, ações existentes e recompensas legítimas. Não há setters
reflexivos de campos privados nem instalação de estado recuperável.

| Nº | Cenário | Prova |
| --- | --- | --- |
| 1 | Repetição sem mutação | repeatedReadDoesNotAdvanceOrMutate |
| 2 | Atributos brutos | rawInvestmentsAreExact |
| 3 | Modo manual não ativado | disabledManualModeStaysDisabled |
| 4 | Pontos livres | freePointsAreNotGrantedAgain |
| 5 | Checkpoint exato | grantCheckpointIsNotRecalculatedFromXp; XP maior sem materializar concessão |
| 6 | Energia fracionária | regenerationFractionKeepsExactDoubleBits |
| 7 | HP/energia sem regen | resourcesAreNotHealedOrRegenerated |
| 8 | Bolsa imutável | slotsAreImmutableAndPreserveNullIndices |
| 9 | Equipamento sem duplicação | equipmentIsNotDuplicatedIntoInventory |
| 10 | XP/domínio | xpAndDomainStayExact e teste R2 xpDomainHalfUnitsAndResetMetadataAreReadExactly |
| 11 | Ataque fracionário | fractionalBasicDeadlineKeepsExactBits; initialNaNIsExplicitRatherThanInventedDeadline |
| 12 | Shunpo/poção/viagem | shunpoPotionAndTravelDeadlinesDoNotMove |
| 13 | Cinco recargas de domínio | fiveSkillDeadlinesUseRealCatalogIds; quatro prazos reais zero e quinto ativo |
| 14 | Sessão ausente | absentSkillSessionIsNotFiveZeroes |
| 15 | Sessão pronta | presentReadySessionIsDistinguishedFromAbsent; sessão real criada pela validação de cast sem alvo |
| 16 | Domínio/mundo divergente | worldBindingMismatchIsRejected; changedDomainRefusesOldContext; changedRegionRejectsStaleSkillSessionWithoutCleaning |
| 17 | Remoção prévia | removedEngineSessionRemainsAbsent; participantRemovalIsRejectedEvenWithRetainedWorld; retirada do laboratório R2 |
| 18 | Sem sessão lazy | absentQueriesDoNotCreateLazySession |
| 19 | Builtin puro | builtinLedgerKeepsCountersFlagsAndDefinitions |
| 20 | Autorado sem prêmio novo | authoredLedgerDoesNotAwardAgain; entrega real anterior à leitura |
| 21 | Sem recibos fabricados | historicalFlagsStayLocalAndUnchanged; captura exactLedgerAndResourcesStillDoNotAuthorizeEconomicActivation |
| 22 | Restrição adversa | adverseControlsAreObservedWithoutCleanup |
| 23 | Intangibilidade | intangibilityRemainsObservationOnly; captura actualLightningPhaseRemainsActiveButIsNotInTheCandidate |
| 24 | Thread incorreta | wrongThreadIsRejectedBeforeRead; recusa de captura R2 em outro owner |
| 25 | Cópia após mutação | copiedStateDoesNotFollowFutureWorldChanges |
| 26 | Falha sem mutação parcial | failedReadDoesNotPartiallyMutate; malformedLedgerReadFailsWithoutChangingInput |

Adicionais: null/contexto/mapa, participante zero, executor diferente, morte,
autoria ausente distinta de ledger vazio, contagem individual de coleta ausente,
listas aninhadas imutáveis, clock de engine long independente do tick, ausência
de sessão versus sessão pronta também no adaptador, e recusa de ativação econômica.

## Limites

O teste de thread incorreta usa o contexto já aberto no owner confiável da
fixture. Não demonstra descoberta de owner em produção nem resistência a um
chamador interno que declare contexto falso. Não houve teste de captura TCP,
concorrência real em coop-world, leave/revisão concorrentes, restart de clock,
recuperação, importação ou migração econômica.

As 57 exclusões headless constam individualmente, com classe/método/motivo, em
`temp/org03-r3/headless-exclusions.json` e nos XMLs arquivados da rodada final.
Distribuição por classe, separada dos testes aprovados:

| Classe | Excluídos |
| --- | ---: |
| CompactHudCharacterGuiTest | 1 |
| GameInputRoutingTest | 4 |
| GamePanelsIntegrationTest | 1 |
| GrabDashRefinementGuiTest | 5 |
| NetworkSkillGuiTest | 1 |
| SentryRefinementGuiTest | 1 |
| SkillAimInputRoutingTest | 1 |
| SkillImpactRefinementGuiTest | 5 |
| SkillInputRefinementGuiTest | 2 |
| SkillLifecycleGuiTest | 3 |
| SkillSlashRefinementGuiTest | 4 |
| TargetCombatGuiTest | 1 |
| Task191LifecycleTest | 1 |
| Task212PlayableUiTest | 1 |
| Task213HudSwingTest | 1 |
| Task224CastingGuiTest | 2 |
| RadminLauncherUiTest | 1 |
| SessionGameReconnectCloseTest | 3 |
| Task201RadminCancellationTest | 6 |
| Task201SessionWindowFlowTest | 1 |
| CompactWorldEditorTest | 1 |
| InGameMapEditorTest | 2 |
| QuestBuilderTest | 1 |
| StudioEmbeddedGameTest | 1 |
| StudioGestureSafetyTest | 1 |
| StudioUsabilityTest | 6 |

Essas exclusões não são homologação de
Swing, foco, teclado físico, Radmin ou launcher nativo. Testes anteriores podem
usar TCP loopback e diretórios temporários; as novas APIs não abrem transporte
nem acessam saves pessoais.
