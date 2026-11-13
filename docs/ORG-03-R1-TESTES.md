# ORG-03-R1 — Testes e evidências

## Resultado final, 10/11/2026

Java 17 / Temurin 17.0.20.1, Maven 3.10.0, Windows 10. **47 novos casos e 87 dirigidos
anteriores: 134 aprovados em 21 classes, zero falhas/erros/skips.**
Suíte geral final headless: **1115 casos reportados, 1058 executados,
57 excluídos, 0 falhas e 0 erros**, em 274 classes.
Exclusões headless não contam como aprovação da interface. Não houve nova
homologação de foco/HUD/hotkeys, teclado físico, Radmin ou launcher.

## Comandos executados da raiz

Compilação inicial:

```powershell
mvn -o -f server/pom.xml '-Dmaven.repo.local=.maven-repository' test-compile
```

Testes novos iniciais: mesmo comando-base com `-Dtest=CharacterSnapshotValidatorTest`.
Regressão dirigida final:

```powershell
mvn -o -f server/pom.xml '-Dmaven.repo.local=.maven-repository' '-Dtest=CharacterSnapshotValidatorTest,CharacterIdentityTest,DomainProgressionTest,RpgWorldTest,LocalSaveStoreTest,CharacterSavesTest,InventoryServiceTest,EquipmentRpgTest,EnergyRulesTest,AttributeCombatRulesTest,QuestBookTest,QuestRewardsTest,QuestLifecycleTest,SyntheticOnlineRepositoryTest,Task194PersistenceTest,Task195SessionOwnershipTest,Task196ResumeTest,Task225R3RecoveryLabTest,SessionCharacterSelectionTest,Task201CharacterSessionTest,AttributeAllocationTest' '-Dsurefire.runOrder=alphabetical' test
```

Suíte geral final:

```powershell
mvn -o -f server/pom.xml '-Dmaven.repo.local=.maven-repository' '-Djava.awt.headless=true' test
```

Sem instalação, dependência nova ou cópia manual de resources. Fixtures novas
carregam catálogos antes de validate; a consulta pura não lê arquivos. Os testes
antigos usam @TempDir, diretórios sintéticos e loopback; não contas/saves pessoais.

## Rodadas, incluindo evidências intermediárias

| Evidência em temp/org03-r1 | Resultado e interpretação |
| --- | --- |
| compile.txt | BUILD SUCCESS, produção e testes compilados para release17 |
| new-tests.txt | 43 casos novos iniciais aprovados |
| directed.txt | 133 casos aprovados, incluindo46 novos após ampliar cobertura |
| full-headless.txt | 1114 reportados,57 excluídos, sem falhas/erros; versão anterior ao caso nulo |
| null-identifiers-before-fix.txt | Um caso com um erro NPE, BUILD FAILURE; reprodução deliberada do defeito novo |
| directed-final.txt | 134 casos aprovados, incluindo47 novos após corrigir IDs internos nulos |
| full-headless-final.txt / full-headless-interrupted.txt | Rodada interrompida antes do agregado; não contada como suíte aprovada |
| full-headless-resumed.txt | Suíte final completa, sobre os mesmos fontes corrigidos; totais acima |

Em cada rodada arquivada há summary.json e XMLs apenas das classes Running
daquele comando. A rodada interrompida conserva o log bruto, sem atribuir-lhe
os XMLs antigos que o Maven deixou no target. Contagens de rodadas repetidas
não são casos únicos somáveis. Os 47 novos casos também estão na suíte geral.

## Falha encontrada e correção

O validador recém-criado registrava ID ausente, mas podia consultar Set/Map
imutável com null em seguida. A reprodução focalizada usou:

```powershell
mvn -o -f server/pom.xml '-Dmaven.repo.local=.maven-repository' '-Dtest=CharacterSnapshotValidatorTest#nullNestedIdentifiersAreRejectedWithoutThrowing' test
```

O primeiro caso produziu NPE em cosméticos. A leitura identificou risco equivalente
para região. Corrigidos somente os guards dessas consultas na classe nova,
mantendo violações explícitas sem default ou fallback. O teste final cobre
ambos. Diagnóstico local em `logs/analises/ORG-03-R1-validacao-nulos.md`.
Nenhuma asserção antiga foi removida e nenhum componente de runtime foi ajustado.

## Cobertura e preservação

Matriz dos 30 requisitos: [ORG-03-R1-MATRIZ-VALIDACOES.md](ORG-03-R1-MATRIZ-VALIDACOES.md).
Além de valores válidos/inválidos, testa cópias profundas de coleções, limites
sem truncar, ausência de cada bloco, nova consulta sem mutação e dependências
diretas de bytecode sem UI/socket/banco/I/O ou referência ao mundo vivo.

Inventário anterior: 2343 hashes. Somente o documento principal recebe adendo;
nenhum arquivo anterior Java, POM, resource, asset, SQL ou ferramenta foi alterado.
Três fontes e um teste são novos. Resultado de conferência em `validation.json`.
Evidências de PNG e medições textuais regeneradas pelos testes foram preservadas;
2 arquivos divergentes arquivados em generated-this-task e restaurados
individualmente, conforme evidence-restored.json. Nenhuma limpeza global.

Na retomada após a pausa, a comparação dos 2343 arquivos ainda não mostrava
diferença nem ausência. Evidências posteriores à pausa também foram verificadas
antes da restauração; nenhum arquivo novo do usuário foi descartado.

## Limites de validade

Nada testa persistência PostgreSQL, autenticação, clock entre reinícios,
captura/importação completas, owner vivo, lease/fence, spawn ocupado durante
ativação ou idempotência durável do novo contrato: não foram implementados.
O laboratório R3 anterior permanece isolado; sua aprovação não vira recuperação
TCP. A intermitência AWT da R2 permanece sem nova investigação nesta tarefa.
