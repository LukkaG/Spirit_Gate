# TASK-177 — Entrega técnica

Implementados: correção da espera artificial do Pong, medições monotônicas opt-in, envio de mudança de direção sem espera de três callbacks, timeline remota limitada/adaptativa, footprint medido de raio 12 na sessão e debug visual de sprite/pivô/corpos visual e autoritativo. Combate mantém suas regras; offline conserva raio 18; inimigos conservam 19. Spawn/portal/respawn respeitam ocupação e movimento da sessão usa varredura contínua. Respawn do inimigo compartilhado aguarda a área livre.

Resultados comparáveis localhost: RTT mediano 54.096→0.133 ms; p95 95.638→0.237 ms. Apresentação determinística 10 Hz: atraso médio 100→76.389 ms. Snapshots padrão continuam 10 Hz; 15 Hz é experimento opt-in com maior tráfego. Não há alegação de menor RTT físico VPN nem de ganho de CPU/alocações. Medições, métodos e limitações em TASK-177-NETWORK-PERFORMANCE.md.

Sprites reais e pivôs medidos, sem editar imagens. Contato entre jogadores 36→24 unidades. O primeiro ensaio detectou um problema nos limites de coordenadas do corpo menor; foi corrigido preservando limites históricos de save/protocolo. Protocol.java e formatos de save não foram modificados. Nenhuma posição final do cliente é aceita como autoridade.

Verificação e evidências: logs de JUnit headless/GUI, proxy TCP com atraso/jitter/retensões, testes geométricos e de cosméticos/animação/portal/respawn/Shunpo, capturas de dois clientes e launcher, medições e manifestos no checkpoint `.task-checkpoints/TASK-177/network-collision/`. O arquivo `validation-summary.json` registra os resultados finais e caminhos exatos dos logs. `packages-*.json` identifica ZIPs novos e SHA-256; o smoke registra a validação dos artefatos extraídos, launcher nativo, Java integrado e dois processos clientes.

Pacotes Anfitrião e Convidado TASK-177 devem ser distribuídos juntos. O protocolo histórico/capacidades permanecem compatíveis, mas um cliente antigo conserva a interpolação/debug anteriores. Releases existentes são preservadas; não usar um ZIP parcial para atualizar apenas o servidor e considerar o conjunto validado.

Pendente: homologação Radmin entre dois computadores, revisão humana das capturas e perfil completo de alocações/GC. ACK individual não está disponível no protocolo histórico. A caixa magenta representa limites alpha; a forma física é estável e independente da pose. Não há hurtbox independente exposta pelo protocolo para desenhar no cliente.

Roteiro completo para o responsável e seu amigo: TASK-177-TESTES.md. Não houve Git push, publicação de servidor, mudança de firewall/VPN, alteração de saves pessoais ou início da TASK-178.
