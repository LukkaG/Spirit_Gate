# TASK-206 — loot compartilhado

31/10/2026. Auditoria: LootService já valida catálogo, probabilidade finita,
quantidades, ledger por spawn/geração e pendências quando o inventário enche.
SharedRpgEncounter apenas emitia evento de recompensa, sem dar itens/XP/moedas.

A morte agora recompensa cada contribuidor vivo, conectado e na mesma região,
individualmente: tabela existente, +3 XP e +5 moedas. Não contribuidores não
recebem; desconectados e mortos não recebem. A política DISABLED permanece.
O ledger da geração impede repetição. Inventário cheio conserva loot pendente;
coleta será exposta junto ao inventário na TASK-207. Histórico usa o ring de
eventos existente (16 eventos); não é log persistente global.

Arquivos: RpgWorld.sharedReward, SharedRpgEncounter e Task206SharedLootTest.
Não há economia persistente nem escrita em saves. Recompensas anteriores
fictícias deixam de ser apenas notificações e passam a corresponder a estado.
