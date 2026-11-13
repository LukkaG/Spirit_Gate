# Continuidade — Spirit Gate

## Adendo visual da HUD — 31/10/2026

Refinamento imediato aplicado após a TASK-213: três blocos inferiores, hotbar
com quatro ações e reservas laterais sem comandos, centro sem logo e HP/energia
alinhados ao painel central. Utilitários uniformizados. Nenhuma alteração de
protocolo/gameplay. Cinco execuções dirigidas aprovadas, com dois clientes TCP,
três resoluções e abertura/fechamento de painéis.

[Adendo obrigatório para 214/217](docs/CORUJAO-VIII-ADENDO-HUD.md) e
[captura atual](docs/TASK-213-ADENDO-HUD-1366.png). Fonte alterada: NetworkGameHud,
NetworkCombatHud, NetworkHotbar; prova gráfica ampliada. Checkpoint:
`.task-checkpoints/TASK-213-ADENDO/corujao-viii/`. Pacotes anteriores intactos;
TASKs 214/217 não iniciadas. Revisão visual humana pendente.

## Corujão VIII — TASK-213 — 31/10/2026

Prompt e quatro wireframes recebidos; a primeira ação limita a execução à 213.
Nova HUD principal do multiplayer V6 implementada e validada nos fontes, com
minimapa no alto à direita, recursos/marca/hotbar na base e botões para os painéis
existentes. Legados/offline/Coop mantêm apresentação anterior. TASK-214 não iniciada.

Branch beco, HEAD `6d72c6595dee6dbe4dea832fe6047ee81f51d4a3`. Sem commit/push.
Java: 692 executados, 670 aprovados em headless, 22 abortos GUI; complemento:
30 aprovados, cobrindo os abortos e repetindo oito casos; zero falhas/skips.
3.912 arquivos de assets/saves/distribuição protegidos intactos. Fontes estáveis
nos testes finais. Capturas anteriores e exclusões históricas preservadas.

Novos fontes: GameHudStyle, NetworkMinimap, NetworkGameHud. Alterados: launcher
multiplayer, NetworkCombatHud/NetworkHotbar, ArenaViewport/DebugPanel e exposição
do diálogo de opções existente. Dois testes novos e adaptação da prova gráfica
212 à abertura do diário pelo livro/J. Helper de testes aceita o ciclo VIII.

[Entrega](docs/TASK-213.md), [testes](docs/TASK-213-TESTES.md),
[progresso](docs/CORUJAO-VIII-PROGRESSO.md). Evidências:
`.task-checkpoints/TASK-213/corujao-viii/` e `TASK-213-GUI/corujao-viii/`.
Execute `rodar-prototipo.cmd`, opções Radmin, para usar os fontes atuais.
Pacotes/EXEs anteriores continuam anteriores à nova HUD; build do ciclo fica
para a 222. Próximo passo: revisão visual local e tarefa de inventário (214).

Os registros do Corujão VII abaixo são históricos. O aceite daquele ciclo
foi informado pelo usuário no novo prompt, sem representar teste físico novo.

## Fechamento técnico local — TASKs 201–212 — 31/10/2026

O usuário autorizou todas as tarefas do plano e solicitou PDF completo.
As TASKs 203–212 foram implementadas em sequência e integradas às 201/202.
Última tarefa validada: TASK-212. Próximos passos externos: revisão visual e
teste Radmin físico. Branch `beco`, HEAD
`6d72c6595dee6dbe4dea832fe6047ee81f51d4a3`, sem commit/push.

Entregas: [relatório completo](docs/CORUJAO-VII-RELATORIO-FINAL.md),
[testes finais](docs/TASK-212-TESTES.md) e
[PDF](docs/Spirit_Gate_Corujao_VII_Relatorio_Completo.pdf).
Pacotes privados em `dist/SpiritGate-CorujaoVII-0.212.0-private`, com papéis
Anfitriao/Convidado e feed remoto desativado. Distribuição oficial anterior intacta.

Java completo: 689 executados, 668 aprovados em headless, 21 abortos GUI,
zero falhas/skips; complemento gráfico: 29 aprovados, cobrindo os abortos e
repetindo oito casos. Python: 97; PowerShell: 39 atualizador/21 GitHub simulado.
Smoke dos dois runtimes/JARs empacotados: três processos saíram 0, dois clientes
V6, raças distintas e convidado impedido de hospedar. Sem Radmin físico.

