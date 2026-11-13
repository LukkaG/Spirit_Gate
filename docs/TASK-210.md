# TASK-210 — missão multiplayer jogável

31/10/2026. A auditoria encontrou missão `coop_slime` em corujao-v.sgquest,
QuestService/AuthoredQuestSession, NPC Anciã na vila e eventos kill compartilhados.
Não foi reconstruído o motor. O cliente ganhou diário visível e botões de
conversa, aceitação e entrega, preservando F/E/C.

Missão individual: aceitar na Anciã; contribuir para derrotar um slime;
acompanhar ACTIVE/READY; retornar ao NPC para entregar. Recompensa: uma poção,
uma vez. Jogador sem aceitação/contribuição não recebe progresso do outro.
Proximidade, vida, região, requisitos e conclusão são validados no servidor.
Replay não duplica avanço ou recompensa. Não presume execução de todas as
missões do World Studio: esta integração expõe somente o contrato instalado.

Arquivos: NetworkQuestPanel, RpgNetworkClientLauncher e Task210QuestJourneyTest.
