# Desempenho medido ? TASK-113

Java 17.0.20.1, Windows, 16 processadores logicos. Catalogo real: 248 PNGs; 4.063.232 bytes de pixels de miniaturas (nao inclui overhead JVM). Cache LRU limitado a 64 MiB. Mapa sintetico de 12.000 tiles, 4096x4096, oito camadas; 40 amostras por zoom apos 12 aquecimentos. Cache do sistema de arquivos nao controlado. Medicoes locais, sem garantia em outra maquina.

| Zoom | p95 antes (ms) | p95 depois (ms) |
|---|---:|---:|
| 0,25 |21,4581|21,2604|
| 0,75 |3,6528|3,7288|
| 2 |2,2980|2,1521|
| 8 |1,9804|1,6702|

O indice de viewport preserva ordem e limita referencias; evita examinar todas as camadas a cada objeto. O ganho e mais claro em zoom alto; o zoom 0,75 piorou ligeiramente nesta rodada. Cenario com muitos tiles visiveis continua dominado pelo desenho. Nao afirmamos ganho universal.

Indexacao: 789/793 ms. Abertura da janela sem indexacao: 283/267 ms. Carregamento do mapa de 2.526.225 bytes: 52/50 ms. Mil ciclos de commit/undo/redo/undo: 8,0/7,6 ms. Cem mil transformacoes zoom/pan: 13,8/8,6 ms; microbenchmark sujeito a JIT.

Salvamento atomico validado: 8.392/8.395 ms, ainda alto no primeiro uso. Uma rodada adicional isolou codificacao 163,8 ms, decodificacao 145,8 ms e escrita simples 3,94 ms; esses numeros nao explicam todo o custo de salvar. Nao removemos validacoes ou backup para melhorar uma metrica. Investigar operacoes temporarias/renomeacao e aquecimento do sistema. Todas as operacoes de arquivo ficam fora da EDT.

Imagens completas agora sao lidas em buffer limitado e o cabecalho e conferido antes da decodificacao; o mesmo buffer e usado para hash e pixels. Pintura nao faz I/O. Geometria do canvas e reutilizada por documento. Sobreposicoes fisicas sao truncadas em 1.000 evidencias para limitar memoria.

Reproduzir apos compilar: `javac -encoding UTF-8 -cp server/target/classes -d server/target/bench tools/WorldStudioBenchmark.java`, depois `java -cp "server/target/classes;server/target/bench" com.projetok.studio.WorldStudioBenchmark pasta-nova .`. JSONs medidos em `world-studio/performance/`. Teste do indice compara 100 viewports com busca exaustiva, incluindo elemento 8192x8192.

Uma falha de compilacao de inicializacao do timer foi corrigida antes da suite aprovada; tentativas preservadas no checkpoint local. Resultado final: 367 testes Java e 30 Python, zero falhas.
