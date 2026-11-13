# ORG-03-R5 — Contrato econômico, recibos e idempotência

## Contexto e estado inicial

Execução de 11/11/2026, branch `beco`, HEAD `13b91dc`. A R4 estava presente com
1133 testes headless aprovados e 57 excluídos. Foram consultados AGENTS.md,
relatórios R1–R4, gate R3, pendências R4, POM e serviços/laboratórios pertinentes.
Status completo, diff inicial, 3760 hashes e checkpoint do documento principal
ficam em `temp/org03-r5`. Alterações locais anteriores foram preservadas.

Nenhum laboratório anterior foi tratado como persistência econômica online.
SyntheticOnlineRepository grava um envelope de RpgSave; os ensaios 194–196
exercitam CAS/locks/retomada desse formato, sem ledger econômico transacional.
Task225R3RecoveryLab demonstra continuidade sintética em memória, com autoridade
própria. Nenhum desses armazenamentos foi reutilizado para confirmar economia.

## Escopo implementado

Somente dois fontes novos, ambos em
`server/src/test/java/com/projetok/rpg/economy/`:

- `EconomicLaboratory.java`: tipos imutáveis, controlador sintético, preparação,
  commit condicional, recibos, reconciliação e auditoria da imagem em memória.
- `EconomicLaboratoryTest.java`: falhas determinísticas, concorrência e regras
  existentes sobre objetos descartáveis, sem jogador, socket ou arquivo pessoal.

Nenhum fonte de produção foi modificado. O experimento não se conecta ao
RpgSessionWorld, captura R4, launcher, banco, saves ou protocolo. Não cria
contas, credenciais, tokens de login, comércio ou pagamento.

## Contrato e etapas

1. **Solicitada:** EconomicOperation imutável, com ID, beneficiário, revisão
   esperada, geração, época e ação completa.
2. **Validada:** ValidatedOperation opaca, produzida somente após conferir
   contexto/oferta/regras e preparar cópias. Não modifica o estado confirmado.
3. **Confirmada:** CAS publica uma LedgerImage com estado, recibo e índices
   compatíveis. Não há segundo passo independente para gravar o recibo.
4. **Rejeitada:** diagnóstico explícito, sem recibo novo ou publicação parcial.
5. **Incerta:** o chamador não sabe se houve confirmação; consulta pelo ID é
   necessária. Retry faz essa consulta antes de qualquer nova preparação.

O resultado de um replay contém o recibo histórico original. Não sobrescreve
o estado atual se operações posteriores já avançaram a revisão.

## Identidade e autoridade sintéticas

BeneficiaryId, EconomicOperationId e GrantId são tipos distintos. EconomicRevision,
AuthorityGeneration e WorldEpoch também são distintos e declarados pela fixture,
sem derivação de clocks, Peer.id, conteúdo, IP ou nome.

O controlador confiável cria o ledger de abertura e vincula Coordinator ao
beneficiário, à instância e ao contexto atual. A fábrica de vínculo é exclusiva
desse controlador de teste; não aceita uma solicitação externa de posse.
Saber um UUID não equivale a possuir um Coordinator admitido nem autoriza
trocar seu beneficiário. Isso **não implementa autenticação real**: alguém com
acesso irrestrito ao controlador do laboratório continua dentro da fronteira
confiável da fixture. Esse limite não é ocultado por um UUID ou fingerprint.

## Fronteira transacional

Uma AtomicReference aponta para a imagem imutável que contém todos os estados,
recibos, índices de concessão e watermarks. A preparação usa inventários novos;
o commit revalida contexto, deduplicação e revisão, audita a imagem candidata
e faz compareAndSet. Disputa sobre a mesma revisão tem um único vencedor de
escrita; disputas entre beneficiários podem confirmar ambas sem perder atualizações.

O CAS é atômico somente nesta memória. Queda do processo pode perder imagem,
recibos e índices. Recriar apenas Coordinator sobre o mesmo ledger não comprova
durabilidade. O teste de perda da memória demonstra essa diferença explicitamente.

## Regras e limites experimentais

QuestRewards.prepare, InventoryService e DomainProgression são usados apenas
sobre cópias locais. A fixture lê definições reais de QuestService/QuestBook e
resolve uma tabela real de LootService antes de executar operações. RNG não
entra no ledger nem é chamado em retry/reconciliação.

XP/moedas fora do limite e overflow são recusas integrais. Em particular, o
laboratório recusa uma concessão cujo valor de XP precisaria ser truncado pelo
limite da curva, para não confirmar resultado diferente do prometido. Esse
gate experimental é mais restritivo e não altera a regra de gameplay existente.

Mobs com bolsa cheia geram direito pendente ligado ao recibo original. Claim
move o pacote inteiro ou recusa sem mover nada. O loot legado recolhe por item;
a atomicidade por pacote desta R5 é uma proposta experimental, não mudança do
runtime. Quest com bolsa cheia continua recusada pelo serviço real. Consumo e
movimentação testados afetam apenas inventário sintético, sem HP/cooldown/UI.

Retenção usa orçamento técnico configurável, sem apagar recibos. No limite,
recusa novos commits e continua permitindo consulta/replay dos confirmados.
Não é política final de retenção nem benchmark de escala.

## Critérios de pronto e documentação

São exigidos recibo e efeito na mesma imagem, replay sem concessão, recusa de
conflitos/revisões antigas, confirmação incerta reconciliável e ausência de
mudanças econômicas de produção. Legado não é convertido: flags não entram
como recibos, e abertura com entrega/direito sem histórico é recusada.

Ver [ADR](ORG-03-R5-ADR-ECONOMIA.md), [matriz](ORG-03-R5-MATRIZ-RECIBOS.md),
[integração futura](ORG-03-R5-PLANO-INTEGRACAO-FUTURA.md),
[testes](ORG-03-R5-TESTES.md) e [relatório](ORG-03-R5-RELATORIO-FINAL.md).
