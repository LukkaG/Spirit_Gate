# TASK-017 - Auditoria reproduzível da base

18/10/2026. APROVADA nos controles automatizados Windows locais.

Documentos anexos tratados como especificação das TASK-017 a 021 dentro do pedido do usuário. Pacote original extraído em docs/pacote-TASK-017_021. Sem Git; base textual exata em .task-checkpoints/TASK-017-before.json.

ZIP original: SpiritGate-0.16.0-rc1-20261018T230252Z-a6150a66-portable.zip, 29.386.343 bytes. SHA-256 confirmado: 9bc425107d23ccfd9a4d36279be4da23c7f016a74c92fe03a9bb1f0d36eb79c1.

Reexecução offline: 216 JUnit, zero falhas, erros ou ignorados; 12 Python verdes. Auditor oficial repetido DEPOIS do clean build: PASS, incluindo igualdade de todas as classes, runtime Java 17, PE x64, whitelist, inventário e manifest. ZIP original não foi reescrito. Controle negativo em cópia descartável: adulteração do JAR rejeitada. Testes Python cobrem traversal, duplicatas, links e nomes Windows inseguros.

Smokes reais Windows padrão e --map aprovados, com JVM integrada e PATH controlado: TASK-017-smoke.json e TASK-017-map-smoke.json. Homologação humana e outro computador NÃO EXECUTADOS. Linux NÃO EXECUTADO: sem ambiente Linux comprovado nesta rodada.

Ocorrência operacional: argumento Maven sem aspas foi interpretado incorretamente pelo PowerShell; reexecutado com '-Dmaven.repo.local=.maven-repository', sem alterar configuração global. Resultado final registrado em TASK-017-java.log. Nenhuma correção de gameplay necessária. Arte e fontes preservadas. Dependências legadas do pom não entram no JAR portátil, conforme auditoria.
