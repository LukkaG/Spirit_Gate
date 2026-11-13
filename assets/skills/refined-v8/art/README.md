# Fontes V8

Rota efetiva: `image_gen` nativo. Dez gerações/edições preservadas; modelo e custo não foram retornados. `prompts.json` e `trail-prompts.json` registram os pedidos.

| Fonte aceita | Referência |
| --- | --- |
| `thunder-cut.png` | Frames de `refined-v2/thunder_1.png`, remontados em grade para edição; também usado pela corrente. |
| `thunder-spear.png` | `refined-v2/thunder_2.png`, remontado em grade. |
| `thunder-dash-alpha.png` | `refined-v2/thunder_4.png`, remontado; edição de `thunder-dash.png` para transparência. |
| `thunder-circuit-padded.png` | `refined-v2/thunder_5.png`; edição de `thunder-circuit.png` para margens. |
| `thunder-impact.png` | `refined-v4/art/impact.png`, com paleta quente. |
| `martial-slam.png` | Estilo das mãos/traços de `refined-v4/art/martial-punch-padded.png`; formas novas de pegada e pancada. |
| `water-trail.png` | Cor/acabamento de `refined-v4/water_5.png`; fragmentos compactos próprios. |
| `wind-trail.png` | Cor/acabamento de `assets/skills/wind_3.png`; fragmentos compactos próprios. |

As grades de referência remontadas estão no checkpoint `refinamento-v8/`. As oito fontes aceitas alimentam nove atlas/67 frames: a folha marcial tem oito frames de pegada e oito de pancada. As duas fontes anteriores às correções elétricas permanecem como evidência, mas não alimentam os atlas finais.

O empacotamento apenas recorta, registra e reduz com nearest. Correções de cor e transparência foram pedidas à ferramenta de imagem, não pintadas por Python. Os resíduos de alfa quase transparente são usados somente para medir os limites de recorte; o alfa dentro da área exportada é preservado.
