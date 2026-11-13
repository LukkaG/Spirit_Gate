# ORG-02-R2 — Relatório final

## Resultado

Investigação executada em desktop reservado e encerrada no limite predefinido:
quatro casos isolados, classes juntas/invertidas, após HUD/hotkeys e seis
repetições. **Nenhuma falha reproduzida; causa indeterminada e intermitência
ainda não resolvida.** Não há correção comprovada nem atribuição causal ao ambiente.

A matriz teve 44 execuções dos casos investigados e cinco casos de contexto,
todos aprovados. A suíte geral executou 1068 casos,
com 0 falhas, 0 erros e
0 skips. Os dois parâmetros isolados usaram seleção
explícita, com um skip intencional em cada rodada. Não são casos únicos
somáveis entre rodadas. Uma tentativa anterior de seletor descobriu zero testes
e não foi contada como aprovação.

## Alterações

- `server/src/test/java/com/projetok/multiplayer/GuiInputTrace.java`: UTC/PID,
  propriedades antigas/novas de foco, identidade/estado de componentes,
  frame/viewport/listeners, retorno das chamadas e filtro opcional de invocação.
- `GamePanelsIntegrationTest.java`: observa o retorno do pedido de foco existente.
- `Task224CastingGuiTest.java`: observa o retorno do pedido de foco do viewport.
- Quatro documentos ORG-02-R2 em `docs/`; scripts/logs locais em `temp/org02-r2/`.

**Somente instrumentação de testes; nenhum patch corretivo.** Sem novos sleeps,
pedidos de foco, tentativas extras, ActionMap ou alteração de asserções/atalhos.
Produção, 40 skills/combate, protocolo, mobs/IA/colisões, inventário/XP/missões,
PostgreSQL/saves, World Studio/launcher e TASK-225/A-04 não foram editados.

## Evidências e compatibilidade

Coletor `SetWinEventHook` iniciado antes dos testes, UTC/ticks e PID correlacionados
com a EDT. Na matriz, os 110 eventos de perda/desativação com `opposite=null`
ocorreram com frames já não visíveis nem displayable. Esses descartes normais
não explicam a perda anormal da R1. Retorno `false` de pedido de foco também
apareceu em casos verdes; não motivou mudança de comportamento.

Dois `test-compile` offline passaram. Resultados, comandos exatos, ordens,
falhas operacionais e limitações estão em [ORG-02-R2-TESTES.md](ORG-02-R2-TESTES.md).
Análise e limites causais: [ORG-02-R2-CAUSA-RAIZ-GUI.md](ORG-02-R2-CAUSA-RAIZ-GUI.md).
Sem homologação de teclado físico, Radmin ou launcher nativo.

Git inicial preservado. Comparação SHA-256 anterior/posterior aos testes:
nenhum arquivo de produção inventariado alterado, nenhum ausente. O inventário
`prepared-sha256.json` foi feito após instrumentação inicial e não é checkpoint
anterior às edições. As alterações posteriores nesse inventário estão
explicitadas em `hash-comparison-final.json`.
100 capturas regeneradas foram arquivadas e restauradas individualmente;
nenhum descarte global. O coletor nativo foi encerrado no fim da coleta.

`logs/monitor/status.json` estava antigo, sem pendências listadas e com PID
inativo. Os TXT recentes consultados tratam de World Studio e não foram
corrigidos/apagados por estarem fora do escopo. Nenhum commit, push, release,
publicação, instalação ou início da ORG-03.

## Aceite, pendência e reversão

O limite foi cumprido e a evidência foi preservada. **Não declarar o ramo de
aceite de causa demonstrada ou classificação ambiental causal como atendido**:
não houve reprodução suficiente para isso. Não repetir indefinidamente.

A reversão consiste em remover exclusivamente os acréscimos observacionais
da R2 nos três testes, preservando a extensão/anotações da R1 e o trabalho
anterior. Guardar os relatórios e logs; não usar restauração global do Git.
Próximo passo sugerido, dependente de nova solicitação: coletar uma ocorrência
anormal com esta instrumentação e identificar o mecanismo da ativação nativa.
