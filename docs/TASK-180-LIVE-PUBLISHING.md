# Publicação pelo anfitrião

1. Atualize Anfitrião e Convidado para o mesmo pacote privado TASK-180.
2. Hospede pelo launcher Radmin normalmente, selecionando o adaptador explicitamente. Para homologação local, o script Servidor-local abre o host gerenciado em loopback.
3. Conecte os dois clientes atualizados e aguarde conteúdo sincronizado.
4. No host, clique **World Studio - autorizar publicacao local** e confirme a autorização daquele servidor. No host local alternativo, use **Autorizar World Studio neste servidor**.
5. O Studio carrega os mapas ativos e mantém um destino local possuído pelo launcher. Abrir o editor por `world-studio.cmd` continua sem autorização automática sobre partidas.
6. Inicie Preview, edite e teste. Alterações e **Salvar rascunho** não alteram a partida.
7. Clique **Salvar - aplicar ao jogo**. A revisão é congelada e revalidada; o painel inferior acompanha fase/hash/clientes. O status da última ação permanece separado dos dados periódicos do servidor.
8. Aguarde **Publicacao confirmada**. Uma revisão candidata/READY ainda não significa commit.

O perfil global está nos metadados do mapa-raiz forest; abra esse mapa para ajustar o Collision Lab e publicar o conjunto. O contrato atual publica forest/village, não regiões arbitrárias ou maiores que 960×640. Dados não suportados são recusados, sem conversão silenciosa. As regras históricas de combate, níveis 1/20 de teste, inventário e progressão são preservadas pelos forks; publicar não concede XP nem escreve personagens.

RevisionCoordinator congela mutações incompatíveis no owner e prepara mundos/encontro candidatos. Valida posição atual dos jogadores, inimigos preservados, spawn, portal e NPCs. Não empurra uma entidade para caber. Um obstáculo sobre uma entidade ou um crescimento de corpo que provoque sobreposição recusa o conjunto.

O servidor envia a candidata e espera READY de todos. Só depois LivePublishing grava staging/histórico. ACTIVATE instala os mundos no tick e aguarda ACTIVE dos renderizadores. Ainda com a simulação pausada, confirma os arquivos/ponteiro e por último autoriza o commit/RESUME. Nenhum sucesso é apresentado antes da confirmação real.

O período de transição é uma barreira segura, não atomicidade física entre monitores. Há pausa mensurável. Comandos antigos de outra revisão não valem depois da troca. Timeout padrão 25 s; a opção de desenvolvimento spiritgate.content.timeoutMillis aceita 500..25000 ms. Recuperação tem janela de oito segundos antes de isolar participante que não restaure a cena.

**Cancelar publicacao antes do commit**, no menu Servidor, solicita recuperação da revisão anterior. Fechar Studio durante publicação também solicita abort; fechar o editor depois do commit não reverte a partida. O Studio não encerra o Game Server que o launcher possui. A tentativa posterior a um commit já finalizado informa que não há transação a cancelar.

Não existe canal administrativo no IP Radmin, porta local adicional ou segredo transmitido. A ponte é a referência Java possuída pelo host na mesma JVM. Servidores externos/outros processos não são descobertos ou controlados automaticamente. O Convidado recebe apenas gameplay e o contrato de conteúdo.
