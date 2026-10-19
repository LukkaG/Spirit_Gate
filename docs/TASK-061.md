# TASK-061 - Primeira masmorra

Santuario exige missao aceita, possui entrada/retorno, dois sentinelas sem respawn e chefe adormecido ate derrota dos guardas. Reutiliza inimigos, combate, loot, quests e portais. Conclusao persistente dungeon-completed; guardas e chefe mortos permanecem mortos apos save. Reliquia entregue a Ancia conclui a campanha inicial.

R recupera personagem morto na vila, preservando progresso; nao reinicia nem altera arena legada. Teste DungeonCycleTest percorre desde personagem novo ate retorno e entrega final, sem injecao de XP/moeda/itens/posicao. Teste isolado de chefe passou a usar fixture com guardas ja derrotados; nao altera baselines historicos.
