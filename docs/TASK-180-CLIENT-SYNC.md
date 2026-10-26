# Preparação e apresentação nos clientes

LocalClient.revision negocia aplicação 4 explicitamente. O launcher atualizado usa essa fábrica; o CLI usa `--revisions`. As fábricas históricas continuam disponíveis para compatibilidade e regressões.

ClientRevisionSync recebe um descritor por vez, com 4 MiB totais e blocos contíguos de até 2800 bytes. Parsing, hashes, StudioValidator, cenas e imagens são preparados em worker próprio, sem serialização Java, scripts ou escrita em caminhos recebidos. Não existe download de PNGs nesta entrega.

Os assets devem já existir nos dois pacotes. O servidor confere contra o catálogo capturado ao hospedar e cada cliente confere contra seu próprio catálogo. Um ID ou hash ausente recusa a publicação com motivo. Não há placeholder declarado sincronizado. Recursos novos exigem um pacote de executáveis/assets atualizado pelo mecanismo de atualização existente, separadamente da publicação da sessão.

Até 128 recursos, 64 MiB de arquivos usados e 64 MiB de pixels ativos por revisão. Durante staging, ativa e candidata podem coexistir; o orçamento de imagens de conteúdo pode chegar a 128 MiB, além das sprites originais e overhead da JVM. A candidata não é pintada antes de ACTIVATE. O cache antigo permanece até a confirmação/recuperação e depois é liberado por referência; não há coleção ilimitada de revisões em memória.

READY é emitido após preparar documentos e imagens. ACTIVATE chama o instalador do RpgNetworkClientLauncher na EDT. Esse instalador troca a cena, o StudioMapRenderer, estruturas e timeline; só então completa o futuro que permite emitir ACTIVE. Não se usa um ACK TCP como substituto dessa instalação.

Durante a barreira, inputs ficam bloqueados e o HUD informa TRANSICAO SEGURA. Snapshots com hash divergente ou recebidos antes de RESUME não entram na apresentação. Um snapshot atrasado mantém seu próprio hash, incluindo a corrida entre leitura e instalação. O buffer de interpolação é reiniciado na troca, impedindo atravessar paredes antigas como transição visual entre revisões.

O renderer de produção usa as mesmas camadas/crops do StudioMapRenderer, com pixels já residentes. NPCs e inimigos com assets autorados usam o identificador de runtime correspondente. Contornos físicos F2 usam perfil e hitboxes da cena ativa, não as constantes antigas. As sprites/cosméticos continuam independentes do corpo físico. P seleciona um portal próximo e envia seu ID de runtime, que pode diferir do ID editorial.

Novo cliente prepara a revisão atual antes de receber gameplay utilizável. Durante uma publicação, novos joins são recusados para manter o conjunto de participantes estável. Após commit, novos clientes V4 passam pela mesma barreira de ingresso; clientes antigos não entram com geometria histórica incompatível.
