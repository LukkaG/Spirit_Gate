# Protocolo Corujão V

Envelope binário V1, frames até 4096 bytes e opcodes 1–8 anteriores preservados. Hello de aplicação 1 recebe State; aplicação 2 recebe SessionState cosmético quando suportada. Aplicação 3 exige `supportsCombat()` e habilita o contrato experimental de combate, incluindo aparência por IDs. Peers antigos em servidor de combate recebem somente jogadores visíveis, sem comandos de ataque RPG autorizados.

| Opcode novo | Contrato |
| --- | --- |
| 9 | CombatState: SessionState V1/V2 aninhado, até 16 eventos ordenados, região forest/village e até oito progressos de quest |
| 10 | WorldRequest: sequência compartilhada, enum RESPAWN/INTERACT/ACCEPT_QUEST/COMPLETE_QUEST/TRAVEL, ID textual limitado |
| 11 | Ping: nonce positivo |
| 12 | Pong: mesmo nonce, somente peer aplicação 3 |

Evento: sequência long, tick long, tipo limitado, jogador int, inimigo int, geração long e quantidade int. Progresso de quest contém ID, estado enumerado e contagens. Profundidade de extensão é limitada: CombatState → SessionState → State; wrappers recursivos são recusados. Nenhum PNG, caminho de arquivo, dano do cliente, token de conta ou save trafega.

Comandos mutantes usam sequência crescente até 10^12, oito por tick, fila global 64. Peer recebe até 120 mensagens/s, saída latest-state de um frame e fila de controle de quatro respostas. O writer único escreve frames inteiros. Handshake 2 s, inatividade do servidor 5 s, leitura do cliente 3 s e sessão dev até 30 min. Ping mede RTT com relógio monotônico do cliente; limite de um pendente, expiração de 5 s, amostragem aproximada 1 s no cliente Swing.

ID de jogador 1..1000000; IDs 1000000+ID do EnemyService para inimigo e 2000001 para NPC da fixture, transportados no campo histórico de ator não jogador. O booleano boss é marcador de ator não jogador nesse adaptador, não promoção do slime a boss de conteúdo.

Contrato de aplicação 3 novo nesta rodada, experimental e sem suporte a builds intermediários dela. Mudanças futuras incompatíveis exigem nova negociação. Testes de bytes históricos, roundtrip, limites e peers mistos continuam essenciais.
