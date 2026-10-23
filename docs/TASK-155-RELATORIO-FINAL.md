# TASK-155 — Relatório

Reconexão explícita em worker cria nova sessão e fecha a anterior; UI evita tentativas concorrentes e descarta conexão nova se janela foi fechada. Sockets, filas limitadas, timeouts e expiração de entrada existentes preservados. Teste real de reconexão e shutdown confirmou remoção de entidade e rejeição de comando após saída. LAN privada exige opção explícita.

Limitações: Reconexão não restaura personagem online nem importa saves offline. LAN entre dois computadores não homologada. Sem firewall, port forwarding, contas ou exposição pública.

Validação: JUnit: 457 executed, 449 passed, 8 aborted, 0 failures, 0 skipped. Nenhuma aprovação artística automática. Diff e hashes no checkpoint.
