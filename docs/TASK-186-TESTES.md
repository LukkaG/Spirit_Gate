# TASK-186 — testes da correção controlada

Estado: **VALIDADA APÓS CORREÇÃO**. Checkpoint novo: `.task-checkpoints/TASK-186/corujao-vi/correction-20261025T215004Z-5efe4e3f/`.

| Etapa | Duração | Exit | Contagens / estado |
| --- | --- | --- | --- |
| `before-specific` | 20,696 s | 1 | FAIL esperado antes do patch: 4 executados, 3 aprovados, 1 falha |
| `corrected-specific` | 21,303 s | 0 | PASS: 6 aprovados |
| `corrected-multiplayer` | 44,231 s | 0 | PASS: 119 aprovados |
| `corrected-java-headless` | 63,890 s | 0 | 569 aprovados, 9 ABORTADOS AMBIENTAIS por premissa GUI, 0 falhas |
| `corrected-java-gui` | 65,289 s | 0 | PASS: 578 aprovados, 0 abortos/falhas/skips |
| `corrected-python` | 4,125 s | 0 | PASS: 33 aprovados |

As durações das etapas específicas incluem compilação integral de 236 fontes e 182 arquivos de teste para `--release 17`. Cada etapa possui `<nome>.json` com comando exato, cwd, duração, exit code e instante UTC; `<nome>.log` preserva stdout/stderr. `verify.py` documenta a preparação e a ordem executadas.

Os processos Java diretos gravaram a saída bruta do Windows; alguns trechos usam a codificação de sistema Cp1252. Os logs foram preservados sem transcodificação. As contagens ASCII podem ser lidas com detecção de codificação ou substituição de caracteres inválidos na visualização; os documentos e manifestos são UTF-8.

Execuções do harness, na raiz do repositório:

```powershell
python .task-checkpoints/TASK-186/corujao-vi/correction-20261025T215004Z-5efe4e3f/verify.py red
# Aplicação do patch mínimo ao fonte real e fortalecimento do teste.
python .task-checkpoints/TASK-186/corujao-vi/correction-20261025T215004Z-5efe4e3f/verify.py corrected
```

Os caminhos acima identificam esta execução e não devem ser reutilizados para sobrescrever logs. Para reproduzir em outra sessão, criar um novo diretório de checkpoint e conferir novamente os efeitos dos runners. O harness usa abertura exclusiva para os logs.

O runner local foi carregado de uma cópia nova. Na etapa específica, apenas o seletor de descoberta foi adaptado para `Task186CommitGateTest`; nenhuma asserção foi retirada. O runner por pacote executou multiplayer e, depois, todo `com.projetok` em headless e com Swing. O classpath dos dois usa somente classes recém-compiladas desta correção e as dependências JUnit cacheadas, sem incluir classes da proposta anterior.

Casos diretamente comprovados:

| Cenário | Resultado observado |
| --- | --- |
| `accepted=false`, hash correspondente | Exceção, ABORTED, revisão antiga, ponteiro ausente como antes, candidata abortada, gameplay recuperado |
| `accepted=true`, hash incorreto | Mesma falha segura e recuperação |
| Confirmação sem transação / ainda em READY | Owner recusa; revisão anterior preservada |
| AFTER_STAGE / AFTER_ACTIVATION / BEFORE_POINTER | Abort/rollback preservam clientes e revisão; sem candidata confirmada |
| Desconexão no ingresso e publicação, por fase | Testes TASK-185 passaram em BEGIN/READY/ACTIVATE/RESUME |
| Reconexão após abort | Revisão anterior consistente, dois jogadores sem duplicação |
| Reconexão após commit confirmado | Candidata confirmada consistente no disco/servidor/clientes |
| Inputs e snapshots com revisão antiga | Negativos históricos reexecutados na suíte multiplayer |

`source-and-history-check.json` comprova que a cópia corrigida corresponde aos fontes reais, que somente os dois fontes autorizados mudaram e que as evidências anteriores conferem. `protected-before.json` e `protected-after.json` registram os 31.629 hashes protegidos, sem divergências. `applied-source-hashes.json` e os dois arquivos `*.applied.patch` registram o diff real.

Python executou descoberta `test_*.py`, direcionando somente `mapas.ROOT` à cópia isolada com `LocalArenaMap` recém-compilado. Fixtures temporárias e negativos foram preservados. Não houve uso de classes antigas como substituto dessa regressão.

Radmin físico e atualizador PowerShell: NÃO EXECUTADOS nesta correção, pendências anteriores preservadas. Não houve alteração da política de execução para executar testes.
