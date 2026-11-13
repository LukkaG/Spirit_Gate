# ORG-03-R6 — ADR de políticas do personagem

## Estado e regra de interpretação

As diferenças abaixo não autorizam alterar produção. **APROVADA ANTERIORMENTE —
D01/D13:** preservar as regras atuais até decisão explícita. Cada recomendação
é do agente; a coluna de aprovação indica quem precisa decidir. Políticas de
tempo/efeitos continuam sem implementação. Ver [matriz central](ORG-03-R6-MATRIZ-DECISOES.md).

Fontes operacionais: `RpgProgression.award`, `QuestRewards.prepare`,
`RpgWorld.awardExperience/sharedReward/completeQuest/completeAuthoredQuest/use/equip/unequip`,
`QuestService.state/complete`, `AuthoredQuestSession`, `LootService.award/collect`,
`SharedRpgEncounter.hit` e `EconomicLaboratory.transition`, inspecionados no checkout.
Os caminhos RPG são relativos a `server/src/main/java/com/projetok/rpg/`; R5 fica
em `server/src/test/java/com/projetok/rpg/economy/`.

## Produção versus laboratório: decisões por diferença

| ID / estado | Regra atual do jogo | Regra R5 | Compatibilidade e risco de migração | Alternativas | Recomendação | Aprovação necessária |
| --- | --- | --- | --- | --- | --- | --- |
| P01 — PENDENTE DE APROVAÇÃO, D14 | RpgProgression trunca XP no teto da curva/limite técnico. Domínio recebe a quantidade nominal via award; XP efetiva pode ser menor. XP também pode aumentar HP pelo máximo de nível no chamador | Rejeita a operação inteira se XP nominal não couber; inclui domínio, mas não HP | Divergente: pode negar moedas/itens/maestria que hoje seriam concedidos no teto, ou perder HP se integração copiar só economia | Preservar truncamento e registrar nominal/efetivo; rejeitar tudo; adiar regra específica por origem | Para primeira integração candidata, conservar semântica atual por origem e registrar valores efetivos; não adotar recusa R5 automaticamente | Gameplay e economia; testar teto, domínio, HP e nível |
| P02 — PENDENTE DE APROVAÇÃO, D15 | Quest usa addExact e recusa além de 1.000.000 moedas. Morte/sharedReward satura moedas nesse teto após outras ações | Recusa integral por teto/overflow para todos os grants | Divergente: unificar muda a recompensa; passos atuais de mob não são transação durável | Manter diferenças explicitadas no recibo; unificar por recusa; unificar por saturação | Preservar diferenças inicialmente, com efeito autorizado nominal/efetivo e fronteira inteira de cada operação | Gameplay; aprovar teto e comportamento de cada origem |
| P03 — PENDENTE DE APROVAÇÃO, D16 | COMPLETED/quest-done impede nova entrega no ledger. Não existe contrato de instância repetível por login/versão | Deduplica por beneficiário + namespace + questId, sem reabrir por versão/GrantId | Compatível com entrega única; não cobre futuras quests repetíveis e pode bloquear repetição intencional | Não repetível; repetível com ciclo/instância de entrega autoritativos | Manter entrega única observada. Repetibilidade futura exige identidade de ciclo própria; versão não é ciclo | Conteúdo/gameplay; nenhum recibo retroativo |
| P04 — RECOMENDADA, D10/D18 | Builtin usa mapa/flags do mundo e QuestService; Plan prepara cópias, mas flag/estado são publicados em RAM pelo chamador | Entrega, recursos, revisão e recibo entram no mesmo CAS | Prova local compatível como objetivo, sem recibo durável nem integração atual | Persistir flag isolada; transação conjunta; reinterpretar flag como recibo | Transação conjunta futura; recusar reconstrução de histórico por flag | Arquitetura e autorização de integração durável |
| P05 — PENDENTE DE APROVAÇÃO, D16/D18 | Autoria tem ledger separado e QuestBook; versão do livro não prova versão de cada concessão. Pode haver contrato de reset e efeitos próprios | Usa namespace authored e definição válida; não cobre todo reset/estado do mundo | Apenas parte é compatível. Colidir namespaces ou presumir que book.version comprova entrega perde história | Versão de definição/contrato por missão; hash de conteúdo; migração autorada específica | Preservar separação builtin/authored e identificar definição exata. Validar pré-requisitos e efeitos extras antes de integrar | Conteúdo/gameplay e arquitetura |
| P06 — RECOMENDADA, D03/D10 | LootService resolve tabela/RNG, tenta inserir cópia e marca derrota por spawn/geração | Oferta resolve resultado antes do ledger; retry não recebe RNG | Compatível como meta; falta origem durável, não basta repetir a seed nem o evento visual | Persistir resultado resolvido; tentar RNG novamente; guardar somente seed | Persistir resultado autorizado, inclusive ausência de drop explicitamente resolvida; nunca rerrolar recuperação | Aprovar produtor da origem e fronteira durável |
| P07 — PENDENTE DE APROVAÇÃO, D17/D27 | Falta de espaço agrega pending por item/quantidade, sem vínculo transacional completo por origem | Direito conserva GrantId, recibo, fonte e pacote | Legado não pode ser convertido fielmente só pelo total agregado | A recusa integral; B não confirmado; C abertura auditada; novos direitos com origem íntegra | Preservar novos direitos com origem; manter A para legado sem prova | Responsável pelos dados + gameplay; B/C não ativadas |
| P08 — PENDENTE DE APROVAÇÃO, D17 | collect tenta o total de cada item e deixa outros itens pendentes se não couberem; não é coleta de uma fração arbitrária da quantidade de um item | Claim exige pacote inteiro, mesmo com múltiplos tipos de item | Divergente: jogador pode recolher menos ou ter tudo bloqueado na R5 | Atomicidade por item agregado; por direito; por pacote | Candidata: conservar coleta por item, mantendo internamente todas as origens. Somar direitos não pode apagar seus recibos; isso exige novo plano de claim, não é a API R5 pronta | Gameplay/economia; testar espaço parcial, múltiplas origens e conservação |
| P09 — PENDENTE DE APROVAÇÃO, D18 | InventoryService.remove/move é componente local; efeitos do comando dependem do chamador | Consume remove item e Move altera slots com recibo; não executam gameplay | Parcial: remover poção sem cura/cooldown ou registrar cada gesto como transação seria mudança não autorizada | Escopo por comando relevante; delta de bolsa separado com reconciliação; agregado ampliado | Definir operação semântica e todos os campos afetados antes de persistir, sem usar remoção como substituto do comando real | Gameplay + arquitetura; critérios de auditoria/retenção |
| P10 — PENDENTE DE APROVAÇÃO, D18 | Equip transfere uma unidade para slot e devolve anterior se couber; unequip depende de espaço. Sem UUID por exemplar | Equipment não existe no estado econômico R5 | Ausente: integrar bolsa sem slot equipado duplica/perde itens ou altera stats | Transferência conjunta bolsa/slot; estado canônico integral; identificador de exemplar só em feature própria | Preservar transferência atômica e stats coerentes; não inventar identidade de exemplar nesta fase | Gameplay/inventário e versão do estado |
| P11 — PENDENTE DE APROVAÇÃO, D18/D20 | use valida vida, consumível, espaço lógico/cooldown e estado; remove um item, cura e define potionReadyTick=ticks+60 | Consome item, sem HP, cura, vida ou deadline | Incompatível como substituto: pode cobrar sem curar ou permitir replay de cura | Transação/registro do comando com todos os efeitos; preparação retida e publicação confirmada | Recibo deve ligar custo, cura autorizada/efetiva e prazo, com owner coerente; não repetir HEAL visual ou regenerar na recuperação | Gameplay + política temporal e protocolo assíncrono |
| P12 — RECOMENDADA, D19 | SharedRpgEncounter verifica contribuidores vivos/na região e concede individualmente; falhas não formam commit global do grupo | CAS pode confirmar por beneficiário; teste entre dois preserva ambos, mas memória não sobrevive ao processo | Compatível com independência, sem prova de completude após crash entre A/B | Transação única do grupo; intenção durável de distribuição + commits individuais | Preferir intenção com elegíveis/resultados resolvidos e pagamentos individuais idempotentes; não anunciar grupo completo antes de reconciliar todos | Arquitetura/gameplay; origem global e falhas de distribuição |

