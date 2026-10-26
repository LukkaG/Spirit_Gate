# TASK-179 — testes e evidências

JUnit: `python tools/run-local-tests.py`. GUI real: `python tools/task179-verify.py server/target/local-tests/<run>`. Testes novos em StudioLiveTest e regressões anteriores da TASK-178/177 e ferramentas. Resultado final e hashes: `.task-checkpoints/TASK-179/world-studio-live/validation-summary.json`.

## Matriz do pedido

| Itens | Evidência / limite |
|---|---|
| 1–8 isolamento | previewIsIsolatedFromRealMainServerAndSaves, publicGameplaySocketHasNoEditorOrAdministrativeCommand, disconnectAndInterruptedJournalLeaveNoFalsePublication; portas e diretórios distintos, save sentinela, principal ativo após encerrar preview |
| 9–13 obstáculos/tiles/camadas | createMoveDeleteAndLayerUpdatesKeepPlayerState, incrementalTilesUseBoundedDeltaAndDoNotShareImageCaches e probe GUI de 20 edits |
| 14,16 perfil/restauração | profileChangesAreLiveAndReversibleWithoutProgressionChanges, occupiedPlayerRejectsWallAndProfileGrowthAtomically |
| 15 offset | somente zero suportado; configuração desconhecida é recusada em task178BaselineProfileAndCanonicalHashAreStable, sem simular suporte |
| 17–18 ordem/ressync | oldSessionAndOutOfOrderEventsCannotMutateAndFullSyncRecovers e patch com hash-base divergente |
| 19–24 colisões | Task178CollisionTest, Task177GeometryTest e oito cenários Live usando o mesmo solver |
| 25–28 spawn/respawn/portal/Shunpo | validação de spawn, playerEnemyShunpoPortalAndResetUseRealRpgRules, testes TASK-177/178 e prechecks de ocupação no backend Live |
| 29–32 inimigo/frame/cosméticos/cinza | SharedRpgEncounter real, Task177AppearanceTest, Task178CollisionTest, Task178ShunpoTest e cenários cinza/cosméticos |
| 33–37 Salvar | saveActivatesMapAndCollisionTogetherAndRetainsHistory e probe GUI: destino preservado antes de Salvar, draft-only sem ativação e hash confirmado após Salvar |
| 38–41 falhas/rollback/conflito | stagingAndActivationFailuresRollbackFilesAndServer, publicação com base antiga, validação rejeitada, publishedFileCorruptionIsDetectedBeforeRecoveryOrActivation, histórico e rollback GUI |
| 42 cliente incompatível | incompatibleClientCannotReceivePublication |
| 43 área ocupada | occupiedPlayerRejectsWallAndProfileGrowthAtomically e fork sintético validado antes da reserva |
| 44 desconexão | reservedServerRejectsMutationAndDisconnectAbortsActivation e destino fechado durante tentativa de publicação |
| 45–54 regressões | suíte geral de offline, coop, PvE, Radmin launcher, Quest Builder, Studio, saves V1/V2, raças/domínios, sprites, movimento e Shunpo |

Publicação/rollback acima referem-se ao destino sintético local, não ao Game Server Radmin. O caso publicationIsBlockedForProductionAndNeverWrites verifica o bloqueio real. Não se declara testado hot reload de produção, que não foi implementado.

## Probe visual e medições

Task179LiveProbe abre o editor e as janelas jogáveis reais sobre um projeto sintético no checkpoint. Copia sprites originais somente para leitura e cria uma imagem de tile de teste, sem alterar assets do projeto. Mede início, 20 edits, validação, tick, paint e heap; grava before-edit.png, live-edit-unpublished.png, saved-and-rollback.png e benchmark.json.

As capturas combinam componentes reais para mostrar editor e gameplay simultaneamente; não são aprovação humana nem captura física de dois computadores. O benchmark inclui debounce de 200 ms e intervalo do timer do editor. Heap não controla GC; os números não são uma alegação de ganho de memória/CPU. Não são RTT de Radmin.

O probe comprova também: salvar rascunho não ativa, edição não cria namespace de publicação, destino mantém seu hash até Salvar, três entradas de histórico e rollback confirmado. Falhas de staging/ativação são injetadas nos testes para verificar ponteiros e estado em memória.

Pendentes: homologação humana das telas, falha fatal de JVM/energia, distribuição de revisão a clientes de produção e teste Radmin remoto. Não foram usados projetos pessoais nas provas de publicação.
