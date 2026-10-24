# TASK-160 — testes

Auditoria de SessionSimulation, RpgSessionWorld, RpgWorld, EnemyService, CombatService, MovementService, RpgCombatEvent, LocalSimulationServer e Protocol.

Evidência reproduzível: `python tools/run-local-tests.py`; testes de rede em `server/src/test/java/com/projetok/multiplayer/SharedRpgCombatTest.java` e suítes existentes. Repositório sintético: `SyntheticOnlineRepositoryTest`. Apresentação: `CorujaoFivePresentationTest`. Política de interface: `RadminAccessTest`.

Resultados consolidados e logs: [CORUJAO-V-HOMOLOGACAO.md](CORUJAO-V-HOMOLOGACAO.md), `.task-checkpoints/TASK-174/corujao-v/`. Evidências de combate em `.task-checkpoints/TASK-175/corujao-v/demo-02/`. Não são execuções isoladas e independentes de cada tarefa.

Limite desta tarefa: Arquitetura entregue. Consulte as pendências específicas em [CORUJAO-V-PENDENCIAS.md](CORUJAO-V-PENDENCIAS.md).
