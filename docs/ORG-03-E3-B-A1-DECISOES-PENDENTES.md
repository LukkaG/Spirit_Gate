# ORG-03-E3-B-A1 — Quadro de decisões humanas pendentes

Revisão iniciada em 16/11 e concluída em 17/11/2026. Responsável para revisão: Lucas; infraestrutura/arquitetura participa
das escolhas pertinentes. **Nenhuma opção abaixo está aceita.** A ordem atual
autoriza preparar esta proposta, não é aceite definitivo B-A1 nem B-A2–B-A6.

## O que já existe, o que se propõe e o que permanece hipótese

| Natureza | Conteúdo |
| --- | --- |
| Decisão existente na governança | Preservação dos sistemas/dados e D03: não fabricar histórico/prêmio/RNG; gates, status e dependências R6/R7 mantidos |
| Evidência existente | E3-A-R3 C01–C09 no escopo sintético, auditoria documental de 34 verificações; não prova D33/restore/failover |
| Decisão proposta | Ratificação E3-B, método B, perfil P0, contrato de cobertura, fases e contenção Q para inspeção |
| Hipótese experimental | Viabilidade de 1 GiB/120 s, tamanhos do comparativo, completude capturável pelo oráculo e dependências da fixture futura |
| Bloqueio | Mecanismo D33 não aprovado; nenhum escritor pós-restore, B06/B08 positivos ou retomada econômica permitidos |
| Autorização ainda necessária | Aceite B-A1 expresso, B-A2 de implementação e B-A3/B-A4/B-A6 operacionais; B-A5 só com decisão/prova de autoridade adequadas |

Os status RECOMENDADA/PENDENTE/BLOQUEADA de D06/D07/D09/D10/D12/D19/D26/D33
continuam como nas [fontes R6](ORG-03-R6-MATRIZ-DECISOES.md) e
[R7](ORG-03-R7-QUADRO-APROVACAO-E1.md). Aceitar um contrato experimental não
aprova automaticamente sua versão de produção.

## Decisões para consolidar o futuro B-A1

