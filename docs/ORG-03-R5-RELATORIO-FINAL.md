# ORG-03-R5 — Relatório final

## Resultado e escopo

Implementado contrato econômico experimental exclusivamente em src/test, com
operações/recibos/estado imutáveis e CAS de uma imagem econômica completa em
memória. Idempotência considera intenção inteira, beneficiário, concessão,
origem, época e geração. Resposta perdida é reconciliada pelo ID sem repetir
XP, moedas, itens, entrega de missão ou transferência de direito pendente.

Nenhum fonte de produção foi modificado. Não há integração com RpgSessionWorld,
captura R4, banco, autenticação, saves, protocolo ou recuperação real. Os gates
de ativação/economia de produção permanecem fechados como antes.

## Arquivos e documentação

Dois fontes novos em `server/src/test/java/com/projetok/rpg/economy/`:
EconomicLaboratory.java e EconomicLaboratoryTest.java. Foram criados escopo,
testes, este relatório, ADR de economia, matriz de recibos e plano futuro R5,
além do adendo na organização principal. Não há dependência nova.

## Garantias comprovadas e limites

| Garantia | Evidência e alcance |
| --- | --- |
| Estado + recibo + revisão + índices juntos | Um CAS de LedgerImage imutável; candidato sem recibo/fingerprint correto é recusado |
| Replay sem concessão | Retorna recibo original; estado atual posterior não é sobrescrito |
| Origem não duplica | GrantId e chave de beneficiário/origem/época/geração, além do ID da operação |
| Concorrência | Duas threads, propostas na mesma base: uma escrita; beneficiários distintos preservam ambos os resultados |
| Missão e loot | Serviços/catálogos existentes sobre cópias; READY/pendência não viram recibo por conveniência |
| Confirmação incerta | Perda da resposta mantém commit; consulta falha continua incerta sem concessão |
| Contexto obsoleto | Revisão, geração de admissão, época, beneficiário e ledger de validação conferidos |
| Retenção | Não há remoção automática; orçamento esgotado recusa novos commits |
| Durabilidade | Não comprovada; imagem, índices e recibos podem desaparecer com o processo |

O controlador de teste fornece as identidades/ofertas/aberturas e vincula
Coordinator. Não há autenticação ou prova de posse real. Um UUID não habilita
trocar beneficiário do coordenador. Fingerprints são integridade experimental,
não assinatura ou defesa contra quem reescreve arbitrariamente toda a memória.

## Testes executados

Primeira rodada nova: 56 casos aprovados. Dirigida: 241 casos em 19 classes,
sem falhas/erros/exclusões, incluindo 62 econômicos e laboratórios 194–196/
225-R3, contratos R1–R4, inventário, loot, quests e domínio. Rodada econômica
final: 63 casos aprovados. Suíte geral: **1253 casos em 279 classes; 1196
executados/aprovados, 57 exclusões gráficas, zero falhas/erros**. Tempo Maven:
2 min 12 s. São 63 casos adicionais sobre os 1133 aprovados da R4.
Logs, XMLs e hashes estão em `temp/org03-r5`. Ver
[testes](ORG-03-R5-TESTES.md) para comandos e cenários.

Não se somam rodadas como casos únicos. Pontos de falha são injetados no código
de laboratório; conhecer sua localização não é garantia equivalente à falha de
um banco real. Recriar Coordinator sobre a mesma memória não é restart durável.
Exclusões headless são separadas e não homologam Swing/Radmin/launcher físico.

## Decisões experimentais que não mudam gameplay

O laboratório recusa recompensa se o XP prometido excederia o limite técnico,
em vez de confirmar valor truncado. Missão entregue não é repetida por mudar
versão ou GrantId. Claim move o pacote inteiro ou recusa; o runtime legado
recolhe por item. Consumo/movimento só afetam inventário sintético, sem representar
HP, cooldown, equipamento ou outros efeitos que uma integração teria de tratar.

Ofertas de mob são previamente resolvidas pelo controlador. A fixture lê a
tabela real de LootService, e o ledger nunca recebe RNG. O teste de pacote com
dois itens é um caso sintético de atomicidade, sem editar regras/tabelas reais.

## Legado e dependências pendentes

Nenhum recibo retroativo foi emitido. Flags de conclusão, derrota ou pending
não são convertidas em prova econômica. Aberturas de teste não importam RpgSave
e recusam entregas/direitos sem histórico. Alternativas A/B/C são propostas,
sem escolher uma migração definitiva.

Faltam armazenamento durável, identidade/posse autorizadas, revisão/geração
entre processos, política de legado, retenção/arquivamento e interação com o
owner em falhas reais. O planejamento futuro não autoriza implementá-los.
Ver [ADR](ORG-03-R5-ADR-ECONOMIA.md) e
[plano futuro](ORG-03-R5-PLANO-INTEGRACAO-FUTURA.md).

## Preservação e reversão

Baseline: branch beco, HEAD 13b91dc, 3760 arquivos inventariados. Status/diff
completos, hashes e checkpoint do principal em `temp/org03-r5`. O diff contra
HEAD inclui trabalho anterior; a entrega R5 deve ser comparada ao checkpoint.
Evidências regeneradas pelos testes antigos são arquivadas e restauradas
individualmente, sem reset/clean global.

Comparação final: **somente o adendo no documento principal mudou entre os
3760 arquivos anteriores; nenhum arquivo ausente ou alteração inesperada**.
Todos os fontes anteriores de produção/teste, POM, assets, ferramentas e
checkpoints ORG inventariados mantiveram os hashes. `productionChanges=[]`
em `validation.json` confirma a ausência de mudanças de produção.

6886 arquivos de evidências foram conferidos, sem diferença remanescente.
Os dois TXT A-04 regenerados pelos testes existentes foram arquivados em
`generated-this-task` e restaurados individualmente. Não houve alteração da
regra A-04. O documento principal passou de SHA-256 `6c98e3a97d04…` para
`6e1553a96fec…`; valores completos em `affected-hashes.json`.

Rollback: remover somente os dois fontes de teste novos, seis documentos e
o adendo R5, preservando trabalho anterior, evidências e alterações posteriores.
Não há formato persistente, banco, save ou protocolo a desfazer. Os fontes de
produção, 40 skills, cooldowns, dano/colisão, assets, World Studio, launcher,
PostgreSQL e A-04 permanecem fora do recorte de implementação.

Sem commit, push, release, publicação, ORG-04 ou TASK-226. Próximo passo sugerido:
revisar ADR e decidir contratos/políticas duráveis antes de solicitar uma
integração de economia ou recuperação em produção.
