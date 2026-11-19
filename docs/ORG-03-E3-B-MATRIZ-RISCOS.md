# ORG-03-E3-B — Matriz de riscos

16/11/2026. Proposta documental, sem execução. Responsável pela decisão: Lucas,
com infraestrutura nas escolhas D12. Prioridade é qualitativa: não foram
estimadas probabilidades por dados de produção. Todo risco econômico abaixo
é crítico; falhas de identidade/descarte são também bloqueios operacionais.

“Esperado” descreve critério proposto do ensaio, não capacidade já implementada
ou política econômica homologada. Se faltar decisão, o comportamento dependente
fica BLOQUEADO; não se registra PASSOU por simplesmente não executar a ação.

| ID / risco | Comportamento esperado proposto | Evidência necessária | Reprovação | Dependência humana / referência |
| --- | --- | --- | --- | --- |
| R01 — perda de confirmação antes de commit | Distinguir intenção preparada, commit não enviado e resultado desconhecido; não anunciar confirmação sem prova nem declarar rollback só por desconexão | Intenção original durável, fronteiras de despacho/ACK, resultado resolvido, identidade do worker, imagens antes/depois e consulta por OperationId com cobertura histórica conhecida | Callback/transport exit vira confirmação; timeout vira NOT_FOUND definitivo; intenção nova substitui a antiga | Aprovar ponto de aceitação e semântica de pendência; D10/D12/D25; B03 |
| R02 — operação confirmada antes da interrupção | Preservar integralmente recibo e efeitos no perfil prometido; se faltar em R, detectar perda e bloquear reaplicação | Confirmação externa ao backup, imagem confirmada, corte S0/S1, cadeia WAL quando aplicável, lookup no alvo e comparação completa de revisões/recursos | Confirmação anterior desaparece dentro do RPO prometido; recibo recriado, efeito duplicado ou perda ocultada por retorno NOT_FOUND | Fixar o que conta como confirmação e RPO/RTO; D12/F06/F30; B01/B02 |
| R03 — backup com estado antigo | Detectar corte/identidade antigos; não usar fence/contador restaurado como prova de atualidade | Manifesto de backup com identidade, versão, corte/LSN quando aplicável, hashes e referência de confirmações posterior independente | Backup antigo aceito como estado atual sem comparação; contador local incrementado ou UUID tratado como revogação global | Definir fonte de verdade e mecanismo contra rollback, sem escolhê-los automaticamente; D12/D33/F28; B02/B06 |
| R04 — recibo histórico ausente/indisponível | Resultado incerto/bloqueado; nenhum benefício novo pela falta de histórico; manter distinção entre indisponível, inválido e ausente com prova | Índice de dedup, localizador, digest, cobertura temporal e cópia externa; logs de indisponibilidade e zero mutações | Falta de arquivo interpretada como nunca aplicado; RNG repetido; GC abre replay | Aprovar retenção/arquivo e corte conjunto, D03/D26/F09/F16; B04 |
| R05 — reaplicação de intenção antiga | Mesma intenção imutável retorna histórico ou recusa segura; outra OperationId para a mesma origem não paga novamente | IDs, origem/GrantId e payload originais; recibos/grants/revisões antes/depois; testes idênticos e divergentes | Novo crédito, item, consumo, revisão econômica ou grant indevido; payload alterado aceito como retry | Aprovar contrato sintético de dedup e escopo de escrita recuperada; D03/D10/D26; B05 |
| R06 — escrita por autoridade obsoleta | Recusar autoridade velha no destino e impedir admissão se validade global não for demonstrável | Processo antigo mantido vivo sob controle, identidade/fence/geração de ambos, tentativa identificada e negativa de escrita, controle positivo da autoridade válida se aprovado | Escrita antiga aceita; matar antigo usado como única prova; teste só com contador local promovido a D33 | Aprovar revogação/era e dependência que sobreviva ao rollback; D07/D33/F20/F28; B06 |
| R07 — concorrência de duas autoridades | No máximo um escritor autorizado; nenhum duplo grant. Na falta de autoridade demonstrável, bloquear ambos os caminhos dependentes | Barreiras concorrentes reproduzíveis, identidades, mesmas origens, decisões de admissão, locks/CAS/recibos e imagens finais; controle positivo separado | Dois commits conflitantes, duas recompensas, resultado por ordem casual sem validade demonstrada; eleição inventada no harness | Aprovar mecanismo/topologia e política de liderança; D06/D07/D12/D33; B08 |
| R08 — recuperação parcial de manifesto | Reconstituir somente pendências comprovadas, preservar A já confirmado; arquivo incompleto não autoriza gerar dívida ou marcar tudo concluído | Manifesto canônico e digest, intenções por beneficiário, recibos locais/arquivo, lacunas de sequência, variante ausente/truncada e pós-reconciliação | Beneficiário pago duas vezes, omitido sem diagnóstico, prefixo salta lacuna; manifesto reconstruído por suposição ou RNG | Aprovar fronteira de aceitação do manifesto e recuperação da parte ausente; D19/D26; B07 |
| R09 — recibos/revisões/grants inconsistentes | Detectar órfãos, duplicações e head incompatível; bloquear efeitos até decisão, sem reparo automático | Todas as versões/heads/recibos/índices/grants, resultados resolvidos, origens, obrigações e diferenças por campo; corte comum ao arquivo e DB | Contagem agregada aceita apesar de bijeção quebrada; last-write-wins; sobrescrita do head com snapshot velho; patch silencioso | Aprovar invariantes e tratamento de incidente; D09/D10/D26; B09 |
| R10 — falha de encerramento seguro | Parar somente processos próprios por handles/identidade; PostgreSQL fast; comprovar ausência antes de descarte. Dúvida preserva dados e bloqueia exclusão | Manifesto de processos, criação/executável/cmdline/data/porta, retorno pg_ctl, status, filhos, listeners, hashes e raízes canônicas; segredos sanitizados | Kill por nome/PID sem identidade, processo desconhecido encerrado, descarte com filho vivo, escape de raiz, segredo em log, ausência de prova final | Aprovar escopo de stop/descarte e tratamento manual de bloqueio; B10; nenhuma permissão para tocar :5432 |

