# TASK-180 — World Studio Live Publishing

Extensão negociada de conteúdo no servidor RPG experimental do launcher. O destino real usa RpgSessionWorld e LocalSimulationServer; o PreviewServer permanece separado. A autoria só chega à partida pelo comando explícito **Salvar - aplicar ao jogo**.

Auditoria inicial: branch/HEAD e estado sujo preservados no checkpoint `.task-checkpoints/TASK-180/live-publishing/`. Nenhum reset, commit, push ou release. Baseline Java: 532 testes, 523 aprovados e nove abortos de Swing em modo headless; zero falhas. Python: 33 aprovados. O cliente anterior conhecia a geometria padrão, não um descritor recebido. Esse fato determinou a inclusão do adaptador declarativo no renderer de produção.

Foram inspecionados WorldStudio, PreviewServer, LivePublishing, SessionCollisionProfile, DocumentStore, StudioExport/Validator, RpgWorld/RpgSessionWorld, SessionSimulation, SharedRpgEncounter, LocalSimulationServer/LocalClient, Protocol, CollisionService/CharacterFootprint, StudioMapRenderer, RpgSceneSet, AssetImages, LauncherSession/RadminSessionPanel e os scripts de empacotamento. Foram consultados os cinco guias da TASK-179 e CORUJAO-V-PROTOCOLO.md.

| Parte | Responsabilidade |
|---|---|
| WorldContent | descritor V1 limitado, documentos canônicos e validação de recursos locais |
| Protocol aplicação 4 | preparação, ACKs, snapshots e comandos com hash de revisão |
| RevisionCoordinator | barreira no owner do Game Server, timeout e recuperação |
| ClientRevisionSync | transferência limitada, preparo em worker e instalação pelo renderer |
| RpgSessionWorld | candidatos reais, preservação de jogadores e encontro compartilhado |
| LivePublishing/PublicationTarget | staging, histórico, ponteiro, commit e rollback para destinos sintéticos e reais |
| LauncherSession | posse local da capacidade administrativa do servidor selecionado |

Os bytes das mensagens anteriores e o envelope V1 continuam iguais. Coop e aplicações 1/2/3 continuam nos modos históricos; clientes antigos presentes bloqueiam publicação, sem expulsão para liberar o Save. Saves V1/V2 não foram alterados. Assets originais são somente lidos; nenhum save pessoal é usado.

O host abre o Studio pelo launcher e autoriza aquela instância local. Não há endpoint administrativo, token em log, descoberta automática de servidores ou autorização baseada em IP/nome público. O pacote Convidado não oferece hospedagem ou autorização editorial. Essa proteção restringe a partida hospedada; não pretende impedir que o proprietário de uma máquina execute Java modificado nela.

Limites: 960×640, região inicial forest e conjunto forest/village, até dois jogadores e seis atores adicionais, documentos até 4 MiB, até 128 assets preinstalados e 64 MiB de pixels por revisão. O preview anterior continua aceitando seus limites próprios. Ambientes animados e gatilhos editoriais ainda não são capacidades do cliente publicado; são recusados explicitamente. Não há transferência de imagens nesta versão.

A homologação física Radmin entre computadores continua pendente. Os testes locais utilizam TCP, servidor do launcher e renderer de produção reais. Não se alega atualização instantânea de monitores ou tolerância comprovada a queda de energia. TASK-181 não iniciada.
