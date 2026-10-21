# Ocorrências preservadas — Corujão III

- TASK-117: primeira tentativa do probe reportou `AssertionError: Diálogo não abriu`. O prazo de espera foi ampliado e o processo passou a encerrar explicitamente em sucesso/falha. Os probes finais passaram. Este item transcreve a saída observada no terminal; não é um log bruto recuperado.
- TASK-117/118: inspeção das capturas detectou atributos cortados e depois altura excessiva das linhas. Layout e limite de altura foram corrigidos; foi acrescentada uma asserção contra recorte dos atributos. Tentativas visuais anteriores permanecem nas pastas das tarefas.
- TASK-127: captura em 640×480 revelou que o HUD ocultava a exploração durante diálogo. Barras compactadas e resumo de missões ocultado enquanto o diálogo está aberto. Capturas anteriores e finais preservadas. A área de exploração continua reduzida nessa resolução.
- TASK-130: suíte inicial falhou ao construir o contexto acessível de `NavigationMap`. Log bruto preservado em `../TASK-corujao3-130-java.log`; a suíte final passou após implementar o contexto acessível.
- TASK-131: probe excedeu a espera pelo diálogo porque novos rótulos tinham acentos substituídos por `?` durante gravação pelo shell. Textos corrigidos em UTF-8; probe e regressão repetidos com sucesso. O timeout foi observado no terminal; não se apresenta este registro como log bruto.
- TASK-133: a primeira captura de comércio foi obtida antes de o timer refletir o novo saldo. O probe passou a aguardar o saldo confirmado na própria janela antes da captura. A compra única e o saldo no snapshot já estavam corretos. Capturas anteriores preservadas em TASK-129.

Nenhum resultado esperado de regra de jogo foi alterado para ocultar falhas. As rodadas finais aprovadas estão em `validacao-final/summary.json` e `../TASK-corujao3-133-final-qualidade.json`.
