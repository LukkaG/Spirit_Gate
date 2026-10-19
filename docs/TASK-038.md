# TASK-038 - Entrada offline independente

RpgLauncher inicia o modo RPG; DebugLauncher continua iniciando a arena historica. Runtime proprio com ciclo explicito, thread unica, snapshots imutaveis e encerramento idempotente. Movimento/colisao reutilizam MovementService/CollisionService. A apresentacao reutiliza integralmente DebugPanel e sua camera, sem um segundo renderer.

Entrada: java -cp server/target/classes com.projetok.prototype.rpg.RpgLauncher. Neste primeiro incremento o mundo contem apenas um personagem provisório e obstaculo de teste; identidade e conteudo seguem nas proximas tarefas. Nenhuma alegacao de ciclo RPG completo nesta etapa.

Testes: limites do mundo, entrada nao finita e suite completa Java/Python. UI real sera verificada no marco integrado.
