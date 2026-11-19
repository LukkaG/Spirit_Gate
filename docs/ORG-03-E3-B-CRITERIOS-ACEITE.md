# ORG-03-E3-B — Critérios de aceite propostos

16/11/2026. Documento para decisão de Lucas. Critérios ainda sujeitos à revisão;
nenhum resultado de backup, restore, failover ou D33 foi produzido nesta etapa.

## Aceite da entrega documental atual

Esta entrega é revisável quando contém os seis documentos solicitados, fontes
de governança, distinção entre observação/cálculo/declaração/limitação, auditoria
estática reproduzível, inventário preservado e dependências explícitas. A revisão
documental pode estar concluída enquanto o início da E3-B permanece bloqueado.

O [encerramento E3-A](ORG-03-E3-A-ENCERRAMENTO.md) sustenta somente R3 no escopo
sintético. Nenhuma mudança de status global foi feita nas matrizes R6/R7 ou nos
relatórios históricos de rodadas interrompidas.

## Aceites humanos separados para trabalho futuro

As linhas abaixo são propostas de autorizações independentes. Nenhuma foi
recebida para E3-B. Não se herdam R3-02 a R3-05 nem os limites daquela rodada.

| Aceite | Conteúdo a decidir expressamente | O que a entrega atual não autoriza |
| --- | --- | --- |
| B-A1 — desenho e contrato experimental | Ratificar subdivisão E3-B, cenários obrigatórios, fronteira de confirmação, manifesto, histórico, tratamento conservador de incerteza e perfil D12/D26; identificar decisões globais que permanecem pendentes | Nenhum contrato econômico novo, implementação ou mudança de D33 |
| B-A2 — implementação isolada | Aprovar cópias novas, revisão do runtime/guardas/validador e testes estáticos/simulados, com hashes de preservação | Nenhum processo PostgreSQL, SQL, backup ou restore |
| B-A3 — preparação operacional | Responsável, data, run-id novo, caminhos absolutos, binários/versão/hash, endpoints exatos e quotas aceitas; disponibilidade e espaço antes do início; método de backup e abrangência | Não há initdb, cluster, reserva de porta ou credencial autorizados agora |
| B-A4 — ensaio de backup/restore | Autorizar separadamente criação de origem/alvo, consultas/fixture sintética, um backup/restore, processos e reinícios limpos limitados; fontes e destinos exatos, sem sobrescrever dados preexistentes | Nenhum crash, failover/promotion ou instalação por associação |
| B-A5 — autoridade/concorrência | Política previamente aprovada para revogação e liderança, dependência fora do rollback e condições de admissão; escolher casos positivos/negativos, limites e falhas permitidas | Não inventar eleição/lease/era nem declarar D33 fechado por bloqueio ou fixture |
| B-A6 — encerramento/descarte | Stop fast próprio, handles/identidades, raízes canônicas removíveis, evidências retidas, prazo/contingência e quem decide se encerramento ficar inconclusivo | Não matar desconhecidos, remover arquivos anteriores ou descartar sem ausência comprovada |

B-A3 deve fixar a ficha completa do [plano](ORG-03-E3-B-PLANO.md): proposta
de dois clusters, um backup/restore, loopback :55437/:55438 e relay opcional
:55439, raiz `temp/org03-e3b/<run-id-novo>`, 1 GiB total/64 MiB de evidências,
4 GiB livres e demais limites. São candidatos não sondados, não uma reserva.
Mudança de data requer registro do run-id/caminho exatos antes de iniciar.
Falta de um valor final bloqueia a operação dependente.

## Evidência mínima de qualquer cenário operacional futuro

O relatório deverá identificar run-id/cenário/versão de scripts, autorização,
binários e configurações efetivas, cluster/data/porta/database/roles e
processos por handle/identidade. Deve apresentar:

1. Intenção imutável, origem/GrantId, conteúdo resolvido e ponto de aceitação,
   com prova de quais dados estavam em S0, em S1 e no destino R.
2. Inventário do backup e dependências, corte temporal/LSN quando aplicável,
   hashes antes/depois e alcance do oráculo independente de confirmações.
3. Barreiras e eventos sequenciais do estímulo, stdout/stderr capturados em
   modo binário e sanitizados, incluindo erros e tentativas intermediárias.
