# ORG-03-R7 — Quadro de aprovação E1

## O que está pronto para revisão

**Nenhum aceite registrado nesta R7.** Os estados abaixo são cópia do registro
[D01–D34 da R6](ORG-03-R6-MATRIZ-DECISOES.md), que permanece intacto: 6 decisões
aprovadas anteriormente, 13 recomendadas, 11 pendentes e 4 bloqueadas por
evidência. Recomendações nesta R7 estão **NÃO APROVADAS**.

As duas tabelas se completam por ID: a primeira apresenta estado/regra/escolha;
a segunda, impactos/teste/gate/escopo exato do eventual aceite. Não há pedido de
aceite único da arquitetura. Para registrar uma decisão futura: ID, alternativa
escolhida, recorte (laboratório ou jogo), responsável, data, ressalvas e evidência.
Campo em branco continua pendente; silêncio não aprova.

E2 = SQL sintético em cluster novo; E3 = prova entre processos/falhas reais;
E4 = owner/ativação; produção inclui homologação/implantação E5. “Não bloqueia
E2” não autoriza sua execução: L01–L05 do [escopo](ORG-03-R7-ESCOPO-E2.md)
continuam sem aceite. Uma garantia bloqueada não se resolve por votação.

## Estado, regra existente e alternativas

| ID | Estado atual R6 | Regra/limite existente no Spirit Gate | Alternativas disponíveis | Recomendação fundamentada — NÃO APROVADA |
| --- | --- | --- | --- | --- |
| D01 | APROVADA ANTERIORMENTE | Runtime/dados preservados; R7 só prepara E1 | Manter escopo; nova tarefa específica para código | Cumprir preservação; evita regressão fora do pedido |
| D02 | APROVADA ANTERIORMENTE | Legado sem prova não ativa recuperação integral | A restrição; B quarentena; C abertura auditada | Manter A provisória e originais; não equivale a apagar progresso |
| D03 | APROVADA ANTERIORMENTE | R5 deduplica em RAM; flags não geram recibo | Resultado resolvido; repetir RNG; fabricar histórico | Manter vedação de rerrolagem/recibo retroativo; resultado original é evidência |
| D04 | RECOMENDADA | R4 identifica participação/peer, não proprietário permanente | IDs próprios; reutilizar peer/nome/IP | IDs separados emitidos pela autoridade; peer é efêmero e não prova posse |
| D05 | PENDENTE DE APROVAÇÃO | R4/R5 não fornecem prova durável de autenticação/posse | Provedor a definir; fixture sintética só para ensaio | Adiar escolha real; usar mapa confiável sintético fora do comando no E2 |
| D06 | RECOMENDADA | Geração/época R5 são sintéticas; sem claim persistente integrado | CAS só de revisão; claim exclusivo com fence | Claim/fence além de revisão, pois revisão não revoga escritor antigo |
| D07 | PENDENTE DE APROVAÇÃO | Não há suspensão de ator antigo comprovada por lease | Sem readmissão até retirada provada; lease com suspensão segura | Negar ativação concorrente sem prova; não presumir que expiração SQL para combate |
| D08 | RECOMENDADA | Captura R4 tem UUID/clocks locais, sem época durável | Época emitida/persistida; derivar tick/UUID | Época própria não reutilizável, para separar origens e reinícios |
| D09 | RECOMENDADA | R1 e R5 têm revisões diferentes, sem adapter durável | Cabeça única; duas autoridades de saldo/XP | Uma cabeça canônica, revisões distintas e CAS conjunto; evita autosave regressivo |
| D10 | RECOMENDADA | Efeito/recibo/índices atômicos só na imagem RAM R5 | Transação conjunta; gravações independentes | Confirmar tudo junto, com ID/intenção imutáveis; evita crédito sem comprovante |
| D11 | RECOMENDADA | CAS em memória não é controle SQL | Read Committed + lock/CAS; Serializable com retry | Comparar ambos no E2; candidata simples usa cabeça bloqueada em todos os escritores |
| D12 | PENDENTE DE APROVAÇÃO | R6 não escolheu perfil de banco; instalação local 18.6 existe | Cluster local isolado; container autorizado; topologia futura | Ensaiar binários existentes em cluster novo; não decidir topologia/RPO de produção |
| D13 | APROVADA ANTERIORMENTE | Regras R5 não substituem produção | Comparar perfis; importar laboratório como regra | Preservar o jogo enquanto diferenças são decididas por origem |
| D14 | PENDENTE DE APROVAÇÃO | XP trunca; domínio recebe nominal; HP pode mudar por nível. R5 rejeita excesso | Truncar; recusar agregado inteiro; política por origem | Preservar regra atual por origem, gravar nominal/efetivo e efeitos em domínio/HP |
| D15 | PENDENTE DE APROVAÇÃO | Quest recusa moedas acima do teto; mob satura. R5 recusa tudo | Preservar diferença; uniformizar recusa; uniformizar saturação | Preservar diferenças inicialmente; não esconder mudança econômica numa migração |
| D16 | PENDENTE DE APROVAÇÃO | Entrega única observada; builtin/authored separados | Única; repetível com ciclo; reabrir por versão | Manter única; versão identifica definição, não nova dívida; repetição exige ciclo próprio |
| D17 | PENDENTE DE APROVAÇÃO | Pending agregado por item, sem procedência completa; R5 transfere pacote | Por item; por direito; pacote inteiro | Compatibilidade por item com referências individuais; não fabricar origens do legado |
| D18 | RECOMENDADA | Poção/equip/XP alteram campos além da bolsa R5 | Comando completo; só saldo/bolsa; projeção preparada | Definir agregado completo por comando antes de persistir; evita cobrança sem cura/efeito |
| D19 | RECOMENDADA | Concessões individuais; R5 não prova grupo sobrevivendo a crash | Transação global; manifesto + commits individuais | Manifesto de elegíveis/resultados e commits idempotentes; preserva independência com pendência rastreável |
| D20 | RECOMENDADA | Continuidade offline é proposta antiga, sem aprovação | Transcorrer; congelar; política por canal | Manter candidata de transcorrer, sem regeneração automática e só após D21 |
| D21 | BLOQUEADA POR EVIDÊNCIA | Múltiplos clocks; double/NaN R3 versus decimal R1 | Codec exato com erro provado; recusar conversão desconhecida | No E2, provar representação/recusa; não escolher fórmula ou declarar tempo durável |
| D22 | PENDENTE DE APROVAÇÃO | Fonte de efeitos do jogador incompleta; Dot do engine é de Enemy | Política separada por efeito; retenção quando desconhecido | Ver quadro por efeito abaixo; ausência não expira penalidade |
| D23 | APROVADA ANTERIORMENTE | Executores transitórios não são estado recuperável | Excluir execução; recriar projétil/fase/evento | Excluir execução mantendo custos, cooldowns e descritores adversos |
| D24 | RECOMENDADA | R4 captura no owner, sem fila de persistência | Owner bloqueante; fila limitada + worker; projeção otimista | Worker externo, uma escrita em voo por personagem e callback revalidado; barreiras ainda precisam de política |
| D25 | PENDENTE DE APROVAÇÃO | Limites de fixtures não são capacidade operacional | Valores experimentais; produção medida; fila ilimitada | Ensaiar limites explícitos no E2; owner/lease/shutdown de produção ficam separados |
| D26 | RECOMENDADA | R5 recusa ao atingir cap, não remove recibos | Histórico ativo; arquivo + índices; compactação comprovada | Manter resultado consultável e dedup mesmo arquivado; nunca purge que reabra grant |
| D27 | RECOMENDADA | A provisória; nenhuma migração B/C ativa | A; B não confirmado; C abertura administrativa | C candidata futura, com inventário auditado/backup/contestação e autorização própria |
| D28 | BLOQUEADA POR EVIDÊNCIA | Nenhuma durabilidade entre processos provada | SQL real E2; crash/restart E3; alegar RAM persistente | Produzir evidência por etapa; não prometer recuperação com teste concorrente apenas |
| D29 | BLOQUEADA POR EVIDÊNCIA | Validação R1 não autoriza ativar; faltam posse/tempo/efeitos/spawn | Reter; ativar só com gates satisfeitos | Manter retenção sem corpo parcial até provas de ativação E4 |
| D30 | APROVADA ANTERIORMENTE | A-04/TASK-226 separados; sem timeout 92 ticks | Correção específica; misturar com persistência | Manter separação; nenhum timeout/desconexão nesta R7/E2 |
| D31 | RECOMENDADA | R5 usa controlador confiável, não segurança de contas real | Roles limitadas + autorização; acesso pelo ID; superusuário no runner | Menor privilégio e reautorização; IDs não são credenciais |
| D32 | PENDENTE DE APROVAÇÃO | Protocolo 6/7 não implementa contrato futuro de recuperação | Capacidade/compatibilidade a especificar; harness sem transporte | E2 não usa rede do jogo; adiar qualquer handshake até decisão específica |
| D33 | BLOQUEADA POR EVIDÊNCIA | Backup pode retroceder fence/época/índices | Revogação global provada; era não reutilizável; negar admissão | Não admitir após restore sem prova de revogação/autoridade nova; E2 não fecha esse risco |
| D34 | PENDENTE DE APROVAÇÃO | R5 máximo de geração rejeita antiga não confirmada | Ordenação; prefixo liquidado; lacunas explícitas | Comparar os três; candidata registra obrigações e prefixo comprovado sem apagar pendência antiga |

