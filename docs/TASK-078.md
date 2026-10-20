# TASK-078 — Sprites experimentais do jogador offline

Data: 19/10/2026, America/Sao_Paulo. Base: checkout principal após TASK-077; HEAD e status inicial registrados em `.task-checkpoints/TASK-078/TASK-078-before.json`.

Implementação concluída com ressalvas de aprovação artística. O comportamento padrão continua geométrico. Nenhum pacote anterior foi recompilado ou substituído.

## Implementação

- `RpgPlayerSprites`: carregamento único e integral dos 12 originais, associação explícita de nomes/direções, verificação SHA-256 antes de decodificar, dimensões e alfa, escala uniforme 0,5 e pivôs individuais. Falha em qualquer arquivo recusa o conjunto inteiro e informa fallback em stderr.
- `RpgPlayerAnimation`: Idle/Walk a partir de posição, região e ticks publicados; quatro direções, dois frames Walk, 150 ms configuráveis, reinício em mudança de estado/direção e preservação da direção ao parar. Repaint não avança o relógio visual. Sem movimento efetivo, inclusive contra parede, usa Idle.
- `experimental-player.properties`: contrato próprio para estes 12 arquivos, incluindo nomes reais, hashes, dimensões e pivôs. Não representa aprovação artística definitiva.
- `DebugPanel`: somente um hook opcional de desenho de ator, nulo por padrão. Se não houver hook ou ele devolver false, segue exatamente a seleção anterior de atlas/geometria. Não depende de classes RPG.
- `RpgLauncher`: flag explícita, título identificável, atualização visual no timer da UI e instalação do hook exclusivamente nesta entrada. `RpgRuntime` e `RpgWorld` foram lidos e permaneceram intactos.
- `RpgPlayerSpritesTest`: dez testes adicionais. `testar-rpg-sprites.cmd`: iniciador de desenvolvimento com modo geométrico alternativo.

O atlas oficial, `DirectionalAnimation` e seu contrato de 59 frames/100 ms continuam intactos. Não foram criados Run/Attack/Hit/KO. Jogador morto retorna ao comportamento geométrico anterior; outros atores nunca usam estes sprites. O HUD principal continua mostrando vida, nome, progressão e equipamento; o desenho experimental substitui o ator e seus adornos geométricos locais.

## Uso

Na raiz do projeto, com Java 17 e classes compiladas:

```powershell
.\testar-rpg-sprites.cmd
.\testar-rpg-sprites.cmd --geometrico
```

Alternativa direta, que permite ajustar a duração:

```powershell
java '-Dspiritgate.rpg.spriteFrameMillis=150' -cp server/target/classes com.projetok.prototype.rpg.RpgLauncher --experimental-player-sprites
java -cp server/target/classes com.projetok.prototype.rpg.RpgLauncher
```

Para compilar e validar novamente sem sobrescrever os logs desta entrega:

```powershell
python tools/qualidade.py --offline --task 078-local
```

Feche o cliente antes de mudar o modo. O menu e os saves existentes não recebem conversões. Fora de pacote, saves continuam na pasta de trabalho; ao usar esses comandos na raiz, essa pasta é a raiz. O iniciador antigo `jogar-rpg-offline.cmd` continua abrindo o EXE antigo, portanto não demonstra esta integração nova.

## Preservação

Checkpoint antes das alterações, ZIP recuperável, hashes de fontes/recursos/assets e status inicial foram criados. O checkpoint final, diff isolado e inventário de arquivos ficam em `.task-checkpoints/TASK-078/`. Arquivos TASK-077, originais PNG, regras, saves, protocolos, Coop, entradas da arena, atlas oficial e distribuições anteriores foram preservados. A alteração no renderer compartilhado é opt-in, sem ativação na arena ou Coop, e o caminho inativo foi comparado pixel a pixel em teste.

O anexo recebido termina no item incompleto “O personagem mantém” da seção 11; esta execução cobre os requisitos completos das seções anteriores, sem presumir texto não recebido. Nenhuma publicação, push, release ou integração de arte em outros modos foi feita.
