# ORG-03-E3-B-A1 — Fases, cenários B01–B10 e riscos R01–R14

Revisão iniciada em 16/11 e concluída em 17/11/2026. **Planejamento, sem cenários executados nesta A1.** Permanecem todos
os dez cenários e quatorze riscos do desenho anterior. Os sufixos de
subcritérios abaixo servem à rastreabilidade, não substituem os IDs originais.

## Divisão proposta em fases

| Fase | Escopo | Entrada/aceite futuro | Saída permitida |
| --- | --- | --- | --- |
| F0 — A1 atual | Documentos, alternativas e leituras estáticas | Ordem documental atual | Proposta revisável; nenhum aceite definitivo inferido |
| F1 — implementação isolada | Novos componentes de inspeção/guardas, testes estáticos/simulados com rótulo próprio; nenhum processo operacional | B-A1 suficiente ao recorte e B-A2 expresso | Pacote revisado, sem executar cluster/SQL/backup/restore |
| F2 — primeira rodada de inspeção | Um backup físico frio candidato, um restore, S0/S1/R, histórico/manifestos e validação de bloqueio dos escritores | Método/perfil/contrato aceitos; B-A3/B-A4/B-A6 explícitos; pacote F1 revisado | Resultados de dados e detecção do recorte; casos/subcritérios positivos de escrita continuam bloqueados |
| F3 — confirmação até alvo posterior | Material/cadeia aprovados para P1/P2, mensuração de preservação; pode continuar apenas leitura | Nova ficha/rodada e perfil/método/quotas aceitos | Prova de dados até T/S1; ainda não implica D33 |
| F4 — autoridade e retomada econômica sintéticas | B05/B06/B07/B08/B09 dependentes de escrita, processo antigo vivo, concorrência e controles | Mecanismo D33/D07 aprovado, B-A5 e operação específica; implementação/provas próprias | Prova limitada de autoridade/idempotência no recorte; sem integração automática E4 |
| E4/E5 posteriores | Mundo/owner/posse/atores e homologação/legado | Gates e autorizações próprios | Fora deste pacote |

A numeração F0–F4 é apenas divisão proposta desta A1, sem substituir a matriz
de falhas F01–F30 da R6. Não se inicia nenhuma fase futura por concluir documentos.

No candidato F2, a origem escreve somente durante a montagem da fixture S0/S1,
com autoridade sintética nova aprovada para isso. Após S1, a origem deve parar
antes do alvo abrir para inspeção. O destino não recebe admissão econômica,
workers de aplicação, callbacks ou função de pagamento habilitada para o leitor.
É necessário revisar permissões, funções SECURITY DEFINER/execução, logins,
configuração e caminhos de conexão; um default read-only sozinho não é prova
de bloqueio de todos os escritores. Administrador de recuperação e leitor têm
funções distintas; alterações de configuração aprovadas ficam inventariadas.
Isso contém o ensaio, sem constituir mecanismo D33 de produção.

## Cobertura integral e parcial da primeira rodada sugerida

| Caso mantido | O que F2 pode avaliar | O que não pode receber PASSOU em F2 |
| --- | --- | --- |
| B01 | Recuperação integral do conjunto S0 para inspeção, incluindo objetos/globais/histórico aprovados | Autoridade ou escrita pós-restore; não fazem parte da aprovação de dados |
| B02 | Caso negativo de backup antigo: DET e bloqueio; PRES de S0 conforme P0 | Preservação da confirmação S1 ausente; P2 não pode passar com esse material |
| B03 | Inspeção das fronteiras já comprovadas pela fixture; classificações com cobertura explícita | Novo ensaio de perda de resposta/commit desconhecido e retomada econômica; subcasos não exercitados ficam pendentes |
| B04 | Subcritério negativo de arquivo ausente em cópia auxiliar nova, leitura incerta e zero efeitos; inventário | Retry econômico real e recuperação automática de histórico |
| B05 | Leitura e comparação do recibo/intenção antigos | Replay por escrita e tentativa com outra OperationId; dependem de AUTH |
| B06 | Subcritério negativo: autoridade sem prova não é admitida | Revogação efetiva de processo vivo e autoridade nova funcional |
| B07 | Inspeção de manifesto parcial e enumeração de dívida comprovada; detectar lacuna em cópia auxiliar | Settlement/pagamento após restore, geração/reparo de manifesto |
| B08 | Subcritério negativo de duas pretensões bloqueadas antes de despacho | Dois escritores reais sob mecanismo de autoridade, fencing e controle positivo |
| B09 | Integridade relacional completa de R e comparação de snapshots/heads históricos | Testar escrita de snapshot antigo e proteção ativa do head com escritor habilitado |
| B10 | Encerramento/descarte de todos os recursos efetivamente autorizados na F2 | Encerramento de topologia/falhas que não existiram nessa rodada |

