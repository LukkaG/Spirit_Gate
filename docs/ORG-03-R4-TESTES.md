# ORG-03-R4 — Testes

## Ambiente e comandos

11/11/2026, Windows, Temurin 17.0.20.1, Maven 3.10.0, release 17. Sem instalação,
dependência nova ou download. Da raiz:

```powershell
mvn -o -f server/pom.xml '-Dmaven.repo.local=.maven-repository' '-Dtest=OwnerCharacterCaptureTest,RpgCharacterObservationTest,CharacterReadModelsTest,CharacterSnapshotCaptureTest,CharacterSnapshotValidatorTest' test
```

Regressão dirigida:

```powershell
mvn -o -f server/pom.xml '-Dmaven.repo.local=.maven-repository' '-Dtest=OwnerCharacterCaptureTest,RpgCharacterObservationTest,CharacterReadModelsTest,CharacterSnapshotCaptureTest,CharacterSnapshotValidatorTest,RpgWorldTest,RpgSkillEngineTest,InventoryServiceTest,EquipmentRpgTest,DomainProgressionTest,EnergyRulesTest,AttributeCombatRulesTest,QuestBookTest,QuestRewardsTest,QuestLifecycleTest,Task195SessionOwnershipTest,Task196ResumeTest,Task225R2ShutdownTcpLabTest,SessionCharacterSelectionTest,SkillProtocolTest,Task207InventoryTest,Task208ProgressionTest,Task209AbilitiesTest,Task210QuestJourneyTest' '-Dsurefire.runOrder=alphabetical' test
```

Suíte geral:

```powershell
mvn -o -f server/pom.xml '-Dmaven.repo.local=.maven-repository' '-Djava.awt.headless=true' test
```

## Rodadas e artefatos

`temp/org03-r4` contém logs, XMLs arquivados por rodada e summaries JSON. Não
somar execuções repetidas como casos únicos. Os novos casos totalizam 16: 13
em OwnerCharacterCaptureTest (inclui parâmetro 6/7) e 3 em RpgCharacterObservationTest.

| Rodada | Resultado |
| --- | --- |
| initial-tests.txt | 114 casos aprovados na primeira versão dos ensaios TCP/R1/R2/R3 |
| capture-tests.txt | 122 casos aprovados, zero falhas/erros/exclusões;16 novos completos |
| directed.txt | 282 casos aprovados em 24 classes, zero falhas/erros/exclusões |
| full-headless.txt | 1190 casos em 278 classes: 1133 aprovados, 57 excluídos, zero falhas/erros; 2 min 18 s |
| full-headless-final.txt | 1190 casos em 278 classes: 1133 aprovados, 57 excluídos, zero falhas/erros; 2 min 18 s |

Não houve falha de compilação/teste nas rodadas dirigidas. Durante a primeira
suíte geral foi reforçado o ensaio de entrada tardia: além dos ticks diferentes,
agora lança Quatro relâmpagos e exige que o deadline use worldTick. Por isso a
suíte geral final recompila e verifica a versão definitiva do teste.

## Associação e pureza comprovadas em TCP

| Cenário | Método de OwnerCharacterCaptureTest |
| --- | --- |
| Dois participantes, protocolos 6/7, cópias/snapshots estáveis | realOwnerCopiesBothParticipantsWithoutChangingPrivateOrObserverState |
| Ausência, pronta, ativa, substituição de mundo e remoção | absentReadyActiveAndRemovedSkillSessionsRemainDistinct |
| Thread errada, owner não vinculado, pedido inválido/ausente | wrongThreadMissingSourceAndInvalidRequestReturnOnlyDiagnostics |
| Domínio/região/congelamento/revisão de conteúdo | domainRegionFreezeAndContentRevisionRejectStaleRequests |
| Socket fechado após departures, antes da captura; readmissão | disconnectBetweenDepartureDrainAndCaptureIsRejectedThenRemoved |
| Timing com região antiga, sem limpeza na leitura, remoção no step | staleSkillRegionIsRefusedWithoutCleanupUntilNormalStepRemovesIt |
| Comandos FIFO, intenção sem avanço e snapshot posterior ao step | consecutiveCommandsCaptureBeforeStepAndSnapshotReflectsMovementAfterStep |
| Mundo de entrada tardia com deadline sobre seu próprio tick | lateParticipantKeepsSeparateWorldSessionAndSkillClocks |
| Recompensa legítima anterior, XP/moedas/ledger sem nova concessão | realRewardBalancesAndLedgersAreReadWithoutAnotherGrant |
| Repetição, tick estável e custo pareado de duas leituras | commandOrderSnapshotsAndReadCostAreMeasuredWithoutAdvancingSimulation |

