# ORG-03-E3-B-A1 — Contrato experimental proposto S0/S1/R

Revisão iniciada em 16/11 e concluída em 17/11/2026. **Candidato a B-A1; não é contrato produtivo nem aceite recebido.**
Referências: D03/D06/D09/D10/D12/D19/D26/D33 na
[matriz R6](ORG-03-R6-MATRIZ-DECISOES.md) e no
[quadro R7](ORG-03-R7-QUADRO-APROVACAO-E1.md). Nenhum campo abaixo define DDL,
schema novo ou algoritmo de emissão de IDs.

## Fronteiras e domínios de evidência

S0 é o estado consistente efetivamente coberto pelo backup e seus complementos
aprovados. Inclui identificação da origem e do corte, método/versão, objetos e
intervalos de histórico abrangidos. No método físico frio proposto, só existe
depois da parada limpa e quiescência de arquivos externos; no lógico é o corte
do snapshot, não o fim da exportação; no temporal precisa identificar o ponto
consistente da base e o alvo adicional aprovado.

S1 é a referência da origem depois de confirmações adicionais ao corte. Cada
operação deve ter ordem/correlação observável e classificação da confirmação.
Relógios de parede são auxiliares; não substituem barreiras, IDs, ordem de
eventos ou LSN quando pertinente.

R é a imagem recuperada **a partir do conjunto aprovado**, com inventário dos
arquivos realmente usados, alvo alcançado e diferenças. No método B inicial,
o alvo é S0: não afirmar que R contém S1. No método C, distinguir R recuperado
até T de S1 posterior a T. R abrir para leitura não concede admissão econômica.

O domínio restaurável inclui DB, cópias de archive/índices e manifestos definidos
para o corte. O oráculo de ensaio fica fora desse conjunto, em caminho próprio
e sem ser sobrescrito pelo restore. É independente do rollback lógico, mas
continua na mesma máquina no laboratório; não prova tolerância a perda do host.

## Oráculo externo: cobertura explícita e limitações

A futura fixture deve produzir um registro de cobertura que identifique:

- Run-id, origem (instância/data/endpoint e identidade lógica), versão do
  contrato e domínios de IDs; nenhuma conta/credencial real.
- Conjunto **enumerado** de OperationIds/GrantIds/origens que conhece; período
  e intervalo de eventos inicial/final, buracos declarados e fronteiras S0/S1.
- Para cada operação, intenção/payload imutável, beneficiário, resultado
  resolvido, confirmação observada (ACK/observador e fronteira correspondente),
  revisões e localizador/digest do recibo quando existente.
- Relação entre intenção preparada, despachos, tentativas, ACK e confirmação;
  origem de cada afirmação, falhas de captura e completude por fluxo.
- Manifesto de arquivos e hashes, momento de selagem, persistência durável
  comprovada no perfil do ensaio e ausência de mutação durante recuperação.

Uma lista de ACKs positivos **não prova ausência** de commit para IDs não
listados. Para “nunca confirmado/não aplicado” é necessária evidência positiva
da fronteira sem COMMIT ou leitura autoritativa de um domínio completo até o
ponto relevante, sem rollback desconhecido. Oráculo incompleto não autoriza
resultado negativo terminal. Captura interrompida produz lacuna de cobertura.

O registro mede o ensaio. Não concede grants, não autentica proprietário, não
emite autoridade, não reconstitui recibos no DB e não vira banco de produção.
SHA-256 verifica bytes, não autenticidade da origem ou completude histórica.

## Classificação por operação, sem colapsar dimensões