Não há migração de valores nem escolha final por esta tabela. Nomes dos métodos
descrevem o código, não homologação física. Compra/venda, reset de domínio e
futuros efeitos econômicos exigem catálogo próprio de comandos antes de ampliar
o contrato; não foram convertidos em features pela R6.

## Tempo: domínios e envelope candidato

**RECOMENDADA — D20; BLOQUEADA POR EVIDÊNCIA — D21.**

| Valor | Fonte atual | Significado proposto para persistência |
| --- | --- | --- |
| WorldTick | RpgWorld.ticks/skillTick | Relógio daquele personagem; entrada tardia diverge da sessão |
| SessionTick | RpgSessionWorld.tick | Passos da sessão, com pausas; não converter por igualdade presumida |
| SkillEngineClock | RpgSkillEngine.clock | Clock global recebido do coordenador, distinto do deadline de ready[5] |
| Horário persistente | Não existe âncora homologada na captura | Observação de fonte de tempo aprovada, com incerteza/proveniência |
| Época de execução | R4 tem UUID efêmero, sem WorldEpoch durável | Identifica domínio do clock e sua continuidade/reinício, emitida pela autoridade |
| Origem/versão | Parcialmente representadas em R1 | Política e escala explícitas; não atribuir significado novo a um número antigo |

