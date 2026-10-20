# TASK-084 - Corujao I

Auditoria entrada -> fila 64 -> tick 30 Hz -> snapshot -> EDT: bindings existentes ja suprimem repeticao de tecla e limpam movimento ao perder foco. Testes novos verificam pressionamentos rapidos, tecla mantida, reset apos foco, limite da fila e cooldown sem duplicacao. Nenhum buffer acrescentado por ausencia de necessidade reproduzida. A recusa de acoes pausadas/encerradas e tratada na TASK-088, conforme pendencia conhecida.
