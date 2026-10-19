# Coop experimental - teste privado

Este pacote e separado do RPG offline e nao le seus personagens/saves. Java integrado. Nao representa contas online nem MMORPG completo.

1. Extraia o ZIP inteiro. Execute SpiritGateCoopServer.exe. O console informa 127.0.0.1:54557. Ele fica aberto ate Ctrl+C; nao instala servico.
2. Abra iniciar-cliente.cmd duas vezes. Use host 127.0.0.1, porta 54557 e nomes distintos. Dois processos exibem jogadores na mesma arena.
3. WASD move; cada cliente deve observar o outro. Nenhum cliente escolhe posicao absoluta ou HP. Movimento para quando foco/renovacao de entrada desaparece.
4. SPACE golpeia perto do chefe; Shift envia Passo Relampago, Q envia Criostase, mouse determina alvo. Vigor e cooldown sao calculados pelo servidor. Manter tecla nao repete golpe automaticamente.
5. Aproxime-se do chefe por lados diferentes para respeitar corpos solidos. Ambos devem causar dano antes da derrota. Participante ainda conectado recebe 20 moedas temporarias, uma vez. Repetir comandos nao duplica recompensa. Quem nao causou dano nao recebe; reconexao nao recupera credito anterior.
6. Feche um cliente, confirme saida no outro e abra novamente. Novo ID transitorio; nenhum progresso offline e alterado. Fechar servidor deve desconectar clientes sem travar suas janelas.
7. LAN opcional, somente se configurada por quem testa: servidor com --bind IP-PRIVADO --allow-lan, cliente com checkbox LAN. Nao usar endereco publico, wildcard ou port forwarding. Nenhuma regra global de firewall e alterada pelo jogo.

Validado automaticamente: dois sockets e dois processos locais; codec, limites, sequencias, movimento, habilidades, derrota/recompensa e desconexao. Teste entre dois computadores nao foi realizado. Um mapa online, sem portais ou saves compartilhados. Sem autenticacao/TLS de producao. O pacote offline continua sendo a entrega principal.
