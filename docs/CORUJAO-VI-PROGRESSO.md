# Corujão VI — progresso verificado

Branch `beco`, HEAD `ad66caed14e3f94bc369f7edd9861900d5a68636`, sem troca de branch/commit/publicação. As mudanças preexistentes foram preservadas.

| TASK | Estado | Evidência principal |
| --- | --- | --- |
| 181–186 | Conclusões anteriores preservadas; 186 corrigida | [Relatório 186](TASK-186-RELATORIO-FINAL.md) |
| 187 | VALIDADA COM RESSALVAS | [Backpressure](TASK-187-RELATORIO-FINAL.md) e [adendo 190](TASK-187-ADENDO-TASK-190.md) |
| 188 | VALIDADA APÓS CORREÇÃO | [Matriz/admissão](TASK-188-RELATORIO-FINAL.md) |
| 189 | VALIDADA COM RESSALVAS | [Observabilidade](TASK-189-RELATORIO-FINAL.md) |
| 190 | VALIDADA COM RESSALVAS, após correção | [Soak](TASK-190-RELATORIO-FINAL.md) |
| 191 | VALIDADA APÓS CORREÇÃO | [Lifecycle](TASK-191-RELATORIO-FINAL.md) |
| 192 | VALIDADA COM RESSALVAS | [Ameaças/Gate C](TASK-192-RELATORIO-FINAL.md) |
| 193 | VALIDADA; ADR aprovado expressamente | [Contrato](TASK-193-CONTRATO-AUDITORIA.md) e [ADR aprovado](ADR-193-PERSISTENCIA-SINTETICA.md) |
| 194 | VALIDADA COM RESSALVAS | [CAS/backup/retry](TASK-194-RELATORIO-FINAL.md) |
| 195 | VALIDADA COM RESSALVAS | [Posse sintética](TASK-195-RELATORIO-FINAL.md) |
| 196 | VALIDADA COM RESSALVAS; Gate D atendido | [Retomada sintética](TASK-196-RELATORIO-FINAL.md), Java gráfico 610/610 e Python 33/33 |
| 197 | VALIDADA | [Assets](TASK-197-RELATORIO-FINAL.md) |
| 198 | VALIDADA COM RESSALVAS | [Pacotes novos verificados](TASK-198-RELATORIO-FINAL.md) |
| 199 | VALIDADA COM RESSALVAS | [Studio/Preview e jogo reais](TASK-199-RELATORIO-FINAL.md) |
| 200 | VALIDADA COM RESSALVAS | [Consolidação](TASK-200-RELATORIO-FINAL.md): 613 Java gráficos, 33 Python e 31.629 hashes |

Gates B e C atendidos nas condições de laboratório documentadas. Regressão final do módulo III: 598 Java gráficos aprovados; headless 588 aprovados e dez abortos GUI; 33 Python aprovados. Rehash integral de 31.629 arquivos protegidos sem divergências. Radmin físico continua pendente, sem equivalência com localhost.

Sessão de evidências: `.task-checkpoints/corujao-vi-session-20261025T221534Z-be629798/`; índice `progress.jsonl` e ponteiros `task-<N>.txt` identificam checkpoints novos por tarefa. Tentativas anteriores, falhas e abortamentos ambientais permanecem nos diretórios originais. Os relatórios por tarefa detalham comandos, cwd, duração, exit codes, contagens, fontes, hashes e limites.

A TASK-193 caracterizou o protótipo em diretórios sintéticos novos, sem alterar produção. O gate “ADR aprovado” exige decisão específica sobre o contrato preparado; não foi inferida aprovação da autorização geral de continuidade. Próxima ação e restrições em [CORUJAO-VI-CONTINUIDADE-193.md](CORUJAO-VI-CONTINUIDADE-193.md).


Decisão posterior: ADR-193 aprovado formalmente pelo responsável; TASK-193 validada. Implementação 194–196 autorizada exclusivamente em laboratório sintético local, sujeita às provas técnicas e aos demais gates.


Encerramento: TASK-200 concluída; Gates D/E atendidos no escopo local e sintético. Radmin físico, política do atualizador e demais ressalvas em CORUJAO-VI-PENDENCIAS.md. Nenhuma TASK-201, commit, push ou publicação. ADR-193 aprovado e decisão preservada; não há aprovação pendente para este bloco executado.
