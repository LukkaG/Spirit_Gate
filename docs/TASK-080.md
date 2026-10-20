# TASK-080 - Corujao I

Estados visuais por ticks: KO > HIT > ATTACK > WALK/IDLE. Eventos efemeros limitados a 64, fora de saves e protocolos. Ausencia de arte Attack/Hit/KO usa fallback Idle/Walk e geometria; RUN indisponivel. Testes de prioridade, expiracao, repeticao e recuperacao. Primeira falha de transicao corrigida descartando eventos anteriores; logs preservados no checkpoint.
