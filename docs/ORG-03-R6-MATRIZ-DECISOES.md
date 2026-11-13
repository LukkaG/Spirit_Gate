# ORG-03-R6 — Matriz de decisões

## Como ler os estados

Uma recomendação deste agente não aprova uma regra de jogo nem autoriza código.
As quatro classificações abaixo são exclusivas do **estado da decisão**. Prova
de execução é outro eixo: observado no checkout, comprovado no laboratório ou
teste futuro não executado. Documento existente não é evidência de homologação.

- **APROVADA ANTERIORMENTE:** restrição/invariante explicitamente autorizada nas
  ordens anteriores; seu alcance é descrito, sem estender aprovação ao banco.
- **RECOMENDADA:** escolha proposta nesta consolidação; requer aceite explícito.
- **PENDENTE DE APROVAÇÃO:** alternativa/parâmetro sem decisão do responsável.
- **BLOQUEADA POR EVIDÊNCIA:** garantia não demonstrada, que não pode ser afirmada
  ou usada para ativar personagem até produzir a evidência indicada.

## Registro central

| ID | Decisão/objeto | Estado | Fundamento e limite | Responsável/fechamento necessário |
| --- | --- | --- | --- | --- |
| D01 | Preservar runtime, protocolo 6/7, 40 skills, saves e escopo documental R6 | APROVADA ANTERIORMENTE | Ordens R1–R5 e restrições reiteradas em R6 | Desenvolvedor; nova ordem explícita para qualquer alteração |
| D02 | Legado sem prova não autoriza recuperação integral; alternativa A provisória | APROVADA ANTERIORMENTE | Gates R3/R4/R5, sem migração ativada | Manter restrição; decisão futura não é inferida da R6 |
| D03 | Não fabricar recibos de flags, não repetir RNG/prêmio por retry | APROVADA ANTERIORMENTE | Ordens R3–R5; prova apenas de laboratório para idempotência R5 | Persistência futura ainda precisa de transação e testes reais |
| D04 | Identificadores separados, CharacterId estável e emitido por autoridade do servidor | RECOMENDADA | ADR original de identidade; UUID de peer não é identidade permanente | Desenvolvedor/arquitetura; aprovar emissão e namespace |
| D05 | Provedor de autenticação, vínculo de propriedade e revogação de acesso | PENDENTE DE APROVAÇÃO | Ausentes no runtime; R5 tem controlador confiável de teste | Responsável por segurança/produto; especificar prova e testes de isolamento |
| D06 | Claim exclusivo com AdmissionGeneration e AuthorityFence verificados em cada escrita | RECOMENDADA | CAS de revisão sozinho não revoga escritor antigo | Arquitetura; teste de duas instâncias e ator antigo |
| D07 | Lease, expiração, renovação e suspensão segura do ator sem banco | PENDENTE DE APROVAÇÃO | Fence de armazenamento não retira corpo/combate por si só | Gameplay + infraestrutura; limites de tempo e prova de isolamento |
| D08 | WorldEpoch persistido para cada época de execução, sem derivação de UUID/tick | RECOMENDADA | R4 só conhece UUID efêmero e clocks locais | Arquitetura; emissão única, restart e validade das origens |
| D09 | Cabeça canônica única com StateRevision e EconomicRevision distintos | RECOMENDADA | R1 tem StateRevision; R5 tem EconomicRevision, sem mapeamento implementado | Aprovar relação e CAS conjunto; proibir autosave sobre economia nova |
| D10 | Efeito econômico, recibo, revisões e deduplicação na mesma transação durável | RECOMENDADA | R5 prova equivalente lógico apenas em RAM | Aprovar protocolo de armazenamento e perfil de confirmação |
| D11 | Transação curta, lock da cabeça e CAS explícito; isolamento a validar | RECOMENDADA | Plano PostgreSQL compara Read Committed e Serializable | Testes SQL reais antes de escolher/configurar implementação |
| D12 | Versão PostgreSQL, topologia, RPO/RTO, pool e perfil de WAL/failover | PENDENTE DE APROVAÇÃO | Nenhum banco foi conectado ou configurado nesta R6 | Infraestrutura + responsável; ensaios de falha e restore |
| D13 | Não transportar diferenças da R5 ao gameplay sem decisão | APROVADA ANTERIORMENTE | Ordem R5 proíbe alterar QuestRewards/LootService/InventoryService | Analisar P01–P12 no ADR de políticas |
| D14 | Teto de XP: truncar prêmio ou rejeitar operação inteira | PENDENTE DE APROVAÇÃO | Runtime e R5 divergem; ver P01 | Gameplay; testes de fronteira e efeito sobre domínio/HP |
| D15 | Moedas no teto: saturação de mob versus recusa de quest | PENDENTE DE APROVAÇÃO | Runtime já tem caminhos diferentes; ver P02 | Gameplay; não uniformizar silenciosamente |
| D16 | Identidade de entrega e eventual repetibilidade de missão | PENDENTE DE APROVAÇÃO | Hoje conclusão é permanente; versão nova não é autorização de replay | Conteúdo/gameplay; definir instância de repetição se existir |
| D17 | Coleta pendente por item versus pacote inteiro | PENDENTE DE APROVAÇÃO | Legacy e R5 divergem; recomendar compatibilidade por item, com origem preservada | Gameplay + economia; não agregar direitos perdendo procedência |
| D18 | Poção/equipamento/XP precisam incluir efeitos de gameplay coerentes | RECOMENDADA | R5 não cobre HP, cooldown nem equipamento; P09–P11 | Aprovar escopo do agregado e protocolo owner/commit |
| D19 | Múltiplos beneficiários: commits individuais com intenção de distribuição durável | RECOMENDADA | Preserva independência observada; completude do grupo ainda não foi provada | Aprovar produtor de origem/manifestações e teste de crash entre beneficiários |
| D20 | Candidata: cooldown continua transcorrendo desconectado | RECOMENDADA | Recomendação original, nunca aprovação; sem cura/regeneração automática | Gameplay; aprovar política e homologar relógio antes de converter |
| D21 | Conversão temporal durável, âncoras, precisão e detecção de salto de relógio | BLOQUEADA POR EVIDÊNCIA | Não há mapeador homologado; R4 não fornece época/tempo duráveis | Provas temporais E2/E3; ausência deve continuar explícita |
| D22 | Tratamento offline de ROOT/STUN/SLOW/BLEED/BURN e efeitos futuros | PENDENTE DE APROVAÇÃO | Duração/penalidade offline é regra de jogo; ledger externo incompleto | Gameplay; sem limpeza automática nem replay de dano antigo |
| D23 | Não restaurar projéteis, dash, intangibilidade, sentinela, marcas e eventos/comandos antigos | APROVADA ANTERIORMENTE | Restrições explícitas R1–R5 e A-04 | Preservar custos/penalidades; isso não autoriza apagá-los |
| D24 | Worker externo, fila limitada, uma escrita em voo por personagem e callback revalidado | RECOMENDADA | R4 não tem fila de persistência; R5 não roda no owner real | Aprovar barreiras dos campos afetados e ponto de aceitação durável |
| D25 | Itens/bytes da fila, prazo/retry, orçamento por tick e shutdown | PENDENTE DE APROVAÇÃO | Números de laboratório não são capacidade de produção | Medir carga; estabelecer limites finitos e comportamento de contingência |
| D26 | Recibos consultáveis + índice durável de deduplicação; arquivamento sem abrir replay | RECOMENDADA | R5 recusa ao atingir limite; não faz GC de recibos | Aprovar retenção com prova de consultas antigas e recuperação |
| D27 | Alternativa C como candidata de migração administrativa, sem ativá-la | RECOMENDADA | A continua restrição; B/C exigem decisão e auditoria independentes | Responsável pelo projeto e pelos dados, com procedimento de contestação |
| D28 | Durabilidade real, crash entre processos e leitura consistente do ledger | BLOQUEADA POR EVIDÊNCIA | AtomicReference e novos Coordinator não comprovam disco/restart | E2/E3 PostgreSQL descartável, logs e evidências de restart |
| D29 | Ativação segura: identidade, efeitos, relógio, conteúdo e spawn atuais | BLOQUEADA POR EVIDÊNCIA | R1/R4 continuam sem autorização de ativação | E4, após políticas aprovadas; sem instalar estado parcial |
| D30 | A-04 e TASK-226 são correções separadas; não ativar timeout de 92 ticks | APROVADA ANTERIORMENTE | Ordens e relatórios TASK-225/ORG-03 | Autorização de gameplay própria, não consequência da R6 |
| D31 | Serviço de armazenamento com menor privilégio e consultas autorizadas por proprietário | RECOMENDADA | Cliente nunca obtém acesso direto ao banco por conhecer ID | Arquitetura/segurança; avaliar defesa adicional e testes por tenant |
| D32 | Capacidade futura de autenticação/recuperação no transporte | PENDENTE DE APROVAÇÃO | Protocolo 6/7 não carrega o contrato futuro | Proposta de compatibilidade separada; sem novo handshake nesta R6 |
| D33 | Restore/failover não pode reutilizar fence, época ou autoridade antiga | BLOQUEADA POR EVIDÊNCIA | Backup pode retroceder contadores; processo antigo pode continuar vivo | Provar revogação global/era de autoridade e restauração antes de admitir |
| D34 | Watermark e concessão antiga legitimamente pendente | PENDENTE DE APROVAÇÃO | R5 rejeita geração antiga não confirmada; persistência futura não pode apagar obrigação comprovada por intenção durável | Economia/arquitetura; ordenar pendências ou definir settlement autorizado, com testes E2/E3 |

