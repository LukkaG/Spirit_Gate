# TASK-127 — Corujão III

Estado: **VALIDADA COM RESSALVAS**.

Auditoria gráfica em janelas reais: NPC/HUD, inventário e diário, 1000x620, 800x600 e 640x480, Java2D 100% e 150%. Corrigidos contraste das barras, revelação de objetivos indisponíveis no HUD e altura excessiva do HUD. Probes passaram: docs/task-127/capturas-final e capturas-final-dpi150.

Em 640x480, diálogo reduz bastante a área de exploração; fechar o diálogo restaura espaço. Recomendada homologação em 800x600 ou maior. Tab/Shift+Tab e DPI físico dependem de revisão humana. Capturas printAll não homologam monitor. Regressões completas aprovadas.

Logs: `docs/TASK-corujao3-127-final-*`. Checkpoints recuperáveis, inventários SHA-256, diff isolado e preservação: `.task-checkpoints/TASK-127/corujao-i/`. O nome histórico da pasta pertence à ferramenta reutilizada; o conteúdo corresponde à tarefa indicada.

[Testes](TASK-127-TESTES.md).
