# TASK-225 — Proposta de sincronização de trajetórias

**Proposta não implementada; exige aprovação de arquitetura/compatibilidade.** A aplicação 7 e todos os bytes V1–V6 permanecem como estavam no início desta tarefa.

## Problema comprovado

`Projectile.advance` persegue a posição atual do alvo por segmentos de até 18 unidades/tick, respeitando cenário e primeiro corpo. `SkillProtocol.Event` informa somente sequência, tick, lançador, skill e dois pontos. O evento inicial fixa o alvo naquele instante; o evento terminal informa o contato real, sem ID de execução/projétil, alvo/geração ou curva intermediária.

Na [reprodução controlada](skills-preview-task225/README.md), o alvo sai de `(340,300)` e passa a mover cinco unidades por tick no eixo Y a partir do terceiro tick. O desvio visual intermediário chega a **28,676 unidades** no tick 10. O contato autoritativo ocorre no tick 11 em `(321,054;331,959)`, e o impacto é desenhado nesse ponto, com erro raster de **0,068**. O HP permanece 1.000 até o contato, depois cai a 980. São eventos reais do engine alimentados ao renderer real, com relógio controlado; não é captura de latência TCP/Radmin.

## O que o contrato atual permite

Preservar o contato final recebido, deduplicar eventos, limpar por revisão/região/desconexão e cancelar o efeito se o snapshot confirmar que o lançador morreu/saiu. Esta última limpeza foi corrigida na TASK-225.

O cliente pode continuar apresentando o segmento inicial como aproximação; não consegue reconstruir a curva exata nem relacionar cancelamento por morte/geração do alvo a um projétil específico. Inferir o inimigo mais próximo da coordenada inicial pode errar com corpos próximos, respawn, primeiro contato diferente do alvo selecionado e sete raios simultâneos. Esse procedimento não foi implementado.

Reposicionar o impacto para o fim da reta ou mudar a simulação para seguir a animação invalidaria a autoridade. Também não se deve apresentar cada amostra intermediária como um novo lançamento/impacto da aplicação 7: isso mudaria silenciosamente a semântica existente.

## Extensão sugerida para aprovação

1. Negociar uma capacidade/aplicação posterior, com DTO próprio. O número e os limites finais devem ser definidos na revisão do protocolo; não reaproveitar silenciosamente o frame de `SkillProtocol.Event`.
2. Identificar execução e projétil por IDs monotônicos da sessão, com lançador, skill, tick e revisão/região. Três cortes/leque precisam de identidade individual; casts não podem ser associados apenas por proximidade.
3. Publicar amostras autoritativas limitadas por snapshot: posição anterior/atual e tick. Publicar também nascimento, contato e cancelamento com causa limitada. Coordenadas finais devem vir da simulação, inclusive interceptação, parede e falta de alcance.
4. Interpolar somente entre amostras recebidas, respeitando lacunas, ordem e revisão. O alvo não é necessário para perseguir localmente se os próprios pontos forem transmitidos. Não extrapolar colisão, aplicar dano nem manter um impacto sem confirmação. Rotação pode usar o último segmento recebido.
5. Manter cache limitado de execuções/terminais no cliente para deduplicação e late join. No servidor, estabelecer orçamento de amostras/bytes e expiração explícita sem descarte silencioso de terminais essenciais; medir antes de escolher cadência.
6. Conservar o caminho legado para aplicações 6/7: aplicação 6 recebe os estados que já entende, e aplicação 7 mantém eventos atuais e sua aproximação conhecida. Só clientes com a nova capacidade recebem o DTO novo.

## Validação exigida antes de integrar

Dois clientes com versões mistas; alvo lateral e móvel rápido; primeiro corpo diferente do selecionado; morte/geração no voo; parede fina; alcance esgotado; eventos duplicados/atrasados/perdidos; revisão, viagem e desconexão; vários projéteis simultâneos. Comparar ponto autoritativo, pixel e tempo de contato, além de medir tráfego/carga. Uma curva exata no tempo do servidor ainda não elimina a latência de exibição; isso deve ser descrito com a política de buffer escolhida.

## Decisão nesta TASK

Manter o A-03 intermediário como limitação comprovada e documentada, com o impacto final correto e a limpeza implementada. Não há alteração de aplicação/protocolo nem afirmação de curva integralmente corrigida. A implementação desta proposta e a TASK-226 não foram iniciadas.
