# TASK-132 — Corujão III

Estado: **VALIDADA COM RESSALVAS**.

Feedback adicional por snapshots confirmados para região, alterações de moedas/obtenção de itens, conclusão de missão e eventos REJECT deduplicados por sequência. Reutiliza tons sintetizados originais e fila limitada existente. Silêncio quando aplicação perde foco, incluindo janelas filhas; listener removido no encerramento. Redução de iluminação também desativa respiração decorativa dos previews. Dois testes cobrem repetição de snapshot/evento, suspensão, mudo e volume real.

Não existem trilhas licenciadas no contrato atual: música, crossfade e canais independentes não são simulados nem oferecidos. Sem novos arquivos artísticos. Falta homologação auditiva física; falha de dispositivo mantém fallback silencioso. Configuração de redução decorativa reutiliza environment existente.

Logs: `docs/TASK-corujao3-132-final-*`. Checkpoints recuperáveis, inventários SHA-256, diff isolado e preservação: `.task-checkpoints/TASK-132/corujao-i/`. O nome histórico da pasta pertence à ferramenta reutilizada; o conteúdo corresponde à tarefa indicada.

[Testes](TASK-132-TESTES.md).
