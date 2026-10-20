# TASK-084 - Corujao I

Estado: **VALIDADA**.

Auditoria entrada -> fila 64 -> tick 30 Hz -> snapshot -> EDT: bindings existentes ja suprimem repeticao de tecla e limpam movimento ao perder foco. Testes novos verificam pressionamentos rapidos, tecla mantida, reset apos foco, limite da fila e cooldown sem duplicacao. Nenhum buffer acrescentado por ausencia de necessidade reproduzida. A recusa de acoes pausadas/encerradas e tratada na TASK-088, conforme pendencia conhecida.

Checkpoints recuperaveis, hashes e diff isolado: `.task-checkpoints/TASK-084/corujao-i/`. Logs da suite: `docs/TASK-corujao-084-*`. A aprovacao artistica permanece humana; os testes nao equivalem a homologacao perceptiva.
