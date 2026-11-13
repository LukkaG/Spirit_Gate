# TASK-201 — testes e evidências

Auditoria e aprovação: 27/10/2026. Fechamento local: 28/10/2026.
A implementação aprovada inclui aplicação 5, fallbacks e redução das janelas
de entrada. A rodada final passou com as fontes congeladas, incluindo
cancelamento durante conexão, reconexão durante encerramento, erro de rede
tardio e recuperação da falha de gravação do ACK após iniciar Java.

## Baseline histórico da auditoria

Estes resultados precedem a implementação e permanecem como referência:

| Verificação | Resultado |
|---|---|
| `python tools/run-local-tests.py` | 239 fontes e 203 arquivos de testes; 641 executados, 631 aprovados, 10 abortados por GUI, 0 falhas e 0 skips. |
| Complemento gráfico do baseline | 17 aprovados; cobre os 10 abortados e repete sete casos. |
| Suite Python do baseline | 94 aprovados. |
| Preservação na auditoria | 1.242 arquivos de fonte/assets/comandos e oito arquivos pessoais conferidos, sem divergências naquela etapa. |

O relatório `docs/TASK-035-estresse.json`, já modificado antes da auditoria,
foi protegido e restaurado para seus bytes anteriores quando necessário.
As capturas regeneradas ficaram no checkpoint 201, preservando as exclusões
autorizadas na limpeza anterior.

## Execuções da implementação registradas

| Verificação | Resultado e evidência |
|---|---|
| Suite Java completa final | 243 fontes e 209 arquivos de testes; **671 executados, 651 aprovados, 20 abortados por GUI, 0 falhas e 0 skips**. `implementation/java-headless.log` e `java-headless.json`; hashes estáveis durante a execução. |
| Complemento gráfico consolidado | Oito classes, **28 executados e aprovados**, 0 abortos/falhas/skips. Cobre os 20 abortos headless e repete oito casos já aprovados. `implementation/java-graphical-consolidated.json`. |
| Suite Python completa final | **97 aprovados**, incluindo os três testes do handoff. `implementation/python-final.json`. |
| Handoff após a correção do ACK | **3 testes aprovados**, já incluídos nos 97 Python. `launcher-handoff/verification.json`. |
| Atualizador PowerShell após a correção do ACK | **39 verificações aprovadas**. `launcher-handoff/updater-regression/results.json` e `verification.json`. |
| Transporte GitHub PowerShell após a correção do ACK | **21 verificações aprovadas**, com transporte HTTPS simulado; não são novos downloads de produção. `launcher-handoff/github-regression/results.json` e `verification.json`. |
| Compilação de entrada | `tools/compile-local.ps1`: 243 fontes Java compiladas. |

O consolidado gráfico contém fluxo de janelas 1, Radmin 1, cancelamento Radmin 7,
reconexão/encerramento 3, gestos do Studio 1, quests 1, lifecycle TASK-191 7 e
usabilidade do Studio 7. As oito repetições são os sete casos de usabilidade e
um caso do formatador do painel, já aprovados em headless. Os 20 abortos foram
conferidos por classe contra o log completo e cobertos nesse consolidado.

As rodadas intermediárias de 18 gráficos e dos seis focados após compartilhar
a apresentação permanecem nas evidências; o fechamento usa a rodada consolidada.

Não somar execuções gráficas repetidas como testes únicos adicionais.
O aborto headless significa que a janela exige ambiente gráfico; não é
apresentado como aprovação visual. As execuções gráficas usam Swing real.

## Matriz executada