| Situação | Evidência mínima | Classificação proposta / ação dependente |
| --- | --- | --- |
| Nunca confirmada com evidência suficiente | Barreira anterior a COMMIT sem despacho e rollback/encerramento provado, ou domínio completo que exclua aplicação; nenhuma confirmação contraditória | NOT_CONFIRMED apenas no escopo provado; não cria autorização para tentar escrever após restore |
| Confirmada e coberta por S0 | Confirmação original correlacionada + recibo/efeito/revisões em S0 e R | CONFIRMED_PRESENT, com igualdade integral; autoridade é outra dimensão |
| Confirmada somente depois de S0 | Confirmação S1 fora do rollback e operação ausente/diferente em R | CONFIRMED_MISSING_IN_R ou DIVERGENT_IN_R; preservação depende do perfil; reconciliação bloqueada sem história suficiente; não reclassificar como nunca aplicada |
| Resultado desconhecido | COMMIT despachado sem prova terminal ou cobertura incompleta | ECONOMIC_UNCERTAIN; sem novo efeito, sem novo ID de conveniência e sem anúncio definitivo |
| Recibo histórico indisponível | Índice/localizador/digest esperado, arquivo ausente/inacessível ou cobertura não demonstrada | HISTORY_UNAVAILABLE/UNCERTAIN para consulta econômica; confirmação externa já comprovada não é apagada; impedir retry que possa repetir efeito |
| Autoridade não validável | Dados podem ser completos, mas não há prova atual de claim/revogação resistente ao rollback | AUTHORITY_UNVERIFIED e admissão/escrita bloqueadas, mesmo com CONFIRMED_PRESENT |

Se houver múltiplas condições, registrar todas: por exemplo confirmação
histórica conhecida, recibo indisponível e autoridade não validada. Não reduzir
essas três afirmações a um único “NOT_FOUND”. Não fabricar recibo/obrigação,
resolver RNG novamente ou transferir recompensa de outra origem.

## Quatro resultados obrigatoriamente separados

| Eixo | Pergunta / oráculo | PASSOU proposto | Falha ou bloqueio |
| --- | --- | --- | --- |
| DET — detecção de perda | Diff por IDs/campos S1 versus R dentro da cobertura externa | Todas as lacunas introduzidas e divergências encontradas, sem falsos negativos | Lacuna omitida reprova; cobertura insuficiente deixa inconclusivo |
| PRES — preservação de confirmação | Conjunto exigido pelo perfil D12 versus R | Todos os resultados/efeitos/recibos/revisões exigidos sobrevivem | Qualquer perda exigida reprova, mesmo que DET passe |
| REC — reconciliação econômica | Histórico completo, IDs/payload/resultados e bijeções | Resultado consultável consistente, sem efeitos extras; distinguir inspeção de retry real | Falta de história bloqueia; duplicação/divergência reprova; retry não executado não passa |
| AUTH — validade da autoridade | Mecanismo D33 aprovado e prova atual de revogação/claim | Escritor válido comprovado e velho recusado sob falha prevista | D33 sem mecanismo/prova permanece BLOQUEADO; negar tudo não é retomada |

Na primeira rodada sugerida, AUTH positivo fica BLOQUEADO e REC de mutação/retry
fica NÃO EXECUTADO/BLOQUEADO. É possível avaliar REC de leitura como subcritério,
com rótulo próprio, sem promovê-lo a idempotência por escrita comprovada.

## Alternativas D12 de perfil experimental RPO/RTO

São opções para decisão, sem promessa de produção. RPO é a fronteira de dados
cuja perda o perfil admite; medir também quantidade/IDs perdidos, não só segundos.
RTO deve distinguir **dados disponíveis para inspeção** de **economia admitida**.

| Perfil candidato | Confirmações exigidas / perda tolerada | Condição de reprovação | Incertas e S1→R | RTO experimental proposto |
| --- | --- | --- | --- | --- |
| P0 — preservação do corte S0 | Zero perda dentro do conjunto confirmado coberto por S0; confirmações exclusivamente pós-S0 podem faltar, desde que enumeradas e detectadas; não é RPO zero até S1 | Qualquer perda/campo divergente em S0 ou lacuna S1 omitida; nenhuma compensação automática | Comparar conjuntos S0 e S1 separados; incertas ficam bloqueadas; R=S0 não apaga confirmação externa S1 | Objetivo de até 120 s para recuperação+inspeção do caso; valor pendente, não medido; RTO econômico indefinido/bloqueado por D33 |
| P1 — perda limitada até alvo T | Definir T e limite Δ antes do ensaio; todas as confirmações até T devem sobreviver; as posteriores só podem faltar dentro da janela/quantidade aceita | Perda anterior a T, idade/quantidade além do limite, T não demonstrado ou alteração do limite depois da falha | Exemplo a decidir: Δ≤10 s e até uma confirmação pós-T; exige ordem e medição confiáveis, não dedução só de relógio | Objetivo de 120 s para dados, se aceito; econômica separada e condicionada a AUTH |
| P2 — preservação integral da janela S1 | Zero perda de todas as confirmações enumeradas até a barreira final S1; método/cadeia deve cobri-las de fato | Qualquer confirmação S1 ausente/divergente; DET positivo não salva PRES | Despachos sem confirmação permanecem incertos até evidência completa; comparar cada operação e resultado, não contagem total | Meta de dados deve ser escolhida dentro da quota aprovada; sem prova D33 não há meta econômica cumprida |

