# ORG-03-E2 — Relatório final

## Resultado

Execução de referência: `20261114-lucas-600aa9e066`, em 14/11/2026. Responsável: Lucas.
Branch `beco`; HEAD `c3d484e76f17c2d93bf0c68ad3ff54280ee8ad39`.
Autorização explícita L01–L05 recebida nesta conversa, com seis ressalvas;
registro local em [authorization.json](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/authorization.json).
As políticas definitivas D14–D22, D27 e D34 não foram aprovadas por este aceite.

**C01–C16 passaram no laboratório PostgreSQL real, exclusivamente sintético.**
Há evidência técnica para revisão humana de E2; aprovação humana do gate e
garantias de gates posteriores não foram presumidas.
Cluster próprio encerrado, PID ausente, porta 55432 sem listener e diretórios
de dados/segredos removidos. Evidências sanitizadas e scripts ficaram locais.

## Ambiente e autorização efetivos

Lucas autorizou L01 harness/DDL/fixtures, L02 cluster isolado, L03 testes/limites,
L04 evidências sanitizadas e L05 encerramento/descarte próprios em 14/11/2026.
Foram mantidas as seis ressalvas: não acessar/interromper 5432; preservar jogo/
saves/contas/configurações; usar só porta/caminho aceitos; interromper em risco
de porta/permissão/identidade; descartar só recursos próprios preservando
evidências; não avançar a E3/E4/integração/gameplay.

Windows/PowerShell, Python 3.14.5, binários locais PostgreSQL 18.6 revalidados.
Nenhum driver/JDBC/Docker instalado. Cluster novo UTF-8/locale C, loopback,
SCRAM, roles e database próprios; [isolamento](ORG-03-E2-ISOLAMENTO.md).

## Execução final e garantias SQL

16 critérios PASSOU; 200 intenções distintas, 137
recibos = 137 operações = 137 grants.
Zero operações órfãs, cabeças órfãs ou transações penduradas na consulta final.
Soma de EconomicRevision das oito cabeças = 137,
igual aos recibos; a diferença de StateRevision inclui o checkpoint sem economia.

Concorrência real: 20 pares de revisão, 20 de deduplicação, claim concorrente,
10 deadlocks 40P01 e 10 serialization failures 40001; retry integral idêntico
em duas tentativas, sem conceder duas vezes. Atomicidade comprovada por
rollback SQL e leitura observadora para versão/recibo/índices/recursos.
Histórico ausente/arquivo indisponível não foi promovido a ausência definitiva;
obrigação antiga permaneceu liquidável; snapshot antigo não sobrescreveu
economia nova. Representação temporal exata não equivale a clock durável.

Prazo após bootstrap, incluindo encerramento: 24.24 s; pico observado
44,199,486 bytes (~42.15 MiB), abaixo de 512 MiB.
[Matriz por caso](ORG-03-E2-MATRIZ-RESULTADOS.md) e
[testes/comandos](ORG-03-E2-TESTES.md).

## Falhas intermediárias e evolução do harness