4. Consulta por identidade da operação em processo independente, imagens
   completas e reconciliação de versões, recibos locais/arquivo, índices,
   grants e obrigações. Ausência de resposta é distinta de resposta negativa.
5. Controles negativos e positivos correspondentes, contabilidade de efeitos
   e revisões antes/depois, retries com o mesmo payload e rejeição do divergente.
6. Consumo de quotas, estado final dos processos e prova de isolamento,
   parada, preservação e descarte limitado.

Hash e contagem agregada são necessários para integridade da captura, mas não
substituem causalidade, completude do histórico ou validade da autoridade.
Falha de captura ou oráculo incompleto deixa o caso INCONCLUSIVO, não PASSOU.

## Critérios por cenário e risco

| Caso / riscos | PASSOU exige | REPROVOU ou BLOQUEADO |
| --- | --- | --- |
| B01 / R02/R11 | Corte coberto recuperado integralmente: mesmos IDs/resultados/recursos/revisões e histórico consultável, com abrangência de objetos comprovada | Perda ou modificação no corte prometido; restauração incompleta; método não aprovado |
| B02 / R02/R03 | Detecção da diferença S0/S1 e bloqueio sem replay; preservação de todas as confirmações exigidas pelo perfil é avaliada separadamente | Perda contrária ao perfil reprova recuperação; detecção isolada não aprova durabilidade; sem oráculo é inconclusivo |
| B03 / R01 | Classificação baseada nas fronteiras e cobertura histórica; ausência de evidência mantém incerteza; nenhum falso ACK/rollback | Desconexão/timeout/saída do relay viram resultado econômico definitivo |
| B04 / R04/R11 | Falta ou alteração de arquivo/índice detectada, estado incerto e zero mutação; restituição histórica só se origem/digest comprovados | Inventar recibo, assumir nunca aplicado ou pagar novamente |
| B05 / R05 | Retry não altera resultado nem head e origem única não paga sob outro ID; autoridade de escrita já provada | Duplicação/revisão indevida ou autoridade não homologada para o recorte |
| B06 / R03/R06 | Revogação demonstrada apesar de rollback e processo antigo vivo; escrita velha negada; nova autoridade positiva conforme política aprovada | Só fence local, matar velho, UUID novo ou bloqueio total apresentados como liderança restaurada; D33 não decidido bloqueia positivo |
| B07 / R08 | A preservado, somente obrigações comprovadas reconstituídas; lacuna/manifesto parcial bloqueia conclusão indevida | Completar manifesto por suposição, pagar novamente A ou esquecer B |
| B08 / R07 | Concorrência reproduzível com mecanismo aprovado; no máximo um escritor válido, origens únicas e controles positivos | Ambos aceitos; recusa geral chamada de recuperação; algoritmo inventado |
| B09 / R09 | Bijeção e unicidades globais, head/versões/estado coerentes, resultados arquivados verificáveis e ausência de órfãos não justificados | Apenas soma de contagens, snapshot velho sobrescreve atual, reparo silencioso |
| B10 / R10/R12/R14 | Identidade e handles próprios, stop fast, ausência antes de descarte canônico, evidências preservadas e quotas atendidas | Processo desconhecido tocado, identidade perdida, descarte prematuro, quota ampliada ou artefato antigo modificado |

## Regra de decisão final

Estados permitidos: NÃO EXECUTADO, BLOQUEADO POR DECISÃO, INCONCLUSIVO,
REPROVOU e PASSOU. Um teste simulado deverá ser identificado como SIMULADO,
sem receber o status de ensaio operacional real. Subcritérios de transporte,
detecção, preservação e autoridade terão resultados separados.

A E3-B somente poderá ser proposta como aprovada no recorte final **se todos
os casos obrigatórios aprovados no desenho tiverem evidência válida e PASSOU**,
além dos controles transversais e encerramento. Não remover casos obrigatórios
após falha para fechar o gate. Alteração de escopo deve ter aceite registrado
e limitações explícitas. Falha ou inconclusão obrigatória mantém o gate aberto.

Experimento que só detecta backup obsoleto pode passar seu objetivo negativo;
não passa por isso a recuperação de confirmações ou liderança. D33 depende
de aprovação do mecanismo e prova de revogação, não da aprovação dos documentos.
Mesmo uma futura E3-B sintética válida não aprova automaticamente E4/E5,
políticas D14–D22/D27/D34, autenticação, migração, produção ou SLA.
