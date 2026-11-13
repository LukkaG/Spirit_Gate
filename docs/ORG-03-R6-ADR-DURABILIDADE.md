# ORG-03-R6 — ADR de durabilidade, identidade e continuidade

## Estado da decisão

**RECOMENDADA — D04, D06, D08–D11, D18, D19, D24, D26 e D31.**
É uma especificação candidata, não aprovação de implementação. Autenticação,
lease, topologia e protocolo dependem de D05/D07/D12/D32; provas duráveis estão
bloqueadas em D28/D33. Estados e identificadores abaixo ainda não foram criados
no runtime pela R6. Ver [registro central](ORG-03-R6-MATRIZ-DECISOES.md).

## Identificadores separados

| Conceito | Emissor futuro proposto | Persistência/validade proposta | Uso e proibição |
| --- | --- | --- | --- |
| AccountId/proprietário | Autoridade de identidade aprovada, associada pelo servidor | Durável; vínculo e revogação auditáveis | Quem pode solicitar o personagem; não nome/IP/UUID informado como senha |
| CharacterId | Serviço confiável do servidor | Estável e durável durante a vida do personagem | Raiz do agregado; não derivar de Peer.id, nome ou world.instance |
| Peer.id | LocalSimulationServer atual | Temporário, por instância/conexão | Roteamento do transporte; não persistir como dono |
| SessionId | Coordenador de admissão futuro | Identifica uma admissão; registro durável de ciclo de vida, sem reutilização para outra | Não é socket, operação econômica ou identidade do personagem |
| AdmissionGeneration | Autoridade durável por personagem | Crescente a cada nova admissão lógica; validade termina ao revogar/substituir | Revalidar input/callback; retry da mesma admissão não cria outra geração |
| AuthorityFence | Armazenamento/autoridade de claim | Ordem durável de autoridade, atualizada em aquisição/revogação conforme política | Escritor com fence antigo não confirma; geração e fence não precisam ser o mesmo contador |
| WorldEpoch | Coordenador durável de épocas de execução | Novo domínio de execução/relógio; identidade persistida e não reutilizada | Separa origens de mobs/clock; não é contentRevision nem UUID truncado |
| StateRevision | Commit da cabeça canônica do personagem | Monotônica por personagem; sem reset/overflow | Ordem de estado confirmado, não número de frame |
| EconomicRevision | Commit de uma mutação econômica | Avança junto da StateRevision quando afeta economia | Não cria outra autoridade de XP/moedas |
| OperationId | Servidor/produtor confiável de intenção | Estável durante tentativas, reconciliação e retenção; persistido com a operação | ID não autoriza efeito; conteúdo/kind/beneficiário são conferidos |

Formatos físicos, algoritmo de emissão e bootstrap durável são **PENDENTE DE
APROVAÇÃO — D04/D05/D08/D12**. Os tipos UUID/long existentes não comprovam essas
semânticas. `BeneficiaryId` da R5 não vira AccountId/CharacterId automaticamente.
Sequência de mob e geração de admissão são domínios distintos.

AuthorityFence exige proteção contra rollback do próprio armazenamento. Repor
backup antigo pode repetir contadores e admitir escritor já revogado. **BLOQUEADA
POR EVIDÊNCIA — D33:** antes de admitir após restore/failover, provar revogação
das execuções antigas ou uma era de autoridade não reutilizável, com procedimento
operacional auditado. WorldEpoch sozinho não substitui esse mecanismo.

## Fluxo futuro de propriedade e admissão

**RECOMENDADA — D04/D06; dependências PENDENTE DE APROVAÇÃO — D05/D07/D32.**

1. Autenticar o proprietário pelo mecanismo aprovado, fora do snapshot. Não
   há definição de senha, token ou handshake implementada nesta etapa.
2. Receber escolha de CharacterId e conferir no servidor seu vínculo atual
   com o proprietário e autorização de acesso. Revalidar revogação quando necessário.
3. Criar/consultar uma operação de admissão idempotente e disputar claim
   exclusivo no armazenamento. Duas solicitações concorrentes não podem obter
   dois claims ativos para o mesmo personagem.
