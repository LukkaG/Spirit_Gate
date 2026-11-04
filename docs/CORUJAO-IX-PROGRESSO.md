# Corujão IX — progresso

Atualização: 03/11/2026. Estado baseado nesta execução, não nos relatórios históricos. Checkpoints 223–225 preservados.

| TASK | Estado | Evidência / condição |
|---|---|---|
| 223 | Encerramento técnico aprovado pelo responsável | Aprovação recebida na autorização de continuidade da TASK-224. |
| 224 | Implementação local entregue para revisão | Política contextual, prévias e feedback com dados existentes, vinte casos documentados, capturas e regressões. Sem alteração de protocolo ou combate. |
| 225 | Auditoria/correção entregues; gate A-04 pendente | Resultado original preservado; regressões de progressão/missão saneadas no adendo R1. A-03 e sua correção de limpeza preservados; A-04 não foi alterado em produção. |
| 225-R1 | Entregue no escopo autorizado; regressões saneadas | Progressão/missão com morte TCP real, asserts ampliados e piso de produção preservado; 12 casos adicionais de A-04. Agregado final: 999 aprovados, zero falhas/erros/skips remanescentes. ADR proposta, sem implementar A-04; gate separado e bloqueante. |
| 225-R2 | Entregue no escopo de laboratório; integração definitiva não ativada | 14 casos novos de prazo/saída, dois clientes TCP reais com observador 6/7, fila antiga, spawn e readmissão. Agregado final: 1.013 aprovados, zero falhas/erros/skips remanescentes; complemento Swing com 106 execuções aprovadas. Nenhum código principal alterado; A-04 continua bloqueante. |
| 225-R3 | Arquitetura e protótipo local entregues; ativação pendente | 20 casos novos de recuperação sintética, posse/geração/expiração, consumo único, concorrência e recargas. Agregado final: 1.033 aprovados, zero falhas/erros/skips remanescentes; complemento gráfico com 106 execuções aprovadas. B recomendada com gates; sem recuperação TCP ou importação no runtime. |
| 226 | Não iniciada | Dependente do plano/gates anteriores. |
| 227 | Não iniciada | Inclui análise do risco condicional A-04. |
| 228 | Não iniciada | Sem mudanças de suporte nesta auditoria. |
| 229 | Não iniciada | Sem arte nova ou mudanças de animação. |
| 230 | Não iniciada | Tooltips atuais foram auditados, não alterados. |
| 231 | Não iniciada | Nenhum valor sugerido ou aplicado; nenhuma métrica de balanceamento nova. |
| 232 | Não iniciada | Sem integração de melhorias futuras ou build privada. |

TASK-224: suíte geral de 939 casos, sem falhas/erros, 54 skips headless. Complemento gráfico: 106 execuções aprovadas, além de rodadas específicas/capturas adicionais. Agregado final por classe em `verification.json` no checkpoint da TASK-224. As ocorrências intermediárias de foco/fixtures estão documentadas em TASK-224-TESTES.md. Nenhum teste Radmin físico foi realizado.

TASK-225, execução original: **987 casos, duas falhas, zero erros e 54 skips headless**. Os 45 casos novos passaram. O checkout já tinha uma classe adicional `EnemyDurabilityTest` (três casos) e piso de HP 10.000, diante do relatório histórico de 939 casos da TASK-224. As falhas de `Task208ProgressionTest` e `Task210QuestJourneyTest` se repetiram isoladamente: seis ataques não matam o alvo de 10.000 HP. Não houve mudança de HP/XP/missão naquela entrega. Complemento Swing e ocorrências em [TASK-225-TESTES.md](TASK-225-TESTES.md).

Complemento gráfico original da TASK-225: 106 execuções, uma falha de foco AWT; repetição isolada aprovada sem mudança de código. Agregado daquela entrega: **987 casos, 985 aprovados, duas falhas, zero erros e zero skips remanescentes**. Foram preservados 71 clips/152 PNGs e restauradas 81 capturas históricas regeneradas pelos testes. Apenas um arquivo de produção recebeu alteração localizada naquela etapa.

