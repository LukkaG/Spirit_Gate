# TASK-204 — interface de combate

31/10/2026. Auditoria de DebugPanel e RpgNetworkClientLauncher: HP já existe
em texto; stamina real é 0–1000; falta uma faixa estável de recursos.

Novo NetworkCombatHud mostra HP/máximo, energia/máximo, recuperação de ataque,
derrota/retorno e alvo confirmado pelo evento HIT do jogador. O overlay 203
mostra dano flutuante, impacto e morte. Atualização na EDT pelo timer existente,
sem disco ou carregamento de imagens. Ausência de snapshot mostra “aguardando”.
Não cria mana, stamina ou recargas fictícias. A recarga exata de habilidades
depende da integração da TASK-209; nesta etapa o ataque indica disponível ou
recuperação usando flags reais. Arquivos: NetworkCombatHud, launcher e teste.
