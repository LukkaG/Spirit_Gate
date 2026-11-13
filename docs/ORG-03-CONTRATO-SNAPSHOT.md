# ORG-03 — Contrato proposto de snapshot recuperável

## Natureza e autoridade

**Especificação conceitual, sem DTO Java novo, serializador ou importação no runtime.**
Nome candidato: `RecoverableCharacterSnapshot`. Versão de formato própria;
não é `RpgSave` V2, `SyntheticOnlineRepository.Document` V1, snapshot wire ou
envelope R3. Um formato futuro inicial pode usar versão 1, sem atribuir versão
de protocolo. Sem Swing, sockets, PlayerConnection, renderização ou RpgWorld.

O servidor captura/valida e recupera; o cliente apenas solicita operações
autorizadas. Estado recebido de cliente/arquivo pessoal não vira autoridade
online. Propriedade da conta/claim é verificada em contexto separado do DTO;
o snapshot não contém segredo, grant, handle, socket ou token de recuperação.

## Estrutura candidata

Obrigatório significa necessário para o contrato completo futuro, não campo já
exportável. Coleção obrigatória vazia é válida quando não há estado; ausência
de bloco obrigatório é erro. Zero de recarga deve ser explícito e validado,
nunca default para um campo faltante.

| Bloco/campo proposto e tipo lógico Java | Obrigação | Origem/critério |
| --- | --- | --- |
| `schemaVersion: int` | Obrigatório | Versão conhecida; incompatível recusa sem mutação |
| `characterId: UUID` | Obrigatório | Identidade estável, autorizada no contexto |
| `stateRevision: long` | Obrigatório | Monotônica, não negativa/sem overflow; não é revisão de conteúdo |
| `rulesVersion: String`, `contentHash: String` | Obrigatório | Catálogos/regras usados; divergência exige migração aprovada |
| `capture: {worldEpoch, sourceTick, capturedAt, clockPolicyVersion}` | Obrigatório | Fonte/época e tempo autoritativos; nenhum relógio do cliente |
| `identity: {name, race, appearance, cosmetics}` | Obrigatório | Normalização e catálogos existentes; cosmetics vazios válidos |
| `progress: {xp: long, domain: String, domainXpHalfUnits: long}` | Obrigatório | Valores reais; nível derivado, nenhuma nova concessão |
| `domainReset: {lastReset: LocalDate?, resetQuestComplete: boolean}` | Obrigatório; data opcional | Nulo significa nunca resetado; não implica pagamento integrado |
| `attributes: {mode, strength, intelligence, agility, energy, levelCheckpoint, unspentPoints}` | Obrigatório | Investimento completo, sincronizado com XP e orçamento da regra |
| `inventory: {capacity: int, entries: List<Slot>}` | Obrigatório | Slot contém `index:int,itemId:String,quantity:int`; cópia imutável |
| `equipment: Map<String,String>` | Obrigatório | Slots RPG canônicos; itens fora da bolsa quando equipados |
| `wallet: {coins: int}` | Obrigatório | Saldo real; não inclui cash inexistente |
| `quests: List<QuestProgress>` | Obrigatório | ID, namespace builtin/autorado, versão/hash da definição, aceitação, objetivos por ID/índice, entrega e vínculo ao recibo |
| `receipts: List<RewardReceipt>` | Obrigatório | Concessão estável, tipo/origem/época/versão/beneficiário, revisão confirmada e referência ao resultado |
| `questEventWatermarks: List<EventWatermark>` | Obrigatório | Sequências autoritativas por fonte/época para não reaplicar eventos |
| `pendingLoot: List<PendingEntitlement>` | Obrigatório | Item/quantidade e recibo de origem; preserva direito sem gerar drop outra vez |
| `resources: {hp:int, energyAbsolute:int, regenerationRemainder:double, lifeState}` | Obrigatório | Saldo exato; morte preservada, sem refill; resto finito validado |
| `cooldowns: {policyVersion, deadlines: List<CooldownDeadline>}` | Obrigatório | Todos os canais exigidos pela raça/domínio/regras, inclusive os prontos |
| `location: {regionId:String,x:float,y:float,spawnRef:String?}` | Obrigatório; spawnRef opcional | Posição candidata, não garantia de espaço livre |
| `recoveryRestrictions: List<Restriction>` | Obrigatório | Vazio só se comprovadamente sem restrição; efeitos adversos não somem por omissão |
| `campaignFacts: Map<String,typedValue>` | Opcional/versionado | Apenas fatos de personagem aprovados; ausência tem semântica explícita |
| `derivedChecks: {level,maxHp,maxEnergy,...}` | Opcional, diagnóstico | Recalcular; nunca fonte paralela de autoridade |

Não persistir coleção heterogênea de `flags` sem discriminar semântica. As flags
locais incluem domínio, loot, missões, mobs e objetos; cada categoria deve ser
convertida por adaptador explícito, não copiada para o online. O ledger de
missões autoradas é separado e ainda precisa de exportação completa futura.
Views de UI não carregam todos os contadores/recibos.

## Invariantes e reaproveitamento

| Área | Reutilizar | Validação adicional necessária ao contrato futuro |
| --- | --- | --- |
| Identidade/raça | CharacterIdentity, CharacterRaces | Vínculo ao CharacterId estável e proprietário autorizado |
| Progressão/domínio | RpgProgression, DomainProgression.validateFor | Versão de regras e consistência com atributos/recibos |
| Inventário | ItemCatalog, InventoryService.restore | Detectar duplicata de slot na lista **antes** de converter a Map |
| Equipamentos | Regras de equip/slot/nível de RpgWorld | Validar candidato sem consumir/adicionar itens nem chamar equip para instalar |
| Missões/recompensas | QuestBook.requireValid, QuestService, QuestRewards | Export/import puro do ledger; recibo atômico durável, sem complete/reward no restore |
| Geometria | CollisionService, RpgCollisionProfile, revisão ativa | Reserva atual e revalidação no owner imediatamente antes de ativar |
| Atributos/energia | StatsComponent, LevelingRules, EnergyRules | Construtor de Stats não rejeita STR/INT/AGI negativas; impor não negativos e orçamento; não reconceder pontos |