Mudanças: 12 fontes existentes e 19 arquivos novos (8 fontes/11 testes),
registrados no checkpoint TASK-212, além das ferramentas e documentos do ciclo.
GameplayProtocol V6 acrescenta ficha privada e operações validadas; V1–V5
permanecem. LauncherSession.start preserva baseline histórico; startExpanded
é usado no novo fluxo Radmin. Publicações existentes não são sobrescritas.
Inventário, loot, XP, domínio e missões são efêmeros; reconexão cria outra sessão.

Preservação: 2.805 arquivos baseline, nenhum ausente; 2.320 assets/saves/arquivos
da distribuição protegidos sem alteração. As exclusões históricas foram mantidas.
Evidências: `.task-checkpoints/TASK-203` a `TASK-212`, `TASK-212-GUI`,
`TASK-212-INTEGRITY` e `CORUJAO-VII-REMAINING`.

Os registros seguintes são históricos e suas restrições de escopo foram
superadas pela autorização posterior do usuário.

## Atualização TASK-202 — 31/10/2026

O usuário autorizou prosseguir com a TASK-202. Catálogo por raça implementado
nos fontes locais: [entrega](docs/TASK-202.md), [testes](docs/TASK-202-TESTES.md).
Branch `beco`, HEAD `6d72c6595dee6dbe4dea832fe6047ee81f51d4a3`.
Arquivos: novo `RaceSpriteCatalog`, `NetworkPlayerVisuals`,
`RpgNetworkClientLauncher`, `SessionCharacterCreation` e novo teste
`Task202RaceSpritesTest`. Atlas Reaper/Human preservados; Aetheri/Spirit
continuam com aparência Human provisória identificada. A ausência do atlas
Reaper agora usa representação geométrica no mundo, com aviso e ingresso bloqueado.
Não houve novo pacote ou publicação. TASK-203 não iniciada.
Próximo passo: revisão visual local; homologação física Radmin continua pendente.
Checkpoints: `.task-checkpoints/TASK-202/corujao-vii/`.

O registro abaixo preserva o encerramento histórico da TASK-201.

Atualização: 28/10/2026.

- **Ciclo:** Corujão VII; execução limitada à TASK-201.
- **Aceite anterior informado:** Corujão VI/TASK-200, conforme o responsável.
  Não é uma homologação nova feita neste ciclo.
- **Tarefa atual:** TASK-201 implementada e validada localmente, com fontes
  congeladas; próxima ação é a revisão local do responsável.
- **Última validação registrada:** suites, provas TCP/gráficas e launcher
  descritos em [docs/TASK-201-TESTES.md](docs/TASK-201-TESTES.md).
- **Próxima tarefa:** TASK-202 não iniciada; não avançar automaticamente.
- **Branch/HEAD:** `beco` / `6d72c6595dee6dbe4dea832fe6047ee81f51d4a3`.
- **Distribuição oficial:** `dist/SpiritGate-GitHub-0.201.0`, preservada com
  seu código anterior; não recebeu as alterações desta tarefa.

## Aprovação e estado técnico

Após a auditoria, o usuário aprovou a aplicação negociada 5, o frame de
criação anterior à admissão e os fallbacks identificados. Acrescentou a
redução de janelas: launcher fecha após confirmar o processo vivo; conexão
Radmin modal; entrada no jogo descarta a navegação e transfere a posse do
servidor para a partida, inclusive ao reconectar.

O novo ingresso usa `SessionCharacterSelection` e validação do servidor.
Nome não é chave de identidade: jogadores de mesmo nome/raça conservam IDs
distintos. Reconexão conserva a escolha e cria outra sessão. O caminho legado
V1–V4 mantém os contratos históricos, incluindo raça por assento apenas nesse
caminho. Cliente novo sem servidor compatível recebe orientação para atualizar,
sem mudança silenciosa da escolha.

A prévia, o mundo e a reconexão usam os mesmos recursos carregados pelo worker.
Reaper e Human têm atlas existentes; Aetheri/Spirit usam Human provisório com
aviso. Reaper não ganhou acessórios incompatíveis nem nova arte.

## Arquivos da implementação

