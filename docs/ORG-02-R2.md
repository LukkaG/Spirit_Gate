# ORG-02-R2 — Investigação do foco nativo AWT/Swing

## Escopo e baseline

09/11/2026, Windows 10, Temurin 17.0.20.1, Maven 3.10.0.
Branch `beco`, HEAD `13b91dc`. Checkout com alterações anteriores preservadas;
estado inicial em `temp/org02-r2/git-status-before.txt`. Referência: ORG-02-R1
e seus relatórios, que registram duas suítes de 1068 casos e desativação com
`opposite=null`, sem identificação da causa.

Investigar dois parâmetros de `SkillInputRefinementGuiTest` e dois métodos de
`Task224CastingGuiTest`. Não modificar produção antes de defeito reproduzível.
Preservar skills, combate, atalhos, protocolo, dados, World Studio, launcher,
TASK-225 e A-04. Não iniciar ORG-03.

## Condições e limite registrados antes da execução

O usuário confirmou: **“Desktop reservado; pode executar”**. O coletor nativo
controlado iniciou antes do primeiro processo gráfico. Um Maven/Surefire por
vez, sem outra automação de interface iniciada pelo agente.

Matriz: quatro casos isolados, duas classes juntas, ordem inversa, após HUD,
após hotkeys e até seis repetições conjuntas. Teto de 30 minutos de execução
gráfica; parar antes com causa suficientemente demonstrada. Se houver patch,
validar os quatro casos, regressões gráficas relacionadas e suíte geral.
O limite de seis repetições foi atingido; não houve repetição ilimitada.

## Instrumentação e preservação

`GuiInputTrace` mantém buffer de 256 registros e adiciona UTC, PID, thread,
propriedades de foco antigas/novas, retorno das chamadas de foco existentes,
identidade/estado do componente, JFrame e viewport e número de listeners.
O observador é removido em `afterEach`. O filtro opcional de invocação JUnit
permite isolar os parâmetros booleanos; sem propriedade, ambos executam.
Não altera ações, asserções, retries, sleeps, TAB/SHIFT+TAB, SPACE ou F5–F9.

Windows: `SetWinEventHook(EVENT_SYSTEM_FOREGROUND)`, fora do Java, registra UTC,
tick do evento e callback, HWND, janela atualmente em primeiro plano, PID,
processo e thread nativa. Não ativa janelas. Timer de 250ms verifica somente
o arquivo de parada e o limite do observador, sem polling de foco.

Fontes/recursos inventariados antes dos testes; capturas anteriores copiadas,
novas saídas arquivadas antes da restauração. Sem commit/push/release/instalação.

## Resultado e critério de aceite

A matriz passou sem reproduzir a falha. A investigação foi encerrada no limite
previsto, com causa indeterminada e intermitência nativa ainda não resolvida.
Não há evidência para atribuir sua causa ao ambiente, ao fixture ou à produção.
Por isso não se declara causa demonstrada, estabilização comprovada ou
homologação definitiva. O ramo do aceite que exige classificação ambiental
causal não está demonstrado; o limite experimental foi cumprido.

Detalhes: [testes](ORG-02-R2-TESTES.md), [análise causal](ORG-02-R2-CAUSA-RAIZ-GUI.md)
e [relatório final](ORG-02-R2-RELATORIO-FINAL.md).
