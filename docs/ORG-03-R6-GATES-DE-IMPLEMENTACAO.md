# ORG-03-R6 — Gates de implementação

## Estado atual

**APROVADA ANTERIORMENTE — D01/D02/D03/D13/D23/D30:** R6 é documentação, com
preservação de código/dados e gates anteriores fechados. Concluir a R6 significa
entregar especificação revisável; não significa aprovar suas recomendações ou
autorizar a etapa seguinte. Não foi iniciada ORG-04, TASK-226 ou A-04.

## Etapas de evidência, sem atribuição de novas TASKs

| Etapa | Estado | Entrada exigida | Entrega/evidência para fechar | Exclusões e aprovação |
| --- | --- | --- | --- | --- |
| E1 — Revisão das decisões | PENDENTE DE APROVAÇÃO | Matriz D01–D34 e diferenças P01–P12 | Aceite explícito, escopo, responsável, data e alternativas rejeitadas; números/semântica ainda abertos identificados | Nenhuma escolha do agente vira aceite |
| E2 — Laboratório PostgreSQL descartável | PENDENTE DE APROVAÇÃO | Autorização específica de ambiente/dependências; contrato de transação e fixture sintética aprovados para ensaio | Constraints, CAS/fence, dedup/reconciliação, isolamento e F01–F03/F09–F11/F15–F17/F25–F26/F29 | Não precisa de contas reais para ensaiar storage; não muda runtime, saves ou produção |
| E3 — Durabilidade entre processos | BLOQUEADA POR EVIDÊNCIA | E2 e perfil de falha/topologia explícito; autorização para processos descartáveis | Crash real antes/depois, resposta perdida, failover/restore, retenção e revogação de autoridade; F04–F08/F16/F18–F21/F23/F28/F30 | RAM/novo Coordinator não fecham este gate |
| E4 — Integração owner/ativação controlada | BLOQUEADA POR EVIDÊNCIA | Autoridade/posse e políticas aprovadas; E2/E3 pertinentes; nova ordem de edição do runtime | Fila limitada e worker fora do tick; callbacks revalidados, barreiras de estado, transação de comandos completos, conteúdo/spawn e efeitos; F07/F11–F14/F18–F25/F27 | Sem publicar benefício ou ator duplicado; protocolo/capacidade futura exige decisão D32 |
| E5 — Homologação e eventual legado | PENDENTE DE APROVAÇÃO | E4, plano de implantação/backup/reversão e decisão formal sobre A/B/C | Testes físicos/ambiente real, atendimento de divergências, auditoria de abertura se C aprovada, restore verificado | Nenhum save pessoal é usado antes de autorização própria; release/publicação continuam separadas |

E2 pode comparar alternativas de política com dados sintéticos sem aprová-las
para jogadores. Aprovação de um laboratório não instala dependências, serviço,
driver ou banco automaticamente: o escopo autorizado precisa dizer o que pode
ser criado. E3/E4 exigem somente as partes de E2 necessárias ao recorte, sem
confundir um protótipo aprovado com persistência pronta.

## Checklist antes de qualquer escrita de produção

**RECOMENDADA — D04/D06/D09/D10/D18/D24/D26/D31.** Exigir:

- Emissão de identidade e prova de propriedade separadas do snapshot; claim
  exclusivo, revogação e proteção contra ator antigo, inclusive após restore.
- Fonte canônica única de XP/saldo/itens; StateRevision/EconomicRevision,
  geração/fence e origem de cada concessão sem ambiguidade.
- Operação imutável e resultado resolvido; transação conjunta, unicidades,
  lookup autorizado e regra para UNCERTAIN. Sem last-write-wins ou RNG de recuperação.
- Política aprovada para XP/coins/quests/coleta/consumo/equipamento/poção e
  distribuição a vários beneficiários; sem importar diferenças da R5 por padrão.
- Relógio, precisão, origem/época e efeitos negativos aprovados; ausência de
  dados não vira pronto/sem penalidade. Custos persistidos sem reviver execução.
- Fila/retorno com limites de itens/bytes/tempo, uma estratégia para campos em
  voo, desligamento e callback antigo. Sem I/O ou espera de banco no owner.
- Retenção consultável e dedup após restart; journal/manifesto para o ponto de
  aceitação escolhido. Não prometer salvar o que só existe em RAM.
- Conteúdo/regras, reserva física e instante de ativação revalidados. A-04
  continua exigindo ordem própria, sem timeout/desconexão automáticos.

Enquanto houver pré-condição necessária sem prova/aceite, o recorte dependente
permanece bloqueado. Isso não impede revisão documental nem ensaio sintético
isolado autorizado; impede anunciar recuperação/durabilidade inexistentes.

## Limites operacionais que ainda precisam de valores

**PENDENTE DE APROVAÇÃO — D07/D12/D25/D26.** Antes do ensaio correspondente,
definir orçamento de fila em itens/bytes, tamanho do comando/payload, concorrência
de workers, tempo de lock/transação, prazo e quantidade de retry, backoff,
reserva de callbacks, duração/renovação de lease, margem temporal, retenção,
RPO/RTO e prazo de shutdown. Não copiar os limites 64/16/256 de outros componentes
como se fossem uma escolha de capacidade para a nova persistência.

## Roteiro de revisão pelo responsável

Começar por D14/D15/D17/D18/D19/D20/D22/D27/D34, que afetam o jogador ou o legado;
depois fechar autoridade, cabeça/revisões, perfil de falha e orçamento. Registrar
cada aceite na matriz com seu alcance, sem alterar o status dos demais por
associação. Essa revisão é o próximo passo sugerido; não foi executada em nome
do responsável. [Matriz de falhas](ORG-03-R6-MATRIZ-FALHAS.md).
