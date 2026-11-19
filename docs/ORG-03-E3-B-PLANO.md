# ORG-03-E3-B — Proposta técnica para revisão

16/11/2026. Responsável pela revisão humana: Lucas. **PLANEJADO; NÃO
IMPLEMENTADO; NÃO AUTORIZADO OPERACIONALMENTE.** Nenhum run-id operacional
E3-B foi criado. A autorização E3-A-R3 está consumida e não se transfere.

## Enquadramento no Plano Diretor e governança

Foram consultados AGENTS.md, os planos Corujão VI, VII, VIII e IX, o
[plano conceitual ORG-03](ORG-03-PLANO-PERSISTENCIA.md), R6/R7 e E2/E3-A-R3.
O [Corujão IX](CORUJAO-IX-PLANO.md) preserva as 40 skills e mantém TASK-226
como etapa própria; não redefine persistência nem concede autorização E3-B.
O [Corujão VI](CORUJAO-VI-PLANO.md) estabelece persistência sintética isolada,
sem equipará-la a produção ou à homologação física.

A fonte específica para a E3 é a tabela dos
[gates R6](ORG-03-R6-GATES-DE-IMPLEMENTACAO.md): durabilidade entre processos,
crash real, resposta perdida, failover/restore, retenção e revogação. A
[pendência E2](ORG-03-E2-PENDENCIAS-E3.md) explicita F20/F28/D33 e F30/D12.
Não foi localizada, nos documentos e diretivas textuais pesquisados, uma
especificação anterior detalhada chamada E3-B. Nas referências E3-A esse nome
aparece como exclusão de escopo. Esta é uma **proposta de decomposição** do
trabalho restante do gate E3, a ser validada por Lucas, não uma atribuição
retroativa de escopo aprovado ao Plano Diretor.

| Referência | Obrigação herdada da governança, sem novo aceite |
| --- | --- |
| D03 | Não fabricar histórico, repetir RNG ou conceder recompensa por retry |
| D06/D09/D10 | Autoridade, head, CAS, revisões e unidade transacional devem continuar explícitos; implementações sintéticas não aprovam políticas de produção |
| D12/F30 | Versão/topologia, confirmação, backup, RPO/RTO e perfil de falha exigem decisão |
| D26/F09/F16 | Histórico consultável e dedup não podem desaparecer silenciosamente com arquivamento/restore |
| D33/F20/F28 | Backup antigo não pode reviver fence/época/autoridade; requer prova de revogação antes de admissão |
| D05/D07/E4 | Autenticação, posse, lease, retirada de ator e interações do jogo não são resolvidas pelo banco sintético |

Fontes: [decisões R6](ORG-03-R6-MATRIZ-DECISOES.md),
[falhas R6](ORG-03-R6-MATRIZ-FALHAS.md),
[quadro R7](ORG-03-R7-QUADRO-APROVACAO-E1.md).
Os status históricos desses documentos não foram alterados.

## Pergunta experimental e condição de segurança

Determinar que evidência permite reconciliar um resultado depois de recuperar
dados, sem confundir banco consistente com histórico econômico completo ou
autoridade válida. “Banco subiu”, “backup tem hash” e “transporte terminou” são
insuficientes para admissão, confirmação econômica ou fechamento de D33.

Serão distinguidos três cortes: S0 é o corte consistente do backup; S1 contém
confirmações posteriores a S0 na origem; R é o estado recuperado. O oráculo
experimental de confirmações e intenções deve ser preservado fora do domínio
restaurado para detectar perdas. Esse oráculo mede o ensaio; não se torna
automaticamente uma fonte de autoridade ou serviço de produção.

Se uma operação confirmada em S1 não existe em R, isso demonstra uma lacuna de
recuperação. NOT_FOUND em R não comprova que a operação nunca ocorreu. Manter
resultado incerto e bloquear a ação dependente é o comportamento conservador
proposto, sujeito ao aceite do contrato experimental. Não recriar recibo nem
recompensar de novo para fazer o ensaio passar. Se o perfil promete preservar
toda confirmação, essa perda reprova o perfil, mesmo quando o bloqueio funciona.

## Desenho proposto, sem escolha implícita de arquitetura

Fase documental atual: fechamento E3-A, matriz, critérios, pendências e revisão
humana. Não há implementação do runtime E3-B nesta entrega.

