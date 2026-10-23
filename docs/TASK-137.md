# TASK-137

Prévia translúcida do pincel alinhada à mesma grade da pintura e sem leitura de disco. Escape e perda de foco cancelam gestos não confirmados sem criar Undo. Preservados zoom, pan, seleção, ferramentas, camadas, colisões e entidades existentes. Teste gráfico verifica prévia e rollback do gesto.

Baseline anterior: checkpoint before e regressão da tarefa precedente. Checkpoints incrementais baseados no ZIP anterior da TASK-134, com manifesto SHA-256 e instruções de restauração em pasta nova.
