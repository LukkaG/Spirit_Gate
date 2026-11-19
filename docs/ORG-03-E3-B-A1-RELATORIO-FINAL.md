# ORG-03-E3-B-A1 — Relatório final da revisão de arquitetura

Revisão iniciada em 16/11 e concluída em 17/11/2026. Spirit Gate / Projeto-K. Responsável pela revisão humana: Lucas.
Branch `beco`, HEAD `c3d484e76f17c2d93bf0c68ad3ff54280ee8ad39`.
**Revisão documental concluída para decisão; B-A1 não aceito definitivamente;
nenhuma implementação ou execução operacional E3-B.**

## Resultado preparado

A proposta recomenda, para uma primeira rodada futura, backup físico com parada
consistente, perfil P0 de preservação do corte S0 e alvo recuperado apenas para
inspeção, sem escritores econômicos. São recomendações técnicas pendentes de
Lucas, não políticas adotadas. A confirmação adicional S1 ausente de R deve
ser detectada e relatada; essa detecção não aprova a recuperação da confirmação.

Foram separados DET (detecção), PRES (preservação), REC (reconciliação) e AUTH
(autoridade), com cobertura explícita do oráculo fora do rollback. A retomada
econômica exige prova de autoridade independente dos dados restaurados. D33
permanece sem mecanismo aprovado; nenhum algoritmo de eleição, quorum, lease,
era global, serviço externo ou promoção foi escolhido/implementado.

## Sete documentos entregues

| Documento | Conteúdo e diferença perante o desenho anterior |
| --- | --- |
| [A1-REVISAO-ESCOPO](ORG-03-E3-B-A1-REVISAO-ESCOPO.md) | Texto proposto de ratificação do nome E3-B, lacuna histórica explícita, status/fontes R6/R7 e fronteiras E3-A/E3-B/E4/E5 |
| [A1-COMPARATIVO-BACKUP](ORG-03-E3-B-A1-COMPARATIVO-BACKUP.md) | A lógico/B físico parado/C base+WAL, objetos/globais/arquivos, garantias/limites, evidências e dimensionamento condicional; recomendação B |
| [A1-CONTRATO-S0-S1-R](ORG-03-E3-B-A1-CONTRATO-S0-S1-R.md) | Fronteiras, cobertura do oráculo, seis situações por operação, quatro eixos, três perfis candidatos D12 e cobertura D26 |
| [A1-ALTERNATIVAS-D33](ORG-03-E3-B-A1-ALTERNATIVAS-D33.md) | Quarentena, recuperação manual exclusiva, registro externo, consenso e HA; estado/rollback/processo vivo/partições/admissão e atalhos insuficientes |
| [A1-FASES-E-CENARIOS](ORG-03-E3-B-A1-FASES-E-CENARIOS.md) | Fases propostas; dependência/pré-condição/estímulo/evidência/oráculo/aceite/reprovação/limite/escrita/PG real de B01–B10; relações dos R01–R14 preservados |
| [A1-DECISOES-PENDENTES](ORG-03-E3-B-A1-DECISOES-PENDENTES.md) | Onze decisões A1-D01–D11 com opções, vantagens, riscos, recomendação e consequência; ficha candidata, autorizações e parecer de prontidão |
| Este relatório | Método, preservação, verificações estáticas, limitações e estado de entrega |

Os seis documentos anteriores não foram editados: [encerramento E3-A](ORG-03-E3-A-ENCERRAMENTO.md),
[plano](ORG-03-E3-B-PLANO.md), [riscos](ORG-03-E3-B-MATRIZ-RISCOS.md),
[critérios](ORG-03-E3-B-CRITERIOS-ACEITE.md),
[pendências](ORG-03-E3-B-PENDENCIAS.md) e
[relatório de preparação](ORG-03-E3-B-RELATORIO-PREPARACAO.md).
Esta A1 é aditiva; não substitui historicamente decisões nem autorizações.

## Fontes, método e preservação

Foram consultados AGENTS.md, a skill local
[spiritgate-server-engineer](../.agents/skills/spiritgate-server-engineer/SKILL.md),
monitor/logs de orientação, os seis documentos anteriores, o Plano Diretor
Corujão VI e os limites do Corujão IX, matriz/ADR/gates R6 e quadro R7.
Os pontos reais LocalSimulationServer, RpgSessionWorld, SyntheticOnlineRepository
e EconomicLaboratory em src/test foram lidos para limitar a comparação D33 à
arquitetura observada. Nenhuma dessas classes foi compilada, instanciada ou alterada.

Foram consultadas fontes oficiais PostgreSQL da série 18: pg_dump, pg_dumpall,
backup de arquivos, continuous archiving/PITR, pg_basebackup, pg_verifybackup
e failover. Links diretos sustentam as distinções nos comparativos. Essa consulta
é documental; não escolhe minor, instala dependência ou acessa banco. As
recomendações e estimativas são inferências de projeto, não resultados oficiais
de benchmark nem garantias publicadas para o Spirit Gate.

