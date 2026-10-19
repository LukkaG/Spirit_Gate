# TASK-004 - Criostase

Spirit Gate | 18/10/2026 | Java 17 e Maven

## Resultado

Q lança uma zona fixa de gelo na posição capturada do mouse. O comando usa o ID cryostasis, custa 55 de vigor e inicia cooldown de 3 segundos. O centro é limitado a 240 unidades do jogador. A zona tem raio 85 e dura 2,5 segundos. Causa 7 de dano ICE por pulso, em 0, 0,5, 1, 1,5 e 2 segundos. Não existe sexto pulso na expiração.

O dano máximo teórico por alvo é 35 com afinidade neutra e permanência nos cinco instantes de pulso. Mobs continuam se movendo normalmente: congelamento e lentidão não foram implementados. Paredes não bloqueiam a habilidade. O raio pode ultrapassar visualmente a arena, mas o centro solicitado deve estar dentro da área jogável; a renderização é recortada nos limites.

## Preparação e proposta apresentada

Foram inspecionados DebugWorld, DebugInput, DebugRuntime, DebugLauncher, DebugSnapshot, DebugPanel, CombatService, Attacker, DamageType, RaceRegistry, StaminaService e testes existentes. Attacker está em domain/entity/capability; DamageType está em domain/race. O comando inicial mvn clean test -q passou com os 53 testes existentes antes das alterações.

A proposta apresentada separou um contrato imutável de intenção, serviço específico de elegibilidade/cooldown, zona com relógio determinístico e adaptador Attacker. A integração usa a fila FIFO existente; o painel apenas desenha os valores publicados. Nenhuma dependência foi adicionada e nenhum arquivo foi excluído ou renomeado.

## Arquitetura e responsabilidades

SkillCastCommand é o contrato único e imutável: skillId, targetX, targetY. Não executa regras e permite transportar entradas inválidas até a validação autoritativa, sem lançar exceções no EDT.

CryostasisService mantém o estado de um único lançador. Valida ID, coordenadas finitas e mouse dentro de [0, largura) e [0, altura), vida e cooldown. Valida a origem, calcula o centro limitado na direção solicitada e usa StaminaService.tryConsume para consumir somente após a elegibilidade. Recusas retornam um motivo explícito e não criam zona ou cooldown. A verificação de vigor é feita antes de criar qualquer estado; o cálculo puro do clamp pode ocorrer antes dessa consulta.

CryostasisZone guarda centro, tempo decorrido e índice do próximo pulso. advance(0) consome o pulso imediato; cada avanço libera somente os novos pulsos devidos. O contador termina em cinco, independentemente do delta. O relógio usa double acumulando deltas de simulação e tolerância de 0,000001 s para arredondamentos de float nos limites. Não há relógio de parede, sleep, timer Swing ou thread adicional de gameplay.

CryostasisAttacker implementa Attacker com dano base 7 e DamageType.ICE. ICE foi acrescentado após EARTH: os valores e a ordem anteriores do enum foram preservados. RaceRegistry e Race permanecem inalterados: affinityFor usa 1,0 para gelo em todas as raças atuais. WATER não é utilizado.

O único ponto que causa dano na skill é combat.attack(attacker, mob). A implementação não chama receiveDamage ou HealthComponent.applyDamage. CombatService permanece byte a byte igual ao estado inicial. A skill verifica vida antes de cada aplicação, evitando repetição de eventos de morte em pulsos posteriores.

## Pulsos, hitboxes e movimentação

Cada pulso consulta vida e posição atuais dos alvos. A interseção usa distância ao quadrado menor ou igual ao quadrado da soma do raio 85 com a hitbox do mob, incluindo tangência. Uma coleção por identidade impede que um alvo duplicado na lista receba duas aplicações no mesmo pulso; alvos que morrerem são ignorados depois disso.

A zona fica fixa no cenário quando o jogador se move. Alvos podem entrar e sair e só são atingidos se estiverem em interseção no instante de aplicação. Vários pulsos cruzados por um delta são processados uma vez cada. Na arena, as posições são amostradas por ticks de até 0,1 s; não há reconstrução de trajetórias entre pulsos.

Zonas já criadas continuam até expirar mesmo se o lançador morrer. Novos lançamentos permanecem bloqueados. Reiniciar cria um mundo e um serviço novos, eliminando zonas e cooldowns.

## Fila, Q e mouse

DebugLauncher coleta posição do mouse no EDT. Pressionar Q captura essa posição em SkillCastCommand e publica na mesma fila das demais entradas. O conjunto de teclas mantidas permite publicar somente na transição inicial; autorepeat e Q mantido não duplicam lançamentos, inclusive após o cooldown liberar. Soltar Q habilita uma nova tentativa.

Antes do primeiro movimento do mouse ou após sair do painel, o alvo é inválido e a tentativa é recusada sem gasto. Sidebar e rodapé não fazem parte da área jogável e também são recusados. O mouse não precisa ser clicado: Q lança no ponto apontado.

DebugCommandBuffer conserva os comandos mesmo quando Q é pressionado e solto inteiramente entre ticks. Ao drenar, publica uma lista imutável. Um reinício elimina intenções anteriores ao comando de reset; intenções explicitamente posteriores a ele pertencem ao novo mundo. Q ainda mantido no EDT não produz uma nova intenção automaticamente após R.

DebugInput foi inspecionado e não precisou ser modificado: a skill usa um comando separado do estado WASD/E/Espaço. Seus construtores anteriores permanecem compatíveis.

## Ordem da simulação e feedback

