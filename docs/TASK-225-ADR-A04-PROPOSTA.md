# TASK-225 — ADR proposta para o risco A-04

**Estado: proposta para decisão; não aprovada e não implementada em produção.** Referência: 03/11/2026, branch `beco`, HEAD `6d72c65`. Recorte autorizado da TASK-225-R1; não inicia a TASK-226 nem altera duração, imunidade, materialização, saída do jogador ou aplicações 6/7.

## Problema e evidência disponível

Quatro relâmpagos inicia intangibilidade, espera 32 ticks, aplica quatro cortes uma única vez e tenta materializar em apoio válido. `LightningField.advance()` conserva a execução enquanto `skillReappear()` falhar. O marcador `resolved` impede repetir os quatro cortes, mas não há prazo máximo para a espera. `receiveDamage()` retorna sem reduzir HP durante a intangibilidade.

A [investigação original](TASK-225-A04-EVIDENCIAS.md) reproduziu 180 ticks de fase/imunidade em câmara artificial sem apoio livre. O cliente expira sua ocultação em cinco segundos se o evento terminal faltar. Liberar os ocupantes permitiu reentrada no tick seguinte. Isso comprova uma condição extrema reproduzível e uma divergência de apresentação; não comprova exploração no mapa distribuído nem em Radmin físico.

As medições originais observaram grande variação entre rodadas. O custo acumulado e os ensaios adicionais desta revisão são registrados em [TASK-225-R1-TESTES.md](TASK-225-R1-TESTES.md), sem converter mediana isolada em garantia de carga. Testes de candidato são modelos descartáveis e não representam alteração do runtime.

## Contratos atuais confirmados

| Responsabilidade | Implementação observada | Consequência para a decisão |
|---|---|---|
| Fase e dano | `RpgSkillEngine.LightningField.advance`, `resolved`, `RpgWorld.receiveDamage` | Os quatro danos são resolvidos uma vez. Encerrar só o desenho não retira a imunidade autoritativa. |
| Materialização | `RpgWorld.skillReappear` e `SharedRpgEncounter.playerReentrySpace` | Testa origem, busca radial e grade, com corpos atuais e caminho reto livre de cenário. Não atravessa parede para alcançar um apoio distante. |
| Cancelamento de execução | `RpgSkillEngine.remove` e `clear` | Limpa flags, pendências, DoTs e eventos do lançador. **Não remove o personagem do mundo nem fecha o socket por si só.** |
| Saída do mundo | `RpgSessionWorld.leave` | Remove a execução, a sessão, sua participação no encontro e admissão de revisão. Não opera diretamente o socket. |
| Transporte | `LocalSimulationServer.Peer.closePeer`, privado; fila `departures`; executor proprietário `owner` | Retira o peer, fecha socket e agenda `world.leave` para o ciclo proprietário. A execução da skill não deve acessar socket nem chamar código de transporte em thread indevida. |
| Pedido de desconexão | `SessionSimulation.disconnectRequested` / implementação atual de `RpgSessionWorld` | Hoje atende o coordenador de revisão. Falta um contrato interno de encerramento por materialização; não se deve reaproveitar uma rejeição de conteúdo com motivo falso. |
| Readmissão | `RpgSessionWorld.joinCharacter` e construtor de `RpgWorld` | Cria personagem efêmero novo, com ID novo e UUID derivado da instância/ID. XP/nível voltam à configuração `syntheticLevel`; domínio volta a `none`; inventário, moedas e missões são reinicializados. Não é retomada autenticada nem preservação do progresso do personagem anterior. |
| Spawn | `joinCharacter`, `CollisionService.isFree`, `RpgCollisionProfile.scenery` | Valida obstáculos, limites, mobs vivos e aliados vivos tangíveis; tenta o spawn e deslocamentos horizontais `0,+40,-40,+80,-80`. Recusa a entrada se nenhum for livre. Não procura qualquer ponto do mapa nem garante sucesso imediato. |
| Recarga após saída | `RpgSkillEngine.remove`, `session` e `RpgSessionWorld.joinCharacter` | A sessão removida perde recargas e demais estados efêmeros. Readmissão cria estado novo; não há conservação confiável de cooldown por identidade estável. |
| Contribuição após saída | `SharedRpgEncounter.leave` e `hit` | A saída retira o mundo do mapa de participantes; não apaga imediatamente IDs antigos dos conjuntos de contribuição. A recompensa percorre somente mundos ainda presentes e IDs contribuidores: ID novo não herda contribuição do ID antigo, mas pode criar contribuição legítima própria. O conjunto daquela geração é removido quando ocorre a morte. |
| Quantidade de participantes | `Semaphore(2)` no transporte; `sessions.size()>=2` na sessão; `players.size()>=2` no encontro | Lançador e um observador TCP cabem no runtime. Lançador mais **dois** observadores TCP simultâneos excedem o contrato atual e não foram autorizados. Dois renderers observadores podem ser exercitados isoladamente, com essa limitação explícita. |
| Apresentação | `NetworkSkillPresentation`, fusível de cinco segundos e limpeza por ausência/morte do ator | A retirada do ator observada cancela sua apresentação. O DTO não expõe estado completo de intangibilidade nem razão específica do encerramento. |
| Motivo de desconexão | `LocalClient` interpreta `Protocol.Error.reason` durante o handshake; no reader da partida com revisão aceita frames esperados de estado/conteúdo e Pong | Enviar `Protocol.Error` depois da admissão não garante mostrar sua razão no cliente atual: pode produzir apenas erro genérico de snapshot/resposta inesperada. Feedback específico precisa de alteração e validação explícitas do contrato cliente/servidor, além de entrega antes do fechamento. |

