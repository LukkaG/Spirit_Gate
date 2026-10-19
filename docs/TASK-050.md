# TASK-050 - IA PvE

Maquina de estados PATROL, CHASE, ATTACK e RETURN. Percepcao exige alcance e linha desobstruida; perde alvo por distancia, limite de perseguicao ou morte. Retorno ao spawn reabilita patrulha. Movimento usa MovementService.moveContinuous e as mesmas paredes; IA nunca move jogador diretamente.

Ataques com recarga de 36 ticks, defesa RPG reduz dano com minimo de 1. Estado e recargas dos inimigos persistem; ataque do jogador tambem guarda recarga. Testes deterministas de aquisicao, dano, morte, obstaculos e continuidade apos save. Nenhum sleep.
