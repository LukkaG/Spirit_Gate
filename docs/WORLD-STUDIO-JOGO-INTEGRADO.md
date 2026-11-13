# World Studio com jogo integrado

O editor apresenta a autoria do mapa acima e o jogo executando abaixo, na mesma janela. Arraste o divisor para ajustar o espaco. O painel usa o servidor local de preview e o renderizador RPG existentes, com dois personagens de teste.

## Como testar

1. Abra `world-studio.cmd` e carregue um mapa com spawn PLAYER. Para comecar com um exemplo, abra Ferramentas > Laboratorio de Colisoes e carregue um cenario.
2. Clique **Iniciar jogo integrado**.
3. Selecione o asset da casa e use **Lapis / Objeto real** na camada visual desejada. A sincronizacao existente valida o documento e aplica a nova revisao ao jogo sem reiniciar a sessao.
4. Escolha **Colisao** e arraste sobre a base da casa no mapa editavel. O retangulo aparece durante o gesto; ao soltar, a hitbox respeita o snap e os limites do mapa. Use **Mapa > Editar colisao** para ajustar coordenadas ou solidez.
5. Clique no painel do jogo: WASD move o jogador 1 e as setas movem o jogador 2. Caminhe contra a casa para testar o bloqueio. F2 alterna os diagnosticos. Ao voltar ao editor, os comandos de movimento sao soltos.
6. Desfazer/refazer e alteracoes nas hitboxes seguem a mesma sincronizacao. O jogo mostra a revisao efetivamente instalada. Se uma parede nova ocupar um personagem, a revisao pode ser recusada; mova o personagem e use Servidor > Sincronizar rascunho completo.
7. Use **Servidor > Encerrar Preview** para parar ou **Reiniciar Preview** para recriar a sessao.

A edicao ocorre no painel de autoria; o painel jogavel testa a revisao aceita. Este fluxo e local, sem gravar personagens pessoais. Aplicar em um servidor principal continua usando a publicacao explicita existente. Limites de mapas, catalogos, eventos e validacao permanecem os documentados em TASK-179-LIVE-EDITOR.md.

Arquivos de implementacao: WorldStudio.java (layout), StudioLiveTools.java (inicio integrado), StudioLiveWindow.java (hospedagem, foco e encerramento), PaintTools.java (previa do gesto), CollisionTools.java (limites das hitboxes). Testes: StudioEmbeddedGameTest, StudioLiveTest, StudioGestureSafetyTest e StudioGeometryTest.
