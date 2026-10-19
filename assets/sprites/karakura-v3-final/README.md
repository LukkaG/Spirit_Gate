# Recortes finais

55 objetos extraídos do Aseprite fornecido, sem redimensionar ou modificar suas cores. O script usa componentes conectados de transparência, agrupa ilhas internas e associa fragmentos pequenos próximos, incluindo pétalas. Todos os pixels visíveis foram preservados e cada arquivo individual passou pela verificação de leitura após gravação.

- `karakura-v3-camadas.aseprite`: atlas na disposição original, com um objeto por camada.
- `sprite-001.aseprite` até `sprite-055.aseprite`: objetos individuais recortados nas suas dimensões reais.
- PNGs equivalentes para importação no jogo.
- `manifest.json`: coordenadas e tamanhos na imagem original.
- `atlas-verificacao.png`: reconstrução dos pixels visíveis do original.

Os terrenos não foram convertidos para 32×32 nem ajustados para repetição. Eventuais falhas já existentes na remoção manual do fundo são preservadas: em especial, revise a peça de calçada `sprite-029`, que contém lacunas internas no arquivo de origem. Não são recuperados pixels apagados anteriormente.

Para executar novamente, na pasta Projeto-K:

```powershell
python tools/separar_sprites.py assets/tilesets/karakura-environment-v3-blend.aseprite assets/sprites/novo-recorte
```

Python 3, sem dependências externas. Use uma pasta de saída nova para não sobrescrever recortes existentes. Os diretórios `karakura-v3-separated` e `karakura-v3-recortes` são verificações intermediárias; esta pasta `karakura-v3-final` é a entrega completa.
