# TASK-197 — consistência de assets

VALIDADA no laboratório local, sem mudança de produção. Três testes novos usam instalações sintéticas separadas host/convidado com o mesmo caminho relativo e hash PNG. WorldContent.prepare prepara os mesmos pixels e revisão com cache próprio por cliente; apagar o arquivo ou fornecer catálogo sem recurso causa rejeição. Adulteração mantendo caminho/ID mas SHA diferente é recusada antes da ativação, com causa Sprite inexistente ou Asset alterado, sem aceitar placeholder como conteúdo sincronizado.

PNG com IHDR declarado de 9000 pixels (CRC válido) e arquivo de 32.000.001 bytes são rejeitados antes de decodificar pixels. Permanecem limites existentes de 8192 por eixo, 8M pixels/imagem, 128 assets/revisão e 64MiB ativos; este teste não amplia nenhum deles nem afirma medir todas as combinações dos limites agregados. Nenhuma URL ou transferência dinâmica de imagem foi acrescentada; o descritor referencia catálogo preinstalado.

Três testes específicos aprovados, além da regressão do pacote Studio. Cenário TCP real de recurso ausente e demais regressões já executados integralmente na TASK-196 (610 gráficos aprovados); publicação com recurso preinstalado nos pacotes novos será exercitada na 198/199. Diffs, fontes, logs, comandos e integridade no checkpoint. Nenhum asset original modificado; falta/adulteração geradas somente em diretórios temporários de teste.


Evidências: `.task-checkpoints/TASK-197/corujao-vi/run-20261026T001845Z-7be8a654`.

Integridade: 31629 entradas verificadas; rehash nesta etapa: 0; divergências: 0. Método e hashes em protected-before/after.json.

Fontes alterados nesta execução (before/after): server/src/test/java/com/projetok/studio/Task197AssetConsistencyTest.java.