| Área | Casos e resultado |
|---|---|
| Modelo e formulário | Nome normalizado, quatro raças, recuperação da escolha, prévia por atlas e dimensões de painel. Dados inválidos são recusados. |
| Aparência | Reaper sem acessórios incompatíveis; Aetheri/Spirit identificados como Human provisório; atlas obrigatório ausente impede confirmar. |
| Protocolo | Round-trip do opcode 16, bytes históricos V1–V4, rejeição de truncamento e campos excedentes. |
| TCP e identidade | Servidor e dois clientes reais; quatro raças iguais com nomes iguais e IDs distintos; Human antes de Reaper; reconexão com nova sessão e escolha preservada. |
| Admissão negativa | Perfil inválido/ausente não cria ator; acessórios Reaper indevidos e segunda criação não alteram outro jogador. Servidor sem capacidade retorna orientação de atualização. |
| Conteúdo | Cliente selecionado e V4 publicam/retornam revisão mantendo a raça; peer V1 continua bloqueando publicação incompatível. |
| Janelas Java | Modal de seleção, descarte da navegação após entrada e preservação do anfitrião na troca de janela/reconexão; encerramento da janela final fecha o servidor. Resultados de conexão aguardam a decisão de cancelamento; reconexão não substitui a janela sob confirmação de encerramento. |
| Renderer | O jogo recebe `Presentation` com os atlas/camadas do worker. A prova gráfica verifica `usesReaper("reaper")` e captura os corpos Human/Reaper. |
| Handoff WinForms | Processo filho continua após a UI sair; papel, argumentos, diretório e lock conservados. Executável inválido/saída imediata mantêm o formulário recuperável; confirmação fora da instalação é recusada. Falha ao gravar ACK encerra somente o filho novo e espera a saída antes de liberar o lock, inclusive quando a tentativa de encerramento é negada. |
| Offline e dados pessoais | O formulário não cria janelas próprias, não usa saves offline e não grava dados pessoais. As regressões offline, Coop e persistência permanecem na suite geral. |

O ACK do handoff confirma processo vivo após a checagem inicial de 350 ms.
Não confirma que a conexão TCP ou o carregamento do mundo já acabaram.
O teste WinForms usa um helper local limitado; não apresenta esse helper
como uma sessão de jogo em dois computadores.

## Correções verificadas no fechamento

1. **Apresentação compartilhada:** prévia, jogo e reconexão recebem os mesmos
   atlas/camadas carregados no worker. A prova de fluxo usa os corpos reais
   Human/Reaper e verifica o atlas Reaper explicitamente.
2. **Cancelamento Radmin:** sete casos, sendo seis gráficos e um de formatação,
   verificam resultados de conexão durante a decisão, confirmação aceita ou
   recusada e descarte dos recursos. Evidência em `implementation/review-cancel/`.
3. **Reconexão durante encerramento:** três testes com `LauncherSession` e
   `LocalClient` reais. Uma ponte TCP local somente retarda ou corta bytes;
   não fabrica mensagens. Os três reproduziram falhas antes da correção e
   passaram depois: confirmação aceita, recusada com nova tentativa bem-sucedida
   e falha de rede após fechar o host, sem popup tardio. Evidência em
   `lifecycle-reconnect/`, com capturas e resultados por execução.
4. **Falha de gravação do ACK:** injeção determinística após iniciar o helper
   comprova que somente o filho novo é encerrado, que o processo anterior
   sobrevive e que o lock permanece até a saída. O caso complementar de
   encerramento negado mantém a supervisão. Evidência em `launcher-handoff/`.

## Classes novas e regressões

- `multiplayer/SessionCharacterSelectionTest`: modelo, frames e autoridade.
- `multiplayer/Task201CharacterSessionTest`: sessões TCP reais e compatibilidade.
- `prototype/rpg/SessionCharacterCreationTest`: quatro casos de formulário,
  recursos, paint e ausência de escrita em saves.
- `prototype/rpg/Task201SessionWindowFlowTest`: fluxo gráfico integrado,
  dois clientes reais e apresentação compartilhada na reconexão.
- `prototype/rpg/Task201RadminCancellationTest`: seis casos gráficos de
  cancelamento concorrente e um caso de formatação do painel.
- `prototype/rpg/SessionGameReconnectCloseTest`: três cenários gráficos de
  reconexão/encerramento, com servidor TCP real.
- `tools/test_launcher_handoff.py` e `updater/Test-Launcher-Handoff.ps1`:
  passagem da UI WinForms para processo independente.

