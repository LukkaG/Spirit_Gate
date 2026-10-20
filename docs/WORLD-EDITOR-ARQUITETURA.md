# World Studio — arquitetura

Aplicação independente em `com.projetok.studio`, Swing e Java 17, no módulo Maven existente. Não requer banco, servidor, motor novo ou dependência adicional.

O documento imutável separa camadas, elementos, recortes, hitboxes, portais e eventos cadastrados. Coordenadas são pixels de mundo; grade inicial 32 não limita o tamanho do PNG. Catálogo, comandos reversíveis, renderer, validação, persistência e exportação têm responsabilidades próprias. I/O usa workers fora da pintura/EDT. Caches são limitados.

`LocalArenaMap` é um contrato legado de retângulos sintéticos, não um formato universal para imagens. Os mapas existentes e ferramentas Python permanecem disponíveis; o Studio usa um formato próprio V1, com exportação explícita e validada.

Integração prevista: sessões isoladas com RpgRuntime/RpgWorld reais, sem saves pessoais. O mundo legado valida regiões village/forest/ruins e coordenadas 960×640 no DTO de save. O adaptador deverá declarar esses limites e não prometer mapas arbitrários persistentes. Se necessário, configurações opcionais de conteúdo/geometria serão injetadas em construtores preservando o caminho padrão. Não haverá migração de save nem alteração de combate/protocolo.

A publicação pedida nesta rodada é o código na branch main do repositório existente. Nenhum deploy do site ou publicação do pacote do editor foi solicitada.
