# TASK-207 — testes

Task207InventoryTest (2), SharedRpgCombatTest (9), Task201CharacterSessionTest
(10): 21 aprovados. Sem falhas/abortos. Evidências no checkpoint TASK-207.

Servidor TCP real e dois clientes V6: Reaper desequipa/equipa sua espada,
Human não recebe nem consegue equipar o item alheio. Operações inválidas
não mudam a ficha. Roundtrip dos novos frames; rejeição de pilhas negativas,
slots duplicados/inválidos, caminhos e envelopes recursivos. Clientes V5,
seleção, reconexão, rejeição e revisão exercitados pela regressão TASK-201.
