# TASK-010 - Regressão determinística da arena

18/10/2026. Base: TASK-009 com 189 testes aprovados por mvn clean test. O inventário considerou MovementReplayTest, suites de colisão/recuperação, habilidades, Freeze, vigor, demo e HUD. Escopo: sequências e invariantes das mecânicas existentes, sem novas funcionalidades de jogo.

## Lacunas antes e abordagem

Os testes anteriores já cobriam os serviços isolados, a fila, três replays históricos de 600 ticks, renderização pura e alguns fluxos assíncronos. Faltava executar o caminho real de reinício do runtime de forma determinística, combinar comandos/recursos/colisões por mais de um tick e proteger a informação de vigor nos valores imediatamente abaixo do custo.

DebugRuntime mantém a mesma thread agendada e a mesma ordem de comandos. O corpo de um tick é disponibilizado internamente para execução pelo proprietário do estado; os testes usam esse método sem iniciar a thread. Uma fábrica interna de DebugWorld permite controlar a aleatoriedade e recriar o demo no R. O loop de produção usa o mesmo método, evitando reproduzir a implementação do runtime dentro dos testes. Não foi introduzido um novo motor de simulação.

As sequências novas usam deltas explícitos de 1/30 s, comandos reais e gerador controlado. Não usam Thread.sleep, janela JFrame, servidor, banco ou rede. Os replays e os testes anteriores permanecem como referências; nenhum baseline foi atualizado.

## Bug reproduzido antes da correção

O HUD anterior formatava o saldo insuficiente com zero casas decimais. Math.nextDown(35f) continua insuficiente para custo 35, mas aparecia como 35/35; o mesmo ocorria com custo 55. HudPrecisionRegressionTest foi escrito primeiro e falhou de fato, registrado em TASK-010-hud-reproducao-falha.log. Após a correção mínima, o mesmo teste passou em TASK-010-hud-correcao.log.

A correção remove apenas essa contagem redundante do aviso, mantendo VIGOR INSUFICIENTE, custo e dados numéricos do snapshot/barra. Não houve alteração no saldo, na comparação do domínio, no consumo ou na regeneração. A suíte completa foi executada após as sequências novas e repetida para conferir estabilidade.

## Cobertura depois

ArenaSequenceRegressionTest acrescenta 8 sequências pelo runtime/fila/bindings reais, com EDT somente para entrada: Shift rápido e mantido além da recarga, competição FIFO com 80/100 de vigor, suspensão/retomada WASD e ataque, Freeze com ataque da IA, R limpando recursos/efeitos/feedback/comandos anteriores e preservando os três leves, morte e reinício. Compara também 100 snapshots completos de duas execuções controladas, sem criar um baseline novo.

DomainSequenceRegressionTest acrescenta 4 sequências: travessia de ator sem dano automático e retorno à física normal/combate; cinco pulsos de ICE e Freeze com geometria sólida ou leve; corpo móvel sobre o destino com recuo e atualização da grade seguida de movimento; floats vizinhos a tangências e bordas. O gerador injetado falha se houver sorteios extras. A referência sem índice é comparada à indexada após corrigir a posição.

HudPrecisionRegressionTest acrescenta 1 caso para o bug descrito acima. No total são 13 testes novos. Os 26 arquivos originais de teste da base 170 foram comparados diretamente e permanecem intactos, assim como seus baselines.

AUTOMATIZADO APROVADO: primeira suíte completa mvn clean test e repetição mvn clean test -o, ambas com 202 testes, zero falhas/erros/ignorados. Contagens e resultados por classe são idênticos. O modo offline confirmou a execução sem acesso a repositório remoto, com as dependências locais. Logs completos TASK-010-final.log e TASK-010-repeticao-offline.log; resumo TASK-010-testes.txt. O teste isolado do runtime também passou (8 casos).

Arquivos: DebugRuntime, correção pontual em SkillHudMapper e três classes novas de teste; documentação/PDF, resumos, logs e diff. Não foram alterados física, fórmula de dano, habilidades ou testes antigos. A exposição de advanceOneTick e da fábrica é restrita ao pacote e exige a thread proprietária; a produção mantém um único agendador.

## Critérios de evidência

Registrar a contagem real e os dois logs finais da suíte; conferir estabilidade sem trocar resultados esperados. Documentar as sequências adicionadas, eventuais limitações e o diff isolado. A homologação humana continua pendente e não impede avançar à TASK-011 se a base automatizada estiver verde.
