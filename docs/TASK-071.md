# TASK-071 - LAN configuravel, validacao local parcial

Servidor permite endereco privado somente com --bind IP-PRIVADO --allow-lan. Sem isso, loopback. Endereco publico, wildcard e multicast recusados. Cliente tem checkbox LAN explicito ou argumento --lan. Nenhum port forwarding, firewall, servico ou bind LAN foi executado nesta rodada.

Teste TwoProcessTest abre dois PROCESSOS Java de cliente e um servidor local real, confirma identidades distintas, movimento e snapshots. Evidencia TWO_PROCESS_LOCAL no log Java. Isso valida dois processos locais; nao equivale a teste entre computadores. Status de homologacao LAN entre duas maquinas: PARCIAL/NAO REALIZADA por ausencia de segunda maquina configurada. Funcionalidade LAN privada disponivel para teste manual posterior.
