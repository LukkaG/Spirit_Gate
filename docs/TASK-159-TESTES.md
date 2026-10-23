# TASK-159 — Testes

## Resultado final recompilado

| Execução | Executados | Aprovados | Abortados | Falhos | Ignorados |
| --- | ---: | ---: | ---: | ---: | ---: |
| Java headless | 465 | 457 | 8 | 0 | 0 |
| Java gráfico integral | 465 | 465 | 0 | 0 | 0 |
| Python unittest | 30 | 30 | 0 | 0 | 0 |

Classes finais: `server/target/local-tests/run-_otplo06`. Logs `java-tests.log` e `full-gui-tests.log` no checkpoint. Os oito abortados ambientais são seis StudioUsabilityTest, um StudioGestureSafetyTest e um QuestBuilderTest; todos passaram na execução gráfica, sem remover suas pré-condições.

Executáveis rc2: servidor e dois clientes nativos em processos separados; dois jogadores, duas aparências, raças/cabelos distintos e códigos de saída `[0,0,0]`. Evidências em `native-smoke-final/result.json`, PNGs e sidecars. Studio rc2 abriu e fechou normalmente; runtime integrado passou `--verify-install` em `studio-smoke-final/`.

As primeiras tentativas estão preservadas em `native-smoke/`, `native-python/`, `java-native-debug/` e `native-smoke-rc2/`. A primeira revelou encerramento durante SwingWorker sem janela inicial; corrigido mantendo janela de conexão. A tentativa PowerShell seguinte capturou corretamente os clientes, mas não obteve um ExitCode utilizável. O harness final usa subprocess com espera e códigos explícitos; o wrapper PowerShell delega a ele. Não são reportadas como aprovações as tentativas incompletas.

486 arquivos originais de assets conferidos por SHA-256, todos intactos. HEAD permanece o da auditoria. Contagem e evidência em `source-inventory.json`. Capturas printAll não homologam monitor, DPI ou aprovação artística.

Compilação nova via `python tools/run-local-tests.py`, --release 17.

JUnit: 465 executed, 457 passed, 8 aborted, 0 failures, 0 skipped

Studio gráfico: Não executado nesta tarefa.

Logs: `.task-checkpoints/TASK-159/corujao-iv/`. Os abortados headless continuam classificados como dependentes de ambiente.
