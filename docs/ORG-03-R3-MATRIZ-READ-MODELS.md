# ORG-03-R3 — Matriz de read models

## Fronteiras Java

Todos os tipos e métodos novos de leitura têm visibilidade de pacote. Os arquivos
abaixo estão em `server/src/main/java/com/projetok/rpg/`. Nenhum objeto de mundo,
inventário vivo, Session, array ready, RNG, corpo ou fila sai nos read models.

| API | Resultado e guarda | Visibilidade |
| --- | --- | --- |
| RpgWorld.readCharacterState(context) | CharacterReadState; valida owner/instância/identidade/região/domínio/presença declarada | pacote |
| RpgSkillEngine.readTiming(participant, world, participants, context) | TimingRead; valida contexto/executor, mapa de participação e sessão real | pacote |
| QuestService.readLedger(progress, flags, context, world) | Ledger; helper de cópia no owner, sem refresh | pacote |
| AuthoredQuestSession.readLedger(context, world) | Ledger autorado já existente | pacote |
| AuthoredQuestSession.readBook(context, world) | QuestBook imutável já validado | pacote |
| CharacterReadContext(world[, engine]) / retire() | Escopo interno declarado; sem autenticação ou mutação no runtime | pacote |

O helper de QuestService recebe os mapas internos pelo chamador do pacote; ele
não prova independentemente a origem desses mapas. O acesso normal é agregado
por RpgWorld. O chamador confiável também associa o ledger autorado ao mundo.

## Dados, precisão e destino

| Campo ou bloco | Fonte real | Leitura R3 | Destino no laboratório / limite |
| --- | --- | --- | --- |
| Identidade observada | CharacterIdentity | Record imutável; UUID/nome/raça/aparência exatos | Observação; UUID não vira identidade persistente |
| Região/posição/tick | region/body/ticks | String, float e long originais | Location/tick sintéticos R2; sem reserva de spawn |
| XP/domínio | xp/domain | long e DomainProgression imutável, inclusive meias unidades/data/reset | Progression; sem concessão |
| Atributos brutos | manualStats | StatsComponent imutável, seis valores exatos | Attributes somente se modo manual ativo |
| Modo/pontos/checkpoint | manualStatsEnabled/unspentPoints/level | Sem ativar modo ou aplicar level-up | Desabilitado continua observação bruta; não converter automaticamente |
| HP/energia | hp/stamina | int absoluto, sem normalizar nem regenerar | Resources; morte preservada |
| Fração de regeneração | energyRegenerationRemainder | double original, sem arredondar | Resources; teste de bits exatos |
| Bolsa/equipamento/moedas | inventory/equipment/coins | Lista com índices e nulls reais, mapa copiado, int | Inventory/Equipment/Wallet sem duplicar item equipado |
| Ataque básico | attackReadyTick/preciseAttackReady | long inteiro e double preciso independentes | Observação exata; NaN inicial explícito, sem deadline fabricado |
| Shunpo/poção/viagem | abilityReadyTick/potionReadyTick/portalReadyTick | long absoluto original | Observação; nenhum prazo durável entre processos |
| Cinco skills | Session.ready + forDomain | Lista de SkillDeadline(id real, long absoluto), somente com sessão PRESENT | Observação; sem conversão prematura a Cooldowns recuperável |
| Referência temporal | world.ticks / engine.clock | Dois long distintos, mesmo ponto de leitura | Sem sincronização, clock de parede ou época inventados |
| Builtin | quests/flags/QuestService.definitions | Ledger copiado: progress/states/flags/definitions/objectives | Observação; quests do DTO continuam ausentes sem versão/prova |
| Autorado | progress/flags/book | Ledger e QuestBook imutáveis | Versão do livro não é versão de entrega |
| Objetivos/índices | Definition.objectives + flags qo | ObjectiveRead por índice e definição, storedCounter nullable | null = flag ausente; não é zero inferido |
| Watermarks locais | flags qe | Strings originais preservadas no ledger | Não virar EventWatermark durável sem época confiável |
| Conclusão/aceitação | progress.containsKey / quest-done / state | Dados e estado reais, sem complete/refresh | Flag histórica não comprova recibo |
| Loot pendente | flags builtin pending | Evidência textual bruta copiada, se presente | Sem PendingEntitlement econômico fabricado |
| ROOT/STUN/SLOW | timers/scale de RpgWorld | long/float originais, mesmo se expirados ainda presentes | Observação sem limpeza, origem desconhecida |
| Evasão/transição/dash/sentry/intangível | campos RpgWorld | Flags/timer observáveis | Intangibilidade e execuções não entram no DTO recuperável |
| Efeitos externos/DoT | executor e fontes não agregadas | externalEffectsKnown=false | Restrição recuperável continua nula; política pendente |
| Recibos/revisão econômica | Ausentes no runtime | Não criados | null / NOT_PRESENT; captura incompleta |
| Posse/época/regras/revisão durável | Não comprovadas pelo runtime | Não derivadas de UUID/tick | Contexto R2 é declarado sintético, não produção |

O contador de coleta pode ser derivado de inventário em um refresh anterior e
estar presente apenas no total `progress`. R3 não executa refresh: conserva o
total e deixa storedCounter ausente quando não existe flag qo. Os flags brutos
continuam disponíveis para diagnosticar duplicatas e watermarks locais. Não
reconciliar valores durante leitura.

## Imutabilidade e diferenças para o DTO

CharacterReadState conserva a precisão nativa, inclusive NaN interno e índices
vazios da bolsa. Isso não significa que esses valores possam ser instalados ou
serializados no contrato recuperável. StatsComponent, CharacterIdentity,
DomainProgression, Stack, Definition/Objective e QuestBook/Draft já são records
imutáveis; suas listas são defensivamente copiadas nos tipos existentes. Os
novos mapas/listas/conjuntos são copiados, inclusive listas aninhadas de objetivos.

RecoverableCharacterSnapshot exige políticas, identidade e referências que a
observação não possui. A conversão fica em CharacterSnapshotCapture, em src/test.
As observações exatas são separadas do candidato; não há flag de ativação nas
APIs de leitura. `readyForActivation` continua falso no resultado R1 do laboratório.

## Contextos e recusas

Thread errada, escopo retirado e contexto de mundo/região/domínio alterado são
recusados antes da cópia. No timing, ABSENT e INCOMPATIBLE carregam deadlines
nulos; PRESENT pode conter cinco prazos vencidos ou zero. Não se chama state(),
session(), remove(), clear() ou qualquer executor de efeitos para observar.
`participants` deve ser o mapa do coordenador no mesmo owner, sem escritores
concorrentes. O contexto é declarativo: não valida peer, autenticação, revisão
durável ou admissão de produção por conta própria.
