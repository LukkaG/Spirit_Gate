# TASK-176 - Testes

Baseline Maven: 477 testes, zero falhas/erros/ignorados (baseline.log). Primeira tentativa com repositorio Maven relativo falhou antes dos testes; caminho absoluto corrigiu a execucao.

Regressao integral final: 480 testes Java, zero falhas/erros/ignorados (java-release.log). Python unittest: 30/30 (python-unit.log). A rodada anterior encontrou regressao de largura compacta no formulario, corrigida e revalidada. O runner headless anterior teve oito abortos ambientais e a mesma falha de layout; nao representa o resultado final.

Apos ajuste textual final: cinco testes direcionados aprovados (final-ui-network.log). LauncherSessionTest usa a fabrica de servidor do painel: dois clientes TCP V3, movimento, porta ocupada, liberacao de porta e listener externo preservado. Suites existentes cobrem combate, HP compartilhado, cosmeticos, portais, reconexao e protocolos anteriores.

RadminLauncherUiTest abre telas Swing reais, verifica resposta da EDT, fecha as janelas e captura host.png/join.png. SpiritGateEntryTest cobre controles offline e tamanhos compactos. Nesta maquina o adaptador Radmin nao foi detectado; nao foram forjados adaptadores do sistema.

Smoke dos pacotes: SHA-256 do ZIP e de cada arquivo, extracao em diretorio novo, autoteste pelos dois SpiritGate.exe, runtime integrado, servidor e dois processos clientes reais. Capturas sincronizadas para manter os dois jogadores conectados. O primeiro harness fechava uma janela cedo; a primeira revisao do probe carregava assets depois da conexao e excedia o timeout de inatividade. Corrigido para carregar antes de conectar, como o cliente real. Evidencias em packages-*-smoke-*/ e logs no checkpoint. Consultar delivery-final-smoke.log para a auditoria dos ZIPs finais.

Teste Radmin remoto e aprovacao visual humana: PENDENTE DE HOMOLOGAÇÃO FÍSICA. Capturas e localhost nao equivalem a aprovacao humana ou homologacao remota.
