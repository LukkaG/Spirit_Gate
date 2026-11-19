# ORG-03-E3-A-R1 — Análise do relay

## Evidência anterior

Fonte: `temp/org03-e3a/20261114-lucas-e3a-5fbb9a8beb/evidence/`:
`events.jsonl`, `process-manifest.json`, `failure.json`, `shutdown.json`,
`ANALISE-FALHA-C02.md`, além dos scripts preservados.

| Sequência | Observação |
| --- | --- |
| 20 | Relay PID 27724, Python, pai/controlador 45260, criação 17:27:03.2221650Z; argv contém script e run-id exclusivos |
| 26 | PAUSE confirmado; 1.371 bytes já encaminhados |
| 27 | COMMIT enviado pelo cliente |
| 28 | Observador confirmou C02-2 enquanto chamador permanecia incerto |
| 29 | 1.389 bytes recebidos, 18 retidos, encaminhados ainda 1.371 |
| 30–31 | Cliente 34660 encerrado após identidade coincidente; exit 1 |
| falha | `tests.py:135` → `Relay.close` → `Lab.terminate_own`; asserção CIM divergente antes de terminate |
| 32 | Na limpeza, handle original do relay confirmou exit 1; não há CLIENT_TERMINATE para ele |

O objeto Popen original fica no registro do controlador. A identidade esperada
contém PID, pai, criação UTC, executável e comando. O código anterior fazia
`poll() is None`, consultava CIM por PID e comparava o dicionário integral.
Se o processo sai nesse intervalo, a ausência CIM é tratada como mismatch.
Essa janela existe por inspeção do código e foi reproduzida por simulação.
Não há consulta CIM persistida na falha original para provar que ela ocorreu ali.

O relay original retorna em EOF de qualquer lado; reset/recv/sendall/select
podem levantar exceção não tratada. stderr era PIPE, mas não havia leitor.
Exit 1 é compatível com erro não tratado; não identifica qual erro nem prova
que houve reset. A causa transitória específica continua **desconhecida**.

## Estados e prioridade

| Estado | Prova exigida | Ação |
| --- | --- | --- |
| RUNNING_MATCH | Objeto original registrado, PID/pai vinculados, todos os campos presentes, CIM integral igual e polls ativos | Último poll; terminate exclusivamente pelo objeto original; wait limitado |
| EXITED_CONFIRMED | Poll do objeto original confirma código de saída, sem divergência CIM observada | Registra código; nenhum terminate |
| IDENTITY_MISMATCH | CIM não nulo diferente do manifesto | Bloqueia e conserva diagnóstico; recusa pedidos seguintes |
| UNKNOWN | Vínculo inválido, metadata ausente/inacessível ou erro de inspeção/handle | Bloqueia; sem fallback |

Divergência observada tem prioridade mesmo se poll posterior indicar saída.
Quando o primeiro poll já confirma saída, CIM não é necessário: nenhum alvo PID
será usado. Ausência CIM com polls ativos é UNKNOWN; metadata ilegível permanece
UNKNOWN mesmo se o poll posterior indicar saída. O fluxo conserva a escolha
mais restritiva. Timeout de wait bloqueia novas tentativas e não chama kill.

## Diferenças aplicadas às cópias

`Lab.register` associa OwnedProcess ao objeto criado e ao controlador.
`terminate_own` delega a ele; registra esperado/observado, polls, exit, erro,
última barreira e stderr. O encerramento usa lock, novo poll antes da ação,
wait de 5 s e ausência de fallback. Não há kill por nome nem seleção de alvo
por PID. O caso de saída entre CIM e ação não tenta outro processo.

Stderr é drenado por thread, limitado a 65.536 caracteres, com truncamento
explícito e sanitização de atribuições password/pgpassword/secret/token e URI
PostgreSQL. Não registrar SQL/payload/credenciais no protocolo de eventos;
o redator é defesa adicional, não sanitizador universal de texto arbitrário.
Erro de leitura fica explícito. Close espera drenagem por até 2 s.

O loop novo conserva contadores no EOF/erro, distingue origem client/server,
registra tipo/errno sem mensagem de socket potencialmente sensível e relança
exceções. EOF de cliente com exit zero é compatível com o cenário, mas não
aprova C02. Exit não zero ou saída sem EOF esperado interrompe. Close repetido
após fechamento bem sucedido é inerte. O CLI da cópia R1 permanece bloqueado.

## Barreira C02 e limites

A cadeia 1–6 (criação/identidade, PAUSE, COMMIT, confirmação independente,
retenção, fim do cliente) tem evidência parcial anterior. O passo 7 tem falha
de inspeção e saída comprovada somente na limpeza. Passos 8–9 não ocorreram.
Os novos testes exercitam EOF com resposta retida e o tratamento de processo;
não simulam resultados SQL como prova econômica. Uma nova rodada deve provar
handle/identidade no passo 7 e então novo worker, mesmo OperationId, consulta,
retry idêntico e imagem sem novos efeitos nos passos 8–9.

Sem teste CIM/TerminateProcess real nem rede PostgreSQL nesta R1. Não se
afirma correção homologada em Windows físico ou aprovação do C02 anterior.
