# ORG-03-R7 — Relatório final

## Resultado

Preparação E1 entregue em 11/11/2026, branch `beco`, HEAD
`13b91dca20e16d6729d2dced79c1a4431b74ca09`. Foram criados os seis documentos
previstos e inserido um adendo de preparação E1 na organização principal.
Índice e contexto no [escopo R7](ORG-03-R7.md).

**E1 continua aguardando revisão/decisões do responsável. E2 não foi autorizado
nem executado.** A matriz R6 permanece byte a byte preservada, com 6 aprovações
anteriores, 13 recomendações, 11 pendências e 4 bloqueios por evidência.

## Conteúdo pronto para revisão

O [quadro E1](ORG-03-R7-QUADRO-APROVACAO-E1.md) cobre os nove campos pedidos em
cada D01–D34, com impactos, testes, gates e aceite delimitado. Há propostas
específicas para XP, moedas, missões, coleta, agregados completos e distribuição,
além de seis decisões de penalidade independentes e três alternativas de
settlement. A restrição A continua; migração C não foi autorizada.

O [escopo E2](ORG-03-R7-ESCOPO-E2.md) define cinco autorizações independentes,
dados exclusivamente sintéticos, quotas experimentais e passos futuros de
criação/teste/encerramento/descarte. Os [critérios](ORG-03-R7-CRITERIOS-E2.md)
trazem 16 casos mensuráveis, cobrindo as 12 falhas R6 prioritárias e recortes
sintéticos de resposta perdida, claim e distribuição. Todos continuam futuros.

## Ambiente observado e rota candidata

Windows 10 Pro 64 bits. PostgreSQL 18.6 instalado, binários fora do PATH mas
verificados por caminho absoluto e `--version`. Há serviço `postgresql-x64-18`
ativo e processo postgres na porta 5432, que **não foi acessado**. A porta
candidata 55432 não tinha listener no momento da inspeção; não foi reservada.
Docker/Podman não foram encontrados nas consultas, sem busca exaustiva do disco.

Proposta não aprovada: usar binários existentes com cluster novo próprio,
loopback, credenciais separadas e Python + psql, sem driver adicional ou POM.
Permissões, espaço, bind e inicialização do cluster ainda precisam ser
verificados depois de autorização. Detalhes no
[inventário/procedimento](ORG-03-R7-AMBIENTE-POSTGRESQL.md).

## Verificação e preservação

Checkpoint local em `temp/org03-r7/`: status/diff prévio do Git, cópia da
organização, manifesto SHA-256 de 9.878 arquivos e inventários JSON. O Git já
continha alterações/exclusões anteriores; elas não foram descartadas. Os oito
documentos R6 foram comparados com os hashes da sua entrega, sem divergência.

A conferência local `python temp/org03-r7/verify_r7.py` verifica documentos,
links/tabelas, cobertura/estados e preservação; resultados em
`temp/org03-r7/validation.json` e diff exclusivo R7 em
`temp/org03-r7/task-documents.diff`. Essa verificação é documental, não teste de
PostgreSQL, gameplay, autenticação ou arquitetura em operação.

Resultado final: **conferência documental aprovada**, seis entregáveis, 24 links
locais, 13 tabelas, 34 decisões com os nove campos e estados preservados,
16 critérios futuros, 12 falhas prioritárias cobertas e cinco autorizações
ainda não aprovadas. Dos 9.878 arquivos protegidos, **9.877 mantiveram SHA-256**;
somente a organização mudou, exclusivamente pela inserção R7. Nenhum arquivo
protegido ausente ou inesperadamente alterado; apenas os seis Markdown previstos
foram adicionados nas árvores protegidas.

A primeira rodada apontou uma suposição incorreta no próprio verificador sobre
o número de colunas da segunda tabela E1 (seis, não sete). O script foi corrigido
e a conferência final passou sem alterar estados/decisões. O resultado inicial
foi preservado em `temp/org03-r7/validation-initial.json`.

Não foram executados SQL, conexão a banco, initdb operacional, criação de
cluster/contas, instalação, migrations, Maven ou testes Java. Consultas
`--version` não executam os procedimentos E2. O baseline R5 de 1.196 aprovados e
57 excluídos continua histórico; não foi reapresentado como teste R7.

Preservados runtime, R1–R6, protocolos 6/7, 40 skills, inventário/economia,
cooldowns, assets, saves, launcher e World Studio no conjunto inventariado.
Sem autenticação/reconexão nova, A-04, TASK-226, ORG-04, commit, push ou publicação.

## O que falta para autorizar E2

1. Registrar a escolha individual de L01–L05 aplicáveis: criação dos artefatos
   experimentais; cluster/credenciais isolados; execução/limites; evidências;
   encerramento e descarte restritos. Responsável/data/ressalvas continuam vazios.
2. Aceitar ou ajustar os perfis sintéticos e valores experimentais. Isso não
   aprova escolhas definitivas de gameplay D14–D22/D34, legado ou tuning real.
3. Confirmar caminhos/porta/versão e responsabilidade pelos recursos novos.
   Se a rota existente não funcionar, qualquer instalação/alternativa exige
   proposta e autorização próprias; não há fallback ao serviço 5432.

Depois desses aceites ainda será necessário implementar/revisar harness e SQL
experimentais, comprovar alvo/permissões e executar os critérios. Crash real,
restart/restore/failover, RPO/RTO, lease e autoridade após rollback ficam E3;
owner, posse real, protocolo, relógios, efeitos e spawn ficam E4. D05/D07/D32
permanecem pendentes e D33 bloqueada. Nenhum aceite documental substitui prova.

## Reversão

Remover somente os seis documentos R7 e seu adendo, comparando com a cópia
anterior e preservando alterações posteriores. Não usar restauração global.
Não há banco/conta/save/runtime a reverter. Próximo passo sugerido: revisão do
pacote E1 pelo responsável; a R7 termina na preparação, sem iniciar E2.
