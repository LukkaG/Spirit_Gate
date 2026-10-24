# Corujão V — relatório final experimental

Implementação integrada de TASK-160..175, com conclusão parcial do pacote de requisitos e pendências explícitas. A entrega real inclui população única de inimigo, ataque PvE do servidor, HP compartilhado, derrota única, respawn, recompensa sintética configurável, Shunpo reutilizado, missão `.sgquest`, duas regiões com portais e visibilidade isolada, política Radmin explícita, protótipo de armazenamento separado e pacotes privados por papel.

A demonstração localhost com dois clientes TCP reais cumpriu o núcleo de combate. Evidências finais em `.task-checkpoints/TASK-175/corujao-v/demo-02/`; a primeira execução válida está em demo-01, preservada junto ao diagnóstico de HUD corrigido. A validação gráfica de regressão e o inventário final estão nos checkpoints 174/175. Resultados exatos e hashes devem ser lidos nesses artefatos.

Resultado: 477/477 testes Java na execução gráfica, 469 aprovados e oito abortados ambientais no headless, zero falhas; Python 30/30. Na demonstração, HP 40→22→0, uma morte e dois eventos de recompensa sintética. Oito abortados headless foram cobertos pela execução gráfica integral.

TASK-175 é parcial quanto ao conjunto integral: homologação Radmin real, mapas personalizados/grandes, ensaio prolongado e jornada nativa completa continuam pendentes. O combate não é mockup. O offline e o Coop histórico usam caminhos anteriores; nenhuma migração pessoal ou autenticação definitiva foi introduzida.

Leia CORUJAO-V-PENDENCIAS.md e CORUJAO-V-RADMIN-MANUAL.md antes de homologar. Releases anteriores são preservados; nenhum push ou publicação foi realizado.
