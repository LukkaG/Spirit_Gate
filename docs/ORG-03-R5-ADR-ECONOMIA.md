# ORG-03-R5 — ADR de economia experimental

## Status

Aceita somente como decisão de laboratório. Não aprova armazenamento durável,
política de migração, autenticação, integração com gameplay ou recuperação.

## Problema verificado

QuestRewards retorna Plan com InventoryService mutável, moedas, XP e domínio.
O chamador ainda precisa marcar a missão e publicar o plano. LootService
deduplica por flags de spawn/geração e pode guardar `pending:item:quantity`;
isso não identifica recibo transacional, beneficiário persistente ou revisão.
SharedRpgEncounter valida contribuidores e emite eventos efêmeros, cujo buffer
é limitado; evento visual/histórico não é ledger durável.

RpgSave e SyntheticOnlineRepository não agregam o contrato econômico inteiro.
Um retry de snapshot idêntico não substitui idempotência por operação/origem.
Reutilizá-los exigiria alterar formatos/garantias fora da autorização da R5.

## Decisão experimental

Usar tipos de teste isolados, imagens imutáveis e um CAS sobre a imagem inteira.
Evitar alteração sequencial independente de bolsa, saldo, entrega e recibo.
O plano validado guarda base e resultado, é ligado ao ledger que o preparou
e não pode ser criado por um construtor público aceitando um snapshot externo.
O commit revalida o contexto e a revisão antes de publicar.

O recibo é identificado pelo EconomicOperationId e contém intenção completa,
revisões antes/depois, fingerprint da base, resultado absoluto e fingerprint do
resultado/intenção. GrantId identifica a concessão, não a tentativa. Fingerprints
SHA-256 canônicos detectam divergências estruturais; não são assinatura,
autenticação ou prova de posse contra um escritor privilegiado malicioso.

## Regras de idempotência

| Situação | Decisão experimental |
| --- | --- |
| Mesmo ID e mesma intenção inteira | Retornar recibo original sem aplicar efeitos |
| Mesmo ID com qualquer campo diferente | ID_CONFLICT |
| Mesmo ID em outro beneficiário | Conflito/recusa de beneficiário, sem recibo cruzado |
| Novo ID para mesma concessão/origem | Recusar; não criar um recibo novo para fingir retry |
| Mob com mesmo spawn/geração/época/beneficiário | Uma concessão econômica |
| Geração posterior autorizada | Novo evento, sujeito à revisão e a nova concessão |
| Evento antigo nunca confirmado, abaixo do watermark | OBSOLETE_SOURCE |
| Missão já entregue | Recusar por namespace/identidade, mesmo com outra versão/GrantId |
| Direito já concedido ou coletado | Não recriar direito nem XP/moedas |
| Claim pendente | Exigir recibo original, retirar direito e inserir itens no mesmo commit |
| Resposta perdida | UNCERTAIN; consultar recibo pelo ID, nunca assumir ausência de commit |
| Consulta falhou | Continuar incerto, sem preparar nova concessão |

A intenção inteira inclui revisão esperada, geração e época: retry deve guardar
a mesma intenção, sem reescrever sua base. Outro contexto de admissão pode
consultar um recibo histórico autorizado, mas não reaplicar a intenção antiga.

A deduplicação de missão é conservadora e vitalícia neste experimento, sem
definir missões repetíveis ou migração definitiva. A fonte de mob inclui época,
namespace/spawn e geração. Um índice adicional de GrantId impede reutilizar o
mesmo direito com outra origem/contexto. A oferta também é vinculada à época.

## Falhas e confirmação

Falha de preparação acontece depois de calcular cópias, antes de publicar.
BEFORE_COMMIT é um ponto conhecido do ensaio: sabemos que não houve publicação.
AFTER_COMMIT perde a resposta, preservando a imagem confirmada; o resultado
externo continua incerto até consulta. DROP_RECEIPT/CORRUPT_RECEIPT produzem
candidato inválido que a auditoria recusa antes do CAS.

Em armazenamento real, uma exceção pode não revelar de que lado do commit
ocorreu a falha. A precisão dos pontos injetados não deve ser atribuída a uma
conexão real indisponível. São necessários atomicidade durável e consulta
consistente pelo ID antes de traduzir esse protocolo para produção.

## Legado: alternativas sem migração automática

| Alternativa | Risco e integridade | Impacto no jogador | Auditoria e condição para implementar |
| --- | --- | --- | --- |
| A — Recusar recuperação integral sem prova | Evita declarar histórico que não existe; bloqueia continuidade por esse mecanismo | Recuperação integral indisponível enquanto não houver contrato | Diagnóstico claro, preservar dados e definir atendimento; é a restrição vigente, sem mudança nova no runtime |
| B — Reter legado não confirmado | Conserva observação, mas não permite tratá-la como concessão comprovada | Exige explicar limites e separar uso permitido de ativação ainda recusada | Proveniência, marca explícita, regras contra mesclar confirmado/não confirmado e decisão de operação |
| C — Marco administrativo auditável | Pode estabelecer abertura autorizada, sem reconstruir fatos desconhecidos | Pode exigir conferência, contestação e tratamento de diferenças | Autorização explícita, evidência independente, inventário de dados, responsável, revisão do marco, backup e reversão localizada |

Nenhuma alternativa B/C foi escolhida ou implementada. Um marco administrativo
futuro seria uma decisão auditada nova sobre abertura; não um RewardReceipt
retroativo fabricado de `quest-done`, derrota ou `pending`. Saldo/flags sozinhos
não demonstram quem recebeu o quê nem em qual revisão econômica.

As aberturas da R5 são cenários sintéticos declarados pelo teste. Não importam
RpgSave ou flags, não alegam origem de um personagem real e recusam entregas/
direitos pendentes sem recibos. Não são um caminho de migração do legado.

## Custos e consequências

Recibos guardam resultados absolutos imutáveis; a auditoria percorre a cadeia e
recalcula fingerprints, sem executar gameplay/RNG. Isso simplifica a prova, mas
tem custo de cópia/memória e não foi projetado como backend de produção.
O orçamento de recibos limita o laboratório e falha fechado; não apaga história
para abrir espaço. Arquivamento, compactação e retenção exigem política própria
que preserve deduplicação e reconciliação após qualquer reinício autorizado.
