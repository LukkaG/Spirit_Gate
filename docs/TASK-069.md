# TASK-069 - Duas sessoes transitorias

LocalClient conecta com timeout, handshake V1 e ID atribuido pelo servidor. Leitor/escritor separados, fila de 32 intencoes e snapshots imutaveis monotônicos. Nenhum cliente escolhe identidade de outro. Desconexao fecha socket e sinaliza observadores; reconexao recebe novo ID, sem conta ou importacao de save.

Teste com dois sockets reais: identidades distintas, observacao de movimento remoto, saida e reconexao. Sequencias antigas e sessoes desconhecidas recusadas no dominio. Corrigida corrida de admissao: conexao encerrada antes do processamento da fila nao pode criar jogador fantasma.
