# TASK-079 — Testes e preservação

Comando: `python tools/qualidade.py --offline --task 079`, na raiz. Exit 0. Maven real, offline, em `server/pom.xml`, executando `clean test`, seguido de unittest Python completo.

| Resultado real | Testes | Falhas | Erros | Ignorados |
| --- | ---: | ---: | ---: | ---: |
| Java | 314 | 0 | 0 | 0 |
| Python | 30 | 0 | 0 | 0 |

A referência da TASK-078 era 307 Java/25 Python. Antes desta tarefa, a integração do atualizador já havia acrescentado um teste Java e cinco Python: base efetiva 308/30. A TASK-079 acrescenta seis testes Java e nenhum Python, resultando em 314/30. Todos os testes anteriores foram preservados; não se presumiu a contagem histórica como resultado atual.

## Cobertura específica

`RpgPlayerRefinementTest`, seis casos:

1. Defaults configurados de 40%/220 ms e retorno a 50%/150 ms via propriedades, sem recompilar.
2. Escalas inválidas, NaN/infinito, duração fora dos limites e propriedades ilegíveis geram fallback. Desativação experimental mantém modo geométrico mesmo com configuração inválida.
3. Os 12 frames em 35%, 40% e 50% conservam o pivô dos pés, centro horizontal e transparência; o desenho cabe em canvas de teste sem cortes nas bordas.
4. Redução proporcional dos dois eixos, com tolerância de rasterização de dois pixels.
5. Durações 220 e 280 ms verificadas por avanço de ticks, estabilidade do frame em repaints repetidos, troca de direção e Idle estável; duração oficial WALK do atlas permanece 0,4 s para quatro frames de 100 ms.
6. As quatro combinações comparativas não alteram o estado lógico em relação a um mundo-controle idêntico. NPC/morte continuam recusados pelo adaptador para manter o fallback existente.

Os testes anteriores de associação das quatro direções, corrupção/ausência/troca de PNG, capitalização exata, cache, pivôs, movimento bloqueado e identidade pixel a pixel do renderer sem hook também passaram. Não foi alterada nenhuma asserção anterior.

O mundo é 30 Hz: a troca é observada no primeiro tick após completar a duração. Para 220 ms, o primeiro frame 2 aparece após sete intervalos (≈233,3 ms); para 280 ms, após nove (300 ms). O cálculo mantém o período médio usando ticks desde o início do estado, sem redefinir o contrato para múltiplos fixos de repaint.

## Evidências

Logs: `docs/TASK-079-java.log`, `TASK-079-python.log`, versões, `TASK-079-qualidade.json` e `TASK-079-testes.txt`. XMLs JUnit finais preservados em `.task-checkpoints/TASK-079/junit/`.

Quatro processos reais de cliente usados na comparação, com assets copiados para pastas de evidência e personagens sintéticos, sem saves pessoais. O harness externo aciona os bindings reais e lê snapshots; não injeta posições ou altera a simulação. Capturas e GIFs são evidências técnicas, não aprovação artística humana. Detalhes em TASK-079-COMPARACAO-VISUAL.md.

Verificação final de hashes e lista exata de modificações em `.task-checkpoints/TASK-079/preservation.json`. O inventário inclui fontes, recursos, ferramentas, documentos existentes e hashes de saves; somente os dois arquivos existentes declarados na tarefa devem divergir. Os novos arquivos próprios aparecem separadamente. Não há alteração dos PNGs ou dos testes anteriores.
