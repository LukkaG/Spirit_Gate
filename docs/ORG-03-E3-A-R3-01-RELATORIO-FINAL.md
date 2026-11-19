# ORG-03-E3-A-R3-01 — Relatório da preparação

Responsável: Lucas. Data: 15/11/2026. Branch `beco`, HEAD
`c3d484e76f17c2d93bf0c68ad3ff54280ee8ad39`, preservados.
**Preparação integrada e validada por testes determinísticos. Execução operacional bloqueada.**
**Somente R3-01 autorizado; R3-02–05 pendentes. E3-A não aprovado.**

## Pacote preparado

Run-id reservado, ainda não consumido:
`20261115-lucas-e3a-r3-9b62f13a70`.
Caminho absoluto:
`C:/Users/lucas/Documents/GitHub/Projeto-K/temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70`.

Foram criados somente scripts, aceite de preparação e evidências da preparação.
Não existem `data`, `secrets`, `execution.claim`, `evidence` operacional nem
`operational-authorization.json`. O claim será exclusivo e não poderá ser removido
para repetir uma rodada. Mudança da data de execução exige novo run-id/caminho,
revisão da parametrização e registro do aceite correspondente antes de iniciar.

**Portas candidatas futuras: 127.0.0.1:55435 para PostgreSQL e 55436 para relay.**
A proposta anterior citava 55433/55434; a instrução atual proibiu as portas
anteriores. A configuração nova respeita essa instrução. Nenhuma porta foi
aberta, sondada ou declarada disponível nesta tarefa. Sem fallback.

## Arquivos e diferenças

Fontes em `scripts/` do novo run-id; ferramentas de preparação/validação em
`tools/org03_e3a_r3/`. O pacote não depende dos diretórios antigos em execução.

| Componente | Origem e integração |
| --- | --- |
| `config.py` | Novo: run-id/caminhos/portas/quotas fixos, aceites operacionais separados, data e claim vinculados ao controlador. |
| `runtime_base.py` / `runtime.py` | Cópia R2 com portas novas e guardas antes de comando/start/SQL. Protocolo Session escreve bytes; criação/fechamento da sessão texto desabilitados. |
| `lab_base.py` | Identidade SQL/SO, stop/recovery, quotas e descarte R2 preservados; portas e autorização parametrizadas. Worker/Relay antigos retirados dessa cópia ativa. |
| `lab_runtime.py` | Worker binário novo; Relay binário com endpoint aceito, ACK privado e captura P1; registro exato de handles, processos parcialmente registrados bloqueiam limpeza. |
| `binary_io.py` | Dreno stdout UTF-8 estrito, EOF obrigatório, erro compartilhado e limites de linha/fluxo/fila. |
| `relay_entry.py` | Entrada nova protegida antes de socket/thread; listener/upstream fixos novos; protocolo JSON binário limitado. Erro do leitor privado invalida o pump. |
| `lifecycle.py`, `transport.py`, `relay.py`, `c02_integration.py` | Cópias P1 byte a byte, sem alteração nas regras aprovadas. |
| `legacy/` | Quatro fontes históricas exatas para os 23 testes e a classe OwnedProcess original. Sem uso operacional dos adaptadores legados. |
| `runner_base.py` | Suite R2 copiada sem o main antigo; normalização do rótulo ECONOMIC_UNCERTAIN. Oráculos econômicos preservados. |
| `runner.py` | TransportIO concreto, C02 integrado ao adaptador P1, C05 separado, caminho lost legado recusado, gate global e entrada de uso único. |
| `schema.sql` | Idêntico ao schema experimental R2. Nenhuma alteração econômica/DDL. |
| Testes | 23 históricos + 48 P1 preservados; 46 testes novos do runner/integração. |

