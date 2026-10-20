# TASK-077 — Contrato técnico de sprites encontrado no código

Este documento descreve o checkout auditado em 19/10/2026. Aprovação artística — exclusiva do responsável. Nenhum asset foi integrado ou aprovado nesta tarefa.

## Carregamento e organização efetivos

`DebugLauncher.defaultVisualManifest()` resolve `visual/character.properties` junto ao EXE (`jpackage.app-path`) ou na raiz de trabalho; chama `ValidatedSpriteAtlas.loadOptional` uma vez e entrega ao `DebugPanel.setPresentation`. PNG e recortes ficam em memória, sem I/O por frame. `assets/manifest-assets.json` e diretórios de sprites são inventário/desenvolvimento; a presença em assets não ativa arte no jogo. Ferramentas `validar_assets.py`, `revisar_sprites.py` e ferramentas de pacote visual fazem validação/revisão/exportação.

O RPG (`RpgLauncher`) e o Coop (`NetworkClientLauncher`) usam `DebugPanel` com snapshots/paletas geométricas, sem conectar `setPresentation`/atlas. Logo, exportar um PNG aprovado para a arena não o integra automaticamente aos personagens, NPCs, inimigos ou objetos do RPG/Coop. Mapas RPG são descritores sintéticos de `RpgMaps`, não tilesets PNG já integrados.

## Contrato atual do atlas Java

| Item | Regra efetiva |
| --- | --- |
| Manifesto | Java Properties, até 1.000.000 bytes; chaves duplicadas recusadas; `status=VALIDADO` |
| Imagem | Referenciada por `image`, hash SHA-256 exato; até 16.000.000 bytes; dimensões positivas até 4.096 por eixo, múltiplas de 64 |
| Decoder | `ImageIO` reconhece imagem; não há no loader exigência explícita de assinatura PNG apesar da convenção/nomenclatura PNG |
| Recortes | x/y não negativos, múltiplos de 64; largura/altura exatamente 64; dentro da imagem e sem reutilizar origem de outro frame |
| Pivô/offset | `pivotX/Y` inteiros 0..64; `offsetX/Y` com módulo até 4.096; desenho relativo — posição do ator |
| Manifesto versionado | `packageVersion=1`, assetId `[a-z][a-z0-9._-]{0,63}`, `frameSize=64`, `tileSize=32`, reviewOrigin não vazio, sha256 igual a approvedSha256 |
| Caminho | Imagem real deve permanecer dentro da pasta do manifesto; contrato versionado também recusa link simbólico direto |
| Renderização | Recorte 64×64 desenhado 64×64 no mundo, nearest-neighbor; transformações de viewport podem escalar apresentação; hitbox não vem do PNG |

O atlas versionado exige todos os 59 frames abaixo. O legado permite ausência de frames, usando geometria no lugar do frame ausente. Status/hashes de manifesto não substituem aprovação humana.

| Estado Java / chave de exportação | Frames por direção | Direções |
| --- | --- | --- |
| IDLE / IDLE | 1 | FRONT, BACK, LEFT, RIGHT |
| WALK / WALK | 4 | mesmas quatro |
| RUN / RUN | 4 | mesmas quatro |
| ATTACK / PUNCH | 3 | mesmas quatro |
| HIT / HIT | 2 | mesmas quatro |
| FAINT / KO | 3 | somente FRONT |

índices começam em 0; 100 ms por frame, tempo vindo do dt de simulação. IDLE/WALK/RUN repetem; ATTACK/HIT/FAINT param no último frame. A seleção automática de locomoção em `DirectionalAnimation.advance` usa WALK/IDLE: existência de RUN no contrato não comprova seleção automática por velocidade. Movimento diagonal escolhe eixo dominante; empate vertical; parado mantém direção. Não há oito direções visuais. Morte usa FRONT. Freeze pausa progressão visual. A ordem de coordenadas — explicitada no manifesto, não deduzida de uma grade universal de autoria.

## Ferramentas, mapas e fallback

O validador Python de pixels aceita PNG RGB/RGBA, 8 bits, não entrelaçado; grades 32 ou 64 no inventário, diferentes do contrato específico 64×64 de personagens do atlas. Importação Aseprite atual exige json-array, 59 frames em ordem/nome ACTION.DIRECTION.INDEX, 100 ms, sourceSize 64×64, sem trim, rotação ou packing incompatível. Esses limites pertencem ao importador atual; não são uma obrigação universal para toda arte futura.

Manifesto ausente retorna null. Imagem/manifesto ilegível, hash incorreto, status pendente ou recorte inválido gera recusa e fallback geométrico, com aviso stderr quando capturado como IOException. Resource do domínio ausente tem outro contrato e pode impedir a inicialização; não confundir com atlas opcional. O loader — uma barreira técnica, não um sistema de revisão artística.

`LocalArenaMap` separa retângulos visuais (camadas 0..10) e geometria física; mundo 960×640 até 8.192×8.192, até 4.096 descritores de cada tipo. Renderer usa imagens sintéticas em cache; tileSize=32 do pacote não cria automaticamente importador de tilesets de formatos variados. Hitbox/colisão permanecem independentes da largura do sprite.

Performance: atlas decodificado pode chegar a aproximadamente 64 MiB em ARGB a 4.096²; carregamento valida dimensões antes de alocar pixels. Frames são consultados por chave; busca de animação por ator e desenho geram trabalho por frame. Culling de ator — condicionado a atlas ausente em `DebugPanel`: ampliar população com atlas merece medição. Não foi feito benchmark novo e não se atribui ganho/perda numérica.

Testes executados: Java `DirectionalAnimationTest`, `VisualPackageTest`, `ScenarioLayerTest`, `CullingTest`, `ArenaViewportTest`, `LocalArenaMapTest`, `RpgMapsTest`; Python `test_assets`, `test_sprite_review`, `test_visual_package`, `test_mapas`. Fixtures sintéticas não aprovam arte real.

## Recomendações futuras, sem implementação

Inventariar separadamente tamanho, pivô, recortes, duração e direções de cada personagem, NPC, inimigo, objeto e tileset antes da integração. Se a arte não couber no contrato atual, projetar adaptação explícita e testes em tarefa autorizada; não recortar/reescalar silenciosamente todos os assets para 64×64. Definir seleção de atlas por entidade, oclusão/camadas, memória e comportamento de fallback no RPG/Coop. Revisar preview e aprovação vinculada ao hash com o responsável antes de ativar qualquer exportação.
