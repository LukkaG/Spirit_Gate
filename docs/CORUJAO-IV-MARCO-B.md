# Marco B — Autoria de missões

TASK-138 a TASK-141 implementadas e testadas. O contrato `.sgquest`, o editor visual e o playtest utilizam os catálogos e serviços existentes. A aplicação de recompensa foi reforçada com preparação integral em cópia antes da publicação, incluindo overflow de XP de domínio.

Regressão final: 435 executados, 427 aprovados, oito abortados headless, zero falhas e zero ignorados. Studio gráfico: 50 aprovados, zero abortados/falhas/ignorados. Os dois abortados adicionais desde a baseline são testes de gesto e Quest Builder; ambos passam com interface gráfica.

Os cenários A–E cobrem missão sem domínio, bloqueio de nível, domínio escolhido compatível, raça incompatível e repetição de recompensa. A demonstração em `.task-checkpoints/TASK-141/corujao-iv/demo` cria conteúdo pelo formulário, grava/reabre o documento, executa a conclusão pelo runtime e registra `duplicateReward=false`.

Limitação deliberada: ledger autorado é efêmero. Não foi alterado o save V2; runtime sintético recusa a gravação de personagem. O contrato de reset não produz reset nem pagamento. A documentação de uso está em CORUJAO-IV-QUEST-BUILDER.md.
