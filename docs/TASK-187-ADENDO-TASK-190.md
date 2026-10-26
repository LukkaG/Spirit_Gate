# TASK-187 — adendo de backpressure na TASK-190

O relatório original da TASK-187 permanece histórico e seus logs não foram alterados. O ensaio com 4.192.250 bytes da TASK-190 encontrou desconexão durante publicação. Uma regressão curta reproduziu o mecanismo: um único Ping recebia overflow porque o próprio produtor já havia preenchido as quatro posições de controle com conteúdo.

A TASK-190 reserva uma das quatro posições para a resposta ao keepalive: conteúdo só é produzido quando `remainingCapacity()>1`. O limite total continua quatro, sem novos frames, opcodes ou filas. Pode haver até quatro emissões por tick se o writer drenar concorrentemente; sob retenção, ficam no máximo três controles de conteúdo pendentes. O teste 187 passou a exigir esse limite de três e o mesmo mínimo de amostras reais de writer retido, além de recuperação do outro cliente.

Excesso de Pings ou peer que não drena controles indefinidamente continua sujeito a isolamento/timeout. Isso não remove limites para acomodar abuso. O teste de Ping único e o soak completo com payload/duração originais devem passar antes da validação da TASK-190. Fonte anterior, falhas e diff ficam preservados no checkpoint da 190.
