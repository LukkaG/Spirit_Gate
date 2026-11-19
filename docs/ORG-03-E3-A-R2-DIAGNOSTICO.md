# ORG-03-E3-A-R2 — Diagnóstico da interrupção C02

Lucas, 15/11/2026. Run-id `20261115-lucas-e3a-r2-8c71d4a902`.
**C02 FALHOU no harness; não há conclusão de falha do contrato econômico.**

## Cadeia observada

Fonte principal:
[events.jsonl](../temp/org03-e3a/20261115-lucas-e3a-r2-8c71d4a902/evidence/events.jsonl).
Controlador PID 12056; relay 49892, criado às 07:05:47.8836820Z;
worker 47772/backend 10540; observador 15372/backend 43596.

| Sequência | Observação |
| --- | --- |
| 22–25 | Identidade do relay/worker registrada; listener 55434 próprio e worker pronto. |
| 26–28 | Imagens anteriores duráveis; C02-2 aplicada em BEGIN, txid 799, ainda sem COMMIT. |
| 29–30 | PAUSE confirmado pelo pump: 1.372 bytes recebidos/encaminhados, zero retidos. |
| 31 | COMMIT_DISPATCHED pelo worker, sem consumo de ACK; estado local UNCERTAIN persistido. |
| 32 | Observador independente consulta C02-2 e vê CONFIRMED, sr=er=1, XP 91, moedas 102, herb 1, missão true. |
| 33–34 | STATS: 1.390 bytes recebidos, 1.372 encaminhados, 18 retidos; confirmação continua bloqueada. |
| 35–36 | Worker RUNNING_MATCH em CIM integral e polls; terminate pelo objeto original. |
| 37 | Relay recebe ConnectionResetError, errno 10054, origem client, paused=true e 18 bytes retidos. |
| 38–39 | Término do worker confirmado pelo handle, exit 1. |
| 40–41 | Leitura do stderr falha com UnicodeDecodeError; trecho anterior e erro são preservados. |
| 42 | Inspeção do relay: poll_before=null, observado CIM=null, poll_after=1; EXITED_CONFIRMED. |
| 43 | Diagnóstico final conserva exit, barreira e stderr parcial; saída não zero causa falha. |
| 44–46 | Limpeza observa novamente o mesmo handle encerrado; registra falha diagnóstica, sem terminate/fallback. |
| 47–49 | Identidade do cluster revalidada, parada fast enviada e ausência de processos/listener comprovada. |

O [transcript SQL](../temp/org03-e3a/20261115-lucas-e3a-r2-8c71d4a902/evidence/sql-transcript.jsonl)
preserva a resposta do observador. A confirmação vista pelo observador é
distinta do retorno de lab.apply dentro de BEGIN. O evento COMMIT_DISPATCHED
e o contador imutável provam que o chamador não recebeu a confirmação TCP.

## Falha primária comprovada

`pump` chama recv no socket do cliente. Após o terminate identificado do
worker, esse recv lançou ConnectionResetError 10054; a R1 registrou tipo,
errno, origem e contadores e relançou a exceção. O relay saiu com código 1.
`Relay.close` reconheceu a saída e recusou promovê-la a sucesso econômico:
`RELAY_NONZERO_EXIT_REQUIRES_DIAGNOSIS`.

A classificação conservadora da R1 funcionou para a corrida observada;
a falha agora é explícita no transporte. Não ocorreu IDENTITY_MISMATCH ou
UNKNOWN registrado, nem ação contra processo desconhecido. A rodada anterior
permanece com causa específica desconhecida: a reprodução atual é compatível
com a hipótese de reset, mas não recupera o stderr/CIM ausente daquela rodada.

Não se chegou a wait_gone do backend C02, ao novo worker, à consulta do mesmo
OperationId por esse novo processo nem ao retry idempotente. O cenário falha
mesmo tendo commit e retenção de bytes comprovados. A ausência posterior de
todos os processos do cluster comprova encerramento, não substitui essas provas.

## Falha secundária comprovada e limite de diagnóstico

O pai abriu stderr com `encoding='utf8'`. O leitor da thread encontrou byte
0xe7 inválido nessa decodificação, na posição 427 do bloco reportado no console,
e encerrou com UnicodeDecodeError. O evento 40 registra o erro; o evento 41
preserva o trecho já lido. `truncated=false` refere-se apenas ao limite de
65.536 caracteres e não significa captura completa depois desse erro.

Hipótese: incompatibilidade entre o encoding efetivo do stderr do Python
filho/erro localizado do Windows e a decodificação UTF-8 fixa do pai.
O encoding exato dos bytes perdidos não foi medido; não se afirma cp1252 como
fato. A causa primária de transporte continua comprovada independentemente
desse texto, pelo evento JSON ASCII RELAY_ERROR com errno 10054.

O erro da thread foi exibido no console da chamada operacional e também está
representado estruturadamente em eventos. Não se fabricou um stderr completo
nem se repetiu a operação para recuperá-lo.

## Encerramento e auditoria posterior

A falha diagnóstica não impediu o fast normal do cluster próprio. O arquivo
`shutdown.json` registra data/secrets descartados e todos os processos próprios
ausentes. A exceção relançada depois dessa prova aparece também em
`shutdown-impediment.json`; trata-se da falha diagnóstica conservada após
limpeza segura, não de tentativa alternativa de apagar recursos.

A primeira auditoria posterior exigia ausência absoluta dos PIDs e falhou:
48632 havia sido reutilizado. O worker original nasceu às 07:05:43.9116010Z,
pai 12056, e seu handle confirmou exit no evento 14. Na auditoria posterior,
48632 nasceu às 07:07:02.3226210Z, pai 44640, após a parada/disposição.
A auditoria foi corrigida somente para distinguir reutilização posterior por
criação/pai, em leitura. Nenhum processo foi controlado. A falha inicial foi
preservada em `delivery-audit-first-attempt.json`; fontes/guardas operacionais
não foram alteradas. A prova de ausência antes do descarte continua em vigor.

## Pendências sem execução adicional

Uma correção futura precisa definir explicitamente o tratamento de reset
esperado após término do cliente, manter erros inesperados como falha e exigir
todas as provas de lookup/retry para aprovar C02. A captura de stderr precisa
de contrato de encoding e propagação explícita de falha de leitura. Os 23
testes puros cobriram erro de socket como exceção esperada; não homologavam
o comportamento econômico completo após reset real nem bytes de encoding misto.

Nenhuma dessas correções foi aplicada às fontes executadas. Nenhuma autorização
foi presumida para outro cluster/rodada. C03–C09, controles finais e SQL global
continuam não executados; crash/recovery não testado.
