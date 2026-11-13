# World Studio — colisão direcional e edição no jogo

> A integração descrita neste relatório foi corrigida na entrega seguinte: [paleta na partida real](WORLD-STUDIO-PALETA-NA-PARTIDA-2026-11-08.md). O acesso pelo painel admin agora edita a própria partida com uma paleta compacta; os trechos abaixo sobre preview incorporado são o registro da implementação anterior.

## Estado verificado e objetivo

Checkout na branch `beco`, HEAD `13b91dc`, com muitas alterações locais anteriores a esta entrega. O editor já tinha canvas, biblioteca, preview autoritativo por TCP local e publicação transacional. A colisão era descrita por retângulo e booleano `solid`; o jogo integrado desenhava esses retângulos como estruturas. A integração anterior mantinha canvas e jogo em áreas separadas.

Esta entrega acrescenta colisões por borda, separação entre física e arte, profundidade para objetos e acesso ao editor na janela da partida do anfitrião. Nenhum commit, push, release ou publicação foi executado.

## Como usar

1. Na partida de um **anfitrião de servidor gerenciado**, abra o menu da partida e escolha **Painel admin → Editor de mapas**. Convidados não recebem essa capacidade.
2. Na aba **Mapas**, selecione um mapa ativo/rascunho e clique em **Entrar no editor**, ou escolha **Novo mapa**. Use ID diferente dos mapas existentes. Mapas novos recebem somente um marcador de entrada do jogador, sem arte de cenário.
3. Para o runtime atual, mantenha **960×640 pixels**. Os demais tamanhos continuam disponíveis para autoria, mas o jogo os recusa explicitamente.
4. A sessão de edição inicia o runtime real do RPG dentro da área central. **Editar mapa no jogo** marcado permite editar com o mouse; desmarcado permite controlar o personagem com WASD. O segundo personagem de teste continua disponível pelas setas quando o jogo recebe foco.
5. Escolha a camada, a imagem e os recortes na biblioteca/aba Tiles. As ferramentas existentes recebem as coordenadas da câmera do jogo; zoom e gestos usam o mesmo enquadramento. Mudanças válidas são agrupadas e aplicadas automaticamente pelo servidor de edição, normalmente após cerca de 200 ms mais validação e tick.
6. Abra **Colisao** para ativar a ferramenta e ver quatro setas por célula. Vermelho indica borda bloqueada; ciano indica passagem. Clique próximo à seta para alternar aquela borda. O centro aplica a combinação do painel. Botão direito limpa a colisão direcional daquela célula. A ferramenta **Colisao retangular** preserva a autoria de hitboxes livres existente.
7. Para árvores, coloque a arte na camada **OBJECTS**. Deixe o lado superior livre e bloqueie os demais conforme a área desejada. Ajuste **Base de profundidade** nas propriedades do elemento se a base visual não coincidir com o tronco. ABOVE/ROOF continuam sempre acima; chão e detalhes continuam abaixo.
8. Use **Salvar rascunho** para persistir a autoria e **Voltar ao jogo** para retornar à partida original. Aplicação à partida compartilhada permanece na ação explícita **Salvar - aplicar ao jogo**, sujeita às validações e permissões existentes.

## Semântica física e visual

- As setas indicam **bordas da célula**, bloqueadas nos dois sentidos; não são movimento permitido nem plataformas de sentido único.
- Máscara `1=cima`, `2=direita`, `4=baixo`, `8=esquerda`. Quatro bordas selecionadas conservam volume integral sólido. Máscaras parciais produzem paredes de um pixel de espessura nas bordas escolhidas. Nenhuma borda não gera obstáculo.
- O corpo real do personagem e a largura efetiva contra cenário continuam valendo. Uma abertura menor que o personagem não se torna atravessável por estar marcada como livre. Para uma árvore grande, configure as células correspondentes ao contorno desejado; não bloqueie automaticamente todas as bordas internas.
- Ocultação atrás da árvore vem da ordenação entre a base do objeto e a posição vertical dos atores, independentemente da colisão. O pivô opcional `depth.pivot` é relativo ao topo da imagem e deve estar dentro da altura do elemento.
- Hitboxes não viram sprites ou blocos de cenário. Contornos e setas aparecem como ferramentas de edição. Offline/Coop não ativam o novo compositor por padrão.
- Desfazer/refazer, movimentação de grupos e prefabs preservam as máscaras. Colisão sobre jogador, spawn ou outra entidade pode ser recusada; a revisão anterior permanece em execução, sem empurrar entidades para encaixar a edição.

