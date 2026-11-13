# TASK-223 — Testes e evidências da auditoria

Execução local de 02/11/2026, branch `beco`, HEAD `6d72c65`. Nenhuma fonte/teste de produção foi editada para obter estes resultados.

## Ambiente e comandos

JDK Temurin 17.0.20.1; projeto Maven `server/pom.xml`, release 17 e dependências de teste já disponíveis. Não houve instalação. Diretório de trabalho dos comandos Maven: `server/`.

```powershell
mvn test -o `
  "-Dmaven.repo.local=C:/Users/lucas/Documents/GitHub/Projeto-K/.maven-repository" `
  "-Dspiritgate.logs.dir=C:/Users/lucas/Documents/GitHub/Projeto-K/.task-checkpoints/CORUJAO-IX/TASK-223/test-errors" `
  "-Djava.awt.headless=true"
```

Depois da suíte geral, foram selecionadas as 23 classes com skips gráficos, mais `NetworkSkillPresentationTest` e `SkillTooltipTest`. Lista exata em `gui-tests.txt` do checkpoint:

```powershell
$tests=Get-Content ../.task-checkpoints/CORUJAO-IX/TASK-223/gui-tests.txt -Raw
mvn test -o `
  "-Dmaven.repo.local=C:/Users/lucas/Documents/GitHub/Projeto-K/.maven-repository" `
  "-Dspiritgate.logs.dir=C:/Users/lucas/Documents/GitHub/Projeto-K/.task-checkpoints/CORUJAO-IX/TASK-223/test-errors" `
  "-Djava.awt.headless=false" "-Dtest=$tests"
```

## Resultados executados

| Rodada | Casos/execuções | Falhas | Erros | Ignorados |
|---|---:|---:|---:|---:|
| Suíte geral | 927 | 0 | 0 | 51 |
| Complemento gráfico, 25 classes | 103 | 0 | 0 | 0 |
| Agregado mais recente por classe | 927 | 0 | 0 | 0 |

O agregado abrange 248 classes e não soma testes repetidos. Não é uma execução única de 1.030 casos diferentes. Os 51 testes inicialmente ignorados foram exercitados no complemento gráfico. Não foi necessário alterar código nem repetir testes por falha nesta auditoria.

Arquivos de evidência sob `.task-checkpoints/CORUJAO-IX/TASK-223/`:

- `full-headless.log` e `headless-reports/`: suíte geral e seus XMLs.
- `gui.log`, `gui-tests.txt`: complemento com Swing ativo.
- `final-reports/`, `verification.json`: XML mais recente por classe e agregado.
- `suite-runtime/`: capturas locais produzidas pelos testes, incluindo observador.
- `suite-generated/`: três PNGs sintéticos criados por testes históricos, arquivados para restabelecer as exclusões preexistentes.

## O que os testes demonstram

- `RpgSkillEngineTest`: 40 casts por fábrica dinâmica, além dos casos específicos de trajetória/contato/geração, controle, corrente, DoT, combo coletivo, Sentinela/AGI, raio intangível, área e suporte. Consulte a matriz para a cobertura individual; a fábrica sozinha não prova todos os totais de dano ou combinações de estado.
- `SkillProtocolTest`: round-trip, validação malformada, dois clientes reais, recargas privadas, aplicação 6 junto da 7 sem receber os frames novos, recusa no backend Coop e tempo global em late join.
- `LightningFieldServerTest`: dois clientes TCP, aliado cruzando corpo invisível, imunidade na fase e retorno/end event para ambos.
- `SkillDisplacementServerTest`: body slam diagonal legal com dois clientes TCP, HP/stun e servidor vivo.
- `NetworkSkillPresentationTest`: 40 clips/alpha, recortes, impactos, deduplicação, direção, ciclo dos vórtices, chuva, dashes e fase de raio.
- Classes gráficas de mira/input/hotbar, Sentry, grab/dash, lifecycle, slash e impactos: interação local com duas janelas, foco e descarte. Testes gerais também cobrem sessões, protocolo legado, inventário, missão, mapas e caminhos offline/Coop existentes.

## Sondas de leitura da auditoria

`Audit223.java`, isolado no checkpoint, foi compilado com `javac --release 17` contra `server/target/classes`. Lê `RpgSkillCatalog` e `NetworkSkillPresentation.load`, resolve a prioridade dos manifestos e grava somente `runtime-catalog.xml` no checkpoint. Resultado: **40 definições e 71 clips carregados**. Não aplica casts, não altera assets e não escreve em saves.

`extract_guide.py` extrai o texto dos streams/fontes do PDF local com biblioteca padrão, sem instalação: **42 páginas**. Artefatos: `guide-pages.json` e `guide-text.txt`. Verificação de nomes/descrições por página em `pdf-skill-checks.json`; comparação dos campos do catálogo auxiliar com o runtime em `guide-vs-runtime.json`, sem diferenças nos campos comparáveis. Essa comparação não transforma os contadores genéricos do PDF em medições de dano.

`build_matrix.py` gera a documentação com dados compilados, assets efetivos e referências de linha verificadas. A classificação é análise explícita, não inferência de cobertura a partir da existência da imagem.

## Preservação e limites

Baseline inicial: 54 M, 2.432 D, 302 entradas não rastreadas. Foram protegidos por SHA-256 **1.792 arquivos** de fontes, testes, assets, ferramentas e guia; verificação final: **zero diferenças**. Todas as 89 capturas preexistentes foram restauradas após arquivar as evidências novas. Três PNGs sintéticos gerados foram movidos para o checkpoint, preservando as 2.432 exclusões iniciais.

Não foram testados fisicamente dois computadores/Radmin, servidor público, launcher distribuído, teclado físico ou desempenho de GPU/CPU sob carga. Nomes de testes contendo Radmin não significam homologação de rede física. Riscos condicionais e lacunas da próxima etapa permanecem no relatório, ainda que esta suíte tenha passado.
