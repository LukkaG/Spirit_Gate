# ORG-03-E3-A — Encerramento documental independente

Revisão de 16/11/2026. Responsável indicado pelo usuário: Lucas. Referência:
`20261115-lucas-e3a-r3-9b62f13a70`, encerrado em 15/11/2026.
Repositório `Projeto-K`, branch `beco`, HEAD
`c3d484e76f17c2d93bf0c68ad3ff54280ee8ad39`.

**Conclusão:** as evidências disponíveis sustentam o fechamento técnico da
E3-A-R3 no escopo sintético autorizado. A revisão estática independente passou
em 34 verificações, sem divergência nos manifestos históricos. Isso não fecha
E3 inteira, D33, restore/failover ou integração de jogadores. A referência
aprovada pelo responsável permanece restrita à rodada R3; os resultados falhos
e inconclusivos das rodadas anteriores continuam preservados.

## Método e classificação das afirmações

Foi aplicada a skill local
[spiritgate-server-engineer](../.agents/skills/spiritgate-server-engineer/SKILL.md),
com consulta ao AGENTS, aos planos Corujão VI–IX e à governança ORG-03/R6/R7.
O código operacional e seu validador histórico foram somente lidos. Este último
não foi executado: grava evidências no run-id encerrado e consulta metadados de
processos. A auditoria nova usa apenas biblioteca padrão Python e arquivos.

| Classe | Significado nesta revisão |
| --- | --- |
| Observado diretamente | Conteúdo atual dos arquivos, mensagens completas dos logs, caminhos ausentes e bytes cujo SHA-256 foi recalculado. Não significa testemunho ao vivo da execução passada. |
| Computado agora | Contagens, sequência, hashes, igualdade de imagens e relações entre recibos, operações, grants e revisões, recomputadas pelo auditor novo. |
| Declarado historicamente | Flags PASSOU, configurações/processos capturados, comandos enviados, classificação do runner e conclusão do relatório de 15/11. São confrontados com artefatos correlatos. |
| Não homologado | Garantia que esses artefatos não demonstram: falha física, backup antigo, revogação global, liderança distribuída, autenticação ou produção. |

Evidência reproduzível: [auditoria.py](evidencias/ORG-03-E3-B-REVISAO-20261116/auditoria.py)
e [resultado.json](evidencias/ORG-03-E3-B-REVISAO-20261116/resultado.json).
O resultado inclui inventário SHA-256 integral dos arquivos existentes do run-id,
sem sobrescrever nenhum deles. Não se executou SQL, teste de rede ou processo
operacional nesta revisão.

## Inventário confrontado

Raiz histórica:
`C:/Users/lucas/Documents/GitHub/Projeto-K/temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70`.
Os links a seguir apontam à subpasta `evidence` dessa raiz.

| Afirmação histórica | Conferência independente | Alcance |
| --- | --- | --- |
| C01–C09 PASSOU | Nove resultados individuais iguais às entradas de results.json; imagens e classificações conferidas abaixo | Evidência do laboratório, não nova execução |
| 306 eventos | IDs exatamente 1–306; distribuição por tipo no resultado novo | Integridade estrutural do transcript, não prova criptográfica de completude da captura |
| 58 snapshots | 58 nomes distintos, hash canônico recalculado e integridade relacional conferida | Quatro personagens materializados nas imagens; oito heads na verificação final |
| 450 registros SQL | 450 linhas JSON válidas; SQLSTATE 00000, P0003 e 42501 | Transcript preservado; consultas não foram repetidas |
| Dois crash-recoveries/quatro reinícios | Dois immediate, dois fast com restart, um fast final; quatro excertos confrontados com log completo | Cinco postmasters sucessivos no mesmo data, sem novo cluster |
| Preservação de 4.147 arquivos | Mapa anterior igual ao posterior; todos os hashes atuais iguais | Exatamente o conjunto inventariado; não inventário universal de arquivos pessoais ignorados |
| 43 artefatos preparados/176 selados | Ambos os manifestos recalculados sem divergência | Selo local de integridade; não assinatura externa ou autenticação |

Fontes: [eventos](../temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/evidence/events.jsonl),
[resultados](../temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/evidence/results.json),
[SQL](../temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/evidence/sql-transcript.jsonl),
[selo](../temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/evidence/artifact-sha256.json).

## Resultado de cada cenário

