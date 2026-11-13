# ORG-03 — Matriz do estado de Player

## Como ler

Auditoria do checkout `beco@13b91dc`, incluindo alterações locais anteriores.
Tipo/nome/proprietário descrevem **o código atual**. Persistência e recuperação
descrevem **proposta futura**, sem implementação. `D`: durável proposto;
`S`: sessão; `E`: efêmero; `W`: mundo. `R`: record/campo final, substituído como
unidade; `M`: mutável no owner; `F`: final; `V`: volatile/sincronizado no transporte.
Risco alto significa ausência de garantia durável, não bug reproduzido.

Proprietários/âncoras de leitura, relativos a `server/src/main/java/com/projetok/`:

| Sigla | Caminho e símbolos reais |
| --- | --- |
| CI | [rpg/CharacterIdentity.java](../server/src/main/java/com/projetok/rpg/CharacterIdentity.java): record/`create` |
| RW | [rpg/RpgWorld.java](../server/src/main/java/com/projetok/rpg/RpgWorld.java): campos, `manualAttributes`, `save`, `restore`, `networkState`, `forkSyntheticRevision` |
| DP | [rpg/DomainProgression.java](../server/src/main/java/com/projetok/rpg/DomainProgression.java): `choose`, `award`, `read`, `validateFor` |
| RP | [rpg/RpgProgression.java](../server/src/main/java/com/projetok/rpg/RpgProgression.java): curva/`level` |
| RS | [multiplayer/RpgSessionWorld.java](../server/src/main/java/com/projetok/multiplayer/RpgSessionWorld.java): `Session`, `joinCharacter`, `leave`, `prepareRevision` |
| LS | [multiplayer/LocalSimulationServer.java](../server/src/main/java/com/projetok/multiplayer/LocalSimulationServer.java): `ids`, `Peer`, owner/filas |
| LC | [multiplayer/LocalClient.java](../server/src/main/java/com/projetok/multiplayer/LocalClient.java): caches/`reconnect` |
| RE | [rpg/RpgSkillEngine.java](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java): `Session`, `Pending`, `state`, `remove` |
| IS | [rpg/InventoryService.java](../server/src/main/java/com/projetok/rpg/InventoryService.java): slots/`restore` |
| QS | [rpg/QuestService.java](../server/src/main/java/com/projetok/rpg/QuestService.java): `state`, `event`, `complete` |
| AQ | [rpg/AuthoredQuestSession.java](../server/src/main/java/com/projetok/rpg/AuthoredQuestSession.java): `progress`, `flags`, `copy` |
| SE | [rpg/SharedRpgEncounter.java](../server/src/main/java/com/projetok/rpg/SharedRpgEncounter.java): `players`, `participants`, `hit`, `leave` |
| RC | [multiplayer/RevisionCoordinator.java](../server/src/main/java/com/projetok/multiplayer/RevisionCoordinator.java): `sequence`, `Client`, transações de conteúdo |
| SO | [rpg/SyntheticOnlineRepository.java](../server/src/main/java/com/projetok/rpg/SyntheticOnlineRepository.java): `Document`, `Grant`, CAS |

## Identidade e revisões

