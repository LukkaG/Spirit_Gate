# ORG-03-R3 — Gate econômico

## Decisão desta etapa

Captura econômica integral permanece **incompleta**. O ledger existente permite
observar aceitação, objetivos, contadores, conclusão e watermarks locais; não
contém recibos transacionais suficientes para provar recompensa confirmada,
beneficiário persistente, revisão econômica durável e hash do resultado.

Uma flag `quest-done:*`, `qe:*`, derrota, saldo atual ou `pending:*` não substitui
esse contrato. R3 copia essas evidências sem conceder nada, reconciliar saldos,
preencher coleções vazias por conveniência ou fabricar recibos retroativos.
Um prêmio real no preparo do teste comprova ausência de repetição na leitura;
não transforma esse teste em histórico econômico completo do personagem.

## Comportamento do adaptador

O candidato pode conter Attributes e Resources exatos, mas quests recuperáveis,
receipts, watermarks duráveis, pendingLoot e restrictions continuam nulos onde
faltam versão/prova/política. Ledger e watermark **locais** são observações,
distintas desses blocos. O validador R1 continua recusando ativação.

Identidade persistente, época, revisão, hash de conteúdo/regras e instante usados
no laboratório continuam explicitamente sintéticos, fornecidos pelo controlador.
Nenhum deles comprova autenticação ou operação econômica de um participante real.

## Alternativas futuras, sem implementação

| Alternativa | Benefício | Custo/risco e decisão necessária |
| --- | --- | --- |
| Recusar exportação integral do legado | Preserva exigência de prova | Personagens antigos precisam de caminho específico; é o comportamento seguro atual |
| Manter estado legado explicitamente não confirmado | Conserva observação sem fingir recibo | Exige regras de uso/quarentena e política de continuidade |
| Migração administrativa com marco de abertura | Permite fronteira auditável entre legado e novos eventos | Exige identidade/posse, reconciliação, backup, revisão durável, auditoria e autorização própria |
| Registrar transações futuras no momento da concessão | Permite idempotência/prova para eventos novos | Altera fronteiras econômicas/persistência; não reconstitui eventos passados por si só |

Nenhuma alternativa foi ativada. Migração não pode simplesmente renomear uma
flag histórica como recibo. Uma decisão futura deve definir origem, deduplicação,
beneficiário, revisão confirmada, atomicidade, retenção e recuperação de falhas.
Não houve SQL, alteração de saves, banco, formato wire ou regra de recompensa.
