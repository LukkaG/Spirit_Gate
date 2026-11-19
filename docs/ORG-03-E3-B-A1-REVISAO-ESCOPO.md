# ORG-03-E3-B-A1 — Revisão do escopo e proposta de ratificação

Revisão iniciada em 16/11 e concluída em 17/11/2026. Projeto Spirit Gate / Projeto-K. Revisão humana: Lucas.
**Proposta decisória; B-A1 não aceito definitivamente; B-A2–B-A6 não autorizados.**
Esta A1 autoriza somente os documentos e verificações estáticas de leitura.

## Proposta formal, ainda pendente

Propõe-se ratificar **E3-B — recuperação de dados e verificação de autoridade
após recuperação** como decomposição do gate E3 genérico de R6, preservando
integralmente E3-A-R3. A decomposição organiza trabalho futuro; não atribui uma
decisão nova ao Plano Diretor retrospectivamente e não fecha o gate E3.

Não foi encontrada especificação histórica detalhada anterior de E3-B. A busca
documentada na revisão anterior encontrou o nome como exclusão em documentos
E3-A. O pedido atual solicita uma proposta de ratificação, não a concede.
O [plano E3-B original](ORG-03-E3-B-PLANO.md) e os outros cinco documentos de
16/11 continuam intactos. Esta revisão A1 os complementa sem reescrever status.

Formulação submetida à decisão humana: “Ratificar a subdivisão E3-B do gate E3
para investigar backup/recuperação, completude econômica e validade de
autoridade, começando por inspeção de uma restauração isolada com escritores
econômicos bloqueados. Ratificar o nome não homologa D33 nem autoriza implementar
ou operar. Aceites posteriores devem identificar recorte, método, perfil,
cenários, caminhos, recursos e exclusões.” **Texto proposto, não aceite recebido.**

## Base de governança e natureza de cada conclusão

| Referência existente | Estado na fonte histórica | Aplicação nesta proposta |
| --- | --- | --- |
| [Plano Diretor Corujão VI](CORUJAO-VI-PLANO.md), objetivo 4 e segurança | Persistência online sintética isolada; dependências/ambiente exigem aceite | Laboratório não usa personagens reais nem instala infraestrutura |
| [Corujão IX](CORUJAO-IX-PLANO.md), limites e sequência | Preservar gameplay e 40 skills; tarefas futuras separadas | Não iniciar TASK-226 nem alterar combate/protocolo/sprites |
| [Gates R6](ORG-03-R6-GATES-DE-IMPLEMENTACAO.md), linha E3 | Durabilidade entre processos BLOQUEADA POR EVIDÊNCIA na R6; inclui restore/failover/retenção/revogação | Fonte do recorte E3-B; execução sintética posterior não muda retroativamente essa matriz |
| [Matriz R6](ORG-03-R6-MATRIZ-DECISOES.md), D03 | APROVADA ANTERIORMENTE: não fabricar recibos nem repetir prêmio/RNG | Restrição mantida em todas as opções |
| Mesma matriz, D06/D09/D10 | RECOMENDADAS: claim/fence, head/revisões, efeito+recibo+dedup transacionais | Invariantes candidatas a verificar no modelo experimental, sem aprovação produtiva por associação |
| Mesma matriz, D07/D12 | PENDENTES DE APROVAÇÃO: lease/suspensão e versão/topologia/RPO/RTO/WAL | Nenhum lease, SLA ou perfil final escolhido aqui |
| Mesma matriz, D19/D26 | RECOMENDADAS: manifestação durável de beneficiários, histórico/dedup consultáveis | Cobertura de manifestos/arquivo faz parte do conjunto recuperável; política final continua pendente |
| Mesma matriz, D33; [ADR R6](ORG-03-R6-ADR-DURABILIDADE.md), proteção contra rollback | BLOQUEADA POR EVIDÊNCIA: não reviver autoridade antiga | D33 independente; escritor restaurado não é admitido só porque o banco abriu |
| [Falhas R6](ORG-03-R6-MATRIZ-FALHAS.md), F09/F16/F20/F23/F28/F30 | Matriz de provas requeridas | Histórico, dívidas, processo antigo, restore e perfil de confirmação têm critérios separados |
| [Quadro R7](ORG-03-R7-QUADRO-APROVACAO-E1.md), D07/D12/D19/D26/D33 e limites E2 | Aceite sintético não aprova autoridade real/retirada do ator | Preservar fronteira laboratório/jogo e decisão/evidência |
| [Pendências E2](ORG-03-E2-PENDENCIAS-E3.md) | E3 ainda requer restauração/revogação/perfil | A E3-A cobriu parte; a E3-B propõe enfrentar o restante sem apagá-lo |

