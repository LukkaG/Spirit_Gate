# TASK-144 — Relatório

Auditoria e regressão de movimento normal e Shunpo. Aproximação ofensiva agora verifica raio físico de 18 em todo o trajeto, rejeitando passagem por parede fina deslocada mesmo quando o centro tem visão livre. Feedback específico para recarga, vigor e estado. Diagonal, ausência de drift e cooldown após roundtrip testados.

Limitações: Velocidade 215, raio, alcance, custo, recarga, escala 0.5 e walk 250 ms preservados. Parâmetros de balanceamento continuam provisórios.

Validação: JUnit: 443 executed, 435 passed, 8 aborted, 0 failures, 0 skipped. Nenhuma aprovação artística automática. Diff e hashes no checkpoint.
