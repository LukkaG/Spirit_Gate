# TASK-022 - Auditoria inicial e baseline

Data local: 19/10/2026. Situação: APROVADA na automação local.

## Objetivo e diagnóstico

Reproduzir a base 0.21.0-rc1 antes de alterar funcionalidades. Não há repositório Git
neste checkout; preservado o mecanismo tools/checkpoint_rodada.py. A TASK-021
registra 223 JUnit e 18 Python. Nenhuma fonte funcional foi alterada nesta tarefa.

## Inventário e decisões

server/pom.xml: Java 17, JUnit 5.11.4, Surefire 3.5.2; Kryonet e PostgreSQL
permanecem legados e são excluídos da arena portátil. Os pacotes domain e prototype
formam o jogo local. tools contém auditor, empacotador, revisão de sprites,
validação de assets, snapshots textuais e geração PDF sem dependências adicionais.
assets conserva arte candidata; dist preserva os releases históricos.

LocalArenaMap: Properties estrito V1, 960x640, limite de 4096 elementos por lista,
geometria e aparência independentes; validação quadrática de sobreposição.
DebugWorld: proprietário das entidades, colisões e habilidades; limites constantes
acoplados ao mundo e à Criostase. DebugRuntime: thread de simulação, fábrica para R,
fila FIFO e publicação imutável de apresentação. DebugPanel: Java2D, viewport e HUD;
limites históricos repetidos em clipping e desenho. ArenaViewport já aceita dimensões
maiores, mas o painel ainda fornece constantes. Não existe índice espacial paralelo.
DirectionalAnimation publica frames determinísticos. ValidatedSpriteAtlas é o
carregador único, com fallback. Revisão/autoria e pacote visual reutilizam contratos.
O ponto de duplicação relevante são limites de arena e contratos entre validadores
Python e Java; mudanças futuras exigem testes cruzados. Nenhuma remoção foi feita.

## Validações executadas

Java: clean test offline, 223 testes, zero falhas, erros ou ignorados.
Python: unittest discover, 18 testes, todos aprovados.
ZIP original 546ce1c5: auditoria PASS após recompilação; inventário, SHA,
whitelist, Java 17, PE x64 e comparação byte a byte do JAR com classes atuais.
Evidências: TASK-022-java.log, TASK-022-python.log, TASK-022-auditoria.json.
A primeira chamada Maven não recebeu corretamente a propriedade no PowerShell;
reexecutada com argumento inteiro entre aspas e caminho absoluto do cache local.
Não foram alteradas configurações globais.

## Reprodução

Na raiz Projeto-K:
python -m unittest discover -s tools -p "test_*.py" -v
mvn "-Dmaven.repo.local=$PWD/.maven-repository" -o -f server/pom.xml clean test
python tools/auditar_rc.py --zip dist/SpiritGate-0.21.0-rc1-20261019T001737Z-546ce1c5-portable.zip --report docs/TASK-022-auditoria.json

## Arquivos, checkpoint e limites

Adicionados os documentos/evidências TASK-022 e tools/registrar_task.py para
reutilizar PDF e fechamento de checkpoint. Antes: .task-checkpoints/TASK-022-before.json;
depois: TASK-022-after.json e docs/TASK-022.diff. Snapshot inicial contém fontes
textuais recuperáveis; assets e releases anteriores permanecem preservados.
Sem smoke de EXE nesta tarefa; a evidência da TASK-021 é histórica, não reexecutada.
Sem homologação em outro Windows e sem aprovação de arte final.
Próximo passo: TASK-023, orquestrador de qualidade, com testes próprios.

## Resultado final observado
Validação local aprovada: 223 JUnit e 18 Python; zero falhas.
