# TASK-133 — Corujão III

Estado: **VALIDADA COM RESSALVAS**.

Consolidação das 17 tarefas. Regressão final: 400 Java e 30 Python, zero falhas/erros/ignorados Java. Oito probes finais passaram, incluindo percurso com identidade criada pela UI, compra/venda, equipamento, missão, combate com XP, habilidade, opções, salvar e continuar. Fixtures sintéticas explícitas de comércio/combate; movimento confirmado no save. Previews carregam redução decorativa no worker também após reiniciar, e classes usam cards selecionáveis com dados reais. Pacote privado Windows rc2 com Java integrado: 169 arquivos auditados, EXE iniciou/encerrou com dados temporários. Saves, PNGs originais, Studio, arena, Coop e protocolos preservados.

Evidências: docs/task-133/validacao-final/summary.json, package-audit.json, package-smoke.json e protected-preservation.json. Medidas reais em validacao-final/SpiritGatePerformanceProbe/performance.json: pintura Swing não equivale a FPS físico; heap sem isolamento de GC. Homologação humana visual/auditiva e teclado físico pendentes. Não há trilhas musicais licenciadas, canais independentes ou crossfade; mapas de autoria mantêm fluxo próprio. Resolução e minimapa são ajustes de sessão. Sem push/publicação. TASK-134 não iniciada.

Logs: `docs/TASK-corujao3-133-final-*`. Checkpoints recuperáveis, inventários SHA-256, diff isolado e preservação: `.task-checkpoints/TASK-133/corujao-i/`. O nome histórico da pasta pertence à ferramenta reutilizada; o conteúdo corresponde à tarefa indicada.

[Testes](TASK-133-TESTES.md).
