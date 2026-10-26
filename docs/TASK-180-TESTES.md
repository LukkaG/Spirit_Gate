# TASK-180 — testes

Baseline: 532 Java, sem falhas (523 aprovados e nove Swing abortados em headless); 33 Python aprovados. Atualizador existente: 32 checks PowerShell aprovados, sem alteração do mecanismo.

WorldRevisionTest adiciona testes atravessando TCP e o backend RpgSessionWorld real:

| Área | Casos |
|---|---|
| Fluxo principal | dois V4, preparo, ativação, perfil, movimento, rollback e histórico |
| Compatibilidade | V1/V2/V3 bloqueiam Save sem kick; portal V3 no baseline; ingresso antigo recusado após mapa autorado |
| Integridade | determinismo, hash incorreto, descritor/frame truncado, excesso, ordem, traversal e wrappers restritos |
| Segurança | Convidado sem capacidade, socket público sem publicação, comandos atrasados com hash antigo e snapshot antigo apos rollback ao mesmo hash |
| Geometria | spawn/parede sobre jogador, inimigo em área recém-solidificada, validações do perfil e portais |
| Transação | staging/disco injetado, falha após ativação, base antiga, duas publicações simultâneas |
| Transporte | disconnect após READY e após ativação, novo join durante barreira, ACK errado/atrasado/duplicado |
| Recuperação | timeout de preparo e de ativação, encerramento do servidor, reinício confirmado e journal interrompido |
| Assets | servidor revalida catálogo preinstalado; cliente sem recurso rejeita, sem placeholder sincronizado |

Os peers manuais de falha dos testes servem para omitir/forjar ACKs e verificar a máquina. Não substituem o teste gráfico integrado: Task180PublishingProbe usa o renderer e as janelas reais de produção.

Regressões completas incluem offline, quatro raças, domínios, progressão, saves V1/V2, coop, protocolos anteriores, PvE, NPCs/portais, cosméticos, reconexão, Quest Builder, World Studio, Collision Lab e TASK-177/178/179. Os hashes de saves, sprites originais e outras entradas protegidas são registrados no checkpoint.

Comandos: `python tools/run-local-tests.py`; GUI real com `python tools/task180-verify.py <run>`; Python com `python -m unittest discover -s tools -p 'test_*.py'`; atualizador com `tools/updater/Test-Atualizador.ps1 -EvidenceDir <checkpoint>`.

Contagens finais, logs, métricas, pacotes e hashes estão em `.task-checkpoints/TASK-180/live-publishing/validation-summary.json`. Falhas descobertas durante o desenvolvimento não são omitidas dos logs intermediários; a conclusão depende da execução final sem regressões essenciais.
