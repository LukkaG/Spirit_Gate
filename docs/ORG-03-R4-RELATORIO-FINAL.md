# ORG-03-R4 — Relatório final

## Resultado e recorte

Implementada captura interna de pacote em RpgSessionWorld, executada no owner
real registrado pelo LocalSimulationServer. Associação entre Session atual,
RpgWorld, RpgSkillEngine e peer ativo foi exercitada com TCP loopback real e
clientes das aplicações 6/7. Nenhum cliente recebeu uma capacidade de exportar
estado. O resultado contém cópias R3 e metadados de origem, ou recusa sem dados
parciais. Não é save nem RecoverableCharacterSnapshot.

Pedidos são ligados à Session atual, conteúdo, região e domínio. Uma Session
retirada/substituída ou um peer em desconexão não é aceito só porque existe
referência antiga ou UUID conhecido. Skill ausente continua ABSENT/null;
presença com cinco prazos zero continua PRESENT. Intangibilidade permanece
observação, sem instalação de execução. Ambos os gates econômico/ativação
permanecem false.

## Mudanças de produção

| Arquivo | Alteração localizada |
| --- | --- |
| multiplayer/RpgSessionWorld.java | Registro de owner/presença, pedido opaco, captura síncrona de pacote, metadados e diagnósticos |
| multiplayer/LocalSimulationServer.java | Um registro no executor antes do primeiro tick, com consulta de peer/departures/closed/connections; sem comando remoto |
| rpg/RpgCharacterObservation.java | Nova ponte tipada entre os pacotes, sem DTO recuperável |
| rpg/CharacterReadState.java | Visibilidade dos tipos/constructor para consumo pelo coordenador, sem mudar dados/leitura |
| rpg/QuestService.java | Visibilidade de Ledger/ObjectiveRead e constructor imutável, sem mudar leitura ou quests |
| rpg/RpgSkillEngine.java | Visibilidade de tipos imutáveis de timing, sem alterar cast/tick/recargas |

Todos relativos a `server/src/main/java/com/projetok/`. SessionSimulation,
RpgWorld, CharacterReadContext, leitoras R3 e métodos anteriores de gameplay
permaneceram sem alteração de lógica. Não se abriu getter público de mundo,
inventário vivo ou Session; somente tipos de valores ficaram públicos em Java.
A operação coordenada continua restrita ao pacote multiplayer.

Testes novos: OwnerCharacterCaptureTest e RpgCharacterObservationTest. Foram
criados os cinco documentos R4 e atualizado o principal de organização. Sem
dependência nova, protocolo, banco, persistência, assets ou publicação.

## Testes e custos

Rodada dirigida: 282 casos aprovados em 24 classes, sem falhas/erros/exclusões.
Inclui R1/R2/R3, sessões, remoção, skills, atributos/energia, inventário, quests,
progressão e protocolo existente. Suíte geral final: **1190 casos em 278 classes,
1133 executados/aprovados, 57 exclusões gráficas, zero falhas/erros**. Tempo
Maven: 2 min 18 s. São 16 casos adicionais sobre os 1117 aprovados da R3.
Logs, XMLs e summaries estão em `temp/org03-r4`; exclusões estão separadas por
classe/método/motivo em `headless-exclusions.json`.

São 16 testes novos. Na medição dirigida de 100 pares após 20 aquecimentos, com
dois participantes reais no mesmo owner: mediana 68.1 µs, p95 164 µs, máximo 297.1 µs
por par. Na geral final: mediana 36.8 µs, p95 108.7 µs, máximo 203.4 µs por par.
Não é SLA nem orçamento aprovado
para polling permanente. Ver [testes](ORG-03-R4-TESTES.md) para condições e comandos.

## TCP comprovado versus laboratório

Comprovado em TCP: participação atual, associação do mundo/engine, owner real,
protocolos 6/7, ausência versus prontidão de skills, prazos ativos, leitura pura
de snapshots/saldos/ledgers, socket em desconexão, remoção, readmissão com novo
ID, pedido antigo após troca de mundo/conteúdo/região/domínio, ordem comando/
step/snapshot, cópias imutáveis e relógios distintos.