Classificações usadas nos sete documentos A1:

- **Existente:** regra/status ou evidência local citada; não nova autorização.
- **Proposta:** alternativa ou critério submetido a Lucas.
- **Hipótese:** pressuposto ainda não medido, inclusive tamanho e tempo de backup.
- **Bloqueio:** falta de decisão, prova ou autorização impede a ação dependente.
- **Autorização necessária:** ordem futura específica; silêncio não é aceite.

## Divisão do trabalho e fronteiras

| Domínio | Conteúdo | Estado e limite |
| --- | --- | --- |
| E3-A-R3 encerrada | C01–C09, dois crash-recoveries, quatro reinícios, perda de resposta, manifestos/dívida/arquivo, CAS/fence antigo/menor privilégio, integridade e descarte | Aprovada no escopo sintético; auditoria de 34 verificações preservada; não repetir ou reutilizar o run-id |
| E3-B proposta, inspeção | Backup consistente S0, confirmação posterior S1, recuperação R; inventário do conjunto, preservação do corte, lacunas, histórico/manifestos e quarentena | Nenhum caso executado; pode ser preparado para futura rodada sem escritores econômicos no destino |
| E3-B proposta, autoridade | Revogação resistente a rollback; processo antigo vivo; duas autoridades, retries/escritas pós-restore | B06/B08 positivos e demais mutações dependentes ficam bloqueados por D33/decisões próprias |
| Outra decisão arquitetural | Método de backup, verdade durável independente, topologia, eleição, quorum, lease, era, réplica e política de retomada | Comparados, não selecionados automaticamente; RPO/RTO produtivos fora deste aceite |
| E4 | Identidade/posse reais, retirada do ator, worker/owner/callback, integração de comandos, mundo e combate | Sem edição/ativação; recusa SQL não prova remoção de ator |
| E5 | Homologação física/ambiente real, implantação, dados legados/migração | Exige E4 e aceite de dados/ambiente; sem saves/contas reais |

## Objetivo verificável do primeiro recorte proposto

Inspecionar uma recuperação isolada e responder quatro perguntas distintas:
“detectou a lacuna?”, “preservou as confirmações exigidas?”, “conseguiu
reconciliar a economia?” e “existe autoridade válida para escrever?”. A primeira
resposta positiva não implica as outras. A proposta inicial exige que o destino
permaneça sem admissão e sem escritores econômicos, inclusive quando os dados
forem íntegros. Escritas internas de startup do PostgreSQL não são retomada da
economia nem prova de autoridade.

O resultado desse primeiro recorte poderá aprovar subcritérios de inspeção,
nunca a E3-B completa com casos obrigatórios bloqueados. Mantêm-se B01–B10 e
R01–R14. Não serão retirados depois de falha para facilitar o fechamento.

## Pacote para decisão

1. [Comparativo de backup](ORG-03-E3-B-A1-COMPARATIVO-BACKUP.md): recomendação técnica B, sujeita a aceite e viabilidade.
2. [Contrato S0/S1/R e perfis D12/D26](ORG-03-E3-B-A1-CONTRATO-S0-S1-R.md): cobertura e quatro resultados independentes.
3. [Alternativas D33](ORG-03-E3-B-A1-ALTERNATIVAS-D33.md): nenhuma eleição/era/lease criada.
4. [Fases, cenários e riscos](ORG-03-E3-B-A1-FASES-E-CENARIOS.md): primeira rodada de inspeção e partes bloqueadas.
5. [Quadro decisório](ORG-03-E3-B-A1-DECISOES-PENDENTES.md) e [relatório](ORG-03-E3-B-A1-RELATORIO-FINAL.md).

Nenhum dos sete documentos implementa runtime, SQL, schema, backup/restore,
eleição, failover ou integração. Os aceites B-A1–B-A6 continuam distintos.