Validadores atuais não são importadores completos. Reutilizar a lógica pura
existente ou extrair uma função estreita em tarefa autorizada, sem duplicar
fórmulas nos DTOs. `RpgWorld.restore()` monta campanha builtin: não é validador
de geometria online autorada e não recupera o estado multiplayer completo.

Não impedir duas pilhas legítimas do mesmo item em slots diferentes. Duplicata
de slot é inválida; nome igual de item não prova duplicação econômica. O modelo
atual não tem UUID por exemplar de equipamento. Item único futuro exigirá
identidade de exemplar própria e regra aprovada, não um campo inventado na auditoria.
Equipar deve transferir uma unidade; restaurar deve instalar o snapshot inteiro,
sem somar aos itens iniciais. Consistência econômica depende também dos recibos.

## Relógio e política de cooldown proposta

**Recomendação para aprovação: recargas continuam transcorrendo desconectado.**
Aplicar às cinco skills do domínio, ataque básico, Shunpo, poção e viagem.
Capturar no owner antes de remover executor/participação; não usar cooldowns
zero de `state()` após remoção. Preservar a fração efetiva do ataque básico
(`preciseAttackReady`), não apenas o inteiro exibido ao cliente.

Cada prazo usa chave estável da ação/skill e versão de catálogo; slot 1 sozinho
não identifica habilidade após troca de domínio. Snapshot inclui carimbo do
relógio de origem e deadline autoritativo. Os ticks de RW/RS e o clock do engine
não devem ser considerados intercambiáveis sem confirmar sua relação na captura.

Na mesma época/processo, usar tempo monotônico do servidor. Para persistir entre
reinícios, converter o restante validado para deadline durável baseado em tempo
confiável do servidor, com versão da política e âncora persistida. Conceitualmente:
`restante = max(0, deadline - agoraAutoritativo)`. Conversão de ticks/fração para
tempo e precisão do armazenamento arredondam conservadoramente, sem liberar cedo.
Não comparar nanoTime/tick do processo anterior com os do novo processo.

Rollback do relógio, época desconhecida ou deadline impossível exigem recusa/
quarentena ou contingência aprovada, nunca assumir expirado/zero. Avanço do relógio
também precisa de política e teste de limites antes da integração. `capturedAt`
do cliente é ignorado. Política de tempo durável não foi implementada na R3:
aquele laboratório preserva prazos em ticks apenas na sua autoridade em memória.

Recarga expirada validamente volta pronta, não reinicia a duração completa.
Recarga ativa mantém restante; não se reinicia nem se elimina pelo login.
O bloco deve estar completo mesmo quando todas as ações estão prontas.

## Recursos, efeitos e A-04

Recomendar HP/energia congelados na captura, sem cura/regeneração offline
automática nesta política proposta. Morte continua morte; respawn é operação
separada. Máximos são recalculados dos atributos/regras, comparados ao snapshot;
incompatibilidade não se resolve com clamp silencioso. Energia salva localmente
é normalizada e arredondada; não serve para recuperar seu valor absoluto online.

Não restaurar intangibilidade, dash, sentinela ativa, projéteis, DoTs em execução,
marcas/checkpoints, animações, mira/alvo, input mantido, fila ou eventos históricos.
Gasto já confirmado e cooldown permanecem. Não gerar impactos/recompensas faltantes
reexecutando uma skill. Liquidação de contribuição/recompensa pendente é operação
econômica própria idempotente; mundo e população não vêm do snapshot individual.

Excluir execução temporária **não autoriza remover toda penalidade ao desconectar**.
Status negativos e custos/benefícios temporários requerem política por tipo.
Recomendação conservadora: não retomar benefício ativo; preservar descritor
restritivo/deadline ou impedir ativação até o fim da restrição. Não reexecutar
DoT antigo para simular duração. O tratamento exato de dano/penalidade offline
fica pendente de aprovação de gameplay antes de implementar recuperação.

Em A-04, preservar progresso permitido e custos/recargas sem copiar a fase de
Quatro relâmpagos. Instalar corpo só em apoio físico válido da revisão atual.
Sem apoio, reter e recusar/diferir ativação; sem deslocamento arbitrário ou
ligação automática do timeout de 92 ticks.

## Recuperação sem efeitos colaterais

Pipeline futuro: verificar posse/geração → ler documento confirmado → validar
versão/revisão/catálogos → validar economia/ledgers → calcular recursos/prazos →
preparar candidato e reserva no owner → confirmar operação exclusiva → ativar
sem concessões/replay. Todos os mapas/listas são cópias imutáveis e limitadas.
Falha antes da ativação mantém original confirmado; após commit usa reconciliação
por recibo. Falta campo obrigatório, região inválida ou spawn ocupado não cria
personagem parcial nem consome autorização silenciosamente.

Limites atuais de save (XP/moedas 1000000, 20 slots de bolsa no RW, pilhas por
catálogo, quatro slots de equipamento) são referências técnicas verificadas,
não autorização para mudar balanceamento nem para aceitar payload ilimitado.
Orçamentos novos de bytes/recibos, retenção e migrações serão validados no DTO
futuro. Não inventar defaults que apaguem progresso durante migração.