O [manifesto de origens](../temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/preparation/source-origins.json)
contém origem/hash/igualdade por arquivo. O
[diff integral](../temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/preparation/integration.diff)
mostra as diferenças contra R2/P1, inclusive os arquivos novos.
`prepare.py` registra a cópia inicial/baseline uma única vez; o pacote final
inclui as integrações posteriores e é conferido pelo manifesto, não por uma
reexecução do gerador sobre o mesmo run-id.

## Ligação efetiva do transporte

`runner.Suite` herda os oráculos de `runner_base.Suite`; seus factories open/
replace apontam para o Worker binário novo. `TransportIO` implementa todos os
hooks que eram injetados na P1: criar relay/worker identificados, obter a guarda
por objeto Popen, persistir UNCERTAIN, observar resultado, verificar socket,
armar canal privado, aguardar pipes/handle e persistir a reconciliação.

O relay informa connection_id e peer aceito. O controlador verifica identidade
do worker/relay e OwningProcess do socket estabelecido com esse endpoint. A
observação RUNNING_MATCH mais recente e PROCESS_TERMINATE são produzidos pela
guarda R1; só então o permit é enviado, e seu ACK/hash deve anteceder terminate.
Permits de outro cliente, conexão, fase, OperationId ou cenário são recusados.
Nenhuma divergência de identidade foi convertida em desconexão esperada.

C02 persiste ECONOMIC_UNCERTAIN após dispatch e antes do corte, usa observador
independente, snapshot pós-commit e contadores retidos, encerra o cliente,
valida transporte/processo/captura e cria worker novo. Só aprova o caso após
mesma intenção/OperationId, recibo histórico igual ao observado e ao retry,
imagem integral inalterada e incremento econômico único. O transporte concluído
mantém ECONOMIC_UNCERTAIN/NOT_EVALUATED até essa reconciliação.

C05 usa seu próprio relay/permit e não consulta resultado econômico antes do
corte: espera apenas os contadores de retenção. Reconcilia depois de fim do
backend e worker novo. C09 mantém três fronteiras próprias sem criar relay;
não reutiliza prova C02/C05. O caminho lost antigo está explicitamente bloqueado.
O limite é duas instâncias de relay, sem terceiro oculto.

## Captura, encerramento e descarte

Stdin/stdout/stderr dos workers e relay são binários. Stdout tem protocolo
estrito: linha 16 KiB no relay, 256 KiB no worker, até 4 MiB lidos por processo
e fila de até 64 linhas; limite/erro/EOF ausente invalida a captura. ACCEPTED
é estado separado e não consome o ACK PAUSE. Stderr usa a implementação P1
intacta: prefixo/texto 64 KiB, blocos 4 KiB, hash/contagem, substituições
explícitas e nenhuma persistência bruta de bytes, SQL ou segredos.
Após receber o evento terminal, o controlador fecha stdin privado antes do wait;
o filho exige join do leitor de controle antes de sair. EOF do controle antes
do evento terminal permanece erro de protocolo.

Na saída por falha, o controlador não depende do stdin de um psql esperando a
resposta retida: encerra os workers registrados pela guarda original e drena
seus pipes. Erro de captura é registrado e continua falha, mesmo quando a parada
segura é concluída. O monitor precisa terminar antes do descarte. Handles
parcialmente registrados ou identidade bloqueada conservam recursos, sem kill
alternativo. Relay já encerrado não recebe terminate adicional.

O stop/recovery herdado mantém identidade completa antes de pg_ctl, até dois
immediate e quatro reinícios no mesmo data. Finish exige status 3, ausência de
postmaster.pid, PIDs/filhos/handles/listeners e nenhuma identidade bloqueada,
antes de remover somente data/secrets canônicos filhos diretos, sem reparse
points. Esses caminhos foram testados com dependências inertes; **nenhuma parada
ou remoção operacional foi executada nesta tarefa**.

## Validação realizada