| ID / questão | Opções, vantagens e riscos | Recomendação técnica proposta | Consequência da escolha / bloqueio |
| --- | --- | --- | --- |
| A1-D01 — nome/recorte | Ratificar E3-B como decomposição de E3; ou pedir novo recorte antes de implementar. Ratificação melhora rastreabilidade; risco é confundir nome com aceite operacional | Ratificar o nome e a fronteira E3-A/E3-B/E4/E5 descrita na revisão de escopo | Sem ratificação, não tratar esta subdivisão como requisito histórico aprovado; não muda resultados E3-A |
| A1-D02 — método da primeira rodada | A lógico: portátil e objetos examináveis, mas globais/cortes complementares; B físico parado: corte simples/cluster completo, mas indisponibilidade e três cópias; C base+WAL: alvo posterior, mas cadeia/configuração/espaço mais complexos | B físico com parada consistente, após dimensionamento | Escolher A/C requer ajustar evidência, scripts novos e orçamento antes do aceite; sem fallback automático |
| A1-D03 — perfil D12 | P0 preserva corte S0 e detecta perdas S1; P1 tolera janela/quantidade escolhidas; P2 exige todas as confirmações até S1. Perfil mais forte exige material e prova compatíveis | P0 para inspeção inicial; nenhuma garantia até S1 ou de produção | Se escolher P2 com apenas S0 antigo, o desenho é insuficiente e deve mudar antes de operar; perda exigida reprova PRES mesmo com DET correto |
| A1-D04 — tempo/RTO | Separar tempo até dados inspecionados e tempo até economia admitida; meta numérica de inspeção dentro de 120 s/caso, ou pedir revisão justificada da quota antes de aceitar | Objetivo experimental de até 120 s para restore+inspeção, ainda não medido; economia sem RTO aceito enquanto D33 pendente | Estouro reprova limite do caso; não prometer SLA produtivo nem usar abertura do banco como t1 econômico |
| A1-D05 — oráculo e cobertura | Registro enumerado independente do rollback; captura incompleta explicitamente incerta; outro desenho de evidência deve demonstrar cobertura equivalente. Só lista de ACKs é insuficiente para negativos | Aprovar campos/cobertura do contrato S0/S1/R e selagem externa ao conjunto restaurável | Sem origem/período/IDs/limites conhecidos, não é possível negar confirmação nem validar integralmente DET/PRES |
| A1-D06 — D26/conjunto recuperável | DB+globais+archive/índice+manifestos no corte; ou recorte menor declarado, que deixa subcritérios pendentes. Recorte menor simplifica mas não pode ser apresentado como recuperação completa | Incluir todas as partes da fixture econômica e metadados necessários; não usar dados reais | Ausência ou corte incompatível mantém UNCERTAIN/bloqueio; nenhuma regeneração automática |
| A1-D07 — intenções/dívidas | Distinguir preparadas, confirmadas, incertas, arquivo indisponível e autoridade inválida; não gerar obrigação de manifesto ausente | Aprovar apenas classificação/inspeção na primeira rodada, preservando intenção/payload/resultados originais | Settlement/retry exige fase posterior com AUTH e autorização; não equivale a aprovar política D19/D34 de produção |
| A1-D08 — D33/arquitetura de autoridade | Q quarentena; M exclusão manual comprovada; E registro externo; C consenso; H HA com fencing. M reduz infraestrutura mas exige cobertura operacional; E/C/H ampliam dependências/falhas a provar | Q só para conter inspeção. Manter mecanismo definitivo pendente até definir topologia e necessidade de múltiplas instâncias | Q não fecha D33. Escolher M/E/C/H exige desenho e prova próprios; sem eles B06/B08 positivos e escrita pós-restore ficam bloqueados |
| A1-D09 — cenários/fases | Manter todos B01–B10 com subcritérios por F2/F3/F4; ou revisar desenho antes de aceitar. Uma rodada pequena é revisável, mas não pode esconder casos bloqueados | Aceitar F2 somente como recorte de inspeção; preservar todas as pendências dos IDs originais | Relatório F2 não pode aprovar E3-B inteira; retirar cenário obrigatório após falha exige decisão explícita, nunca reclassificação silenciosa |
| A1-D10 — recursos/viabilidade | Usar ficha candidata anterior após medir/estimar pior caso; reduzir recorte ou pedir novos limites antes da operação se não couber | Manter candidatos sem reserva: dois clusters, um backup/restore, zero crashes/failovers; confirmar 1 GiB/64 MiB logs e 4 GiB livres | Tamanhos do comparativo são hipótese. Nenhuma ampliação, exclusão de evidências ou liberação de porta automática |
| A1-D11 — fronteira de inspeção | Origem escreve fixture S0/S1 sob autoridade sintética aprovada; alvo só leitura por leitor restrito, sem workers econômicos; alternativa de retomada só após D33 | Aprovar origem parada antes de abrir alvo e separar administrador de recuperação de leitor econômico | Evita efeitos no recorte; não substitui antigo-processo-vivo e fencing positivos. Configuração read-only isolada não basta |

Fontes detalhadas: [escopo](ORG-03-E3-B-A1-REVISAO-ESCOPO.md),
[backup](ORG-03-E3-B-A1-COMPARATIVO-BACKUP.md),
[contrato/perfis](ORG-03-E3-B-A1-CONTRATO-S0-S1-R.md),
[D33](ORG-03-E3-B-A1-ALTERNATIVAS-D33.md) e
[fases/cenários/riscos](ORG-03-E3-B-A1-FASES-E-CENARIOS.md).

## Registros exigidos antes de uma implementação B-A2

Registrar responsável/data e escolhas aceitas/rejeitadas por A1-Dxx, escopo
sintético, casos/subcritérios obrigatórios e bloqueados, versão dos documentos e
limitações. Para B-A2 restrito à inspeção, o mecanismo positivo D33 pode continuar
pendente **se o aceite disser expressamente que não será implementado/admitido
escritor pós-restore e que positivos de autoridade continuam bloqueados**.

Mesmo nesse recorte, antes de implementar definir método, contrato, cobertura,
oráculos, bloqueio de escritores, guardas e critérios de F1. B-A2 deve autorizar
código novo isolado e testes estáticos/simulados, sem importar/rodar scripts
operacionais E3-A e sem processos de banco/worker/relay. A entrega A1 contém
somente documentos e evidências estáticas de preservação; não implementa F1.

