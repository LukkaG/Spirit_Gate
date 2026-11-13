# ORG-03-R6 — Plano conceitual PostgreSQL

## Estado e limites

**RECOMENDADA — D09–D11/D24/D26/D31. PENDENTE DE APROVAÇÃO — D12/D25/D34.
BLOQUEADA POR EVIDÊNCIA — D28/D33.** Não há DDL, migration, driver, pool,
conexão ou SQL executável criado nesta R6. Versão PostgreSQL e ambiente de teste
continuam sem escolha aprovada. A documentação oficial versão 18 foi consultada
como referência técnica, não como seleção de versão para o projeto.

## Agregados e chaves candidatas

Estes são nomes lógicos, não obrigação de uma tabela por record Java. Candidata
inicial: cabeça relacional, estado canônico versionado, operações/recibos e
índices de origem. Normalizar subestados pode ser melhor depois de conhecer as
consultas, mas não manter duas fontes graváveis de XP/moedas.

| Agregado lógico | Chave candidata e referências | Invariantes propostas |
| --- | --- | --- |
| Identidade/propriedade | CharacterId estável; referência de proprietário AccountId | Dono vem da autoridade aprovada; nome não é chave de posse; acesso sempre revalidado |
| Cabeça do personagem | CharacterId único, StateRevision, EconomicRevision, referência à versão canônica | Uma cabeça atual; CAS sobre base e autoridade; contadores não retrocedem |
| Admissão/claim | Uma autoridade atual por CharacterId; SessionId, geração, fence, fase e lease se aprovado | Histórico de admissões separado do claim atual; expirado não significa ator fisicamente retirado |
| Estado recuperável canônico | CharacterId + StateRevision; EconomicRevision/versão/hash associados | Imutável após commit; head aponta para versão correta. XP, bolsa, equipamento e direitos têm autoridade única |
| Operação/recibo | OperationId único no namespace durável, kind, CharacterId, intenção/fingerprint, base/resultado | Mesmo ID não muda conteúdo; resultado terminal não é sobrescrito; receipt referencia versão confirmada |
| Concessão/origem | GrantId único; unicidade econômica por beneficiário + origem tipada | Quest: namespace/id/ciclo aprovado. Mob: época/namespace/spawn/geração/tipo. Versão de definição é evidência, não replay automático |
| Direitos pendentes | No estado canônico, com GrantId e recibo de origem | Quantidade/origem não ficam duplicadas numa carteira paralela. Índice de claim só referencia concessão/recibo e impede segunda coleta |
| Watermarks | Beneficiário + tipo/fonte + época/domínio corretos | Não substituem recibo; não avançar apagando pendência legítima; D34 |
| Distribuição para beneficiários | ID de evento de origem, época/spawn/geração, elegíveis e resultados resolvidos | Manifesto/journal de origem separado do snapshot individual; não perder o beneficiário B após pagar A |
| Conteúdo/regras/política | IDs/hashes/versionamentos aprovados, referenciados por estado/operação | Catálogos não são copiados como gameplay executável por personagem; incompatibilidade recusa |

Snapshots/recibos históricos podem conter valores antigos, mas são evidência
somente de leitura. Não aceitar esses valores como uma segunda cabeça. Se
inventário ou direitos virarem linhas normalizadas canônicas, o snapshot é uma
exportação derivada dessas linhas, não outro documento gravável independente.
Índices/projeções carregam referências/versionamento e devem ser verificáveis;
discordância é erro de integridade, não oportunidade de escolher o saldo maior.

## Constraints e validação candidatas

**RECOMENDADA — D09/D10/D31; desenho físico PENDENTE DE APROVAÇÃO — D12.**

Requisitos de negócio: chave primária de personagem/operação; exclusão de claim
ativo concorrente; unicidade de versão, concessão e coleta; referências de
recibo ao mesmo beneficiário/versão; campos obrigatórios explícitos; quantidades
e revisões válidas; listas sem slots duplicados; domínio/atributos consistentes
com regras. ItemId pode repetir em slots legítimos. Retenção não deve usar
cascade que apague prova econômica ao remover uma projeção.

