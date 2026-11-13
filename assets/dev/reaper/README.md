# Reaper

Arte fornecida pelo usuario (`art_source: existing`), sem geracao ou alteracao dos PNGs.
36 imagens de 64x64 com alfa: Idle e Walk de oito frames em quatro direcoes.
Pivo compartilhado (32, 60), escala nativa 1.0, interpolacao nearest-neighbor,
125 ms por frame e ciclo de caminhada de um segundo. O pivo fixo preserva
a oscilacao original das poses. SHA-256 e mapeamento em
`server/src/main/resources/rpg/reaper-player.properties`.

Reaper e a primeira raca na criacao e usa estas sprites por padrao no RPG offline,
com escala 1.0 e 125 ms por frame.
Execute `jogar-rpg-offline.cmd` ou `testar-rpg-sprites.cmd`, crie/selecione
uma Reaper e entre no jogo. Saves existentes preservam sua raca.
`testar-rpg-sprites.cmd --geometrico` desativa as sprites para comparacao.
As propriedades `spiritgate.rpg.spriteScale` e `spiritgate.rpg.spriteFrameMillis`
permitem ajustar escala e duracao. O conjunto anterior `player-base` permanece
disponivel para os consumidores e testes existentes.

No multiplayer, a apresentacao usa Reaper em 1x e Human em 0.4x.
A referencia e a altura visivel das artes: Reaper possui 52-54 pixels de
altura no original, enquanto Human possui 134-142. O resultado aproxima
ambos de 54 pixels de altura sem ampliar a Reaper. Seu canvas permanece
64x64 e o desenho usa nearest-neighbor com o pivo dos pes preservado.
A reducao do Human descarta parte dos detalhes na apresentacao; os PNGs
originais de ambos permanecem intactos. A escala da janela e do Windows
ainda afeta o tamanho final em pixels da tela. A escala visual nao altera
a hitbox ou o alcance dos ataques.

Teste de camera 0.200.7: a Reaper continua em 1x logico. O Human e seus
cosmeticos sao reduzidos uma vez para canvas residente 64x64, pivo (32,60),
antes do zoom. O seletor Zoom no multiplayer compara 1x, 2x e 3x, iniciando
em 2x. Camera e cenario ampliam por inteiros; os PNGs originais permanecem
intactos. Veja docs/TESTE-CAMERA-0.200.7.md para pacotes e verificacoes.

Teste 0.200.8: o seletor tambem oferece 0.75x e 1.25x, iniciando em 1.25x.
Estas escalas fracionarias mantem nearest-neighbor, com reducao/repeticao
irregular de pixels conforme o tamanho escolhido. Os PNGs nao mudam.
Veja docs/TESTE-ZOOM-0.200.8.md.