## Impactos, prova, gate e escopo do aceite

“Jogo” na última coluna solicita uma decisão futura de política/contrato, não
autorização de implementá-la nesta R7. “Laboratório” limita o aceite a L01–L05.
As seis restrições já aprovadas não precisam ser aprovadas de novo.

| ID | Consequência para o jogador | Consequência para o backend | Teste necessário | Gate que bloqueia | Escopo exato do aceite a registrar |
| --- | --- | --- | --- | --- | --- |
| D01 | Nenhuma mudança de experiência | Fontes/dados intactos | Hash/diff R7 | Restrição de todas as etapas | Nenhum novo aceite; cumprir escopo já autorizado |
| D02 | Integral recusada, progresso preservado | Não promover flags a economia comprovada | C09/F15 | Uso de legado em E4/E5; não E2 sintético | Nenhum novo aceite de A; B/C somente em D27 |
| D03 | Retry não cria outro prêmio | Guardar resultado/origem, não só seed | C04/C06 | Restrição E2–produção | Nenhum novo aceite; ensaio técnico depende de L01/L03 |
| D04 | Personagem mantém identidade entre conexões | Separar proprietário, personagem e peer | C11; E3/E4 posse real | E2 requer só IDs de fixture; contrato real antes de E4 | Laboratório: IDs sintéticos; jogo: emissão/namespace estáveis sem implementar login |
| D05 | Impede acesso a personagem alheio | Vínculo de posse/revogação ainda por definir | C11 limitado; F21 E3/E4 | Não bloqueia E2 sem contas; bloqueia E4 real | Não aprovar provedor agora; permitir só proprietário sintético em L01 |
| D06 | Evita dois donos ativos do mesmo personagem | Claim/fence em cada escrita | C11 e F19/F20 | E2 precisa do modelo sintético; E3/E4 exclusividade real | Laboratório: CAS/claim simulado; jogo: contrato de exclusividade separado de lease |
| D07 | Suspensão/recusa pode afetar disponibilidade | Fence SQL não remove corpo antigo | F19/F20 entre processos/atores | Não E2; E3 de lease e E4 | Adiar lease/durações; exigir prova antes de readmissão concorrente |
| D08 | Evita duplicar recompensa de outra execução | Persistir época com origens/clocks | C04/C13; F28 E3 | E2 só época sintética; E3/E4 emissão real | Laboratório: separar épocas; jogo: emissor durável sem reutilização |
| D09 | Não perde XP/bolsa por checkpoint atrasado | Uma cabeça; revisões monotônicas/CAS | C02/C08 | Contrato mínimo E2; prova real em E3/E4 | Autorizar ensaio da cabeça única; não escolher JSON versus normalização de produção |
| D10 | Tudo recebido uma vez, ou sem efeito parcial | Commit conjunto e lookup de recibo | C03–C07 | Contrato mínimo E2; E3 para durabilidade | Ensaiar atomicidade SQL/recibo/revisões/dedup, sem anunciar salvamento de jogador |
| D11 | Contenção pode produzir conflito/retry | Locks curtos, unicidades e isolamento | C02/C15 | E2 requer perfil experimental explícito | Comparar Read Committed com lock/CAS e Serializable; não selecionar tuning definitivo |
| D12 | Nenhuma mudança atual; futura garantia depende do perfil | Versão/topologia/configuração verificáveis | Alvo/versão E2; F28/F30 E3 | Ambiente bloqueia execução E2; perfil definitivo bloqueia E3/produção | L02: binários 18.6/cluster/porta próprios; não RPO/RTO, standby ou failover |
| D13 | Preserva regras conhecidas | Não copiar transition R5 ao runtime | Hash e C03 perfis rotulados | Restrição geral | Nenhum novo aceite; comparar sem alterar jogo |
| D14 | No teto, itens/moedas/domínio não somem por recusa nova implícita | Recibo distingue nominal/efetivo e HP | C03 fronteiras de XP/nível/domínio | Não bloqueia comparação E2; regra final antes de E4 | Jogo: preservar regra por origem, incluindo domínio/HP; laboratório pode comparar alternativas |
| D15 | Quest e mob mantêm comportamentos de teto distintos | Regras por origem, sem uniformização silenciosa | C03 abaixo/no/acima do teto e rollback | Não E2 comparativo; E4 | Jogo: manter recusa de quest e saturação de mob; não aprovar teto novo |
| D16 | Atualizar missão não dá segundo prêmio | Namespaces e ciclo separados de versão | C04/C09, mudança de definição sem nova entrega | Não E2; E4 para repetibilidade | Jogo: entrega única atual; nenhuma missão repetível criada; futuro ciclo exige outro aceite |
| D17 | Pode coletar um tipo mesmo se outro não couber | Claim referencia cada origem individual consumida | C12, espaço parcial e múltiplas origens | Não E2 comparativo; E4 | Jogo: coleta por item inteiro cabível, sem fração arbitrária; laboratório compara pacote sem adotá-lo |
| D18 | Evita poção cobrada sem cura/equipamento perdido | Agregado semântico completo e reserva de campos | C03; E4 barreiras/HP/cooldown | E2 precisa de fixture completa; E4 precisa de semântica/reserva aprovadas | Ensaiar agregados completos; aprovar separadamente política de publicação/reserva do jogo |
| D19 | Participantes não perdem dívida após pagar só um | Manifesto e conciliação por beneficiário | C16; F23 crash E3 | Não E2 comparativo; E3 distribuição/E4 | Candidata: manifesto + commits individuais; não atomicidade global obrigatória nem produtor de morte já pronto |
| D20 | Offline pode reduzir restante, sem cura automática | Policy version e âncora coerente | C14 só representação; F14/F25 E3/E4 | Não E2; E4 recuperação | Jogo: escolher transcorrer/congelar/por canal depois das provas D21; proposta atual não é aceite |
| D21 | Não libera recarga cedo por precisão/clock inválidos | Codec/origem/margem e mapper versionados | C14 e F14/F25 com restart E3 | Não E2 limitado; E3 temporal/E4 | Autorizar apenas representação/recusa E2; homologação temporal exige evidência, sem fórmula aceita agora |
| D22 | Login não limpa penalidades nem cria dano surpresa | Fonte completa de descritores/policy por efeito | Quadro por efeito; F24 | Não E2 sintético; E4 | Seis escolhas independentes abaixo; sem aprovar dano offline implicitamente |
| D23 | Não repete impactos/eventos; mantém custos | Snapshot exclui execução, não obrigação | Validação R1; F22/F24 E4 | Restrição E2–produção | Nenhum novo aceite; preservar exclusões existentes |
| D24 | Ação pode aguardar/ser recusada sob pressão | Fila/worker/callback limitados; sem I/O no tick | F07/F18/F27 E4 | Não bloqueia E2 sem owner; E4 | Só contrato candidato; reserva/projeção de gameplay e orçamento real exigem aceite específico |
| D25 | Pressão não pode gerar falso sucesso | Quotas/timeouts/retry/encerramento finitos | C15 e medição de quotas | Números experimentais precisam de L03 antes de E2; reais E3/E4 | L03: somente tabela experimental; sem copiar limites ao owner, lease ou produção |
| D26 | Retry antigo não duplica nem perde resultado | Arquivo consultável + índices preservados | C07/C10, restart E3 | E2 requer perfil sintético; E3 retenção/restart; produção política final | Ensaiar cap 32/arquivo/recusa; não autorizar TTL, purge ou retenção real |
| D27 | Progresso experimental exige auditoria/contestação | Abertura nova é marco, não histórico fabricado | C09 só A; auditoria futura E5 | Não E2; migração E5/produção | Nenhuma B/C agora; eventual C exige autorização de dados independente |
| D28 | Não prometer recuperação ainda inexistente | Evidência por domínio de falha | C01–C16; F05/F06/F30 E3 | E2 produz prova parcial; bloqueia fechar E3/E4 sem ela | Não há aceite que substitua teste; autorizar E2 não declara durabilidade entre processos |
| D29 | Sem corpo inválido/parcial ou duplicado | Posse/tempo/efeitos/conteúdo/spawn revalidados | F12/F13/F24 E4 | Não E2; E4 | Manter gate fechado; recuperação real depende de tarefa/evidência próprias |
| D30 | Quatro relâmpagos não muda | Sem timeout/desconexão automática | Hash de fontes; futuro F22 separado | Restrição geral | Nenhum novo aceite e nenhuma ativação A-04/TASK-226 |
| D31 | ID conhecido não expõe dados de outro dono | Runner restrito, bootstrap separado, autorização atual | C11, roles/permissões E2; F21 E3/E4 | E2 requer isolamento sintético; E4 segurança real | L02/L03 para roles/segredos/negação sintéticos, sem aprovar autenticação real ou acesso direto do cliente |
| D32 | Clientes atuais mantêm compatibilidade | Sem handshake/mensagem nova em E2 | Futuro teste de versão/capacidade E4 | Não E2 nem storage E3; E4 transporte | Adiar proposta de protocolo; nenhum novo número/handshake autorizado |
| D33 | Restore não pode ressuscitar prêmio/ator antigo | Revogação global/era além de contador restaurado | F20/F28 E3 | Não E2 sem restore; bloqueia restore seguro E3/E4 | Manter bloqueado; autorização futura de falha não equivale a prova aprovada |
| D34 | Prêmio devido antigo não desaparece | Pendências explícitas, prefixo/lacunas e dedup | C13/F26 e crash E3 | Não E2 comparativo; E3 settlement/E4 | Ensaiar três alternativas; escolher política definitiva somente após evidência e revisão |

