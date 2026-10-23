# TASK-145

Investigação e otimização localizada do primeiro salvamento. Três processos antes/depois, cinco novos saves e backups por processo; 254 assets e 12.000 elementos. Temporário exclusivo CREATE_NEW evita inicialização criptográfica JDK no caminho do mapa. Mediana local inicial 8228,93 para 243,00 ms; validações, backup e atomicidade preservados.

Baseline anterior: checkpoint before e regressão da tarefa precedente. Checkpoints incrementais baseados no ZIP anterior da TASK-134, com manifesto SHA-256 e instruções de restauração em pasta nova.
