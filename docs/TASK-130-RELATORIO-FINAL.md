# TASK-130 — Corujão III

Estado: **VALIDADA COM RESSALVAS**.

Minimapa de regiões legadas e mapa regional gráfico com zoom 1–3, jogador, NPCs e portais do snapshot; obstáculos vêm de LocalArenaMap. Ocultação manual e automática abaixo de 800px para preservar exploração. Não revela recursos secretos ou inimigos distantes. Testes de projeção, zoom inválido, consulta sem mutação e atualização após portal. Probe real de mapa/zoom aprovado. Falha intermediária de contexto acessível corrigida; log original preservado.

StudioExport/RpgSceneSet inspecionados: até três regiões 960x640. Esta UI atende mapas legados do launcher; sessões de autoria continuam no fluxo próprio e não recebem geometria presumida. Integração visual específica de autoria permanece pendente. Sem garantia de acessibilidade física por análise de navegação. Teclado/DPI físico pendentes.

Logs: `docs/TASK-corujao3-130-final-*`. Checkpoints recuperáveis, inventários SHA-256, diff isolado e preservação: `.task-checkpoints/TASK-130/corujao-i/`. O nome histórico da pasta pertence à ferramenta reutilizada; o conteúdo corresponde à tarefa indicada.

[Testes](TASK-130-TESTES.md).