Fontes: [RpgSkillEngine](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java), [RpgWorld](../server/src/main/java/com/projetok/rpg/RpgWorld.java), [SharedRpgEncounter](../server/src/main/java/com/projetok/rpg/SharedRpgEncounter.java), [RpgSessionWorld](../server/src/main/java/com/projetok/multiplayer/RpgSessionWorld.java), [LocalSimulationServer](../server/src/main/java/com/projetok/multiplayer/LocalSimulationServer.java), [SessionSimulation](../server/src/main/java/com/projetok/multiplayer/SessionSimulation.java) e [NetworkSkillPresentation](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java).

## Alternativas comparadas

| Critério | A — Prazo e encerramento da participação | B — Recuperação em apoio previamente validado | C — Manter a espera atual |
|---|---|---|---|
| Regra proposta | Manter os 32 ticks; tolerar mais 60; tentar apoio até o tick relativo 92. Se continuar impossível, retirar participação de modo coordenado. | Definir um apoio de recuperação e os critérios explícitos de uso, invalidá-lo quando revisão/região/ocupação mudar e revalidá-lo na execução. | Continuar a busca sem limite após os 32 ticks. |
| Segurança física | Remover integralmente o corpo evita materializar dentro de aliado/parede. Limpar apenas a imunidade é insuficiente e não integra esta alternativa. | Só é segura com apoio **e trajeto** elegíveis no instante final. Ponto que foi livre antes pode estar ocupado agora. Recuperação através de parede ou entre mapas seria regra nova de teleporte, não autorizada implicitamente. | Conserva as validações físicas existentes; não materializa em apoio inválido. |
| Invulnerabilidade persistente | Limita a participação bloqueada se a retirada lógica/transportada for efetiva e finita; apenas acrescentar timeout ao desenho ou à pendência não resolve. | Resolve somente se houver garantia demonstrada de recuperação finita; se o ponto falhar e continuar esperando, reproduz o problema. | Risco condicional já reproduzido: fase e imunidade podem persistir sem prazo. |
| Experiência do jogador | Pode desconectar o jogador em situação extrema; não há morte artificial nem perda de HP inventada. A readmissão atual perde o progresso efêmero anterior, incluindo itens/XP/moedas/missões já conquistados. Exige aceite desse custo, mensagem coerente e fluxo de retorno claro. | Evita retirada da partida quando existir apoio válido, mas pode deslocar o jogador e mudar sua vantagem tática. A origem/local de recuperação precisa de regra de produto. | Mantém a sessão e seu progresso, mas deixa o jogador sem ações e pode mostrar seu corpo após o fusível visual enquanto continua imune. |
| Combate e cooldown | Não reembolsar energia nem repetir golpes. A saída atual descarta cooldowns e o mundo com seu progresso; readmissão volta ao nível sintético configurado, domínio `none` e inventário/moedas/missões iniciais. Isso exige aceite explícito ou projeto separado de identidade/retomada. Recompensas já recebidas pelos quatro cortes também fazem parte do estado descartado; não há transferência automática. | Poderia conservar a mesma sessão, progresso, custo e recargas. Deslocamento pode escapar de cerco/ataque; posição, enfrentamento e região devem ser definidos e testados. | Custo, progresso e recarga continuam nos relógios atuais; a execução não termina enquanto faltar apoio. Não oferece recuperação finita ao jogador. |
| Dano duplicado | Conservar `resolved`; encerramento idempotente não chama a área novamente, não cria recompensa/death fictícia e elimina pendências antigas. Nova conjuração após readmissão é outra execução, não retransmissão da anterior. | Conservar `resolved` e não relançar a skill durante a recuperação. Repetir tentativa de apoio não pode repetir quatro cortes. | `resolved` já conserva quatro impactos únicos; risco de repetição não foi reproduzido na espera atual. |
| Efeito sobre dois clientes | O lançador observa desconexão; o observador recebe ausência do ator e limpa a fase. IDs novos na readmissão não devem herdar o efeito antigo. Não inventar um impacto de materialização onde não ocorreu. | Ambos devem observar a mesma posição final e encerramento da fase; o cliente não escolhe o apoio. Eventos atuais podem representar posição final, mas não explicam a causa ou todo o estado de recuperação. | Ambos recebem os mesmos eventos disponíveis; cada relógio visual pode atingir o fusível em momento diferente sob atraso/perda. O servidor continua imune em ambos os casos. |
| Reconexão | Reutilizar somente a admissão normal em spawn validado, aceitando recusa se todos os candidatos estiverem ocupados. Não restaurar o mundo preso nem confiar em nome/IP como identidade. | Se a conexão cair, a saída normal continua necessária; reconexão atual cria outro mundo, portanto não há retomada garantida no apoio de recuperação. | Desconectar limpa a execução antiga; readmissão cria mundo novo e perde cooldown. A espera não soluciona sua própria condição extrema. |
| Protocolo | Encerramento e ausência do ator podem usar o contrato 6/7 atual, com canal **interno** entre mundo e transporte. Razão estruturada, ACK de retirada, estado de fase ou retomada autenticada exigem proposta própria se não couberem nos contratos atuais. | Coordenada de ator já existe, mas regra de correção/teleporte, motivo e fase podem exigir contrato novo para apresentação fiel. Não inventar opcode ou significado novo de evento 7 nesta revisão. | Nenhum frame novo; a lacuna de estado que causa a divergência visual permanece. |
| Custo por tick | Até o limite, conserva custo da busca atual; depois da retirada não continua buscando. Precisa limitar também o ciclo de encerramento. Não é por si uma otimização da busca. | Apoio ainda válido pode custar uma validação local; manutenção/revalidação da reserva e fallback aumentam complexidade. Sem reserva exclusiva não há garantia de disponibilidade. | Pode repetir busca completa indefinidamente por execução bloqueada, competindo com o orçamento do owner a 30 Hz. |
| Rollback | Exige retirar/encerrar de modo aprovado execuções já em transição antes de remover a política. Não reintroduzir participantes removidos ou ressuscitar cooldown/efeito antigo. A proposta atual é apenas documental. | Remover política e referências de recuperação após resolver execuções em trânsito; comparar revisão/identidade. Não restaurar uma coordenada antiga como rollback genérico. | Nenhum rollback de implementação nesta revisão, pois corresponde ao comportamento vigente; custo é manter o risco conhecido e o gate bloqueado. |

