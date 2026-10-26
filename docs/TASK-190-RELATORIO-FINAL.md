# CORUJÃO VI — TASK-190

Estado: **VALIDADA APÓS CORREÇÃO, COM RESSALVAS DE MEDIÇÃO**. Branch beco, HEAD ad66caed14e3f94bc369f7edd9861900d5a68636. Transição 189 -> 190 -> 191 automaticamente.

Ensaio opt-in real com dois clientes TCP V4, cinco inimigos/um NPC, asset sintético preinstalado e 4.192.250 bytes de descritor (2.054 bytes abaixo de 4 MiB). Acréscimo de metadado acima do limite foi recusado pelo mesmo pipeline de validação. Nenhum PNG trafegou. Dois períodos de pelo menos 150 s, mesma configuração/cadência, cada um com dois pares de publicação/rollback e hashes/ponteiro conferidos ao retomar. Heap Java limitado a 1 GiB e observações limitadas a 4.096 por período. Sem migração de dados pessoais.

| Periodo | Duracao (s) | Amostras | Heap p95 (MiB) | Threads p95 | Tick p95 de janelas (ms) | Paint p95 (ms) | RTT p95 de janelas (ms) | Operacoes | Publicacao p95 (ms) |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| baseline | 150.073 | 1401 | 346.224 | 22 | 2.0003 | 0.5943 | 0.2970 | 4 | 17381.544 |
| followup | 150.021 | 1397 | 438.507 | 22 | 1.4594 | 0.5538 | 0.3076 | 4 | 17369.021 |

Arquivos sinteticos do ensaio: 42045982 bytes; limite planejado 300 MiB.

O primeiro soak (run-20261025T224117Z-d65e7391) completou um par e abortou no seguinte por desconexão, sem commit falso. A regressão curta posterior (run-20261025T224955Z-7a08dcaf) comprovou que um único Ping fechava um peer quando conteúdo já preenchia quatro controles. A tentativa run-20261025T224820Z-b5a03493 teve falha ambiental de captura Cp1252/UTF-8, preservada sem inventar contagem. A correção mínima reserva uma posição de controle para Pong; capacidade total permanece quatro e protocolo/limites são preservados. Atualizada a fixture 187 para exigir no máximo três controles de conteúdo e as mesmas amostras de IO retido. O negativo passou após patch e a regressão multiplayer passou antes e depois do soak (129 casos).

Os dados comparam baseline e acompanhamento dentro do mesmo ensaio, não versões do código nem tempos de 180/182. Heap não foi normalizado por GC; deltas não provam vazamento nem ganho. Tick e RTT são percentis de janelas móveis de 256 amostras, posteriormente resumidos no tempo, não p95 de todos os ticks individuais. Paint mede camadas do renderer real offscreen, incluindo indexação por amostra, e não a janela completa com atores em monitor/VPN. Amostragem de 100 ms inclui overhead do observador nos dois períodos e pode atrasar por GC/agendamento. As primeiras janelas ainda podem refletir preparo anterior à fase medida. Nenhuma melhora de performance ou ausência de pausa perceptível é alegada.

Logs da reprodução, fonte anterior, patch, fixture, CSVs e JSONs de cada período estão preservados. Nenhuma instalação, mudança de rede, save/schema pessoal ou publicação externa. Radmin físico permanece pendente. A instrumentação/memória foi validada somente para as cargas e duração explicitadas.


Evidências: `.task-checkpoints/TASK-190/corujao-vi/run-20261025T225047Z-830687b0`.

Integridade: 31629 entradas verificadas; rehash nesta etapa: 0; divergências: 0. Método e hashes em protected-before/after.json.

Fontes alterados nesta task: server/src/main/java/com/projetok/multiplayer/LocalSimulationServer.java, server/src/test/java/com/projetok/multiplayer/Task187BackpressureTest.java.
