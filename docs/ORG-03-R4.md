# ORG-03-R4 — Captura coordenada no owner multiplayer

## Estado inicial e objetivo

Execução de 11/11/2026, branch `beco`, HEAD `13b91dc`. A R3 estava presente,
com baseline reportado de 1117 aprovados e 57 exclusões headless. Checkout
com muitas alterações preexistentes: status completo, diff inicial, hashes e
checkpoints estão em `temp/org03-r4`. Nada foi descartado, commitado ou publicado.

Foram lidos AGENTS.md, skill de engenharia do servidor, matriz/gate R3, POM,
fontes de simulação/transporte/revisão e testes pertinentes. O monitor tem um
status antigo sem pendências declaradas; os TXT locais conhecidos de IDE/World
Studio permanecem fora deste recorte. Não se inferiu resolução desses erros.

Objetivo: obter uma observação imutável de uma participação real no `coop-world`,
sem exportação TCP, persistência, instalação ou recuperação.

## Diagnóstico da arquitetura

LocalSimulationServer já tem um ScheduledExecutorService de um único owner,
fila `commands` de capacidade 64 e conjunto concorrente de `departures`.
Seu ciclo mantém esta ordem:

1. Drenar departures e executar `world.leave`.
2. Executar até 64 comandos FIFO.
3. Executar `world.step`; RpgSessionWorld processa revisões no início do step.
4. Construir snapshot global; verificar pedidos de desconexão e controles.
5. Construir snapshots privados/públicos de cada peer e sinalizar seus writers.

Uma saída de socket pode ocorrer depois de departures e antes de um comando.
Por isso, a simples presença em `sessions` não basta: a captura verifica também
o peer ativo, `closed`, `connections`, `departures` e pedido de desconexão da
revisão, antes e depois da cópia.

## Implementação mínima

RpgSessionWorld recebeu operação de pacote `captureCharacter(request)`, com
pedido opaco obtido por `characterCaptureRequest(participant)`. O pedido guarda
a Session esperada, contexto de conteúdo, região e domínio. Comparar o objeto
Session evita confundir outra admissão com o mesmo inteiro/UUID efêmero.

LocalSimulationServer registra o Thread real e uma consulta pura de presença
do peer em um único trabalho no executor, antes do primeiro tick. A captura
compara a identidade do Thread, não seu nome. SessionSimulation não mudou.
Não foi adicionada fila nem método de enfileiramento de captura em produção.

Os read models da R3 pertencem a outro pacote Java. A solução escolhida foi
uma ponte tipada `RpgCharacterObservation.read(...)` e visibilidade pública
apenas nos tipos imutáveis necessários. Os métodos de RpgWorld/RpgSkillEngine/
QuestService/AuthoredQuestSession continuam de pacote. Isso evita duplicar o
modelo, expor objetos mutáveis ou criar getters individuais públicos. A R3
continua sendo a implementação de leitura; a documentação R4 atualiza sua
fronteira de visibilidade, antes restrita integralmente ao pacote RPG.

## Contrato e critérios de pronto

O resultado contém observação completa dentro do escopo disponível e metadados
de origem, ou diagnóstico sem observação/metadados parciais. Recusas abrangem
owner incorreto, pedido inválido, ausência/remoção/substituição, desconexão,
conteúdo/região/domínio divergente, transição, binding de skills e falha de leitura.

Clock de engine, tick de mundo e tick de sessão são valores distintos. Os cinco
deadlines de Session.ready usam o tick do personagem, não o clock global do
engine. Número/hash do conteúdo são os efetivamente instalados. UUID de instância
é efêmero e época durável permanece indisponível.

Sem manualAttributes/save/snapshot recuperável, concessão, refresh de missão,
RNG, remoção de skill, limpeza de penalidade ou avanço de simulação na captura.
`economicHistoryComplete()` e `activationAuthorized()` permanecem falsos.
Não foi criado RecoverableCharacterSnapshot no caminho de produção.

## Validação e exclusões

Testes novos usam LocalSimulationServer/LocalClient reais, aplicações 6/7,
loopback efêmero e até duas conexões. A fixture acessa por reflexão somente a
fila existente para inserir FutureTask; não lê nem modifica os campos privados
de Session/RpgWorld/engine. Todas as esperas e medições ficam nos testes.

Os ensaios de fechamento/FIFO usam barreiras temporárias e limitadas no owner
**apenas na fixture**, para controlar o interleaving. A operação de produção não
espera I/O, banco, future ou latch. Há testes de relógios distintos, recompensa
real, imutabilidade, controle adverso e recusa de pedidos antigos.

Fora do escopo: publicação, autenticação, identidade durável, revisão econômica,
recibos, importação, recuperação, PostgreSQL, migração de saves, ORG-04,
TASK-226 e timeout/desconexão A-04. Protocolo, 40 habilidades, assets, launcher,
World Studio e métodos anteriores de gameplay foram preservados.

Ver [matriz](ORG-03-R4-MATRIZ-OWNER-CAPTURE.md),
[autoridade pendente](ORG-03-R4-PENDENCIAS-AUTORIDADE.md),
[testes](ORG-03-R4-TESTES.md) e [relatório](ORG-03-R4-RELATORIO-FINAL.md).
