# Efeitos V4 — contato, solo e arte detalhada

[Prancha dos efeitos](contact.png) · [QA](qa.json) · [reprodução e integridade](verification.json)

Nove atlas/75 frames, compartilhados por metadados de impacto e fases auxiliares. Oito folhas novas foram geradas/editadas com `image_gen` nativo, com três correções de margem. As variantes, prompts e referências ficam em `assets/skills/refined-v4/art/`. As fissuras partiram das referências 53/343 examinadas no diretório fornecido; os 415 originais foram inventariados novamente.

O script `tools/prepare_skill_assets_v4.py` faz recorte, registro de pontos/bases, composição e redução nearest. Nenhuma fonte é ampliada; as dimensões de desenho também ficam menores que o canvas exportado. Não há repintura ou recoloração por Python. O círculo de dano sísmico continua 110; a nova arte mantém a dimensão de apresentação 320×320 da V3.

As linhas retas de separação da folha de fissura foram excluídas pelos retângulos de recorte, sem alterar a arte. A folha de vórtice usa faixas de altura desigual e registro da base para manter os frames completos. QA não encontrou frames vazios, pixels na borda ou avisos. A reprodução em pasta separada gerou os nove PNGs e dois metadados idênticos; 91 PNGs anteriores e os 415 originais ficaram intactos.

Capturas de dois clientes Swing/TCP reais:

- [Fissura parcial](runtime/fissura-parcial.png), [trilha no observador](runtime/fissura-chao-observador.png) e [erupção](runtime/fissura-erupcao.png).
- [Queda das flechas](runtime/chuva-em-queda.png) e [impacto no chão](runtime/chuva-impacto-chao.png).
- [Soco](runtime/soco-detalhado.png), [sequência](runtime/sequencia-socos.png) e [sísmico com nova fonte](runtime/sismico-resolucao-nativa.png).
- [Prisão de água](runtime/prisao-agua-polida.png), [impacto do corte](runtime/impacto-corte-agua.png) e [vórtice](runtime/vortex-agua-polido.png).
- [Três lançamentos de vento](runtime/vento-tres-lancamentos.png).

Os tamanhos, detalhes e balanceamento ainda precisam da revisão humana em jogo. Esses efeitos não acrescentam animações corporais novas aos personagens.
