# TASK-059 - Bau e recursos

Objetos declarativos: bau inicial com espada, ervas do bosque com duas pocoes e respawn de 600 ticks, cache da ruina com tres pocoes. F coleta a ate 65 unidades, sem inferir hitbox pelo desenho. Baús abertos e tempo de respawn persistem. Inventario cheio nao consome objeto.

Teste integrado de distancia, coleta unica, save e repeticao. Acrescentado diagnostico --smoke nome captura.png para captura REAL da janela por Robot, sem gravar progresso. Nao substitui homologacao humana.

Marco A automatizado: OfflineCycleTest percorreu criacao, conversa, quest, portal, combate, loot, recompensa, compra, equipamento, save e retomada sem injetar progresso. Pacote 0.58.0-rc1 gerado e preservado em dist, com Java integrado e hashes.
