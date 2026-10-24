# Mundo compartilhado experimental

`SharedRpgEncounter` possui uma `EnemyService`, com spawn `coop_slime`, tipo existente `slime`, região `forest`, posição (220,300), HP 40. O ID de rede é 1000100, estável durante respawns; a geração diferencia derrotas. Jogadores não possuem população própria com inimigos.

`RpgSessionWorld` mantém perfis sintéticos por conexão, geometria e catálogo cosmético. Colisões de jogadores e inimigo usam `CollisionService`; entrada/reentrada procura posição livre próxima ao spawn. Respawn de jogador recusa chegada ocupada. Mortos deixam de bloquear outros jogadores. A IA usa corpos vivos da região e escolhe o jogador vivo mais próximo, com desempate determinístico por entrada.

O relógio do encontro é global; um personagem recém-conectado não antecipa respawn de inimigo. Snapshots para cada peer filtram jogadores, inimigos, aparências e eventos pela região. O NPC Anciã é compartilhado na vila; diálogo e ledger de quest são individuais.

Testes: `SharedRpgCombatTest`, `RpgSessionServerTest`, regressões de movimento/colisão e transporte. A estabilidade demonstrada cobre o encontro sintético, não uma campanha online completa.
