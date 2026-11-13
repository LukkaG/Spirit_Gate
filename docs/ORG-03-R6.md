# ORG-03-R6 — Contratos de durabilidade, identidade e continuidade

## Contexto e objetivo

Consolidação documental em 11/11/2026. R1–R5 permitem discutir estrutura,
leitura/captura coerente e idempotência experimental; não demonstram uma
recuperação durável entre processos. Esta revisão especifica contratos,
alternativas e provas necessárias para uma implementação futura, sem iniciá-la.

**APROVADA ANTERIORMENTE — D01/D13/D30:** preservar runtime e dados; não integrar
PostgreSQL, autenticação, reconexão, A-04, ORG-04 ou TASK-226 nesta etapa.

## Estado atual verificado

- Branch `beco`, HEAD `13b91dca20e16d6729d2dced79c1a4431b74ca09`.
- Checkout já continha trabalho local extenso. Status e diff inicial foram
  registrados em `temp/org03-r6/`, sem descarte, commit ou troca de branch.
- R1 contém DTO/validador, mas não importador autorizado. A negativa de ativação
  permanece; textos anteriores que dizem não existir DTO são históricos.
- R3 conserva valores nativos, inclusive precisão fracionária e ausência; R4
  captura no owner real, vincula Session/peer e revalida contexto. Não fornece
  identidade permanente, época durável ou prova econômica.
- R5 usa `AtomicReference<LedgerImage>` e CAS em memória, exclusivamente em
  `src/test`. Tem recibos e índices sintéticos; não há disco, credenciais,
  integração econômica ao owner ou transação PostgreSQL comprovados.
- Runtime e laboratório diferem em teto de XP/moedas, coleta pendente,
  equipamentos e efeitos completos de poção. Não se pode aplicar um recibo R5
  como recuperação de todo o personagem.
- Clocks de mundo, sessão e engine têm origens distintas. Efeitos BLEED/BURN
  observados no engine são de inimigos; não comprovam ledger persistente de
  efeitos negativos do jogador.

Foram consultados AGENTS.md, `logs/README.md`, status do monitor e relatórios
locais existentes. Os erros antigos de IDE/World Studio não justificam mudar
esses sistemas na R6; não se declara sua correção ou homologação.

## Auditoria documental e fontes locais

Contratos auditados: [identidade/reconexão](ORG-03-ADR-IDENTIDADE-E-RECONEXAO.md),
[snapshot](ORG-03-CONTRATO-SNAPSHOT.md),
[persistência](ORG-03-PLANO-PERSISTENCIA.md),
[autoridade R4](ORG-03-R4-PENDENCIAS-AUTORIDADE.md),
[economia R5](ORG-03-R5-ADR-ECONOMIA.md),
[recibos R5](ORG-03-R5-MATRIZ-RECIBOS.md) e
[integração futura R5](ORG-03-R5-PLANO-INTEGRACAO-FUTURA.md).

Fontes de comportamento consultados em `server/src/main/java/com/projetok/rpg/`:
RpgWorld, RpgSessionWorld, RpgSkillEngine, RpgProgression, DomainProgression,
QuestRewards, QuestService, LootService e InventoryService; fronteira de owner
em LocalSimulationServer. Os nomes identificam o código lido, sem propor novos
pacotes ou pontos de entrada. R1–R5 e seus relatórios permanecem intactos.

A [matriz de decisões](ORG-03-R6-MATRIZ-DECISOES.md) registra divergências e
proveniência, sem reescrever silenciosamente recomendações antigas como aceites.
Documentação oficial PostgreSQL é citada junto das afirmações técnicas no plano;
não escolhe versão, provedor ou configuração para o projeto.

## Entregáveis e leitura

| Documento | Conteúdo |
| --- | --- |
| [ADR de durabilidade](ORG-03-R6-ADR-DURABILIDADE.md) | Dez identificadores, posse/claim/fence, revisão conjunta, operação/recibo e fronteira owner/worker |
| [ADR de políticas do jogador](ORG-03-R6-ADR-POLITICAS-PLAYER.md) | Doze comparações runtime/R5, nove canais de cooldown, efeitos negativos, legado e A-04 |
| [Matriz de decisões](ORG-03-R6-MATRIZ-DECISOES.md) | D01–D34 com estado explícito, responsável e prova/aceite necessários |
| [Plano PostgreSQL](ORG-03-R6-PLANO-POSTGRESQL.md) | Agregados, chaves, constraints, concorrência, atomicidade, consultas e retenção conceituais |
| [Matriz de falhas](ORG-03-R6-MATRIZ-FALHAS.md) | F01–F30, comportamento esperado, evidência e etapa futura; nenhum cenário homologado pela escrita |
| [Gates de implementação](ORG-03-R6-GATES-DE-IMPLEMENTACAO.md) | E1–E5, aprovações, critérios de entrada e evidências necessárias |
| [Relatório final](ORG-03-R6-RELATORIO-FINAL.md) | Verificação realizada, preservação, limites e pendências |

## Contratos propostos e limites

**RECOMENDADA — D04/D06/D08/D09/D10/D24/D26:** separar identidade de conexão e
propriedade; usar exclusividade com fence, cabeça canônica única, transação de
efeitos/recibo/revisões/deduplicação e fila externa limitada. Nenhum desses
componentes foi instalado no runtime. Fence de banco sozinho não impede um ator
antigo de causar dano; admissão permanece bloqueada sem prova de suspensão segura.

**PENDENTE DE APROVAÇÃO — D14–D17/D22/D34:** fechar regras de recompensa,
repetibilidade, coleta, penalidades offline e obrigações antigas pendentes antes
de definir uma política definitiva. A candidata de migração administrativa C
(D27, **RECOMENDADA**) não foi ativada. A recusa integral provisória A continua.

**BLOQUEADA POR EVIDÊNCIA — D21/D28/D29/D33:** clocks/precisão, durabilidade real,
ativação segura e revogação após restore/failover exigem ensaios futuros. Não há
fórmula homologada de ticks para timestamp nem promessa de restauração atual.

O fluxo futuro é owner → comando imutável → fila limitada → worker → transação
curta → callback no owner → revalidação de geração/fence → publicação ou
reconciliação. Aceitação em fila volátil não equivale a confirmação durável;
resposta perdida exige consulta autorizada do mesmo OperationId. Não criar nova
concessão por incerteza. Locks de banco não ficam abertos esperando o owner.

## Validação e reversão

Verificação documental e SHA-256, com evidências locais em `temp/org03-r6/`.
Não foram executados testes Java ou SQL nesta revisão documental. Os 1.196 testes
executados/aprovados e 57 excluídos são o baseline histórico da R5, não uma nova
rodada R6. Testes físicos, Radmin, launcher e crash entre processos continuam
fora da evidência desta entrega.

A reversão da R6 é localizada: remover somente seus oito documentos novos e
seu adendo na organização principal, preservando alterações anteriores. A cópia
prévia da organização em `temp/org03-r6/ORGANIZACAO-before.md` serve para comparar
o trecho, não para sobrescrever futuras alterações de terceiros.

O próximo passo sugerido é a revisão E1 das decisões, sem executar E2 ou qualquer
implementação automaticamente.
