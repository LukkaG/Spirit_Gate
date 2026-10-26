# CORUJÃO VI — TASK-187

Estado: **VALIDADA COM RESSALVAS**. Branch beco, HEAD ad66caed14e3f94bc369f7edd9861900d5a68636 preservados. Transição: 186 corrigida -> 187 -> 188 automaticamente.

Implementado somente Task187BackpressureTest, sem alteração de produção. Ensaio real com dois sockets V4 e descritor válido de 659.857 bytes: leitura de um peer suspensa por dois segundos, buffers reduzidos apenas nesses sockets, pelo menos dez amostras de writer em escrita nativa com fila de quatro controles cheia. Owner continuou executando, comandos permaneceram limitados a 64 e snapshot a um; o outro cliente recuperou a revisão após o fechamento do peer lento. Frames lidos após a retenção permaneceram íntegros.

Teste independente fixa bytes de todos os opcodes 1–15, Hello de aplicações 1–4, limites de frame e recusa de wrappers recursivos. Regressão multiplayer: 121/121 aprovados, zero falhas/abortos/skips na execução de fechamento; específico: 2/2 aprovados. Limites mantidos: MAX_FRAME=4096, CHUNK=2800, conteúdo=4 MiB, dois jogadores e seis outros atores, writer único e filas sem crescimento livre. O custo de snapshots é limitado por um frame pendente por peer; o ensaio de preparo congela gameplay e não mede FPS/latência VPN.

Ressalvas e tentativas preservadas: primeira fixture esperava incorretamente avanço de gameplay dentro da barreira; segunda gerava até 100 Pings/s e podia provocar overflow de controles no cliente chamado normal. Ambas as falhas permanecem nos diretórios run-20261025T221619Z-ab49230c e run-20261025T222031Z-4145b1a0, com seus logs. A fixture final exige a mesma pressão real, mas representa o peer normal com até dois Pings/s e sem ping durante preparo. O teto de 120 mensagens/s não garante aceitação: fila cheia pode isolar um emissor antes desse teto. Não foi alegada tolerância ilimitada a Pings/peers lentos nem alterada a proteção para obter verde. A amostragem de stack é específica do JDK 17 usado no laboratório.

Logs contêm os picos efetivamente observados, bytes e número de amostras de retenção. Não se afirma melhoria de desempenho, teste físico Radmin ou segurança de produção. Nenhuma dependência, política, rede, save ou distribuição foi alterada.


Evidências: `.task-checkpoints/TASK-187/corujao-vi/run-20261025T222531Z-a2ddfc6e`.

Integridade: 31629 entradas verificadas; rehash nesta etapa: 0; divergências: 0. Método e hashes em protected-before/after.json.

Fontes alterados nesta task: nenhum.
