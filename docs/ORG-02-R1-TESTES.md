# ORG-02-R1 — Testes e evidências

09/11/2026, Windows 10, Temurin 17.0.20.1, Maven 3.10.0. Sem instalação,
download, cópia manual de resources ou execução concorrente de testes gráficos.
Comandos partem da raiz. Evidências locais em `temp/org02-r1/`.

## Rodadas iniciais

| Rodada | Seleção/ordem real | Casos | Falhas | Erros | Skips |
| --- | --- | ---: | ---: | ---: | ---: |
| 01 | SkillInputRefinementGuiTest isolada | 2 | 0 | 0 | 0 |
| 02 | Task224CastingGuiTest isolada | 2 | 0 | 0 | 0 |
| 03 | SkillInputRefinementGuiTest → Task224CastingGuiTest | 4 | 1 | 0 | 0 |
| 04 | Task224CastingGuiTest → SkillInputRefinementGuiTest | 4 | 0 | 0 | 0 |
| 05 | CompactHudCharacterGuiTest → GameInputRoutingTest → GamePanelsIntegrationTest → SkillAimInputRoutingTest → SkillInputRefinementGuiTest → Task224CastingGuiTest | 11 | 0 | 0 | 0 |

Na rodada 03 o segundo parâmetro de área falhou na linha 46 por
`Evento sem componente focado após aguardar a transferência AWT`, lançado
por `SwingKeys.post`, linha 24. Não é a asserção `hudDomainSkill3` histórica.
O primeiro parâmetro e ambos os métodos de Task224 passaram nessa rodada.
As quatro primeiras rodadas usaram as duas classes originais sem instrumentação;
a rodada 05 também não usou a extensão, ainda não vinculada às classes.

Comando-base dirigido:

```powershell
mvn -o -f server/pom.xml '-Dmaven.repo.local=.maven-repository' '-Dtest=SkillInputRefinementGuiTest,Task224CastingGuiTest' '-Dsurefire.runOrder=alphabetical' test
```

Na 01/02, `-Dtest` seleciona apenas a classe indicada. Na 04,
`-Dsurefire.runOrder=reversealphabetical`. Na 05, a seleção contém as seis
classes listadas, com ordem alfabética. Não presumimos que a ordem da lista
`-Dtest` seja respeitada: usamos a ordem `Running` registrada pelo Surefire.

## Regressão geral e repetições

A rodada 06 executa o lifecycle completo com `-Dspiritgate.test.guiTrace=true`
para imprimir também os sucessos instrumentados. A regressão final usa o comando
padrão solicitado, sem cópia de recursos:

```powershell
mvn -o -f server/pom.xml -Dmaven.repo.local=.maven-repository test
```

| Rodada | Seleção | Casos | Falhas | Erros | Skips | Tempo Maven |
| --- | --- | ---: | ---: | ---: | ---: | --- |
| 06 | Suíte completa, trace explícito | 1068 | 0 | 0 | 0 | 4min42s |
| 07 | Duas classes, alfabética, trace automático de falha | 4 | 1 | 0 | 0 | 18,946s |
| 08 | Repetição da mesma seleção/ordem | 4 | 0 | 0 | 0 | 18,068s |
| 09 | Suíte completa, comando padrão | 1068 | 0 | 0 | 0 | 4min32s |
| 10 | Mob e recursos, incluindo os novos testes ORG-02 | 23 | 0 | 0 | 0 | 4,434s |

Rodada 07: primeiro parâmetro do caso A falhou na linha 44, ao pressionar a
seta direita após a preparação. `SwingKeys.post` não encontrou componente focado
no prazo. A EDT registrou perda de foco/desativação nativa antes da falha,
com sessão conectada e roteador aberto; detalhes na análise GUI. O teste original
de aquisição de foco passou nessa invocação. A rodada 08 passou nos quatro casos.
Tempos exatos de execução constam nos logs; não somar casos entre rodadas.

Comando 10:

