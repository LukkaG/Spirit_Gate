# TASK-151

Servidor TCP existente agora aceita backend opcional por SessionSimulation; padrão Coop histórico preservado. Backend RpgSessionWorld mantém dois perfis sintéticos RpgWorld na região compartilhada, corpos de colisão e movimento autoritativo a 30 ticks/s. CLI --rpg habilita o modo experimental. Teste real com dois clientes confirmou movimento e encerramento.

Baseline anterior: checkpoint before e regressão da tarefa precedente. Checkpoints incrementais baseados no ZIP anterior da TASK-134, com manifesto SHA-256 e instruções de restauração em pasta nova.