## Compatibilidade e limites

- Mapas antigos continuam sendo lidos com todas as bordas bloqueadas para hitboxes sólidas, mantendo a física original.
- Documentos sem máscaras novas continuam sendo escritos em **schema 1**. Colisões direcionais usam **schema 2**, com um campo adicional por hitbox. Clientes/editores antigos recusam essa versão; todos os participantes precisam desta implementação para receber conteúdo direcional. O número do protocolo de gameplay não foi alterado.
- A sessão de edição é local, autoritativa e usa o runtime real. Não é a mesma conexão/personagem persistido da partida compartilhada: alterações ao vivo são testadas nessa sessão. **Não foi implementada mutação automática da partida compartilhada a cada pincelada**, respeitando a separação de edição/publicação do projeto.
- O runtime conserva até três regiões por conjunto de playtest, com nomes de conteúdo `forest`, `village` e `ruins`. A publicação multiplayer conserva a exigência de início em `forest` e regiões `forest`/`village`. O catálogo administrativo lista os mapas ativos, os já abertos e até 256 arquivos locais encontrados até três níveis em `studio-projects`, excluindo históricos de publicação.
- O editor depende dos assets do projeto para iniciar o jogo integrado. A homologação física de aparência, Radmin e launcher nativo continua pendente.

## Arquivos envolvidos

- Dados/física: `WorldDocument`, `DocumentStore`, `StudioGeometry`, `GroupEditing`, `Prefabs`.
- Ferramentas/interface: `CollisionTools`, `PaintTools`, `WorldCanvas`, `LayerTools`, `FileTools`, `WorldStudio` e novo `AdminMapTools`.
- Sessão e acesso: `StudioLiveTools`, `StudioLiveWindow`, `LauncherSession`, `SessionGameLifecycle`, `GameInputRouter`.
- Apresentação: `DebugPanel`, `StudioMapRenderer`, `StudioMiniMap`, `StudioPlaytest`, `QuestPlaytest`, `RpgNetworkClientLauncher`.
- Novos testes: `DirectionalMapEditingTest` e `InGameMapEditorTest`.

Os nomes acima correspondem a classes Java do módulo `server`. As alterações anteriores nesses arquivos foram preservadas.

## Validação

Java 17 e Maven 3.10.0 confirmados. Compilação Maven executada. Os testes novos cobrem máscaras, travessia nos dois sentidos, entrada/saída por cima, varredura, diagonal, schema antigo/novo, backup, grupos, prefabs, patch, conteúdo de rede e ocultação visual. A integração Swing cobre incorporação/restauração da partida, clique convertido pela câmera, atualização da revisão autoritativa e alternância entre edição e movimento.

Comando da regressão focalizada:

```powershell
mvn -q -f server/pom.xml '-Dtest=InGameMapEditorTest,DirectionalMapEditingTest,Studio*Test,DocumentStoreTest,DocumentFirstSaveSafetyTest,GroupEditingTest,PrefabsTest,PortalGraphTest,EditorOperationsTest,WorldDocumentTest,Task178CollisionTest,WorldRevisionTest,GameInputRoutingTest,SkillAimInputRoutingTest,SoftSlideCollisionTest,PlayerBlockCollisionTest,SessionGameReconnectCloseTest' test
```

A primeira execução ampliada teve 137 testes, com uma falha de transferência de foco AWT em `GameInputRoutingTest`; o mesmo método passou isoladamente. A repetição completa terminou com **137 testes, zero falhas, zero erros e zero ignorados**. Isso não substitui homologação manual em máquinas físicas.

Também foi executado `powershell -NoProfile -ExecutionPolicy Bypass -File tools/compile-local.ps1 -Force`: 272 fontes compiladas e classes locais do cliente atualizadas em `server/target/launcher/classes`. Não houve empacotamento ou distribuição de launcher nativo.

## Reversão

Reverter somente os trechos desta entrega, preservando os diffs locais anteriores. Antes de voltar a uma build antiga, guardar os documentos schema 2 e seus backups: ela não consegue abrir colisões direcionais. Remover as bordas direcionais nesta implementação permite voltar a salvar schema 1, mas perde essa configuração; fazer isso somente em cópias. Não restaurar globalmente o checkout nem apagar mapas ou saves.
