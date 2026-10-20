# TASK-078 — Validação visual experimental

Ambiente gráfico disponível. O cliente real foi aberto com título `SPRITES EXPERIMENTAIS`, menu/HUD/cenário de RPG offline e personagem sintético em pasta isolada. Também foi aberto sem a opção, comprovando apresentação geométrica. Não foram usados saves pessoais.

## Evidências reais

Pasta `.task-checkpoints/TASK-078/visual/`:

- `idle-FRONT.png`, `idle-BACK.png`, `idle-LEFT.png`, `idle-RIGHT.png`: capturas após soltar a tecla e retornar ao Idle correto.
- `walk-FRONT.1.png`/`.2.png`, `walk-BACK.1.png`/`.2.png`, `walk-LEFT.1.png`/`.2.png`, `walk-RIGHT.1.png`/`.2.png`: os dois frames por direção no cliente em movimento.
- `collision-solid.png`: jogador parado à esquerda do obstáculo enquanto a entrada tenta avançar para a direita. Permanece com a última orientação efetiva (Back), pois a tentativa bloqueada não movimenta o corpo. Isso é intencional.
- `visual-evidence.json`: nomes dos frames, x/y/tick e `windowShowing=true`, com `humanApproved=false`.

Modo anterior: `.task-checkpoints/TASK-078/geometry/geometry-default.png` e seu `visual-evidence.json`. Logs: `visual-run.log` e `geometry-run.log`. Código externo reprodutível: `VisualProbe.java`; ele usa uma pasta própria indicada em `jpackage.app-path`, cópias dos 12 PNGs e save sintético. As capturas são da árvore de componentes Swing de uma janela efetivamente aberta, não captura do desktop inteiro. Não se apresenta essa evidência como validação do compositor/monitor do usuário.

Foram abertas e examinadas a folha dos 12 originais, a captura Idle Front, Walk Right 2 e a captura do obstáculo. Associação de todas as direções e alternância são adicionalmente cobertas pelos dez testes. A aparência no cenário mantém transparência e proporção; não há fundo opaco dos PNGs. A escala de 50% foi usada em todos os frames. Não houve aprovação artística humana, teste perceptivo de longa duração ou afirmação de qualidade definitiva de suavidade.

## Homologação manual ainda necessária

1. Na raiz, abrir `testar-rpg-sprites.cmd`; criar personagem de teste ou continuar um existente, sem conversão.
2. Pressionar e soltar S, W, A, D separadamente. Conferir dois frames Walk e Idle na última direção; comparar tamanho, pés, cintura/cabeça e eventual oscilação.
3. Testar diagonais: eixo dominante, empate vertical. Empurrar bordas e o obstáculo central; quando não há deslocamento, o personagem fica Idle. Testar contato por todos os lados e comparar percepção visual com raio físico inalterado de 18.
4. Conferir combate, inventário, equipamento, portais, salvar e continuar. Não há sprites Attack/Hit/KO novos; a ausência dessas animações é conhecida, não falha escondida.
5. Fechar e reabrir com `testar-rpg-sprites.cmd --geometrico`, comparando o fluxo antigo. Para outro tempo, usar o comando Java com `-Dspiritgate.rpg.spriteFrameMillis=...` (30 a 2000 ms).
6. Se precisar ensaiar fallback manual, usar cópia do projeto/assets e retirar/corromper um PNG da cópia; não alterar os originais desta entrega. Os testes já cobrem esse caso com diretórios temporários.

Pivôs, escala, ritmo de 150 ms, base sem roupa/equipamento visual e integração com futuras animações aguardam decisão do responsável. Os dois arquétipos/aparências usam a mesma base experimental quando o modo é habilitado; não há camadas de equipamento ou promessa de conservar a diferenciação cromática geométrica na arte.
