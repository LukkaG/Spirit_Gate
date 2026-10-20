# TASK-077 — Testes e evidências

Data: 19/10/2026. Resultado da suíte: 297 Java + 25 Python, sem falhas. Não foi alterado código, teste ou configuração para obter aprovação.

## Comandos e execução

1. Na raiz real: `python tools/qualidade.py --offline --task retomada-expansao`, exit 0. Executa Maven real, offline, com repositório `.maven-repository`, `-f server/pom.xml clean test`; depois `python -m unittest discover -s tools -p test_*.py -v`. O módulo correto — server, e não a raiz. Não foi repetida a mesma suíte Maven sem necessidade.
2. Continuidade: `python tools/consolidar_expansao.py reports` foi adaptado para importação do módulo original e chamada `reports()` com `DOCS=.task-checkpoints/TASK-077/consolidacao`, contendo cópias dos JSONs históricos. `ROOT` continua no checkout atual. Exit 0; 40 tarefas, 102 alterações, referência histórica de 297 Java e 25 Python. A adaptação — necessária porque a execução literal sobrescreveria arquivos anteriores, proibido pela tarefa. Nenhum relatório histórico foi sobrescrito; os outputs isolados não são uma nova homologação das TASK-037..076.
3. Sonda adicional isolada: `java --class-path server/target/classes .task-checkpoints/TASK-077/AuditProbe.java .task-checkpoints/TASK-077/temporary-saves`. Segunda execução exit 0; a primeira falhou na compilação porque Set-Content Windows incluiu BOM UTF-8 e a codificação do compilador não o aceitou. Corrigida somente a evidência nova para ASCII e repetida; ambos os logs preservados. Nenhum arquivo de produção foi corrigido.

## Totais reais

| Grupo | Testes | Falhas | Erros | Ignorados |
| --- | ---: | ---: | ---: | ---: |
| Java (83 suítes JUnit) | 297 | 0 | 0 | 0 |
| Python unittest | 25 | 0 | 0 | 0 |

Java: domínio 125, prototype 120 (inclui prototype.rpg), rpg 37, multiplayer 15. Nenhuma divergência numérica da referência 297/25. Os XMLs desta execução foram copiados antes de qualquer outra execução de build para `junit/`, e `test-summary.json` contém nomes de todos os casos. Os números da sonda adicional não foram somados — suíte.

## Cobertura de salvamento (dados temporários)

| Requisito | Evidência automatizada |
| --- | --- |
| Criação de personagem | CharacterIdentityTest, CharacterIdentity/CharacterSaves e AuditProbe com dois UUIDs |
| Salvar progresso | LocalSaveStoreTest e OfflineCycleTest: gravação em @TempDir |
| Retomar após reinício lógico | restore(load()), igualdade de estado e atributos; sonda recarrega dois arquivos; sem alegar reinício físico do Windows |
| Inventário/equipamentos | InventoryServiceTest, EquipmentRpgTest, OfflineCycleTest |
| Missões | QuestLifecycleTest, QuestRewardsTest, OfflineCycleTest e DungeonCycleTest |
| Inimigos | EnemyRpgTest, EnemyAiTest, RpgFinalReliabilityTest (continuidade determinística) |
| Backup | LocalSaveStoreTest e CharacterSavesTest |
| Corrupção | CorruptBackupProtectionTest PASS: arquivo semanticamente corrompido não substitui backup bom; LocalSaveStoreTest e RpgFinalReliabilityTest |
| Isolamento entre personagens | AuditProbe PASS: dois arquivos UUID, inventários e estados independentes |
| Sem perda/duplicação nos cenários cobertos | InventoryServiceTest, TradeTest, QuestRewardsTest, LootServiceTest e EquipmentRpgTest |

RpgRuntimeSaveTest confirma captura de comandos anteriores e pausa; stress de RpgFinalReliabilityTest cobre 20.000 ticks e recargas periódicas. CorruptBackupProtectionTest continua válido sem alteração. Não foram usados saves pessoais. A sonda guarda apenas personagens sintéticos no checkpoint; os testes JUnit usam @TempDir.

## Coop e arena

LocalServerTest, TwoClientsTest, TwoProcessTest, AuthoritativeCombatTest, CooperativeEncounterTest, NetworkFightIntegrationTest, NetworkRobustnessTest, ProtocolTest e RemotePresentationTest passaram. Cobrem loopback, dois IDs distintos, movimento/snapshots, rejeição de sequências antigas, reconexão com novo ID, combate e dano reais por TCP, chefe morto, recompensa única de 20 moedas por participante e encerramento. O stdout JUnit contém `COOP_TCP_REAL bossHp=0 rewards=20,20`. A robustez inclui mensagens inválidas, conexão silenciosa e limites de fila/conexões; não representa teste de carga público nem autenticação.

Arena: suites de sequência de combate/domínio, colisão, dash, Cryostasis, foco/controles e renderer passaram. Não foi aberta janela para homologação manual nem refeito o smoke dos pacotes; as evidências anteriores de UI são históricas. Dois processos na mesma máquina não homologam LAN física. TASK-071 permanece pendente; TASK-074 sem transições online.

## Evidências reproduzíveis

Tudo sob `.task-checkpoints/TASK-077/`: logs `TASK-retomada-expansao-java.log`, `-python.log`, `-qualidade.json`, `-testes.txt`, versões; `junit/TEST-*.xml`; `test-summary.json`; `consolidacao.log` e saída isolada; `AuditProbe.java`, `probe.log` (primeira falha) e `probe-second.log` (sucesso da sonda, incluindo reprodução do risco P077-01); hashes e comparação histórica. O programa da sonda termina com exit 0 quando o risco esperado — reproduzido: isso não significa que o ciclo de vida foi corrigido.

O build gera target/caches; esses diretórios não são produção e não entram no manifesto de preservação. Nenhum bloqueio indispensável de execução permaneceu nesta máquina.