4. Atribuir SessionId, AdmissionGeneration e AuthorityFence na fronteira durável
   aprovada. Uma resposta perdida consulta a admissão existente; não incrementa
   geração de novo como se fosse uma escolha independente.
5. Ler somente o estado confirmado e seu ledger. Conferir formato, ambas as
   revisões, políticas, conteúdo/regras, relógio e restrições. Ausência não é zero.
6. Preparar candidato e reservar spawn no owner, sem corpo público, input,
   recompensa ou execução. Não manter transação SQL aberta aguardando essa reserva.
7. Confirmar a fase de admissão durável, com fence atual, e enfileirar ativação.
   O owner revalida identidade de Session, geração/fence, conteúdo, reserva e
   estado antes de instalar. Commit e corpo em RAM são fronteiras diferentes.
8. Em falha pós-commit, reter a admissão como reconciliável. Não apagar documento
   nem compensar prêmio com uma segunda concessão. Novo owner usa a cabeça atual.
9. Para encerrar, impedir novos comandos incompatíveis, resolver escritas em voo,
   capturar estado permitido antes da retirada e revogar autoridade. Liberar o
   claim só quando não houver ator/escritor antigo ainda autorizado.

Fases candidatas: RETIDO → CLAIMED/PREPARING → COMMITTED_PENDING_ACTIVATION →
ACTIVE → QUIESCING → RETIDO; RECONCILING pode interromper o fluxo. São rótulos
de projeto, sem enum novo ou implementação. Recibo econômico não é recibo de
admissão, nem a confirmação de uma admissão garante que o corpo foi instalado.

**PENDENTE DE APROVAÇÃO — D07:** expirar lease no banco não faz um corpo antigo
parar de causar dano em outro processo. É necessário limitar e revogar também
a autoridade da simulação, com suspensão segura diante de partição. Sem essa
prova, negar nova ativação concorrente. Não usar desconexão A-04 como atalho.

## Uma cabeça canônica e duas revisões

**RECOMENDADA — D09.** Cada personagem tem uma cabeça que aponta para um estado
recuperável versionado canônico. Esse estado contém a economia uma única vez.
Normalização seletiva é possível, mas não manter saldo/XP graváveis independentes
num JSON e numa tabela. Recibos e snapshots históricos são somente leitura;
não são uma segunda cabeça de estado.

Uma transação econômica confere `(CharacterId, StateRevision-base,
EconomicRevision-base, AdmissionGeneration, AuthorityFence)` e avança ambas
as revisões na mesma confirmação. Uma escrita não econômica pode preservar
EconomicRevision, mas precisa respeitar a cabeça econômica atual. Nenhum
autosave de posição/HP pode sobrescrever uma economia mais nova com o blob de
uma captura antiga. A estratégia de patch/payload continua dependente de prova
de coerência; não basta alterar o número da revisão e reenviar o snapshot velho.

Exemplo normativo proposto: base `(s=10,e=4)` e duas mutações econômicas distintas
concorrem; no máximo uma confirma `(11,5)`. A outra conflita. Retry idêntico da
vencedora devolve seu recibo, mesmo que a cabeça já esteja em `(12,6)`, sem
instalar o estado histórico `(11,5)`. Os números são exemplo, não dados do jogo.

## Fronteira econômica durável

**RECOMENDADA — D10/D11.** Intenção aceita carrega identidade, kind, beneficiário,
origem/concessão, revisões esperadas, geração/fence, época e resultado autorizado
pré-resolvido, com versões de conteúdo/regras. Cliente não fornece delta de
saldo aceito por confiança. Fonte de recompensa deve ser validada pelo servidor.

A mesma transação confirma XP/domínio afetados, moedas, bolsa, direitos,
entrega da missão, revisões, recibo e índices de concessão/watermarks necessários.
Qualquer conflito aborta a transição inteira. Não gravar recibo separado depois
de anunciar o crédito. Não recalcular drop, quest ou XP para reconstruir recibo.

