# TASK-138 — Relatório

Contrato QuestBook e documento sgquest V1, UTF-8, estritamente declarativo, separado de sgmap e save. IDs, textos, NPC, região, raça/domínio, nível, dependências, objetivos e recompensa usam catálogos e tipos reais. Validação de ciclos e condições contraditórias. Gravação temporária validada e backup.

Limitações: Contrato de reset somente declarativo, sem execução financeira. Objetivos suportados: kill, collect, talk e visit. Recompensa única de item, XP e moedas conforme QuestService atual. Persistência de personagem continua V2 sem mudanças.

Validação: JUnit: 427 executed, 420 passed, 7 aborted, 0 failures, 0 skipped. Nenhuma aprovação artística automática. Diff e hashes no checkpoint.
