# ORG-02-R2 — Testes e evidências

## Execução controlada

09/11/2026, desktop reservado confirmado pelo usuário. Cada rodada usa um
processo Maven e uma JVM Surefire sequencial; nenhuma execução gráfica paralela.
`temp/org02-r2/native-controlled/ready` antecede os testes. UTC nos logs nativo
e EDT usa o mesmo relógio Windows; tempos relativos usam relógio monotônico.
Não interpretar o timestamp UTC de 10/11 como mudança da data local de 09/11.

| Rodada | Ordem real do Surefire | Casos reportados | Falhas | Erros | Skips |
| --- | --- | ---: | ---: | ---: | ---: |
| 01-isolated-a1 | Nenhuma classe executada | 0 | 0 | 0 | 0 |
| 02-isolated-a1 | SkillInputRefinementGuiTest | 2 | 0 | 0 | 1 |
| 03-isolated-a2 | SkillInputRefinementGuiTest | 2 | 0 | 0 | 1 |
| 04-isolated-b1 | Task224CastingGuiTest | 1 | 0 | 0 | 0 |
| 05-isolated-b2 | Task224CastingGuiTest | 1 | 0 | 0 | 0 |
| 06-together | SkillInputRefinementGuiTest → Task224CastingGuiTest | 4 | 0 | 0 | 0 |
| 07-reverse | Task224CastingGuiTest → SkillInputRefinementGuiTest | 4 | 0 | 0 | 0 |
| 08-after-hud | CompactHudCharacterGuiTest → SkillInputRefinementGuiTest → Task224CastingGuiTest | 5 | 0 | 0 | 0 |
| 09-after-hotkeys | GameInputRoutingTest → SkillInputRefinementGuiTest → Task224CastingGuiTest | 8 | 0 | 0 | 0 |
| 10-repeat-1 | SkillInputRefinementGuiTest → Task224CastingGuiTest | 4 | 0 | 0 | 0 |
| 11-repeat-2 | SkillInputRefinementGuiTest → Task224CastingGuiTest | 4 | 0 | 0 | 0 |
| 12-repeat-3 | SkillInputRefinementGuiTest → Task224CastingGuiTest | 4 | 0 | 0 | 0 |
| 13-repeat-4 | SkillInputRefinementGuiTest → Task224CastingGuiTest | 4 | 0 | 0 | 0 |
| 14-repeat-5 | SkillInputRefinementGuiTest → Task224CastingGuiTest | 4 | 0 | 0 | 0 |
| 15-repeat-6 | SkillInputRefinementGuiTest → Task224CastingGuiTest | 4 | 0 | 0 | 0 |
| 16-full-suite | Suíte padrão: 273 classes | 1068 | 0 | 0 | 0 |

As rodadas 02/03 têm um parâmetro executado e outro excluído explicitamente
pelo filtro diagnóstico. Os dois skips são seleção intencional, não headless
nem ocultação de falha. Na rodada 01 o seletor `[1]` do Surefire descobriu zero
testes; seu retorno zero **não conta como teste aprovado**. O comando e log
foram preservados. As demais rodadas dirigidas executaram todos os selecionados.

Antes da suíte geral: **44 execuções dos quatro casos investigados e cinco casos
de contexto (HUD/hotkeys), todos aprovados**. São repetições, não 49 casos únicos.
Suíte geral: **1068 casos, 0 falhas,
0 erros, 0 skips**.

## Comandos e seleção

Base dirigida:

```powershell
mvn -o -f server/pom.xml '-Dmaven.repo.local=.maven-repository' '-Dtest=SkillInputRefinementGuiTest,Task224CastingGuiTest' '-Dsurefire.runOrder=alphabetical' '-Dspiritgate.test.guiTrace=true' test
```

Rodadas 02/03: somente `SkillInputRefinementGuiTest`, com
`-Dspiritgate.test.guiInvocation=1` ou `2`. Rodadas 04/05: `-Dtest=Classe#método`
usando os nomes completos dos métodos registrados no respectivo `summary.json`.
Rodada 07: `reversealphabetical`. Rodada 08 acrescenta `CompactHudCharacterGuiTest`;
09 acrescenta `GameInputRoutingTest`. Ambas precederam as classes investigadas
na mesma JVM. Rodadas 10–15 repetem a seleção conjunta, na mesma ordem.

Suíte geral, sem seletor nem ordem imposta:

```powershell
mvn -o -f server/pom.xml '-Dmaven.repo.local=.maven-repository' '-Dspiritgate.test.guiTrace=true' test
```

Dois `test-compile` offline prévios também passaram. Maven informou classes
atualizadas; `javap` confirmou os métodos novos no bytecode, compatível com
compilação automática da IDE antes do Maven. Não atribuir essa recompilação
ao Maven. Logs: `compile.txt` e `compile-final.txt`.

## Evidências e falhas intermediárias

Cada diretório de rodada contém `maven.txt`, `summary.json` e somente XMLs das
classes registradas como `Running`. `rounds.json` consolida ordens, totais,
comandos, horários e contagens observadas. Não somar XMLs antigos do target.
`correlated-losses.json` relaciona perdas AWT com os dois eventos nativos mais
próximos; proximidade temporal não constitui prova causal.

A primeira inicialização do script nativo foi recusada pela política PowerShell;
foi executado com `-ExecutionPolicy Bypass` só naquele processo, sem mudar
política persistente. Uma tentativa de edição por `python -c` falhou por sintaxe
antes de escrever; `prepare.py` fez as substituições posteriormente.
O primeiro inventário amplo foi interrompido por demora, sem completar:
não é backup válido. Nenhuma falha intermediária de teste foi descartada.

`prepared-sha256.json` registra 1643 fontes/recursos e 23 capturas, após a
instrumentação inicial e antes dos testes. Não é baseline anterior às edições.
O backup de PNGs foi ampliado para 6702 arquivos existentes antes de HUD/suíte;
as 23 capturas iniciais já estavam preservadas antes dos quatro casos.
100 capturas divergentes foram arquivadas em `captures-generated/`
e restauradas do backup; lista em `captures-restored.json`.

Os testes usam entrada AWT sintética e TCP loopback. Não houve homologação
com teclado físico, outra máquina/Radmin ou launcher nativo.
