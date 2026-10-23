# TASK-145 — Relatório

Investigação e otimização localizada do primeiro salvamento. Três processos antes/depois, cinco novos saves e backups por processo; 254 assets e 12.000 elementos. Temporário exclusivo CREATE_NEW evita inicialização criptográfica JDK no caminho do mapa. Mediana local inicial 8228,93 para 243,00 ms; validações, backup e atomicidade preservados.

Limitações: Resultados locais, caches e atividade de máquina não isolados. Salvamentos aquecidos, abertura, pintura, memória e transformações não receberam promessa de ganho. Metodologia e dados brutos em CORUJAO-IV-OTIMIZACOES.md.

Validação: JUnit: 444 executed, 436 passed, 8 aborted, 0 failures, 0 skipped. Nenhuma aprovação artística automática. Diff e hashes no checkpoint.
