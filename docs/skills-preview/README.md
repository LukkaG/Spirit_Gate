# Prévias das habilidades

Quarenta GIFs, oito contact sheets, oito folhas com todos os frames e um teste sobre fundo claro mostram os mesmos frames usados pelos PNGs de runtime. Os GIFs usam fundo navy apenas para inspeção; os PNGs preservam alfa real.

As fontes são imagens fornecidas pelo desenvolvedor, com uma variante editada pelo image_gen em cada família. Recorte, escala uniforme, rotação e composição adaptam sua apresentação. O script de empacotamento não redesenha nem recolore a arte. A proveniência e os retângulos de origem ficam em `assets/skills/provenance.json`.

Reproduzir (PowerShell):

```powershell
python tools/prepare_skill_assets.py --source-dir 'C:\Users\lucas\Downloads\animations'
```

Para atualizar exclusivamente este bundle, acrescentar `--replace-generated`. As variantes aprovadas são lidas de `assets/skills/variants/variants.json` (opcionalmente `--variant-map`). Uma variante aprovada da folha de água pode ser fornecida por `--water-variant <arquivo.png>` com o mesmo grid 3×3. O gerador preserva arquivos de origem e recusa PNGs de saída alterados manualmente.

Os tempos, composição e dimensões são valores iniciais de apresentação. Dano, colisão, alvos e duração dos status permanecem sob autoridade do servidor. A homologação visual dentro do cliente e em rede continua necessária.
