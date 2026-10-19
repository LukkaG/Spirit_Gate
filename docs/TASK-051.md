# TASK-051 - Guardiao do Santuario

Chefe de 320 HP: ataque basico e especial em area com estados PREPARE, execucao na simulacao e RECOVER. Aviso de 30 ticks; abaixo de 50% HP, aviso de 18 ticks, raio 135 e dano dobrado. Recuperacao de 45 ticks. Snapshot de aviso usa o desenho circular do renderer existente; desenhar nao aplica dano.

Morte interrompe todas as acoes, nao tem respawn e concede reliquia unica pelo ledger. Flag boss-defeated persistente. Teste do encontro comprova aviso sem dano antecipado, execucao, derrota, recompensa e ausencia de ataques apos morte. Fase e derivada do HP salvo.
