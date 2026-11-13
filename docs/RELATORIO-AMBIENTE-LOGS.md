# Ambiente local de logs e atendimento de erros

Data: 01/11/2026. Checkout verificado: branch `beco`, HEAD `6d72c65`.
Havia 46 arquivos modificados, 2432 exclusoes e 246 caminhos nao rastreados
antes desta entrega; essas alteracoes anteriores foram preservadas.

## Resultado

Foi criado o ambiente descrito em [logs/README.md](../logs/README.md): TXT locais
para erros do Java e dos scripts de abertura/compilacao, importacao dos erros
Oracle Java do workspace e atendimento sequencial pelo Codex CLI existente.
O monitor foi ativado e sua primeira analise real foi iniciada. A entrega do
ambiente nao equivale a homologacao das correcoes futuras.

O acompanhamento depende do computador ligado, do processo ativo, da
autenticacao e da disponibilidade do Codex. As respostas sao gravadas em
`logs/analises`, em atendimentos separados desta conversa. Nenhum servico de
inicializacao do Windows foi instalado. Os trechos enviados ao Codex usam a
conta ja autenticada e seus limites.

## Arquivos e comportamento

- `server/src/main/java/com/projetok/diagnostics/ErrorReports.java`: captura de
  stderr e excecoes nao tratadas, UTF-8, concorrencia, fallback no console,
  handler anterior e rotacao em partes de ate 4 MiB sem excluir arquivos.
- Integracoes localizadas em `RpgLauncher`, `LauncherSession`, `RpgRuntime`,
  `LocalSimulationServer`, `RpgNetworkClientLauncher`, `NetworkClientLauncher`,
  `DebugLauncher`, `DebugRuntime`, `WorldStudio` e `StudioPlaytest`.
- `ErrorReportsTest.java` e `ErrorReportsProbe.java`, no pacote de testes
  `com.projetok.diagnostics`: verificacao em JVMs isoladas.
- `tools/compile-local.ps1` e `tools/launch-local.ps1`: gravacao das falhas
  locais pelo novo `tools/registrar-erro-local.ps1`, mantendo a compilacao com
  Java 17 e a recusa de classes antigas em caso de falha.
- `tools/coletar_erros_ide.py` e seu teste: coleta incremental somente dos logs
  Oracle vinculados ao workspace, com cursor e publicacao atomica dos TXT.
- `tools/monitorar_erros.py` e seu teste: estabilidade da escrita, deltas,
  checkpoints, tentativas com intervalo, trava de instancia unica, respostas,
  parada graciosa e encerramento da arvore da analise em timeout/interrupcao.
- `tools/monitorar-erros.ps1` e `monitorar-erros.cmd`: iniciar em segundo plano,
  consultar e parar; `tools/test_local_error_reports.py` verifica os scripts.
- `.gitignore`: ignora somente os relatorios gerados nas subpastas de `logs`.
  `AGENTS.md`, `tools/README.md` e `logs/README.md`: instrucoes de atendimento e uso.
- `.vscode/settings.json` (local, ignorado): JDK 26 para o Oracle Language
  Server, JDK 17 para o projeto; os dois JDKs ja estavam instalados.

## Erro real encontrado

Quatro logs Oracle das duas sessoes recentes indicavam que o servidor de
linguagem tentava iniciar com Java 17 e recusava versoes anteriores ao Java 21.
Os TXT originais foram importados em `logs/erros/ide-oracle-*.txt`.

As configuracoes `jdk.jdkhome` e `jdk.project.jdkhome` foram confirmadas no
manifesto e nas descricoes da extensao Oracle instalada. Foram definidos,
respectivamente, o JDK 26 existente e o Temurin 17 existente. O projeto e o
alvo de compilacao continuam no Java 17. A validacao do servidor de linguagem
apos recarregar o VS Code continua pendente.

## Verificacao executada

Na pasta `server`, usando o cache Maven existente, sem instalar dependencias:

```powershell
mvn.cmd -o '-Dmaven.repo.local=../.maven-repository' '-Dtest=ErrorReportsTest,DiagnosticReportTest,CorujaoRuntimeTest,RpgRuntimeSaveTest,Task189DiagnosticsTest,LocalServerTest' test
```

Resultado: **21 testes Java aprovados**, zero falhas/erros. Incluem os dez
cenarios novos do coletor, EDT, concorrencia, Unicode fragmentado, rotacao,
handler anterior, disco indisponivel, falhas agendadas e regressao dos
diagnosticos, simulacao e salvamento.

Na pasta `tools`:

```powershell
python -m unittest test_local_error_reports test_coletar_erros_ide test_monitorar_erros -q
```

Resultado: **29 aprovados e um ignorado**, entre 30 testes. O ignorado exige
criar link simbolico, operacao recusada por este Windows. Houve verificacao
real de erro do javac em projeto descartavel com espacos no caminho, mantendo
as classes anteriores e o diagnostico; nenhuma fonte do jogo foi corrompida.

Na raiz:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/compile-local.ps1 -Force
```

Resultado: **268 fontes compiladas para Java 17**, sem erro. O compilador
emitiu avisos de operacoes unchecked existentes; nao houve expansao de escopo
para altera-los.

Tambem foram verificados sintaxe dos scripts PowerShell, inicio/status/parada
do processo real em modo de aviso, importacao real dos quatro TXT da IDE e
inicio de atendimento real pelo Codex com `workspace-write`, sem elevacao.
O primeiro atendimento ainda estava em andamento no registro desta entrega.

## Compatibilidade e pendencias

Nao foram alterados contratos de rede, serializacao, autoridade, regras de
gameplay, formatos de saves, HUD, assets ou publicacao. Offline e Coop passam
a registrar erros nas entradas integradas, mantendo suas regras existentes.
A exportacao manual de diagnostico e seu contrato de privacidade foram
preservados e testados. Nao houve commit, push, release ou publicacao.

Catches que descartam excecoes sem registro, falhas fatais da JVM e processos
nao integrados continuam fora da captura. Comandos Maven/terminais executados
diretamente precisam ter sua saida salva em TXT para entrar nesta fila.
Relatorios e respostas nao sao apagados automaticamente; arquivamento manual
com o monitor parado pode ser necessario para liberar espaco.

Testes fisicos, Radmin, launcher nativo e o reinicio efetivo do servidor de
linguagem permanecem pendentes. Aplicacoes Java ja abertas precisam ser
reiniciadas para usar o coletor compilado.

## Reversao e proximo passo

Solicite a parada com `monitorar-erros.cmd -Acao Parar` e confirme o status
encerrado. Remova apenas as integracoes e arquivos desta entrega, preservando
os relatorios e os diffs anteriores; nao restaure arquivos inteiros do Git
que ja tinham mudancas locais. As duas chaves locais da extensao podem ser
removidas separadamente caso se deseje reverter essa configuracao.

Proximo passo: usar **Developer: Reload Window** no VS Code e verificar se o
Oracle Language Server inicia sem novo erro. Depois, acompanhar o status e as
respostas em `logs/analises` durante o uso normal do jogo.
