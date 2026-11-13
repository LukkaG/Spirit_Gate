# ORG-03-R7 — Ambiente PostgreSQL observado

## Inspeção somente de leitura

Observação em 11/11/2026, aproximadamente 23h27–23h28, horário de São Paulo.
Evidências locais: `temp/org03-r7/environment.json` e
`temp/org03-r7/environment-details.json`. Foram consultados SO, PATH, diretórios
conhecidos, registro de programas, serviços e listeners TCP. As únicas execuções
de binários PostgreSQL foram `--version`, sem conexão ou SQL.

| Item | Resultado observado | Limite da conclusão |
| --- | --- | --- |
| Sistema operacional | Windows 10 Pro, 10.0.19045, 64 bits | Inventário desta máquina |
| PostgreSQL no PATH | psql, postgres, initdb, pg_ctl, createdb e dropdb não resolvidos | Isso não significa ausência de instalação |
| Instalação conhecida | `C:\Program Files\PostgreSQL\18` | Registro informa 18.6-3; não foi lido seu diretório de dados |
| Binários disponíveis | Os seis executáveis em `C:\Program Files\PostgreSQL\18\bin`; todos responderam versão 18.6, exit code 0 | Não comprova permissão para criar cluster nem funcionamento de conexões |
| Serviço PostgreSQL existente | `postgresql-x64-18`, Running | Não foi parado, reiniciado, configurado ou acessado |
| Porta existente | 5432 em IPv4/IPv6; PID 3700, processo postgres | Não identifica finalidade, bancos, usuários ou conteúdo; não é alvo do E2 |
| Porta candidata E2 | 55432 sem listener observado | Não está reservada; revalidar antes de executar e abortar se ocupada |
| Docker/Podman | Não resolvidos no PATH; diretórios padrão consultados ausentes; sem serviço/programa correspondente nas consultas | Não prova ausência em toda a máquina; não são necessários para a rota candidata |
| Python | `C:\Python314\python.exe`, 3.14.5 | Biblioteca padrão basta para a rota com psql; harness ainda não existe |
| Java/javac | JDK Temurin 17 disponível; Java 17.0.20.1 | Compatível com release 17 declarado; nenhum build/teste Java executado |
| Maven | `C:\Tools\apache-maven-3.10.0\bin\mvn.cmd`, 3.10.0 | `server/pom.xml` declara JUnit de teste, sem JDBC PostgreSQL; não foi alterado |

Outros listeners observados incluem 3306, 27017 e 33060, além de portas de
sistema/aplicações. O inventário completo fica na evidência local; não se
inferiu produto, finalidade ou autorização de acesso pelo número da porta.
Nenhum desses listeners será reaproveitado. Não foram consultadas senhas,
variáveis de conexão, tabelas, saves ou dados de contas.

## Rota candidata, não executada

**RECOMENDADA, NÃO APROVADA:** reaproveitar somente os binários 18.6 encontrados
para iniciar, futuramente, um **cluster novo independente**, em diretório próprio
e porta distinta, com Python + psql. Não criar apenas um schema no serviço 5432.
Não instalar Docker, driver Python ou JDBC para esse recorte. A versão instalada
é candidata de ensaio, sem decisão sobre versão/topologia de produção D12.

