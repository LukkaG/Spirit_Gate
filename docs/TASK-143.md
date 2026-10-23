# TASK-143

Minimapa de playtest desenha exclusivamente documento autorado, sólidos, portais e jogador da região atual. Projeção deriva das dimensões reais; descritor ausente não recorre ao mapa legado. Renderização por camadas reutiliza CanvasIndex para culling e preserva ABOVE/ROOF. Câmera lógica existente permanece independente de efeitos.

Baseline anterior: checkpoint before e regressão da tarefa precedente. Checkpoints incrementais baseados no ZIP anterior da TASK-134, com manifesto SHA-256 e instruções de restauração em pasta nova.
