# TASK-078 — Relatório final

**Implementação experimental concluída, com ressalvas artísticas.** Data: 19/10/2026, America/Sao_Paulo. Integração restrita ao jogador principal do RPG offline, mediante opção explícita. Sem release ou push.

## Resultado

Os 12 PNGs fornecidos foram inspecionados, decodificados, associados e registrados por SHA-256. Há quatro Idle e dois Walk por direção. Três nomes reais diferem em capitalização da lista da tarefa; foram preservados e mapeados explicitamente. Dimensões, hashes, limites alfa e pivôs estão em TASK-078-ASSETS.md e no JSON de inspeção.

Os arquivos não cabem no contrato oficial 64×64. A implementação usa adaptador próprio que carrega os PNGs completos uma vez, verifica seus hashes antes do decode e aplica nearest-neighbor com escala uniforme 0,5. Pivôs por frame removem a variação das margens transparentes e alinham a base visível à posição lógica. Não se criaram arquivos de imagem transformados para o jogo.

`RpgPlayerAnimation` usa movimento efetivo entre snapshots e relógio de ticks para alternar os dois frames a cada 150 ms configuráveis. Parada, colisão bloqueada, mudanças de direção/estado e repaints repetidos foram testados. A direção efetiva é mantida ao parar; diagonais usam eixo dominante e empate vertical. Não altera simulação, I/O de save ou combate.

## Arquivos de implementação

Criados: `RpgPlayerSprites.java`, `RpgPlayerAnimation.java`, `experimental-player.properties`, `RpgPlayerSpritesTest.java` e `testar-rpg-sprites.cmd`. Modificados: `RpgLauncher.java` e `DebugPanel.java` (hook de apresentação opcional, nulo por padrão). Lista completa e diff em `.task-checkpoints/TASK-078/`.

`ValidatedSpriteAtlas`, `DirectionalAnimation`, `DebugLauncher`, `DebugRuntime`, `DebugWorld`, Coop, protocolos, `RpgWorld`, `RpgRuntime`, persistência, saves e os 12 originais não foram alterados. O renderer compartilhado recebeu uma extensão desativada por padrão, sem dependência RPG; não houve integração na entrada da arena ou Coop nem substituição de seus pacotes. Comparação do caminho inativo foi pixel a pixel.

## Testes e execução real

- Final: **307 testes Java e 25 Python**, zero falhas, erros ou ignorados; 10 casos Java novos e todos os anteriores mantidos.
- Primeira execução: um teste novo presumiu tangência exata na borda; a expectativa foi substituída por verificação do bloqueio real, sem alterar o serviço. Logs anteriores preservados.
- Cliente real com sprites: capturas Idle nas quatro direções, os oito Walk, retorno Walk→Idle, escala no cenário e bloqueio junto ao obstáculo. Segundo harness terminou com exit 0; primeira tentativa interrompida preservada.
- Cliente real geométrico: aberto e capturado sem ativar sprites.
- Arquivos ausentes, corrompidos, com capitalização diferente ou trocados: fallback geométrico automático e diagnóstico legível, verificado em testes isolados.

Detalhes: TASK-078-TESTES.md e TASK-078-VALIDACAO-VISUAL.md. Capturas automatizadas de componentes de janela real não equivalem à aprovação artística humana.

## Como iniciar

Na raiz, executar `testar-rpg-sprites.cmd`. Para o modo anterior, `testar-rpg-sprites.cmd --geometrico`. As classes já foram compiladas nesta validação. Para recompilar depois, `python tools/qualidade.py --offline --task 078-local`.

Alternativa: `java -cp server/target/classes com.projetok.prototype.rpg.RpgLauncher --experimental-player-sprites`. Duração opcional antes de `-cp`: `-Dspiritgate.rpg.spriteFrameMillis=150`. Sem a flag ou propriedade de ativação, o comportamento é o geométrico. Reiniciar o cliente para trocar o modo. A campanha e os saves continuam no formato original; o iniciador do EXE antigo não usa estas classes novas.

## Limitações e aprovação pendente

Integração de desenvolvimento, não empacotada em release. O caminho dos assets é `assets/dev/player-base` sob a pasta de trabalho ou junto ao EXE; pacotes antigos sem esses arquivos/classes não ganham suporte automaticamente. Pivôs inferidos por alfa, escala 50% e ciclo de dois frames/150 ms precisam de homologação perceptiva do responsável. Downsampling nearest-neighbor mantém bordas discretas, mas reduz detalhe dos originais; não é uma promessa de escala inteira nativa 1:1.

Não existem Run/Attack/Hit/KO, camadas visuais de equipamento, sprites próprios por arquétipo/aparência ou integração online nesta entrega. A morte usa fallback anterior. O HUD principal mantém informações do jogador; os adornos geométricos locais do ator são substituídos pela sprite no modo experimental. Os riscos de ciclo de vida da TASK-077 não foram corrigidos, por estarem fora desta integração.

## Preservação e encerramento

Checkpoints `TASK-078-before` e `TASK-078-after`, hashes, ZIPs recuperáveis, evidências reais e diff isolado disponíveis em `.task-checkpoints/TASK-078/`. `preservation.json` lista exclusivamente as duas fontes existentes alteradas e os arquivos novos; confirma preservação de assets, regras, runtime, saves, protocolo, Coop e infraestrutura oficial do atlas. Relatórios e checkpoints anteriores permanecem intactos.

O responsável mantém a aprovação artística. Nenhuma animação não fornecida foi inventada, nem se iniciou tarefa seguinte.
