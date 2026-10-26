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