Se houver um registro durável de **intenção pendente**, ele é identificado como
tal e não comprova pagamento. A aceitação, o journal de origem e a política de
reserva são decisões explícitas D24/D25. Sem journal não se promete sobreviver
à perda de comandos ainda apenas em RAM. Resultado final não pode ser anunciado
antes da confirmação durável do perfil aprovado.

| Resposta proposta | Significado verificável | Ação do chamador |
| --- | --- | --- |
| CONFIRMED | Recibo e efeitos confirmados na mesma fronteira durável, ou recibo idêntico encontrado | Publicar/reconciliar no owner atual; não aplicar delta novamente |
| REJECTED | Regra/autorização inválida com certeza de que essa tentativa não confirmou efeito | Informar motivo; não transformar em nova concessão automaticamente |
| CONFLICT | Revisão/fence/concessão/ID-conteúdo divergente; transição desta tentativa não aplicada | Relê/revalida; ID divergente não é retry aceitável |
| UNCERTAIN | Não foi possível determinar com segurança se confirmou | Consultar pelo OperationId após nova autorização; manter bloqueadas ações conflitantes |

REJECTED por uma tentativa não pode negar um commit anterior: se existir recibo
da mesma operação, retornar o resultado confirmado após autorização. Falha de
conexão/timeout não é REJECTED apenas porque não veio resposta. O resultado
confirmado guarda valores autorizados e efetivamente aplicados, inclusive
truncamentos se a política aprovada os permitir.

### Consulta e reconciliação

**RECOMENDADA — D10/D26/D31.** Reautenticar/reautorizar o solicitante atual e seu
acesso ao CharacterId, depois buscar OperationId e conferir kind/beneficiário/
conteúdo original. O contexto de acesso atual pode ser novo; a intenção original
e seu fence histórico não são reescritos para fingir retry de outra admissão.

Recibo encontrado deve ser íntegro e ligado ao estado confirmado. Outra pessoa
não recebe dados por saber seu ID. Recibo ausente numa leitura atrasada, durante
transação em voo ou com histórico indisponível não prova que nada aconteceu.
Uma repetição segura mantém o mesmo ID/intenção e atravessa os mesmos locks,
unicidades e CAS; nunca cria outro ID para tentar escapar da incerteza.

Só um protocolo testado, com autoridade atual, leitura da fonte correta e
resolução da transação anterior pode concluir ausência definitiva. Ausência do
ledger ou de arquivo histórico esperado é falha de integridade/UNCERTAIN, não
licença para recalcular recompensa. O [plano PostgreSQL](ORG-03-R6-PLANO-POSTGRESQL.md)
detalha concorrência, consultas e retenção.

## Múltiplos beneficiários e origem persistida

**RECOMENDADA — D19, sem aprovação de implementação.** O runtime concede por
contribuidor elegível; a R5 prova transações independentes por beneficiário,
não sobrevivência de uma distribuição inteira a crash. Recomenda-se uma intenção
durável de distribuição com origem única, elegíveis e resultados resolvidos por
beneficiário, seguida de commits individuais idempotentes. Um crash entre A e B
deve deixar B identificável como pendente, sem rolar RNG outra vez.

Alternativa: uma transação única para todo grupo, com locks ordenados. Ela acopla
o sucesso dos participantes e pode mudar o gameplay. A criação da origem, seu
ponto de aceitação e sua atomicidade com morte/loot globais ainda exigem decisão
própria; não pertencem automaticamente ao snapshot individual. Sem manifesto
ou transação de grupo provados, não afirmar que todos receberam após restart.

## Fronteira assíncrona e contingências

**RECOMENDADA — D18/D24; parâmetros PENDENTE DE APROVAÇÃO — D25.**

1. No ponto permitido do ciclo, o owner captura coerentemente e valida a ação.
2. Prepara comando imutável com OperationId, CharacterId, conteúdo resolvido,
   versões, revisões esperadas, geração/fence e origem temporal. Não entrega
   RpgWorld, mapas/arrays mutáveis ou Session ao worker.
