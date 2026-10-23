# Marco E — Arquitetura, servidor e protocolo

TASK-150 documentou transporte e decisão de backend opcional. TASK-151 implementou SessionSimulation e RpgSessionWorld, preservando o Coop padrão. O servidor com `--rpg` usa 127.0.0.1, duas sessões sintéticas e movimento de RpgWorld, com corpos compartilhados e sem campanhas pessoais.

TASK-152 manteve bytes históricos do protocolo V1 e fortaleceu UTF-8. O golden test e toda a regressão passaram: 451 executados, 443 aprovados, oito abortados ambientais, zero falhas/ignorados. Os testes reais de dois clientes exercitam handshake, movimento, capacidade e shutdown.

Backend RPG ainda não possui combate compartilhado. Ataques, habilidades de arena e Shunpo são recusados nesse perfil iniciante. Isso não altera o combate offline nem o Coop histórico. LAN em dois computadores, autenticação, persistência online e segurança de produção não estão homologadas.
