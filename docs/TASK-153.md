# TASK-153

Cliente RPG experimental apresenta dois jogadores com PNGs originais, escala 0.5, Idle/Walk em quatro direções e Walk 250 ms. Interpolação apenas visual. Probe com duas janelas e conexões TCP reais confirmou observação mútua, movimento independente, remoção remota ao sair e reconexão com novo ID; capturas e JSON no checkpoint.

Baseline anterior: checkpoint before e regressão da tarefa precedente. Checkpoints incrementais baseados no ZIP anterior da TASK-134, com manifesto SHA-256 e instruções de restauração em pasta nova.