| Caso | Evidência sustentada | Limite relevante |
| --- | --- | --- |
| C01 | PREPARED sem COMMIT; worker antigo encerrado, outro consulta NOT_FOUND; imagem integral anterior igual à posterior | Rollback no mesmo histórico disponível; não ausência demonstrada em backup antigo |
| C02 | Commit observado com resposta retida; reset esperado; permit vinculado à operação; novo worker reconcilia e retry preserva imagem pós-commit | Sucesso do transporte não aprova economia |
| C03 | Crash antes de COMMIT, redo real, novo lookup NOT_FOUND e imagem inalterada; confirmação anterior preservada | Não prova falha de energia/disco |
| C04 | COMMIT_ACK e observação anteriores ao crash; redo real; imagem confirmada e recibos sobrevivem, retry sem efeito novo | Perfil local síncrono, sem restore |
| C05 | Caller ECONOMIC_UNCERTAIN antes do lookup; outro worker encontra CONFIRMED e retry único | Cenário separado de C02; nenhuma confirmação econômica prévia ao corte é presumida para o caller |
| C06 | A confirmado antes do despacho B; restart limpo; somente C06-B-7 identificado pendente, A preservado | Não demonstra recuperação de manifesto ausente no backup |
| C07 | Sequência 2 paga com sequência 1 pendente, prefixo 0; restart; dívida antiga paga uma vez e prefixo 2 | Watermark não substitui recibos nem prova D33 |
| C08 | Arquivo histórico com hash, leitor novo, indisponibilidade produz UNCERTAIN/ARCHIVE_UNAVAILABLE; retry ARCHIVED sem novo prêmio; imagem final igual | Rename reversível do arquivo não equivale a restore de backup |
| C09 | PREPARED classificado NOT_CONFIRMED; ACK classificado CONFIRMED; despacho sem ACK mantém caller incerto, reconciliado depois | Classificações dependem das fronteiras capturadas, não de mero término de processo |

Os JSON individuais C01.json–C09.json estão ao lado de results.json. Em C02/C05
o relay recebeu 1.390 bytes do servidor, encaminhou 1.372 e reteve 18. Os dois
terminais EXPECTED_CLIENT_DISCONNECT têm errno 10054 e termination_requested.
Em C02 o hash do permit coincide com o evento de encerramento. A completion
do transporte continua `ECONOMIC_UNCERTAIN` / `NOT_EVALUATED`; a reconciliação
integral posterior é que sustenta `SCENARIO_PASSED`. C09 não usa essa saída do
relay como critério econômico. A cadeia original inclui identidade, armamento,
ACK privado e encerramento por handles; esta revisão lê a cadeia registrada,
sem repetir encerramentos.

## WAL: revisão independente de C03/C04

O [postgres.log completo](../temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/evidence/postgres.log)
foi lido, e os quatro excertos de recovery correspondem ao seu conteúdo após
normalizar terminadores de linha somente em memória. A identidade e os perfis
registram **PostgreSQL 18.6**. Para interpretar essas mensagens foi consultado
o código da tag exata REL_18_6, e não uma versão recente presumida.

| Caso | Sequência observada nos logs, horário São Paulo |
| --- | --- |
| C03 | Immediate 14:00:01.394; start 14:00:07.464; recuperação automática 14:00:07.747; redo 0/8E8970 → 0/DD5DE0; `unexpected pageaddr` em 0/DD6000, página contendo 0/5D6000; checkpoint de fim; pronto 14:00:08.235 |
| C04 | Immediate 14:00:14.985; start 14:00:20.743; recuperação automática 14:00:21.013; redo 0/DD60C0 → 0/E15B78; comprimento de registro zero em 0/E15BA0; checkpoint de fim; pronto 14:00:21.086 |

