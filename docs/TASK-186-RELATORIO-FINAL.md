# CORUJÃO VI — TASK-186

Estado: **VALIDADA APÓS CORREÇÃO** em 25/10/2026. Bloqueio técnico específico resolvido no código real.

Branch `beco`; HEAD `ad66caed14e3f94bc369f7edd9861900d5a68636`, preservados. Alterações preexistentes mantidas. Esta execução ficou restrita à correção autorizada; TASK-187 a TASK-200 permanecem não iniciadas.

## Defeito e alteração aplicada

A confirmação final em `LivePublishing.java:54` comparava somente o hash. A árvore anterior foi recompilada integralmente e reproduziu o defeito: `accepted=false`, `Result.active=true`, servidor em `VERIFYING` e cliente bloqueado. O teste específico apresentou três aprovações e uma falha esperada antes do patch.

Foi aplicada ao fonte real a condição mínima da proposta: exigir `confirmed.accepted()` e hash correspondente antes de retornar sucesso. Uma recusa ou hash incorreto lança a falha pelo caminho existente de abort/rollback. Não houve refatoração da transação nem mudança de contratos de transporte, saves, combate, progressão ou acesso administrativo.

A coerência da transação continua sob responsabilidade do owner em `RevisionCoordinator.confirmReserved()`: exige transação existente, fase `VERIFYING` e todos os clientes `ACTIVE`; somente então confirma, muda para `COMMITTED` e libera a simulação. O teste adicional comprovou recusa em `IDLE` e `READY`. Essa validação existente não foi contornada nem substituída por uma checagem de status fora do owner.

## Prova de falha segura

O teste da TASK-186 foi fortalecido, sem suavizar expectativas. Tanto uma confirmação negativa com hash correto quanto uma confirmação aceita com hash incorreto agora demonstram:

- Exceção de confirmação, sem resultado falso de publicação bem-sucedida.
- Transação em `ABORTED`, revisão anterior no servidor e no cliente.
- Ponteiro da primeira publicação restaurado à ausência anterior; journal pendente removido.
- Candidata marcada como abortada e excluída do histórico confirmado.
- Cliente liberado, aceitando o hash anterior e recusando o hash candidato.
- Comando de movimento efetivamente processado após a recuperação, mantendo a revisão anterior nos dois participantes.

A injeção é local na interface Java `PublicationTarget`, com servidor RPG e cliente TCP reais. Não constitui demonstração de exploração por um socket de jogador. `AFTER_STAGE`, `AFTER_ACTIVATION` e `BEFORE_POINTER` também preservaram a revisão anterior. A suíte multiplayer reexecutou desconexões em BEGIN/READY/ACTIVATE/RESUME e reconexões após abort e após commit confirmado.

## Resultados atuais

| Verificação | Resultado |
| --- | --- |
| Reprodução anterior, compilação integral nova | 4 executados: 3 aprovados, 1 falha esperada; exit 1 |
| TASK-186 corrigida | 6/6 aprovados; zero falhas/abortos/skips |
| Suíte multiplayer | 119/119 aprovados; zero falhas/abortos/skips |
| Java completa headless | 578 executados: 569 aprovados, 9 abortados por premissa GUI; zero falhas/skips |
| Java completa gráfica | 578/578 aprovados; zero falhas/abortos/skips |
| Python com classes novas | 33 aprovados; zero falhas |
| SHA-256 protegido antes e depois | 31.629 arquivos conferidos em cada comparação; zero divergências |

Foram compilados 236 fontes de produção e 182 arquivos de teste para Java 17 antes e depois da alteração. As classes corrigidas foram geradas exclusivamente a partir de uma cópia nova e conferida dos fontes reais; nenhuma classe do ambiente da proposta isolada foi reutilizada. O cache JUnit já existente foi usado sem download ou dependência nova. Java/javac 17.0.20.1, Python 3.14.5 e Maven 3.10.0 tiveram versões registradas; as suítes usaram o runner local cacheado.

As execuções Java tiveram cwd isolado para manter capturas e arquivos relativos fora dos documentos/checkpoints históricos. Python usou o `LocalArenaMap` dessa compilação corrigida. Logs originais e da proposta anterior foram preservados e tiveram seus hashes conferidos novamente.

## Inventário e evidências

Somente dois arquivos em `server/src` diferem da entrada desta correção:

- `server/src/main/java/com/projetok/studio/LivePublishing.java`: condição e mensagem na confirmação final.
- `server/src/test/java/com/projetok/multiplayer/Task186CommitGateTest.java`: verificações de recusa, hash, estado, ponteiro e gameplay recuperado.

SHA-256 de `LivePublishing.java`: antes `364cf774733782f755d2d71e499499f9972447cd0473ac2ee612cdfa0dc3fce4`; depois `70215e54170740cb4b9df140f19463b1445f3561411ebacd6dc0ec7964bfea6d`.

Checkpoint posterior: [.task-checkpoints/TASK-186/corujao-vi/correction-20261025T215004Z-5efe4e3f](../.task-checkpoints/TASK-186/corujao-vi/correction-20261025T215004Z-5efe4e3f/). Contém plano, before/after, manifestos dos fontes, inventários protegidos, patches efetivamente aplicados, comandos/cwd/duração/exit code, logs de todas as suítes e bytecode da compilação corrigida. O relatório anterior foi preservado em `original-report.md`; as evidências originais continuam em `run-20261025T180152Z-c81cf638`.

Detalhamento: [TASK-186-TESTES.md](TASK-186-TESTES.md). Progresso: [CORUJAO-VI-CONTINUIDADE-186.md](CORUJAO-VI-CONTINUIDADE-186.md).

## Limites e continuidade

Nenhuma verificação essencial falhou após a correção. Os nove abortos headless são ambientais e os respectivos casos passaram na execução gráfica. Homologação física Radmin, revisão humana das propostas da TASK-181 e impedimentos anteriores de Maven/atualizador permanecem pendentes; não são declarados resolvidos por estes testes.

Não houve instalação, alteração de rede/política, commit, push, publicação ou atualização de canal. O encerramento desta execução conclui exclusivamente a correção autorizada da TASK-186; a continuidade da TASK-187 deve respeitar o escopo de autorização e os demais gates do Plano Diretor.
