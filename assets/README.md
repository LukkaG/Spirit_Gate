# Arte do cenário

- `tilesets/karakura-environment-v3-blend.png`: terceira versão, combinando a paleta suave da primeira com os contornos e a definição da segunda, via edição no imagegen integrado. As duas versões anteriores foram preservadas. As limitações de recorte descritas abaixo também se aplicam a esta variante.

- `tilesets/karakura-environment-atlas.png`: atlas principal gerado com imagegen integrado, usando a referência de subúrbio japonês fornecida na conversa.
- `source/karakura-environment-v1.png`: primeira versão, preservada para edição.
- `sprites/`: destino para objetos recortados.
- `images/`: destino para outras imagens do jogo.

O atlas contém água e margens de canal, grama e terra, asfalto e faixas, calçadas, arbustos, cercas vivas, árvores e cerejeiras, casas, apartamentos e comércio.

## Aseprite

Abra o PNG e salve uma cópia como `.aseprite` para edição. Selecione e recorte cada objeto pelas suas bordas; remova o fundo com a ferramenta de seleção antes de exportar objetos transparentes. Para tiles, ajuste cada recorte para 32×32 com interpolação nearest neighbor e confira as bordas repetidas. Edifícios podem ocupar vários tiles.

## Limitações da geração

Esta entrega contém imagens raster geradas, ainda sem integração ao renderizador Java. A geração não respeitou a grade exata solicitada, nem garante fundo uniforme #808080, ausência total de antialiasing, paleta limitada ou repetição perfeita dos terrenos. Não use o recorte automático global de 32×32 nesta imagem: é necessário ajustar e validar os recortes no Aseprite antes de usar como tileset final.
