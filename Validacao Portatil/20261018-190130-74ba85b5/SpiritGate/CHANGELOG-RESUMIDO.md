# Spirit Gate 0.11.0-rc1 - Candidata a homologação

Data: 18/10/2026. Windows x64, Java 17 integrado. Candidata local de testes; não é release pública, instalador ou cliente multiplayer.

Base: TASK-006.1 com 170 testes aprovados.

- TASK-007: SpiritGate.exe portátil com app/ e runtime/, inventário e hashes.
- TASK-008: dois arbustos e um poste leves demonstram WASD bloqueante e dash seletivo.
- TASK-009: Q/Shift mostram disponibilidade, custo, recarga, vigor insuficiente e feedback real.
- TASK-010: sequências determinísticas de comandos, colisões, Freeze e reinício; corrigido aviso de vigor que arredondava um saldo insuficiente para o custo.
- TASK-011: compilação consolidada, pacote novo e kit de homologação posterior.

Suíte consolidada: 202 testes, zero falhas, erros ou ignorados; repetição offline estável. Fórmulas de dano e sistemas anteriores preservados. Não há novas dependências externas de runtime.

O SHA-256 do ZIP fica no arquivo externo .sha256; manifest.json contém os hashes individuais. O hash do próprio ZIP não é incluído dentro dele para evitar referência circular. Consulte o relatório consolidado da rodada para logs e limitações.

Pendências: outro Windows sem Java/Maven e revisão humana de cada tarefa. Uma espera por saída livre pode persistir se todos os pontos da trajetória ficarem ocupados por atores. O EXE não é assinado.
