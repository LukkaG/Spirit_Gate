# Fontes dos cortes V6

Rota: `image_gen` nativo. Modelo e custo não foram informados pela ferramenta. As oito gerações/edições são preservadas; somente quatro folhas aceitas alimentam os atlas.

| Família | Fonte aceita | Referências e revisão |
| --- | --- | --- |
| X de fogo | `fire-cross.png` | Silhueta dos X de `C:/Users/lucas/Downloads/animations/304.png`, conforme referência do usuário; arte de fogo nova. |
| Giro de espada/água | `water-spin-isolated.png` | Arcos da referência anexada, anel `277.png` e cor de `refined-v4/art/water-cut.png`. A primeira geração `water-spin.png` foi rejeitada por cruzar células; a edição isolou os frames. |
| Sísmico discreto | `seismic-transparent.png` | Rachaduras de `225.png`, a terceira referência do usuário. `seismic-clean.png` ficou linear; `seismic-radial.png` corrigiu a distribuição; `seismic-readable.png` melhorou contraste mas gerou fundo indevido, removido por nova edição nativa. |
| Três lâminas de vento | `wind-triple.png` | `275.png`, correspondente à quarta referência anexada. Novas formas e progressão de nascimento/dissipação. |

Prompts em `prompts.json`, `edit-prompts.json` e `contrast-prompts.json`. O script de empacotamento conserva escala única por sequência, registra pivôs e reduz com nearest. Não repinta ou recolore por Python; a extração do fundo do sísmico foi feita pela ferramenta nativa de imagem. Os resíduos de alfa 1/2 nos gutters são ignorados apenas na medição do recorte; o alfa dentro do recorte é preservado. O contato do vento reutiliza seis frames finais da mesma fonte, sem desenho adicional.
