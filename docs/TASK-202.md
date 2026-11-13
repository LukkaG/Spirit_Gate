# TASK-202 — catálogo de sprites por raça

Implementação local autorizada pelo usuário em 31/10/2026. Escopo: seção 5
do [plano](CORUJAO-VII-PLANO.md). Não inicia a TASK-203.

## Inventário verificado

| Raça | Atlas usado | Idle/direção | Walk/direção | Cadência | Acessórios |
|---|---|---|---|---|---|
| Reaper | Próprio, 36 PNGs | 1 | 8 | 125 ms | Indisponíveis |
| Human | player-base, 20 PNGs | 1 | 4 | 250 ms | Camadas Human existentes |
| Aetheri | Human provisório | 1 emprestado | 4 emprestados | 250 ms | Camadas Human existentes |
| Spirit | Human provisório | 1 emprestado | 4 emprestados | 250 ms | Camadas Human existentes |

Todos têm Frente, Costas, Esquerda e Direita. **Run, Attack, Hit e Death não
possuem quadros próprios nesses atlas.** Os efeitos geométricos de combate
existentes não são animações de atlas. Aetheri e Spirit não ganharam arte.
As propriedades em `server/src/main/resources/rpg/` vinculam nomes, SHA-256,
dimensões e pivôs aos PNGs; o carregador valida os arquivos antes de decodificar.

Human conserva escala de origem 0,4, normalizada uma vez para canvas residente
64×64 e pivô (32,60); os arquivos de origem têm dimensões/pivôs individuais
registrados em `experimental-player.properties`. Reaper conserva canvas 64×64,
pivô (32,60) e escala 1. Os pivôs são ancorados na posição do ator, e os testes
verificam pixels ocupados sem ultrapassar o solo. Escala visual não define colisão.

## Implementação

`RaceSpriteCatalog` registra as quatro raças, atlas efetivo, caráter provisório,
direções, quadros, tempo, escala, canvas, pivô e compatibilidade de acessórios.
Os mapas e conjuntos são imutáveis. A ausência de uma ação é explícita em
`supports`; não há simulação de novos quadros de combate.

`NetworkPlayerVisuals` consulta o catálogo para normalização, cadência e
acessórios. Prévia e jogo consultam a mesma regra de compatibilidade. A raça
negociada continua vindo do servidor; nome e ordem de entrada não escolhem atlas.
Identidade vazia do cliente legado conserva o caminho Human.

Quando Reaper não carrega, a prévia informa a indisponibilidade e bloqueia o
ingresso, como na TASK-201. O renderer passa a devolver ausência de sprite,
ativando o desenho geométrico de segurança do mundo, em vez de emprestar Human
silenciosamente. Aetheri/Spirit continuam com aviso de aparência Human provisória.
Human ausente também bloqueia o ingresso das raças que dependem dele.

## Arquivos da mudança

- Novo `prototype/rpg/RaceSpriteCatalog.java`.
- `multiplayer/NetworkPlayerVisuals.java`.
- `multiplayer/RpgNetworkClientLauncher.java`.
- `prototype/rpg/SessionCharacterCreation.java`.
- Novo `multiplayer/Task202RaceSpritesTest.java` em testes.
- Documentos da TASK-202 e continuidade do Corujão VII.

Não houve alteração de PNGs, colisões, protocolo, regras de combate ou saves.
Os fontes locais são executados por `rodar-prototipo.cmd`, opções Radmin.
O EXE oficial continua anterior a estas mudanças. Sem novo pacote ou publicação.

Validações e limitações: [TASK-202-TESTES.md](TASK-202-TESTES.md).