## Duplicações, conflitos e decisões ainda abertas

| Referência anterior | Situação observada | Consolidação R6 |
| --- | --- | --- |
| Contrato ORG-03 começa dizendo que não existe DTO | R1 criou DTO/validador depois; texto antigo é histórico | Usar tipos atuais R1 como referência estrutural, nunca como importador aprovado |
| ADR original pede UUID estável/fence | R4 só liga Session/peer/Thread reais da instância atual | D04–D08 são contratos futuros, não funcionalidades já implementadas |
| Plano original lista progressão/carteira e payload | Poderia ser lido como duas autoridades de XP/saldo | D09 escolhe recomendar um estado canônico; histórico é somente leitura |
| Contrato original recomenda tempo offline contínuo | A redação não equivale a decisão de gameplay | D20 recomendado; D21 ainda bloqueado por evidência |
| Contrato usa origem temporal única em alguns campos | R4 mostra worldTick, sessionTick e engineClock distintos | Cada prazo precisa de origem explícita, sem assumir igualdade |
| R1 aceita decimal até 9 casas; R3 conserva double/NaN nativos | Nem todo valor nativo cabe exatamente nesse formato | Sem conversão silenciosa; versão/precisão dependem de D21 |
| Plano antigo inclui versão de quest na chave candidata | Isso poderia pagar novamente só por atualizar definição | D16 pendente; preservar conclusão atual e manter versão como evidência, não chave automática de replay |
| QuestRewards versus concessão de mob | XP/domínio e limites de moedas variam por caminho | P01/P02 registram diferenças; R5 não escolhe regra de produção |
| Pending legacy agregado por item versus GrantId R5 | Legado perdeu associação completa de origem | Não reconstruir recibo; D02/D17/D27 |
| R5 CAS da imagem inteira | PostgreSQL/owner são fronteiras diferentes | D10/D24/D28 exigem provas adicionais; sem transação mágica RAM+banco |
| R5 watermark de maior geração | Evento antigo não confirmado é recusado; uma intenção durável pendente futura não pode ser esquecida por esse atalho | D34 exige decisão de ordenação/settlement; não alterar a R5 nesta tarefa |
| Relatórios de 1196 testes aprovados | Prova anterior em headless | R6 só verifica documentos/hashes; não atribui nova execução a esses testes |

O registro central deve receber o aceite explícito, escopo, data e responsável
quando uma decisão mudar de estado. Nenhum desses campos foi preenchido em nome
do desenvolvedor por esta R6. Ver [gates](ORG-03-R6-GATES-DE-IMPLEMENTACAO.md).
