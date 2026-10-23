# TASK-158

Capacidade cosmética de aplicação 2 negociada por peer, mantendo envelope e bytes V1. AppearanceRequest altera apenas a própria sessão e valida catálogo, sequência e limites. Snapshots estendidos carregam IDs e raça atribuída pelo servidor; nenhum PNG. Clientes renderizam camadas locais e fallback. Probe real confirmou A/B com aparências e raças diferentes, movimento, saída e reconexão limpa.

Baseline anterior: checkpoint before e regressão da tarefa precedente. Checkpoints incrementais baseados no ZIP anterior da TASK-134, com manifesto SHA-256 e instruções de restauração em pasta nova.
