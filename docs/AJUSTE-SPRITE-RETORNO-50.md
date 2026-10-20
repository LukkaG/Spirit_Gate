# Retorno ao tamanho anterior da sprite

A pedido do responsável, o padrão local volta à escala uniforme de **0,5 (50%)**, mantendo a caminhada em **220 ms por frame**. Direções, pivôs, controles e velocidade lógica permanecem preservados.

Para testar o código atual compilado, execute `testar-rpg-sprites.cmd`. As variáveis opcionais `SPIRIT_GATE_SPRITE_SCALE` e `SPIRIT_GATE_FRAME_MS` continuam permitindo ajustes sem recompilar.

Os relatórios da TASK-079 registram a configuração histórica daquela tarefa. Esta alteração posterior substitui apenas o padrão de escala local. O site publicado e os pacotes já distribuídos não foram atualizados por este ajuste.
