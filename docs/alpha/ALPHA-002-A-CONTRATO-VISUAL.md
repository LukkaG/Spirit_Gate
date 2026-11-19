# ALPHA-002-A — Contrato de integração visual de Quatro Relâmpagos

Estado: interface documentada; frente visual não implementada. Nenhum byte de Protocol, GameplayProtocol ou SkillProtocol foi alterado.

## Autoridade e janela

`thunder_5` mantém o corpo no centro original, com identidade, região e ocupação normais. Os quatro dashes futuros são imagens de apresentação: não escrever coordenadas do ator, enviar movimento, alterar hitbox, decidir contato ou calcular dano.

Duração atual: `RpgSkillCatalog.get("thunder_5").mechanics().durationTicks() == 32`, com relógio de simulação de 30 ticks/s. Raio configurado: 110; a seleção autoritativa considera o corpo do inimigo de raio 19, tangência e linha de visão. Não aumentar o raio para acomodar arte.

Janela autoritativa: início inclusivo, término exclusivo, `[início, início + 32)`. No tick terminal a imunidade e o controle de área já expiraram; na ordem normal mundo → encontro → engine, a IA pode voltar a agir antes dos quatro impactos desse tick. Os quatro danos continuam concentrados no término da duração, como antes; não existem quatro janelas de dano separadas de oito ticks. O campo de intervalo 8 no catálogo não agendava esses impactos separadamente e não passou a fazê-lo nesta entrega.

## Dados existentes no wire

| Fonte | Campos disponíveis | Uso e limite |
| --- | --- | --- |
| `SkillProtocol.State` | `hash`, `tick`, `events`, cinco `cooldowns`, `sentryShots`, `windCheckpoint` | Apenas aplicação 7 recebe estado de skills. Eventos da região, limitados a 16. Recargas são privadas ao destinatário. |
| `SkillProtocol.Event` | `sequence`, `tick`, `player`, `skill`, `fromX`, `fromY`, `x`, `y` | Sequência monotônica de eventos do engine. Não contém ID explícito de execução, geração do personagem, duração ou lista de vítimas. |
| Snapshot de atores | ID, posição, HP, facing e demais campos existentes de `Protocol.Actor` | O lançador continua presente e parado. Posição física sempre vem do snapshot, nunca dos rastros. |
| Eventos de combate | sequência, tick, tipo, jogador, inimigo, geração do inimigo e quantidade | Confirmam os impactos/recompensas existentes. Geração é da vítima do combate, não um token de execução da técnica. |
| Revisão/região | Hash no estado e região no fluxo de mundo | Invalidam contexto visual antigo. Não equivalem a credencial de recuperação. |

### Marcadores preservados

- Início: `skill == "thunder_5"`, `from=(centroX, centroY)` e destino diferente da origem. O deslocamento de **uma unidade no evento** é somente um marcador: para a metade esquerda do mapa, `x=centroX+1`; para a direita, `x=centroX-1`; `y=centroY`. O ator não se move essa unidade.
- Fim normal: mesmo `player`/skill, `from == destino == centro`, sequência posterior e tick inicial + 32 na execução contínua normal.
- Quatro cortes: um evento terminal visual e quatro aplicações de dano reais via ledger, para cada inimigo elegível sobrevivente até cada impacto. Não emitir dano local nem supor que sempre há quatro eventos HIT quando a vítima morre antes do quarto golpe.
- O servidor não publica um marcador individual para cada dash visual e não publica a lista de inimigos paralisados. O cliente pode desenhar o círculo/rastros, mas não declarar autoridade de controle por estimativa geométrica.

## Integração proposta para a próxima frente

1. Vincular o efeito a `(hash, região, player, sequence do início)`; ignorar sequências repetidas e snapshots antigos. Essa chave é visual, não autenticação nem geração wire.
2. Ocultar somente o sprite normal do lançador durante a janela recebida. Preservar seu ator no snapshot, seleção e coordenada física. Desenhar os quatro rastros ao redor do centro, sem substituir a posição do ator pela posição da animação.
3. Derivar o progresso de `State.tick - Event.tick` e da duração do catálogo compatível, compensando idade do evento. Não reiniciar a animação ao receber o mesmo início.
4. Encerrar no terminal normal. Como proteção visual, terminar também quando o relógio recebido ultrapassar o fim configurado; não depender exclusivamente do fusível antigo de cinco segundos. Exibir o corpo novamente não torna o cliente autoridade sobre imunidade.
5. Invalidar efeitos em mudança de hash/região, desconexão, ausência/morte confirmada do lançador ou novo contexto de sessão. Snapshot anterior ao início não deve apagar uma execução recém-recebida.
6. Não reexecutar cortes após cancelamento ou retransmissão. Um terminal sem início disponível pode desenhar um efeito curto de impacto, mas não deve recriar a fase oculta.

## Cancelamento e limites reais

`NetworkSkillPresentation` atualmente mantém `lightningFields`, usa `hidden(player)`, limpa por terminal, região/revisão e ausência/morte, e possui timeout visual de cinco segundos. Esse cliente não foi alterado. A nova execução física já é finita; o renderer antigo ainda pode manter ocultação além da duração se perder o terminal ou se houver cancelamento com ator vivo no mesmo contexto.

O cancelamento do engine remove pendências/eventos; não existe motivo de cancelamento ou ID de execução explícito no wire atual. O buffer de 16 eventos pode eliminar um início/terminal antes de um observador lento vê-lo. Aplicação 6 não recebe o canal de skills; conserva mundo e combate, sem promessa de quatro dashes/ocultação equivalentes à aplicação 7.

Sem mudar wire, a próxima frente pode melhorar expiração por duração, deduplicação e limpeza contextual usando os dados acima. Cancelamento explícito e correlação inequívoca entre gerações exigiriam proposta de extensão negociada separada; não foram implementados. Não inventar geração usando nome/IP ou tratar a sequência como prova de posse.

Aceite visual futuro: corpo oculto apenas na janela, quatro rastros sem movimento físico, término/cancelamento sem efeitos órfãos, comportamento sob atraso/eventos repetidos e observadores 6/7 explicado. Nenhuma nova arte é autorizada por este documento isoladamente.
