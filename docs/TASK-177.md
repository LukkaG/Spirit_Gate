# TASK-177 — Movimento, RTT e colisão

Escopo: RPG multiplayer autoritativo Java 17/TCP. Sem push, publicação, mudanças no Radmin/firewall ou saves pessoais. TASK-176 local preservada. Auditoria anterior às alterações e hashes: `.task-checkpoints/TASK-177/network-collision/`.

O writer do servidor esperava até 100 ms por snapshots mesmo havendo um Pong na fila de controles. Acordar o writer por sinal compartilhado remove essa espera; controles continuam prioritários, snapshots mantêm fila de um estado, comandos e saídas continuam limitados. TCP_NODELAY e flush por frame já estavam ativos.

MOVE segue validado e aplicado a 215 unidades/s no servidor. O cliente envia mudanças no próximo callback gráfico, com heartbeat de 100 ms; não há predição ou posições finais aceitas. A timeline remota usa histórico limitado, ticks ordenados, relógio monotônico local e buffer adaptativo 50..150 ms. Lacunas grandes, morte, teleporte e troca de região não interpolam entre lugares incompatíveis. Não há extrapolação.

O footprint da sessão é um círculo estável de raio 12, derivado das 20 poses originais, compartilhado pelo servidor e debug. O RPG offline conserva raio 18; inimigos conservam 19. Combate, interação, velocidade, dano e recompensas não são dimensionados pelo footprint. Movimento da sessão usa varredura contínua X/Y. Limites antigos de coordenadas são preservados para manter protocolo e saves compatíveis. Respawn de inimigo compartilhado aguarda a posição ficar desocupada.

Parâmetros técnicos: `-Dspiritgate.network.debug=true`, `-Dspiritgate.network.snapshotHz=10` ou `15` no servidor; `-Dspiritgate.network.bufferMillis=0` (adaptativo) ou `50..150` no cliente. Propriedades Java devem vir antes de `-cp`. Default de snapshots permanece 10 Hz. F2 alterna o overlay; iniciar com a propriedade de debug habilita também as amostras detalhadas de RTT/pintura. Nenhum número de Hz do servidor é inventado no cliente.

Consulte TASK-177-NETWORK-PERFORMANCE.md, TASK-177-CHARACTER-COLLISION.md e TASK-177-TESTES.md. Homologação física entre dois computadores Radmin permanece pendente.
