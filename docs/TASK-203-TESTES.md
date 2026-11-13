# TASK-203 — testes

Runner: `python tools/corujao7_verify.py TASK-203
com.projetok.multiplayer.Task203CombatPresentationTest
com.projetok.multiplayer.SharedRpgCombatTest` (argumentos na mesma linha).

2 testes de apresentação e 9 de combate aprovados, sem abortos ou falhas.
Incluem servidor TCP real com dois clientes, dano compartilhado, replay,
morte, respawn, portas e recompensas existentes. Evidências:
`.task-checkpoints/TASK-203/corujao-vii/verification.json` e logs por classe.

O teste novo detectou hit residual após respawn, corrigido antes do resultado
final. A primeira suite ampla foi mantida como rodada intermediária.
Validação visual humana e Radmin físico não foram executados.
