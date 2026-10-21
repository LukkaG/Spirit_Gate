# Spirit Gate - tela de entrada

Tela escolhida e implementada: [login Portal Urbano](PORTAL-URBANO-LOGIN.md), com formulário adaptado à porta circular e captura real. A arte e seu prompt estão em [portal urbano v3](URBAN-DOOR-V3.md). A versão [cartoon v2](CARTOON-V2.md) e a documentação original abaixo foram preservadas.

Arte original gerada com a ferramenta integrada imagegen e incorporada em `server/src/main/resources/rpg/login/spirit-gate-entry.png`. A interface e o titulo sao controles Swing reais, sobre a pintura; o emblema geometrico e desenhado pelo jogo.

Abra `jogar-rpg-offline.cmd` com o Java instalado e as classes compiladas para acessar esta versao. O script usa as classes atuais quando disponiveis e conserva o EXE anterior como alternativa. Os EXEs historicos em dist nao foram substituidos. Novos pacotes do script build-rpg-portable incluem a arte dentro dos recursos rpg.

ENTER abre a selecao de personagens locais. Username preenche a sugestao de nome na criacao; Remember Me guarda somente esse nome em saves/entry.properties. O modo atual nao autentica contas: Password esta desativado e Forgot Password explica o uso local. Nenhuma senha e recebida ou armazenada. Os personagens e os saves continuam no fluxo existente.

Preview real da interface: spirit-gate-entry-preview.png (1280 x 720). Exportacao: `java -cp server/target/classes com.projetok.prototype.rpg.SpiritGateEntry docs/login/spirit-gate-entry-preview.png`.

## Prompt final (imagegen integrado)

Create a production background asset for the original game SPIRIT GATE. Wide 16:9 cinematic premium detailed digital painting, original dark spiritual fantasy anime-inspired environment. Midnight navy, black, cold grey, ethereal cyan and icy white palette. Monumental ancient ruined spiritual portal, glowing blue energy, enormous moon veiled by storm clouds, distant sanctuary towers, ritual stone pillars and hanging chains, blue spirit flames, drifting luminous particles, ground covered with shallow reflective water and mist. Strong depth, elegant dramatic supernatural lighting, memorable epic landscape between material and spiritual worlds. Balanced composition framing a central login interface: keep the center third dark and low detail, with mystical architecture to either side and bright celestial portal behind the upper center. Foreground wet stone and luminous mist. No characters, no skull emblems, no recognizable franchise symbols. NO TEXT, NO LOGO, NO UI FIELDS, NO BUTTONS: real crisp title SPIRIT GATE and functional login controls will be rendered by the game above this background. This is the clean background layer for the user's requested login concept and actual game integration.

## Botao ENTER estatico

A pedido do usuario, toda a animacao foi removida. `SpiritFlameButton` agora desenha uma arte fixa com chamas azuis nos cantos e o texto ENTER em Swing. Nao ha timers, simulacao, pulsacao ou particulas animadas. Clique, teclado e indicador de foco continuam funcionando.

Arte: `server/src/main/resources/rpg/login/enter-blue-flames-static.png`, gerada com a ferramenta integrada imagegen a partir da ultima referencia anexada pelo usuario. A moldura e enquadrada ao desenhar para remover as margens vazias da tela gerada sem alterar o PNG original.

Previa atual: `spirit-gate-entry-preview.png`. A previa GIF e os utilitarios da simulacao anterior foram retirados.

### Prompt final do botao

Use the user's last attached image as the visual reference. Create ONE STATIC game login button asset closely matching its style and layout. Extremely wide short horizontal rectangle, approximately 6.3:1 aspect ratio, centered and nearly filling a wide canvas with only a tiny transparent margin for outer glow. Thin sharp rectangular neon cyan outline, dark midnight navy/black translucent interior. Detailed hand-painted bright cyan/icy white spectral flame clusters originate from lower-left and lower-right corners and lick diagonally inward/upward. Jagged organic hooked flame tips, branching tongues, dark negative-space gaps, small broken wisps and tiny glowing blue embers exactly in the spirit of the reference. Left and right clusters asymmetric and painterly, NOT repeated vector spear shapes, not geometric claws, not smooth ribbons. White-hot luminous roots, electric cyan midtones, deep blue fringes, tasteful luminous bloom. Each fire cluster occupies only outer 20-23 percent of the button, leave the center 50 percent empty and nearly black for the game's own ENTER label. NO LETTERS, NO WORDS, NO TEXT anywhere: the software renders ENTER separately. No surrounding scene, no mockup of entire screen, no extra controls, no stone border or ornaments. Transparent background OUTSIDE the rectangle and its glow. Polished dark fantasy game UI illustration, static finished asset, faithful to supplied reference composition.
