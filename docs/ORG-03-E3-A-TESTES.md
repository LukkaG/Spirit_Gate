# ORG-03-E3-A — Testes realizados e limites das evidências

Responsável Lucas, 14/11/2026. Run-id `20261114-lucas-e3a-5fbb9a8beb`.
**1 PASSOU, 1 FALHOU no harness, 7 NÃO EXECUTADOS.**

## Comandos e verificações efetivas

Na preparação, ast.parse conferiu cinco Python sem executar seus módulos.
Git/versões/listeners/volume foram inspecionados; hashes E2 conferidos por
hashlib.sha256. A execução operacional foi uma única chamada:

```powershell
python temp/org03-e3a/20261114-lucas-e3a-5fbb9a8beb/scripts/tests.py
```

Saída observada: `C01 PASSOU`, depois
`INTERROMPIDO WORKER_IDENTITY_MISMATCH`; processo controlador retornou código 1.
Não se reexecutou o comando, não se retirou execution.claim e não se criou
outro cluster. Nenhum Maven/Java, teste físico/Radmin/launcher ou suíte E2
foi executado. O POM consultado confirma Java 17/JUnit; não houve mudança Java.

## C01 — Falha real antes do commit

Cliente psql PID 27188, backend PostgreSQL 2592, aplicou agregado C01-1 dentro
de BEGIN sem enviar COMMIT. Barreira PREPARED_WITHOUT_COMMIT registrou txid 798;
observador independente continuou vendo a imagem anterior.
Cliente foi encerrado após identidade exata; log PostgreSQL registrou EOF
com transação aberta. Observador comprovou desaparecimento do backend.
Novo cliente PID 39128 consultou o mesmo OperationId e recebeu NOT_FOUND.

Imagens completas antes/depois dos quatro personagens do owner 1 são iguais;
nenhuma mudança de revisões, recursos, direitos, recibos, operações ou grants.
O fim do cliente sozinho não foi usado como prova de rollback.
[C01.json](../temp/org03-e3a/20261114-lucas-e3a-5fbb9a8beb/evidence/C01.json).

## C02 — Prova parcial e falha do harness

Relay real PID 27724 em 55434 pausou servidor→cliente antes de COMMIT.
Cliente PID 34660/backend 8160 enviou COMMIT de C02-2. Observador viu
CONFIRMED, sr=er=1, XP=91, moedas=102 e missão=true; chamador ficou UNCERTAIN.
Contador do relay: 1.371 bytes encaminhados antes/depois, 1.389 recebidos,
18 retidos. A confirmação permaneceu bloqueada no socket.

Após encerrar o cliente identificado, a inspeção de identidade do relay
divergiu e bloqueou o kill. O relay foi observado encerrado na limpeza.
Não se chegou ao novo worker/lookup/retry do cenário; portanto **C02 FALHOU**,
sem promoção da prova parcial a aprovação de idempotência após restart.
[C02.json](../temp/org03-e3a/20261114-lucas-e3a-5fbb9a8beb/evidence/C02.json),
[análise](../temp/org03-e3a/20261114-lucas-e3a-5fbb9a8beb/evidence/ANALISE-FALHA-C02.md).

## Rastreabilidade e oráculos

[Transcript SQL](../temp/org03-e3a/20261114-lucas-e3a-5fbb9a8beb/evidence/sql-transcript.jsonl)
conserva backend, SQLSTATE, comandos e resultados sintéticos.
[Eventos](../temp/org03-e3a/20261114-lucas-e3a-5fbb9a8beb/evidence/events.jsonl)
ordenam barreiras, identidades, contadores e término dos processos.
Intenções foram gravadas com flush/fsync antes de envio; snapshots incluem
hash e comparação integral. lab.export autoriza por beneficiário e expõe
versões, recibos, operações, grants, cabeça, recursos e obrigações.

Os invariantes de C01 foram verificados em snapshots observados. A consulta
global final de integridade, controles de fence/CAS/histórico, C03–C09 e retry
de C02 não foram alcançados. Não declarar zero órfãos globais ao término com
base em uma consulta que não foi executada. Encerramento de conexões é
comprovado pela ausência dos processos do cluster, não por esse SQL final.

## Validação da entrega

Conferência de scripts executados contra source-sha256, links/UTF-8, resultados,
consumo de quotas, hashes anteriores e ausência de data/secrets/listeners em
[delivery-validation.json](../temp/org03-e3a/20261114-lucas-e3a-5fbb9a8beb/evidence/delivery-validation.json).
Essa validação documental não altera o resultado técnico da rodada.
