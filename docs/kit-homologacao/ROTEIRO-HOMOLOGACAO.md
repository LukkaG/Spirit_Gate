# Homologação posterior - Spirit Gate 0.11.0-rc1

Todos os itens abaixo estão PENDENTES DE HOMOLOGACAO HUMANA. Os testes automatizados e o smoke local estão documentados separadamente. Esta é uma candidata a avaliação local, sem publicação ou multiplayer.

## TASK-007 - Abrir sem Java/Maven

Em outro Windows x64, sem Java e Maven instalados, extrair o ZIP inteiro para uma pasta com espaços. Abrir SpiritGate.exe mantendo app/ e runtime/ juntos. Conferir abertura, fechamento e nova abertura. Verificar VERSION.txt. Não mover somente o EXE e não desativar proteções do Windows. O executável não é assinado.

## TASK-008 - Arbustos e poste (três minutos)

Ativar H para hitboxes. Ir ao sul do ponto inicial até a faixa y=420. Dois arbustos verdes e um poste devem estar visíveis. Tentar atravessar cada objeto com WASD: bloqueia. Apontar além deles e pressionar Shift: atravessa os leves, respeita vigor/recarga e termina em posição livre. Depois, WASD volta a colidir. Testar Shift contra uma parede grande e contra bordas: continuam bloqueantes. R restaura os três leves.

## TASK-009 - Estados no HUD

Observar Q e Shift prontos, gastar vigor com E e tentar lançar sem saldo. Conferir aviso de recurso insuficiente, recusa sem gasto/cooldown e desaparecimento do feedback breve. Lançar Q e Shift, observar recargas e indicador de movimento. Um alvo além de parede fora do alcance do dash não deve ser rotulado como contato. Ao alcançar efetivamente uma parede/borda, o aviso deve indicar bloqueio sólido/borda. R limpa estados transitórios.

A saída pendente aparece somente se atores móveis ocuparem toda a trajetória do dash; não há um atalho de teste para forçar esse caso. Se ocorrer, o movimento aguarda uma mudança do espaço sem novo consumo/cooldown; o jogador continua vulnerável. R deve recuperar a arena. Registrar o caso se a espera persistir.

## TASK-010 - Sequências de jogo

Tocar e soltar Shift rapidamente; segurar Shift; esperar a recarga sem soltar: não deve ativar sozinho. Soltar e pressionar de novo deve permitir nova tentativa. Repetir Q/Shift em ordens diferentes com pouco vigor. Caminhar durante o dash e conferir retorno de WASD ao fim. Usar Q sobre mob: Freeze pode ocorrer, impede movimento e permite ataques em alcance. A probabilidade é 30%, portanto uma tentativa manual sem Freeze não é falha. Morrer e usar R; verificar vida/vigor restaurados, recargas/rastros/zonas limpos e nenhuma repetição automática de tecla mantida.

## TASK-006.1 - Colisão seletiva de atores e paredes

Aproximar um mob, apontar além dele e usar Shift: o jogador pode atravessá-lo sem causar dano automático. Ataques recebidos continuam possíveis. Mirar contra uma parede: o corpo não ultrapassa a face considerando seu raio. O dash deve terminar normalmente livre ou recuar pela trajetória em caso de destino ocupado.

## TASK-011 - Integridade e relato

Conferir o SHA-256 do ZIP com o arquivo .sha256 externo. Guardar versão, Windows, passos e resultado ao preencher MODELO-RELATO-BUG.md. Revisar cada task separadamente. Nenhum item deste roteiro foi marcado como homologado pelo agente.
