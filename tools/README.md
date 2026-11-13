# Ferramentas atuais do Spirit Gate

Os scripts de execucoes antigas de tarefas, pacotes Corujao e probes avulsos
foram removidos. Permanecem as ferramentas reutilizaveis abaixo.

## Compilacao e distribuicao

- `compile-local.ps1` e `launch-local.ps1`: compilacao Java 17 e abertura local.
- `build-multiplayer-update.py`: recompila o jogo e prepara a proxima versao.
- `integrar_launcher_radmin.py` e `preparar_atualizacao.py`: pacotes Anfitriao e Convidado.
- `publicar_atualizacoes_git.py`: preparacao e publicacao do canal GitHub.
- `updater/`: bootstrap, interface e atualizador distribuidos aos jogadores.
- `build-*-portable.*` e `build-world-studio.ps1`: empacotadores reutilizaveis.
- `build-skills-portable.ps1`: previa das 40 skills com Java integrado para outro
  Windows x64; pacote/instrucoes em [testes portateis](../testes-portateis/skills-windows-x64/README.md).
- `testar-skills.cmd`: previa pelos fontes, com JDK 17, sem Maven/cache local.

O procedimento atual esta em [LAUNCHER-GITHUB.md](../docs/LAUNCHER-GITHUB.md).
As ferramentas CDN continuam preparadas para uma futura migracao.

## Testes automaticos

- `monitorar_erros.py` e `coletar_erros_ide.py`: coleta e atendimento local dos
  erros; veja [ambiente de logs](../logs/README.md). O atalho da raiz
  `monitorar-erros.cmd` inicia, consulta e para o monitor.

- `server/src/test`: suite Java de regras do jogo, multiplayer, colisao e persistencia.
- `test_*.py`: verificacoes de assets, mapas, ZIPs, launcher e publicacao.
- `qualidade.py` e `run-local-tests.py`: execucao das suites.
- `updater/Test-*.ps1`: verificacoes de transporte, rollback e abertura.
- `UpdatableRadminUiProbe.java`: auxiliar exigido por `updater/Test-Radmin-Launch.ps1`.
- `test-windows-portable.ps1`, `SmokePrivate*.ps1` e `smoke_private_multiplayer.py`:
  verificacoes reutilizaveis dos pacotes nativos.

Esses testes ficam no desenvolvimento. Os pacotes dos jogadores incluem
somente o jogo e os arquivos necessarios para abrir e atualizar.

## Criacao de conteudo e documentacao

As ferramentas de assets, sprites, mapas, auditoria e PDF permanecem.
`GenerateTemporaryCosmetics.java` gera os cosmeticos experimentais existentes.
`WorldStudioBenchmark.java` acompanha o manual de desempenho do Studio.
`gerar_pdf_task002.py`, apesar do nome historico, e uma biblioteca usada por
outras ferramentas de documentacao.

O inventario removido esta no
[relatorio geral da limpeza](../docs/RELATORIO-GERAL-LIMPEZA-2026-10-27.md).
