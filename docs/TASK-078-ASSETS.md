# TASK-078 — Inventário e avaliação dos assets

Os 12 arquivos reais de `assets/dev/player-base/` foram decodificados pelo validador PNG existente, que verifica assinatura, CRC, dimensões e pixels. Todos possuem alfa com pixels totalmente transparentes; nenhum possui alfa parcial. Há margens transparentes diferentes entre arquivos. Os originais foram preservados byte a byte.

A folha de inspeção `.task-checkpoints/TASK-078/assets-contact.png` foi aberta e examinada: Front apresenta o rosto; Back apresenta as costas; Left e Right apresentam os perfis correspondentes; os pares Walk têm poses distintas e orientação coerente. Isso é inspeção técnica das imagens, sem aprovação artística humana em nome do responsável.

## Divergência de nomes encontrada

Nove nomes coincidem exatamente com a lista recebida. Os três abaixo diferem em capitalização. O contrato usa explicitamente o nome REAL, inclusive em Windows, sem renomear arquivos ou aceitar qualquer nome por comparação case-insensitive:

| Lista da tarefa | Arquivo real preservado |
| --- | --- |
| `Walk_Character_Front1.png` | `Walk_character_Front1.png` |
| `Walk_character_Left1.png` | `Walk_Character_Left1.png` |
| `Walk_character_Left2.png` | `Walk_Character_Left2.png` |

## Escala, margens e pivô

Nenhuma imagem foi cortada, regravada, redimensionada em disco ou espelhada. A adaptação separada desenha o PNG completo em escala uniforme 1/2 via nearest-neighbor. A altura visível resultante fica aproximadamente entre 67 e 71 pixels do mundo, adequada ao ensaio no cenário 960×640; não é uma decisão artística definitiva. O canvas varia entre 132×148 e 163×154, incluindo imagens 155×159; não há grade 64×64 comum.

Para cada imagem, o pivô de desenho foi registrado como centro horizontal do retângulo não transparente e primeira linha após seu último pixel visível. Isso elimina diferenças de margens e alinha a base da silhueta à posição lógica, com arredondamento máximo de meio pixel do mundo. Não se usa o centro geométrico do canvas como posição dos pés. A escala é a mesma para todos os frames, sem normalizar cada silhueta para uma altura artificialmente idêntica.

Os testes verificam os 12 desenhos, alinhamento da base e centro, transparência e imutabilidade do estado. A escolha por limites alfa é uma aproximação técnica: extensão das pernas, detalhes soltos e diferenças de proporção originais ainda precisam de avaliação humana em movimento; não se alega eliminação artística de todo possível efeito de balanço/deslizamento.

## Associação e proteção

Chaves `DIRECTION.0`: Idle; `.1` e `.2`: Walk 1/2, na ordem nominal dos arquivos. Os metadados estão em `server/src/main/resources/rpg/experimental-player.properties`. SHA-256 é conferido antes da alocação da imagem, seguido por dimensões e canal alfa. Arquivo ausente, capitalização diferente, corrupção ou troca entre dois PNGs recusam o conjunto e conservam geometria. A carga fica em memória, sem acesso aos arquivos por tick/repaint. Substituição futura de arte requer revisão e atualização explícita dos metadados; editar os PNGs não os ativa silenciosamente.

Não foram usados o contrato oficial de 59 frames, importação Aseprite nem transformação derivada dos originais. A única derivação visual é a folha de contato de evidência.

## Dimensões e hashes reais

Limites visíveis abaixo são inclusivos, em pixels do PNG original. O JSON completo inclui contagem de pixels transparentes e está em `.task-checkpoints/TASK-078/asset-inspection.json`.

| Arquivo | Canvas | Limites visiveis x0,y0,x1,y1 | SHA-256 |
| --- | --- | --- | --- |
| `Main_Character_Idle_Back.png` | 153x153 | [42, 10, 111, 145] | `81d93e06334d60b36b553eb6ff6e086c1d6c92658e6a70012a6a2b2a65464d6e` |
| `Main_Character_Idle_Front.png` | 153x153 | [45, 11, 111, 144] | `2daf77863ea6b0c6b7840c6a83e682e16e79564b160eced24858b1526b38297c` |
| `Main_Character_Idle_Left.png` | 153x153 | [48, 10, 106, 143] | `0795c957fb19c81c64d4881308fa2e3c29fffaf2a43601f6134efd32034fe192` |
| `Main_Character_Idle_Right.png` | 153x153 | [47, 12, 105, 145] | `eb0c0a64d4e7009d7415e5f0c54dc99aec1c6e140b50b2d7990da10cafaa7ec2` |
| `Walk_Character_Back1.png` | 142x153 | [31, 7, 98, 147] | `ff83b04e6d2f95dbbaa4017b39af6a4d9c61d421fd0585d0f27bd119981a1408` |
| `Walk_Character_Back2.png` | 142x153 | [34, 10, 103, 151] | `2932e4aa72430fdf72e243eef3136d315014de3f02741de3d7a6f0d5e4f132c3` |
| `Walk_character_Front1.png` | 132x148 | [33, 3, 102, 144] | `8bb5c12853fa084db51624a99ee3b350f342531a21a0dbd8b1deb5d37ee3168b` |
| `Walk_character_Front2.png` | 132x148 | [35, 5, 103, 144] | `8dfb422cf689a2a59e6953c20f5fae1732a23384e8bb65fa2a84733ed89a0d51` |
| `Walk_Character_Left1.png` | 163x154 | [45, 7, 112, 143] | `a1bcbf3de8714a1ff9785a6ede1d07805f31f65fd042ea26477efbded77a4c93` |
| `Walk_Character_Left2.png` | 163x154 | [41, 7, 117, 143] | `68369a5834cd08c9a0dd0920857b977d6bea52441b8f1b1613b2f3d15891f560` |
| `Walk_character_Right1.png` | 155x159 | [40, 12, 107, 148] | `74e7382a3287a83eee87fc6fc2bda6b195956793865f6223b4ad0ba37ad55176` |
| `Walk_character_Right2.png` | 155x159 | [37, 7, 113, 143] | `1e742253e9b75db53e37f2fbc08ad2dbf4ae934b83f78ae15b9952438c823ce9` |
