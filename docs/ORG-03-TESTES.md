# ORG-03 — Testes, evidências e limites

## Resultado executado nesta etapa

09/11/2026, Windows 10, Java Temurin 17.0.20.1 e Maven 3.10.0.
**87 casos em 20 classes aprovados; zero falhas, erros ou skips.**
Lifecycle Maven offline, sem cópia manual de recursos. Tempo Maven: 10,164s.
Nenhum arquivo Java foi criado/alterado; não há teste novo disfarçando uma
proposta como integração existente.

Comando, executado da raiz:

```powershell
mvn -o -f server/pom.xml '-Dmaven.repo.local=.maven-repository' '-Dtest=CharacterIdentityTest,DomainProgressionTest,RpgWorldTest,LocalSaveStoreTest,CharacterSavesTest,InventoryServiceTest,EquipmentRpgTest,EnergyRulesTest,AttributeCombatRulesTest,QuestBookTest,QuestRewardsTest,QuestLifecycleTest,SyntheticOnlineRepositoryTest,Task194PersistenceTest,Task195SessionOwnershipTest,Task196ResumeTest,Task225R3RecoveryLabTest,SessionCharacterSelectionTest,Task201CharacterSessionTest,AttributeAllocationTest' '-Dsurefire.runOrder=alphabetical' test
```

Evidência local: `temp/org03/maven-directed.txt`, `test-summary.json` e
`directed-reports/`. O resumo arquiva somente XMLs das classes `Running` desta
execução, sem somar XMLs antigos deixados pelo Maven no target.

Foram usados diretórios temporários dos testes, arquivos `online-synthetic-*`
de laboratório, memória e TCP loopback. Testes de retomada também criam processos
Java separados para arquivos sintéticos; não abrem clientes gráficos nem banco.
Nenhum teste foi executado contra conta/save pessoal. O teste de recompensas
do laboratório não é recuperação online após restart do servidor de jogo.

## Matriz pedida: evidência atual e futuro contrato

Todos os testes citados abaixo são existentes e fizeram parte dos 87 casos.
Sua aprovação vale no escopo declarado; o DTO futuro ainda não existe.

| Cenário | Evidência executada no código/laboratório atual | Critério conceitual para o novo contrato |
| --- | --- | --- |
| Snapshot válido | LocalSaveStoreTest.roundTripAndFailurePreserveLastGoodSave; Task225R3RecoveryLabTest.validRecoveryPreservesEntireSnapshotAndDropsBlockedPhase | Instalar candidato completo sem concessões nem execução antiga |
| Snapshot incompleto | LocalSaveStoreTest.rejectsCorruptionVersionAndDuplicateFields; R3 unsupportedVersionAndMissingCooldownAreRejected | Recusar ausência de qualquer bloco obrigatório, inclusive atributos/ledger; cobertura nova completa pendente |
| Versão incompatível | Os dois testes anteriores rejeitam schemas inválidos | Recusar antes de mutação; migrador próprio somente se aprovado |
| Estado antigo | Task194PersistenceTest.concurrentCasHasExactlyOneCommit; Task196ResumeTest.divergentRetryAndStaleHandleNeverOverwriteConfirmedRewards | Revisão e fence atuais; nunca overwrite de geração antiga |
| Inventário duplicado | R3 duplicateRecoveryCannotDuplicateItemsOrRewards; InventoryServiceTest.stacksSplitTransferAndRollbackWithoutLoss | Replay não soma; lista com slot duplicado recusa antes de Map. Este último é leitura de restore/plano de teste, não novo caso executado |
| Missão já recompensada | QuestRewardsTest.stableEventsAndWorldRewardsAreIdempotent; R3 previouslyGrantedRealCatalogRewardsAreNotReplayed | Recibo de pagamento e estado econômico no mesmo commit |
| Cooldown ativo | R3 cooldownAndResourcePolicyHasNoFreeResetOrHealing | Preservar deadline e restante; clock durável/rollback de relógio ainda sem teste implementado |
| Sessão antiga | R3 oldTicketCannotAffectAnotherAdmissionGeneration; Task195SessionOwnershipTest | Comando/callback/handle antigos não afetam geração nova |
| Reconexão concorrente | R3 simultaneousRecoveriesHaveExactlyOneWinner | Um claim/fence; falha pós-commit resolve por recibo, não nova admissão |
| Região inválida | R3 changedContextIsRejectedWithoutConsuming; Task196ResumeTest.incompatibleContentRegionAndObstructedPositionFailSafely | Região/revisão desconhecidas recusam sem reset silencioso |
| Spawn ocupado | R3 blockedSpawnDoesNotConsumeOrRestoreInsideObstacle e realGeometryRejectsObstructedSavedPosition | Revalidar corpos atuais e reserva no momento de ativação |
| Efeito temporário não restaurável | R3 validRecoveryPreservesEntireSnapshotAndDropsBlockedPhase | Intangibilidade/projéteis/input zerados; custo/recarga e restrições pertinentes preservados |

R3 usa envelope de teste e callbacks de spawn; não instala esse estado em
RpgSessionWorld nem negocia recuperação por TCP. Parte dos dados da fixture
R3 é preparação explícita, não XP/morte/recompensa produzidos naquele cenário.
Task201CharacterSessionTest valida a admissão/reconexão **efêmera atual**,
inclusive nomes/raças iguais e novo ID. Não comprova identidade persistente.

## Preservação e verificação documental

Inventário SHA-256 anterior: 2336 arquivos existentes de `server/src`, assets,
tools, database, POM e Markdown na raiz de docs. O conteúdo anterior do documento
principal foi copiado para `temp/org03/ORGANIZACAO-before.md`. Git status e
name-status anteriores foram arquivados. Nenhuma exclusão anterior foi revertida.

O verificador compara todos os hashes, arquivos ausentes, novos arquivos de
fonte/SQL, links relativos e estrutura das matrizes. Resultado e lista de
alterações em `temp/org03/validation.json`. A única alteração permitida entre
os arquivos anteriores é o adendo no documento principal; sete documentos
ORG-03 são novos. Leitura das fontes e diffs existentes não foi tratada como
autorização para editar produção.

O primeiro resumo global de `git diff --stat` produziu saída extensa/truncada;
a preservação usa a lista arquivada e hashes, não esse trecho como backup.
Uma busca com caminho inválido durante a auditoria foi substituída por caminhos
explícitos; não houve edição em consequência dessa busca. Nenhuma falha de teste
intermediária foi observada na única rodada dirigida.

## O que não foi executado e por quê

A suíte geral de 1068 casos foi executada na ORG-02-R2, não repetida nesta ORG-03.
O diff desta etapa é documental, sem alteração de runtime/testes/POM; os testes
dirigidos dão evidência dos contratos auditados. Uma nova suíte gráfica não
validaria o contrato proposto e exigiria novamente ambiente gráfico controlado.
O problema de foco da ORG-02-R2 permanece sem causa comprovada.

Não houve PostgreSQL, SQL/migration, conta real, importação multiplayer,
homologação física, Radmin ou launcher. CAS transacional de banco, lease entre
servidores, captura/importação completas, clock entre reinícios e política de
status adversos são testes futuros, condicionados à implementação autorizada.