No começo do tick, os cooldowns e efeitos anteriores são atualizados. CryostasisService avança as zonas existentes e devolve danos já aplicados pelo CombatService. Depois, se o jogador estiver vivo, ocorrem regeneração, movimento e ataque básico na ordem anterior, seguidos dos comandos de skill, IA dos mobs e esforço E. Assim, Q tem prioridade sobre E no mesmo lote para uso do saldo disponível.

As zonas novas recebem pulso imediato e só envelhecem a partir do próximo tick. A regeneração continua sendo uma por tick, nunca por comando. Os eventos de morte existentes incrementam defeated; os resultados de dano viram os mesmos EffectView usados pelos números flutuantes. O coordenador preserva a mensagem de derrota do mob ao receber um resultado letal.

DebugSnapshot publica cryostasisCooldown e uma lista imutável de IceZoneView com centro, raio, tempo e duração. DebugPanel desenha preenchimento azul-claro, borda com marcas de gelo e pulsação discreta baseada no tempo publicado, sem mutação do mundo. O rodapé foi ampliado em 32 pixels para o atalho Q e o cooldown sem sobrepor as informações antigas. WASD, Espaço, E, H e R foram mantidos.

## Testes executados

Comando final na pasta server: mvn clean test. Resultado real: BUILD SUCCESS, 73 testes, zero falhas, zero erros e zero ignorados; compilação com release 17.

CryostasisServiceTest: 13 testes novos. Verificam lançamento válido, custo 55, dano imediato, vigor insuficiente, cooldown e liberação, clamp exato e diagonal, ID inválido, NaN, infinitos, mouse fora da arena, jogador morto, cinco pulsos sem sexto, delta de 30 Hz, passos grandes, entrada/saída, tangência, deduplicação, cadáveres, evento de morte pelo CombatService, afinidade neutra, ordem do enum e zona fixa após movimento/morte do lançador.

CryostasisIntegrationTest: 7 testes novos. Verificam publicação de zona e dano, vigor e ataque preservados, comandos inválidos/cooldown, paredes, expiração, runtime real e reinício sem estado pendente, fila de skills, mouse e Q no EDT, toque rápido, tecla mantida mesmo após liberar cooldown e captura renderizada sem gameplay no painel.

Os 53 testes anteriores continuam aprovados. MovementReplayTest não foi alterado, seus três baselines não foram recalculados, e a projeção anterior continua verificando 1.800 ticks. Os novos campos do snapshot ficam fora dessa projeção por não pertencerem ao estado legado. Movimento, colisões, corte, dano básico e vigor mantêm suas asserções anteriores.

Os testes usam fixtures que configuram HP para cenários de morte; somente a preparação dos testes modifica esse HP diretamente. A implementação da habilidade usa exclusivamente CombatService para dano.

Resultados por classe: TASK-004-testes.txt. Captura: TASK-004-criostase.png, gerada pelo teste e inspecionada. A imagem mostra zona ativa sobre um mob, número de dano, vigor 45/100 e recarga 3,0 s. Não houve sessão interativa com JFrame; testes do mouse e do teclado usam o EDT com a thread real de simulação.

## Arquivos novos e alterados

Novos, sob server/src/main/java/com/projetok/domain/skill/:
- SkillCastCommand.java.
- CryostasisAttacker.java.
- CryostasisZone.java.
- CryostasisService.java.

Alterados, sob server/src/main/java/com/projetok/:
- domain/race/DamageType.java: ICE ao final.
- prototype/DebugCommandBuffer.java: comandos de skill no lote e limpeza em R.
- prototype/DebugRuntime.java: transporte de SkillCastCommand.
- prototype/DebugLauncher.java: Q e captura do mouse.
- prototype/DebugWorld.java: elegibilidade, avanço e publicação de resultados.
- prototype/DebugSnapshot.java: cooldown e zonas por valor.
- prototype/DebugPanel.java: gelo, pulsação e indicador.

Novos testes, sob server/src/test/java/com/projetok/:
- domain/skill/CryostasisServiceTest.java.
- prototype/CryostasisIntegrationTest.java.

Documentação e ferramentas:
- README-PROTOTIPO.md e docs/HISTORICO-TECNICO.md.
- docs/TASK-004.md, docs/TASK-004-testes.txt, docs/TASK-004.diff.
- docs/Spirit_Gate_TASK-004.pdf e docs/TASK-004-criostase.png.
- tools/gerar_pdf_task004.py.

O diff textual é relativo ao estado anterior a esta tarefa, capturado antes de editar. Não inclui server/target, binários, segredos ou a si próprio recursivamente. A captura final e o PDF são entregues separadamente, como artefatos de documentação. Não houve commit ou branch porque a pasta não possui checkout Git acessível.

## Trade-offs e limites

O serviço é específico e mantém estado por lançador; não é um framework de habilidades. O mundo cria sua própria instância. A lista de zonas foi mantida para representação e ciclo de vida claros, embora cooldown de 3 s e duração de 2,5 s impeçam sobreposição de lançamentos normais deste jogador.

Os parâmetros são os solicitados para o protótipo. Não há modificadores de stats no adaptador nem redefinição de afinidades raciais. Não foram adicionados congelamento, lentidão, status genéricos, PvP, multiplayer ou segunda habilidade. Movimento, colisões e sua varredura opcional não foram alterados.

## Reprodução e revisão

Na pasta server: mvn clean test. Na pasta Projeto-K: python -B tools/gerar_pdf_task004.py para regenerar o PDF. Para validar manualmente, execute rodar-prototipo.cmd, aponte dentro da arena e pressione Q. Observe custo e pulsos, mova-se para confirmar centro fixo, aponte longe para ver o clamp, segure Q além de 3 s para confirmar ausência de repetição e use R para limpar o estado.

Entrega concluída para revisão técnica. Nenhuma próxima task foi iniciada.
