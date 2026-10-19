# TASK-045 - Consumiveis

Pocao recupera ate 50 HP, com recarga de 60 ticks (2 segundos), persistida no save. Consumo somente apos validar categoria, vida, HP maximo, quantidade, recarga e ausencia de transicao. Feedback publicado no snapshot e exibido no HUD reutilizado. Dano e cura acontecem exclusivamente no dominio.

Testes de HP cheio, morte, uso repetido, espera deterministica e save durante recarga. Flag de transicao bloqueia uso e sera acionada pelo servico de portais. Nenhuma alteracao de vigor ou habilidades legadas.