No PostgreSQL, CHECK não impede NULL sozinho nem é garantia para regras que
consultam outras linhas. UNIQUE normalmente distingue NULLs; a escolha de
nullabilidade/unicidade deve preservar chaves de origem. PK/UNIQUE/FK e índices
apropriados complementam validação transacional; FK não cria automaticamente
o índice do lado referenciador. [Constraints oficiais](https://www.postgresql.org/docs/18/ddl-constraints.html).

Campos de payload não ganham automaticamente FKs relacionais. A implementação
deverá mostrar o que é constraint física, o que é validação do codec/catálogo e
o que é regra transacional, com testes negativos. Não converter listas em Map
antes de detectar duplicatas, nem confiar em hash como autorização.

## Transação candidata de uma mutação econômica

**RECOMENDADA — D10/D11.** Protocolo lógico, sem SQL pronto para execução:

1. Validar comando imutável e credenciais de serviço fora da transação; nenhum
   lock deve esperar owner, renderer, consulta HTTP ou RNG.
2. Abrir transação curta, adquirir a cabeça do personagem na ordem estável
   aprovada e conferir a autorização atual de acesso. A consulta de resultado
   antigo não exige que a revisão/fence históricos ainda sejam os atuais.
3. Consultar OperationId dentro da fronteira de serialização. Confirmado e
   idêntico retorna recibo; divergente retorna conflito. Não atualizar recibo
   existente com um upsert que troque seu conteúdo.
4. Para transição ainda não confirmada, conferir claim, geração/fence e revisões
   esperadas, origem/concessão e eventuais intenções pendentes. Ausência de recibo
   só permite prosseguir quando o protocolo de exclusão resolve a transação
   anterior; não executar um SELECT e um crédito independente em seguida.
   Se o recibo era esperado e está inacessível, manter UNCERTAIN. Mudança de
   admissão não permite reapresentar intenção antiga com fence substituído.
5. Validar o resultado autorizado contra a base canônica e versões. Aplicar a
   proposta sem rerrolar drop ou conceder por flags. A operação conhece todos
   os campos que mudam, inclusive HP/cooldown se o comando exigir.
6. Inserir versão canônica, recibo, referências de grant/claim e watermarks,
   e mover a cabeça condicionalmente às revisões e autoridade atuais. Exigir
   exatamente uma cabeça afetada; zero é conflito, mais de uma é erro estrutural.
7. Qualquer falha aborta a transição inteira. Confirmar a transação sob o perfil
   de durabilidade aprovado antes de anunciar resultado definitivo.
8. Enfileirar resposta imutável ao owner. Se a resposta sumir, consultar pelo
   mesmo ID e autorização atual; não creditar outra vez.

Um registro durável de intenção/journal, caso aprovado, pode existir antes dos
efeitos como PENDING; isso não é recibo de pagamento. Sua aceitação, cancelamento,
reserva e retomada precisam de estados explícitos e testes. O commit dos efeitos
e seu recibo confirmado continua conjunto.

### Isolamento, locks e retry

**RECOMENDADA — D11:** avaliar Read Committed com lock explícito da cabeça e
predicados CAS em todos os escritores; alternativa Serializable para invariantes
que escapem desse conjunto de locks. Read Committed usa snapshots por comando
e reavalia a condição de UPDATE após esperar outra atualização. Serializable
pode exigir retry da transação inteira. Nenhum nível elimina a necessidade de
ID idempotente, unicidade e fence. [Isolamento oficial](https://www.postgresql.org/docs/18/transaction-iso.html).

Locks de linha duram até o fim da transação; deadlocks podem abortar trabalho.
Exigir ordem estável de CharacterId nos fluxos de grupo e transações curtas,
com timeouts/retry limitados. Não escolher automaticamente uma vítima lógica
nem usar advisory lock por processo como prova durável de posse. [Locks
oficiais](https://www.postgresql.org/docs/18/explicit-locking.html).

O teste deve mostrar todos os escritores disputando a mesma cabeça. Isolar só
a rota de prêmio e permitir autosave antigo por outra rota quebra o contrato.
Retry de serialization/deadlock deve manter a intenção/ID e revalidar autorização;
mudança de conteúdo de negócio é outra decisão, não atualização silenciosa do ID.

## Perfil de durabilidade e resposta incerta

**PENDENTE DE APROVAÇÃO — D12; BLOQUEADA POR EVIDÊNCIA — D28/D33.**

Proposta para créditos que serão anunciados como definitivos: armazenamento
registrado em WAL, sincronização local efetiva, fsync habilitado e perfil de
commit síncrono adequado ao cenário aprovado. PostgreSQL admite sucesso antes
de flush durável no commit assíncrono; isso pode perder transações recentes em
crash e não serve de prova para a garantia pretendida. [Commit assíncrono](https://www.postgresql.org/docs/18/wal-async-commit.html).

O comportamento de synchronous_commit depende também de standbys síncronos
configurados; confirmação local não implica automaticamente aplicação visível
numa réplica. fsync desabilitado altera as garantias de recuperação. Esses
parâmetros, armazenamento e testes de queda precisam constar da evidência,
sem considerar o nome PostgreSQL uma garantia suficiente. [Configuração WAL](https://www.postgresql.org/docs/18/runtime-config-wal.html).

Failover/restore e perda do nó exigem RPO/RTO e política de replicação próprios.
Replicação assíncrona pode deixar commits ainda não replicados de fora do nó
promovido. Não consultar réplica atrasada para declarar que uma operação nunca
existiu. [Standbys e replicação](https://www.postgresql.org/docs/18/warm-standby.html).

CONFIRMED só será usado no domínio de falha efetivamente testado. Sucesso na
máquina local não prova perda zero do armazenamento físico ou sobrevivência à
perda de todo o cluster. Restauração antiga não pode reautorizar fence, época,
concessão ou operação já consumida. Não há configuração aplicada nesta R6.

## Consultas mínimas e índices candidatos

**RECOMENDADA — D26/D31.** Consultas propostas, sempre com autorização atual:

- Resolver personagens de um proprietário e carregar a cabeça/claim por CharacterId.
- Disputar claim, renovar/revogar fence e consultar a operação de admissão sem
  admitir outra participação por uma resposta perdida.
- Carregar a versão canônica confirmada e verificar referências do seu ledger.
- Consultar OperationId completo e comparar kind/beneficiário/conteúdo original.
- Resolver GrantId/chave de origem e direitos pendentes com seus recibos.
- Listar operações/distribuições pendentes para reconciliação após crash/shutdown.
- Localizar recibos antigos no arquivo sem confundi-lo com ausência definitiva.

Índices candidatos seguem essas consultas: proprietário→CharacterId,
OperationId único, `(CharacterId, StateRevision)` único, grant por origem e
beneficiário, claim por GrantId, head por personagem e pendências por estado/
idade. Índices de FK dependem de joins, exclusão e planos reais. Medir com dados
representativos em E2, sem prometer melhoria quantitativa por recomendação.

## Retenção, crescimento e late settlement

**RECOMENDADA — D26; política final PENDENTE DE APROVAÇÃO — D12/D25/D34.**

| Estratégia candidata | Proteção exigida | Teste antes de ativar |
| --- | --- | --- |
| Recibos completos ativos | Limite de crescimento monitorado; pressão recusa/degrada de modo aprovado, sem purge | Capacidade, backpressure e consultas antigas |
| Arquivo de resultados completos | Catálogo de localização, integridade, autorização e restauração comprovados | Arquivo indisponível/corrompido deve dar incerto/bloqueio, nunca conceder novamente |
| Índice compacto de deduplicação/tombstone | Conservar identidade da operação/concessão, vínculo ao resultado e conteúdo necessário para distinguir conflito | Retry divergente, novo ID para grant antigo, restart e restore |
| Compactação de versões do estado | Cabeça e recibos referenciados continuam resolvíveis; não promover histórico a autoridade | Snapshot antigo, cadeia/referências e replay após compactação |
| Particionamento de histórico | Não enfraquecer unicidade global de OperationId/origem nem consulta entre arquivo e ativo | Duplicata atravessando partições e manutenção concorrente |

Não escolher TTL nem apagar recibos para caber no orçamento. “At most once” sem
capacidade de devolver o resultado antigo é insuficiente para reconciliação.
Separar marca de deduplicação do resultado arquivado só é aceitável com contrato
de disponibilidade e integridade que não converta falha de arquivo em NOT_FOUND.

**PENDENTE DE APROVAÇÃO — D34:** a R5 guarda a maior geração de mob e recusa
uma antiga não confirmada. Num sistema durável, uma origem já autorizada e
pendente precisa ser distinguida de replay sem prova. Alternativas: ordenar
settlements antes de avançar fonte; usar dedup por concessão e watermark somente
de prefixo liquidado comprovadamente; manter registro de lacunas pendentes.
Não descartar um prêmio devido só porque outra geração terminou primeiro.

Após mudança de admissão, uma intenção antiga não pode simplesmente trocar seu
fence no mesmo payload e reaplicar. Se faltar confirmação, resolver primeiro a
tentativa anterior; eventual novo settlement autorizado referencia a origem/
GrantId original, conserva resultado resolvido e volta a disputar unicidade.
Esse caminho é uma proposta separada, sem aprovação para criar novo ID como
escape de UNCERTAIN ou para reconstruir origem a partir de flag.

## Permissões, backup e implementação posterior

**RECOMENDADA — D31:** somente o servidor acessa o armazenamento, com menor
privilégio e operações autorizadas. Conta de manutenção/migration é separada
do serviço de runtime; logs não devem expor credenciais ou estado privado a
outro jogador. RLS/roles e isolamento de tenant são escolhas de desenho a
testar, não substitutos de autenticação ou do fence.

Backups precisam de restauração ensaiada, recibos/índices coerentes e proteção
contra ressurreição de autoridade antiga. Observabilidade deverá distinguir
confirmados, conflitos, incertos, fila, idade da pendência, divergência de
hash/revisão e falha de relógio. Sem migrar dados pessoais nesta etapa.

Toda construção física e execução dos cenários dependem dos
[gates de implementação](ORG-03-R6-GATES-DE-IMPLEMENTACAO.md).
