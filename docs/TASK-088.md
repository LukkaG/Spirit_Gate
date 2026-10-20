# TASK-088 - Corujao I

P077-01 reproduzida antes: futureDone=false; depois: true. Fila limitada agora guarda cancelamento; pausa/close/falha recusam novas acoes e resolvem pendencias. saveAndPause captura comandos anteriores, pausa antes do IO e mantem retomada em erro. Testes de corrida 300 acoes/close, falha de tick injetada e save confirmado aprovados. Launcher informa falha fatal e encerra sem alegar save. Primeiro teste presumiu mapa por item em vez de slot:item, corrigido usando InventoryService real; log preservado.
