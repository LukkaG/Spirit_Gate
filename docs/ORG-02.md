# ORG-02 — Estabilização da arquitetura e do build

## Contexto e motivação

Consolidar a extração inicial do catálogo de mobs e investigar o carregamento de
recursos sem alterar o jogo nem iniciar persistência PostgreSQL.
Base: [organização](ORGANIZACAO-JOGO-2026-11-09.md).

## Estado atual verificado

Branch `beco`, HEAD `13b91dc`; início com 69 modificados, 2429 removidos e 370
entradas não rastreadas segundo `git status --short` (diretórios podem ser agrupados).
Todos esses estados são anteriores à ORG-02 e foram preservados.
Java 17 e Maven 3.10.0; único POM encontrado: `server/pom.xml`.
Recursos reais em `server/src/main/resources/rpg`, incluindo 13 properties e um sgquest.

## Objetivo e escopo

Investigar recursos/lifecycle, comparar modelos de mob, conferir constraints SQL,
preparar testes descartáveis e executar regressão Java/gráfica disponível.
Nenhuma movimentação de pacotes ou consolidação dos dois runtimes nesta etapa.

## Fora do escopo

Integração JDBC, credenciais, produção, migração de saves, nova curva de XP,
balanceamento, recompensas, alteração das 40 skills, protocolo, cooldowns,
TASK-225/A-04, World Studio, launcher, assets e distribuições.

## Arquivos envolvidos

- `server/pom.xml` e recursos: inspeção, sem alteração de configuração presumida.
- `domain/mob/MobDefinition`, `MobCatalog`, `rpg/EnemyService`: auditoria.
- `domain/entity/Mob`, `domain/factory/MobArchetype`, `MobFactory`: comparação.
- Novos testes `ResourceLifecycleTest`, `MobDefinitionTest`, `MobCatalogRuntimeTest`.
- `database/drafts/001_mob_catalog.sql`: revisão, sem execução.
- `database/tests/`: preparação de teste PostgreSQL independente do jogo.

Caminhos Java relativos a `server/src/main/java/com/projetok`; testes em `src/test/java`.

## Plano incremental e critérios de aceite

1. Inspecionar Git, logs, fontes e configurações reais.
2. Reproduzir ausência de recursos na saída; exigir que `mvn test` os recupere.
3. Não modificar POM sem defeito comprovado; registrar hipótese e limitação histórica.
4. Conferir catálogo versus runtime e fronteiras de validação sem mudar valores.
5. Gerar SQL de regressão exclusivamente para base descartável e registrar execução real.
6. Reexecutar os 19 testes, suíte geral e complemento gráfico disponível.
7. Entregar matriz, testes, relatório e atualização da organização.

## Riscos, compatibilidade e rollback

Os dois modelos de mob não são equivalentes em atributos, IA, loot e estado por geração.
Uma futura adaptação exige decisões separadas; esta etapa adiciona testes e evidências.
Reversão: remover somente arquivos de teste/documentação e gerador novos da ORG-02,
e o adendo acrescentado à organização. Não restaurar arquivos globalmente pelo Git.
O banco, saves e comportamento do jogo não são modificados por essa reversão.

## Entregáveis

[Testes](ORG-02-TESTES.md), [matriz](ORG-02-MATRIZ-MODELOS-MOB.md),
[relatório](ORG-02-RELATORIO-FINAL.md) e preparação em `database/tests/`.
