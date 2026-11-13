# Refinamento visual V2

Overlay parcial: 13 habilidades revisadas e cinco fases auxiliares, sem substituir o bundle V1. Dez folhas novas foram editadas pelo image_gen nativo a partir dos PNGs fornecidos; os prompts e versões ficam em `assets/skills/refined-v2/art/`. Fogo reutiliza também o corte aprovado e a erupção da fonte249.

O empacotamento usa retângulos explícitos, registro declarado, enquadramento comum e escala uniforme nearest. Não há repintura, recoloração ou alteração de alfa pelo Python. A lança usa faixas de recorte com alturas diferentes para respeitar os intervalos transparentes da geração.

Reproduzir: `python tools/prepare_skill_assets.py --source-dir 'C:\Users\lucas\Downloads\animations' --refined-v2`. Para regenerar este pacote, acrescentar `--replace-generated`; o script recusa PNGs alterados manualmente. O manifesto é publicado atomicamente após todos os PNGs e QA.

Alvos, duração, dano, colisão e trajetórias permanecem definidos no servidor. Draw sizes são apresentação, não hitboxes. GIFs usam fundo navy apenas para inspeção. Conferir o resultado integrado no cliente e em rede.