Continuam aplicáveis as regressões de protocolo, backpressure, matriz de
admissão, cosméticos, colisão, combate compartilhado, revisão de mundo,
rollback, reconexão, saves V1/V2, raças, atlas Reaper e camadas normalizadas.
Expectativas históricas V1–V4 foram conservadas; os casos de seleção usam
a capacidade negociada nova.

## Evidências

Raiz: `.task-checkpoints/TASK-201/corujao-vii/`.

- Auditoria: `baseline.json`, `status-before.txt`, `head-before.txt`,
  `diff-before.txt`, `baseline-source.zip`, `sources-before.json`,
  `personal-data-before.json` e `audit-verification.json`.
- Baseline: `baseline-java.*`, `baseline-graphical.json`, `baseline-python.*`,
  `graphical-*.log`, `baseline-ui/` e `baseline-test-images/`.
- Backend isolado: `backend-validation/results.json` e `tcp-tests.log`.
- UI isolada: `ui-unit/tests-final.log` e `results.json`.
- Implementação: `implementation/`, com logs completos e resultados
  gráficos/Python. `java-headless.json`, `java-graphical-consolidated.json`
  e `python-final.json` registram as rodadas finais. Rodadas intermediárias
  estão identificadas pelo nome.
- Revisão contra o baseline: `implementation/task201-source.patch` e
  `source-changes.json`, com 26 arquivos de código/testes, preservando as
  alterações anteriores à TASK-201.
- Cancelamento: `implementation/review-cancel/results.json`, `tests.log` e capturas.
- Reconexão/encerramento: `lifecycle-reconnect/reproduction.log`, `fixed.log`,
  `fixed.json` e diretórios `run-*/` com capturas e `result.json`.
- Modal real: `modal-validation/host.png` e `join.png`.
- Fluxo integrado: as provas de ingresso em `ui-flow/run-*/` contêm `result.json`,
  `selection-0.png`, `selection-1.png`, `game-human.png`, `game-reaper.png`
  e `reconnected-human.png`. A prova de apresentação compartilhada inclui
  a asserção de atlas Reaper; execuções intermediárias não a substituem.
- Handoff: `launcher-handoff/verification.json` e suas regressões.

As capturas são imagens dos componentes Swing reais. Não substituem aceite
artístico humano ou teste entre computadores.

## Preservação e disponibilidade

A conferência da implementação manteve **705 assets** e **oito arquivos
pessoais** sem divergências. No conjunto de **1.242 arquivos do baseline**,
**14 existentes** tiveram as alterações previstas e **1.228 ficaram intactos**;
nenhum estava ausente. Foram registrados **12 arquivos novos de código/testes**.
As **2.432 exclusões anteriores** da limpeza foram preservadas. A conferência
está em `implementation/data-integrity.json`, com fontes estáveis desde os
testes finais e os bytes anteriores de `TASK-035-estresse.json` preservados.

A distribuição oficial 0.201.0 conserva os dois canais, com 391 arquivos
por papel e manifesto SHA-256
`252cb97262083ebfc9aea79bdbd4ea4a9de04a24f647d9597634c02522276547`.
O ZIP fonte permanece com SHA-256
`1a56363a01137165cecdda10b5f9dea6dd750c4c79ddb4c456e9dc336b908b2c`.
Essas verificações não significam que a distribuição recebeu o código novo.

Nenhum pacote novo, commit, push, release, dependência ou endpoint remoto
foi introduzido nesta tarefa. Para testar os fontes, usar `rodar-prototipo.cmd`
e as opções Radmin do Portal de entrada. O launcher oficial 0.201.0 continua
com sua implementação anterior até uma entrega posterior autorizada.

## Limitações e fechamento

Não houve nova prova em dois computadores ou Radmin físico. Aetheri/Spirit
permanecem com aparência Human provisória. O aceite humano do Corujão VI é
informação histórica do responsável, e não teste novo do Corujão VII.

A TASK-201 está implementada e validada localmente. O próximo passo é a revisão
local do responsável, incluindo legibilidade e experiência visual. A TASK-202
não foi iniciada e depende de nova instrução; o Gate A continua dependendo
dela, e os demais gates permanecem fora do escopo desta execução.
