# Medição e limites

Benchmark reproduzível: `tools/CorujaoFiveDemo.java`; aquece 300 ticks e mede 3000 ticks com dois snapshots, dois jogadores e um inimigo. Métricas efetivas em `.task-checkpoints/TASK-175/corujao-v/demo-02/benchmark.json`, incluindo média/p95/máximo, CPU da thread, heap usado e tamanho de frame. `result.json` inclui tempo para duas conexões, RTT e quatro capturas Swing.

A execução inicial em demo-01 mediu média 729 µs, p95 2046 µs, máximo 36031 µs, frame de 257 bytes e heap usado 280608024 bytes. O máximo excede um tick de 33,3 ms; nenhuma alegação de ausência de stutter. Esses números incluem alocações e GC não controlado, não são orçamento de produção nem comparação de otimização. A execução final deve ser consultada diretamente no JSON.

Demo-02 final: média 723,6 µs, p95 2111,1 µs e máximo 40784,7 µs; CPU da thread 2109,4 ms para 3000 amostras; heap usado 330775064 bytes. Duas conexões em 85,5 ms, RTT localhost observado 61,8 ms e quatro capturas em 288,1 ms. RTT inclui filas/escalonamento e não equivale à latência física da rede. A leitura do HUD pode mostrar outra amostra, pois a medição é periódica.

Tick alvo 30 Hz; snapshots 10 Hz; cliente interpola remotamente por 100 ms, conserva estado local autoritativo e aplica snap em salto >50 px ou gap >15 ticks. Snapshots antigos não alteram estado/aparência. Animação recusa regressão de tick e termina walk após 500 ms sem atualização. Perda/jitter de TCP não são simulados como datagramas; testes de apresentação exercitam estados atrasados, saltos e lacunas. Harness de falhas de transporte e medições remotas permanecem pendentes.

Sem mudanças especulativas de IA ou estruturas centrais. Filas, frame, rate limit e timeouts permanecem limitados; fechamento libera sockets/threads. Falta estudo prolongado de CPU total, memória após horas e renderização sob carga.
