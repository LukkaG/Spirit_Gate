# TASK-064 - Diario, mapa e objetivos

J abre diario completo com estados, contagens e todos os objetivos traduzidos do catalogo. Missoes indisponiveis nao revelam detalhes. M mostra representacao da regiao atual, jogador, solidos, NPCs, portais e recursos; inimigos apenas em raio proximo. Regioes futuras nao sao desenhadas. Historico de regioes visitadas persiste.

Janelas atualizam snapshots durante transicao e apos load, sem mudar camera ou dominio. Persistencia da recarga de portal tambem foi ajustada para impedir que reabrir save remova a protecao contra alternancia imediata. Suite completa inclui ciclo de tres regioes/quests/save; nenhuma alteracao de baseline historico.