TASK-225-R1: as mesmas seis verificações focalizadas passaram após a configuração explícita de encontro de catálogo, sem XP/READY/morte manual. Os três casos de durabilidade continuam aprovados e exigem HP 10.000 no caminho de produção. A primeira suíte geral R1 teve três erros de classe de teste ausente (`VisualPackageTest$1`); a reconstrução dos artefatos de teste, sem mudar seu fonte ou apagar arquivos, permitiu repetir a suíte com **999 casos, zero falhas/erros e 54 skips headless**. Nenhuma fonte de produção foi alterada no adendo. Rodadas e complemento gráfico em [TASK-225-R1-TESTES.md](TASK-225-R1-TESTES.md).

Complemento Swing R1: **106 execuções, zero falhas/erros/skips**. O caso final de missão/moedas foi repetido e aprovado. Agregado por classe: **999 casos aprovados em 259 classes, zero falhas/erros/skips remanescentes**, sem somar repetições. Os 291 arquivos protegidos de produção e 152 PNGs permaneceram íntegros; 82 capturas históricas e o TXT da investigação original foram arquivados/restaurados. O resultado histórico de 225 continua preservado.

A-03: desvio intermediário medido de 28,676 unidades; efeito terminal coincide com o contato, erro raster 0,068. A-04: imunidade persistiu aos 180 ticks em câmara artificial sem apoio; quatro cortes únicos e reentrada quando o apoio foi liberado. Nenhuma regra de imunidade/materialização foi alterada. [Relatório](TASK-225-RELATORIO-FINAL.md), [matriz](TASK-225-MATRIZ-COLISOES.md) e [investigação A-04](TASK-225-A04-EVIDENCIAS.md).

O impedimento de regressão de fixtures foi saneado no recorte da R1, mantendo as limitações de homologação humana. Gate B permanece bloqueado pela decisão/implementação pendente do A-04; suíte verde não encerra essa condição. A [ADR proposta](TASK-225-ADR-A04-PROPOSTA.md) recomenda A condicionada a retirada coordenada e aceite explícito das consequências da readmissão efêmera. Gates posteriores e TASK-226 não foram iniciados. Sem commit, push, release, publicação, build privada ou modificação de saves.

TASK-225-R2: autorização restrita ao laboratório. O modelo isolado tenta materializar antes de retirar aos 92, aceita apoio nos ticks 91/92 e não repete remoção aos 93. O TCP aciona uma causa equivalente durante a fase, reusa `leave`/`disconnectRequested`/`closePeer` e não pede snapshots privados após a retirada. Não há timeout de 92 no runtime. A perda de progresso/recargas foi aceita somente como premissa experimental do ensaio; ativação requer aceite explícito da opção A ou recuperação de personagem (B). [Testes](TASK-225-R2-TESTES.md), [relatório](TASK-225-R2-RELATORIO-FINAL.md) e [plano não aplicado](TASK-225-R2-PLANO-INTEGRACAO-A04.md). TASK-225 não está plenamente homologada; TASK-226 permanece não iniciada.

TASK-225-R3, execução de 03–04/11/2026: a [ADR de continuidade](TASK-225-R3-ADR-CONTINUIDADE.md) recomenda B condicionada a captura/importação completas e posse/protocolo aprovados. Protótipo apenas de teste preserva snapshot/recargas em memória e descarta fase/transitórios; não sobrevive ao restart nem recupera personagem por TCP. Os testes R2 foram repetidos sem mudar seu significado. [Testes R3](TASK-225-R3-TESTES.md) e [relatório](TASK-225-R3-RELATORIO-FINAL.md). Autorização direta do usuário permite commit/push do recorte R3, sem ativar A-04, publicar conteúdo ou iniciar TASK-226.
