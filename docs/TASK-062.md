# TASK-062 - Menu e personagens

Menu principal: criar, continuar selecionado, arena historica e sair. Criacao escolhe nome/arquetipo/aparencia e grava identidade inicial em arquivo por UUID, sem sobrescrever outro personagem. Listagem e IO em SwingWorker. Saves invalidos ficam visiveis/preservados; backup valido pode ser continuado em novo arquivo de recuperacao. Nenhuma exclusao automatica.

Fechar RPG ou botao Menu salva assincronamente antes de voltar. Falha preserva sessao aberta. Pasta portatil de saves resolvida junto ao EXE; execucao por classes usa raiz atual. Testes de catalogo, corrupcao, backup e preservacao. Diagnostico distingue captura de desktop de captura dos componentes Swing da janela em execucao, com JSON de ticks/visibilidade; nenhuma e homologacao humana.
