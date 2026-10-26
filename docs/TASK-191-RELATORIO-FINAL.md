# CORUJÃO VI — TASK-191

Estado: **VALIDADA APÓS CORREÇÃO**. Branch beco, HEAD ad66caed14e3f94bc369f7edd9861900d5a68636. Transição 190 -> 191 -> 192 automaticamente.

Corrigidos dois defeitos reproduzidos no primeiro ensaio (run-20261025T230523Z-d39a8a78, 4 aprovados/1 GUI ambiental/2 falhas): futuro administrativo apenas enfileirado ficava órfão após shutdown; saída de conexão recusada cancelava a reserva do Preview. A fila de RevisionCoordinator conserva o limite 16 e agora associa cada operação ao futuro, serializando enqueue/step/shutdown e concluindo excepcionalmente pendências descartadas, sem executá-las. Preview ignora saída desconhecida e serializa submit/close para evitar enfileiramento depois da drenagem. Protocolo, saves e capacidade editorial não mudaram.

Resultados finais: específico headless 6 aprovados e 1 abortado ambiental GUI, zero falhas; gráfico 7/7 aprovados; multiplayer 135 aprovados e 1 GUI ambiental, zero falhas; Studio 67 aprovados e 8 GUI ambientais, zero falhas. A prova gráfica real fechou WorldStudio/Preview e preservou o Game Server host, na etapa dispose posterior à decisão de descarte.

Uma tentativa gráfica anterior (run-20261025T230908Z-89d55da1) ficou em diálogo legítimo de descarte e foi interrompida com exit 15 após identificação da própria JVM por jcmd e thread dump. Não foi contada como aprovada. WMI negou leitura de processo; jcmd existente permitiu identificar somente o classpath do teste sem novas permissões. A fixture foi corrigida para exercitar dispose, sem alterar proteção de rascunho. Logs/gui-abort/gui-threads e primeira reprodução permaneceram intactos.

Shutdown em WAITING_CLIENTS, READY e VERIFYING retomou somente a revisão confirmada. Processo-filho real confirmou uma revisão, ativou candidata e sofreu halt(23) antes do ponteiro/commit; o pai verificou journal bloqueando startup, recuperação explícita com journal preservado e cliente retornando ao hash confirmado. Não foi teste de falta de energia. Futuros enfileirados e chamadas após close foram verificados, sem respostas pendentes indefinidamente.

Documentação em docs/TASK-191-ENCERRAMENTO-RECUPERACAO.md. Diffs completos de RevisionCoordinator e PreviewServer são comparados aos fontes anteriores às correções e preservados no checkpoint final, embora a última execução só tenha ajustado a fixture gráfica. Não houve nova dependência, gravação em saves pessoais, alteração de rede, commit ou publicação. Radmin físico e durabilidade física permanecem não homologados.


Evidências: `.task-checkpoints/TASK-191/corujao-vi/run-20261025T231530Z-349c19cc`.

Integridade: 31629 entradas verificadas; rehash nesta etapa: 0; divergências: 0. Método e hashes em protected-before/after.json.

Fontes alterados nesta execução (before/after): nenhum.
