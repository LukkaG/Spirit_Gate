# Testes de rede e apresentação

TASK-151: servidor RPG sem Swing, dois clientes TCP, movimento remoto, capacidade, sequência e encerramento.

TASK-152: bytes Hello V1 fixados por golden test; frames excessivos, truncados, desconhecidos, UTF-8 inválido e dados excedentes recusados. Testes históricos do Coop e dos dois processos preservados.

TASK-153: probe de duas janelas com TCP real, movimento independente observado nos dois clientes, remoção após saída e novo ID na reconexão. Capturas printAll não representam homologação física.

TASK-154/155: paredes, limites, colisão de corpos, magnitude inválida, ausência de comandos autorizados após saída, expiração de entrada, reconexão controlada, encerramento de servidor e recusa de endereços públicos. Combate de RPG em rede não implementado; ações não suportadas não alteram vida, moedas ou posição.

TASK-158: extensão cosmética roundtrip, capacidade negociada coexistindo com peer V1, ID inexistente, replay, ID por caminho, preservação da raça atribuída e remoção de metadados ao sair. Probe com duas janelas confirmou cabelos/roupas distintos visíveis remotamente, raças distintas, movimento, desconexão e aparência limpa na nova sessão. Evidências em `.task-checkpoints/TASK-158/corujao-iv/two-clients/`.

Regressão da TASK-158: 463 executados, 455 aprovados, oito abortados de ambiente headless, zero falhas e zero ignorados. Validação integral gráfica e auditoria dos executáveis finais pertencem à TASK-159.
