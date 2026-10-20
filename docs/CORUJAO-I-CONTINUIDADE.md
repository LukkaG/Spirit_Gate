# Corujão I — continuidade

Consultar CORUJAO-I-RELATORIO-FINAL.md, CORUJAO-I-INVENTARIO.md, CORUJAO-I-HOMOLOGACAO.md e CORUJAO-I-PENDENCIAS.md. O progresso por etapa e as contagens reais estão em CORUJAO-I-PROGRESSO.json.

Os documentos históricos da TASK-079 anterior foram preservados. A nova TASK-079 do Corujão está em `docs/corujao-i/`. As demais tarefas desta rodada estão diretamente em docs. Checkpoints recuperáveis e diffs por etapa: `.task-checkpoints/TASK-NNN/corujao-i/`.

Recompilar/validar: `python tools/qualidade.py --offline --task identificador-novo`. Usar identificador novo para preservar os logs existentes. Java 17; POM em server/pom.xml. Nenhuma dependência foi acrescentada.

Navegador: game-src/game-resources são cópias explícitas da produção desta rodada. Manter as adaptações de BrowserLauncher, Random do template e filesystem /str ao sincronizar. Executar `python build.py`, `python check.py` e o QA antes de usar o fluxo Sites. O atualizador continua com distribuição separada; não publicar automaticamente os pacotes desta rodada.

Parar na TASK-090 e aguardar homologação do responsável. Não iniciar TASK-091, ampliar multiplayer ou declarar a arte definitiva.
