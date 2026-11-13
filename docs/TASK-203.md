# TASK-203 — apresentação de combate

31/10/2026. Inspeção: RpgWorld, SharedRpgEncounter, RpgSessionWorld,
RpgNetworkClientLauncher, RemoteTimeline, DebugPanel e Protocol.

O servidor já confirma ataques, cooldown, dano e morte. Não existem quadros
de combate nos atlas. A implementação usa sinais geométricos sobre a sprite,
sem declarar animação artística pronta. Flags de ataque são publicadas pelo
servidor dentro do campo existente; nenhum opcode foi alterado nesta tarefa.

NetworkCombatPresentation apresenta início, golpe, recuperação, dano e morte.
O estado Idle/Walk continua no renderer existente. Snapshots repetidos ou
anteriores são ignorados; efeitos não enviam comandos nem aplicam dano.
HP zero prevalece sobre ataque e hit. Respawn limpa o hit anterior.

Arquivos: novo NetworkCombatPresentation, RpgWorld.attackRemaining,
RpgSessionWorld.snapshot, RpgNetworkClientLauncher e Task203CombatPresentationTest.
Risco: snapshots perdidos podem omitir transientes curtos; HP final permanece
autoritativo. Não mudou alcance, hitbox ou movimentação.
