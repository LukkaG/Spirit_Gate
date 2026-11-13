# Cortes V6 — fogo, giro de água, sísmico e vento

[Prancha](contact.png) · [QA](qa.json) · [Reprodução e integridade](verification.json)

| Efeito | Animação | Captura no jogo |
| --- | --- | --- |
| Fogo 5 — cortes em X | [GIF](fire_5.gif) | [Ápice](runtime/fire-apice.png) |
| Água 2 — arco girando ao redor do corpo | [GIF](water_2.gif) | [Ápice](runtime/water-apice.png) |
| Martial 3 — rachaduras simples | [GIF](martial_3.gif) | [Ápice](runtime/martial-apice.png) |
| Vento 4/5 — três lâminas | [GIF](wind_4.gif) | [Ida](runtime/vento-ida.png), [contato](runtime/vento-contato.png), [retorno](runtime/vento-retorno.png) |

Quatro folhas novas, 12 frames por família, mais seis frames compartilhados de dissipação do vento: cinco atlas/54 frames exportados, sem ampliar as fontes. Os GIFs repetem apenas para inspeção. No jogo, os cortes se formam e se dissipam; a chuva de fogo apresenta uma nova onda por pulso existente. Os cinco X de cada onda são visuais e não multiplicam o dano da área.

O giro de água tem centro transparente e fica alinhado ao centro visual do corpo. O sísmico mantém chão vazio entre oito rachaduras principais, sem a explosão de pedras anterior. A erupção e os dois vórtices aprovados na V5 não foram substituídos.

O vendaval de repulsão agora é skillshot frontal, dispensa seleção e só cria checkpoint ao acertar. A volta é target, exige o inimigo da quarta habilidade e apresenta as lâminas no sentido do checkpoint ao usuário. O raio frontal de contato é 18 mais o corpo do inimigo; a área de repulsão permanece 70 e o deslocamento continua respeitando paredes e corpos atuais.

As capturas foram feitas com servidor local e dois clientes Swing/TCP. A aparência final continua sujeita à revisão humana. Consulte o [relatório técnico](../SKILLS-REFINAMENTO-V6.md) para testes, compatibilidade e reversão.
