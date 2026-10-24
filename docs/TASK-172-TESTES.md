# TASK-172 — testes

Seletores, Idle/Walk, pivôs, fallback e aparência remota preservados. Adaptador só aceita associações explícitas validadas; nenhum vínculo inferido.

Evidência reproduzível: `python tools/run-local-tests.py`; testes de rede em `server/src/test/java/com/projetok/multiplayer/SharedRpgCombatTest.java` e suítes existentes. Repositório sintético: `SyntheticOnlineRepositoryTest`. Apresentação: `CorujaoFivePresentationTest`. Política de interface: `RadminAccessTest`.

Resultados consolidados e logs: [CORUJAO-V-HOMOLOGACAO.md](CORUJAO-V-HOMOLOGACAO.md), `.task-checkpoints/TASK-174/corujao-v/`. Evidências de combate em `.task-checkpoints/TASK-175/corujao-v/demo-02/`. Não são execuções isoladas e independentes de cada tarefa.

Limite desta tarefa: Infraestrutura preservada; associações artísticas pendentes. Consulte as pendências específicas em [CORUJAO-V-PENDENCIAS.md](CORUJAO-V-PENDENCIAS.md).