| Nome e tipo atual | Proprietário | Origem | Validação autoritativa atual | Mutabilidade | Escopo | Persistir? | Critério de recuperação proposto | Duplicação | Dependências |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `id: UUID` | CI | `randomUUID` local; `nameUUIDFromBytes(instance:id)` online | Não nulo; não autentica proprietário | R | personagem local / S online | D, substituindo a origem online futura | Mesmo ID estável autorizado, nunca derivado do novo peer | Alto se confundido com posse | CI, RS, autorização futura |
| `id: int` | LS.Peer / RS mapa / GameEntity | `AtomicInteger.incrementAndGet` por servidor | LS/RS: 1..1000000; no máximo dois; GameEntity isolado não valida faixa | F / chave | conexão e participação | Não | Novo ID; não importar antigo | Alto se reutilizado entre épocas | LS, RS, Protocol.Welcome |
| `body.id: int = 1` | RW.GameEntity interno | Construtor do mundo individual | ID interno, não identificador global de ator | F | instância RW | Não | Recriar corpo com mapeamento novo | Alto ao exportar como personagem | RS.proxy, colisão |
| `instance: UUID` | RS | `randomUUID` por mundo | Interno ao servidor | F | W | Não como personagem | Nova época; comparar contexto | Alto se ignorar restart | UUID de admissão |
| `name: String` | CI / GameEntity / seleção | Seleção normalizada NFC | CI 3..24; Hello e seleção validam; nomes iguais permitidos | R/F | personagem/apresentação | D | Restaurar valor validado, sem usar como login | Alto se chave de dono | CharacterIdentity, SessionCharacterSelection |
| `archetype: String`, `race()` | CI | Catálogo CharacterRaces; campo legado expõe raça | Normalização legada e catálogo | R | personagem | D | Raça canônica compatível com domínio | Médio em migração | CharacterRaces, Domains |
| `appearance: String` | CI | `azure`/`amber`; online cria `azure` | CI limita os dois valores | R | personagem | D | Não confundir com hair/clothing | Médio | CI |
| `appearance: CosmeticCatalog.Selection` | RS.Session | Criação/AppearanceRequest | Catálogo e restrições de raça/aplicação | M | S, futuro personagem | D se aprovado | IDs cosméticos atuais válidos | Baixo | catálogo cosmético |
| `initialRegion: String` | CI | `village` | CI exige village | R | definição inicial | D/metadado | Não substituir região atual por inicial | Médio | RW.region |
| `baseHp/baseAttack: int` | CI | CharacterRaces no create | CI faixas 1..1000/1..100 | R | personagem/base | Revalidar/rederivar por versão | Não aceitar base arbitrária do cliente | Alto por inflação | catálogo/versão de regras |
| `domain: String` | DP em RW | Escolha de domínio autoritativa | Nível >=15, raça compatível, escolha única | R/M | personagem | D | Preservar e revalidar | Alto se reset implícito | XP, Domains |
| `application: int` | RS.Session/LS.Peer | Hello negociado | Suporte real; aplicações 6/7 distintas | M | S/conexão | Não | Nova negociação, sem alteração wire | Baixo | Protocol |
| `sequence: long` | RS.Session / LC | Comandos sequenciados | Rejeita `<=` anterior; limite oito comandos/tick | M | S | Não | Nova sequência sob nova geração | Alto para replay | intents/gameplay/skill |
| `targetGeneration: long` | RS.Session | Geração atual do mob | Alvo vivo/região/geração em skill/targetAction | M | S | Não | Alvo limpo; não é geração do personagem | Alto se confundido | EnemyService |
| `content.hash`, `number`, `RC.sequence: long` | RS/LiveRevision/RC | Conteúdo e transação de sincronização | Hash e revisão verificados na admissão/comandos | M | W/conteúdo | D como compatibilidade | Recusar ou migrar explicitamente divergência | Alto se tratado como revisão do save | RC, WorldContent |
| `revision: long` | SO.Document | CAS sintético por arquivo | Revisão esperada, lock e integridade | R | documento de laboratório | Referência, não contrato online | Não confundir com hash de conteúdo | Alto se sobrescrita antiga | SO, payload RpgSave |
| Conta, dono, geração de admissão durável | **Ausentes no runtime RPG** | Apenas propostas/lab R3 | Não existe autenticação online correspondente | — | futuro personagem/posse | D em contratos futuros | Autorização separada de UUID e fencing | Alto | ADR/contas futuras |

## Progressão, atributos e desbloqueio