Envelope conceitual candidato: versão de política/codec, canal/SkillId/versão de
regra, relógio e época de origem, tick observado e deadline nativos, estado de
precisão/inicialização, âncora persistente e intervalo de incerteza, resultado
da verificação e referência da operação que consumiu o custo. A fonte temporal
não vem do cliente. Commit-time do worker não é automaticamente capture-time
do owner; atraso de fila e amostragem têm de ser demonstrados.

| Canal | Dado atual a preservar | Condição da proposta de recuperação |
| --- | --- | --- |
| Ataque básico | attackReadyTick e preciseAttackReady; double fracionário e possível NaN de precisão ainda não inicializada | Guardar ambos e modo/origem. NaN não significa pronto nem prova de ação nunca usada |
| Shunpo | abilityReadyTick | Prazo compartilhado real, sem reiniciar pela admissão |
| Poção | potionReadyTick | Preservar custo/HP autorizados; prazo não pode sumir nem duplicar cura |
| Viagem | portalReadyTick | Separar prazo de validade de spawn/conteúdo; não viajar automaticamente |
| Cinco skills | Session.ready, IDs reais do catálogo, worldTick de origem | Ausência de Session não é cinco prazos zero. EngineClock não substitui worldTick |

R3 preserva double nativo; R1 limita decimal a nove casas. Valores que não cabem
sem perda exigem decisão de codec/formato, não arredondamento oculto. Recomenda-se
conservar a representação nativa como evidência e exigir mapeador versionado
com teste de erro conservador, sem liberação antecipada. Não se propõe aqui uma
fórmula de ticks→timestamp, tipo físico final ou implementação de expiração.

### Alternativas temporais e condições

**RECOMENDADA — D20:** manter a candidata original de recargas transcorrerem
desconectado, somente após homologação de D21. Alternativas: congelar restante
durante ausência; política distinta por canal. Ambas mudam experiência e
precisam de aprovação explícita. Nenhuma é fallback automático.

| Situação | Resultado exigido da política aprovada | Evidência futura |
| --- | --- | --- |
| Reconexão imediata | Mesmo prazo lógico; não reiniciar duração inteira nem zerar | Restante fracionário e mesma operação, antes/depois da admissão |
| Parcialmente transcorrido | Preservar restante coerente com origem/âncora/política | Atraso da fila, amostras e precisão em E2/E3 |
| Expirado comprovadamente | Pronto sem novo custo, recarga ou recurso | Fonte confiável e prova de expiração, não mera ausência de Session |
| Processo reiniciado | Nova época não compara diretamente nanoTime/ticks antigos | Âncora/codec duráveis e relógio do novo processo validados |
| Clock do sistema voltou ou saltou adiante | Detectar ambiguidade; não expirar por salto nem fabricar tempo negativo | Ensaios de ajuste e falha da fonte temporal |
| Clock/prazo impossível de verificar | Reter candidato e recusar/diferir ativação conforme contingência aprovada | Diagnóstico explícito; nenhum zero por conveniência |
| Retry/callback atrasado | Não deslocar âncora para o novo login e prolongar/reduzir a recarga indevidamente | Idempotência temporal com o mesmo recibo/versão |

