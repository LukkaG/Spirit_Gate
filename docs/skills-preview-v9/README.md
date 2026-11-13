# Prévia V9 — corte frontal e fase do trovão

Abra `tools/testar-skills.cmd` e use **Trocar kit / reiniciar treino** sem fechar a janela principal.

1. Água slot 5, Vento slot 3 e Raio slot 4: desselecione o alvo, olhe para cada direção e conjure. Confira o avanço, o corte ligado ao corpo e o rastro que nasce atrás dele. Atravesse um mob e experimente uma parede; cada mob recebe um acerto por passagem.
2. Raio slot 5: o personagem desaparece dentro do círculo dourado por cerca de 1,07 s. Um aliado pode ocupar seu lugar e mobs não podem feri-lo nessa fase. Ao terminar surgem quatro cortes; todo o círculo causa dano, respeitando paredes.
3. Confira o retorno da sprite, da colisão e da vulnerabilidade. Se o aliado ocupou a origem, o personagem reaparece em um apoio livre próximo.

Capturas locais geradas pelo teste com duas janelas TCP:

- [Dash de raio](runtime/thunder_4-rastro.png), [observador](runtime/thunder_4-observador.png).
- [Dash de água](runtime/water_5-rastro.png), [vento](runtime/wind_3-rastro.png).
- [Fase invisível](runtime/raio-invisivel.png), [observador](runtime/raio-invisivel-observador.png).
- [Cortes finais](runtime/raio-quatro-cortes.png), [observador](runtime/raio-quatro-cortes-observador.png). A captura registra um instante da sequência, não todos os quatro cortes simultaneamente.

As imagens preservam a HUD real do jogo. [Relatório técnico e limitações](../SKILLS-REFINAMENTO-V9.md). Aprovação visual e teste físico permanecem pendentes.
