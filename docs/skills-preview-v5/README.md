# Ciclos V5 — erupção e vórtices

[Prancha](contact.png) · [Ciclos dentro do jogo](runtime/ciclos-no-jogo.png) · [QA](qa.json) · [Integridade e reprodução](verification.json)

Três sequências novas, com 12 frames cada, geradas com `image_gen` nativo. Os efeitos nascem pequenos, atingem o ápice e voltam a poucas partículas. As fontes, referências e prompts ficam em `assets/skills/refined-v5/art/`. O empacotamento apenas registra apoios, recorta e reduz com nearest; não amplia, repinta ou recolore as fontes. As regiões das folhas têm alturas diferentes e usam retângulos explícitos, sem cortar o núcleo dos efeitos.

| Efeito | GIF no tamanho de desenho | Captura do ápice |
| --- | --- | --- |
| Fogo 4 — erupção | [Animação](fire_4.gif) | [Jogo](runtime/fire_4-jogo-apice.png) |
| Fogo 3 — vórtice | [Animação](fire_3.gif) | [Jogo](runtime/fire_3-jogo-apice.png) |
| Água 4 — vórtice | [Animação](water_4.gif) | [Observador](runtime/water_4-observador-apice.png) |

Os GIFs repetem para facilitar a inspeção. No jogo cada efeito percorre o ciclo uma vez. Os vórtices distribuem os frames pelos 40 ticks existentes; a erupção mostra 1.200 ms de animação depois do contato confirmado. A escala é única por sequência: os estágios pequenos não recebem zoom individual. O pivô no chão fica registrado também nesses estágios.

Os primeiros frames têm respectivamente 2,82%, 0,96% e 2,61% da cobertura opaca do ápice; os últimos têm 1,53%, 0,36% e 0,99%. QA: 36 frames, nenhum vazio, nenhuma borda ocupada e nenhuma ampliação. Esses números verificam o envelope; a avaliação estética continua humana.

Os testes produziram capturas de início, ápice e fim, tanto isoladas em transparência quanto em dois clientes Swing/TCP. Não são provas de Radmin ou launcher nativo. Consulte o [relatório V5](../SKILLS-REFINAMENTO-V5.md) para escopo, testes e reversão.
