# TASK-129 — Corujão III

Estado: **VALIDADA COM RESSALVAS**.

Janela temática com vendedor e inventário vendável, preços unitários reais, quantidade, saldo, capacidade e descrição. Oferta e cotação extraídas de RpgWorld.trade sem alterar regras. Operações serializadas por pending e confirmadas pelo runtime. Testes de inventário cheio, saldo insuficiente, item inexistente, quantidade inválida e imutabilidade da oferta. Probe real: dois cliques geram uma compra, fechamento durante operação, saldo 88 e save temporário íntegro. Capturas: docs/task-129/capturas-final.

Homologação visual e teclado físico pendentes; printAll não valida monitor físico.

Logs: `docs/TASK-corujao3-129-final-*`. Checkpoints recuperáveis, inventários SHA-256, diff isolado e preservação: `.task-checkpoints/TASK-129/corujao-i/`. O nome histórico da pasta pertence à ferramenta reutilizada; o conteúdo corresponde à tarefa indicada.

[Testes](TASK-129-TESTES.md).
