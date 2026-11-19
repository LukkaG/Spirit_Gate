# ORG-03-E2 — Isolamento e descarte

Execução de referência: `20261114-lucas-600aa9e066`, em 14/11/2026. Responsável: Lucas.
Branch `beco`; HEAD `c3d484e76f17c2d93bf0c68ad3ff54280ee8ad39`.
Autorização explícita L01–L05 recebida nesta conversa, com seis ressalvas;
registro local em [authorization.json](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/authorization.json).
As políticas definitivas D14–D22, D27 e D34 não foram aprovadas por este aceite.

## Recursos próprios

| Item | Configuração/resultado |
| --- | --- |
| Binários | C:/Program Files/PostgreSQL/18/bin; versão 18.6 e SHA-256 conferidos para postgres/psql/initdb/pg_ctl |
| Bind | Exclusivamente 127.0.0.1:55432; nenhum fallback |
| Dados | temp/org03-e2/20261114-lucas-600aa9e066/data, criado novo e já removido |
| Namespace | cluster/database prefixados e exclusivos; cluster_name e data_directory conferidos |
| Autenticação | SCRAM-SHA-256 TCP; hba somente 127.0.0.1/32; sem trust amplo |
| Segredos | Aleatórios e exclusivos; diretório com herança de ACL removida e permissão do SID executor; já removido |
| Processos | pg_ctl/postmaster próprios; PID conferido com listener e postmaster.pid; sem serviço Windows |
| Configuração | UTF-8, locale C, shared_buffers 16 MB, max_connections 8, WAL 1 MB/segmento, min/max WAL 16/64 MB |
| Timeouts declarados | connect 3 s, statement 5 s, lock 1 s, idle transaction 5 s, transaction 10 s; deadlock usa lock local de 4 s |
| Limites observados | 44,199,486 bytes de pico; 108,612,644,864 bytes livres antes; 24.24 s após bootstrap |

Variáveis PG herdadas foram neutralizadas sem imprimir valores. Todos os
psql usam `-X -w -h 127.0.0.1 -p 55432 -U <role própria> -d <banco próprio>`.
Senhas não foram passadas na linha de comando; bootstrap de role envia o
segredo por stdin, fora do transcript, com log_statement desativado e sem
log de SQL em erro. O pgpass exclusivo ficou no diretório protegido.
Nenhum banco/save/credencial/diretório de dados preexistente foi utilizado.
O banco postgres consultado na identificação foi o recém-criado por initdb
no cluster do laboratório, não o serviço instalado.

## Identidade antes de qualquer DDL

O primeiro SQL verifica data_directory resolvido, server_version, port,
cluster_name, database e inet_server_addr. São comparados PID do postmaster,
listener e manifesto. O schema só é criado após a comprovação do alvo e a
criação do database próprio, cuja identidade também é conferida pelo harness.
Reparse points, junctions e symlinks são recusados na cadeia dos caminhos.
Permissão/porta/identidade divergentes interrompem sem alternativa implícita.

## Menor privilégio e recursos não acessados

Bootstrap administrativo separado; máximo de três sessões de teste e nenhuma
sessão administrativa sobreposta. Owner 1/2 são logins sintéticos diferentes.
O runner não é superusuário, não cria roles/database/schema e não escreve
tabelas diretamente. C11 registra as recusas 42501 e as flags efetivas.
Não foram executados comandos/conexões dirigidos à porta 5432, Stop-Service,
firewall, instalação, acesso a saves ou comandos por nome de processo.

## Encerramento comprovado

As sessões próprias receberam ROLLBACK/quit. Depois da confirmação de identidade
e PID, pg_ctl stop usou somente o data_directory desta execução, modo fast
(parada ordenada com encerramento das sessões próprias) e prazo 15 s.
Resultado: código 0; pg_ctl status 3, postmaster.pid ausente, PID ausente e
nenhum listener 55432. Só então shutil.rmtree removeu os alvos absolutos
`data` e `secrets`, com checagem de parentesco com run-id e ausência de links.
Nenhuma pasta temp completa foi removida.

[shutdown.json](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/shutdown.json) comprova o resultado.
Todas as tentativas anteriores também têm encerramento e descarte registrados.
Os logs PostgreSQL preservados contêm apenas dados sintéticos e eventos do
laboratório, sem senhas. A [guarda](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/single-use-guard.json)
recusou reexecução e manteve os hashes das evidências intactos.

O limite de disco é observado entre lotes, não imposto pelo filesystem.
Este ensaio não é prova de segurança contra outro processo local com privilégios
do mesmo usuário nem qualifica topologia, WAL ou RPO/RTO de produção.