Em `server/src/main/java/com/projetok/`:

- Novos `multiplayer/SessionCharacterSelection.java` e
  `prototype/rpg/SessionCharacterCreation.java`.
- `multiplayer/Protocol.java`, `SessionSimulation.java`,
  `LocalSimulationServer.java`, `LocalClient.java`, `RpgSessionWorld.java`,
  `RevisionCoordinator.java`, `NetworkPlayerVisuals.java` e
  `RpgNetworkClientLauncher.java`.
- `prototype/rpg/RadminSessionPanel.java`, `SpiritGateEntry.java`,
  `RpgMenu.java` e novos `SessionWindowNavigation.java` e
  `SessionGameLifecycle.java`.

Launcher: `tools/updater/Launcher-SpiritGate.ps1` e
`Atualizar-SpiritGate.ps1`. Testes novos: `SessionCharacterSelectionTest`,
`Task201CharacterSessionTest`, `SessionCharacterCreationTest`,
`Task201SessionWindowFlowTest`, `Task201RadminCancellationTest`,
`SessionGameReconnectCloseTest`, `tools/test_launcher_handoff.py` e
`updater/Test-Launcher-Handoff.ps1`. Os sete documentos de tarefa/continuidade
foram atualizados, preservando o pedido original e o baseline histórico.

O estado Git anterior inclui modificações e exclusões da limpeza autorizada,
além de correções anteriores. Não restaurar exclusões nem limpar a árvore Git.

## Validações e preservação

As rodadas completas e dirigidas estão em
[docs/TASK-201-TESTES.md](docs/TASK-201-TESTES.md). O baseline da auditoria
permanece identificado separadamente; não usar seus números como se fossem
prova da implementação. Na rodada final, Java compilou 243 fontes e 209
arquivos de testes: 671 executados, 651 aprovados em headless, 20 abortados
por exigir GUI e nenhuma falha ou skip. O consolidado Swing aprovou 28
execuções, cobrindo os 20 abortos e repetindo oito casos. Python aprovou 97
testes, incluindo os três de handoff. As regressões PowerShell após a correção
do ACK aprovaram 39 verificações do atualizador e 21 do transporte GitHub.

As quatro correções finais foram verificadas: apresentação compartilhada;
conclusão de conexão durante cancelamento Radmin; reconexão durante decisão
de encerrar a partida, sem janela ou popup tardio; e falha ao gravar ACK após
iniciar Java, encerrando somente o processo recém-criado e aguardando a saída
antes de liberar o lock. As fontes permaneceram estáveis nos testes finais.

A conferência preservou 705 assets e oito arquivos pessoais. No conjunto
de 1.242 arquivos do baseline, 14 existentes tiveram as alterações previstas,
1.228 ficaram intactos e nenhum estava ausente. Há 12 arquivos novos de
código/testes; as 2.432 exclusões anteriores foram preservadas.
Os dois canais oficiais continuam com
391 arquivos por papel, manifesto `252cb97262083ebfc9aea79bdbd4ea4a9de04a24f647d9597634c02522276547`
e fonte ZIP `1a56363a01137165cecdda10b5f9dea6dd750c4c79ddb4c456e9dc336b908b2c`.

Evidências: `.task-checkpoints/TASK-201/corujao-vii/`, em especial
`implementation/`, `implementation/review-cancel/`, `lifecycle-reconnect/`,
`backend-validation/`, `ui-flow/` e `launcher-handoff/`. Para revisar somente
a diferença desta tarefa contra o checkpoint anterior, usar
`implementation/task201-source.patch` e `source-changes.json`: 26 arquivos
de código/testes, preservando as alterações anteriores.
Não houve teste novo em dois computadores nem aceite artístico humano.

## Como testar e continuar

Execute `rodar-prototipo.cmd` e use as opções Radmin no Portal de entrada.
`jogar-rpg-offline.cmd` é um alias que também abre esse fluxo. Esses comandos
executam os fontes atuais; o EXE oficial 0.201.0 continua com sua versão anterior.
Nenhum novo pacote, preview ou publicação foi gerado nesta tarefa.

Submeter a experiência local à revisão do responsável. TASK-202 somente mediante nova
instrução. Contas, migrações, dependências novas, Git push e releases não
estão autorizados como consequência da TASK-201.
