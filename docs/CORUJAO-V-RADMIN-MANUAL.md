# Manual privado — Radmin VPN

Status: teste remoto entre dois computadores PENDENTE. Localhost não comprova VPN. A ajuda oficial menciona a faixa 26.0.0.0/8: https://www.radmin-vpn.com/help/ . Essa faixa não é classificada como site-local por Java; ela só é autorizada no modo Radmin explícito.

1. Extraia o pacote Anfitriao no computador A e Convidado no B. Ambos entram na mesma rede privada Radmin já configurada pelos responsáveis. No Radmin, copie o IPv4 26.x.x.x mostrado para o computador A; confirme no adaptador Radmin usando `ipconfig`.
2. A abre `Servidor-Radmin.cmd`, informa porta (padrão 54557) e seu IP Radmin. O programa verifica que o IP pertence a um adaptador local ativo cujo nome/descrição contém Radmin. Bind em 0.0.0.0, Ethernet, IP não local ou adaptador desligado é recusado.
3. A e B abrem `Cliente-Radmin.cmd`, informam IP Radmin de A, a mesma porta e nomes sintéticos. O cliente exige um adaptador Radmin ativo, usa seu endereço como origem e aceita destino 26.x.x.x. Se houver múltiplos IPv4 Radmin, a seleção automática é recusada.
4. Em B, `Test-NetConnection 26.x.x.x -Port 54557` verifica TCP. Ping pode ser bloqueado sem impedir TCP. Em A, `Get-NetTCPConnection -State Listen -LocalPort 54557` permite conferir o bind. Substitua o IP de exemplo pelo real. Não é necessário encaminhar portas do roteador para esta configuração privada.
5. WASD movimenta; SPACE ataca. Ambos devem enxergar o mesmo HP. P, perto de (140,300), leva é vila; F interage com Anciã, E aceita a missão; aguarde cooldown de portal e P retorna. Depois de participar da derrota, volte ao NPC e C conclui. O outro jogador pode permanecer na floresta. SHIFT/Q só funcionam em perfil sintético nível 20 habilitado no servidor. R recupera jogador morto se o spawn estiver livre.
6. Feche as duas janelas de cliente. Encerre o servidor com Ctrl+C na janela que o iniciou. Verifique o fim do listener. Reconectar cria sessão nova e descarta progresso anterior desse jogador.

Para localhost, use `Servidor-local.cmd` e `Iniciar-dois-clientes.cmd` no Anfitriao. Porta padrão 54557. Para portas diferentes, use os clientes interativos individualmente.

Diagnóstico: conexão recusada costuma indicar listener ausente ou porta errada; timeout indica ausência de resposta/rota/regra local; versão incompatível indica executáveis de contratos diferentes; bind recusado indica IP não atribuído ao adaptador ou porta ocupada. Confira o texto original da exceção e os dados de IP/porta antes de repetir. O projeto não altera firewall/roteador. Caso uma regra do sistema impeça a conexão, a mudança deve ser decidida manualmente pelo responsável, restrita à interface/rede privada; não desative as proteções como procedimento de teste.

Homologação: registrar data, máquinas, versões/hash dos dois ZIPs, IPs privados utilizados, RTT, handshake, movimento/colisão, ataque/HP, morte única, quest, troca individual de região, desconexão/reconexão e encerramento. Capturar telas e logs em ambas as máquinas. Até esse registro existir, o remoto permanece pendente.
