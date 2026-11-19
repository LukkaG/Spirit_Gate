# ORG-03-E3-A — Isolamento e encerramento efetivos

Responsável Lucas, 14/11/2026. Run-id `20261114-lucas-e3a-5fbb9a8beb`.

## Recursos próprios e identidade

Binários PostgreSQL 18.6 existentes em `C:/Program Files/PostgreSQL/18/bin`;
postgres/psql/initdb/pg_ctl conferidos por versão e SHA-256 no preflight.
Python 3.14.5, biblioteca padrão, sem instalação/driver novo.
Data exclusivo criado e depois removido:
`temp/org03-e3a/20261114-lucas-e3a-5fbb9a8beb/data`.
Database, cluster_name e roles com prefixo `e3a_5fbb9a8beb`.

Postmaster PID 28104; bind somente `127.0.0.1:55433`.
Relay PID 27724, bind `127.0.0.1:55434`, upstream fixo 55433.
Novo cluster UTF-8/locale C/SCRAM; pgpass e senhas novos no diretório secrets
com ACL restrita ao SID executor, sem senha em argv ou transcript.
Variáveis PG herdadas foram neutralizadas sem expor valores.

Antes do DDL, comparados data_directory, cluster_name, versão, database,
inet_server_addr/port, postmaster.pid, postmaster.opts, pg_ctl status, listener,
executável, comando, PID e criação do processo. Workers/relay próprios
registrados com PID, criação, comando e relação com controlador PID 45260.
[Identidade SQL](../temp/org03-e3a/20261114-lucas-e3a-5fbb9a8beb/evidence/cluster-identity.json),
[manifesto de processos](../temp/org03-e3a/20261114-lucas-e3a-5fbb9a8beb/evidence/process-manifest.json).

## Perfil efetivamente medido

| Parâmetro | Valor observado |
| --- | --- |
| PostgreSQL | 18.6 |
| fsync / synchronous_commit / full_page_writes | on / on / on; também verificada confirmação síncrona na sessão escritora |
| WAL | replica; segmento 1 MiB; min/max WAL 16/64 MiB; archive_mode off |
| Sincronização WAL | open_datasync; buffers 512 KiB; writer_delay 200 ms; compressão off |
| Checkpoint | timeout 300 s, completion_target 0.9, warning 30 s |
| Memória/conexões | shared_buffers 16 MiB; max_connections 8; harness até 2 sessões normais nesta execução |
| Isolamento | READ COMMITTED |
| Timeouts | connect 3 s; statement 10 s; lock 2 s; idle transaction 30 s; transaction 45 s |
| Relações | Todas as 13 tabelas lab medidas como persistência logged |
| Armazenamento | Volume NTFS; cache físico/controlador não homologados; espaço livre inicial 133.539.946.496 bytes |

[Perfil completo](../temp/org03-e3a/20261114-lucas-e3a-5fbb9a8beb/evidence/profile-initial.json).
Opções habilitadas não comprovam durabilidade após queda: esse ensaio não ocorreu.
COMMIT observado em C02 é distinto do retorno de lab.apply dentro de BEGIN.

## Falha de inspeção e parada segura

Dois clientes foram terminados somente após identidade coincidente. Em C02,
a identidade consultada do relay divergiu do registro e bloqueou o kill;
nenhum método alternativo foi usado. O relay foi observado encerrado na limpeza.
Não se enviou pg_ctl immediate: **zero crashes e zero reinícios**.

O postmaster próprio foi reidentificado e parado por
`pg_ctl -D <data próprio> -m fast -w -t 20 stop`. Provas: retorno 0,
pg_ctl status 3, postmaster.pid ausente, postmaster/filhos próprios ausentes,
workers/relay registrados ausentes e portas 55433/55434 sem listener.
O log PostgreSQL registra fast shutdown, checkpoint de encerramento e término.
A mensagem de checkpoint `shutdown immediate` não transforma esta parada fast
em crash: o modo enviado foi fast e houve checkpoint de encerramento limpo.

Só após essas provas foram removidos os alvos absolutos data/secrets,
validados como filhos diretos canônicos do run-id, sem links/junctions.
Scripts, logs e evidências ficaram preservados. Nenhum dado ativo remanesceu.
[shutdown.json](../temp/org03-e3a/20261114-lucas-e3a-5fbb9a8beb/evidence/shutdown.json),
[log PostgreSQL](../temp/org03-e3a/20261114-lucas-e3a-5fbb9a8beb/evidence/postgres.log).

## Fronteiras preservadas

Sem conexão ou comando dirigido a 5432, serviço Windows, dados/credenciais
preexistentes, firewall ou outro laboratório ativo. Sem instalação, alteração
de POM/runtime/protocolo/saves/contas ou publicação. O manifesto do projeto/E2
é conferido na entrega; seu escopo não inclui todo arquivo pessoal ignorado.
Nenhum processo foi encerrado por nome genérico.

Não há evidência de crash-recovery, restore, failover, perda física ou RPO/RTO.
O procedimento immediate autorizado não chegou a ser usado. Referência técnica
para distinguir modos: [pg_ctl 18](https://www.postgresql.org/docs/18/app-pg-ctl.html).
