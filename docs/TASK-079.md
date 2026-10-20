# TASK-079 — Refinamento visual do personagem principal

Data: 19/10/2026, America/Sao_Paulo. Base: integração TASK-078 e adaptações locais do atualizador já existentes. Escopo: somente escala e duração experimental de Walk no RPG offline. Estado técnico: concluída, aguardando homologação visual do responsável.

## Parâmetros implementados

| Propriedade JVM | Padrão novo | Valores aceitos | Retorno ao anterior |
| --- | ---: | --- | ---: |
| `spiritgate.rpg.spriteScale` | `0.4` | número finito entre `0.1` e `1.0`, inclusive | `0.5` |
| `spiritgate.rpg.spriteFrameMillis` | `220` | inteiro entre `30` e `2000` ms | `150` |

Escala é uma fração com ponto decimal, não porcentagem escrita como `40`. O mesmo fator é aplicado aos dois eixos do PNG completo. Posição de desenho: arredondamento de `posição lógica − pivô × escala`. O desenho mantém nearest-neighbor e alfa. Pivôs e imagens originais não foram modificados. A quantização de rasterização pode variar em até aproximadamente um pixel; os testes admitem margem de dois pixels para limites visíveis reduzidos.

Valores inválidos, NaN, infinito, texto ilegível ou duração fora do limite recusam a apresentação experimental com diagnóstico e fallback geométrico. Não há correção silenciosa para outro valor. O modo geométrico continua disponível sem validar parâmetros de sprites quando a opção experimental estiver desligada.

O sequenciador `RpgPlayerAnimation` não precisou de alteração: continua consumindo snapshots e ticks a 30 Hz, com dois frames por direção, reinício na mudança de estado/direção, Idle sem deslocamento efetivo e preservação da última direção. Repaint não altera o relógio. O contrato oficial de 59 frames/100 ms permanece intacto.

## Arquivos alterados e criados

- Alterado `RpgPlayerSprites.java`: escala configurável, aplicação aos pivôs, defaults 0,4/220, sobrecargas para testes e validação/fallback.
- Alterado `testar-rpg-sprites.cmd`: novos defaults e variáveis de ambiente opcionais `SPIRIT_GATE_SPRITE_SCALE` e `SPIRIT_GATE_FRAME_MS`.
- Criado `RpgPlayerRefinementTest.java`: seis testes específicos adicionais, sem alteração dos testes anteriores.
- Quatro documentos TASK-079, logs, capturas, GIFs e checkpoints próprios.

Não foram alterados mundo, runtime, combate, hitboxes, colisão, saves, progressão, Coop, protocolo, arena, atlas oficial, direção/associação de frames ou PNGs. Alterações locais anteriores permaneceram presentes; o diff próprio é calculado contra o checkpoint do início desta tarefa, não contra um Git que já continha outras alterações pendentes.

## Checkpoints e limites

`.task-checkpoints/TASK-079/TASK-079-before.json` e `TASK-079-before.zip` foram criados antes das alterações. Saves pessoais entram apenas por hash no inventário e não no ZIP. `TASK-079-after.json`, ZIP posterior, `preservation.json` e `TASK-079.diff` fecham a rastreabilidade.

Nenhuma nova release, build portátil, publicação do canal `updates`, push, TASK-080 ou animação adicional foi executada. Os pacotes já publicados continuam com a configuração anterior; esta entrega é local para avaliação antes de nova autorização.