## Recomendação e condições de aprovação

**Recomendar A como política de contingência**, mantendo a materialização normal até o limite. É a alternativa que oferece término finito sem exigir que exista um apoio livre nem escolher uma posição arbitrária. B depende de uma regra adicional de espaço/reserva/deslocamento que o projeto ainda não fornece; C preserva um estado que já foi reproduzido como persistente e não encerra o risco.

A recomendação é condicionada às decisões abaixo; não equivale a autorização de implementação:

1. Aprovar 60 ticks adicionais após a fase normal de 32, com teto de 92 ticks relativos ao lançamento. Definir o tick por relógio autoritativo, sem timer individual nem tempo da animação.
2. Aprovar a retirada da participação em condição extrema, incluindo seu custo de experiência. O comportamento normal até o tick 32 e os quatro danos únicos não mudam.
3. Aceitar explicitamente que a readmissão atual é um **novo personagem efêmero**, com cooldowns novos e **perda do progresso anterior**: XP/nível reiniciados conforme `syntheticLevel`, domínio `none`, inventário/moedas/missões iniciais. Isso inclui ganhos já creditados pelos quatro cortes antes da retirada. Alternativamente, exigir uma política separada de identidade/retomada antes de implementar A. Nome e IP não permitem assegurar identidade; não se deve prometer preservar cooldown, itens, XP ou missões na reconexão atual. Se a conservação for requisito, a aprovação de A isoladamente é insuficiente.
4. Aprovar que a primeira versão possa encerrar com feedback genérico de conexão, sem alterar aplicações 6/7, ou exigir proposta separada para causa estruturada/estado de fase. O `Protocol.Error` existente não garante entrega antes de fechar o socket e o reader atual da partida não trata sua razão como mensagem de encerramento. Mensagem específica não pode ser prometida sem definir e testar o contrato, o canal de controle e o ciclo de saída. Mesmo reaproveitar esse frame exigiria revisão explícita da semântica e fallback de clientes anteriores; não é uma capacidade já homologada.

