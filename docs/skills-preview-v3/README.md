# Efeitos V3

Sete atlas e 63 frames, gerados/editados com `image_gen` nativo e empacotados por `tools/prepare_skill_assets_v3.py`. Todas as flechas de Aetheri usam energia azul sem gelo. Martial 5 possui sequência própria de punhos e chutes; Martial 3 herda o PNG V2 com apresentação ampliada para 320×320.

- [Prancha](contact.png), [QA](qa.json), [reprodução](reproduction.json), [integridade dos anteriores](integrity.json).
- [Básico de energia partindo do centro](runtime/flecha-energia-centro.png).
- [Sentinela ativa](runtime/sentinela-ativa.png).
- [Impacto sísmico ampliado](runtime/impacto-sismico-ampliado.png).
- [Combo de punhos e chutes](runtime/combo-punhos-chutes.png).

Prompts e variantes: `assets/skills/refined-v3/art/`. A reprodução em diretório separado confirmou PNGs e metadados idênticos. O preparador somente recorta, registra, escala nearest e compõe as fontes; não repinta por código. Os originais e as revisões anteriores ficam preservados. Arte e balanceamento ainda dependem da revisão humana em jogo.