**Recomendação:** P0 para o método físico frio B inicial, explicitando que a
confirmação S1 propositalmente ausente será detectada e não recuperada. P1/P2
exigem outra definição de material/alvo, por exemplo WAL aprovado ou backup
posterior, e não podem ser prometidos ao restaurar exclusivamente S0 antigo.
O exemplo numérico P1 e as metas de tempo são propostas, não escolhas aceitas.

Medir futuramente: instante de indisponibilidade/acionamento, início/fim de
transferência, restore, startup/replay, identidade, leitura integral, reconciliação
e decisão de admissão. Na inspeção, definir t0 como acionamento do restore e
t1 como fim da verificação lógica; informar também duração anterior de backup.
Se exceder a quota de 120 s/caso, reprovar o limite de tempo do caso, sem relabelar
como PASSOU por o banco finalmente abrir. A indisponibilidade produtiva incluiria
detecção, transferência remota, investigação, humanos, dependências, tráfego e
retomada de ator; nada disso foi medido aqui.

## D26 — Cobertura obrigatória do conjunto recuperável

| Elemento | Conteúdo e vínculo que devem sobreviver no corte | Completude insuficiente / critério observável |
| --- | --- | --- |
| OperationId | Namespace/origem e identidade estável entre todas as tentativas | ID ausente em R não prova ausência fora do corte; diferença registrada |
| GrantId e origem econômica | Beneficiário/origem tipada associados à operação; unicidade e linhagem | Dois grants/origens repetidos ou origem não rastreável bloqueiam/reprovam; não atribuir ID novo |
| Payload imutável | Kind, beneficiário, bases e parâmetros resolvidos, fingerprint e versão | Hash diferente no retry é conflito; hash igual não substitui autorização |
| Resultado resolvido | Recursos/deltas/imagem e revisões que realmente foram confirmados | Resultado ausente não pode ser reconstruído por RNG ou regras atuais |
| Recibo local/arquivado | Estado da operação, resultado e referências; arquivo com localizador/digest | Lookup incompleto/inacessível mantém incerteza; não pagar de novo |
| Índice de dedup | OperationId e restrições por origem/GrantId que continuem consultáveis | Recibo sem índice ou índice sem histórico suficiente bloqueia retomada; nenhum purge proposto |
| Manifestos | Conjunto de elegíveis, resultados/intenções por beneficiário, dívidas, sequências e lacunas | Parte ausente não significa grupo concluído nem permite gerar obrigações novas |
| EconomicRevision | Linhagem de efeitos/recibos e revisão por personagem | Contador agregado igual com mapeamento errado reprova; retrocesso restaurado não é autoridade atual |
| Head/StateRevision e snapshots | Head único aponta à versão correta; snapshot histórico é leitura | Head incompatível com payload/versão ou sobrescrita com snapshot velho reprova |

O inventário deve enumerar todos os elementos dentro/fora de S0 e como seu corte
foi coordenado. Invariantes: bijeção de operações/recibos/grants no modelo,
origens únicas, versões compatíveis, dívidas explicadas e nenhum órfão oculto.
Não impor que todos os sistemas de produção tenham uma tabela por rótulo ou que
SR=ER sempre: a relação depende do contrato e das operações do modelo.

Se a completude não for demonstrável: registrar IDs afetados, lacuna/razão,
última fronteira comprovada, zero novas concessões/revisões e barreira de
admissão; conservar todos os artefatos. Qualquer tratamento reparador exige
decisão própria. [Cenários e riscos](ORG-03-E3-B-A1-FASES-E-CENARIOS.md).
