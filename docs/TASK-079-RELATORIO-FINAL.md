# TASK-079 — Relatório final

**Concluída tecnicamente; aguardando homologação visual do responsável.** Nenhuma aprovação artística automática foi atribuída.

O RPG offline passa a recomendar **40% de escala e 220 ms por frame Walk**. As quatro direções e os dois frames por direção foram preservados. É possível retornar a 50%/150 ms ou testar 35%/220 ms e 40%/280 ms apenas com propriedades JVM ou variáveis de ambiente do iniciador de desenvolvimento, sem recompilar.

`RpgPlayerSprites` usa escala uniforme configurável, aplica esse fator aos pivôs existentes e ao desenho completo em nearest-neighbor, mantendo alfa. Valores inválidos geram diagnóstico e fallback geométrico. O relógio visual já existente continua baseado em ticks, independente de repaint; `RpgPlayerAnimation` não precisou de alteração. Os PNGs e seus metadados permaneceram intactos.

Alterados somente dois arquivos existentes: `RpgPlayerSprites.java` e `testar-rpg-sprites.cmd`. Criado `RpgPlayerRefinementTest.java` com seis casos. Foram produzidos os quatro documentos solicitados, logs, capturas/GIFs das quatro combinações e checkpoints antes/depois com hashes e diff isolado.

Resultado automatizado: **314 testes Java e 30 Python aprovados**, sem falhas, erros ou ignorados. A base efetiva anterior à tarefa era 308/30, pois o atualizador acrescentara 1 Java e 5 Python após a referência 307/25 da TASK-078. Nesta tarefa entram 6 Java adicionais; nenhum teste anterior foi removido ou alterado.

A comparação abriu quatro clientes reais com parâmetros explícitos e personagens sintéticos, usando os bindings de movimento existentes e capturas da janela. As evidências incluem Idle inicial no mesmo ponto, Walk 1/2 e Idle nas quatro direções e uma sequência GIF frontal por configuração. Os resultados, comandos e ressalvas estão em TASK-079-COMPARACAO-VISUAL.md. A resolução de 30 Hz e o custo da captura limitam conclusões perceptivas sobre cadência; homologação do ritmo e do tamanho é do responsável.

Os testes verificaram proporcionalidade, pés e centro em diferentes escalas, ausência de cortes no canvas, alfa, defaults/configuração inválida, duração, Idle, troca de direção, igualdade de estado lógico e fallback. Os hashes antes/depois confirmam que combate, deslocamento, hitboxes, colisão, saves/progressão, arena, Coop, protocolo e atlas oficial não receberam alterações. Alterações de trabalho anteriores foram preservadas e distinguidas do diff TASK-079.

Evidências: `.task-checkpoints/TASK-079/` contém `TASK-079-before/after.json`, ZIPs recuperáveis sem saves pessoais, `preservation.json`, diff isolado, XMLs JUnit, quatro pastas de capturas e GIFs, comparação em PNG/HTML e harness externo. Os saves existentes foram verificados apenas por hash.

Para avaliar agora, executar `testar-rpg-sprites.cmd` na raiz; para recompilar futuramente sem sobrescrever estes logs, usar `python tools/qualidade.py --offline --task 079-local`. Fechar e reiniciar o cliente ao trocar parâmetros.

Não foi gerada nova release, não houve push ou alteração do canal público de atualização, não foram integradas animações novas e a TASK-080 não foi iniciada. O pacote público atual continua com a configuração anterior. Próximo passo: o responsável escolher/homologar a combinação visual antes de autorizar distribuição.
