# TASK-079 — Comparação visual

Comparação feita com o cliente Swing real do RPG offline. Os parâmetros abaixo são aplicados por propriedades da JVM, sem recompilar entre os ensaios. Capturas de componentes de janelas abertas não representam aprovação artística do responsável nem teste do compositor de desktop em outra máquina.

| Configuração | Escala | Walk por frame | Evidências |
| --- | ---: | ---: | --- |
| Anterior | 0,50 | 150 ms | `.task-checkpoints/TASK-079/anterior/` |
| Recomendada | 0,40 | 220 ms | `.task-checkpoints/TASK-079/recomendada/` |
| Alternativa menor | 0,35 | 220 ms | `.task-checkpoints/TASK-079/menor/` |
| Alternativa mais lenta | 0,40 | 280 ms | `.task-checkpoints/TASK-079/mais-lenta/` |

Em cada pasta: `initial-idle-FRONT.png` (mesmo ponto lógico inicial para comparar tamanho), os oito `walk-DIRECTION.1/.2.png`, quatro `idle-DIRECTION.png`, `walk-FRONT-real.gif`, `visual-evidence.json`, `sequence-timing.json` e `run.log`. O GIF registra uma sequência curta de capturas do cliente durante a caminhada frontal; os atrasos são derivados dos intervalos observados da captura. Não foram inventados frames, interpoladas poses ou criados assets de jogo. A sequência não é benchmark de frame pacing: captura/serialização introduzem custo e a simulação tem resolução de 30 Hz. Reabra o GIF para repetir a reprodução se o visualizador não fizer loop.

`comparacao-escalas.png` apresenta recortes em mesma escala de pixels das quatro capturas iniciais, rotulados; os PNGs completos originais da evidência permanecem disponíveis. `comparacao.html` reúne capturas e GIFs para inspeção local. O harness reproduzível está em `.task-checkpoints/TASK-079/RefinementVisualProbe.java`.

A redução de 50% para 40% corresponde a 20% menos largura/altura em relação à apresentação anterior. Para 35%, a redução relativa é de 30%. A escala 40% é mantida no ensaio de 280 ms, isolando o ritmo. Alterar Walk não altera o deslocamento de 215 unidades/s nem qualquer raio físico. Não se inferiu aprovação estética a partir desses valores.

## Testar manualmente

Na raiz, com as classes já compiladas pela suíte desta tarefa:

```powershell
# Recomendada: 40%, 220 ms; usa os defaults novos
.\testar-rpg-sprites.cmd

# Anterior: 50%, 150 ms
java '-Dspiritgate.rpg.spriteScale=0.5' '-Dspiritgate.rpg.spriteFrameMillis=150' -cp server/target/classes com.projetok.prototype.rpg.RpgLauncher --experimental-player-sprites

# Alternativa menor: 35%, 220 ms
java '-Dspiritgate.rpg.spriteScale=0.35' '-Dspiritgate.rpg.spriteFrameMillis=220' -cp server/target/classes com.projetok.prototype.rpg.RpgLauncher --experimental-player-sprites

# Alternativa mais lenta: 40%, 280 ms
java '-Dspiritgate.rpg.spriteScale=0.4' '-Dspiritgate.rpg.spriteFrameMillis=280' -cp server/target/classes com.projetok.prototype.rpg.RpgLauncher --experimental-player-sprites

# Geométrico anterior
.\testar-rpg-sprites.cmd --geometrico
```

Alternativa sem escrever flags Java:

```powershell
$env:SPIRIT_GATE_SPRITE_SCALE='0.35'
$env:SPIRIT_GATE_FRAME_MS='220'
.\testar-rpg-sprites.cmd
Remove-Item Env:SPIRIT_GATE_SPRITE_SCALE,Env:SPIRIT_GATE_FRAME_MS
```

Reinicie o cliente para aplicar outra configuração. Use S/W/A/D e solte cada tecla para conferir o retorno ao Idle. Compare proporção, alinhamento dos pés, percepção do contato com paredes e conforto do ciclo de dois frames. Equipamento, combate e progressão não recebem novos desenhos nesta tarefa.

Os comandos de desenvolvimento, sem `spiritgate.rpg.dataDir`, mantêm o comportamento anterior de saves na pasta de trabalho; não fazem conversão. O harness de evidências usa sua própria pasta de dados. Os pacotes já distribuídos e o atualizador público não recebem esta alteração até uma publicação futura expressamente autorizada.
