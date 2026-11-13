# TASK-205 — IA por inimigo

31/10/2026. EnemyService já implementa patrulha, perseguição, ataque, retorno,
morte e respawn, com linha de visão e colisões. A falha estava na integração:
SharedRpgEncounter selecionava um único alvo a partir do primeiro mob da região.

A seleção agora ocorre para cada inimigo, entre jogadores vivos da mesma
região, por distância e ID em empate. O motor existente executa somente o mob
selecionado por chamada. Saída, morte ou mudança de região remove o alvo elegível.
IA continua confinada ao servidor. Não foi criado estado paralelo no cliente.

Arquivos: EnemyService, SharedRpgEncounter e Task205EnemyTargetsTest.
Limites: navegação usa o movimento com colisões existente; não adiciona A* ou
mapa infinito. Estados existentes são preservados para compatibilidade de saves.
