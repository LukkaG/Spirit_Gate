# TASK-176 - Manual do launcher

Extraia o ZIP inteiro em diretorio novo. Abra SpiritGate.exe; nao e necessario executar scripts de servidor e cliente. Java integrado. Anfitriao oferece Offline, Hospedar e Entrar. Convidado oferece Entrar; hospedagem e offline ficam desabilitados.

Instale Radmin VPN pelo site oficial https://www.radmin-vpn.com/ quando necessario. Crie ou entre na mesma rede pelo aplicativo. O launcher pode abrir a instalacao encontrada nos caminhos padrao; nao instala, nao configura redes, nao guarda senhas e nao altera firewall.

Hospedar: selecione explicitamente seu IPv4 Radmin, configure porta (padrao 54557), clique Iniciar servidor. Aguarde Servidor pronto. Copie o IP e compartilhe IP/porta. Entre na propria partida com nome publico sintetico.

Entrar: selecione seu adaptador Radmin, informe IP do anfitriao, porta e nome publico. Conectar valida endereco e adaptador e aguarda handshake V3 e snapshot contendo o jogador. TCP aberto nao comprova sessao admitida. Adaptador presente nao comprova mesma rede VPN.

Diagnosticar atualiza adaptadores: ausente, inativo, ativo sem IPv4 e multiplos enderecos. A tentativa de conexao verifica endpoint e handshake. Erros de porta ocupada, versao, indisponibilidade e timeout aparecem no painel. Somente IPs associados a adaptador Radmin ativo sao aceitos.

WASD mover; SPACE atacar; SHIFT/Q Shunpo conforme nivel; P portal; F NPC; E/C missao; R respawn. Aparencia, RTT e reconexao usam o cliente existente. Reconectar cria nova sessao sintetica e preserva a interface selecionada. Fechar cliente desconecta. Encerrar servidor/Voltar pede confirmacao quando ha servidor e desconecta participantes. Operacoes em andamento descartam e encerram recursos quando terminam.

O Convidado inclui o JAR comum: tipos de servidor sao dependencias de ligacao do painel compartilhado. Nao inclui scripts de hospedagem e a interface desabilita hospedar. Isso nao constitui isolamento de seguranca; o backend permanece no JAR comum. Nao ha contas ou autenticacao de producao.

Homologacao fisica: em dois computadores, extraia os pacotes novos, ingresse na mesma rede Radmin, hospede e conecte ambos pela interface. Registre capturas de IP/bind, dois jogadores, combate/HP compartilhado, movimento, aparencia, portal, RTT, desconexao/reconexao e liberacao de porta. Verifique porta ocupada sem afetar processo externo. Aprovacao visual pertence ao responsavel.

Radmin remoto: PENDENTE DE HOMOLOGAÇÃO FÍSICA. Localhost nao comprova funcionamento remoto.
