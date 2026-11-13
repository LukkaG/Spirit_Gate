# V8 — agarrão, raio dourado e rastros dos dashes

[Prancha](contact.png) · [QA](qa.json) · [Integridade/reprodução](verification.json) · [Relatório técnico](../SKILLS-REFINAMENTO-V8.md)

## Agarrão e body slam — Martial Artist 4

[Pegada](martial_4.gif) · [Pancada e dissipação](martial_4.impact.gif) · [Captura da pegada](runtime/agarrão.png) · [Pancada no jogo](runtime/body-slam.png) · [Observador](runtime/body-slam-observador.png)

Duas fases de efeito sobre os personagens existentes: as mãos fecham a pegada e fazem um arco de lançamento; depois vêm o golpe no chão, pequenas lascas e poeira baixa. A pegada dura 200 ms e a pancada/dissipação 360 ms. O ponto da pancada é o alcançado pelo deslocamento autoritativo, inclusive se houver parede ou outro corpo. Dano, stun, energia e recarga continuam com as regras anteriores; a apresentação não adia o dano. Não foram adicionadas novas poses corporais de personagens.

## Raio em amarelo/laranja

[Corte/corrente](thunder_1.gif) · [Lança](thunder_2.gif) · [Textura da investida](thunder_4.gif) · [Ícone do circuito](thunder_5.gif) · [Contato elétrico](thunder_1.impact.gif)

Os cinco slots e os impactos receberam núcleo claro, canais amarelos e bordas âmbar/laranja. A corrente compartilha a folha do corte, com seu timing próprio. As silhuetas elétricas preservam a proposta anterior, sem copiar personagens das referências citadas. O círculo é usado como arte do slot; durante o circuito, o efeito acompanha os quatro deslocamentos reais.

## Rastros que acompanham o corpo

| Movimento | Fragmento/textura | Captura |
| --- | --- | --- |
| Água 5 | [Fragmentos](water_5.travel.gif) | [Lançador](runtime/water_5-rastro.png), [observador](runtime/water_5-observador.png) |
| Vento 3 | [Fragmentos](wind_3.travel.gif) | [Lançador](runtime/wind_3-rastro.png), [observador](runtime/wind_3-observador.png) |
| Raio 4 | [Textura](thunder_4.gif) | [Lançador](runtime/thunder_4-rastro.png), [observador](runtime/thunder_4-observador.png) |
| Raio 5 | Mesma textura elétrica, respeitando as curvas | [Segundo trecho](runtime/circuito-segundo-trecho.png), [quarto trecho](runtime/circuito-quarto-trecho.png) |

O rastro nasce somente entre posições já exibidas pelo corpo, não entre o início e o destino previsto. Os fragmentos mantêm espaçamento ao longo do caminho e desaparecem gradualmente em até 220 ms; com movimento reduzido, em 80 ms. O corpo fica livre de sobreposição. Término antecipado, portal, morte, revisão ou desconexão não deixam uma faixa permanente. Uma correção grande de posição não desenha uma linha falsa atravessando o mapa.

As fotos mostram a integração Swing/TCP; os GIFs mostram as texturas repetidas para inspeção, não o trajeto completo do dash. A legibilidade final e o tamanho dos efeitos continuam sujeitos à revisão humana.

## Fontes

Dez gerações/edições com `image_gen` nativo foram preservadas em `assets/skills/refined-v8/art/`, com prompts e variantes. Oito fontes aceitas alimentam nove atlas/67 frames exportados. O empacotamento apenas recorta, registra e reduz com nearest. Não recolore por Python, não amplia fontes e não substitui PNGs anteriores. Modelo/custo não informados pela ferramenta ficam nulos na proveniência.

A reprodução independente gerou nove PNGs e dois metadados idênticos. Os 133 PNGs anteriores e os 415 originais da pasta fornecida permaneceram iguais por hash. Para carregar, reabra `tools/testar-skills.cmd` uma vez; a troca de domínios na mesma janela continua disponível.