## Ficha operacional que ainda não existe

Os itens são propostas herdadas do desenho anterior, não escolhas aceitas nem
recursos reservados. A futura autorização deve tornar todos os campos exatos.

| Campo | Candidato / pendência |
| --- | --- |
| Responsável/data | Lucas / data real da execução futura, a registrar |
| Run-id | `<AAAAMMDD>-lucas-e3b-<nonce-novo>`; nenhum criado; nunca usar o run-id consumido E3-A |
| Caminho | `C:/Users/lucas/Documents/GitHub/Projeto-K/temp/org03-e3b/<run-id-novo>`; sem diretório operacional criado nesta A1 |
| Raízes próprias | source-data, restored-data, backup, history, evidence e secrets; oráculo fora do conjunto restaurável |
| Portas | Candidatas loopback :55437/:55438; não sondadas. :55439 somente se relay for necessário e explicitamente autorizado; recomendação F2 sem relay |
| Recursos | Até dois clusters, um backup/restore, quatro starts totais/dois reinícios limpos como máximos candidatos; recomendação B usa três starts/um reinício; zero crash/failover/promotion |
| Cargas | Oito personagens/dois owners, 64 intenções, três workers simultâneos/32 totais, seis conexões agregadas, até três tentativas/intenção e 16 KiB/payload; valores não aceitos |
| Tempo/disco | 120 s/caso, 20 min totais; 1 GiB incluindo tudo, até 64 MiB logs/evidências; 4 GiB livres antes de iniciar; viabilidade pendente |
| Binários/configuração | Somente existentes; versão/caminho/hash a fixar. A versão E3-A 18.6 não é escolha automática para E3-B nem ordem de atualizar |
| Encerramento | Handles/identidades originais, PostgreSQL próprio fast, ausência antes de descarte de raízes canônicas aprovadas; nenhuma ação sobre :5432 ou processo desconhecido |

Portas 5432/55433–55436 e todos os run-ids/scripts/aceites operacionais E3-A
permanecem fora do novo ensaio. A1 não verifica disponibilidade, espaço ou
credenciais atuais e não autoriza instalações.

## Aceites posteriores: independentes e ainda ausentes

| Aceite do desenho anterior | O que ainda precisa ocorrer |
| --- | --- |
| B-A1 | Lucas revisar e aceitar explicitamente as escolhas do quadro, preservando o que permanece pendente |
| B-A2 | Nova ordem para implementar apenas o pacote isolado e testes permitidos, depois de definido seu recorte |
| B-A3 | Ficha operacional exata, binários, caminhos, endpoints, quotas e pré-condições de isolamento/disponibilidade |
| B-A4 | Autorizar expressamente cluster/fixture/SQL/backup/restore e processos/reinícios delimitados; nenhum crash/failover por associação |
| B-A5 | Aprovar mecanismo de autoridade e experimentos positivos/concorrentes próprios; não necessário para simplesmente manter escritor bloqueado, mas necessário para retomar escrita |
| B-A6 | Aprovar parada/descarte e contingência, raízes removíveis, evidências retidas e ação manual em caso de identidade/encerramento inconclusivos |

## Parecer de prontidão

| Marco | Estado preciso nesta A1 |
| --- | --- |
| B-A1 | **PRONTO PARA REVISÃO HUMANA**, com alternativas e recomendações; não aprovado definitivamente |
| B-A2 | **NÃO AUTORIZADO / NÃO INICIADO**; aguarda decisões suficientes de método/contrato/recorte e ordem expressa; pode ser limitado a inspeção sem resolver D33 positivo |
| Primeira rodada operacional | **NÃO PRONTA / NÃO AUTORIZADA**; falta F1 implementada/revisada, perfil/método aceitos, ficha/viabilidade e B-A3/B-A4/B-A6 |
| B06/B08 positivos e economia pós-restore | **BLOQUEADOS** por mecanismo D33 não aprovado/provado, além das autorizações operacionais |

Não preencher “aprovado”, data de aceite ou assinatura em nome de Lucas.
Nenhuma resposta ou aprovação é presumida pelo tempo decorrido.
