# Regiões e portais

Fixture compatível com o runtime atual: `forest` e `village`, ambas 960×640 e obstáculo existente (380,180,120,120). `RpgSceneSet`, `PortalService` e `RpgWorld.travel` são reutilizados. Portal `to_village`: (140,300) → vila (140,320); `to_forest`: (140,320) → floresta (140,300). P solicita a rota conhecida para a região atual. O servidor verifica ID, origem, distância 65, cooldown 30 ticks, requisitos existentes e chegada sem jogador vivo/inimigo. Falha preserva região e posição de origem.

Peers recebem apenas jogadores e aparências da mesma região. Slime/eventos de combate são visíveis na floresta; Anciã (190,320) na vila. O cliente troca o rótulo e entidades da região confirmada. A geometria das duas fixtures é idêntica; não há carregador genérico de mapas de rede.

O contrato `.sgmap`/`RpgSceneSet`, o DTO `Actor` e o save V2 ainda limitam coordenadas. Não foi relaxado esse limite. Evolução proposta: registro de região com ID, dimensões, hash/revisão de geometria, spawns e portais validados, índice de entidades por região e snapshots específicos. Mapas personalizados/grandes no backend online permanecem pendentes; nenhum suporte persistente é declarado.
