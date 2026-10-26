# CORUJ?O VI ? TASK-185
Estado: **VALIDADA**.
Branch/HEAD verificados: beco / ad66caed14e3f94bc369f7edd9861900d5a68636.
Escopo implementado: oito ensaios TCP de corte no ingresso e candidata em BEGIN, READY enviado, ACTIVATE e RESUME; reconex?o recebe apenas revis?o confirmada sem duplicar jogadores.
Arquivo alterado: server/src/test/java/com/projetok/multiplayer/Task185ReconnectTest.java (novo). Nenhuma corre??o de produ??o necess?ria nos casos exercitados.
Testes executados: PASS, 113/113 multiplayer, zero falhas/abortos/skips, incluindo seis cen?rios 183, oito novos e negativos hist?ricos de inputs/snapshots antigos, conte?do parcial, wrappers e controle editorial. Fontes novas compiladas para Java 17 contra baseline rec?m-compilado.
Resultados: cortes antes do commit preservam base e ponteiro vazio; corte ap?s RESUME preserva candidata confirmada, ponteiro e reconex?o compat?veis. READY enviado ? distinto de READY processado; teste hist?rico disconnectAfterReadyAndDuringActivationRecoversRemainingPlayer cobre reserve conclu?do.
Logs reais/comandos/cwd/dura??o/exit code: commands.json e multiplayer.log no checkpoint .task-checkpoints/TASK-185/corujao-vi/run-20261025T175912Z-e72a0cec.
Integridade: invent?rio protegido da 183 referenciado por SHA em before.json; testes executados no isolamento 182 e em @TempDir. Nenhuma escrita nas ?reas protegidas por estes testes. Compara??o SHA final ser? consolidada na pr?xima execu??o de integridade.
Regress?es ou riscos: sem altera??o do servidor; renderer de prepara??o headless n?o comprova GUI sob VPN. Testes n?o constituem login/autentica??o de contas.
Pend?ncias humanas/externas: Radmin f?sico, atualizador ambiental e propostas 181 continuam pendentes.
Pr?xima a??o: TASK-186 automaticamente.
