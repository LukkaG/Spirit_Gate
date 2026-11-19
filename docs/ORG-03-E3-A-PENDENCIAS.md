# ORG-03-E3-A — Pendências após rodada interrompida

Lucas, 14/11/2026. **E3-A não aprovado tecnicamente.** Os cinco aceites e a
identificação do responsável foram recebidos; não há pendência desses campos.

| Pendência | Estado e condição para avançar |
| --- | --- |
| Lifecycle do relay | Falha do harness em C02. Hipótese de corrida com saída espontânea; valor CIM transitório/stderr não capturados. Corrigir em cópia futura, sem flexibilizar identidade antes de kill. |
| Evidência diagnóstica | Próxima cópia deve persistir identidade esperada/obtida, poll/handle e stderr sanitizado; distinguir ausência comprovada de processo e identidade realmente divergente. |
| Nova rodada | Único cluster autorizado foi criado/encerrado/descartado. Outro initdb/cluster e run-id precisam de autorização adicional. Não remover a guarda nem reutilizar dados. |
| C02 | Falta novo worker, consulta por OperationId e retry sem novos efeitos. Parte de commit/perda de resposta não aprova o caso inteiro. |
| C03–C09 | Todos não executados após interrupção segura. Sem provas de queda/recovery, manifestos/dívidas/arquivo após restart ou encerramento incerto específico. |
| Controles transversais | Fence obsoleto, CAS/snapshot histórico, acesso negativo e integridade global final não foram alcançados. |
| Gate humano | Revisar pacote e limites reais; não converter C01 ou encerramento seguro em aprovação de E3-A. |

[Análise e correção proposta](../temp/org03-e3a/20261114-lucas-e3a-5fbb9a8beb/evidence/ANALISE-FALHA-C02.md).
Scripts executados ficaram intactos; nenhuma correção foi anunciada como validada.
Uma correção futura deve aceitar saída espontânea apenas quando provada pelo
handle original, sem kill adicional; divergência de identidade não nula continua
bloqueando qualquer ação. Não mascarar erros genéricos de processo/socket.

## Garantias fora do recorte

D33 continua pendente de prova em recorte com backup/restore antigo e processo
antigo vivo. Nesta rodada nem a recusa sintética de fence após restart foi testada;
não há revogação global comprovada. Clock durável, posse/autenticação reais,
lease, restore/failover, RPO/RTO de produção, tolerância à perda física,
persistência de jogadores e recuperação/ativação integral permanecem não testados.

D14–D22/D27/D34 não recebem aprovação de política pelo aceite experimental.
Sem E3-B/E4/E5, ORG-04, TASK-226, migração, integração, gameplay, protocolo ou A-04.
Erros antigos de World Studio/IDE/cliente permanecem fora deste atendimento.

## Recuperação operacional do laboratório

Não há recurso ativo ou dado/segredo próprio a recuperar: descarte seguro
comprovado em shutdown.json. Não tentar recovery sobre uma pasta inexistente
nem anunciar evidências de recovery PostgreSQL que não foram produzidas.
