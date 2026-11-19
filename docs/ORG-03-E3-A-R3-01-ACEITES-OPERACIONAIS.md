# ORG-03-E3-A-R3-01 — Aceites operacionais pendentes

**PARA REVISÃO — R3-02, R3-03, R3-04 e R3-05 NÃO AUTORIZADOS.**
Somente R3-01 foi aceito. Este arquivo não constitui autorização operacional.
Responsável proposto: Lucas. Preparado em 15/11/2026.

## Identificação comum a conferir no aceite

Run-id reservado: `20261115-lucas-e3a-r3-9b62f13a70`.
Raiz: `C:/Users/lucas/Documents/GitHub/Projeto-K/temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70`.
Data: `<raiz>/data`; segredos: `<raiz>/secrets`; scripts: `<raiz>/scripts`;
evidências futuras: `<raiz>/evidence`; preparação preservada: `<raiz>/preparation`.
Portas candidatas fixas novas: **127.0.0.1:55435** (PostgreSQL) e
**127.0.0.1:55436** (relay, upstream somente 55435).
Disponibilidade não medida. As portas anteriores 55433/55434 não serão usadas.

Se a execução ocorrer em outra data, o pacote precisa de novo run-id/caminho,
configuração/revisão/hash e aceite exatos antes do início. Não reaproveitar claim
consumido. Não escolher portas alternativas automaticamente.

## R3-02 — Criação do único cluster

Aceite solicitado: criar **exatamente um cluster descartável**, com binários
PostgreSQL existentes, somente no data/portas acima, depois de revalidar
isolamento, caminhos sem reparse points, hashes/versões dos binários, ausência
de listener e bind exclusivo, SID/ACL de secrets e pelo menos 2 GiB livres.
Nenhuma instalação, serviço Windows, conexão ou controle do PostgreSQL 5432.
Identidade SQL + SO completas antes de DDL. Sem initdb de reparo ou segundo cluster.

Estado: **PENDENTE**. Nenhum initdb/start executado.

## R3-03 — Cenários e processos próprios

Aceite solicitado: executar **C01–C09** na ordem revisada, com até três workers
simultâneos/32 totais e um relay simultâneo/duas instâncias totais para C02/C05.
Quatro conexões SQL simultâneas; bootstrap privilegiado separado. Encerrar
somente processos próprios pelos handles originais e manifestos/identidades,
sem kill por nome/PID avulso, sem ignorar mismatch/UNKNOWN.

O permit exige socket/worker/conexão/OperationId/fase vinculados, RUNNING_MATCH
recente, pedido real de terminate e ACK privado antes da ação. ECONOMIC_UNCERTAIN
continua durável. C02 requer reconciliação completa; C05 e C09 têm provas
independentes. Conclusão do transporte não autoriza aprovação econômica.

Estado: **PENDENTE**. Nenhum cenário operacional executado na R3.

## R3-04 — Interrupções e recuperação

Aceite solicitado: até **dois crashes**, exclusivamente
`pg_ctl stop -m immediate` no cluster próprio, em C03/C04; até **quatro reinícios**
além do start inicial, sempre no mesmo data, sem novo initdb/fallback.
Stop até 20 s; start/recovery até 45 s. Provar redo no log, identidade revalidada
e imagem/recibos após recovery. Não chamar fast/clean restart de crash.

Estado: **PENDENTE**. Zero crash/restart/recovery na R3.

## R3-05 — Evidências, parada e descarte

Aceite solicitado: registrar evidências sanitizadas e consultas de verificação,
encerrar workers/relay próprios, parar o cluster em **fast** após identidade e
descartar apenas data/secrets próprios após status 3, ausência de postmaster.pid,
postmaster/filhos/handles/listeners e nenhuma identidade bloqueada.
Na dúvida, conservar recursos/evidências para decisão humana; sem destruição
alternativa. Preservar scripts, claim, aceite, logs, hashes e falhas intermediárias.

Estado: **PENDENTE**. Nenhuma parada/descarte operacional executado na preparação.

## Quotas comuns obrigatórias

| Recurso | Teto |
| --- | --- |
| Cluster / immediate / reinícios | 1 / 2 / 4 |
| Personagens / proprietários | 8 sintéticos / 2 |
| Intenções / tentativas por intenção / payload | 64 / 3 / 16 KiB |
| Workers simultâneos / totais | 3 / 32 |
| Relay simultâneo / instâncias totais | 1 / 2 |
| Conexões SQL | 4; bootstrap separado |
| Prazo | 120 s/cenário; 1.200 s totais |
| Stop / start-recovery / CIM / wait | 20 s / 45 s / 8 s / 5 s |
| Armazenamento total / evidências | 256 MiB / 32 MiB |
| Espaço livre inicial mínimo | 2 GiB |
| Stderr / buffer retido TCP | 64 KiB / 16 KiB |
| Stdout | Linha até 16 KiB relay / 256 KiB worker; fluxo 4 MiB por processo; fila 64 linhas |

Exceder um teto interrompe; não expandir para obter aprovação. Medição periódica
não é quota física. A entrada preparada requer os **quatro aceites** antes de
consumir o claim, para que a rodada tenha autorização de encerramento desde o início.

O registro operacional futuro é separado, em `<raiz>/operational-authorization.json`,
e deve refletir aceites humanos explícitos, responsável, data, run-id, caminho,
host, portas e exatamente os limites de `scripts/config.py`. Esse arquivo não
foi criado e não deve ser fabricado por testes ou inferência do aceite R3-01.

Nenhum aceite inclui produção/gameplay, autenticação do jogo, migração/recuperação
de personagens, E3-B/E4/E5, A-04, ORG-04, TASK-226, instalação, commit, push,
reset/clean ou publicação. Gate continua não aprovado até evidência completa
de todos os casos/controles obrigatórios, sem falhas ou inconclusivos.
