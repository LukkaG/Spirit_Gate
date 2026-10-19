# TASK-047 - Evolucao RPG

Progressao configuravel: limite 20, XP acumulada para nivel L = 60*L*(L-1)/2; +12 HP e +3 ataque por nivel. Recompensa pode conceder varios niveis; XP satura no limite sem overflow. HP recebe apenas o aumento de capacidade e personagens mortos nao ressuscitam por XP.

Equipamentos agora respeitam nivel derivado. Snapshot publica nivel e HP maximo; save preserva XP e recalcula atributos. Testes de multiplos niveis, limite, valores invalidos e restauracao. Concessao por derrota sera conectada ao ledger de loot na TASK-049.
