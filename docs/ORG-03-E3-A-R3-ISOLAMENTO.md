# ORG-03-E3-A-R3 — Isolamento, preservação e descarte

Lucas, 15/11/2026. Run-id `20261115-lucas-e3a-r3-9b62f13a70`.
**Cluster próprio encerrado e dados/segredos próprios removidos com comprovação.**

## Antes do claim

Branch/HEAD/status conferidos; 43 artefatos da preparação e suas fontes iguais
ao pacote revisado, 4.147 arquivos anteriores sem diferenças. Responsável,
data local, run-id, raiz e quatro aceites registrados no manifesto operacional
separado. Não havia data/secrets/claim/evidence operacional anteriores.

Preflight real às **13:58:43 -03**, antes de consumir execution.claim:

- Caminho canônico filho de temp/org03-e3a, cadeia sem symlink/junction.
- SID executor/owner da raiz coincidentes; capacidade de escrita confirmada.
- **126.023.036.928 bytes livres**, acima do mínimo de 2 GiB.
- Binários existentes postgres/psql/initdb/pg_ctl 18.6, SHA-256 iguais aos
  anteriores, sem instalação ou mudança de serviço/configuração existente.
- Nenhum listener em **55435/55436**; bind exclusivo loopback das duas portas
  confirmado e sockets de preflight fechados. Disponibilidade revalidada pelo
  runner antes do uso. Nenhuma porta alternativa.
- Variáveis PG herdadas ausentes; runtime filtra nomes PG e define só valores
  próprios. Nenhuma conexão/consulta/controle dirigido à 5432.

[preclaim-preflight.json](../temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/operational-preflight/preclaim-preflight.json).

Secrets foi criado após o claim; antes de qualquer senha ser escrita, a DACL
foi protegida e conferida com único Allow FullControl para o SID executor,
sem herança. Pgpass/initpw novos e SCRAM; nenhuma senha em argv/transcript.
Metadados de ACL em
[secret-acl.json](../temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/evidence/secret-acl.json).
A raiz de logs não é declarada privada pelo mesmo mecanismo; seus conteúdos
são sanitizados/sintéticos e não contêm os segredos removidos.

## Identidade operacional

Database, cluster_name e roles possuem prefixo **e3a_9b62f13a70**. Bind do
PostgreSQL só em 127.0.0.1:55435; relay só em 127.0.0.1:55436 com upstream
fixo 55435. Postmaster inicial **20112**, criação **16:59:41.0075420Z**;
postmasters seguintes 9340, 4364, 42460 e 40216, sempre no mesmo data.

Antes de DDL e de cada stop: identidade SQL (data, porta, cluster, versão,
database, endereço), executável/comando/criação/PID, postmaster.pid/opts,
pg_ctl status e listener coincidentes. Retornos/ausências após cada stop e
identidade de cada novo start foram registrados. Não se inferiu o alvo só por PID.

Workers/relays pertencem ao controlador **12924**, com PID/pai/criação/
executável/comando e Popen original no registro. As únicas classificações
observadas foram RUNNING_MATCH e EXITED_CONFIRMED; sem mismatch/UNKNOWN.
O endpoint aceito do relay e OwningProcess do socket cliente foram conferidos
antes de armar o permit. Nenhum processo desconhecido foi encerrado.

[process-manifest.json](../temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/evidence/process-manifest.json),
[events.jsonl](../temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/evidence/events.jsonl).

## Encerramento final

Após os controles e SQL global, as sessões foram encerradas. Cluster final
**40216** reidentificado no evento 304. Evento 305 envia exclusivamente
`pg_ctl -D <data próprio> -m fast -w -t 20 stop`; retorno 0. O log registra
fast às **14:01:34.281 -03**, checkpoint e término às **14:01:34.327 -03**.
Evento 306 confirma postmaster e filhos ausentes. O código revisado verificou
status 3, ausência de postmaster.pid/listener, todos os handles terminados,
todos os PIDs registrados ausentes e nenhum lifecycle bloqueado.

Somente depois foram resolvidos e verificados os alvos absolutos filhos
diretos `data` e `secrets`, incluindo recusa de reparse points descendentes;
a remoção atingiu apenas esses dois diretórios. `shutdown.json` registra
stopped=true, discarded=[data,secrets], todos os processos registrados ausentes,
listeners vazios e cleanup_errors vazio. Sem impedimento de parada/descarte.

A auditoria posterior confirmou novamente ausência de data/secrets, ausência
de listeners em 55435/55436 e ausência das identidades originais de workers,
relays e postmasters. Nenhum PID reutilizado foi encontrado nessa auditoria;
ela foi exclusivamente de leitura e não enviou sinais a processos.

## Preservação e resíduos mantidos

Permanecem scripts, preparação, autorização operacional/texto recebido, claim,
306 eventos, 58 snapshots, SQL sanitizado, perfis, logs, recibo arquivado C08,
resultados, hashes e validação de entrega. A árvore pós-descarte contém somente
artefatos da rodada e documentação de preparação; não há cluster/segredo ativo.

SHA-256 final confere **4.147 arquivos anteriores**, os **43 artefatos revisados**
e todas as fontes executadas, sem diferenças. Isso preserva o escopo enumerado;
não é inventário de todos os arquivos pessoais/saves ignorados da máquina.
Nenhuma ação foi dirigida a saves, contas, credenciais preexistentes, serviços,
firewall, instalação, produção ou laboratórios encerrados.

A porta 5432 não foi consultada nem conectada para comprovar o laboratório;
a fronteira é demonstrada pelas portas/identidades fixas, preflight, manifests,
comandos e ausência de fallback. Não se declara uma auditoria de saúde desse
serviço preexistente. Sem commit/push/reset/clean/publicação ou gate posterior.