| Nome e tipo atual | Proprietário | Origem | Validação autoritativa atual | Mutabilidade | Escopo | Persistir? | Critério de recuperação proposto | Duplicação | Dependências |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `xp: long` | RW | Recompensas; início sintético nível 1/20 | RP faixa 0..1000000, teto técnico; prêmio validado | M | personagem | D | Substituir total, nunca conceder XP de novo | Alto | RP, recibos |
| `level(): int` | RW/RP | Derivado de XP e curva | `RP.level` | derivado | personagem | Não como autoridade paralela | Recalcular da XP/versão; não aplicar level-up novamente | Alto se reconceder pontos | progression.properties |
| `xpHalfUnits: long` | DP | XP premiada após domínio | Não negativo; `Math.addExact`; none exige zero | R | personagem | D | Inteiro exato, não double arredondado | Alto | domínio/recompensa |
| `lastReset: LocalDate` | DP | Reset autorizado | Intervalo mensal/política em `reset`; `read` valida data | R | personagem | D opcional | Nulo significa nunca, sem inventar reset | Médio | carteira futura/política |
| `resetQuestComplete: boolean` | DP | Contrato de reset | Fluxo/política específica; pagamento não integrado | R | personagem | D | Preservar prova de conclusão com recibo | Alto | QuestBook.resetContract |
| `manualStatsEnabled: boolean` | RW | `manualAttributes()` sintético | Somente mundo sintético; habilita lazy | M | S atual | D como modo de regras futuro | Não desligar ao restaurar e zerar investimentos | Alto | StatsComponent |
| `manualStats.strength: int` | RW.StatsComponent | Gasto de ponto no owner | `allocateAttribute`/`spendOnStrength`; construtor não valida STR negativa | R/M | personagem em S | D | Validar não negativo e orçamento total futuro | Alto | LevelingRules, bônus |
| `manualStats.intelligence: int` | RW.StatsComponent | Gasto de ponto | Mesma operação; construtor não valida INT negativa | R/M | personagem em S | D | Preservar investimento, validar orçamento | Alto | AttributeCombatRules |
| `manualStats.agility: int` | RW.StatsComponent | Gasto de ponto | Mesma operação; construtor não valida AGI negativa | R/M | personagem em S | D | Preservar; não resetar precisão de recarga | Alto | ataque/esquiva |
| `manualStats.energy: int` | RW.StatsComponent | Gasto de ponto | Record exige 0..10000 | R/M | personagem em S | D | Preservar antes de calcular energia máxima | Alto | EnergyRules |
| `manualStats.level: int` | RW.StatsComponent | Sincronização lazy com level() | Record >=1; while concede por nível | R/M | personagem em S | D/checkpoint de alocação | Validar sincronização sem reconceder pontos | Alto | manualAttributes |
| `manualStats.unspentPoints: int` | RW.StatsComponent | LevelingRules | >=0, spend exige saldo; nível 20 dá 71 no RPG sintético | R/M | personagem em S | D | Preservar orçamento e versão da regra | Alto | sem crescimento racial automático no RPG |
| Nível/domínio/raça para skills | RE.cast / QuestBook.Draft | Catálogos e estado do personagem | Executores reais validam requisitos | derivado | personagem/regra | Não persistir bool de desbloqueio | Recalcular pelos catálogos | Alto se confiar no cliente | RpgSkillCatalog, CharacterRaces |
| Ataque/defesa/HP máximo | RW métodos | Base + nível + itens/atributos | Funções de produção | derivado | personagem/regra | Só valores diagnósticos opcionais | Recalcular; discrepância recusa/migração | Médio | itens/regras |

## Combate e recursos

| Nome e tipo atual | Proprietário | Origem | Validação autoritativa atual | Mutabilidade | Escopo | Persistir? | Critério de recuperação proposto | Duplicação | Dependências |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `hp: int` | RW | Dano/cura/recompensa/respawn | Métodos de combate; restore <=maxHp | M | personagem | D recuperável | Valor capturado; zero continua morto, sem cura no login | Alto | regras de HP |
| `stamina: int` | RW | Energia absoluta; inicial 1000 | EnergyRules/custo; save legado normaliza 0..1000 | M | personagem/recurso | D | Valor absoluto, não fração arredondada | Alto | atributos Energy |
| `energyRegenerationRemainder: double` | RW | Acúmulo fracionário no tick | Cálculo do owner, zera no máximo | M | execução de recurso | Condicional no contrato exato | Finito/coerente; sem ganho gratuito por arredondamento | Médio | regen/máximo |
| `maximumEnergy(): int` | RW | EnergyRules(trainedEnergy) | Regra de produção | derivado | personagem | Apenas verificação | Recalcular antes de validar saldo | Alto | atributos |
| `ready: long[5]` | RE.Session | Fim de recarga `now + cooldownTicks` | RE.cast verifica prazo | M | S atual, D proposto | D deadlines, não execução | Capturar antes de remove; transcorrer desconectado | Alto | relógio autoritativo |
| `attackReadyTick: long`, `preciseAttackReady: double` | RW | Ataque básico/AGI | Bloqueio por tick e próxima recarga fracionária | M | S atual | D prazo efetivo/precisão | Não arredondar para liberar antes | Alto | AttributeCombatRules |
| `abilityReadyTick: long` | RW | Shunpo | `shunpoReady` | M | S atual/save flags | D prazo | Expiração autoritativa, sem reset | Alto | custo/energia |
| `potionReadyTick: long` | RW | Uso de consumível | Vida/estoque/tick | M | S atual/save flags | D prazo | Não restaurar item consumido | Alto | inventário/HP |
| `portalReadyTick: long` | RW | Viagem | Fluxo de travel/tick | M | S atual/save flags | D prazo | Preservar restrição de viagem | Médio | região |
| `evadeUntilTick: long` | RW | Esquiva defensiva | `receiveDamage` consulta prazo | M | E benefício temporário | Não restaurar execução | Sem nova imunidade; custo/recarga continuam | Alto | política de efeitos |
| `skillRootUntil/skillStunUntil/skillSlowUntil: long`, `skillSlowScale: float` | RW | Efeitos no combate | Métodos de skill/tick | M | E/restrição | Não copiar campos; política pendente | Descritor restritivo limitado ou aguardar fim; não limpar debuff vantajosamente | Alto | contrato de efeitos |
| `skillDashing/skillSentry/skillIntangible: boolean` | RW | Executor de técnica | Somente sintético/executor | M | E | Nunca como estado restaurável | Iniciar sem dash/sentinela/intangibilidade | Alto/A-04 | apoio físico válido |
| `sentryShots: int`, `sentryUntil/checkpointUntil: long`, `sentryReady: double`, `checkpoints: Map<Integer,WindMark>` | RE.Session | Execução de skills | Executor real | M | E | Não | Descartar marcas, tiros e buffers; manter recargas/custos | Alto | RE, mundo |
| `pending: List<Pending>`, `dots: Map<String,Dot>` | RE | Projéteis/áreas/efeitos | Validações de execução/geração | M | E/W | Não | Não reaplicar impacto ou DoT antigo | Alto | vítimas/gerações |
| `events`, `eventSequence/sequence: long` | RW/RE | Combate/renderização | Buffers limitados, sequência do owner | M | E | Não como replay | Novos buffers; recibos econômicos são separados | Alto | observadores |
| Skill/botão habilitado | UI/derivado do estado | Snapshot privado e regras | Servidor revalida cast | derivado | apresentação | Não | Recalcular; nunca recuperar enabled=true | Alto | energia/alvo/nível/CD |

