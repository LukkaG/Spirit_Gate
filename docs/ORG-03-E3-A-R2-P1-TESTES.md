# ORG-03-E3-A-R2-P1 — Testes e evidências

Lucas, 15/11/2026. Biblioteca padrão Python 3.14.5/Windows, sem dependências novas.

## Resultado final

**71 testes puros passaram: 23 históricos preservados + 48 novos.**
Dois controles reais finais loopback passaram. Zero SQL, PostgreSQL, cluster
ou crash. Os resultados técnicos anteriores E2/E3/R1/R2 não foram alterados.

```powershell
# Suíte combinada, sintaxe e preservação; não abre sockets da fixture:
python -B tools/org03_e3a_transport_fix/validate.py

# Suíte combinada e dois controles reais de transporte local:
python -B tools/org03_e3a_transport_fix/validate.py --with-fixture
```

A chamada final com fixture retornou código 0. Saídas completas em diretório
único `evidence/validation-*/`, referenciado pelo
[validation.json](../tools/org03_e3a_transport_fix/evidence/validation.json).
Esse resultado separa testes unitários, fixture e histórico de falhas.

Os 23 históricos rodam sem alteração nas cópias legacy, com as APIs antigas.
Os 48 novos exercitam as APIs corrigidas; OwnedProcess é exatamente a mesma
classe importada de legacy. Não se alteraram testes antigos para redefinir
sucesso de reset, nem se afirma que a captura antiga aceita bytes novos.

## Cobertura dos vinte requisitos

| Requisito do pedido | Testes/evidência |
| --- | --- |
| 1 Reset esperado autorizado | novo 01, 20 e fixture EXPECTED_RESET real. |
| 2 Reset antes da barreira | novos 02, 07. |
| 3 Reset lado servidor | novos 03 e 16; sendall atribuído ao destino. |
| 4 Cliente desconhecido | novos 04, 11. |
| 5 Cliente identificado encerrado | novos 05, 08, 20–22 e fixture com CIM/OwningProcess. |
| 6 EOF com retidos | novo 06; novo 18 recusa EOF sem permit. |
| 7 Relay espontâneo zero | novo 33 recusa. |
| 8 Relay espontâneo não zero | novo 34 e fixture UNEXPECTED_RESET real. |
| 9 Exit antes da inspeção | histórico 02/07; novo 35, sem terminate. |
| 10 Identidade divergente mesmo após exit | histórico test_mismatch_even_if_handle_exits; novo 22 impede armamento. |
| 11 CIM inconclusivo | históricos 06/08/09. |
| 12 Close repetido | históricos 13/adaptador; novo 35. |
| 13 UTF-8 válido | novo 23. |
| 14 UTF-8 inválido | novo 24; novo 31 para cauda inválida. |
| 15 Acentos/encoding misto | novo 25; novo 29 sequência dividida; novo 30 U+FFFD válido. |
| 16 Stderr maior que teto | novo 26 drena e confere hash completo; novo 47 limita expansão sanitizada. |
| 17 Erro na thread | novo 27: erro compartilhado e validate falha; novo 32 sem EOF. |
| 18 Sanitização | novo 28: URI, senhas, tokens, campos extras e SQL não persistidos. |
| 19 Retenção imutável | novos 10, 13, 19, 41; counters reais na fixture. |
| 20 Não aprovar sem novo worker | novo 37; 38/40/42 recusam imagem/ID/worker; integração 44–46. |

Novos 09/12/14/15/17 cobrem fase, observador, abort, timeout e send_client.
O novo 39 apenas verifica o ramo positivo do gate com mocks; não é prova SQL.
Novo 48 recusa prova de transporte da fixture como entrada para aprovar C02.
Novo 43 verifica identidade da classe de guarda histórica. Novos 44–46
verificam bloqueio sem autorização R3, ordem UNCERTAIN/reconciliação e recusa
de persistir sucesso com recibo divergente.

## Fixture real, portas e ciclo de vida

Cada controle usa dois subprocessos Python próprios (relay e cliente), um
upstream sintético em thread do controlador e sockets exclusivos 127.0.0.1.
Portas alocadas por bind(0), registradas e recusadas se coincidirem com
5432/55433/55434; sem sondar/conectar serviços existentes. Os filhos conferem
claim/caminho/pai; controlador confere identidade completa e OwningProcess
do socket do cliente com endpoint efetivamente aceito pelo relay.

SO_LINGER abortivo no cliente força reset após terminate do handle identificado.
Não é perda de máquina nem crash do PostgreSQL. Dados TCP são literais da
fixture em memória; logs retêm somente metadados e contadores.

| Controle final | Diretório | Portas upstream/relay | Resultado |
| --- | --- | --- | --- |
| Reset esperado | `fixture-20261115-f60abf5473dc` | 65526 / 65527 | PASSOU: errno 10054; permit/ACK; forwarded=8, received=18, held=10; relay exit 0. |
| Reset não armado | `fixture-20261115-eb8f50904bd6` | 65531 / 65532 | PASSOU como teste negativo: errno 10054 continua erro, relay exit 1. |

Ambos mantiveram `economic_test=NOT_EVALUATED`; nenhum resultado SQL real.
Cada `result.json` comprova handles encerrados, ausência de listeners próprios,
zero cleanup_errors e zero terminate contra relay já encerrado. O processo
controlador também retornou; não há helper persistente da fixture.

## Histórico preservado

Nove controles reais no total: oito PASSOU, um FALHOU inicialmente. Quatro pares
positivo/negativo passaram após a correção do formato do canal; o último par
usa a fonte final. A falha inicial, suas portas 65087/65088 e cleanup estão
preservados. Nenhuma repetição automática de tentativa falha foi usada: houve
correção diagnosticada entre a falha e a nova execução da fixture.

Uma validação intermediária de 69 testes tinha 68 PASSOU e uma asserção de
teste FALHOU por mensagem de guarda anterior. Seu diretório não foi removido;
o ajuste e o teste adicional constam na análise. A rodada final tem 71 PASSOU,
zero FALHOU/INCONCLUSIVO/SKIP na suíte final. Falhas históricas continuam falhas.

**NÃO EXECUTADOS nesta tarefa:** C01–C09 com PostgreSQL, crash/recovery,
SQL global de integridade, R3, testes físicos/Radmin. Mocks/loopback não os
homologam. Verificação por SHA-256: 4.081 arquivos anteriores sem diferenças;
cópias legacy idênticas às fontes R2; manifesto de novos artefatos separado.
