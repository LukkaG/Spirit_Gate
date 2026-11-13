# ORG-02-R1 — Análise das falhas GUI

## O que os erros originais realmente verificam

Evidência preservada da ORG-02: `temp/org02-suite-reports/` e `temp/org02-suite.txt`.
Os três stack traces terminam na comparação `focusOwner == find(frame, name)`
em `GamePanelsIntegrationTest.focus`, linha 102. O argumento `name` aparece
como mensagem de falha. Portanto `hudDomainSkill3/1 ==> expected true, was false`
significa **foco diferente do componente solicitado após o prazo**.
Não significa, por si só, `button.isEnabled() == false`.

No caso A, antes de chamar `focus`, o teste aguarda explicitamente a habilitação
do slot 3 e a seleção de domínio. No caso B, o teste encontra o tooltip de
distância do slot 1 antes de pedir foco. A linha posterior verifica que a
estimativa de alcance não desabilita o comando, mas não chegou a ser executada
na falha original. São pontos diferentes; não presumimos um defeito comum de skill.

## Auditoria dos componentes

| Componente | Comportamento confirmado no código | Consequência para a investigação |
| --- | --- | --- |
| GameInputRouter | Dispatcher limitado à janela proprietária; TAB no viewport seleciona, TAB na HUD retorna ao Swing; campos editáveis não executam gameplay; contexto limpa teclas/quarentena | Não atribui foco a outro controle nem altera `enabled` das skills |
| NetworkSkillControls | Refresh recebe ficha/energia/vida; valida nível, custo, alvo, recarga e contexto; preparação local separada de envio | Slot 3 de área não exige alvo; slot 1 ofensivo exige mob vivo; nenhum deles é habilitado pelo teste |
| NetworkHotbar | Obtém vida e energia do cliente e delega refresh às domain skills | Disponibilidade é estado da sessão, não uma configuração global entre testes |
| NetworkGameHud | Compõe mundo, dock, minimapa, feedback e painéis; layout local | Não introduz janela nativa independente para cada painel |
| GamePanels | Mantém painel ativo/modal e foco anterior; fecha removendo componentes/callbacks da raiz | Mudança de contexto libera teclas e cancela mira; não modifica regras do servidor |
| NetworkSkillPreview | Estimativas de alvo/alcance/visão; alvo válido distante/obstruído continua válido | Aviso estimado não é recusa autoritativa; PROBE estático é usado para consulta, sem mutação nessa rota |
| RpgNetworkClientLauncher | Timer Swing de 16ms atualiza controles na EDT; callback de snapshot agenda refresh; worker carrega assets e publica na EDT | Não foi encontrada escrita de controles fora da EDT nessas rotas auditadas |

## Ciclo de vida e estado compartilhado

`GameInputRouter.close()` remove dispatcher, listener de `focusOwner` e listener
da janela, restaura bindings e é idempotente. O `detach` do launcher para timer,
fecha painéis e roteador, remove callback de snapshot e fecha cliente. O worker
verifica attachment/displayability antes de publicar assets. Fixtures dispõem
as janelas na EDT e encerram clientes/servidores. Não há evidência observada
de um timer de sessão encerrada alterando a sessão seguinte.

Os dois testes usam `@TempDir` e restauram `spiritgate.rpg.dataDir` em finally.
`SwingKeys.modifiers` é estático e limpo nos encerramentos relevantes. O estado
nativo de AWT/foco permanece compartilhado pelo processo e desktop. Eventos
`windowClosed` são assíncronos: outros helpers já usam drenagem de EDT antes
de contar listeners. `GameInputRoutingTest` verifica a remoção dos listeners.
Não há configuração de execução paralela no POM ou `junit-platform.properties`
presente no checkout; as ordens efetivas estão nos logs de cada rodada.

## Helpers de foco e teclado

`GamePanelsIntegrationTest.focus` chama `frame.toFront()` uma vez, repete
`requestFocusInWindow()` dentro de um prazo de 3s e exige o destinatário exato.
`SwingKeys` publica KeyEvents na fila AWT para o componente atualmente focado,
inclui KEY_TYPED quando pertinente e nunca chama o ActionMap diretamente.
Se o destinatário ficar nulo por 2s, falha explicitamente. As pausas de polling
e drenagem já existiam; esta etapa não acrescenta sleeps para tornar casos verdes.

## Instrumentação adicionada

`GuiInputTrace`, extensão JUnit aplicada somente às duas classes investigadas,
observa até 256 eventos AWT recentes por invocação, na EDT, com tempo relativo,
thread, origem, foco/janela, sessão, seleção, nível/domínio, energia, cooldowns,
estado do roteador/modal e preparação. Não consome nem cria eventos. Remove
seu listener em `afterEach`, inclusive após falhas. Em falha imprime a sequência;
`-Dspiritgate.test.guiTrace=true` também imprime sucessos.

O registro inicial ocorre antes do fixture; o registro final após o método inclui
o finally que já dispôs as janelas. Assim, `focus=null` no fim de um caso aprovado
**não prova perda durante o teste**. Para isso devem ser lidos os eventos temporais,
que guardam o contexto no instante da ocorrência. O registro é observacional,
mas seu custo pode influenciar timing; ausência de falha instrumentada não prova cura.

O observador AWT recebe `WINDOW_CLOSED` antes dos listeners da própria janela.
Por isso `routerClosed=false` no registro desse evento, sozinho, não comprova
vazamento: o detach pode ser executado no processamento subsequente do mesmo evento.

## Reprodução e limites

As classes isoladas passaram. Na primeira execução conjunta ocorreu uma falha
diferente, em `SwingKeys.post`, ao confirmar área no segundo parâmetro do caso A:
nenhum destinatário focado durante o prazo. Esse stack trace não é o das três
falhas originais. Ordem invertida e execução após HUD/hotkeys passaram.

Não se pode deduzir, desses fatos, quem retirou o foco, se a ativação nativa foi
recusada, se houve interferência externa ou se há uma corrida específica no
fixture. Tampouco há prova de erro em energia, cooldown ou disponibilidade.
Os resultados finais e a menor investigação seguinte constam no
[relatório final](ORG-02-R1-RELATORIO-FINAL.md).

Na repetição instrumentada 07, também houve ausência de destinatário, agora
na linha 44 (press da seta direita, após preparar área). O trace anterior à falha
registrou, na EDT, `FOCUS_LOST` aos 2850ms, `WINDOW_LOST_FOCUS` aos 2860ms e
`WINDOW_DEACTIVATED` aos 2861ms, todos com `opposite=null`. O servidor continuava
conectado, mundo desbloqueado, sem modal, roteador aberto, nível 20 e bow_long;
HP 322/328, stamina 805/1000, recargas `[0,0,0,139,0]` e mira cancelada.
A recarga 139 pertence ao slot 4 lançado antes; o slot 3 tinha recarga zero.
O callback real de perda de foco cancela a preparação; não foi removido para
fazer o teste passar. A repetição 08 passou nos quatro casos.

Essa sequência demonstra desativação nativa na ocorrência 07. Não identifica
o agente da desativação, nem prova que as três falhas históricas tenham a mesma
origem. O monitor de primeiro plano do Windows foi iniciado depois, durante 09;
seus dados são observações posteriores e não evidência retroativa da causa.