Fase futura I, após aprovação de implementação e operação: origem e alvo
descartáveis isolados, um backup e um restore, ambos sintéticos. Preparar antes
de S0 um histórico confirmado e manifestos parcialmente concluídos; depois de
S0 confirmar uma operação na origem e registrar evidência externa independente.
Restaurar S0 uma única vez. Examinar preservação do passado coberto e detecção
da lacuna S0→S1 sem admitir escritores por padrão. Ausência de história deve
produzir bloqueio, não retorno automático à economia.

Fase futura II, dependente de D33/D07/D12: ensaiar tentativas de duas autoridades
e recuperação de liderança conforme mecanismo previamente aprovado. Se não
houver mecanismo aprovado, o ensaio só pode comprovar detecção/bloqueio; o
caso positivo de recuperação de autoridade fica BLOQUEADO. Não se escolherá
eleição, quorum, era externa, lease ou política de promoção neste documento.
Se a fase II exigir outra rodada ou mais recursos, haverá run-id e aceite próprios.

O responsável e infraestrutura devem escolher backup lógico, físico frio ou
base backup com WAL/PITR, incluindo o alvo temporal e objetos abrangidos.
Essas abordagens têm pressupostos distintos; não são intercambiáveis. Backup
mais WAL permite recuperação além do corte base quando a cadeia está disponível.
[Documentação oficial PostgreSQL 18: backup/restore](https://www.postgresql.org/docs/18/backup.html),
[arquivamento contínuo](https://www.postgresql.org/docs/18/continuous-archiving.html).
Esses links da série 18 podem evoluir; a execução futura deve fixar e registrar
a versão efetiva. A versão histórica auditada continua sendo 18.6.

## Catálogo proposto de ensaios

Os IDs B01–B10 são novos rótulos de planejamento. Não são resultados executados.
Cada caso exige imagens integrais antes/depois e logs de todas as falhas.

| Caso | Preparação/estímulo futuro | Evidência/expectativa proposta | Dependência |
| --- | --- | --- | --- |
| B01 — corte consistente | Backup S0 contendo confirmação, recibo e intenção; restore isolado | Mesmo resultado resolvido, OperationId, GrantId, revisão, hash de manifesto e objetos de autorização necessários; nenhuma admissão automática | Método de backup, fronteira de aceitação e escopo aprovados |
| B02 — backup antigo | Confirmar operação somente em S1; R contém S0 | Detectar confirmação ausente comparando oráculo e inventário; impedir replay; declarar perda conforme perfil RPO | D12/D26; fonte de verdade fora do rollback aprovada antes de qualquer recuperação automática |
| B03 — perda antes de commit | Intenção durável e despacho sem confirmação, com variantes pré-COMMIT e resultado desconhecido | Distinguir intenção não aplicada de resultado incerto por evidência suficiente; indisponibilidade não equivale a rollback | Fronteiras experimentais e política de pendência |
| B04 — histórico incompleto | Resultado arquivado indisponível ou digest/índice incompatível em cópia sintética | UNCERTAIN/bloqueio, zero nova concessão; hash não substitui completude | D26, corte conjunto DB/arquivo |
| B05 — replay | Reenviar mesma intenção imutável e tentar outro ID para mesma origem | Mesmo recibo ou recusa segura; nenhum incremento extra de recursos/revisão/grant | Dedup disponível; não executar escrita se D33 não resolvido |
| B06 — autoridade obsoleta | Levar fence/geração do backup e comparar com autoridade vigente fora de R | Escrita antiga recusada; falha na prova impede admissão; registrar old-process vivo sem o encerrar para simular fencing | D33 e mecanismo aprovado; sem isso, caso positivo bloqueado |
| B07 — manifestos parciais | A concluído/B pendente em S0; variante manifesto ausente/truncado em cópia auxiliar | Recuperar apenas dívida demonstrada; manifesto incompleto não autoriza regeneração; preservar A | Contrato de manifesto e completude aprovados |
| B08 — duas autoridades | Duas instâncias tentam operar na mesma origem e personagem | No máximo um escritor válido e efeito; ambas recusadas se validade não puder ser provada | D33/D07, política de isolamento; não inventar eleição |
| B09 — reconciliação global | Confrontar todos os heads, versões, recibos locais/arquivados, operações, grants e obrigações | Bijeções, origens únicas, revisão consistente; snapshot antigo não sobrescreve head atual | Modelo sintético revisado; resultado negativo não é reparado silenciosamente |
| B10 — encerramento | Parar processos próprios e conferir identidades/handles e inventário | Prova de parada antes de descarte próprio; qualquer dúvida preserva dados/evidências e bloqueia exclusão | Aprovação operacional e plano de contingência |

B05/B06/B08 positivos dependem da autoridade aprovada, mesmo se B01 puder
validar dados em modo de inspeção. Detectar bloqueio corretamente não equivale
a demonstrar retomada de liderança. Failover real não é necessário para os
casos documentais; se proposto depois, exigirá perfil e aceite separados.

## Proposta de ambiente e quotas, ainda sem autorização

Não foram sondadas portas, criados diretórios operacionais ou reservados recursos.
Antes de qualquer implementação operacional, a ficha final deve substituir os
campos abaixo por valores exatos e receber aceite de Lucas.

| Item | Proposta a aprovar |
| --- | --- |
| Run-id | `<AAAAMMDD>-lucas-e3b-<nonce-novo>`; gerar e registrar data/caminho exatos antes de operar; nunca reutilizar E3-A |
| Raiz futura | `C:/Users/lucas/Documents/GitHub/Projeto-K/temp/org03-e3b/<run-id-novo>` |
| Subdiretórios | `source-data`, `restored-data`, `backup`, `history`, `evidence`, `secrets`; oráculo externo ao conjunto restaurável, sob raiz própria |
| Endpoints candidatos | Somente loopback: origem :55437, alvo :55438; relay :55439 apenas se necessário e autorizado; disponibilidade NÃO verificada; sem fallback |
| Topologia máxima | Dois clusters descartáveis (origem e alvo), até dois simultâneos; um backup e um restore na rodada; nenhum terceiro data ou recriação como fallback |
| Interrupções | Zero crashes, zero failovers/promotions nesta proposta inicial; até quatro starts totais e até dois reinícios limpos, explicitados no aceite |
| Dados | Até oito personagens, dois proprietários, 64 intenções, payload 16 KiB; no máximo três tentativas por intenção |
| Processos/conexões | Até três workers simultâneos, 32 instâncias totais, seis conexões SQL agregadas entre os dois clusters; até dois coordenadores sintéticos contidos nessa concorrência; um relay se aprovado |
| Tempo | 120 s por caso, 20 min de operação total, aborto seguro ao atingir quota; não reiniciar cronômetro após falha |
| Disco | Proposta de 1 GiB total incluindo origem, alvo, backup, WAL e arquivos auxiliares; até 64 MiB de logs/evidências; pelo menos 4 GiB livres antes de iniciar; limites menores exigem demonstração de viabilidade antes do aceite |
| Dependências | Somente binários já existentes e identificados por versão/caminho/hash; nenhuma instalação ou serviço |

São quotas propostas para um laboratório diferente, não extensão dos limites
E3-A. Capacidade de backup/WAL deve ser estimada antes de aceitar; falta de espaço
ou recurso bloqueia o início. Sem ampliação automática, reaproveitamento de
portas 5432/55433–55436 ou kill por nome. Não se deve reaproveitar código de
limpeza do R3 sem revisão do novo conjunto de raízes e handles.

## Segurança e resultado esperado da próxima decisão

Antes de operar: revisar scripts em cópia nova, guardas de uso único, caminhos
canônicos sem links de escape, identidades SO+SQL e objetos envolvidos. Segredos
novos sob ACL restrita, capturas stdout/stderr binárias sanitizadas, evidências
intermediárias append-only, limite de armazenamento incluindo WAL. Nenhum acesso
ao PostgreSQL 5432, jogo, credenciais/saves reais ou ferramentas anteriores.

Parada: somente handles originais com identidade correspondente; PostgreSQL
próprio por pg_ctl fast. Confirmar ausência de processos/filhos/listeners e status
antes de remover somente dados/segredos canônicos explicitamente aprovados.
Identidade divergente ou handle perdido bloqueia ação e descarte; preservar
diagnóstico sem matar processo desconhecido.

Saída desejada: pacote revisado e limites aceitos para implementação isolada,
seguido de autorização operacional específica. A revisão atual não autoriza
nenhuma dessas ações. Consultar [critérios](ORG-03-E3-B-CRITERIOS-ACEITE.md),
[riscos](ORG-03-E3-B-MATRIZ-RISCOS.md) e [pendências](ORG-03-E3-B-PENDENCIAS.md).
