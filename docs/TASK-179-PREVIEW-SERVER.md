# Preview Server — isolamento

PreviewServer encapsula um LocalSimulationServer próprio e um SessionSimulation de desenvolvimento. A porta é solicitada ao SO em 127.0.0.1:0, não 54557 nem um adaptador Radmin. Cada instância possui seu backend, personagens, encontro, fila limitada de 16 operações editoriais, diretório temporário e executor de mundo. Não há endpoint administrativo em TCP.

Sockets de jogador negociam o Protocol existente e enviam movimento validado. Os comandos editoriais usam uma referência Java local possuída pelo Studio, UUID de sessão, revisão base, nova revisão, tipo, referência e resposta de validação. Não há intérprete de scripts, shell ou comandos de sistema. Um cliente comum não possui essa referência e mensagens administrativas não existem no protocolo público.

DraftPatch transmite diferenças de elementos, hitboxes e portais, com remoções e ordem explícitas. Cabeçalho, camadas e metadados são limitados. O receptor recompõe o documento a partir do hash-base, valida com StudioExport/StudioValidator e prepara conteúdo imutável no worker. A instalação é enfileirada e confirma no tick. Bases antigas são rejeitadas; FULL_SYNC é uma recuperação separada. Mudanças de dependências/regiões usam snapshot completo.

O motor não é duplicado: RpgWorld prepara um fork sintético com novos dados de cena e estado do personagem preservado. SharedRpgEncounter suporta as regiões/definições injetadas e preserva inimigos de definição idêntica. Todas as posições dos participantes são verificadas antes de instalar o conjunto. Nenhuma instância antiga é mutada para preparar a candidata. A troca também vale para perfil, objetos, NPCs e portais.

Assets são lidos por hash em workers. Cada janela tem seu AssetImages de até 64 MiB, sprites e animações próprias. Thumbnails mutáveis do editor não entram no conteúdo congelado. Os documentos, perfis e referências compartilhadas são imutáveis; caches de imagem e entidades não são compartilhados entre preview e destino. RpgContent/catálogos estáticos são apenas consultados; os probes estáticos de geometria existentes não recebem mutações.

Estados: desligado, iniciando, pronto, sincronizando, simulando, erro e encerrando. Pausa continua processando a fila administrativa em fronteiras de tick. Reiniciar cria uma sessão isolada; fechar o Studio encerra sockets, timers e servidores que ele possui. Servidores externos não são procurados nem encerrados.

Limite do isolamento em mesma JVM: falhas normais de sessão/socket/validação são isoladas; uma falha fatal da JVM ou do processo pode encerrar todas as suas janelas. Não se alega isolamento de processo ou orçamento separado de CPU. Os testes demonstram portas, diretórios, estados e recursos distintos, preservando uma sessão principal independente durante encerramento do preview.
