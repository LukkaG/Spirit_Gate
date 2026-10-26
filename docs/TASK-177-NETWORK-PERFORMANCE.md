# Rede e apresentação — medições

Ambiente: Windows, Temurin OpenJDK 17.0.20.1, localhost, dois clientes reais TCP, servidor RPG com combate sintético nível 20. Classes originais preservadas em `server/target/local-tests/run-cht4w7xp/classes`. Nenhuma medição abaixo representa uma rota Radmin.

Antes de modificar parâmetros: 30 Hz, snapshots 10 Hz, Swing 33 ms, inputs a cada três callbacks (~99 ms), interpolação fixa 100 ms. Filas: comandos 64, cliente 32, snapshot por peer 1, controles por peer 4. TCP_NODELAY já ativo; flush por frame. Instrumentos, hipóteses e primeira medição registrados em `baseline.md` no checkpoint antes da implementação.

| Medida | Antes, 10 Hz | Depois, 10 Hz | Experimento, 15 Hz |
|---|---:|---:|---:|
| RTT mediano (ms) | 54.096 | 0.133 | 0.116 |
| RTT p95 (ms) | 95.638 | 0.237 | 0.246 |
| Intervalo snapshot mediano (ms) | 99.999 | 99.996 | 66.009 |
| Intervalo snapshot p95 (ms) | 111.003 | 110.006 | 78.008 |
| Pico comandos observado | 2 | 2 | 2 |
| Tick final | 609 | 610 | 609 |
| CPU agregado processo (ms) | 17593.8 | 17187.5 | 18234.4 |
| Tempo total processo (ms) | 28399.9 | 28407.4 | 28370.5 |
| Bytes escritos servidor (dois peers) | não instrumentado | 102494 | 152477 |
| Frames escritos servidor | não instrumentado | 506 | 707 |
| Tick processamento mediana/p95 (ms), últimas 256 amostras | não instrumentado | .452 / 1.209 | .598 / .968 |
| Snapshots substituídos na fila | não instrumentado | 0 | 0 |

`Task177NetworkProbe`: 100 medições com 200 ms de observação cada, polling de 1 ms e dois inputs a cada janela; total inclui inicialização do conteúdo. Ensaios válidos exigem os dois clientes conectados e `server.failure()==null` no final. A duração/CPU inclui o harness, inicialização e serialização de snapshots, não apenas a simulação. Não houve isolamento de CPU do sistema; diferenças pequenas de CPU não sustentam uma alegação de ganho. O teste não reproduz throughput de internet congestionada.

O ganho de RTT vem do writer: controles agora acordam a escrita imediatamente em vez de aguardarem a fila de snapshots por até 100 ms. O RTT é calculado com nanoTime local, começando no writer do cliente antes da serialização/envio e terminando na recepção do Pong correspondente. Portanto inclui processamento da aplicação e transporte; não é ping ICMP e não é atraso visual. Não depende de relógios sincronizados.

O experimento de 15 Hz transmitiu cerca de 49% mais bytes neste cenário e reduziu o intervalo de snapshots, sem ganho adicional relevante de RTT. Default mantido em 10 Hz; 15 Hz é ajuste técnico opt-in, limitado aos divisores suportados de 30 Hz. Frames seguem limitados a 4096 bytes; o smoke valida cliente/servidor com o mesmo JAR.

Comparação independente do renderizador: `Task177PresentationProbe`, movimento constante de 60 unidades/s, mesmos snapshots 10 Hz, amostragem 60 FPS, 570 amostras após aquecimento: atraso médio de apresentação **100.000 → 76.389 ms**, buffer final 75 ms. É um ensaio determinístico da apresentação; não mede input-to-photon físico nem RTT VPN. O cenário tem verdade de referência local conhecida. O buffer adaptativo usa mediana/p95 de intervalos locais, limite 50..150 ms; não acumula atraso indefinidamente. Estados antigos são recusados e targetTick nunca retrocede. Após lacuna, segura o último estado em vez de extrapolar sem limite.

Input: mudança de direção é transmitida no próximo callback Swing (33 ms), mantendo heartbeat de 100 ms, em vez de esperar três callbacks. Velocidade/diagonal/cooldown continuam calculados no servidor. Não foi introduzida predição. ACK individual de input não existe no protocolo histórico: tempo input→confirmação fica explicitamente indisponível, sem inventar correlação por posição.

Instrumentação opt-in: RTT mediana/p95, intervalos e idade de snapshots, buffer, pintura e intervalo de paint/FPS, escrita/bytes/frames, filas, substituições e descartes. Servidor mede tick/intervalo com nanoTime e imprime resumo a cada 300 ticks. O cliente não exibe um tick rate de servidor fictício; logs locais são a fonte desse número. Amostras são anéis limitados a 256; histórico visual tem até 20 estados. Alpha das sprites é calculado uma vez no carregamento; debug de pintura não lê arquivos nem recalcula colisões.

Alocações: a timeline produz DTOs de apresentação, cópias de listas e mapas por amostra, como o interpolador anterior; mantém no máximo 20 snapshots, sem fila de atraso ilimitada. Medidas de bytes exatos só estão disponíveis com debug habilitado, que adiciona um wrapper de OutputStream por escrita. Em repetição do workload com ThreadMXBean: threads `coop-*` ainda ativas alocaram 481902192 bytes no baseline e 485532720 bytes depois (~0.75% a mais, debug habilitado apenas depois); CPU dessas threads 718.750/796.875 ms. Inclui inicialização do mundo, leitores e escritores; não captura threads já terminadas nem todo o processo. A repetição confirmou RTT 46.161/94.086 → .124/.300 ms (mediana/p95). Não foi feito perfil completo de alocações/GC; nenhuma redução de alocação é alegada.

Render: mesmo probe Swing com dois clientes, 240 printAll após 60 de aquecimento: mediana/p95 16.081/23.131 ms antes, 15.617/19.229 ms depois sem debug, 15.299/18.061 ms com debug. Variações de aquecimento/carga impedem atribuir ganho ao renderizador. No ensaio de debug, paint mediano/p95 2.267/3.338 ms, FPS observado 56.33; o probe também solicita pintura e portanto altera a cadência natural de 33 ms do timer. Não são FPS da VPN. Tempos e capturas ficam nos arquivos `*-render.txt/png` do checkpoint; printAll inclui a árvore Swing inteira e não equivale ao paint de gameplay nem a aprovação visual humana.

Ensaios de desenvolvimento rejeitados: primeira rodada após trocar raio encontrou coordenadas menores que 18, rejeitadas pelo save histórico; os logs `after-10hz.txt` e `after-diagnostic.txt` não são validação final. A borda física compatível corrigiu o problema sem relaxar protocolo/save. Usar `after-10hz-valid.txt` e `after-15hz.txt` para a tabela acima.

Limitação externa: não há segundo computador Radmin disponível. Não se comprovou nem se descartou gargalo da rota VPN. Roteiro de homologação em TASK-177-TESTES.md.
