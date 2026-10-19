# TASK-044 - Equipamento RPG

Slots weapon, torso, legs e accessory validados por catalogo. Equipar remove uma unidade, devolve equipamento anterior e publica somente quando a transacao inteira cabe. Desequipar recusa inventario cheio sem perder item. Ataque/defesa recalculados a partir do estado atual, sem acumular modificadores.

Espada adiciona 8 ataque; manto adiciona 5 defesa. Formulas exclusivas do RPG; nenhuma alteracao na arena. Save valida compatibilidade dos slots ao restaurar. Testes incluem item ausente/incompativel, atributos apos save, cheio e rollback.