## Esboço interno de implementação para revisão futura

Nenhum passo abaixo foi aplicado nesta TASK-225-R1.

1. `LightningField` mede `world.skillTick()-start`. No tick relativo 32, resolve os quatro cortes uma vez e faz a primeira busca. Nos ticks 33..92 pode fazer até 60 tentativas adicionais. No tick 92, primeiro tenta a materialização; somente a falha solicita encerramento. Isso define **61 chamadas possíveis** incluindo a inicial, sem ambiguidade de borda.
2. A pendência devolve ao coordenador um resultado interno de falta definitiva de apoio, com a identidade da sessão/execução. Não conhece sockets nem chama UI. O coordenador rejeita resultados de sessão substituída/região/revisão antiga.
3. A thread proprietária retira a participação de forma idempotente: cancelar skills, limpar flags/pendências, remover corpo/sessão/encontro e registrar o pedido interno de desconexão. A retirada deve ser observável antes de qualquer dano/ação/snapshot posterior que ainda use o ator. **Não chamar somente `skillDashing(false)` sobre um corpo obstruído.**
4. O transporte reconhece o pedido interno e fecha o peer antes de pedir snapshots privados dessa sessão removida. O fluxo atual chama `world.step()` e depois consulta peers/snapshots; removê-la sem esse cuidado pode chamar `skillsSnapshotFor`/`gameplaySnapshotFor` para ID inexistente. O canal de encerramento precisa continuar consultável até seu consumo, mesmo depois da retirada lógica.
5. A saída normal posterior (`departures -> world.leave`) deve ser idempotente; comandos já enfileirados do ID antigo são descartados pelas validações de sessão. Não deixar um peer fantasma nem um corpo invisível durante a espera de fechamento. Falha/atraso do transporte não pode restabelecer o personagem.
6. Só a admissão existente pode criar o próximo personagem; ela revalida spawn e pode recusar. Nenhum evento terminal com coordenada fictícia é emitido para simular uma materialização que não ocorreu.

Esse desenho requer trabalho explícito na fronteira entre engine, coordenador e transporte. O teste isolado que retira `engine`, mapa de participantes e encontro demonstra a política lógica do candidato; **não valida por si `Peer.closePeer`, filas, comandos concorrentes, cliente real ou atomicidade dessa integração**.

## Custo acumulado e orçamento

No fixture original 960 × 640, raio 12, uma tentativa bloqueada visita até 37.450 candidatos: origem, 1.024 sondas radiais e 36.425 pontos da grade. É contagem derivada dos laços, não contagem instrumentada de chamadas em produção. O custo de cada candidato depende dos obstáculos/corpos e das verificações de segmento.

Com 61 tentativas completas da proposta A, o limite derivado desse mesmo fixture seria **2.284.450 candidatos** ao longo da execução. Não é uma previsão de milissegundos. O ensaio adicional de 60 tentativas consecutivas nos ticks 32..91 mede uma janela finita da espera atual, e não a integração final da política de saída aos 92.

Duas fixtures executadas sequencialmente na mesma thread modelam acúmulo de trabalho; não simulam concorrência real de várias sessões, contenção, GC, transporte e carga total do servidor. Relatar tempo total, maior tentativa e dados brutos; não multiplicar uma mediana para garantir capacidade de jogadores. Reservas/cache, busca incremental ou cadência reduzida são opções futuras separadas, com invalidação de obstáculos/corpos e limites próprios; não foram inseridas silenciosamente nesta ADR.

