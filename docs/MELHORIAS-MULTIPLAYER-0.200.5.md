# Multiplayer — 0.200.5

Reaper em **1,5x**, Human em 1x. Feche jogo, servidor e launcher anteriores; abra `SpiritGate-Launcher.exe` do Anfitrião em `dist/SpiritGate-Launcher-Grafico-0.200.5`. Envie ao amigo o ZIP Convidado dessa versão. O canal online continua sem publicação.

## Trabalho removido do servidor

Consultas de posição, vida, raça e região montavam repetidamente o `snapshot()` completo de `RpgWorld`, que também chama `save()` e copia inventário, missões e flags. `NetworkState` fornece somente o estado transitório necessário à sessão. NPCs e portais têm consultas específicas. `RpgSessionWorld` e a seleção de alvos/corpos de `SharedRpgEncounter` usam essas leituras leves. As rotinas de save e snapshot completo continuam disponíveis e preservadas.

Streams de socket possuem buffers de 8 KiB persistentes durante a conexão; o protocolo mantém o flush por frame. Isso agrupa operações de leitura/escrita sem esperar acumular um lote de comandos. O formato dos pacotes, a autoridade do servidor, a colisão e os limites das filas permanecem os mesmos.

## Apresentação

Além do timer de 16 ms, a recepção de snapshot agenda uma atualização na EDT. Assim o estado recebido pode ser apresentado sem esperar o próximo callback do timer. `SnapshotRefresh` agrupa notificações em uma única tarefa pendente; o callback lê o snapshot mais recente. O timer continua interpolando entre recepções e a janela remove o listener ao fechar. Não há posição ou combate calculados pelo cliente como autoridade.

Mantidos: simulação e snapshots a 30 Hz, heartbeat de 100 ms, limites do buffer remoto e correções de bloco/cenário. Não houve aumento de frequência nem mudança de protocolo nesta entrega.

## Tamanho da Reaper

O atlas é desenhado a 1,5x com nearest-neighbor. O canvas de 64 × 64 passa a 96 × 96; o pivô dos pés é preservado. PNGs originais e bloco de colisão não mudam. Por ser escala fracionária, os pixels ocupam alternadamente uma ou duas unidades na imagem apresentada; não há criação de cores por suavização. Os limites de debug e o nome usam a mesma escala da pose.

## Comparação local com 0.200.4

Mesmo `Task177NetworkProbe`, dois clientes TCP, 100 janelas de 200 ms, JDK 17 e diagnóstico habilitado. Os tempos totais incluem a inicialização e não foram obtidos com CPU isolada.

| Medida | 0.200.4 | 0.200.5 |
| --- | --- | --- |
| Processamento do tick p95 | 1,831 ms | 0,312 ms |
| Alocação nas threads coop ainda ativas | 826.356.232 bytes | 87.775.024 bytes |
| CPU dessas threads | 1.140,625 ms | 421,875 ms |
| CPU total do processo | 17.796,9 ms | 13.109,4 ms |
| Tempo total | 28.363,0 ms | 28.462,6 ms |
| Intervalo snapshot mediano | 33,000 ms | 33,003 ms |
| Intervalo snapshot p95 | 45,993 ms | 44,057 ms |
| RTT mediano / p95 | 0,121 / 0,206 ms | 0,099 / 0,235 ms |
| Bytes enviados pelo servidor | 303.869 | 304.616 |

A queda de alocação medida é de aproximadamente 89%; isso não é uma medida de memória residente nem perfil completo de GC. O ganho é no trabalho executado pelo servidor, sem promessa de redução do ping pela Radmin. RTT p95 variou para cima neste ensaio, apesar da melhora de processamento.

Render separado, com 240 amostras após 60 de aquecimento: `printAll` p95 16,999 → 17,438 ms; pintura p95 3,113 → 2,771 ms. Capturas forçadas alteram a frequência observada de pintura e não demonstram FPS real do usuário. A remoção da espera do timer é uma mudança de agendamento; não foi medido input-to-photon em dois computadores.

## Verificação

- Multiplayer: 147 testes passaram, 1 abortado por exigir UI, nenhuma falha. Log em `server/target/local-tests/run-4yq9rv00/test-results.log`.
- RPG: 97 testes passaram, nenhuma falha. Log em `server/target/local-tests/run-xvquuy7j/test-results.log`.
- O estado leve foi comparado ao completo após movimento, progressão, Shunpo, portal, dano, morte e respawn. Leituras repetidas não alteram saves.
- As 36 poses Reaper foram verificadas em 1,5x quanto a dimensões, pivô e ausência de cores interpoladas. O teste de fidelidade em 2x foi preservado.
- Há testes do agrupamento de notificações e de sinalização após publicar um snapshot recebido por TCP real.
- ZIPs e payloads são conferidos por SHA-256; as duas interfaces gráficas e dois processos clientes dos pacotes foram exercitados localmente.

Build, logs, métricas e capturas ficam em `.task-checkpoints/melhorias-multiplayer-0.200.5`. Teste físico Radmin entre os participantes continua necessário. Nenhum commit, push ou deploy foi executado.