Antes de escrever os sete documentos foi calculado inventário SHA-256 de
**4.343 arquivos existentes**, união dos manifestos históricos, árvore E3-A-R3
inventariada, documentação e ferramentas anteriores incluídas na revisão. O
baseline fica em
[preservacao-antes.json](evidencias/ORG-03-E3-B-A1-20261116/preservacao-antes.json).
O sufixo 20261116 da pasta de evidências identifica o início da revisão,
não um run-id operacional. A conclusão ocorreu em 17/11/2026.
A verificação final e hashes da entrega ficam em
[validacao-final.json](evidencias/ORG-03-E3-B-A1-20261116/validacao-final.json).
É evidência desta revisão, sem run-id ou diretório operacional E3-B.

O fechamento E3-A-R3, os 34 resultados de auditoria e sua primeira passagem,
C01–C09, logs de dois crash-recoveries, controles SQL, manifestos, snapshots,
recibos e falhas/tentativas anteriores permanecem inalterados no conjunto
conferido. A verificação de preservação compara bytes; não reexecuta esses testes
nem os reclassifica. O run-id histórico continua consumido.

## Validação executada e seus limites

Executadas somente leituras Git/arquivos, consultas à documentação pública e
verificações estáticas em memória por Python/biblioteca padrão:

- Preservação antes/depois por SHA-256 dos 4.343 caminhos do baseline e dos
  manifestos R3 pertinentes, sem alteração de arquivos anteriores.
- Resolução de links locais dos sete novos documentos e referências aos
  documentos/fontes realmente existentes; leitura UTF-8 sem caractere de substituição.
- Cobertura documental dos IDs B01–B10, R01–R14, A1-D01–D11 e das decisões
  D03/D06/D07/D09/D10/D12/D19/D26/D33, com bloqueios e autorização separados.
- Ausência de novos arquivos de implementação/harness nesta entrega; comparação
  de branch/HEAD/diff rastreado antes/depois, sem operações Git mutáveis.

Os números/diagnósticos concretos da validação estão no JSON final. Não foram
executados Maven/Java, o auditor histórico que grava resultados, testes de
transporte, fixtures de worker/relay, PostgreSQL, initdb, SQL, backup, restore,
crash, failover, eleição ou sondagem de portas. Nenhum resultado simulado foi
usado para substituir ensaio operacional não realizado.

A [primeira passagem da validação A1](evidencias/ORG-03-E3-B-A1-20261116/validacao-primeira-passagem.json)
foi preservada: apontou o link ao próprio JSON antes de sua criação e falha no
comparador de campos por codificação do comando enviado ao Python. Foi corrigida
a passagem de texto do verificador e a redação nova afetada pela mesma conversão,
sem alterar arquivos anteriores. A passagem final confere novamente os links,
campos e a preservação; nenhum desses alertas representa resultado operacional.

Os 4.343 arquivos são o conjunto inventariado, não todos os arquivos ignorados
ou pessoais do computador. Os valores de armazenamento e RTO nos documentos
são hipóteses/metas a decidir; disponibilidade e capacidade atuais não foram
testadas. Não foi feita auditoria física de WAL ou de hardware.

## Riscos e bloqueios remanescentes

Backup íntegro pode estar temporalmente incompleto e conter autoridade antiga.
Faltam decisão do método/perfil/conjunto, cobertura operacional do oráculo,
dimensionamento e implementação isolada revisada. Histórico parcial bloqueia
reconciliação; não permite repetir prêmio/RNG ou fabricar recibo/manifesto.
Estado consistente ou fence local não prova revogação resistente ao rollback.

Todos B01–B10 e R01–R14 foram mantidos. A primeira rodada proposta tem objetivos
de inspeção integral em B01-dados, B02-negativo/P0 e B10 para seus recursos;
os outros recebem somente os subcritérios identificados ou ficam bloqueados.
Isso não fecha os IDs completos ou E3-B com partes obrigatórias pendentes.
Positivos B06/B08 e todas as mutações pós-restore dependentes de autoridade
continuam bloqueados por D33, além de necessitarem autorização operacional.

Nenhuma funcionalidade do servidor/cliente, Java/Maven, gameplay, economia
produtiva, combate, skills/sprites, saves, contas, credenciais ou produção foi
alterada. Nenhum acesso a :5432, porta operacional, script de execução ou aceite
E3-A foi reutilizado. Sem instalação, reset/clean, commit/push/publicação,
E4/E5, ORG-04, A-04 ou TASK-226.

## Parecer final de prontidão

**B-A1:** proposta pronta para revisão humana, com recomendações e escolhas
pendentes; não é aceite definitivo.

**B-A2:** não autorizado e não iniciado. Depois de aceitar as decisões suficientes
do recorte, Lucas poderá autorizar implementação isolada para inspeção, mantendo
explicitamente D33 positivo bloqueado. Nenhuma implementação foi antecipada.

**Primeira rodada operacional:** não pronta e não autorizada. Depende de pacote
implementado/revisado em nova ordem, método/perfil/contrato aceitos, run-id/data/
caminhos/portas/binários/quotas exatos, viabilidade e B-A3/B-A4/B-A6. Retomar
escrita econômica ou positivos de autoridade exige também B-A5, mecanismo aceito
e prova própria. Aguardar aprovação humana específica antes de avançar.
