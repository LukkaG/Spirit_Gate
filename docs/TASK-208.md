# TASK-208 — progressão multiplayer

31/10/2026. RpgProgression e DomainProgression já calculam níveis e atributos.
O servidor passa a publicar XP, nível, próximo limiar cumulativo, ataque, defesa
e domínio na ficha privada V6. A recompensa da TASK-206 é a fonte de XP de mobs.
Não existe comando para o cliente enviar XP ou nível.

NetworkProgressLabel mostra os valores recebidos e sinaliza ganho/subida de nível.
Requisitos existentes: iniciação no nível 15, Shunpo no 20. Não há recompensa
de nível inventada; HP máximo e ataque usam a curva real (60 de passo, +12 HP,
+3 ataque por nível). O progresso é da sessão, sem migração ou save pessoal.

Arquivos: RpgWorld.ProgressView, GameplayProtocol.Progress/Sheet,
RpgSessionWorld, NetworkProgressLabel, launcher e Task208ProgressionTest.