Assim, B01-dados, B02-negativo/P0 e B10 podem ter seus objetivos de inspeção
avaliados integralmente no recorte. Os demais ficam parcialmente cobertos ou
bloqueados, com subcritérios individuais. Isso não reclassifica B01–B10 completos
nem fecha E3-B: o relatório deve conservar cada parte obrigatória ainda pendente.

## Especificações propostas por cenário

### B01 — Corte consistente

- **Dependência/pré-condições:** método e P0 aprovados; origem nova identificada,
  S0 selado, objetos e histórico externo inventariados; leitor do alvo restrito.
- **Estímulo:** recuperar uma única cópia em alvo novo; inspecionar seu conjunto
  S0. **Evidência:** backup/cópia, delta de configuração, startup/identidade,
  consultas integrais e capturas. **Oráculo:** inventário e imagens S0 externos ao rollback.
- **Aprovação:** nenhum objeto/dado/recibo/revisão exigido ausente ou divergente;
  sem admissão de escritor. **Reprovação:** perda em S0, omissão não aprovada,
  erro de restore ignorado ou escrita econômica habilitada.
- **Limite:** prova do corte, não S1, D33 ou RPO de produção.
  **Autoridade de escrita:** só na origem para fixture; nenhuma pós-restore.
  **PostgreSQL real:** necessário para prova operacional; leitura de arquivos
  ou simulação não substitui restore. **F2:** objetivo de dados completo.

### B02 — Backup antigo

- **Dependência/pré-condições:** contrato/oráculo com cobertura S0/S1 e perfil
  aprovados; pelo menos uma confirmação posterior ao backup na origem.
- **Estímulo:** comparar R=S0 com S1 sem pagar/reaplicar operação ausente.
  **Evidência:** IDs, barreiras de confirmação, diff por campo e decisão de
  bloqueio. **Oráculo:** confirmações S1 seladas fora do conjunto restaurável.
- **Aprovação:** DET identifica toda lacuna; P0 preserva S0; ausência em R não
  vira nunca aplicado. **Reprovação:** lacuna omitida, replay ou perda contra o
  perfil escolhido; se P2 foi prometido, confirmação S1 ausente reprova PRES.
- **Limite:** detectar perda não recupera a confirmação. **Autoridade de escrita:**
  origem para produzir S1; nenhuma no alvo. **PostgreSQL real:** sim, para prova
  S1 versus restore. **F2:** negativo completo; PRES-S1 não demonstrado.

### B03 — Fronteiras de confirmação

- **Dependência/pré-condições:** D10/D12 e cobertura do fluxo aprovadas; separar
  nunca despachado, pré-COMMIT comprovado e despacho sem resposta terminal.
- **Estímulo:** classificar intenções nessas fronteiras; futuros subcasos reais
  exigem falha especificamente autorizada, sem herdar relay/aceite E3-A.
  **Evidência:** intenção, despachos/ACK/rollback, consultas e capturas completas.
  **Oráculo:** fronteiras comprovadas e cobertura histórica, não timeout.
- **Aprovação:** cada resultado respeita a prova; desconhecido continua incerto.
  **Reprovação:** saída de worker/relay vira resultado econômico, consulta ao
  backup velho prova falso rollback ou confirmação é inventada.
- **Limite:** F2 não reexecuta perda de transporte como se fosse E3-A.
  **Autoridade de escrita:** necessária na origem para estímulo real; qualquer
  retomada no alvo exige D33. **PostgreSQL real:** sim para ensaio de commit;
  classificador estático é apenas simulação. **F2:** subcritérios de inspeção.

### B04 — Histórico incompleto

- **Dependência/pré-condições:** D26/corte conjunto aceitos; arquivo esperado
  identificado por índice/localizador/digest; backup original imutável.
- **Estímulo:** apresentar à inspeção uma cópia auxiliar nova com histórico
  indisponível/divergente, sem alterar a evidência selada. **Evidência:** cópia,
  diferença, lookup incompleto e imagens sem efeitos. **Oráculo:** inventário
  externo e recibo original coberto, quando disponível.
- **Aprovação:** ausência/integridade insuficiente distinguida de nunca aplicado;
  leitura incerta e nenhuma concessão. **Reprovação:** novo prêmio/recibo,
  replay automático ou completude inferida apenas por hash.
- **Limite:** recuperação automática e retry não são demonstrados por leitura.
  **Autoridade de escrita:** nenhuma para negativo; necessária para retry futuro.
  **PostgreSQL real:** para comprovar vínculo do índice no alvo; teste de
  arquivo isolado não basta. **F2:** negativo, partes positivas pendentes.