## Identidade: o mínimo que E2 pode provar

AccountId representa proprietário, CharacterId o personagem estável; SessionId
uma admissão, AdmissionGeneration sua geração e AuthorityFence o direito de
escrever. WorldEpoch separa execuções/origens. StateRevision ordena estado e
EconomicRevision ordena mutações econômicas, avançando conjuntamente quando
aplicável. OperationId identifica uma intenção, não uma sessão nem autorização.
Peer.id continua temporário e fica fora da identidade persistente proposta.

No E2 o controlador emite IDs e muda contexto de forma confiável. Dois contextos
sintéticos permitem testar negação cruzada, unicidade e escritor obsoleto.
Isso não demonstra senha/token, posse real, revogação de credenciais, lease,
segurança do transporte ou exclusividade do corpo. D05/D07/D32 permanecem
pendentes, D33 bloqueada; nenhum é promovido por esse ensaio limitado.

## D18 — Escopos completos a revisar separadamente

| Comando | Campos/efeitos inseparáveis na proposta | Aceite delimitado e prova |
| --- | --- | --- |
| Poção | Remoção de item, HP autorizado/efetivo e deadline no relógio correto | Não é Consume R5 isolado; C03 sintético e E4 com política de reserva/publicação e D21 |
| Equipamento | Bolsa, item anterior, slot equipado e atributos derivados coerentes | Sem UUID de exemplar novo; C03 exige conservação e nenhuma transferência parcial |
| Recompensa | XP nominal/efetiva, domínio e HP afetados, moedas, itens/direitos e entrega aplicáveis | Registrar política por origem; C03/C04 sem flags usadas como comprovantes retroativos |

