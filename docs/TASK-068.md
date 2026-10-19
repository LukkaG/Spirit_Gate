# TASK-068 - Servidor local headless

LocalSimulationServer usa TCP JDK ligado somente a loopback por padrao (porta 54557 ou efemera em testes). Thread proprietaria a 30 Hz, fila limitada 64, duas sessoes, snapshots 10 Hz e caixa de saida de uma posicao por cliente. Leitura/escrita de socket em threads separadas. Jogadores usam PlayerFactory, MovementService e CollisionService existentes; nenhum Swing/PostgreSQL.

Handshake em 2 s, ociosidade em 5 s, 120 mensagens/s por conexao e sessao maxima 30 min. Comandos de sessao encerrada/sequencias antigas rejeitados; movimento expira apos 15 ticks sem renovacao. Teste abre socket real em localhost, envia intencao, observa posicao autoritativa e encerra servidor. Nao abre porta publica nem altera firewall.
