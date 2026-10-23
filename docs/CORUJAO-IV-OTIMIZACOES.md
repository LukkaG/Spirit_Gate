# Otimização mensurada de salvamento

TASK-145. Windows, Java 17.0.20.1, 16 processadores lógicos, 254 assets e mapa de 12.000 elementos (4096×4096). Fontes recompilados em --release 17.

Três processos novos antes e três depois; cada processo salva cinco arquivos novos e repete o salvamento com backup, verificando roundtrip de ambos. O primeiro par de processos foi sequencial; os pares 2/3 compartilharam a máquina. Cache do sistema, GC e atividade do Windows não foram isolados. Não são medições de SSD puro nem garantias universais.

| Primeiro salvamento por processo | Antes (ms) | Depois (ms) |
|---|---:|---:|
| 1 | 8248,58 | 243,00 |
| 2 | 8217,61 | 245,79 |
| 3 | 8228,93 | 224,32 |

Medianas: 8228,93 → 243,00 ms nesta máquina. Salvamentos seguintes permaneceram na ordem de 112–191 ms antes e 114–173 ms depois, com variação; não há evidência de aceleração universal do caminho já aquecido.

O probe isolado de temporário JDK mediu 8024,48 ms na primeira chamada e 0,42 ms na segunda. `javap -c -p java.nio.file.TempFileHelper` confirmou o uso de SecureRandom nesse caminho. Após a mudança, a primeira chamada direta ao temporário JDK, feita ao final do probe, ainda custou aproximadamente oito segundos; o salvamento já não a utiliza. Isso sustenta a atribuição da latência local à inicialização desse caminho, sem diagnosticar a causa interna do provedor de entropia do Windows.

Mudança localizada em DocumentStore: reserva do temporário com Files.createFile/CREATE_NEW e nome por processo, relógio monotônico e sequência, com até 256 tentativas de colisão. Não depende de segredo criptográfico; nunca reutiliza nome existente. Permanecem encode, decode antes de gravar, releitura/validação do temporário, backup íntegro e movimento atômico com fallback existente. Saves V1/V2 não foram modificados.

O campo histórico `warmedJdkTempMs` nos JSONs do probe significa chamada JDK posterior aos salvamentos. Depois da mudança ela deixa de estar aquecida, precisamente porque o caminho otimizado não a chamou.

Benchmark geral posterior: indexação 1089,31 ms; save 202,80 ms; load 48,15 ms. Pintura p50/p95 (ms): zoom 0,25 = 11,29/19,07; 0,75 = 3,24/3,86; 2 = 1,82/2,05; 8 = 1,39/1,52. Abertura de janela 340,98 ms, versus 319,57 na baseline; undo/redo e zoom/pan também variaram para cima. Não se atribui ganho a essas métricas sem mudança e isolamento próprios.

Evidências brutas: `.task-checkpoints/TASK-145/corujao-iv/`, incluindo before-probe-1..3, after-probe-1..3, temp-init e after-world. O teste DocumentFirstSaveSafetyTest verifica backup anterior, arquivo final, preservação de temporário alheio e limpeza dos próprios temporários. Regressão completa sem falhas.