```powershell
mvn -o -f server/pom.xml '-Dmaven.repo.local=.maven-repository' '-Dtest=ResourceLifecycleTest,MobDefinitionTest,MobCatalogRuntimeTest,MobCatalogTest,EnemyRpgTest,EnemyAiTest,MobCollisionTest,MobCeroTest,EnemyDurabilityTest,Task208ProgressionTest' '-Dsurefire.runOrder=alphabetical' test
```

As duas suítes completas incluíram `ResourceLifecycleTest`, `MobDefinitionTest`
e `MobCatalogRuntimeTest`, além dos casos anteriores. O padrão de descoberta do
POM não precisou ser alterado. Todos os casos gráficos foram executados: zero skips.
Conferência independente dos XMLs das classes efetivamente executadas: 273 classes
e 1068 casos em cada suíte; 10 classes e 23 casos na rodada dirigida.

O monitor nativo temporário `observe-native-focus.ps1` usa somente leitura de
`GetForegroundWindow`/PID; foi iniciado durante 09, após a falha 07, e encerrado
por `stop-native-monitor`. `native-focus.txt` é evidência local posterior, não
prova da janela que recebeu foco em 07. O polling serve à observação e não
retarda nem altera a execução dos testes.

## Cobertura funcional usada

| Requisito | Evidência automatizada |
| --- | --- |
| TAB no mundo e TAB/SHIFT+TAB na HUD | GameInputRoutingTest; Task224CastingGuiTest |
| SPACE básico, ENTER focado, campos de texto sem gameplay | GameInputRoutingTest; GamePanelsIntegrationTest |
| F5–F9, remapeamento, ausência de duplicação por repetição de press | GameInputRoutingTest; testes de skill reais TCP |
| Preparar, mover mira sem caminhar, confirmar por ENTER/mesma skill e cancelar | SkillInputRefinementGuiTest; SkillAimInputRoutingTest; Task224CastingGuiTest |
| Energia, nível, cooldown e feedback distintos | Task224CastingGuiTest; EnergyRulesTest; RpgSkillEngineTest; SkillProtocolTest |
| Desconexão/reconexão e ausência de mira/tecla herdada | Task224CastingGuiTest; SessionGameReconnectCloseTest; testes de sessão |
| Nove ações acessíveis e layout | Task224CastingGuiTest; GamePanelsIntegrationTest; CompactHudCharacterGuiTest |
| Resoluções 1366×768, 1000×700, 640×480 | GamePanelsIntegrationTest e demais fixtures de HUD |
| Mob e resources Maven | ResourceLifecycleTest; MobDefinitionTest; MobCatalogRuntimeTest e 19 casos anteriores |

Os resultados são de execução automatizada com eventos AWT e TCP loopback;
não são homologação de teclado físico, outra máquina/Radmin ou launcher nativo.

## Preservação e leitura das evidências

`before-sha256.json` registra 1605 arquivos anteriores de produção, testes,
resources, assets, SQL e ferramentas. `git-status-before.txt` preserva a lista
inicial. O diff global iniciado foi interrompido por lentidão de leitura dos
artefatos removidos e é apenas parcial; não é usado como backup de restauração.

Cada rodada tem seu log; as ordens e totais são extraídos em `rounds.json`.
Os diretórios 02 em diante arquivam XMLs do Surefire, inclusive XMLs antigos que
o Maven deixa na pasta: para interpretar uma rodada dirigida, considerar apenas
as classes registradas como `Running` no respectivo log. Não somar XMLs antigos
ou execuções repetidas como casos únicos.

Capturas geradas são evidências, não assets aprovados. Há cópia das pastas
históricas em `captures-before/`; capturas anteriores ao backup podem já ser
saídas das primeiras rodadas. A baseline de fontes/recursos antecede os testes.
Ao final, 106 PNGs divergentes foram arquivados em `captures-generated/` e
restaurados a partir desse backup. A lista está em `captures-restored.json`.
Validação dos links locais e whitespace dos arquivos da entrega: sem problemas.

Não houve teste SQL, instalação, migration, publicação ou alteração de saves pessoais.