**117 testes Python PASSOU:** 23 históricos, 48 P1 e 46 de integração.
Sintaxe de 19 módulos conferida; importações/conexões entre módulos exercitadas
sob guardas que proíbem socket, conexão, subprocesso operacional e remoção real.
As suítes são executadas em processos Python próprios de teste, sem worker,
relay, psql ou pg_ctl reais. Os testes de protocolo usam BytesIO/Mocks.

Comando efetivamente executado:

```powershell
python -B tools/org03_e3a_r3/validate.py
```

[Validação estruturada](../temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/preparation/validation.json),
[cobertura e resultados](ORG-03-E3-A-R3-01-TESTES.md).
Um comando auxiliar de edição falhou por aspas do PowerShell antes de aplicar
a transformação; ela foi corrigida com patch e coberta pelo teste binário.
Isso não iniciou nenhum recurso nem alterou originais; não foi contado como
teste SQL ou cenário executado.

**SHA-256 antes/depois de 4.147 arquivos anteriores sem diferenças.** Inclui
rastreados, laboratórios E2/E3/R1/R2, P1 e documentos anteriores, inclusive a
proposta R3 original. Hashes completos em `preparation/preservation-before.json`
e `preservation-after.json`; fontes novas em `source-sha256.json`.
O manifesto cobre os arquivos enumerados, não todo arquivo pessoal ignorado.

## Limites e prontidão exata

Tetos futuros: um cluster; dois crashes; quatro reinícios; oito personagens/
dois proprietários; 64 intenções; três tentativas; payload 16 KiB; três workers
simultâneos/32 totais; quatro conexões SQL; um relay simultâneo/duas instâncias.
120 s/caso e 1.200 s totais; stop 20 s, start/recovery 45 s, CIM 8 s, wait 5 s.
256 MiB totais, incluindo 32 MiB de evidências; mínimo inicial 2 GiB livres.
Os limites adicionais de canais acima são tetos de falha, nunca expansão.
Medição periódica de disco não equivale a quota física do filesystem.

**Estado: PREPARADO_PARA_REVISAO_HUMANA; OPERACAO_BLOQUEADA.** As dependências
P1 agora estão ligadas ao runner concreto; não há hook abstrato pendente para
C02/C05. Isso não é homologação em PostgreSQL. Permanecem pendentes:

- Revisão humana deste pacote/diff e aceites R3-02–05, com as portas novas.
- Confirmação da data/run-id/caminhos e manifesto operacional separado.
- Preflight real: disponibilidade/bind exclusivo, binários existentes e seus
  hashes/versões, espaço, SID/ACL, ambiente PG e isolamento SQL/SO antes de DDL.
- Execução real C01–C09, controles finais, crash/recovery e descarte, todos
  **NÃO EXECUTADOS**. Fixtures TCP reais também **NÃO REEXECUTADAS** no R3-01.

Riscos remanescentes: concorrência Windows/EOF/reset reais com psql, timing de
CIM/metadados TCP, limites efetivos de saída, recuperação PostgreSQL/hardware
e eventual conservação de recursos na dúvida de identidade. A prova de P1
continua histórica; os testes novos não a substituem por homologação SQL.
Uma falha obrigatória ou controle inconclusivo impede gate, mesmo com nove
rótulos PASSOU. Falha de encerramento também impede gate global.

Sem alteração de produção/Java/POM/gameplay, protocolo, saves, contas, ferramentas
antigas ou 5432; sem instalação, commit, push, reset/clean, publicação ou gates
posteriores. Nenhum acesso ao PostgreSQL/portas anteriores foi realizado.
E2/E3-A/R1/R2 continuam com seus resultados originais; E3-A segue não aprovado.

Os aceites pendentes estão [separados para revisão](ORG-03-E3-A-R3-01-ACEITES-OPERACIONAIS.md).
Nenhuma permissão operacional foi presumida. Reversão: revisar/retirar somente
as adições R3-01, preservando evidências e trabalho posterior; não há banco ativo
a reverter nem motivo para reset/clean.
