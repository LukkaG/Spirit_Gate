# ORG-03-E3-A-R3 — Provas de crash-recovery

Lucas, 15/11/2026. Run-id `20261115-lucas-e3a-r3-9b62f13a70`.
**Dois crashes reais de PostgreSQL próprio, dois recoveries comprovados.**
Todos os horários abaixo são de São Paulo (UTC−03).

## Diretório e identidade únicos

Data utilizado em todos os starts:
`C:/Users/lucas/Documents/GitHub/Projeto-K/temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/data`.
Cluster/database `e3a_9b62f13a70`, host 127.0.0.1, porta 55435, PostgreSQL 18.6.
Um initdb e cinco postmasters sucessivos no mesmo data, com comando/criação
e identidade SQL/SO revalidados. O data só foi removido depois da parada final.

| Etapa | Modo de parada anterior | Postmaster | Evidência |
| --- | --- | --- | --- |
| Start inicial | Nenhuma | 20112 | Identidade eventos 1–2. |
| C03 | immediate em 20112 | 9340 | Primeiro crash-recovery, eventos 65–71. |
| C04 | immediate em 9340 | 4364 | Segundo crash-recovery, eventos 82–88. |
| C06 | fast em 4364 | 42460 | Restart limpo, número 3. |
| C07 | fast em 42460 | 40216 | Restart limpo, número 4. |
| Saída final | fast em 40216 | Nenhum | Eventos 304–306, status 3 e descarte. |

O modo enviado foi sempre preservado em DB_STOP_DISPATCHED. Não houve sinal
alternativo, kill por nome, recriação de cluster ou fallback de data/porta.

## C03 — Transação sem COMMIT

O worker aplicou C03-3 (equipamento sintético) dentro de BEGIN; barreira
PREPARED_WITHOUT_COMMIT, backend/worker/txid gravados; observador ainda via
a imagem anterior. **COMMIT não foi enviado.** Após identidade do cluster,
`pg_ctl -D <data próprio> -m immediate -w -t 20 stop` foi enviado.

Log: immediate às **14:00:01.394**; novo start às **14:00:07.464**;
recuperação automática às 14:00:07.747; redo **0/8E8970 → 0/DD5DE0**;
checkpoint de fim de recovery e pronto para conexões às **14:00:08.235**.
Retorno do pg_ctl, ausência de postmaster.pid/processos/listener antes do start
e identidade nova foram verificados.

Novos workers consultaram o mesmo OperationId: **NOT_FOUND**. Imagem completa
antes/depois igual, sem recibo/índice/grant/estado parcial visível. O C02
anteriormente confirmado permaneceu igual. Arquivos:
[C03.json](../temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/evidence/C03.json),
[recovery-C03.json](../temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/evidence/recovery-C03.json).

## C04 — Confirmação antes da interrupção

Worker recebeu COMMIT_ACK de C04-4; observador confirmou recibo, revisões,
recursos e índices, e foi registrada a imagem integral. Flush LSN
**0/E15BA0**. Sem checkpoint manual entre confirmação e corte.

Log: immediate às **14:00:14.985**; novo start às **14:00:20.743**;
recuperação automática às 14:00:21.013; redo **0/DD60C0 → 0/E15B78**;
checkpoint de fim de recovery e pronto às **14:00:21.086**.

Novos workers comprovaram a mesma imagem e os recibos confirmados anteriores;
retry de C04-4 retornou histórico sem novo efeito. Arquivos:
[C04.json](../temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/evidence/C04.json),
[recovery-C04.json](../temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/evidence/recovery-C04.json).

## Diagnósticos preservados no limite do WAL

O recovery C03 registra `unexpected pageaddr` no próximo trecho lido. O código
oficial descreve esse teste como uma situação típica de segmento reciclado
ainda não sobrescrito. O recovery C04 registra comprimento zero ao tentar ler
o próximo registro em **0/E15BA0**, coincidente com o flush LSN observado antes
do corte. O leitor valida endereço/comprimento; o recovery trata a ausência de
próximo registro válido no fluxo local. Referências primárias:
[xlogreader.c, PostgreSQL 18.6](https://github.com/postgres/postgres/blob/REL_18_6/src/backend/access/transam/xlogreader.c#L1233),
[xlogrecovery.c, PostgreSQL 18.6](https://github.com/postgres/postgres/blob/REL_18_6/src/backend/access/transam/xlogrecovery.c#L2967).

**Inferência limitada à rodada:** esses diagnósticos são compatíveis com o fim
válido do WAL após os cortes, considerando redo concluído, checkpoint, estado
pronto e verificação posterior dos recibos/imagens. Não são usados isoladamente
como prova de durabilidade nem como auditoria física de corrupção. Nenhuma
mensagem foi ocultada ou convertida em aprovação automática.

Os avisos e as recusas SQL dos controles estão no
[postgres.log completo](../temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/evidence/postgres.log).
Não houve PANIC/FATAL; os recoveries terminaram e as invariantes consultadas
passaram. O checkpoint de parada fast pode conter o texto `shutdown immediate`;
isso não muda o modo de parada enviado nem transforma C06/C07 em crash.

## Perfil e limites das conclusões

Cinco perfis — inicial e após cada reinício — mantiveram fsync,
synchronous_commit e full_page_writes em on, com as 13 relações lab logged.
O escritor também confirmou suas flags de sessão. Essas opções são contexto;
a prova efetiva é a cadeia barreira/corte/redo/consulta independente.

Permanecem fora: energia/disco/controlador/cache físico, backup/restore,
failover, D33 e SLA/RPO/RTO de produção. Os intervalos acima são instantes
observados de um laboratório local, sem extrapolação para operação real do jogo.