Um timestamp do banco não é uma prova completa de clock monotônico. As funções
de tempo também têm pontos de amostragem diferentes: transaction_timestamp se
refere ao início da transação; clock_timestamp observa o instante da chamada.
Escolher uma delas não homologa a conversão owner→tempo durável. [Documentação
oficial de data/hora](https://www.postgresql.org/docs/18/functions-datetime.html#FUNCTIONS-DATETIME-CURRENT).

**RECOMENDADA — D20/D29:** preservar HP, energia absoluta e fração, sem cura ou
regeneração offline automática; morte permanece morte. Isso é proposta de
recuperação, não modificação de returnHome/respawn atuais. Recalcular máximos
não permite clamp silencioso que apague diferença de regra.

## Penalidades e estado transitório

**PENDENTE DE APROVAÇÃO — D22.** O read model conhece timers ROOT/STUN/SLOW de
RpgWorld, mas os testes os aplicam em laboratório; não provam que todos os
efeitos de combate estejam exportados para o jogador. BLEED/BURN atuais do
engine têm execução Dot voltada a Enemy, com alvo/geração e world.skillTick.
Não inventar ledger de DoT do personagem a partir disso. Efeitos externos
continuam desconhecidos no read model R3/R4.

| Efeito | Alternativas candidatas | Risco | Recomendação e aprovação |
| --- | --- | --- | --- |
| ROOT | Expirar por tempo confiável; congelar restante offline | Limpar por desconexão ou prender por duração nova indevida | Preservar descritor/origem/prazo; aprovar regra offline e D21 antes de ativar |
| STUN | Mesmas alternativas, incluindo bloqueio de ações correto | Login pode remover penalidade de ataque além de movimento | Não usar só blocked genérico; gameplay decide duração/efeitos |
| SLOW | Preservar escala e duração; expiração ou congelamento aprovados | Restaurar escala padrão elimina penalidade; escala antiga pode conflitar com regra nova | Capturar intensidade e versão; sem normalização silenciosa |
| BLEED | Suspender duração/penalidade; transcorrer offline com regra explícita; retenção até evidência completa | Dano offline, morte e compensação não são decisões de serialização | Não restaurar executor nem aplicar pulsos atrasados; bloquear ativação incompleta até política de gameplay |
| BURN | Alternativas de BLEED, avaliadas separadamente se a regra diferir | Burst de dano no login ou limpeza gratuita | Mesma proteção; nenhuma equiparação automática ao ROOT |
| Efeito futuro/desconhecido | Registro versionado e adaptador aprovado; retenção/quarentena | Omitir campo para declarar ausência | UNKNOWN não é ASSESSED/vazio; novo tipo exige política e testes |

Preservar descriptor não executa dano. Não há aprovação de dano offline,
deferred damage, compensação de XP ou vencimento automático dessas penalidades.
Desconectar não pode virar limpeza vantajosa, mas evitar esse abuso não autoriza
inventar uma punição nova. Sem fonte/política suficientes, a recuperação fica
bloqueada, mantendo o estado retido e diagnóstico.

**APROVADA ANTERIORMENTE — D23:** não restaurar projéteis, dash em execução,
intangibilidade, sentinela ativa, marcas/checkpoints temporários, comandos,
alvos/mira mantidos ou eventos/animações antigos. Exclusão desses executores não
apaga custos, prazos ou descritores adversos. Não repetir impactos para liquidar
uma habilidade; concessão já confirmada é consultada por recibo.

## Legado e A-04

**APROVADA ANTERIORMENTE — D02/D30:** manter A como restrição provisória de
recuperação integral; não ativar timeout de 92 ticks ou desconexão automática.
**RECOMENDADA — D27:** considerar C somente como migração futura auditada.
B continua **PENDENTE DE APROVAÇÃO**. Nenhuma flag vira recibo retroativo.

| Alternativa | Integridade/risco | Impacto nos jogadores experimentais | Condições de implementação |
| --- | --- | --- | --- |
| A — Recusar integral sem prova | Evita inventar economia; progresso não é apagado | Não oferece recuperação integral por esse mecanismo | Explicar limitação, preservar originais e permitir decisão/atendimento; não reclassificar como perdido |
| B — Legado não confirmado | Conserva observações; risco de misturar com estado comprovado | Pode exigir uso restrito/quarentena e revisão manual | Marca de proveniência, isolamento, regras de uso e vedação de ativação por omissão |
| C — Marco administrativo | Nova abertura autorizada, não reconstrução do passado | Exige conferir divergências e permitir contestação | Inventário auditado, backups verificados, responsável, aceite, evidência independente, marco/revisão e reversão localizada |

A-04 é um requisito de recuperação futura, não uma implementação nesta R6:
capturar antes de remove/leave, preservar custos/deadlines e estado confirmado,
excluir a fase de Quatro relâmpagos e validar apoio físico na revisão atual.
Se o spawn/posição estiver ocupado ou inválido, reter sem corpo ativo e sem
teleporte arbitrário, refund ou reset. Reserva/callback precisam de geração e
fence atuais; o ledger não pode repetir impactos, fabricar XP/itens ou ocultar
commit confirmado. Provas constam de F13/F22 e dos gates, ainda não executadas.
