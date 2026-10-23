# TASK-144

Auditoria e regressão de movimento normal e Shunpo. Aproximação ofensiva agora verifica raio físico de 18 em todo o trajeto, rejeitando passagem por parede fina deslocada mesmo quando o centro tem visão livre. Feedback específico para recarga, vigor e estado. Diagonal, ausência de drift e cooldown após roundtrip testados.

Baseline anterior: checkpoint before e regressão da tarefa precedente. Checkpoints incrementais baseados no ZIP anterior da TASK-134, com manifesto SHA-256 e instruções de restauração em pasta nova.
