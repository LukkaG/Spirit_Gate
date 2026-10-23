# TASK-152 — Relatório

Protocolo V1 documentado com limites, sequenciamento, ownership e timeouts reais. Golden wire test conserva exatamente o Hello histórico. Decodificador passa a recusar UTF-8 inválido; testes cobrem frame truncado, dados excedentes e rejeições estruturais, além das regressões de rede existentes.

Limitações: Sem autenticação comercial nem alteração incompatível de bytes V1. Confirmação de envio não significa ação aceita. Extensões cosméticas futuras exigem negociação por peer.

Validação: JUnit: 451 executed, 443 passed, 8 aborted, 0 failures, 0 skipped. Nenhuma aprovação artística automática. Diff e hashes no checkpoint.
