# CORUJÃO VI — TASK-192

Estado: **VALIDADA COM RESSALVAS**. Branch beco, HEAD ad66caed14e3f94bc369f7edd9861900d5a68636. Transição 191 -> 192; Gate C atendido antes de abrir 193.

Produzida matriz ameaça/mitigação/teste/risco residual em docs/TASK-192-MODELO-AMEACAS.md, com plano futuro separado de identidade/TLS. Nenhuma proteção de produção, autenticação de contas ou homologação física foi declarada. Nenhum endpoint, credencial, dependência ou política nova.

Novo teste cobre flood finito (121 mensagens enviadas até isolamento na coleta final, pico de comandos 13 <=64, peer normal continua), 600 mutações binárias com seed 192 (418 recusadas/182 ainda válidas e reencodadas), tamanhos extremos e quorum por socket: A pronto com ACKs duplicados não substitui B retido; BEGIN do cliente não inicia publicação. Demais negativos de traversal, guest, assets, geometria, timeout, wrappers e protocolos foram reexecutados na regressão completa.

Resultados finais: específico 3/3; Java headless 598 executados, 588 aprovados, dez abortados por premissa GUI, zero falhas/skips; Java gráfico 598/598, zero falhas/abortos/skips; Python 33/33 com LocalArenaMap recém-compilado. Rehash SHA-256 integral no encerramento do módulo: 31.629 arquivos protegidos, zero divergências. Fontes de protocolo, save, progressão e combate preservados em relação à retomada; o delta do módulo registra os arquivos efetivamente alterados.

A execução anterior run-20261025T232038Z-7c20a7c6 também passou. Foi preservada e substituída por uma fixture mais determinística: A confirma seu READY normalmente após END e o teste aguarda exatamente um READY antes de duplicar ACK. Isso elimina corrida de ACK precoce na fixture, mantendo a exigência de quorum. Nenhuma asserção de segurança foi relaxada nem teste desabilitado.

Gate C: diagnóstico local/JFR e custo limitado registrados na 189; dois períodos comparáveis totalizando 300,09 s na 190, com ressalvas de GC/paint offscreen; lifecycle/process crash/journal e GUI reais na 191; ameaças e regressão consolidada nesta 192. Os defeitos reproduzidos foram corrigidos com logs antes/depois, sem ocultar tentativas. Radmin físico, atualizador PowerShell e autorização de ADR para persistência permanecem pendentes em seus escopos próprios. Sem commit, push, publicação ou integração com contas/saves pessoais.


Evidências: `.task-checkpoints/TASK-192/corujao-vi/run-20261025T232547Z-87c81ea2`.

Integridade: 31629 entradas verificadas; rehash nesta etapa: 0; divergências: 0. Método e hashes em protected-before/after.json.

Fontes alterados nesta execução (before/after): nenhum.
