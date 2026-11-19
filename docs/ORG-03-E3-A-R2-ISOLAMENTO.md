# ORG-03-E3-A-R2 — Isolamento e descarte

Lucas, 15/11/2026. Run-id `20261115-lucas-e3a-r2-8c71d4a902`.

## Alvos exclusivos e preflight

Caminho canônico da rodada:
`C:/Users/lucas/Documents/GitHub/Projeto-K/temp/org03-e3a/20261115-lucas-e3a-r2-8c71d4a902`.
Somente os filhos diretos `data` e `secrets` foram criados e descartados.
Cadeias sem symlink/junction verificadas antes do uso/descarte. Claim de uso
único preservado. Um único initdb, sem reutilização de cluster anterior.

Binários existentes em `C:/Program Files/PostgreSQL/18/bin`, versão 18.6;
hashes postgres/psql/initdb/pg_ctl em
[preflight.json](../temp/org03-e3a/20261115-lucas-e3a-r2-8c71d4a902/evidence/preflight.json).
Espaço livre inicial 127.303.045.120 bytes. Nenhuma variável PG herdada encontrada;
o runtime neutraliza nomes PG e define apenas os valores próprios registrados.

5432 foi inspecionada apenas por metadados TCP: listeners `::` e `0.0.0.0`,
PID 3700, antes e depois. Nenhuma conexão SQL, parada ou alteração dirigida
a esse serviço. 55433/55434 sem listener antes do uso e binds exclusivos
Windows revalidados, sem fallback.

Segredos novos, fora do argv/evidências SQL: SCRAM e pgpass em secrets com ACL
protegida, um único Allow FullControl para SID executor
`S-1-5-21-795864056-262458669-780167792-1001`, sem herança. Conferência efetiva em
[secret-acl.json](../temp/org03-e3a/20261115-lucas-e3a-r2-8c71d4a902/evidence/secret-acl.json).

## Identidade antes de DDL e controle

Postmaster PID 12472, criado 07:05:39.7749330Z, binário postgres.exe conferido,
comando -D no data exclusivo; postmaster.pid/opts, pg_ctl status, listener
127.0.0.1:55433 e identidade SQL coincidentes. Cluster/database
`e3a_8c71d4a902`, roles novas com mesmo prefixo. Asserções antes do primeiro DDL,
depois no database criado e antes do fast, eventos 1, 2 e 47.

Workers/relay registrados por PID, pai 12056, comando, executável, criação e
handle original. Somente workers 48632/47772 receberam terminate, após
RUNNING_MATCH integral. Workers 32116/15372 encerrados normalmente pelo stdin
próprio. Relay 49892 saiu espontaneamente com exit 1; nenhum terminate enviado
a ele, nenhum processo escolhido por nome/PID avulso.

## Perfil efetivo e limites

[profile-initial.json](../temp/org03-e3a/20261115-lucas-e3a-r2-8c71d4a902/evidence/profile-initial.json):
fsync/synchronous_commit/full_page_writes on; relações lab logged;
READ COMMITTED; shared_buffers 16 MiB; max_connections 8; harness com no máximo
dois workers simultâneos nesta rodada e limite autorizado de quatro SQL.
Bootstrap privilegiado separado das sessões normais. WAL com segmento 1 MiB,
min/max 16/64 MiB, archive_mode off. Timeouts de statements/transações próprios.
As flags de privilégios foram configuradas, mas seus testes negativos finais
não foram alcançados. Não apresentar configuração como teste negativo passado.

Pico medido 42.797.893 bytes; quatro workers totais/um relay, duas intenções,
uma tentativa cada. Limites de tempo/disco não excedidos. A medição periódica
não equivale a quota física; NTFS/cache físico/controlador não homologados.

## Prova de parada e descarte

Após encerrar clientes, a identidade completa do cluster foi revalidada.
Evento 48 envia exclusivamente `pg_ctl -D <data próprio> -m fast -w -t 20 stop`.
Retorno 0; postmaster.pid ausente, pg_ctl status 3, listener 55433 e postmaster/
filhos conhecidos ausentes, antes de marcar DB_STOPPED no evento 49.
Todos os handles registrados confirmaram saída, todos os respectivos PIDs
estavam ausentes e não havia lifecycle bloqueado antes do descarte.

Somente depois: alvos absolutos resolvidos como filhos diretos próprios,
recusa de reparse points em descendentes e remoção de data/secrets.
[shutdown.json](../temp/org03-e3a/20261115-lucas-e3a-r2-8c71d4a902/evidence/shutdown.json).
O [postgres.log](../temp/org03-e3a/20261115-lucas-e3a-r2-8c71d4a902/evidence/postgres.log)
registra fast às 04:05:52.944 -03, checkpoint e encerramento às 04:05:53.668 -03.
`checkpoint starting: shutdown immediate` é texto do checkpoint de parada;
o modo enviado foi fast, não um crash. Zero immediate/reinícios/recovery.

Auditoria posterior: data/secrets ausentes, 55433/55434 sem listener, processos
originais ausentes. Um PID reutilizado pelo Windows foi distinguido por criação
posterior e pai diferente, somente em leitura; detalhes no diagnóstico.
Metadados 5432 idênticos antes/depois. Nenhuma intervenção no novo ocupante.

## Preservação e fronteiras

SHA-256 de 4.023 arquivos anteriores e das sete fontes executadas conferidos,
sem mudanças. Originais E2/E3/R1 e seus scripts/evidências preservados.
Scripts, evidências sanitizadas, aceite, claim, hashes e diagnósticos R2 retidos.
Nenhum dado/segredo ativo remanescente da rodada. Logs SQL contêm somente
fixtures sintéticas; segredos foram excluídos da gravação de bootstrap sensível;
relay registra metadados/contadores, nunca payload TCP.

Sem commit, push, publicação, instalação, produção/gameplay ou gates posteriores.
A validação de entrega comprova esses artefatos/recursos enumerados; não constitui
auditoria global de todo o computador nem aprovação técnica de E3-A.
