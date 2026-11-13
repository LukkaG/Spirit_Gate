# Proporção das sprites — 0.200.6

A escala numérica não é comparável entre atlas de densidades diferentes. A Reaper possui canvas 64 × 64 e altura visível de 52–54 pixels. O Human possui canvas de até 153 × 153 e altura visível de 134–142 pixels. Com ambos em 1x, o Human fica muito maior.

O multiplayer usa agora **Reaper 1x e Human 0,4x**, aproximando ambos de 54 pixels de altura visível. A Reaper é desenhada pixel a pixel a partir do original; a redução do Human descarta parte do detalhe na apresentação. Ambos preservam proporção, pivô dos pés e PNGs originais. A escala visual não muda hitbox, velocidade ou alcance. O modo offline permanece como antes.

Prévia com o renderizador real e poses frontais: `.task-checkpoints/proporcao-sprites-0.200.6/same-height.png`. Quatro verificações específicas passaram, incluindo as 36 poses Reaper sem alteração de pixels e diferença máxima de seis pixels na altura visível entre as poses dos dois atlas. Os testes de escala 1,5x e animação foram preservados; o padrão passou a ser 1x.

Pacotes em `dist/SpiritGate-Launcher-Grafico-0.200.6`. Feche jogo, servidor e launcher anteriores, abra o EXE do Anfitrião e envie o ZIP Convidado da mesma versão ao amigo. ZIPs, manifesto e todos os arquivos distribuídos foram conferidos por SHA-256. A entrega inclui as melhorias de rede da 0.200.5 e continua sem canal online publicado.