O leitor verifica o endereço da página; o comentário oficial associa a
divergência tipicamente à reciclagem ainda não sobrescrita. Também recusa
registro menor que o cabeçalho. Isso explica os diagnósticos, sem provar sua
causa física nesta máquina.
[Fonte oficial 18.6: xlogreader.c](https://github.com/postgres/postgres/blob/REL_18_6/src/backend/access/transam/xlogreader.c#L1233).

O recovery local pode terminar ao não encontrar próximo registro válido; o
caminho difere de standby/recuperação por arquivo. A coincidência do flush LSN
pré-C04 com 0/E15BA0 é contexto adicional, não demonstração isolada.
[Fonte oficial 18.6: xlogrecovery.c](https://github.com/postgres/postgres/blob/REL_18_6/src/backend/access/transam/xlogrecovery.c#L2941).

**Inferência desta revisão:** com redo concluído, checkpoint, disponibilidade
posterior e imagens/recibos preservados, as mensagens são compatíveis com o fim
do WAL válido na rodada. A formulação limitada do relatório anterior é
sustentável. Não há PANIC/FATAL no log; isso, isoladamente, não excluiria
corrupção. WAL físico já foi descartado com o data: não há auditoria física
nova possível sobre ele. Nenhuma mensagem foi suprimida.

O texto de checkpoint `shutdown immediate` não é a classificação de pg_ctl:
os eventos DB_STOP_DISPATCHED registram os modos immediate, immediate, fast,
fast, fast. C06/C07 permanecem reinícios limpos. Recusas P0003/42501 são
negativas previstas; avisos de ROLLBACK sem transação não são commits perdidos.

## Autoridade, integridade e isolamento

[controls.json](../temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/evidence/controls.json)
registra recusa de escrita antiga P0003, operação positiva com geração/fence 2/2,
CAS antigo CONFLICT, retry histórico de C02 e negativas 42501 para leitura
direta/acesso entre proprietários. As flags superuser/createdb/createrole/
replication dos papéis testados são falsas. A bijeção dos snapshots preserva
origem/GrantId, operação, recibo, imagem e revisão, inclusive resultado arquivado.

[final-integrity.json](../temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/evidence/final-integrity.json)
registra 11 recibos = 11 operações = 11 grants = soma EconomicRevision 11;
órfãos de operações/grants/heads e transações pendentes são zero. Heads:
personagem 1 em SR/ER 8/8, geração/fence 2/2; 2–4 em 1/1; 5–8 em 0/0.
São consistentes com as imagens; não certificam todos os escritores reais.
O próprio controle declara **D33 NOT_TESTED**.

Identidades históricas apontam exclusivamente a data do run-id, cluster
`e3a_9b62f13a70`, PostgreSQL 127.0.0.1:55435 e relay :55436. Os cinco perfis
mantêm fsync, synchronous_commit e full_page_writes on e relações logged.
Não se consultou o PostgreSQL :5432 nesta auditoria; a ausência de conexão,
consulta ou controle a ele é também o escopo declarado nos registros anteriores,
não uma inspeção atual de saúde desse servidor.

## Encerramento e limites de preservação

[shutdown.json](../temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/evidence/shutdown.json)
registra fast final, ausência dos processos registrados/listeners antes de
descartar exclusivamente data/secrets e nenhum cleanup_error. Esses diretórios
continuam ausentes na leitura atual. Os handles originais e identidades
históricas sustentam o encerramento então; não se procuraram PIDs antigos agora
nem se encerraram processos. Capturas stderr: 30 completas, sem truncamento ou
erro de leitura; o rótulo RELAY_STDERR também inclui workers.

A rodada consumiu 139,104 s, 15 intenções, 28 workers, dois relays, no máximo
três tentativas/intenção e pico registrado de 45.130.189 bytes. O inventário
histórico de entrega registra aproximadamente 3,77 MiB de logs/evidências,
dentro de 32 MiB; árvore final aproximadamente 3,96 MiB. O espaço inicial
registrado excedia 117 GiB. São medidas históricas; a revisão não mediu capacidade
operacional atual. O limite de quatro conexões é sustentado pela estrutura do
harness, não por amostragem contínua independente de conexões SQL.

Os 4.147 hashes anteriores, 43 artefatos preparados e 176 do selo final
permanecem iguais. SHA-256 comprova preservação dos bytes listados; não aprova
semântica, não recupera dados descartados e não atesta arquivos fora do inventário.

## Divergências e limitações registradas

A primeira passagem do auditor novo marcou quatro comparações de excertos como
falhas: comparou CRLF preservado em JSON com LF normalizado por read_text.
O diagnóstico foi corrigido somente no auditor desta revisão. A
[primeira passagem](evidencias/ORG-03-E3-B-REVISAO-20261116/resultado-primeira-passagem.json)
foi preservada; a segunda passou nas 34 verificações. Não houve divergência
de bytes históricos nem correção retroativa do laboratório.

Os documentos antigos que dizem “E3-A não aprovada” descrevem suas próprias
rodadas interrompidas. Não foram atualizados para esconder falhas. O fechamento
atual referencia explicitamente R3, sem reclassificá-los.

Permanecem não homologados D33, backup/restore/failover, relógio durável, lease,
identidade/posse reais, revogação do ator, falha elétrica/física, RPO/RTO e
políticas econômicas definitivas. [Plano E3-B](ORG-03-E3-B-PLANO.md) e
[pendências](ORG-03-E3-B-PENDENCIAS.md) delimitam a próxima revisão humana.
