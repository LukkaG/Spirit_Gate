# TASK-080 - Corujao I

Estado: **VALIDADA COM RESSALVAS**.

Estados visuais por ticks: KO > HIT > ATTACK > WALK/IDLE. Eventos efemeros limitados a 64, fora de saves e protocolos. Ausencia de arte Attack/Hit/KO usa fallback Idle/Walk e geometria; RUN indisponivel. Testes de prioridade, expiracao, repeticao e recuperacao. Primeira falha de transicao corrigida descartando eventos anteriores; logs preservados no checkpoint.

Checkpoints recuperaveis, hashes e diff isolado: `.task-checkpoints/TASK-080/corujao-i/`. Logs da suite: `docs/TASK-corujao-080-*`. A aprovacao artistica permanece humana; os testes nao equivalem a homologacao perceptiva.
