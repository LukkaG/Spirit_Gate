# ORG-02 — Matriz dos modelos de mob

## Modelos confirmados

- **Espécie:** `domain/mob/MobDefinition` e `MobCatalog`, dados imutáveis.
- **Runtime RPG:** `rpg/EnemyService.Definition` e `EnemyService.Enemy`; usados por
  RpgWorld e SharedRpgEncounter. O catálogo adapta para Definition no construtor.
- **Modelo por componentes:** `domain/entity/Mob`, criado por `MobFactory` a partir
  de `MobArchetype`; usado em `prototype/DebugWorld` e `demo/EcosystemDemo`.

## Comparação de responsabilidades

| Campo/sistema | Catálogo e runtime RPG | Modelo por componentes | Implicação |
| --- | --- | --- | --- |
| Identificador de espécie | ID textual validado; referencia content, spawn e loot | MobArchetype não contém ID próprio de espécie | Não substituir pelo nome exibido ou ID de entidade |
| Instância | ID inteiro atribuído a partir de 100 em ordem de spawn | Inteiro fornecido ao factory | Não assumir identidade persistente global |
| Nome/região | Nome e região vêm de RpgContent; região de spawn deve coincidir | Nome do archetype, sem região equivalente no molde | Metadados não estão completos só em MobDefinition |
| HP | Espécie 1..10000; encontro pode aplicar piso; HP atual mutável na instância | HealthComponent recalculado por VitalityService, raça, nível e equipamento | Cálculos diferentes; unificação mudaria gameplay |
| Ataque | Valor 1..100 por espécie | CombatComponent calcula com atributos, equipamento e estilo | Não converter ataque em força por inferência |
| Velocidade | 1..200; avanço contínuo nominal em ticks de 30 Hz | Sem campo de velocidade em MobArchetype | Exige contrato explícito de unidades |
| Agressividade | Raio 40..800, percepção e seleção no runtime | AiComponent/AggressiveBehavior busca alvo (padrão 8 unidades) | Sem equivalência automática de escala |
| Leash | >= aggro e <=900; retorno ao home no RPG | Sem campo correspondente em MobArchetype | Precisa preservar comportamento de retorno |
| Respawn | 0..9000 ticks; zero desativa; readyTick por instância | Não implementado pelo factory/Mob | Manter no serviço de população |
| Spawn/home | ID textual de spawn + região + posição; geometria validada, até64 | Posição inicial fornecida ao factory; sem ID textual de spawn | Espécie, spawn e instância são entidades distintas |
| Geração | Começa em1 e aumenta no respawn; ledger separa derrotas | Sem geração equivalente em Mob | Essencial para impedir recompensa duplicada |
| Colisão | Raio19, navegação e corpos ocupados; multiplayer evita respawn sobre jogador | GameEntity/Position; factory não estabelece o contrato RPG de colisão | Não alterar colisão ao trocar representação |
| IA | Patrulha, perseguição, ataque, retorno, boss, técnicas e efeitos efêmeros | Comportamento anexado ao AiComponent; tick delegado | Modelo genérico não substitui a máquina RPG |
| Loot | LootService por espécie; ledger spawn/geração; pendências de inventário | LootTable.roll por Mob, sem ledger por geração nesse método | Sorteio isolado não equivale à concessão idempotente |
| Missões | Eventos de kill/collect e QuestService/QuestBook usam IDs existentes | Sem integração equivalente dentro de MobFactory | Preservar IDs e o ponto atual de emissão de eventos |
| Nível | Restrito a1, recompensas acima disso não definidas | baseLevel no archetype e StatsComponent | Não levantar a restrição por organização |
| Raça | MobDefinition não contém raça | RaceComponent obrigatório no factory | Não inventar mapeamento espécie→raça |
| Save | EnemyService grava flags de spawn, HP, posição, geração, readyTick e estado | Mob/Factory não expõem esse formato RPG | Nenhuma migração nesta etapa |

## Catálogo real e referências

10 IDs: `slime`, `wolf`, `warden`, `sentinel`, `mob_guardiao`, `mob_alado`,
`mob_rastreador`, `mob_tecela`, `mob_predador`, `mob_sentinela`.
Todos possuem metadados enemy no conteúdo real; os valores foram carregados pelos testes.
O catálogo não aplica o piso de 10000HP usado em certos encontros multiplayer.
O teste MobCatalogRuntimeTest compara os oito campos das 10 espécies com a adaptação
do runtime, em piso1 e piso10000, incluindo spawn, ID e geração inicial.

Spawns nativos: forest_a/b (slime), forest_c (wolf), sanctuary (warden),
ruins_guard_a/b (sentinel). Os seis mobs experimentais não foram acrescentados a esses
spawns nem receberam recompensas novas. Loot deles tem chance0 no arquivo atual.
Quests nativas first_steps e sanctuary referenciam slime e warden, respectivamente;
o ID de espécie permanece distinto do ID de spawn e do ID da missão.

## Adaptação incremental recomendada — não executada

1. Manter MobDefinition como contrato de espécie e o runtime como dono da instância.
2. Definir interfaces de leitura necessárias para UI/IA sem mudar regras de dano.
3. Caracterizar cálculos de HP/ataque nos dois modelos antes de qualquer conversão.
4. Projetar IDs de spawn e recibos por geração antes da persistência de mundo.
5. Só então decidir se o modelo por componentes será adaptado ou restrito ao protótipo.

## SQL versus Java

`database/drafts/001_mob_catalog.sql` tem os mesmos oito campos e limites de MobDefinition:
id textual 1..40 com inicial minúscula, hp1..10000, ataque1..100, velocidade1..200,
aggro40..800, leash entre aggro e900, respawn0..9000, nível1.
PK implica ID único e não nulo; demais colunas são NOT NULL. Java usa int primitivo
e valida ID nulo; MobCatalog rejeita duplicidade. SQL usa integer, não float.

O arquivo properties aceita seis números e assume nível1; importação SQL exige
os sete números explicitamente. O gerador normaliza essa diferença. Respawn é
`respawn_ticks` em SQL e `respawn` em Java, sem conversão de unidade.
Não há FK atual: espécie não inclui raça, spawn, loot ou missão. Futuros vínculos
devem usar IDs estáveis, validar referências e indexar FKs; não inserir colunas
fictícias para aparentar um contrato completo. O schema é apenas um rascunho.
Paridade estática revisada; constraints e COPY em PostgreSQL ainda pendentes de execução.
