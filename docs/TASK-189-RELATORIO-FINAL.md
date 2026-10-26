# CORUJÃO VI — TASK-189

Estado: **VALIDADA COM RESSALVAS**. Branch beco, HEAD ad66caed14e3f94bc369f7edd9861900d5a68636. Gate B registrado na 188; transição 189 -> 190 automática.

Reutilizada instrumentação local opt-in. Acrescentados JSON numéricos limitados de rede/publicação sem hashes, nomes ou dados pessoais; filas agregadas amostradas no owner; contador de falhas de leitura/handshake; índice do anel limitado a 0..255 além das 256 amostras já existentes. Sem mudanças nos frames ou na máquina de publicação/admissão.

Específico: 3/3 PASS. Multiplayer: 128/128 PASS. Reexecução específica com JFR: 3/3 PASS. JSON validado com parser Python estrito (oito eventos na primeira coleta), sem NaN/Infinity ou campos pessoais. JFR local de nove segundos coletado e lido por jfr summary, limitado a 16 MiB; eventos de ambiente/propriedades/JVM/processos desabilitados e contagem zero confirmada.

Microensaio informativo sem rede: cinco pares intercalados de 5.000 frames após 2.000 de warmup; na coleta específica normal, medianas 161,320 ns/frame desabilitado e 289,960 ns/frame habilitado. Não é benchmark de jogo e não demonstra impacto imperceptível ou melhoria. Paint foi medido no renderer real offscreen, não em monitor remoto; READY/ACTIVE foram medidos sobre publicação real com dois clientes TCP. Janelas de percentis são das últimas 256 amostras.

Documentação: docs/TASK-189-OBSERVABILIDADE.md. Diffs, fontes compilados, logs/exit codes/durações, configuração JFC, gravação e resumo JFR arquivados no checkpoint novo. Radmin físico e perfil longo permanecem pendentes para os passos seguintes; nenhuma dependência, instalação, rede, privilégio administrativo ou persistência pessoal foi alterado. Ressalva: o custo sintético medido não autoriza alegar ausência de impacto em toda carga.


Evidências: `.task-checkpoints/TASK-189/corujao-vi/run-20261025T223352Z-4cc94a94`.

Integridade: 31629 entradas verificadas; rehash nesta etapa: 0; divergências: 0. Método e hashes em protected-before/after.json.

Fontes alterados nesta task: server/src/main/java/com/projetok/multiplayer/LocalSimulationServer.java, server/src/main/java/com/projetok/multiplayer/NetworkDiagnostics.java, server/src/main/java/com/projetok/multiplayer/RevisionCoordinator.java, server/src/test/java/com/projetok/multiplayer/Task189DiagnosticsTest.java.