## Riscos adicionais transversais

| ID | Risco / controle de ensaio proposto | Critério que bloqueia |
| --- | --- | --- |
| R11 | Backup com hash correto mas incompleto; inventariar roles, schema, dados, histórico externo, manifestos, dependências e WAL necessário ao alvo | Digest válido sem prova de conjunto recuperável não aprova B01 |
| R12 | Disco/tempo/processos excedidos; incluir simultaneamente backup, WAL, origem, alvo e evidências na quota | Falta de espaço, corte de captura, retry fora do orçamento ou limpeza automática de evidências reprovam controle operacional |
| R13 | Confundir simulação com experimento real; rotular fixtures e oráculo de teste | Resultado construído em memória anunciado como restore, fencing ou eleição comprovados invalida o caso |
| R14 | Restore em endpoint incorreto ou restauração de permissões inadequadas; validar identidade e negar privilégios | Qualquer acesso à produção, :5432, segredos antigos ou caminho preexistente interrompe o ensaio; não procurar fallback |

## Interpretação de falhas e dependências

Um bloqueio seguro em B02/B04 pode satisfazer o subcritério de detecção; não
prova preservação de confirmações ou retomada. Perda incompatível com a garantia
prometida reprova R02 mesmo que R03 detecte a perda. As duas conclusões devem
constar separadamente no relatório futuro.

R06/R07 não se encerram pela simples recusa SQL de um fence velho após restart:
essa prova limitada já existe em E3-A-R3, mas não revoga a autoridade que o backup
antigo possa ressuscitar. Um oráculo de teste só detecta a condição; definir uma
fonte de verdade de autoridade exige decisão humana, implementação revisada e
prova própria. Interações reais de combate/owner continuam no gate E4.

Não há mitigação executada nesta etapa além da revisão de documentos e hashes.
Consultar [plano](ORG-03-E3-B-PLANO.md), [critérios](ORG-03-E3-B-CRITERIOS-ACEITE.md)
e [pendências](ORG-03-E3-B-PENDENCIAS.md).
