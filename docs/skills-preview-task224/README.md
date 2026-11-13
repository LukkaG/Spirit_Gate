# TASK-224 — evidências de seleção, hotbar e área

Capturas de duas janelas TCP locais com a HUD real:

- [Sem alvo ofensivo](runtime/sem-alvo.png), [inimigo selecionado](runtime/alvo-inimigo.png).
- [Alvo fora de alcance](runtime/alvo-fora-de-alcance-previa.png), [possível bloqueio de visão](runtime/alvo-sem-linha-de-visao-previa.png): cenas/atores reais recebidos por TCP, avisos de prévia.
- [Aliado bloqueando skill ofensiva](runtime/alvo-aliado-ofensiva-bloqueada.png), [aliado de suporte e HP](runtime/aliado-suporte-hp.png).
- [Área possivelmente obstruída](runtime/area-obstruida-previa.png), [preparada](runtime/area-preparada.png), [cancelada sem conjurar](runtime/area-cancelada-sem-gasto.png).
- [Envio sem confirmação identificável](runtime/envio-sem-confirmacao-nao-e-rejeicao.png): a ausência de evento não foi apresentada como rejeição confirmada.
- [Evento/recarga recebidos](runtime/evento-servidor-recarga.png), [observador](runtime/evento-observador.png).
- [HUD em 1000 px](runtime/hud-1000.png), [640×480](runtime/hud-640.png): nove comandos preservados.
- [Desconectado](runtime/desconectado.png), [desconexão durante preparação](runtime/desconexao-durante-preparacao.png), [reconexão sem mira ou tecla presa](runtime/reconexao-sem-mira-ou-tecla-presa.png).
- [Alvo em outra região](runtime/alvo-em-outra-regiao.png), [alvo desconectado](runtime/alvo-desconectado.png): seleção anterior é descartada quando o ator deixa o snapshot elegível.
- [Nível insuficiente](runtime/fixture-ui-nivel-14.png), [energia insuficiente](runtime/fixture-ui-sem-energia.png): **estes dois estados são fixtures de apresentação**, fornecidos diretamente ao refresh da UI no teste. Não são alteração da ficha autoritativa nem prova de consumo/recusa real no servidor.

TAB troca alvo apenas com foco no viewport. TAB/SHIFT+TAB navegam pela HUD; ENTER ativa o controle focado. Durante área preparada, ENTER/mesma tecla confirma e ESC cancela. SPACE conserva o básico durante gameplay mesmo com botão destacado. Não existe nova tecla de alternância.

LONGE/LOS são prévias, não autoridade. Mensagens de evento/recarga descrevem dados recebidos, não ACK por intenção. [Relatório](../TASK-224-RELATORIO-FINAL.md), [testes](../TASK-224-TESTES.md).
