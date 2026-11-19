# ORG-03-E3-A — Relatório técnico da rodada

## Resultado

Responsável **Lucas**, **14/11/2026**. Run-id `20261114-lucas-e3a-5fbb9a8beb`.
Branch `beco`, HEAD `c3d484e76f17c2d93bf0c68ad3ff54280ee8ad39`.
Autorizações E3A-01–05/quotas recebidas; nome confirmado antes da execução.

**C01 PASSOU; C02 FALHOU no harness; C03–C09 NÃO EXECUTADOS. E3-A não aprovado.**
A rodada usou um cluster sintético e terminou em 28,155 s, incluindo limpeza.
Não houve crash PostgreSQL, reinício ou recuperação após queda.
Parada fast, ausência dos processos/listeners e descarte próprios comprovados.

## Evidências e diagnóstico

C01 encerrou cliente real antes do COMMIT. Novo cliente consultou o mesmo ID
após término do backend; NOT_FOUND e imagens completas iguais demonstraram
ausência de efeito/recibo/índices parciais no caso testado. Evidência inclui
PIDs distintos, transação 798 e EOF com transação aberta no log do PostgreSQL.

C02 confirmou uma operação no observador enquanto a resposta ficou retida
no relay TCP (18 bytes, encaminhamento servidor→cliente sem avanço). Após
encerrar o cliente identificado, a asserção de identidade do relay falhou
antes do kill e interrompeu a rodada. Não houve reconciliação/retry por novo
worker; o caso não conta como aprovação.

O relay foi observado encerrado na limpeza, exit_code=1. Há hipótese de corrida
entre saída espontânea após EOF/reset, poll e consulta CIM. O valor retornado
pela inspeção e stderr não foram gravados; a causa transitória exata não é
afirmada como comprovada. Classificação: **falha do harness**, sem evidência
de falha do contrato econômico. Não houve comando alternativo contra o relay.

[Resultados brutos](../temp/org03-e3a/20261114-lucas-e3a-5fbb9a8beb/evidence/results.json),
[matriz](ORG-03-E3-A-MATRIZ-FALHAS.md),
[análise da falha](../temp/org03-e3a/20261114-lucas-e3a-5fbb9a8beb/evidence/ANALISE-FALHA-C02.md).

## Consumo e preservação

Um initdb/cluster, zero immediate, zero reinícios, duas intenções/uma aplicação
cada, quatro instâncias psql (até duas simultâneas) e um relay. Pico medido
42.901.104 bytes (~40,91 MiB), abaixo de 256 MiB. Logs/evidências ficaram abaixo
de 32 MiB. fsync/synchronous_commit/full_page_writes medidos on, sem isso
ser anunciado como prova de durabilidade após crash.

Encerramento: postmaster 28104 e filhos ausentes, todos os clientes/relay
registrados ausentes, pg_ctl status 3, portas 55433/55434 sem listener;
data/secrets removidos somente após essas provas.
[shutdown.json](../temp/org03-e3a/20261114-lucas-e3a-5fbb9a8beb/evidence/shutdown.json).
Sem consultas ou interrupções dirigidas à porta 5432 ou serviço existente.

Os 3.951 arquivos anteriores protegidos, três scripts finais e 250 entradas
do manifesto E2 são conferidos por SHA-256 na entrega. Scripts executados E3
continuam iguais ao source-sha256. Esse inventário não cobre todos os arquivos
pessoais/ignorados. Evidências temp são locais e ignoradas pelo Git, sem upload.
[Validação da entrega](../temp/org03-e3a/20261114-lucas-e3a-5fbb9a8beb/evidence/delivery-validation.json).

## Alterações e limites

Seis documentos E3 atualizados para refletir a rodada, scripts/DDL experimentais
e evidências apenas no run-id aprovado, mais registro local de análise em
logs/analises/20261114-org03-e3a-c02.md. Versões documentais anteriores ao resultado
foram preservadas. Nenhum script/evidência E2 alterado.

Não houve alteração no jogo/server/src/main/POM, protocolos 6/7, 40 skills,
economia/gameplay, saves/contas, offline/Coop, assets, HUD, World Studio,
launcher ou A-04. Sem Maven/Java, teste físico/Radmin/launcher, migração,
integração, commit/push/release/publicação, E3-B/E4/E5/ORG-04/TASK-226.

Não demonstrados: recuperação PostgreSQL após crash, retry completo de C02,
fence/snapshot/histórico, distribuição/dívidas após restart, integridade SQL
global final, backup/restore/failover, D33, relógio durável, RPO/RTO de produção,
revogação global, persistência ou recuperação integral de jogadores reais.

## Reversão e próximo passo

Não há banco ativo ou migration a reverter. Remover apenas os seis documentos
novos e os artefatos próprios desejados após revisão, preservando evidências
e alterações posteriores; sem reset/clean. O próximo passo técnico proposto
é corrigir/diagnosticar o lifecycle do relay em cópia futura e testar sua saída.
Uma nova rodada com cluster/run-id novos depende de autorização adicional:
o limite de um cluster desta rodada foi consumido. Não foi ampliado para aprovar testes.
