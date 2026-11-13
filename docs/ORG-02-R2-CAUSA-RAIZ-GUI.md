# ORG-02-R2 — Causa raiz GUI

## Conclusão

**Causa indeterminada; perda anormal não reproduzida no limite controlado.**
Não foi demonstrado defeito em fixture/helper ou produção; nenhum patch
corretivo foi aplicado. O diagnóstico reforça a necessidade de correlação
nativa, mas não transforma ausência de reprodução em correção ou prova de
interferência externa. A intermitência continua não resolvida.

## O que foi demonstrado

Na matriz controlada, os quatro casos passaram isolados, juntos, na ordem
inversa, após HUD/hotkeys e em seis repetições. O contador e os comandos estão
em [ORG-02-R2-TESTES.md](ORG-02-R2-TESTES.md). A suíte geral também está registrada
ali. Não se modificaram regras de disponibilidade ou entrada para obter verde.

O trace registra propriedade antiga/nova, identidade de componentes, foco,
janela ativa/focada, retorno do pedido de foco e estado do viewport. Os eventos
observados foram processados na EDT (`AWT-EventQueue-0`). A coleta nativa existe
desde antes da primeira janela e identifica o processo proprietário do HWND.

Na matriz anterior à suíte, foram correlacionados 110 eventos com `opposite=null`:
88 `FOCUS_LOST`, 11 `WINDOW_LOST_FOCUS` e 11 `WINDOW_DEACTIVATED`.
Todos os 110 tinham o frame já com `showing=false` e `displayable=false`.
Isso é compatível com descarte dos fixtures, não com a desativação anormal
durante a ação registrada na R1. A contagem final, incluindo suíte, está em
`temp/org02-r2/correlated-losses.json` (120 registros).

Exemplo verificável: rodada 04, `2026-11-10T01:10:50.799277900Z`,
`FOCUS_LOST` com `cause=CLEAR_GLOBAL_FOCUS_OWNER`, frame não visível; `WINDOW_CLOSED`
às `01:10:50.804275700Z`. O log nativo mostra o retorno ao editor no encerramento.
Os timestamps nativos do evento e do callback podem diferir; há callbacks
em que o HWND do evento já difere do `GetForegroundWindow` observado.

Pedidos com retorno `false` também ocorreram em testes aprovados. Exemplo:
rodada 04, `01:10:45.597669800Z`, pedido para o viewport antes de completar a
ativação; o fluxo prosseguiu após o foco real. Retorno `false` isolado não
é prova de falha nem justifica mais tentativas. As asserções continuam exigindo
o destinatário real e os efeitos de gameplay existentes.

## Fixtures, listeners e interferência

Os dois frames de cada fixture compartilham desktop e EDT. A matriz alterna
classes/ordens e inclui HUD/hotkeys na mesma JVM, sem falha reproduzida.
`GameInputRouter.close()` remove dispatcher/listener de foco/listener de janela;
`windowClosed` aciona encerramento. Fixtures dispõem frames na EDT.
Task224 drena a fila ao fechar; SkillInput dispõe seus frames em finally.
Essa diferença é ponto de investigação, não defeito demonstrado.
`GameInputRoutingTest`, executado na rodada 09 e na suíte, contém verificação
de encerramento do router e restauração da quantidade de listeners de foco.

O estado `routerClosed=false` no observador de `WINDOW_CLOSED` pode preceder
o listener que executa o detach; não prova vazamento posterior. O trace remove
seus próprios observadores ao final. Não foi acrescentada ativação periódica,
fallback de destinatário ou supressão de asserções.

## Limites e classificação

As evidências anteriores da R1 continuam válidas: desativação comprovada, autor
desconhecido. A observação preliminar desta revisão, sem testes, inclui outros
processos; ela não é evidência de interferência nas falhas históricas.
Durante a matriz, as transições nativas observadas envolveram Java e o editor.
Isso não demonstra quem teria iniciado a perda histórica.

O buffer tem 256 registros por invocação; sequências longas podem perder o
início. A instrumentação tem custo e pode mudar timing. O hook observa eventos
de primeiro plano, não a pilha do processo que solicitou a ativação.
Uma execução verde, mesmo repetida, não prova estabilização.

**Limite experimental cumprido; classificação causal permanece aberta.**
Não há evidência suficiente para atribuir a origem ao ambiente Windows,
ao usuário, a outro processo, a temporização, a gameplay ou a fixtures.
Como registro operacional, trata-se de intermitência de foco nativo ainda não
resolvida. O aceite de causa demonstrada/classificação ambiental comprovada
não pode ser afirmado. Sem expansão automática de testes ou início da ORG-03.
