# TASK-074 - Robustez e limites

Testes deterministas simulam perda, atraso, duplicacao e reordenacao de intencoes por 2500 ticks, incluindo desconexao/reconexao durante habilidades. Comparacao integral com seed fixa. Entrada ausente expira; sessoes fechadas nao movimentam/atacam. Payload <=4096, fila mundial64, cliente32, saida1, conexoes2, 120 mensagens/s e 30 min por sessao.

Corrigido fechamento de conexoes ainda no handshake: sockets pendentes agora sao rastreados, fechados e leitores interrompidos. Servidor nao pode ser reiniciado apos close. IDs de jogadores limitados/reservados para nao colidir com chefe. Teste real injeta frame excessivo e encerra servidor com handshake incompleto sem falha fatal.

Escopo parcial explicitado: experimento online tem um mapa/encontro; transicoes de regioes em rede nao foram implementadas, portanto desconexao durante transicao ONLINE nao foi homologada. Transicoes e save offline continuam cobertos pela campanha. Isso nao e multiplayer completo nem auditoria de seguranca de producao.
