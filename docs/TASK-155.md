# TASK-155

Reconexão explícita em worker cria nova sessão e fecha a anterior; UI evita tentativas concorrentes e descarta conexão nova se janela foi fechada. Sockets, filas limitadas, timeouts e expiração de entrada existentes preservados. Teste real de reconexão e shutdown confirmou remoção de entidade e rejeição de comando após saída. LAN privada exige opção explícita.

Baseline anterior: checkpoint before e regressão da tarefa precedente. Checkpoints incrementais baseados no ZIP anterior da TASK-134, com manifesto SHA-256 e instruções de restauração em pasta nova.
