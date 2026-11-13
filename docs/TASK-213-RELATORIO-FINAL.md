# TASK-213 — fechamento local

Adendo visual posterior implementado: [HUD refinada](TASK-213-ADENDO-HUD-1366.png),
com três blocos, reservas laterais e centro sem logo. Cinco execuções dirigidas
aprovadas; [requisitos e resultados](CORUJAO-VIII-ADENDO-HUD.md). O texto abaixo
preserva o fechamento da primeira entrega.

Nova HUD principal do multiplayer V6 implementada nos fontes: mundo livre,
minimapa superior direito, HP/energia e marca centrais, quatro slots e botões
rápidos, com acabamento navy/ciano inspirado na entrada aprovada.

Java completo sem falhas: 692 executados, 670 aprovados em headless e 22 abortos
GUI cobertos pelo complemento de 30 execuções gráficas aprovadas. Prova TCP
com dois clientes, três resoluções, ataque, movimento e abertura de painéis.
Assets, saves, distribuições e protocolo preservados.

[Escopo e arquivos](TASK-213.md) · [Testes](TASK-213-TESTES.md) ·
[HUD 1366×768](TASK-213-HUD-1366.png) · [HUD 640×480](TASK-213-HUD-640.png).

Disponível por `rodar-prototipo.cmd` e opções Radmin. Nenhum pacote/EXE oficial
foi atualizado, nem houve publicação. Revisão visual humana permanece pendente.
O ciclo VIII continua aberto; TASK-214 não iniciada nesta execução.


## Adendo obrigatório de janelas e teclado - 31/10/2026

Implementado no V6: cinco painéis internos translúcidos, roteamento contextual de teclado e configurações com Aplicar/Cancelar. Base das TASKs 214-217 adiantada por determinação do novo adendo; seus escopos completos continuam pendentes. TASK-216 tem implementação parcial de apresentação e controles. Regressão: 695 testes Java (670 aprovados, 25 abortos gráficos), complementada por 33 execuções GUI aprovadas, sem falhas. Teclado físico e revisão visual humana permanecem pendentes. Ver [relatório do adendo](ADENDO-JANELAS-HOTKEYS-RELATORIO.md) e [PDF](Spirit_Gate_Adendo_Janelas_Hotkeys.pdf).
