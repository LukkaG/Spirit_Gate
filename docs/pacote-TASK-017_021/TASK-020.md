# TASK-020 - Câmera, viewport e coordenadas de entrada

**Objetivo:** preparar a arena para mapas maiores preservando colisão, mira por mouse, pixel art e comportamento padrão de 960x640.

## Escopo

- Auditar coordenadas de mundo, tela, origem da câmera, conversão do mouse, escala do Java2D e o desenho do HUD. Apresentar plano antes de editar.
- Introduzir transformação determinística mundo <-> tela: câmera e zoom inteiro onde viável, limites e recorte, com API pura e testes de ida/volta. Resolução padrão deve manter visão atual e coordenadas equivalentes às anteriores.
- Caso o mapa local aceite dimensões superiores à viewport, seguir o jogador sem revelar regiões fora dos limites; caso não aceite, habilitar apenas a transformação/câmera e um cenário sintético nos testes, sem mudar schema de produção de forma incompatível.
- Preservar a mira: Shift + mouse e Q + mouse devem usar coordenadas de mundo corretas após deslocamento da câmera/escala. Revisar bordas, diagonais e janela redimensionada.
- Renderer continua puro, sem modificar mundo; Hitboxes (`H`), trail, HUD, soft/solid obstacle e sprites opcionais devem manter alinhamento visual. Nenhuma mudança em alcance/custo/dano/física.
- Cobrir por testes: viewport padrão, câmera deslocada, zoom, redimensionamento, coordenadas de ataque e colisões, render repetido sem mudança no snapshot e reinício R.

## Aceite

- Baselines antigos verdes; screenshot(s) de cenário sintético opcional e testes determinísticos confirmam alinhamento e mira.
- Nenhuma nova mecânica de corrida, dash ou combate; saída padrão mantém aparência e controle anteriores.
