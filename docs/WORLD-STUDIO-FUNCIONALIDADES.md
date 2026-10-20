# Contrato de funcionalidades ? World Studio 0.114.0-rc1

| Funcionalidade | Editor | Playtest/exportacao |
|---|---|---|
| Biblioteca PNG, busca, categoria, SHA e miniatura | Sim; raizes autorizadas | Copia somente assets usados, por hash |
| Recorte virtual de tileset, margem/espaco/regiao | Sim | Recorte persistido; PNG original intacto |
| Pintura, balde, retangulo, selecao e camadas | Sim; limites documentados | Camadas visuais e ordem preservadas |
| Undo/Redo | 64 comandos e limite de referencias | Historico nao exportado |
| Colisao explicita | Retangulos; raio real 18 na previa | CollisionService real; transitavel nao abre furo em solido |
| NPC, bau, recurso, inimigo, jogador, ponto | IDs dos catalogos reais | NPC/ENEMY respeitam regiao; ponto e anotacao |
| Portais e grafo | IDs, destino, chegada e missao existente | Runtime conserva distancia/cooldown/campanha |
| Documento e backup | `.sgmap` schema 1, UTF-8, dados declarativos | Bundle opt-in validado, sem saves pessoais |
| Autotiling | NESW, 16 mascaras, fallback explicito | Resultado estatico; sem cantos internos de 8 vizinhos |
| Prefab | Selecao, colisao, camadas, terrenos, miniatura | Instancias independentes; nao agrupa portais/eventos |
| Edicao coletiva | Elementos e colisoes; alinhar/distribuir elementos | Coordenadas e ordem; portais/eventos tem formularios proprios |
| Rotacao/espelhamento | Nao implementados | Nenhuma transformacao silenciosa de direcoes |
| Animacao ambiental | 1..32 imagens, ordem, 30..5000 ms, loop | Somente previa; bundle usa asset base estatico |
| Iluminacao/clima | Dia/noite/entardecer/chuva/nevoa/interior/ciclo | Metadata de previa nao aplicada ao jogo; evento ENVIRONMENT funciona no playtest |
| NPC visual | Nome, sprite, posicao, orientacao, dialogo, missao | Nome e dialogo reais; orientacao nao inventa sprites; missao so referencia |
| Encontros | Ponto ou area, quantidade 1..64 | Populacao total <=64; limite=quantidade; ativacao/respawn builtin |
| Eventos | ENTER, AREA, INTERACT, NEAR_NPC:id, QUEST_DONE:id, MANUAL | MESSAGE, ENVIRONMENT, PORTAL, INTERACTION, CHAIN; oito por update; sem scripts |
| Minimapa/navegacao | Clique centraliza; colisao e marcadores | Analise amostrada aproximada, sem certificacao de jogabilidade |
| Diagnostico | Painel navegavel, evidencias e sugestoes | Erros bloqueiam exportacao; revisoes nunca aceitas automaticamente |

O adaptador inicial aceita ate tres mapas alcancaveis 960x640, com regioes de conteudo distintas village/forest/ruins. A autoria aceita 32..8192 pixels por eixo. A arena historica e o Coop nao recebem esses documentos. Mapa exportado e aberto pelo playtest isolado; nao substitui a campanha padrao nem os saves existentes.

Sprites personalizadas de NPC/inimigo sao opcionais. Bau/recurso ainda usa apresentacao geometrica do cliente real; nao prometemos que uma sprite atribuida ao marcador apareca nesses dois tipos. Conteudo invisivel no editor conserva suas referencias logicas; visibilidade de camada nao desliga fisica nem populacoes.

INTERACTION solicita a interacao proxima que o runtime escolher, sem forcar entidade arbitraria. INTERACT observa sucesso de interacao humana. Eventos `once` valem por sessao sintetica e nao persistem no personagem. PORTAL continua sujeito a proximidade e condicoes reais. Falhas ficam no log; nao ha teleporte administrativo.

Limites: 100.000 elementos, 32 camadas, 4.096 hitboxes, 64 portais, 256 eventos, documento 32 MB; catalogo ate 4.096 candidatos PNG, 8M pixels/8192 por eixo/32 MB por imagem; cache completo 64 MiB. Imagens podem ser expulsas do LRU em projetos muito grandes; placeholders exigem novo aquecimento/reabertura. O primeiro salvamento de 12.000 elementos mediu aproximadamente 8,4 s; continua assincrono.