### B05 — Replay de intenção antiga

- **Dependência/pré-condições:** D03/D10/D26, histórico completo e D33 válido
  para o escritor; intenção original e origem únicas; B-A5/operacional próprios.
- **Estímulo:** mesma intenção/payload e variação divergente; outro ID para a
  mesma origem. **Evidência:** resultados e imagens integrais antes/depois,
  recusas e IDs de cada tentativa. **Oráculo:** recibo original, origem/GrantId
  e versão confirmada, sem resolver prêmio novamente.
- **Aprovação:** histórico idêntico ou recusa apropriada, nenhum efeito/revisão
  extra indevido. **Reprovação:** duplicação, conteúdo mutável aceito ou erro
  de histórico tratado como autorização para novo ID.
- **Limite:** dedup local não fecha D33. **Autoridade de escrita:** sim, bloqueada
  pós-restore enquanto D33 pendente. **PostgreSQL real:** sim para replay real.
  **F2:** somente leitura; caso de escrita BLOQUEADO.

### B06 — Autoridade obsoleta

- **Dependência/pré-condições:** mecanismo D33/D07 aceito, domínios de rollback
  identificados e processo antigo controlado vivo para o positivo futuro.
- **Estímulo:** negativo rejeita admissão sem prova antes de despachar SQL;
  positivo futuro tenta escrever com autoridade velha e com a nova aprovada.
  **Evidência:** identidades, prova de revogação e resultados/efeitos.
  **Oráculo:** mecanismo aprovado fora da verdade retrocedida, não fence de R.
- **Aprovação:** negativo bloqueia; positivo exige velho recusado e novo válido.
  **Reprovação:** velho efetiva escrita, ambos válidos ou simples bloqueio de
  tudo declarado retomada. Sem mecanismo, positivo é BLOQUEADO, não PASSOU.
- **Limite:** SQL não retira corpo/combate; E4 separado. **Autoridade de escrita:**
  necessária no positivo; negativo inicial não habilita nenhum escritor.
  **PostgreSQL real:** obrigatório para revogação efetiva; classificador negativo
  pode ser estático, com esse rótulo. **F2:** só negativo de admissão.

### B07 — Manifesto parcial

- **Dependência/pré-condições:** D19/D26 e fronteira do manifesto aprovados;
  A concluído/B pendente em S0, origens/resultados originais preservados.
- **Estímulo:** inspecionar manifesto e variante auxiliar ausente/truncada;
  futuro settlement apenas da dívida comprovada. **Evidência:** elegíveis,
  lacunas, recibos e pendências identificadas, estado A/B antes/depois.
  **Oráculo:** manifesto original coberto e recibos correlacionados.
- **Aprovação:** enumerar só dívida demonstrada; manifesto insuficiente bloqueia;
  positivo futuro paga B uma vez e preserva A. **Reprovação:** inventar obrigação,
  repetir A, esquecer B ou avançar prefixo sobre lacuna.
- **Limite:** relatório de dívida não é pagamento ou reconstrução durável.
  **Autoridade de escrita:** necessária para settlement, bloqueada em F2.
  **PostgreSQL real:** sim para cruzamento com restore e settlement; fixture
  estática só testa análise. **F2:** inspeção/negativo; positivo bloqueado.

### B08 — Duas autoridades

- **Dependência/pré-condições:** D06/D07/D33/mecanismo de comunicação e
  particionamento aprovados; duas instâncias identificadas sob controle.
- **Estímulo:** negativo classifica duas pretensões sem prova como bloqueadas;
  positivo futuro concorre na mesma origem/beneficiário com barreira real.
  **Evidência:** IDs de instâncias/tentativas, decisões de autoridade, eventos e
  commits/recusas/imagens. **Oráculo:** autoridade aprovada e origem única.
- **Aprovação:** positivo tem no máximo um escritor válido e um efeito; negativo
  não despacha escritores. **Reprovação:** duas autoridades aceitas, vencedor
  casual sem validade ou eleição criada pelo teste.
- **Limite:** dois leitores ou dois bloqueios não provam fencing concorrente.
  **Autoridade de escrita:** sim no positivo; bloqueada agora.
  **PostgreSQL real:** sim no positivo; negativo estático não o substitui.
  **F2:** só negativo; B08 positivo BLOQUEADO.

### B09 — Integridade global e snapshot antigo

- **Dependência/pré-condições:** contrato D09/D10/D26, material coberto e
  imagens completas; autoridade aprovada se testar atualização/retry.
- **Estímulo:** confrontar heads, versões, operações, recibos/arquivo, grants,
  origens, dívidas e snapshot antigo; depois, em fase autorizada, tentar
  sobrescrita antiga. **Evidência:** diffs completos e bijeções, não só somas.
  **Oráculo:** S0/S1 cobertos e relação de revisões/resultados do modelo.
