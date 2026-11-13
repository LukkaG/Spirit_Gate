# ORG-03-R5 — Plano de integração futura, sem execução

## Pré-condições de autorização

Antes de qualquer integração, aprovar identidade persistente/posse, revisão
econômica durável, geração de autoridade, época e política de legado. A captura
R4 não fornece esses contratos. A confirmação de R5 vale apenas no ledger de
teste e não autoriza ativar um RecoverableCharacterSnapshot.

O armazenamento futuro precisa confirmar conjuntamente efeitos, recibo,
revisão e deduplicação, com consulta consistente por operação após resposta
incerta. A escolha de backend, formato, esquema, migrações e conexão não foi
feita nesta tarefa. Não há SQL, JDBC, banco criado ou alteração de saves.

## Pontos reais inspecionados

Caminhos abaixo relativos a `server/src/main/java/com/projetok/rpg/`.
São pontos candidatos de integração **não alterados**.

| Fluxo atual | Ponto futuro proposto | Garantia que falta |
| --- | --- | --- |
| QuestService.accept/event/refresh/complete | O recibo não nasce de View ou flag; ligar entrega à operação autorizada | Prontidão, definição/versão e beneficiário comprovados |
| RpgWorld.completeQuest + QuestRewards.prepare | Após validar NPC/prontidão e preparar plano, confirmar plano + entrega + recibo juntos | Fronteira durável e publicação coerente no owner |
| RpgWorld.completeAuthoredQuest + AuthoredQuestSession.complete | Mesma regra, preservando namespace e contrato autorado | Versão semântica da missão, não apenas QuestBook.version |
| SharedRpgEncounter.hit + RpgWorld.sharedReward | Após morte/contribuição/vida/região válidas, uma concessão por beneficiário e origem | Origem estável de morte/spawn/geração/época e prova da concessão |
| LootService.award | Resolver o resultado uma vez; armazená-lo no mesmo commit de prêmio/direito | Não reexecutar RNG em retry/recuperação |
| LootService.collect + RpgWorld.collectLoot | Referenciar o grant original ao mover direito para a bolsa | Claim idempotente, conservação dos itens e política de parcialidade aprovada |
| InventoryService.add/remove/move | Reutilizar operações sobre cópias dentro da fronteira aprovada | Não tratar retorno booleano local como commit durável |
| RpgWorld.use/equip/unequip | Definir quais efeitos econômicos e de gameplay precisam ser coerentes | HP/cooldown/equipamento não existem no modelo R5; não integrar removendo só o item |

O runtime atual recolhe pendências por item, enquanto a R5 testa pacote inteiro
atômico. Essa diferença precisa de decisão explícita antes de migrar. O exemplo
de pacote com dois itens é sintético, usa itens reais de catálogo e não altera
a tabela de drops do jogo.

## Etapas futuras propostas

1. Revisar ADR, namespaces, identidade de concessão, repetibilidade de quests
   e o que constitui consumo/movimento econômico relevante.
2. Aprovar o tratamento A/B/C do legado com evidências e impacto no jogador.
   Não preencher recibos retroativamente a partir de flags.
3. Definir a fronteira durável e os estados de resultado: confirmado, rejeitado
   e incerto. Ausência de resposta nunca deve ser traduzida em permissão de repetir.
4. Definir retenção/arquivamento sem perder deduplicação ou direito de consulta.
   O limite técnico da fixture não é uma política de produção.
5. Projetar a interação com owner: capturar base coerente, validar contexto,
   executar trabalho externo fora do tick e revalidar callback/revisão/admissão
   antes de publicar. Não bloquear o owner aguardando I/O.
6. Ensaiar falhas reais de armazenamento/processo, concorrência entre instâncias,
   callback atrasado, contexto trocado, resposta perdida e observador multiplayer.
7. Somente mediante nova autorização, alterar os pontos de gameplay em unidades
   pequenas, com rollback e provas de que nenhuma concessão foi duplicada/perdida.

Isso é planejamento, não autorização para executar nenhuma etapa de produção.

## Autoridade, reconciliação e reinício

Um Coordinator sintético novo sobre a mesma MemoryLedger recupera recibos
porque a memória continua existindo. Não se transfere essa conclusão a restart
de processo, queda de energia, banco indisponível ou inicialização com histórico
parcial. Um personagem já existente não pode ser aberto como se fosse uma nova
gênese apenas porque uma consulta falhou ou retornou dados insuficientes.

Revisão econômica deve ter origem própria, não sessionTick/worldTick/conteúdo.
Resultados históricos de operação são comprovantes para reconciliação, não
snapshots livres para sobrescrever uma revisão posterior. Identidade de operação
e comparação do conteúdo precisam ser mantidas durante retry e após troca de
coordenador autorizado. A política real de fencing entre processos é pendente.

## Gate de continuidade

Enquanto essas pré-condições não existirem, manter R3/R4 fail-closed: observação
e ledger local não viram recuperação integral. Sem integração com PostgreSQL,
RpgSessionWorld, launcher, contas, protocolo 6/7, A-04, ORG-04 ou TASK-226 nesta R5.