Aceitar um agregado não escolhe automaticamente congelamento de combate,
projeção otimista ou compensação de ações em voo. Essas escolhas de E4 continuam
separadas de L01/L03.

## D22 — Uma decisão por penalidade

Todas as seis linhas são **PENDENTE DE APROVAÇÃO**, sem alteração de D22. A
recomendação de cada linha está **NÃO APROVADA**. E2 pode armazenar descritores
sintéticos; somente E4, com fonte completa e D21, pode recuperar jogador.

| Efeito | Regra/evidência atual | Alternativas | Recomendação candidata | Impacto jogador/backend e teste | Aceite específico necessário |
| --- | --- | --- | --- | --- | --- |
| ROOT | Timer local conhecido; exportação completa de todas as fontes não comprovada | Congelar restante; transcorrer por âncora homologada | Transcorrer só com tempo confiável; reter se desconhecido | Não limpar no login; guardar origem/duração; testar imediato/parcial/expirado/clock inválido | Escolher tratamento offline de ROOT e contingência, sem reset automático |
| STUN | Timer local; ações bloqueadas exigem semântica própria | Congelar; transcorrer; bloqueio de ativação quando incerto | Transcorrer só após prova, com bloqueio correto de ações | Evitar login contornar ação bloqueada; F24 + captura de todas as fontes | Decidir STUN separadamente de ROOT |
| SLOW | Timer e escala locais | Congelar escala/restante; transcorrer; reter | Conservar intensidade/versão e transcorrer só se comprovado | Não normalizar velocidade no login; testar escala, stacking desconhecido e versões incompatíveis | Aprovar duração/intensidade e política de múltiplas fontes, não só um timestamp |
| BLEED | Dot de Enemy não comprova ledger Player | Congelar descritor/restante; dano offline explícito; reter sem ativar | Para futura primeira integração, suspender avanço offline do descritor e exigir política de retomada sem burst; se fonte incompleta, reter | Não curar nem aplicar pulsos atrasados; testar duração/cadência/custo de desconexão após decisão | Escolher duração e dano separadamente; nenhum dano offline ou pulso no login aprovado |
| BURN | Mesmo limite de evidência; não presumir equivalência a BLEED | Congelar; transcorrer com regra de dano; retenção | Mesma candidata conservadora, com aceite independente | Guardar intensidade/cadência/origem; evitar cleanse/burst; F24 próprio | Decidir BURN e efeitos de morte offline, sem herdar aceite de BLEED |
| Desconhecido | Ausência de informação não é ausência do efeito | Retenção; adaptador versionado; omissão (inaceitável para integral) | Reter e bloquear ativação integral até avaliação completa | Pode adiar login; backend distingue UNKNOWN de vazio; teste campo/tipo novo e fonte ausente | Aprovar contingência/atendimento; novo tipo não ganha política por omissão |