Somente laboratório controlado: ID/UUID reutilizados com outra Session,
presença perdida exatamente na segunda guarda e aplicação direta de ROOT/STUN/
SLOW na ponte. A substituição de conteúdo usa APIs reais em memória do
coordenador dentro do ensaio; não comprova publicação distribuída completa.

Os testes não alegam Radmin físico, launcher nativo ou Swing homologados. As
exclusões gráficas são contabilizadas separadamente. Nenhuma persistência,
reconexão recuperável, migração ou política nova de efeitos foi testada/ativada.

## Autoridade e dados incompletos

O UUID da instância é conhecido, mas efêmero. O ID de participante é de conexão.
Número/hash do conteúdo não viram revisão econômica/personagem. A época durável
continua null. Os cinco deadlines usam worldTick; engineClock e sessionTick são
observados separadamente, sem conversão entre reinícios.

Identidade persistente/posse, recibos, revisão econômica, clock durável e
políticas de efeitos continuam pendentes. `economicHistoryComplete=false` e
`activationAuthorized=false`; o adaptador de laboratório continua incompleto.
Ver [pendências](ORG-03-R4-PENDENCIAS-AUTORIDADE.md).

## Preservação, hashes e riscos

Branch inicial `beco`, HEAD `13b91dc`, sem troca/commit/push. Inventário inicial
de 3737 arquivos, status completo e diff anterior em `temp/org03-r4`; checkpoints
em bytes dos seis arquivos anteriores previstos em `before/`. O diff relativo
ao início desta tarefa está em `task-diff.patch`, e hashes/validação finais nos
JSONs. O diff contra HEAD também inclui trabalho anterior e não é a entrega R4.

As evidências que testes antigos podem regenerar foram preservadas antes das
rodadas; saídas novas são arquivadas antes da restauração individual. R1/R2/R3
documentais e checkpoints anteriores não foram sobrescritos. A-04, 40 skills,
protocolo6/7, World Studio, launcher, saves e PostgreSQL permaneceram fora do
escopo de implementação.

Riscos residuais: callers internos devem respeitar o owner e seu orçamento;
não há consumidor permanente/fila de captura. A vinculação é de uma instância
ao owner dessa execução, sem reatribuição em restart. Código arbitrário com
referências privadas/reflexão não é autenticado por esta API. Desconexão pode
ocorrer depois do ponto de leitura válido; resultado nunca autoriza instalar.

## Rollback e próxima autorização

Revisar o diff incremental e retirar somente as adições R4 em RpgSessionWorld
e o registro de startup em LocalSimulationServer. Reverter os modificadores de
visibilidade dos três modelos, remover a ponte e os dois testes novos e retirar
o adendo/documentos R4 se desejado. Usar `before/` como referência, preservando
mudanças posteriores e trabalho anterior; não restaurar globalmente pelo Git.
Nenhum dado persistente ou formato wire precisa de rollback.

Próximo passo sugerido: decidir os contratos de identidade, economia e tempo
duráveis antes de autorizar qualquer persistência/recuperação. Agendamento
permanente de captura também precisa de orçamento/cancelamento e aprovação
específicos. Não se iniciou ORG-04/TASK-226 nem ativação de A-04.


## Comparação final de hashes

3737 arquivos anteriores inventariados; seis alterações previstas, zero inesperadas ou ausentes.
Lógica anterior dos cinco fontes preservada. 6886 evidências conferidas após restauração, sem diferenças remanescentes.
Os dois TXT A-04 regenerados foram arquivados e restaurados individualmente.

Prefixos SHA-256; valores completos em `temp/org03-r4/affected-hashes.json`.

| Arquivo | Antes | Depois |
| --- | --- | --- |
| ORGANIZACAO-JOGO-2026-11-09.md | 0b381c3fa85c | 6c98e3a97d04 |
| LocalSimulationServer.java | 62ed5dac4371 | bfd444e5df5b |
| RpgSessionWorld.java | 561ce07f6525 | dc1aceaae693 |
| CharacterReadState.java | 25c92ef5adf9 | 7814e961fdc8 |
| QuestService.java | 7a06b3e41a76 | bb083df0054b |
| RpgSkillEngine.java | 9093ece4dfa2 | 642664d86fe0 |
