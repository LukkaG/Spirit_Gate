# ORG-03-E3-A — Matriz de resultados e falhas

Lucas, 14/11/2026. Run-id `20261114-lucas-e3a-5fbb9a8beb`.
Os cinco aceites foram recebidos. **E3-A não aprovado tecnicamente.**

| Caso | Estado efetivo | Evidência / resultado |
| --- | --- | --- |
| C01 — Cliente antes do commit | PASSOU | PREPARED sem COMMIT; cliente encerrado; backend ausente; cliente novo com mesmo OperationId recebe NOT_FOUND; imagens antes/depois idênticas. C01.json. |
| C02 — Commit com resposta perdida | FALHOU — HARNESS | Commit observado e 18 bytes retidos no relay; falha WORKER_IDENTITY_MISMATCH ao conferir relay antes de kill; sem reconciliação/retry em novo cliente. C02.json, events/transcript, ANALISE-FALHA-C02.md. |
| C03 — Crash pré-confirmação | NÃO EXECUTADO | Rodada interrompida em C02; nenhuma chamada immediate. |
| C04 — Crash após confirmação | NÃO EXECUTADO | Nenhuma queda/recovery PostgreSQL. |
| C05 — Resultado incerto | NÃO EXECUTADO | UNCERTAIN parcial de C02 não substitui este cenário. |
| C06 — Beneficiários A/B | NÃO EXECUTADO | Sem retomada de coordenador ou manifesto de distribuição testados. |
| C07 — Obrigação antiga | NÃO EXECUTADO | Sem prova de sobrevivência de obrigação após restart. |
| C08 — Arquivo/histórico | NÃO EXECUTADO | Sem ensaio de arquivo ausente/restaurado nesta rodada. |
| C09 — Encerramento pendente | NÃO EXECUTADO | Subcasos PREPARED/ACK/UNCERTAIN específicos não executados. |

Total: **1 PASSOU, 1 FALHOU no harness, 7 NÃO EXECUTADOS**.
[Resultados brutos](../temp/org03-e3a/20261114-lucas-e3a-5fbb9a8beb/evidence/results.json).
Nenhum caso está bloqueado por falta dos aceites recebidos. Os não executados
decorrem da falha e do encerramento da única rodada/cluster autorizados.

## Controles transversais

| Controle | Estado e limite |
| --- | --- |
| Revisões/recursos/índices atômicos | C01 comprovou imagem inalterada em conexão independente após fim do backend; C02 observou recibo confirmado. Sem prova após crash de servidor. |
| Confirmados sobrevivem a crash | NÃO TESTADO; zero crashes/recoveries. |
| Intenção estável e ausência de RNG | Duas intenções persistidas, uma aplicação cada; reconciliação completa só em C01, sem nova aplicação. |
| Retry idempotente em novo worker | NÃO TESTADO: C02 abortou antes desse passo. |
| Fence obsoleto em worker reiniciado | NÃO EXECUTADO. |
| Snapshot antigo versus cabeça atual | NÃO EXECUTADO. |
| Manifestos/dívidas/watermarks/arquivo | Infraestrutura implementada; cenários não executados. |
| Menor privilégio | Roles criadas sem superuser/createdb/createrole/replication; testes negativos de acesso não alcançados. |
| Integridade global SQL final | NÃO EXECUTADO; não inferir zero órfãos globais. |
| Encerramento/descarte próprio | COMPROVADO: fast, PIDs/listeners ausentes e data/secrets removidos. |

## Classificação da falha

A asserção de identidade interrompeu o harness antes de kill contra o relay.
Há hipótese de corrida com saída espontânea após encerramento do cliente;
o valor CIM transitório e stderr do relay não foram gravados. A causa específica
não é afirmada como certeza. Não houve evidência de falha do contrato econômico,
troca de alvo efetivamente encerrado ou impacto em outro serviço.

Barreiras propostas para C03–C09 e critérios completos anteriores estão
preservados em
[matriz anterior aos resultados](../temp/org03-e3a/20261114-lucas-e3a-5fbb9a8beb/evidence/documents-before-results/ORG-03-E3-A-MATRIZ-FALHAS.md).
Uma nova rodada precisará implementá-los e prová-los com recursos autorizados.