- **Aprovação:** integridade completa no corte; positivo futuro recusa snapshot
  velho sem sobrescrever economia recente. **Reprovação:** órfão/duplicação,
  versão errada, perda de obrigação ou reparo silencioso.
- **Limite:** bijeção não prova atualidade global nem autoridade.
  **Autoridade de escrita:** não para inspeção, sim para tentativa ativa.
  **PostgreSQL real:** sim para R operacional; análise JSON é estática.
  **F2:** integridade de leitura; proteção ativa ainda pendente.

### B10 — Encerramento e descarte seguro

- **Dependência/pré-condições:** B-A6 e ficha operacional exata, raízes próprias,
  handles/identidades e inventário da rodada; contingência aceita.
- **Estímulo:** parada fast dos bancos próprios e encerramento dos workers
  efetivamente existentes por handles originais; conferir ausência antes de
  descartar só dados/segredos autorizados. **Evidência:** comandos/retornos,
  processos/filhos/listeners/status, caminhos canônicos e hashes preservados.
  **Oráculo:** manifesto de propriedade da rodada e checagens independentes.
- **Aprovação:** nenhum recurso próprio ativo antes do descarte e nenhuma
  alteração indevida. **Reprovação:** kill desconhecido, identidade divergente
  ignorada, quota ampliada ou descarte prematuro; dúvida bloqueia exclusão.
- **Limite:** não garante encerramento de topologia não ensaiada nem saúde de
  :5432. **Autoridade de escrita econômica:** não; controle de ciclo de vida
  exige autorização própria. **PostgreSQL real:** sim para encerrar o laboratório
  real. **F2:** completo somente para os recursos da rodada autorizada.

## Revisão dos riscos existentes: nenhum removido

Todos os comportamentos/evidências/reprovações e dependências da
[matriz R01–R14](ORG-03-E3-B-MATRIZ-RISCOS.md) são mantidos. Acrescentam-se estas
relações, sem editar a matriz histórica:

| Risco mantido | Relação explícita / evidência adicional proposta | Casos / decisão |
| --- | --- | --- |
| R01 — confirmação antes de commit | Transporte interrompido não resolve economia; cobertura deve distinguir despacho de ACK/commit | B03; contrato e perfil D12 |
| R02 — confirmação antes da interrupção | Backup antigo pode perder confirmação conhecida; DET e PRES têm vereditos independentes | B01/B02; P0/P1/P2 |
| R03 — estado antigo | Dado íntegro pode portar autoridade inválida; comparar corte e autoridade independentemente | B02/B06; D33 |
| R04 — histórico indisponível | Histórico parcial torna dedup não demonstrável; falta de recibo não reabre concessão | B04/B05; D26 |
| R05 — replay antigo | Mesmo payload/hash não concede autoridade e outro ID não contorna origem única | B05/B08; D03/D26/D33 |
| R06 — escritor obsoleto | Restaurar fence velho pode torná-lo aparentemente atual; parar origem só contém o recorte | B06; mecanismo de revogação |
| R07 — duas autoridades | Eleição/seleção e recusa efetiva no commit são obrigações distintas; incluir reconexão/cache/partição | B08; D07/D33 |
| R08 — manifesto parcial | Corte desigual entre manifesto e DB cria dívida omitida ou duplicada; não reconstruir por hipótese | B07; D19/D26 |
| R09 — integridade econômica | Somas iguais não provam bijeção/head correto; integridade não prova completude pós-S0 | B09; D09/D10/D26 |
| R10 — encerramento | Cópia física exige parada comprovada para S0 e encerramento final; não tocar processo desconhecido | B01/B10; B-A6 |
| R11 — backup incompleto | Hash válido do arquivo não prova que todos os objetos, WAL e histórico foram incluídos | B01/B04/B09; método/inventário |
| R12 — quota | Três cópias físicas, WAL e temporários entram no pico; margem hipotética não é espaço medido | B01/B10; dimensionamento antes do aceite |
| R13 — simulação tratada como prova | Negativos estáticos/classificação não são restore real nem D33 positivo | Todos; rótulos e evidência causal |
| R14 — destino/permissões | Identificador físico do clone pode repetir origem; data/porta/handle e privilégios devem ser revalidados | B01/B06/B10; isolamento/ACL |

As relações não constituem mitigação implementada. Um subcritério bloqueado
permanece identificado; falha não vira novo escopo reduzido sem decisão humana.
Estado desta A1: todos os ensaios operacionais NÃO EXECUTADOS; nenhum PASSOU
operacional novo, nenhum recurso criado ou porta sondada.
