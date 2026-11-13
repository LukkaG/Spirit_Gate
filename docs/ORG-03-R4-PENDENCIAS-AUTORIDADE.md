# ORG-03-R4 — Pendências de autoridade

## Comprovado neste recorte

O LocalSimulationServer registra seu Thread real e a consulta local de presença.
RpgSessionWorld resolve a Session admitida, verifica o peer atual e combina os
read models no mesmo ponto do owner. Pedido antigo não se torna válido apenas
por reter RpgWorld/Session ou encontrar um UUID igual.

Isso permite observação autoritativa **na instância atual**. Não comprova dono
de conta, direito de reconectar, sessão autenticada persistente, exclusão entre
dois processos ou autorização de instalar dados.

## Ausências preservadas

| Autoridade/dado | Situação | Conduta R4 |
| --- | --- | --- |
| CharacterId persistente e posse | Não demonstrados pelo UUID online | Não criados nem deduzidos de nome/IP/ID |
| Revisão de personagem/econômica | Não existe prova durável neste runtime | Número de LiveRevision permanece revisão de conteúdo |
| Geração/fence entre processos | Ausentes | Objeto Session é só vínculo local, sem alegação de geração durável |
| Época temporal durável | Ausente | Metadata.durableWorldEpoch=null |
| Recibos transacionais | Flags/saldos não bastam | Nenhum RewardReceipt fabricado |
| Watermarks | qe locais disponíveis | Não recebem época/revisão inventadas |
| Loot pendente | Evidência local da R3 | Não vira direito econômico confirmado |
| Efeitos externos/DoTs/origem | Incompletos | externalEffectsKnown=false; não declarar personagem limpo |
| Continuidade offline/restart | Política não aprovada | Deadlines ficam no relógio local original |
| Intangibilidade/projéteis/execução | Observáveis apenas parcialmente | Sem conversão em estado recuperável ou replay |

## Gate econômico

`CharacterCaptureResult.economicHistoryComplete()` e `activationAuthorized()`
retornam false. O resultado é observação, sem candidato recuperável em produção.
O adaptador de laboratório R2/R3 continua incompleto e recusado pelo validador R1.
Não houve transação econômica, cálculo de recompensa, RNG, save ou importação
dentro da leitura. Uma recompensa legítima anterior à captura foi exercitada
em TCP e consultas repetidas não a concedem novamente.

Esta restrição fail-closed é provisória e não define migração de personagens
legados. As alternativas R3 continuam pendentes: recusar exportação integral,
reter legado não confirmado ou aprovar migração auditável e registro de futuras
transações. Nada disso foi implementado ou aprovado por esta R4.

## Limites da autoridade operacional

A ligação é de um RpgSessionWorld a um owner durante a vida desse servidor.
Não se implementou reatribuição de owner/reuso da mesma instância após restart.
Wrappers externos de SessionSimulation não recebem registro automaticamente;
sem registro a captura recusa. A ponte Java pública não autentica seu chamador:
o uso seguro depende do coordenador que detém as referências privadas.

Não existe endpoint de exportação nem API de fila para consumidores externos.
Uma futura API de agendamento precisa definir capacidade, cancelamento,
respostas em shutdown, prioridade, frequência e orçamento por tick antes de
ser conectada a qualquer consumidor permanente. O ensaio injeta apenas tarefas
de teste na fila limitada existente; não instala esse consumidor em produção.

## Próxima autorização necessária

Antes de qualquer importação/recuperação/persistência: decidir identidade/posse,
revisão durável, prova econômica, clock entre processos e política de efeitos.
Definir também controle de concorrência e compatibilidade de regras/conteúdo
na instalação. Essas decisões não podem ser substituídas pelos metadados desta
observação. Sem ORG-04, TASK-226, PostgreSQL ou ativação de A-04 nesta entrega.
