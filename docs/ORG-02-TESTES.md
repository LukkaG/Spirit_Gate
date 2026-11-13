# ORG-02 — Testes e evidências

Data: 09/11/2026. Windows, Temurin Java17.0.20.1, Maven3.10.0, execução offline
com dependências já presentes em `.maven-repository`. Nenhuma instalação.
Comandos abaixo são executados na raiz do repositório.

## Resultados

| Execução | Testes | Falhas | Erros | Ignorados | Resultado |
| --- | ---: | ---: | ---: | ---: | --- |
| 19 anteriores, lifecycle padrão e recursos removidos da saída | 19 | 0 | 0 | 0 | Aprovada |
| Suíte geral antes dos novos testes serem compilados | 1064 | 3 | 0 | 0 | Falhou em três casos gráficos |
| Compilação do zero das saídas Java +19 anteriores +4 novos | 23 | 0 | 0 | 0 | Aprovada |
| Repetição isolada das duas classes gráficas que falharam | 4 | 0 | 0 | 0 | Aprovada isoladamente |
| Geração de SQL descartável com catálogo real | 10 espécies/30 rejeições | — | — | — | Preparado, não executado no PostgreSQL |

As execuções se sobrepõem; não somar a tabela como casos únicos. A repetição isolada
não transforma a suíte geral em aprovada. Não houve nova execução completa após
compilar os quatro testes novos, pois nenhuma fonte de produção foi alterada.

## Comandos Maven

19 anteriores:

```powershell
mvn -o -f server/pom.xml -Dmaven.repo.local=.maven-repository '-Dtest=MobCatalogTest,EnemyRpgTest,EnemyAiTest,MobCollisionTest,MobCeroTest,EnemyDurabilityTest,Task208ProgressionTest' test
```

Suíte geral:

```powershell
mvn -o -f server/pom.xml -Dmaven.repo.local=.maven-repository test
```

Compilação com saídas inicialmente ausentes e novos testes:

```powershell
mvn -o -f server/pom.xml -Dmaven.repo.local=.maven-repository '-Dtest=ResourceLifecycleTest,MobDefinitionTest,MobCatalogRuntimeTest,MobCatalogTest,EnemyRpgTest,EnemyAiTest,MobCollisionTest,MobCeroTest,EnemyDurabilityTest,Task208ProgressionTest' test
```

Complemento gráfico isolado:

```powershell
mvn -o -f server/pom.xml -Dmaven.repo.local=.maven-repository '-Dtest=SkillInputRefinementGuiTest,Task224CastingGuiTest' test
```

## Investigação de recursos

1. Revisados POM, `.gitignore`, diretórios de recursos, variáveis MAVEN e configuração
   de projeto; não há bloco resources customizado ou exclusão de properties/sgquest.
2. `mvn -o -X -f server/pom.xml -Dmaven.repo.local=.maven-repository resources:resources`
   registrou origem src/main/resources, destino target/classes, filtering=false,
   skip=false, overwrite=false e includes/excludes vazios.
3. Removidos somente os14 arquivos de dados gerados de target/classes/rpg, com
   verificação do caminho absoluto. `mvn test` os recriou e os19 testes passaram.
4. Para comprovar build sem saídas Java anteriores, os diretórios gerados classes,
   test-classes e maven-status foram preservados em target/org02-before-cold; os
   caminhos foram verificados dentro de server/target. Nenhuma fonte foi movida.
5. O comando padrão recompilou produção/testes e passou nos23 casos. Nenhum recurso
   foi copiado manualmente para satisfazer os testes durante esta ORG-02.
6. ResourceLifecycleTest abre os14 recursos reais exclusivamente via classpath e
   inicializa RpgWorld. MobCatalogRuntimeTest percorre10 espécies em dois pisos de HP.

O Resources Plugin3.5.0 e Compiler Plugin3.16.0 foram selecionados pelo Maven instalado.
Não foi comprovada falha de exclusão, saída, incremental ou cópia do POM; não houve
correção especulativa na configuração. A causa histórica da primeira falha da etapa
anterior permanece indeterminada. Há histórico de invocação direta de compiler:compile
e surefire:test: esses goals não substituem o lifecycle `test`, mas isso não prova
a causa da primeira execução, que também registrou resources antes de compile.

Referências oficiais de comportamento:
[Resources Plugin](https://maven.apache.org/plugins/maven-resources-plugin/usage.html),
[lifecycle Maven](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html).

## Falhas gráficas preservadas

Na suíte geral:

- SkillInputRefinementGuiTest.groundUsesKeyboardWithoutMovingPlayerAndConfirmsWithEnterOrSameSkill:
  dois parâmetros falharam na linha34; `hudDomainSkill3`, esperado true, recebido false.
- Task224CastingGuiTest.viewportAndHudTabStaySeparateAndGroundPreviewNeverPretendsAcceptance:
  linha64; `hudDomainSkill1`, esperado true, recebido false.

Ambas classes passaram isoladamente: quatro casos, zero skips. A suíte geral incluiu
10 classes cujo nome contém Gui, com25 casos e três falhas. Outros testes Swing também
foram executados. O comportamento observado é sensível ao contexto de execução;
não há causa raiz comprovada para atribuí-lo a foco, timing ou regra de habilitação.
Nenhuma mudança em input/skills foi feita para tornar os testes verdes.

## Evidências locais

- `temp/org02-resources-debug.txt`: configuração e cópia de recursos.
- `temp/org02-directed.txt`: os19 testes com saída de dados removida previamente.
- `temp/org02-suite.txt` e `temp/org02-suite-reports/`: resultado geral original,
  preservado antes das repetições para não perder as falhas.
- `temp/org02-cold.txt`: compilação do zero e23 testes.
- `temp/org02-gui.txt`: repetição gráfica isolada.
- `temp/org02-preservation.json`: hashes de331 arquivos de produção/recursos/POM/SQL;
  comparação posterior encontrou zero alterações nesses arquivos.

Arquivos temp/target são evidências locais, não artefatos publicados.

## PostgreSQL e limitações

Gerador executado: `python database/tests/prepare_mob_catalog_test.py`.
Instruções e proteções em [database/tests](../database/tests/README.md).
psql/postgres/initdb/docker não encontrados no PATH; isso não prova ausência de
instalações fora do PATH. SQL preparado, constraints e COPY não executados em servidor.
Não foram testados produção, rede física/Radmin, teclado físico ou launcher nativo.
