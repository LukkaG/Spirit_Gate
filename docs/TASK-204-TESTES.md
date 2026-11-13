# TASK-204 — testes

`python tools/corujao7_verify.py TASK-204
com.projetok.multiplayer.Task204CombatHudTest
com.projetok.multiplayer.Task203CombatPresentationTest` (uma linha).

3 testes aprovados. O HUD foi atualizado e pintado em BufferedImage na EDT:
HP 75/130, energia 750/1000, recuperação e morte; sem valores inventados quando
snapshot está ausente. Evidências no checkpoint TASK-204/corujao-vii.
Teste de pixels não substitui avaliação humana de legibilidade.