## Inventário e economia

| Nome e tipo atual | Proprietário | Origem | Validação autoritativa atual | Mutabilidade | Escopo | Persistir? | Critério de recuperação proposto | Duplicação | Dependências |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `slots: Stack[]`, `Stack(String id,int quantity)` | IS em RW | Loot/compra/uso/troca local | Catálogo, capacidade, quantidade e maxStack nas operações | M; Stack R | personagem | D | Captura consistente; cada slot no máximo uma entrada | Alto | ItemCatalog |
| `capacity: int` implícita no array | IS | RW cria 20; serviço aceita 1..100 | Construtor e restore | F | personagem/configuração | Versão/capacidade no contrato | Não ampliar para acomodar snapshot inválido | Médio | slots |
| `Map<String,Integer>` chaves `slot:itemId` | IS.snapshot/RpgSave | Export dos slots | restore detecta slots repetidos inclusive IDs diferentes | R | documento local | Adaptar para entradas tipadas | Validar antes de construir mapa que oculte duplicatas | Alto | formato V2 |
| `equipment: Map<String,String>` | RW | equip/unequip sobre cópia | Categoria, nível, slot e estoque; restore checa slot | M | personagem | D | Substituir junto com inventário, nunca equip() de novo | Alto | quatro slots RPG |
| `coins: int` | RW | Loot/quest/transação | Save 0..1000000; QuestRewards usa addExact | M | personagem | D | Total e recibos no mesmo commit | Alto | moedas não são cash |
| `pending:item:quantity` | RW.flags / LootService | Falta de espaço no loot | limite 9999 por item; coleta remove flag após add | M | direito econômico do personagem | D | Preservar pendência uma vez, sem reexecutar loot | Alto | slots/recibos |
| `defeat:spawn:generation` | RW.flags / LootService | Ledger de drop | Rejeita geração <=última | M | personagem/contexto do mundo | D como recibo contextual futuro | Incluir época/spawn/geração; não confundir mundos | Alto | RNG/encontro |
| `cashBalance` de reset | Parâmetro DP.reset, não carteira RW | Serviço futuro não integrado | Regra pura valida saldo/política | não armazenado em RW | economia futura | Não inventar saldo atual | Integração separada; não converter coins em cash | Alto | autorização de produto |

## Missões