### Evidência focalizada adicional da R1

A rodada `a04-compiled.log` aprovou **12 casos novos, zero falhas/erros/skips**: nove no teste de materialização e três de observadores/TCP. O candidato de 92 ticks foi executado **externamente ao runtime**, retirando engine, encontro e mapa de participantes do teste; não valida fechamento de socket nem constitui aprovação da política.

| Janela medida no fixture | Total acumulado | Mediana da tentativa/rodada | Máximo da tentativa/rodada |
|---|---:|---:|---:|
| Um mundo, 60 tentativas consecutivas | 70,934 ms | 1,118 ms | 2,022 ms |
| Dois mundos independentes sequenciais, 60 rodadas / 120 chamadas | 112,861 ms | 1,649 ms por par | 3,303 ms por par |

O intervalo medido cobre somente `engine.tick`; `world.tick` e `encounter.step` ficam fora. Os números identificam a rodada focalizada preservada em [a04-compiled.log](../.task-checkpoints/CORUJAO-IX/TASK-225-R1/a04-compiled.log), não a última repetição de toda a suíte. [Dados brutos da rodada mais recente](skills-preview-task225-r1/a04-consecutive-searches.txt) podem variar ao repetir o teste. Não há limiar de tempo usado como aprovação nem garantia de servidor sob carga.

Dois renderers receberam frames serializados dos eventos reais; a perda deliberada do terminal em um deles reproduziu o limite de apresentação. Os cenários TCP usaram **lançador e um observador**, verificando quatro impactos únicos, materialização normal e reconexão do lançador. O novo mundo recebeu ID diferente, domínio `none` e recargas zero; isso não demonstra conservação de progresso ou integração da saída candidata.

## Critérios de validação para uma implementação aprovada

- Área livre, borda, paredes e aliado temporário preservam o término normal e os apoios permitidos.
- Nenhuma repetição do mesmo tick/frame ou pedido de encerramento repete os quatro danos, morte, contribuição ou recompensa.
- Ausência completa de apoio: fase até o prazo aprovado, última tentativa definida, retirada lógica única e nenhuma busca/ação/corpo antigo após o encerramento.
- Liberação nos ticks 31, 32, 91 e 92 materializa somente em apoio válido; liberação depois da retirada não ressuscita a sessão.
- Morte, perda de sessão, troca de região/revisão e substituição de mundo descartam resultado antigo; uma morte natural durante imunidade não deve ser simulada como caminho de gameplay válido. Estado inválido pode ser injetado apenas como fault de teste, com rótulo explícito.
- TCP com lançador e observador confirma ausência do ator, limpeza do cliente, fechamento de peer, liberação da capacidade e comandos antigos rejeitados. O terceiro participante continua recusado enquanto o limite de dois não receber autorização própria.
- Dois observadores isolados, com terminal ausente em um deles, demonstram o limite da apresentação; isso não deve ser descrito como três clientes TCP simultâneos.
- Readmissão aceita somente spawn revalidado; spawn bloqueado é recusado sem corpo parcial. Novo ID/UUID não herda fase antiga. A política de cooldown/identidade escolhida deve ser verificada explicitamente.
- Medir tempo acumulado no owner com cenários reproduzíveis, sem exceder limites externos de teste. Exercitar aplicações 6/7 e complemento gráfico, sem alterar mensagens ou usar animação para decidir dano.

## Decisão pendente e gate

O responsável precisa escolher A, B ou C e aprovar suas consequências de sessão/progresso/cooldown/feedback. A recomendação técnica é **A como contingência para o contrato efêmero atual, condicionada à retirada coordenada e ao aceite explícito da perda de progresso na reconexão**. Se exigir preservar identidade, itens, XP, missões ou cooldown, preparar antes a decisão correspondente; não resolver por nome/IP ou estado fictício no cliente.

Esta revisão prepara a decisão e os testes. A-04 continua um gate separado e bloqueante até haver decisão aprovada e implementação validada, caso necessária, mesmo que as regressões de progressão/missão sejam saneadas e a suíte fique verde. A-03 continua documentado, com sua correção de limpeza preservada e sem implementação da proposta de trajetória.
