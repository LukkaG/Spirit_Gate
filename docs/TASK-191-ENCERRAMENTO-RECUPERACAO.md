# TASK-191 — encerramento e retomada

Durante publicação, aguarde confirmação ou cancelamento/recuperação antes de encerrar a hospedagem. Fechar o Studio solicita abort da transação em andamento; isso não transforma a solicitação em confirmação imediata. Confira a fase, o resultado e o ponteiro verificado antes de assumir qual revisão está confirmada. Não altere `active.txt` manualmente numa partida aberta.

Feche Studio/janelas de preview que ele possui. Seus timers, clientes de Preview e servidor Preview são encerrados; o host de Game Server autorizado continua independente. A prova Swing da TASK-191 verifica a etapa de dispose posterior à decisão de descarte, não automatiza nem remove o diálogo de rascunho. A pasta temporária vazia do Preview é removida; o fechamento não faz limpeza recursiva de dados desconhecidos.

Feche clientes e, por último, a hospedagem. O coordenador agora conclui excepcionalmente tanto operações administrativas ainda na fila limitada a 16 quanto futuros de transação ativa; não executa operações descartadas para tentar drená-las. Novos pedidos após shutdown são recusados. Enqueue, execução do owner e shutdown são serializados nessa fila. Um commit que já havia sido confirmado permanece confirmado.

No reinício, somente a revisão confirmada, com hash/documentos/assets validados, é carregada. `pending.properties` impede hospedar até recuperação explícita. Com a hospedagem desligada, use a recuperação prevista em [TASK-180-ROLLBACK.md](TASK-180-ROLLBACK.md), preserve o journal renomeado `recovered-*` e confira o hash ao retornar. A candidata não confirmada não deve ser tomada como ativa.

A regressão interrompeu um processo-filho real com `Runtime.halt(23)` depois de ativar a candidata e antes de escrever ponteiro/confirmar. O processo pai verificou bloqueio por journal, recuperação explícita e retorno da revisão anteriormente confirmada. Isso demonstra aquele corte de processo em filesystem local; não é teste de falta física de energia, kernel crash, armazenamento defeituoso ou consenso distribuído.

Conexões recusadas, que nunca participaram da sessão, não cancelam a reserva do host nem do Preview ao fechar. A saída de um participante real conserva o fluxo de recuperação anterior. Não há endpoint administrativo novo ou direito de publicação atribuído ao socket.
