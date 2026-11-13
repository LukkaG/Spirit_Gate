# ORG-02 — Relatório final

## Resultado

Auditoria e estabilização verificável da primeira extração do catálogo concluídas
no escopo autorizado. O lifecycle padrão carrega os recursos reais sem cópia manual,
inclusive com saídas Java inicialmente ausentes. Não foi reproduzido defeito do POM;
a causa histórica da ausência inicial de recursos continua sem comprovação.

## Alterações entregues

- ResourceLifecycleTest: verifica14 arquivos reais no classpath e inicialização de mundo.
- MobDefinitionTest: fronteiras numéricas, IDs e restrição de nível.
- MobCatalogRuntimeTest: equivalência dos parâmetros das10 espécies na adaptação
  existente, com piso de HP1 e10000 e identidade inicial da instância.
- `database/tests/prepare_mob_catalog_test.py` e README: preparação de teste SQL
  transacional para base descartável, catálogo real, CSV roundtrip e30 rejeições.
- ORG-02.md, ORG-02-TESTES.md, ORG-02-MATRIZ-MODELOS-MOB.md e este relatório.
- Adendo no documento de organização, substituindo a pendência genérica de build
  pelo resultado e pelas limitações efetivamente observados.

Não houve modificação de fontes de produção, POM, rascunho SQL ou recursos nesta etapa.
Nenhum pacote foi movido. As alterações preexistentes do checkout foram preservadas.

## Verificação

19 testes anteriores aprovados pelo lifecycle padrão;23 aprovados com saídas de
compilação inicialmente ausentes. Suíte geral:1064 casos,1061 aprovados,3 falhas,
nenhum erro/skip. As duas classes com falhas passaram na repetição isolada (4 casos).
Não declarar a suíte geral verde. [Comandos e evidências](ORG-02-TESTES.md).

## Arquitetura e SQL

O catálogo de espécie, a instância RPG e o Mob por componentes são responsabilidades
diferentes. A matriz registra diferenças de HP, ataque, IA, spawn, geração, colisões,
loot e missões. Nenhuma equivalência de gameplay foi presumida.
Constraints do SQL e validações de MobDefinition coincidem na revisão estática;
o rascunho não cobre relações nem estado do mundo. Os testes de servidor PostgreSQL
estão preparados, mas não executados por indisponibilidade das ferramentas no PATH.

## Preservação e riscos pendentes

As40 habilidades, protocolo, dano, progressão, missões, saves, recargas, TASK-225/A-04,
World Studio, launcher, assets e distribuições não receberam alterações nesta etapa.
O inventário de331 arquivos de produção/recursos/POM/SQL permaneceu idêntico.
Não houve JDBC, credenciais, instalação, conexão com PostgreSQL, commit, push ou release.

Pendências: causa histórica dos recursos não reproduzida; três falhas gráficas na
execução conjunta sem causa raiz; validação SQL real; homologação física/Radmin.
Os logs locais históricos de World Studio e IDE permanecem fora do recorte de correção
da ORG-02; não foram declarados resolvidos por estes testes.

## Recomendações para etapas futuras — não iniciadas

1. **Player:** separar identidade, estado de sessão e snapshot durável; inventariar
   atributos manuais, energia, inventário/equipamentos e autoria antes do schema.
2. **Skills:** distinguir definição, execução autoritativa e apresentação, mantendo
   as40 habilidades e seus contratos; investigar falhas de GUI em tarefa própria.
3. **Quest:** separar definição de progresso por personagem; preservar IDs e planejar
   recibo transacional de recompensa para não duplicar XP/itens em retry.
4. **PostgreSQL:** executar o teste preparado numa base descartável; completar relações,
   revisão e idempotência antes de propor migrations/driver/pool e repositório assíncrono.
5. **Mob:** caracterizar primeiro o modelo por componentes e manter a adaptação atual
   do catálogo até haver equivalência de comportamento demonstrada.

Essas recomendações exigem escopo separado. TASK-225 e integração PostgreSQL não iniciadas.

## Reversão

Remover somente os três novos arquivos de teste, o gerador/README de testes SQL,
os documentos ORG-02 e o adendo na organização. Não restaurar o checkout globalmente.
As saídas Java anteriores continuam guardadas em target/org02-before-cold como
evidência local; o runtime atual usa as saídas recompiladas pelo Maven.
