# TASK-046 - Loot deterministico

Tabelas Properties schema=1 com inimigo, item, chance e faixa de quantidade. RandomGenerator injetado. Ledger guarda maior geracao recompensada por spawn, rejeitando duplicacao e repeticao de geracoes antigas sem crescimento ilimitado por morte.

Inventario cheio envia itens a recompensas pendentes persistentes nas flags do save; collect tenta entregar sem perda. Limite explicito recusa nova transacao ao exceder 9999 pendentes por item. API sera chamada exclusivamente na derrota PvE ao integrar spawns na TASK-049; ainda nao ha inimigos jogaveis neste incremento.

Testes de RNG fixo, idempotencia, geracoes antigas, inventario cheio, coleta posterior e chance invalida. Suite completa.
