# TASK-168 — relatório

Sessões novas isoladas; retomada por token não implementada.

UUID distinto por instância/conexão, timeout e limpeza existentes. Reconexão descarta estado individual. Token inválido/expirado não se aplica: protocolo não aceita tokens.

[Contrato consolidado](CORUJAO-V-ARQUITETURA.md). Checkpoint em `.task-checkpoints/TASK-168/corujao-v/`: delta/hash cumulativo contra a referência 160; não representa uma fronteira temporal retroativa. Sem push, publicação ou migração de saves pessoais.
