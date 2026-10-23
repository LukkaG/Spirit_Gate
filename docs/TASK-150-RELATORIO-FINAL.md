# TASK-150 — Relatório

Arquitetura cliente-servidor documentada a partir do TCP real, ownership, handshake, filas, snapshots e falhas. Decisão: transporte reutilizado com backend RPG opcional, preservando CoopWorld padrão. RpgWorld agregado por perfil sintético, sem população paralela de inimigos. Combate compartilhado inicialmente fora do escopo funcional.

Limitações: Sem mudança de protocolo nesta tarefa. Backend RPG será implementado na TASK-151. Nomes não autenticam contas; LAN física não homologada. Segurança de produção fora do protótipo.

Validação: JUnit: 447 executed, 439 passed, 8 aborted, 0 failures, 0 skipped. Nenhuma aprovação artística automática. Diff e hashes no checkpoint.