| Execução independente | Resultado e tratamento |
| --- | --- |
| [20261114-lucas-4540923ff3](../temp/org03-e2/20261114-lucas-4540923ff3/evidence/results.json) | Falha antes do DDL/testes: pg_ctl/postmaster herdou pipe, impedindo EOF da captura no Windows. Harness próprio PID 21976 foi identificado pela linha de comando e encerrado; identidade SQL do cluster foi conferida e parada ordenada comprovada. A captura de startup passou a arquivo. O runtime desta tentativa recebeu essa correção para permitir recuperação; o código executado inicialmente difere nesse ponto, registrado em startup-failure.json. |
| [20261114-lucas-31007fc3ec](../temp/org03-e2/20261114-lucas-31007fc3ec/evidence/results.json) | Schema criado; C01 FALHOU ao abrir sessão: consulta de PID sem ponto e vírgula. Outros critérios NÃO EXECUTADO. Corrigido delimitador no próximo run-id. |
| [20261114-lucas-2b3a61a9e8](../temp/org03-e2/20261114-lucas-2b3a61a9e8/evidence/results.json) | C01 FALHOU no parser do harness: json_agg devolveu JSON multilinha e só a última linha foi lida. Outros critérios NÃO EXECUTADO. Corrigida junção das linhas no próximo run-id. |
| [20261114-lucas-22c60f5ac6](../temp/org03-e2/20261114-lucas-22c60f5ac6/evidence/results.json) | C01–C16 PASSOU; 132 recibos. Rodada preservada, sem somar contagens à execução final. |
| [20261114-lucas-38e6b25bae](../temp/org03-e2/20261114-lucas-38e6b25bae/evidence/results.json) | C01–C16 PASSOU; 137 recibos. Fronteiras econômicas, estado intermediário de dívida e codec reforçados após revisão de cobertura. |
| [20261114-lucas-2bbf642513](../temp/org03-e2/20261114-lucas-2bbf642513/evidence/results.json) | C01–C16 PASSOU nas provas estruturadas; 137 recibos. Guarda de uso único aprovada. Validação posterior do transcript FALHOU: gravação concorrente sem lock perdeu três registros de deadlock; relatório não fechado nessa rodada. Mantidos C15.json, transcript e report-validation-failure.json. |
| [20261114-lucas-600aa9e066](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/results.json) | C01–C16 PASSOU; 137 recibos. Transcript serializado por lock do harness; conferência cruzada com dez deadlocks/dez serialization failures. Guarda de uso único aprovada. |

Cada tentativa usou cluster/diretório novos; nenhuma reutilizou dados.
As três primeiras falhas foram do harness, não defeitos do Spirit Gate.
Os limites não foram ampliados para obter aprovação. Até três tentativas se
referem a retry técnico da mesma intenção, não à contagem de execuções novas
do harness; nos casos SQL elegíveis ocorreram duas tentativas por intenção.
Todas as tentativas têm shutdown.json; nenhum dado/segredo remanesce nelas.

## Preservação e entregáveis

3,690 arquivos versionados anteriores conferidos por SHA-256,
zero alterados ou ausentes. R1–R7, matriz de decisões, laboratórios anteriores,
fontes/testes Java, POMs, assets e ferramentas versionados permaneceram intactos.
O manifesto cobre arquivos versionados; não é inventário integral de arquivos
ignorados/pessoais. Não foram feitos writes em saves, bancos existentes ou
laboratórios anteriores. Nenhum documento antigo precisou de adendo/checkpoint.

Foram adicionados exclusivamente seis relatórios Markdown em docs e os
scripts/DDL/evidências dentro de temp/org03-e2 dos run-ids acima. Os artefatos
em temp são ignorados pelo Git e precisam permanecer disponíveis para revisão
local; não houve publicação. [Validação da entrega](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/delivery-validation.json)
e [manifesto final](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/artifact-sha256.json).

Não se executou Maven/Java, teste físico/Radmin/launcher ou integração com o jogo.
Protocolos, offline/Coop, gameplay, 40 skills, HUD, World Studio e launcher não
receberam alterações. Sem commit, push, release, publicação, ORG-04 ou TASK-226.

## Pendências e reversão

Crash real, restart/restore/failover, RPO/RTO, autoridade após restore, posse
autenticada, retirada de owner e continuidade temporal continuam não
demonstrados. [Pendências E3/E4](ORG-03-E2-PENDENCIAS-E3.md).

Não há banco ativo ou migração a reverter: o descarte autorizado foi concluído.
Reversão documental consiste em remover apenas os seis documentos novos,
preservando alterações posteriores e evidências desejadas; sem reset/clean.
Próximo passo sugerido: revisão humana do pacote E2. Nenhum gate posterior
foi iniciado e uma eventual autorização E3 deve ser específica.