`initdb` cria um novo cluster e recebe o diretório de dados; a escolha de locale
e encoding faz parte dessa inicialização. Isso sustenta propor um cluster novo,
sem usar o data directory existente. [Manual initdb 18](https://www.postgresql.org/docs/18/app-initdb.html).

O servidor permite configurar endereço de escuta e porta no início. A proposta
fixa somente `127.0.0.1:55432`, sem alteração do serviço existente ou regra de
firewall. Loopback limita a rede, mas não substitui credenciais/autorizações.
[Configuração de conexões 18](https://www.postgresql.org/docs/18/runtime-config-connection.html).

Não há falta de binários PostgreSQL detectada. Ainda faltam: autorização do
recorte L01–L05, implementação revisada do harness/SQL exclusivamente sintéticos,
verificação futura de ACL/permissões, espaço disponível, limites e bind, criação
de credenciais próprias e comprovação de isolamento. A viabilidade operacional
de iniciar um segundo processo não foi testada.

## Procedimento futuro: criação, verificação, encerramento e descarte

**PENDENTE DE APROVAÇÃO. Nenhum passo abaixo foi executado.** São requisitos do
executor futuro; não são scripts já existentes nem autorização implícita.

1. Receber os aceites identificados no [escopo E2](ORG-03-R7-ESCOPO-E2.md).
   Revalidar caminhos dos binários, ausência de listener 55432 e espaço mínimo.
   Não tentar 5432 como fallback. Se a porta mudou de dono, interromper.
2. Gerar run-id e criar somente
   `temp/org03-e2/<run-id>/` dentro deste checkout. Resolver caminho absoluto;
   recusar reparse point/junction/symlink em sua cadeia. Se já existir, não
   esvaziar nem reutilizar. Registrar manifesto de propriedade do laboratório.
3. Separar `data`, `evidence`, scripts da fixture e arquivo de credencial com
   ACL restrita ao usuário executor. Usar credenciais novas, aleatórias e só do
   cluster de laboratório. Não copiar pgpass/credenciais do serviço instalado.
4. Usar o initdb encontrado **somente no novo `data`**. Inicializar encoding
   UTF-8 e locale explicitamente registrados; configurar autenticação por senha
   SCRAM para a rota TCP, sem trust amplo. Iniciar processo próprio por pg_ctl,
   sem registrar serviço, com bind loopback/porta candidata e log local sem segredo.
5. A primeira conexão futura deve usar host, porta, database, usuário e arquivo
   de credenciais explícitos, com psql sem arquivo pessoal de inicialização.
   O executor neutraliza defaults de conexão herdados, sem imprimir seus valores.
   Nunca aceitar URI/DSN livre apontando a outro banco.
6. Antes de DDL/fixtures, conferir via conexão futura o data_directory canônico,
   porta, versão, cluster/run-id e database esperados. Divergência aborta. Criar
   apenas database/schema/roles `org03_e2_<run-id-curto>` desse cluster, com nomes
   normalizados. Bootstrap e runner têm privilégios distintos; runner não é
   superusuário. Credenciais de banco não são autenticação de jogadores.
7. Executar somente os testes autorizados, registrar evidências e quotas. Não
   ler/exportar/importar dados do jogo. Não abrir listener para clientes do jogo.
8. Encerrar sessões próprias, solicitar parada ordenada somente do processo
   associado ao manifesto/data directory desta execução e confirmar sua saída.
   Não matar por nome `postgres`, não parar serviço e não liberar porta de outro PID.
9. Dispor somente dos recursos criados por esta execução, se L05 autorizar.
   Antes de remoção recursiva, verificar caminho resolvido dentro da raiz do
   run-id, identidade do manifesto, ausência de links e processo parado. Preservar
   evidências sem credenciais; remover o segredo separado. Se não for possível
   provar propriedade ou parada, reter em quarentena e reportar, sem apagar.

Não haverá DROP DATABASE em serviço preexistente nem comando de limpeza global.
Nenhuma credencial, cluster, script SQL ou recurso E2 foi criado nesta R7.

## Alternativas de ambiente

| Alternativa | Consequência e aprovação separada |
| --- | --- |
| Binários instalados + cluster próprio | Preferida: sem download ou dependência nova; exige L02 e prova futura de isolamento |
| Container dedicado | Só reconsiderar se a rota local falhar; Docker/Podman não foram encontrados nas consultas. Instalação, download de imagem, volume e porta exigem nova autorização |
| Instalação/extração adicional | Não necessária pelo inventário; se requerida, apresentar versão, origem, integridade e impacto antes de instalar |
| Banco/schema no serviço existente | Fora da proposta mínima, pois não atende ao isolamento definido; não é fallback autorizado |