| Nome e tipo atual | Proprietário | Origem | Validação autoritativa atual | Mutabilidade | Escopo | Persistir? | Critério de recuperação proposto | Duplicação | Dependências |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `quests: Map<String,Integer>` | RW; AQ.progress separado | Aceitação e progresso agregado | QS.accept/state; limites no save | M | personagem | D | Ausente= não aceita; preservar objetivos, não só total | Alto | catálogo builtin/autorado |
| `qo:quest:index:count` | RW.flags/AQ.flags | Eventos kill/talk/collect/visit | QS limita ao objetivo; restore valida índices builtin | M | personagem | D | ID/versão do objetivo; não reemitir evento | Alto | QuestBook/Definition |
| `quest-done:quest` | RW.flags/AQ.flags | QS.complete em READY | Completa uma vez; save verifica progresso builtin | M | personagem | D | Junto da recompensa/recibo, sem chamada complete | Alto | QuestRewards |
| `qe:type:source:sequence` | RW.flags/AQ.flags | Ledger de eventos | QS ignora sequência antiga por origem/tipo | M | personagem/contexto | D/adaptado | Namespace de mundo/evento estável | Alto | mortes/visitas/diálogo |
| `State AVAILABLE/ACTIVE/READY/COMPLETED/UNAVAILABLE` | QS.View | Derivado de flags/progresso/pré-requisitos | QS.state | derivado | apresentação | Não como autoridade independente | Revalidar, diferenciar entregue de recompensa confirmada | Alto | definição/pré-requisitos |
| `book: QuestBook`, `drafts`, `version: int` | AQ/QuestBook | Conteúdo autorado | requireValid; version=1; até64 quests | F/R | W/definição | Referência/versionamento | Não serializar definições com o personagem | Médio | QuestDocuments |
| Recompensa `Plan(inventory,coins,xp,domain)` | QuestRewards | Plano inteiro pré-validado | Cópias, limites e addExact | R com inventário candidato | transição | Persistir resultado+recibo | Não reexecutar prepare/concessão ao restaurar | Alto | estoque/XP/domínio |
| Recibo econômico durável próprio | Ausente no runtime online | Flags locais e lab R3 não equivalem ao futuro banco | Sem transação online durável atual | — | futuro personagem | D obrigatório | Chave única de concessão e resultado atômico | Alto | plano de persistência |

## Mundo, participação e conexão

| Nome e tipo atual | Proprietário | Origem | Validação autoritativa atual | Mutabilidade | Escopo | Persistir? | Critério de recuperação proposto | Duplicação | Dependências |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `region: String` | RW | Spawn/travel | Catálogo/cenário e portais | M | localização do personagem | D | Revisão compatível e região existente | Alto | RpgSceneSet |
| `body.position: Position(float x,float y)` | RW.GameEntity | Spawn/movimento | CollisionService, limites e corpos | M | localização | D candidata | Revalidar geometria e ocupação ao ativar | Alto | não é autorização |
| `home: Position`, `seat: int` | RS.Session | Admissão e revisão | Spawn escolhido e espaço livre; dois seats | M/F | S | Não como identidade; referência de spawn futura | Spawn canônico validado; sem confiar no seat antigo | Médio | conteúdo/colisão |
| `ticks: long` | RW/RS | Tick da simulação | Owner; save limita ticks | M | relógio local S/W | Não como relógio durável isolado | Época+referência confiável; restart não reinicia CD | Alto | prazos |
| `x/y/fx/fy: float`, `inputTick: long` | RS.Session | Entrada validada | Sequência/vetor/tick | M | S/E | Não | Movimento pendente zerado; facing é opcional validado | Médio | input |
| `target/allyTarget: int` | RS.Session | SELECT_TARGET | Vida/região/geração; aliado não vira vítima | M | S | Não | Sem alvo herdado | Alto | atores atuais |
| `participants: Map<String,Set<Integer>>` | SE | Contribuição por spawn:geração | hit só de participante atual, recompensa vivos na região | M | W | Não copiar no personagem | Direito pendente exige política/recibo estável próprio | Alto | IDs de participação |
| `players`, população, gerações de mobs | SE/EnemyService | Instâncias do encontro | Owner/geração/catálogo | M | W | Não no snapshot individual | Mundo mantém sua própria autoridade | Alto | mobs/IA |
| `visited/opened/object-until`, dungeon/boss flags | RW.flags | Exploração e objetos offline | validateFlags/catálogo | M | campanha local / W compartilhado | Classificar por campanha | Só flags de personagem explicitamente aprovadas; não restaurar mundo | Alto | objetos/encontros |
| Socket, reader/writer, filas, semáforo | LS.Peer/LC | Transporte atual | Fechamento CAS; capacidade/filas limitadas | F/V/M | conexão | Nunca | Nova conexão, filas vazias | Alto | comandos antigos |
| `playerSheet/skillState/attributeState/energyState/state` | LC | Snapshots recebidos | Filtra revisão/tick/sequência; cache não é autoridade | V | apresentação/conexão | Nunca como fonte de recuperação | Servidor publica novos snapshots após admissão | Alto | protocolo 6/7 |
| `facingX/facingY: float` | RW | Movimento/combate | Save valida vetor finito unitário | M | orientação | D opcional | Restaurar somente orientação válida, sem input | Baixo | save flag facing |
| `activeNpc/dialogueNode/message: String`, `transitioning: boolean` | RW | Interação e viagem no owner | NPC próximo/estado e fluxo de transição | M | S/E | Não | Diálogo fechado, mensagem nova, nenhuma viagem pendente | Médio | NpcService/DialogueService |
| `physicalRadius: float`, `sessionProfile`, `contentFork: boolean` | RW | Configuração sintética/revisão | Configuração antes da simulação, CollisionService | M/F por etapa | W/S | Referência de versão, não objetos | Perfil atual validado pelo servidor | Alto | regras físicas |
| `scenes/space/enemies/sharedEncounter/npcs/portals/objects` | RW | Catálogos/cenário e encontro | Construção/validação de conteúdo atual | F/M | W/serviços | Não no personagem | Reconstruir referências ao mundo atual | Alto | revisão/mundo |
| `random/evasionRandom: RandomGenerator` | RW | Fonte interna/fixture | Uso restrito de cálculo autoritativo | M | E | Não como objeto Java | Não rerolar prêmio já resolvido; usar recibo/resultado | Alto | loot/esquiva |

