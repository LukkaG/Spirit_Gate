# TASK-049 - Inimigos, spawns e combate basico

Catalogos enemies/spawns definem HP, ataque, velocidade, percepcao, limite de perseguicao, respawn, regiao e posicao. Populacao maxima 64, quatro spawns iniciais. Validacao recusa locais obstruidos e regioes proibidas. Vila segura, tres inimigos no bosque, chefe sem respawn no santuario.

Ataque basico autoritativo escolhe alvo proximo com linha livre, recarga de 12 ticks; 82 unidades guardian/145 mystic. Derrota integra loot, XP e cinco moedas por ledger de geracao. Estado de HP/posicao/morte/respawn e persistente. Renderizacao de inimigos e controles sera integrada com UI; IA na tarefa seguinte.

Teste integrado elimina inimigo, verifica recompensa unica, salva/carrega e aguarda respawn sem sleeps. Suite completa.
