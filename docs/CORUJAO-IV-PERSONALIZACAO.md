# Aparência por camadas — experimental

World Studio → Personagem → Aparência temporária abre um perfil sintético. Selecione cabelo, roupa, direção e Idle/Walk; `none` remove a camada. A prévia é ampliada 3×, mas a escala de gameplay do corpo continua 0,5 e o Walk continua 250 ms.

Ordem de composição: cabelo traseiro, corpo original, roupa e cabelo frontal. Cada pose usa seu pivô declarado. Não há cópia do corpo por combinação, deformação, deslocamento de pés, mudança de hitbox ou atributo de combate. Imagens são carregadas/validadas em worker e a pintura consulta apenas memória. Cache cosmético limitado a 64 MiB; recursos ausentes ou divergentes geram aviso e fallback por camada.

O catálogo único é o manifesto versionado dos assets temporários. IDs são validados pelo catálogo, sem inferir raça/domínio por imagem. Perfis de preview usam `studio-projects/appearance-preview.properties`, schema cosmético V1, separados do save V2 e sem vínculo automático com personagem pessoal. I/O ocorre fora da EDT.

Testes percorrem todas as combinações de três cabelos e três roupas, quatro direções e frames Walk, além de Idle, ausência de camada, ID inválido e roundtrip de perfil. Capturas em `.task-checkpoints/TASK-157/corujao-iv/preview/`. Aprovação artística continua pendente.
