# TASK-029 - Robustez dos pacotes visuais

Objetivo: corrigir lacunas reais no carregador e staging existentes.
Base: TASK-028, 230 JUnit e 25 Python aprovados.
Diagnóstico: já havia controle de versão, SHA/revisão, grade, recortes, duplicação
de frames, fallback legado individual e carga única sem IO no draw. Lacunas:
Properties aceitava chave duplicada; offsets inteiros extremos; arquivos sem teto
e decodificação de pixels antes de validar dimensões; caminho indireto externo.
Correções em ValidatedSpriteAtlas: Properties estrito, manifest até 1 MB, PNG até
16 MB, dimensões até 4096 por eixo inspecionadas por ImageReader antes de decode,
offset até 4096 por eixo e conferência de caminho real dentro da pasta do pacote.
validar_assets.py compartilha limite de offset para staging recusar antes da cópia.
Não foi criado carregador paralelo ou cache global com recursos sem ciclo de vida.

Testes: Properties duplicado, offsets extremos (inclusive MIN_VALUE), arquivo
excessivo, negativo de staging. Regressões existentes incluem remoção da fonte após
load e desenho ainda funcional, ausente/corrompido, hashes, pivots e versão.
Resultados: TASK-029-qualidade.json, TASK-029-testes.txt, logs reais completos.
Reprodução: python tools/qualidade.py --offline --task 029
Arquivos/diff e checkpoints padronizados; próxima TASK-030.
Limites: testes de distribuição usam fixtures sintéticas em temporários; nenhuma
arte real foi aprovada ou integrada. Execução sem pacote continua com fallback.
Homologação artística e em outro Windows permanece pendente. Limites documentados
podem recusar pacotes antigos excessivos; os contratos 59 frames usuais cabem.

## Resultado final observado
Validação local aprovada: 231 JUnit e 25 Python; zero falhas.