## Modelo por componentes e limites do inventário

`Player` herda `GameEntity(id:int,name:String,position:Position,components:Map)`
e guarda `connection:PlayerConnection` final e não nula. Seus dados são delegados:

| Nome e tipo atual | Proprietário | Origem | Validação autoritativa atual | Mutabilidade | Escopo | Persistir? | Critério de recuperação proposto | Duplicação | Dependências |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `connection: PlayerConnection` | Player | PlayerFactory/chamador | Objects.requireNonNull; interface não é login | F | conexão | Não | Adaptador futuro, não serializar Player inteiro | Alto | Coop/debug/demo |
| `StatsComponent` | Player.components | PlayerArchetype/factory/leveling | Regras de gasto; não equivalente a XP de RW | R | personagem do modelo | Só por adaptador explícito | Não misturar curva/base racial com RPG | Alto | Vitality/Leveling |
| `currentHealth/maxHealth: int` | HealthComponent | Factory/Vitality/dano | Max>=1, dano>=0, limite de cura | M | personagem | Atual; máximo derivado | Preservar dano, não usar constructor para curar | Alto | VitalityService |
| `current/maximum: float` | StaminaComponent | Inicial100, factory | Finitos, saldo/custo válidos | M/F | personagem do modelo | Conforme adaptador | Não confundir escala100 com energia RPG1000+ | Alto | serviço de vigor |
| `items: List<Item>` | InventoryComponent | Factory/add/remove | Container simples, sem slots/maxStack RPG | M | personagem do modelo | Não exportar como IS sem adaptação | Definir conversão e equivalência antes de migrar | Alto | domain.item.Item |
| `equipped: EnumMap<EquipmentSlot,Item>` | EquipmentComponent | Factory/EquipmentService | Container faz put; serviço delega e recalcula HP, sem validar posse/compatibilidade do item | M | personagem do modelo | Por adaptador futuro | Exigir validação explícita; não confundir enum com slots String RPG | Alto | EquipmentService |
| `style: CombatStyle` | CombatComponent | Factory ou setStyle | Fallback UnarmedStrike não nulo | M | personagem/modelo | ID de definição, nunca objeto executável | Adaptador explícito; não é o domínio RPG | Médio | combate por componentes |
| `race: Race` | RaceComponent | PlayerArchetype/RaceRegistry | Factory recebe archetype; record isolado não valida conta | R | personagem/modelo | Referência de catálogo | Não mapear automaticamente enum/objeto para CharacterRaces | Médio | RaceRegistry |

Não há cadastro de conta no Player. Seu Javadoc descreve conexão real, mas o
CoopWorld injeta um PlayerConnection sem efeitos. Comentário não é autenticação.
O corpo interno de RW, o proxy de RS e Player têm funções diferentes; seus ints
não são UUIDs duráveis. A matriz não propõe fundir esses modelos nesta tarefa.
