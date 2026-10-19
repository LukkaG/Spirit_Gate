# TASK-070 - Replicacao e cliente visual

Snapshots autoritativos 10 Hz, maximo oito atores/4096 bytes. NetworkClientLauncher desenha jogadores locais/remotos usando DebugPanel e camera existentes. Cliente envia intencoes; nao move GameEntity nem calcula fisica. Interpolacao visual de 100 ms fica em RemotePresentation e nunca altera DTO recebido ou estado do servidor.

Cliente independente: iniciar servidor explicitamente e depois NetworkClientLauncher (host porta nome). WASD envia direcao renovada; SPACE/Shift/Q sao intencoes enumeradas (combate sera conectado na TASK-072). Sem servidor automatico, conta ou save offline. Testes cobrem interpolacao pura e dois clientes/socket reais da etapa anterior. Captura de componentes Swing opcional e identificada como automatizada.
