# Corujão III — estado consolidado

| TASK | Estado | Entrega |
|---|---|---|
| 117 | VALIDADA COM RESSALVAS | [Relatório](TASK-117-RELATORIO-FINAL.md) |
| 118 | VALIDADA COM RESSALVAS | [Relatório](TASK-118-RELATORIO-FINAL.md) |
| 119 | VALIDADA COM RESSALVAS | [Relatório](TASK-119-RELATORIO-FINAL.md) |
| 120 | VALIDADA COM RESSALVAS | [Relatório](TASK-120-RELATORIO-FINAL.md) |
| 121 | VALIDADA COM RESSALVAS | [Relatório](TASK-121-RELATORIO-FINAL.md) |
| 122 | VALIDADA COM RESSALVAS | [Relatório](TASK-122-RELATORIO-FINAL.md) |
| 123 | VALIDADA COM RESSALVAS | [Relatório](TASK-123-RELATORIO-FINAL.md) |
| 124 | VALIDADA COM RESSALVAS | [Relatório](TASK-124-RELATORIO-FINAL.md) |
| 125 | VALIDADA COM RESSALVAS | [Relatório](TASK-125-RELATORIO-FINAL.md) |
| 126 | VALIDADA COM RESSALVAS | [Relatório](TASK-126-RELATORIO-FINAL.md) |
| 127 | VALIDADA COM RESSALVAS | [Relatório](TASK-127-RELATORIO-FINAL.md) |
| 128 | VALIDADA COM RESSALVAS | [Relatório](TASK-128-RELATORIO-FINAL.md) |
| 129 | VALIDADA COM RESSALVAS | [Relatório](TASK-129-RELATORIO-FINAL.md) |
| 130 | VALIDADA COM RESSALVAS | [Relatório](TASK-130-RELATORIO-FINAL.md) |
| 131 | VALIDADA COM RESSALVAS | [Relatório](TASK-131-RELATORIO-FINAL.md) |
| 132 | VALIDADA COM RESSALVAS | [Relatório](TASK-132-RELATORIO-FINAL.md) |
| 133 | VALIDADA COM RESSALVAS | [Relatório](TASK-133-RELATORIO-FINAL.md) |

Marcos A (117–120), B (121–125), C (126–128) e expansão (129–133). A extensão complementar autoriza continuar até 133. Estados são técnicos; não representam aprovação artística.

## Entrega final

**400 testes Java e 30 testes Python, sem falhas. Oito probes gráficos aprovados.**

[Pacote privado Windows com Java integrado](../dist/SpiritGate-RPG-Sprites-Teste-0.133.0-rc2-20261021T035239Z-034cc11f.zip). Não publicado. Extraia todo o ZIP e abra `SpiritGateRPG.exe`; arte de personagens continua experimental.

[Auditoria do pacote](task-133/package-audit.json), [inicialização real do EXE](task-133/package-smoke.json), [probes finais](task-133/validacao-final/summary.json), [percurso integrado](task-133/validacao-final/SpiritGateFinalJourneyProbe/probe.txt), [medidas](task-133/validacao-final/SpiritGatePerformanceProbe/performance.json).

Abertura de janela: 416.2 ms. Média de carregar/restaurar 100 saves: 4.24 ms. Heap adicional observado sem isolamento de GC: 17825864 bytes. Pintura Swing em movimento: 22.4 ciclos/s; não equivale a FPS físico. Minimap offscreen: 28.1 µs/pintura após aquecimento. Sem garantia geral de ausência de pausas da EDT.

O percurso usa o mesmo personagem criado pela UI, com duas fixtures explícitas para comércio/combate. Valida integração e persistência; não substitui uma campanha natural jogada por uma pessoa.

SHA-256 do ZIP: `ab75e4f7503d9a0f40de7500985c6031e3b6d62b08f5ea18354e00a783eec467`. Nenhum save pessoal incluído; 169 arquivos verificados e recursos coincidem com a fonte. Versões anteriores preservadas.