A fixture passa RpgSessionWorld diretamente ao servidor real. Não fabrica o
owner por nome e não usa proxy para declarar presença. O nome coop-world é uma
asserção complementar; a guarda real compara o objeto Thread registrado pelo
servidor. A única reflexão lê a fila commands para o ensaio, sem obter RpgWorld,
Session ou executor privado de skills.

Comparam-se os snapshots existentes de mundo, gameplay, combate e skills no
mesmo ponto serializado, além do resultado exato repetido: HP, energia, fração,
XP, moedas, bolsa, equipamento, missões, prazos, posições, identidades e flags
permanecem iguais. Depois a simulação segue e os clientes continuam conectados,
recebendo exclusivamente os formatos existentes. Não se envia a captura ao TCP.
Movimento após step e regressões existentes de colisão completam a verificação;
não se modificou geometria, corpo ou regras de colisão para capturar.

## Provas somente de laboratório

Dois métodos de OwnerCharacterCaptureTest declaram owner/presença locais:
`sameNumericIdWithAnotherSessionCannotReuseOldRequestInLaboratory` demonstra
troca de Session com ID e UUID iguais, e
`disappearanceAtFinalPresenceCheckDiscardsWholeObservationInLaboratory` controla
a guarda final. Não são apresentados como admissão TCP real nem corrida medida
de scheduler/reader nessa segunda guarda.

RpgCharacterObservationTest verifica ROOT 37, STUN 29, SLOW 41 com escala 0.6 e
efeitos externos desconhecidos, sem limpar controles; verifica também owner
incorreto antes de ler referência nula e mapa de participação divergente. São
provas da ponte com mundos descartáveis, sem alegação de aplicação desses três
controles por combate TCP. A R3 continua cobrindo precisão/imutabilidade dos
read models; os 47 testes R1 e 23 de captura experimental foram reexecutados.

## Custo e orçamento

Medição em Windows/JDK17, servidor loopback com dois participantes, uma única
tarefa no owner, 20 pares de aquecimento e 100 pares medidos. Cada amostra inclui
duas capturas sequenciais e construção da lista; assertions ficam fora do
intervalo medido. Não representa dois owners concorrentes, tráfego intenso,
perfil do GC ou garantia de throughput/latência em produção.

Na rodada dirigida: mínimo 40.2 µs, mediana 68.1 µs, p95 164 µs e máximo 297.1 µs
**por par de capturas**. O log final registra nova medição sob a suíte geral.
Na geral final: mínimo 26.9 µs, mediana 36.8 µs, p95 108.7 µs e máximo 203.4 µs
por par. São amostras sobre o conteúdo baseline, com bolsa de 20 slots e sem
histórico longo de flags/missões. O custo de cópia varia com o estado existente;
aquecimento/JIT/GC/agendamento também afetam a medição.
Não há limiar de aprovação por tempo. O tick nominal é33.33 ms; não se deduz
desse ensaio um orçamento aprovado de polling ou frequência segura de exportação.
Não há fila nova em produção; a fila de comandos mantém capacidade 64 e limite 64
por rodada. FutureTask/esperas/cancelamento pertencem exclusivamente à fixture.

## Exclusões e limites

Exclusões gráficas finais são separadas em `headless-exclusions.json`, com
classe, método e motivo, além dos XMLs. Não são homologação Swing, foco,
hotkeys, Radmin físico ou launcher nativo. Testes existentes podem usar
diretórios temporários e transporte loopback. Nenhum save pessoal foi acessado.

Não se validou persistência/recuperação, autenticação, migração econômica,
restart de clock, reatribuição de owner, carga de produção ou publicação
distribuída completa. A troca de revisão dos testes é instalação local em
memória pelas APIs já existentes, sem arquivo/canal updates ou alteração de
World Studio. Nenhum timeout A-04 foi implementado.

## Preservação e verificação final

Comparação de 3737 arquivos anteriores: seis alterações previstas, nenhuma
inesperada e nenhum arquivo ausente. Os cinco fontes anteriores modificados
foram conferidos contra os checkpoints: somente adições de captura/startup e
modificadores de visibilidade previstos; lógica anterior preservada.

6886 arquivos de evidências foram conferidos/restaurados conforme o inventário.
Os dois TXT de medições A-04 regenerados pela suíte tiveram as saídas novas
arquivadas em `generated-this-task` e os originais restaurados individualmente.
Detalhes em `validation.json`, `affected-hashes.json` e `evidence-restored.json`.

O primeiro script auxiliar de comparação esperava a declaração de classe sem
`implements SessionSimulation` e falhou ao localizar o marcador. Foi corrigido
somente no script; a nova verificação passou integralmente. Isso não foi falha
Java, de teste ou de gameplay e não exigiu mudança de produção.


## Exclusões gráficas separadas

57 casos excluídos, distribuídos abaixo; os métodos e motivos estão no JSON final.

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
