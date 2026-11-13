# World Studio — paleta na partida real

Esta entrega corrige a interpretação da integração anterior: a ferramenta agora fica sobre **a partida já aberta**, em um painel interno de 340×540 no máximo, e a edição altera o mapa do **mesmo servidor e dos mesmos clientes**. Não se troca o viewport, a HUD, o personagem ou a conexão por um preview.

## Uso

1. Reinicie o cliente local atualizado e entre na partida como anfitrião de servidor gerenciado.
2. No menu da partida, abra **Painel admin → Editor de mapas**.
3. A paleta aparece sobre o mundo. Escolha ferramenta, camada e recorte na biblioteca/opções. O painel pode ser arrastado pelos limites da janela.
4. **Editar cenário**, ou **F4**, alterna criação e controle do personagem. Selecionar uma ferramenta ou imagem também ativa a edição. Escape cancela o gesto e sai desse modo. O jogador continua sendo o da partida atual.
5. Pinte no próprio mundo. Gestos confirmados são agrupados por cerca de 200 ms e enviados ao servidor. As alterações são validadas e sincronizadas com os demais clientes antes de a simulação retomar. O intervalo real depende da validação e da rede; não é atualização a cada quadro.
6. Abra **Opções → Colisao** para configurar as bordas. As setas vetoriais têm haste curta, ponta larga, contorno e sombra. Bordas livres são ciano discreto; bloqueadas têm preenchimento coral e contorno claro. A física direcional anterior foi preservada.
7. **Mapas / novo mapa** permite escolher a cena existente ou criar uma cena vazia na região escolhida. A capacidade administrativa leva o mesmo personagem até um spawn livre quando muda a região. Novo mapa substitui a cena ativa dessa região em memória, preservando os portais existentes; não altera os arquivos originais automaticamente.
8. **Salvar mapa (.sgmap)** grava o documento. **Salvar mundo em disco** persiste o conjunto pela transação existente. Fechar a paleta mantém as alterações já aceitas na partida; alterações só em memória não sobrevivem ao encerramento do servidor.

## Arquitetura e compatibilidade

- `InGameMapEditor` reaproveita `WorldStudio`, `PaintTools`, biblioteca, camadas e colisões como ferramentas. Usa `GamePanels`, já existente, e não cria uma segunda infraestrutura de janelas.
- `DebugPanel` recebe apenas interceptador de gestos e overlay de autoria opcionais. O restante da HUD e do desenho continua sendo o da partida.
- `LiveMapEdits` usa `reserve → activate → confirm` no `RevisionCoordinator`, com recuperação em caso de cancelamento/falha. Essas aplicações são em memória e não chamam o gravador de histórico. Uma reserva recusada não cancela a transação de outro chamador.
- A visita administrativa passa por `RevisionCoordinator` e `RpgSessionWorld`, na thread proprietária do servidor. Valida região, jogador, geometria e ocupação antes de posicionar o personagem. Não adiciona comando administrativo ao socket público.
- O servidor continua autoritativo. Colisões que ocupariam atores ou spawns são recusadas, mantendo o mapa anterior. A troca de revisão usa a barreira existente e pode interromper a simulação brevemente; não é um protocolo novo de streaming por tile.
- Mantidos os limites atuais de 960×640 e regiões `forest`/`village` da partida compartilhada. O cadastro de novos arquivos não aumenta a quantidade de regiões simultâneas do runtime.
- Mantidas as regras e formatos da colisão direcional anterior: leitura dos mapas antigos, schema 2 para máscaras parciais e mesma geometria física. As setas são UI vetorial; não houve geração ou sobrescrita de sprites.
- Offline/Coop não instalam esses interceptadores. Não houve alteração de protocolo de gameplay, saves pessoais, banco, release, canal `updates` ou instalação de ferramentas.

## Validação

Novos testes:

- `LiveMapEditsTest`: mesma partida com dois clientes, sincronização, ausência de gravação automática, salvamento explícito, nova cena, visita administrativa, cancelamento, reserva concorrente e recusa de colisão sobre jogador.
- `CompactWorldEditorTest`: HUD e viewport originais, mesma conexão e ID de personagem, ausência de `PreviewServer`/`StudioLiveWindow`, clique no mundo sincronizado com outro cliente, fechamento sem desconexão e setas desenhadas em imagem.

Evidências geradas pelo teste visual em `server/target/world-editor-evidence/compact-editor.png` e `arrows.png`. São capturas do teste local, não homologação de Radmin ou de launcher nativo.

O resultado final da regressão e da compilação é registrado no relatório desta conversa e em `logs/analises/20261108-paleta-na-partida-real.md`.

## Reversão

Retirar apenas os trechos desta entrega e os componentes novos, preservando os diffs anteriores. A configuração de colisões direcionais da entrega anterior pode permanecer. Reiniciar o servidor sem salvar o mundo descarta alterações apenas em memória; manter cópias de qualquer `.sgmap` salvo. Não usar restauração global do checkout, excluir mapas ou apagar saves.
