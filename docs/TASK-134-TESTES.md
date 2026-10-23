# TASK-134 ? Testes reais

- `python tools/run-local-tests.py`: 419 executados, 413 aprovados, 6 abortados, 0 falhas, 0 ignorados. Compila??o de 191 fontes e 144 arquivos de testes com --release 17. Log copiado para o checkpoint.
- `python -m unittest discover -s tools -p 'test_*.py'`: 30 testes, OK. Mensagem de argumento inv?lido ? produzida por teste negativo e n?o indica falha da su?te.
- Reexecu??o com `java -Djava.awt.headless=false ... LocalJunitRunner com.projetok.studio`: 45 executados/aprovados, 0 abortados, 0 ignorados, 0 falhas; `gui-tests.log`.
- Benchmark recompilado contra classes desta execu??o: `tools/WorldStudioBenchmark.java`, sa?da em `benchmark/metrics.json`.

## Classifica??o dos seis abortados

Todos pertencem a StudioUsabilityTest, usam assumeFalse(GraphicsEnvironment.isHeadless()) e s?o dependentes de ambiente. Todos passaram ao habilitar interface gr?fica:

1. mouseButtonsKeepIndependentAssetsCropsAndPaintingModes
2. dragSelectMoveCopyAndPastePreserveTheGroup
3. librarySelectionOpensFullTilesheetIncludingPartialEdges
4. paletteUsesActualMouseButtonWithoutOverwritingOtherBrush
5. tilesheetDragCopiesWholeRegionWithoutShrinkingIt
6. rightDragFillsIntermediateCellsAndCommitsOneUndo

## Medi??o inicial

Java 17.0.20.1, Windows, 16 processadores l?gicos, 254 assets, 12.000 elementos. Indexa??o 1288,3738 ms; salvamento 8189,5514 ms; carregamento 53,3809 ms; abertura de janela 319,5701 ms. Pintura p50/p95 em ms: zoom 0,25 = 12,0998/21,8009; 0,75 = 3,5008/4,4904; 2 = 1,9478/2,5479; 8 = 1,4352/1,7178. Medi??o ?nica; caches n?o controlados. N?o representa ganho de otimiza??o.
