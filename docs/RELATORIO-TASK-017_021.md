# Spirit Gate - Relatório consolidado TASK-017 a TASK-021

18/10/2026. Trabalho concluído no Projeto-K local. Todas as etapas passaram pelos controles automatizados disponíveis. Aprovação artística e homologação humana continuam pendentes.

## Entrega executável

ZIP final: dist/SpiritGate-0.21.0-rc1-20261019T001737Z-546ce1c5-portable.zip

Tamanho: 29.391.539 bytes.

SHA-256: 303f469b87aa0752d6dcd44e21ae5efef78295cf971884976939b1d8ae6cc534

Extraia integralmente e abra SpiritGate/SpiritGate.exe. Java 17 está integrado. Preserve app/ e runtime/. O EXE não tem assinatura. Kit de teste acompanha o ZIP. O candidato intermediário 539628a7 permanece no dist, mas não é o indicado para entrega.

## O que foi realizado

TASK-017: reexecutada a auditoria da base original 0.16.0-rc1. Confirmados tamanho, hash documentado, manifest, inventário, classes do JAR, runtime e PE x64. 216 JUnit e 12 Python verdes. Controle negativo rejeitado e dois smokes Windows locais aprovados. Sem correção de gameplay necessária.

TASK-018: ferramenta de autoria/revisão baseada no pipeline TASK-013, template transparente vazio com exatamente 59 posições 64x64, índice com ação/direção/frame/duração/pivot, previews HTML e relatório por arquivo. Importação Aseprite estrita, sem autocorreção. Testes de ordem, direção, ausência, pivots, alfa, sobreposição e hash. 216 JUnit e 16 Python verdes.

TASK-019: estendido o loader existente para pacote visual v1, com identidade estável, SHA e origem de revisão, 59 recortes, pivots/offsets e dimensões. Ativação opcional junto ao EXE, uma vez na inicialização; corrupção/ausência mantém fallback. Staging por whitelist e auditoria do pacote. Fixtures exclusivamente sintéticas nos testes. 219 JUnit e 18 Python verdes.

TASK-020: ArenaViewport pura para mundo/tela, limites da câmera, zoom inteiro 1 a 4, escala proporcional e letterbox. Renderer aplica câmera ao mundo e mantém HUD fixo no canvas. Janela redimensionável. Shift/Q convertem mira no momento do comando. 223 JUnit e 18 Python verdes, incluindo comparação integral dos snapshots, colisões, reinício e render puro.

TASK-021: clean rebuild de 0.21.0-rc1, repetição offline, auditoria do ZIP original, controle negativo, teste de distribuição visual sintética e smokes Windows padrão/mapa. Kit humano, documentação, PDFs, diffs, checkpoints e inventário de entregas consolidados.

## Preservação e ajustes

99 arquivos Java históricos de domínio/testes comparados sem alteração. Os 216 testes da base foram mantidos; sete novos testes Java e seis Python acrescentados ao total. Movimento, SOLID/SOFT, dash seletivo, retorno, vigor, Criostase, Freeze, custos/danos/cooldowns, FIFO, morte, R e replays continuam cobertos pela suíte.

Os 228 PNGs preservaram seus SHA-256 e continuam sem aprovação; nenhuma imagem artística foi redimensionada, recortada ou substituída. Produção segue no fallback geométrico. Template vazio e capturas sintéticas estão identificados. Não foi gerado atlas de produção.

Ocorrências resolvidas: argumento Maven passou a ser invocado com aspas no PowerShell; BOM de relatório removido para o gerador PDF; corrigida codificação textual de relatórios; fixture nova de câmera ajustada para alvo visível relativo ao spawn real, após recusa correta de alvo fora da viewport. Nenhuma asserção ou baseline histórico alterado. Logs da primeira execução foram preservados.

## Evidências e reprodução

Relatórios por etapa: docs/TASK-017.md a TASK-021.md e Spirit_Gate_TASK-NNN.pdf. Diffs por etapa: docs/TASK-NNN.diff. Referências da base e estados finais: .task-checkpoints/TASK-NNN-before.json e after.json; sem Git/push.

Logs de Java/Python por etapa, TASK-021-build-final.log e TASK-021-offline.log. Auditoria final TASK-021-auditoria.json; controle negativo TASK-021-negativo.json; pacote sintético TASK-021-visual-distribuicao.json; preservação TASK-021-preservacao.json.

Smokes finais: TASK-021-smoke.json e TASK-021-map-smoke.json. Capturas TASK-021-exe.png, TASK-021-mapa-exe.png e TASK-020-camera-sintetica.png. Template: docs/autoria-018; preview final: autoria-018/revisao-final/preview.html. Revisão dos PNGs: docs/revisao-fontes-021/preview.html e relatorio.json, com referências locais aos originais.

Java: mvn.cmd -o '-Dmaven.repo.local=.maven-repository' -f server/pom.xml clean test

Python: python -m unittest discover -s tools -p 'test_*.py'

Build: powershell -NoProfile -ExecutionPolicy Bypass -File tools/build-windows-portable.ps1 -Version 0.21.0-rc1 -MavenRepository .maven-repository -TesterKitDirectory docs/kit-homologacao-017-021

Auditar: python tools/auditar_rc.py --zip ARQUIVO --report RELATORIO

## Limites e pendências

Smokes ocorreram neste Windows, em pasta com espaços fora do checkout, PATH sem Java/Maven e JVM integrada comprovada. Ambas as variantes encerraram com código 0. Não houve homologação em outro computador, sessão humana de jogabilidade ou auditoria Linux. Inspeção de sockets por CIM indisponível. Hashes verificam integridade; não substituem assinatura/autenticidade ou auditoria absoluta de segredos.

Mapa de produção continua 960x640. Mundo maior foi exercitado somente na transformação sintética. Zoom opcional existe como API de integração, sem atalho novo. Em janelas muito pequenas, redução prejudica legibilidade. Rota de retorno totalmente ocupada pode continuar aguardando saída, comportamento histórico.

Aprovação real de arte exige revisão humana do SHA exato e origem registrada. Qualquer edição invalida aprovação anterior. Nenhum trabalho foi deixado como exercício obrigatório para concluir automação; permanecem somente os limites humanos/externos documentados. Sem rede, banco, credenciais, publicação ou multiplayer alterados. Rodada encerrada na TASK-021.
