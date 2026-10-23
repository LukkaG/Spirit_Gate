# TASK-152

Protocolo V1 documentado com limites, sequenciamento, ownership e timeouts reais. Golden wire test conserva exatamente o Hello histórico. Decodificador passa a recusar UTF-8 inválido; testes cobrem frame truncado, dados excedentes e rejeições estruturais, além das regressões de rede existentes.

Baseline anterior: checkpoint before e regressão da tarefa precedente. Checkpoints incrementais baseados no ZIP anterior da TASK-134, com manifesto SHA-256 e instruções de restauração em pasta nova.