Congelar descritor não define como o executor volta a produzir efeitos no jogo.
Não restaurar executor antigo e não liquidar pulsos atrasados continuam restrições;
cadência futura, morte, disponibilidade e abuso de desconexão exigem gameplay.

## D34 — Comparação de settlement e watermark

| Alternativa | Benefício | Custo/risco | Aceite e teste propostos |
| --- | --- | --- | --- |
| Ordenar settlement por fonte | Não ultrapassa pendência conhecida; implementação inicial simples | Uma intenção antiga bloqueia as novas; disponibilidade menor; origem precisa enumerar obrigações | C13 deve segurar a nova até resolver a antiga; medir retenção, sem concluir que geração ausente nunca devia prêmio |
| Prefixo efetivamente liquidado | Marca compacta só avança quando todo trecho conhecido está resolvido | Precisa prova de completude; máximo de geração não é prefixo | C13 paga nova primeiro sem avançar além da lacuna; depois paga antiga e avança uma vez |
| Registro de lacunas/pendentes | Permite ordem de conclusão diferente mantendo dívida identificável | Crescimento/consulta/retomada mais complexos; não pode apagar lacuna por TTL | C13 exige origem, intenção e dedup por grant; geração antiga sem prova continua recusada |

**RECOMENDADA, NÃO APROVADA:** comparar os três perfis em E2; candidata combina
obrigações explícitas e prefixo comprovado. Não considerar todo número faltante
de geração uma dívida, nem considerar ausência de registro prova de inexistência.
Após nova admissão, resolver resultado anterior; não trocar fence/conteúdo do
mesmo OperationId para contornar UNCERTAIN. Settlement novo, se autorizado,
preserva origem/GrantId e disputa a mesma deduplicação.

## Roteiro curto para o responsável

Primeiro revisar L01–L05 para um ensaio sintético, podendo adiar todas as escolhas
definitivas do jogo. Para E4, revisar separadamente D14/D15/D16/D17 e cada agregado
D18; depois D19, D20/D21, as seis políticas D22 e D34. D27 não autoriza migração
nessa revisão. D05/D07/D32 e D33 não estão fechadas. Não é necessário responder
ou aprovar todas as linhas para considerar o pacote documental R7 entregue.

Modelo de registro posterior, ainda não preenchido: **ID Dxx/Lxx; alternativa;
escopo laboratório/jogo; aceite, recusa ou ajuste; responsável; data;
limites/ressalvas; evidência exigida/produzida**. Um aceite de laboratório não
altera o estado de uma política de jogo nem remove bloqueio por evidência.
