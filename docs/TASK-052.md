# TASK-052 - Interacao contextual

NPCs posicionados em conteudo, nomes pelo registro central. F interage com o mais proximo a ate 70 unidades e linha desobstruida. Morte/transicao impedem interacao. NPCs RPG sao nao combatentes; NPCs e colisoes da arena legada permanecem intactos.

Controles reais conectados por fila limitada de 64 comandos, consumida pela thread da simulacao: SPACE golpe, Q habilidade RPG, 1 pocao, F interacao e F5 save assincrono. Entradas discretas usam press/release para impedir repeticao do SO. IO em executor separado; snapshot imutavel capturado antes da escrita.

Testes de alcance, identidade do NPC, morte e suite integral. Dialogos e comercio seguem nas tarefas especificas.
