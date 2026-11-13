# Multiplayer ? 0.200.4

Abra SpiritGate-Launcher.exe do Anfitri?o na entrega dist/SpiritGate-Launcher-Grafico-0.200.4. Seu amigo deve extrair o ZIP Convidado da mesma vers?o. Fechem servidor, jogo e launcher anteriores antes de testar. A entrega permanece local, sem publica??o online.

## Contato entre jogadores

Bloco s?lido de apoio 24 ? 24 no perfil padr?o (meia largura 12), independente dos pixels da anima??o. Spawn, portal, respawn e troca de revis?o usam a mesma geometria. Deslizar na face e recuar continuam permitidos; o percurso completo ? validado contra atravessamento, inclusive na diagonal. N?o empurra o amigo. Cen?rio e inimigos conservam seus contratos. Perfil publicado pode mudar a dimens?o do bloco.

A regress?o num?rica reproduzia uma dist?ncia aceita pelo c?lculo float e considerada sobreposta pela varredura double, prendendo a sa?da. O solver permite a separa??o v?lida desse contato. O cliente tamb?m evita desenhar o bloco hist?rico do amigo dentro da posi??o atual local. F2 mostra blocos verde/vermelho, apoio ciano e limites magenta do atlas efetivamente desenhado.

O bloco fica nos p?s: cabelo, roupa e parte superior das imagens podem se sobrepor por profundidade sem significar passagem pelo bloco f?sico. A arte ampliada n?o altera combate ou dist?ncia f?sica.

## Cad?ncia e atraso

Simula??o permanece30 Hz; snapshots padr?o10?30 Hz; override spiritgate.network.snapshotHz aceita10,15,30. Interface33?16 ms. Altera??es de tecla s?o enviadas no evento, sem aguardar timer; heartbeat100ms mantido. Buffer remoto adapta25..150ms, segura ?ltimo estado na falta de rede e n?o extrapola posi??o. Paredes e corpo autoritativo permanecem no servidor. Estruturas do documento s?o reutilizadas at? trocar revis?o/regi?o, sem reconstruir listas a cada quadro.

Mesma carga Task177NetworkProbe, localhost,2 clientes,100janelas200ms, inclui inicializa??o:

| Medida | Antes | Depois |
| --- | --- | --- |
| Intervalo snapshot mediano | 99.989ms|33.001ms|
| Intervalo snapshot p95 |109.014ms|45.008ms|
| RTT mediano |0.117ms|0.116 ms|
| RTT p95 |0.263ms|0.190ms|
| Processamento tick p95 |1.130ms|1.058ms|
| Bytes servidor2 clientes |102494|303371|
| Pico fila comandos |2|2|
| Snapshots substitu?dos |0|0|

O tr?fego ? aproximadamente3x; n?o h? alega??o de menor custo ou ping VPN. CPU agregado do processo16.55?17.19s em cerca28.35s; threads coop ativas312.5?765.6ms e aloca??o485.8?821.3MB, incluindo inicializa??o e snapshots. N?o representa perfil completo de GC. Render Task177RenderProbe,240amostras ap?s60aquecimento: printAllp95 18.344?19.181ms; pintura p95 2.464?2.618ms. O harness for?a capturas e altera a cad?ncia: n?o ? uma medi??o de FPS real do usu?rio. Rota Radmin entre m?quinas e input-to-photon permanecem sem medi??o.

## Sprites

Reaper em2x, Human/base em1x. Cada pixel original ? preservado por nearest-neighbor; sem altera??o dos PNGs, recorte ou gera??o de detalhe. O piv? dos p?s permanece fixo. As36 poses Reaper s?o comparadas pixel-a-pixel em blocos2?2 por teste. Nomes acompanham o topo real da pose e F2 usa atlas/escala/cad?ncia por ra?a. Defaults/configura??o offline anteriores preservados. A escala do monitor pode afetar o resultado final.

## Verifica??o

54 testes de movimento passaram, incluindo contato fracion?rio, recuo, tang?ncia, corredor, quinas e aus?ncia de tunelamento. Su?te RPG:61 passaram,1 abortado por exigir UI. Su?te multiplayer final: 143 passaram, 1 abortado por exigir UI, nenhuma falha (server/target/local-tests/run-qkl03zqa/test-results.log). Dois pressupostos antigos foram corrigidos nos testes: combate usa or?amento por ticks, e leitura de conte?do permite snapshots v?lidos intercalados, mantendo asser??es de combate, recompensa e backpressure.

ZIPs e arquivos extra?dos conferidos por SHA-256; duas interfaces gr?ficas executadas; servidor real com dois processos clientes usando oJava dos pacotes. Captura inspecionada em .task-checkpoints/melhorias-multiplayer-0.200.4/packages-smoke/client-a.png.

Evid?ncias, m?tricas antes/depois e build: .task-checkpoints/melhorias-multiplayer-0.200.4. Recriar pacote com tools/build-multiplayer-update.py, informando canal base, pastas novas de evid?ncia e entrega, e --version. Nenhum commit,push ou deploy executado.
