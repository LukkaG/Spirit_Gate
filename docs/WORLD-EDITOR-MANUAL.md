# Spirit Gate World Studio — manual

Abra `world-studio.cmd` no checkout compilado, ou extraia o pacote inteiro e execute SpiritGateWorldStudio.exe. Não exige PostgreSQL, servidor ou Java instalado quando empacotado.

Use Arquivo → Novo mapa e escolha nome, ID, tamanho, grade e região de conteúdo. A biblioteca indexa somente assets e recursos autorizados; busca por nome/categoria e miniaturas não modificam os PNGs. Defina categorias manualmente. Escolha uma imagem, use Recortar tileset para cadastrar largura/altura, margem, espaço e região e clique no recorte desejado.

No canvas, roda do mouse altera zoom ancorado; botão central arrasta a câmera. Selecione camada no painel direito. Objetos reais mantêm o tamanho do recorte; Tile na grade ajusta o desenho ao tamanho escolhido. Escolha lápis, borracha, retângulo, balde, conta-gotas ou seleção. Ctrl+C/Ctrl+V copiam/colam; Ctrl+Z/Ctrl+Y desfazem/refazem. Retângulo e balde têm limite de 4.096 células. Selecione um elemento e use Editar elemento selecionado para ajustar dimensões, camada, referência e propriedades.

Crie, renomeie, reordene, oculte ou bloqueie camadas. Colisões são retângulos explícitos, nunca inferidas da imagem. Menu Mapa permite criar/editar; ferramenta Colisão desenha pelo mouse. Ver → Raio do jogador usa o mesmo CollisionService com raio 18. Snap pode ser alternado no menu Editar. Uma área transitável não abre um furo em outro retângulo sólido.

Menu Entidades coloca NPC, baú, recurso, inimigo e jogador. Escolha IDs reais; NPCs e inimigos respeitam a região de conteúdo. Um mapa exportável precisa de exatamente um PLAYER em posição livre. Portais referenciam o ID de outro documento, chegada e missão aceita opcional. Conexões entre mapas mostra o grafo e os problemas; abrir documentos do mesmo diretório registra mapas relacionados.

Salvar/Salvar como usam `.sgmap` V1 e backup `.bak` de uma versão íntegra. Para recuperar, escolha Recuperar backup e depois Salvar como. Um arquivo corrompido não é sobrescrito automaticamente. Não há migração silenciosa de mapas antigos.

Para o primeiro teste, abra `assets/maps/studio_tutorial.sgmap`, examine spawn/NPC/parede/portal e use Mapa → Testar mapa no RPG. A exportação validada inicia runtime e cliente reais em sessão sintética sem salvar personagens. WASD move, Espaço ataca, Q usa habilidade e F interage. R recupera após KO. O botão Colisões revela a geometria; o botão adicional usa a habilidade do Guardian. Diagnósticos ficam no diretório do playtest ao fechar.

O primeiro adaptador aceita mapas 960×640 e até três regiões de conteúdo distintas (village, forest, ruins) no conjunto alcançável. A autoria aceita até 8192×8192; mapas incompatíveis continuam editáveis, mas não são apresentados como executáveis no jogo. Condições de campanha existentes, inclusive o acesso ao santuário, continuam válidas.

Esta é ferramenta experimental: arte e experiência precisam de homologação humana. O marco TASK-102 é ampliado nas TASK-103 a TASK-114; consultar o relatório final para funcionalidades avançadas e limites efetivos.


## World Studio consolidado

Tutorial completo: `WORLD-STUDIO-TUTORIAL.md`. Contratos e limites: `WORLD-STUDIO-FUNCIONALIDADES.md`. Medicoes: `WORLD-STUDIO-PERFORMANCE.md`. Atalhos adicionais: Ctrl+S salvar, Ctrl+Shift+S salvar como, Ctrl+O abrir, Ctrl+N novo. Editar > Configurar grade ajusta o snap futuro sem mover objetos existentes. O menu Selecao permite recuperar e desbloquear objetos por ID.
