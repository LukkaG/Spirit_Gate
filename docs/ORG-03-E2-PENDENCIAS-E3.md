# ORG-03-E2 — Pendências para E3 e demais gates

Execução de referência: `20261114-lucas-600aa9e066`, em 14/11/2026. Responsável: Lucas.
Branch `beco`; HEAD `c3d484e76f17c2d93bf0c68ad3ff54280ee8ad39`.
Autorização explícita L01–L05 recebida nesta conversa, com seis ressalvas;
registro local em [authorization.json](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/authorization.json).
As políticas definitivas D14–D22, D27 e D34 não foram aprovadas por este aceite.

## Garantias ainda não demonstradas

| Gate | Trabalho e evidência futuros, sujeitos a autorização própria |
| --- | --- |
| E3 — F04/F05/F06/F08 | Perda real de conexão/processo antes/depois do commit, reconciliação após restart e ausência de falso rollback |
| E3 — F09/F16/F23 | Recuperação real de arquivo/histórico e sobrevivência do manifesto/dívida ao crash entre beneficiários |
| E3 — F20/F28/D33 | Restore/failover sem reviver fence/época/OperationId/GrantId; revogação de autoridade antiga |
| E3 — F14/F25/D21 | Relógio confiável, âncoras, margem conservadora, saltos/restart e conversão homologada; E2 provou só representação |
| E3 — F30/D12 | Perfil de confirmação, WAL/replicação, backup e RPO/RTO sob falha real |
| E4 — D05/D07/D24/D29/D32 | Identidade/posse real, lease/retirada do ator, worker/fila/callback, owner, spawn, conteúdo e compatibilidade de transporte |
| Políticas | D14–D22/D27/D34: decidir regras reais separadamente; nenhuma alternativa sintética foi adotada no jogo |
| E5 | Homologação/implantação e eventual migração auditada de dados, sem fabricar histórico antigo |

## Próxima decisão necessária

Revisar [relatório final](ORG-03-E2-RELATORIO-FINAL.md) e evidências C01–C16
para registrar a avaliação humana de E2. L01–L05 foram atendidos somente no
laboratório; não é preciso repetir esses aceites para revisar a entrega.
Qualquer E3 exige pedido/aceite específico com cenário de falha, recursos,
limites, isolamento, recuperação, evidência e descarte. Não há autorização
para matar/reiniciar banco em teste de crash, restore ou failover nesta entrega.

Antes de uma integração futura, revisar schema/codec e invariantes com todos os
escritores reais, não importar diretamente o JSON ou funções experimentais.
Autorização sintética não é modelo de segurança de contas. A API de archive
usa localizador e hash no harness; um serviço real precisará resolver esse
contrato e a autorização atual sem perder deduplicação.

## Restrições mantidas

Sem alteração de R1–R7, R5 em RAM, server/src/main, server/pom.xml, RpgWorld,
RpgSessionWorld, LocalSimulationServer, RpgSkillEngine, QuestRewards,
LootService, InventoryService, protocolo 6/7, 40 skills, cooldowns, HUD,
World Studio, launcher, saves/contas ou bancos existentes. TASK-225/A-04,
ORG-04 e TASK-226 não foram iniciadas. Sem commit/push/release/publicação.