3. Reserva capacidade e campos conflitantes; a oferta à fila limitada não espera
   banco nem bloqueia o tick. A política de reserva ainda requer aceite.
4. Worker externo lê o comando e abre transação curta; nenhuma chamada de volta
   ao owner ocorre mantendo lock SQL. Uma escrita em voo por personagem é a
   candidata inicial, não configuração já presente.
5. A transação verifica autorização/fence/revisões, confirma todos os efeitos
   e recibo juntos ou termina sem efeitos parciais; conexão perdida pode tornar
   o resultado incerto. O worker não altera a simulação diretamente.
6. Retorna resultado imutável por canal limitado; a capacidade de retorno e a
   reconciliação durável não podem depender de descartar silenciosamente sucesso.
7. No ciclo seguinte permitido, o owner revalida Session atual, geração/fence,
   revisões, conteúdo e reserva; publica somente os campos preparados válidos
   ou agenda reconciliação. Nunca reaplica deltas de recibo já observado.

| Condição | Política candidata e limite |
| --- | --- |
| Fila cheia antes de aceitar | Recusar/adiar a ação antes de consumir ou publicar benefício. Não alegar que salvou; liberar apenas reserva sem efeito e sem obrigação já durável |
| Worker indisponível / banco fora antes do envio | Interromper novas aceitações afetadas e sinalizar indisponibilidade; não acumular fila ilimitada nem bloquear tick. Obrigações já aceitas duravelmente continuam pendentes |
| Timeout durante execução / perda de resposta | UNCERTAIN até resolução; manter mesma intenção/ID e impedir ação conflitante. Não tratar cancelamento local como rollback confirmado |
| Callback atrasado / canal de retorno cheio | Conservar confirmação no armazenamento e retomar consulta; callback antigo não altera ator novo. Capacidade de retorno e indicadores de reconciliação têm orçamento limitado |
| Falha após commit | Recibo/efeitos continuam confirmados no perfil aprovado. Retomar por consulta autorizada, sem RNG, cura, prêmio ou débito repetido |
| Personagem desconectado antes da confirmação | Encerrar novas ações; conciliar transação em voo e cabeça durável antes de nova ativação. Liberação do claim exige protocolo, não um simples finally local |
| Shutdown | Fechar admissão de novos comandos, drenar até prazo aprovado e registrar pendências duráveis. Não anunciar sucesso para o que só ficou em RAM; ao expirar o prazo, conservar incerto o que pode ter confirmado |

Indisponibilidade pode exigir suspender ações conflitantes e a autoridade do ator;
o comportamento de combate/movimento e a duração dessa suspensão precisam de
D07/D18/D25. Não se introduz aqui desconexão automática, timeout A-04 ou
promessa de disponibilidade durante partição. F08/F18/F20/F27 cobrem os ensaios.

## Publicação no owner e janela de perda

**RECOMENDADA — D18/D24; limites PENDENTE DE APROVAÇÃO — D25.** Uma escrita em
voo por personagem simplifica a primeira integração. O owner precisa reservar
os campos e efeitos conflitantes ou manter estado preparado sem benefício
irreversível até confirmação. Movimento/combate só podem continuar quando não
invalidarem essa reserva; não aplicar um snapshot inteiro atrasado sobre HP,
posição ou economia que já mudou.

Congelar combate, adiar morte/prêmio ou usar projeção otimista são escolhas de
gameplay e disponibilidade, não ajustes automáticos desta arquitetura. Até
definir esse comportamento, a integração de escrita permanece bloqueada. Um
callback com geração/fence antigo pode corresponder a commit real: descartar
seu poder de mutar o ator atual, mas conservar o recibo e encaminhar reconciliação.

Sem confirmação, fila em RAM não é backup. Com confirmação, desconexão ou falha
de ativação não autoriza esquecer o estado durável. A garantia proposta é
idempotência dos efeitos e reconciliação, não entrega exatamente uma vez de
mensagens nem atomicidade automática entre RAM e PostgreSQL.
