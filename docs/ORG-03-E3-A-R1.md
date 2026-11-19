# ORG-03-E3-A-R1 — Lifecycle do relay

Responsável: Lucas. Preparação em 15/11/2026. Branch `beco`, HEAD
`c3d484e76f17c2d93bf0c68ad3ff54280ee8ad39`.

## Contexto e estado verificado

A rodada `20261114-lucas-e3a-5fbb9a8beb` terminou com C01 PASSOU,
C02 FALHOU no harness e C03–C09 NÃO EXECUTADOS. Seus scripts, evidências
e seis documentos permanecem preservados. Os seis documentos E2 e seis E3
já estavam não versionados ao iniciar R1; diff rastreado vazio.

Foram lidos AGENTS.md, a skill local spiritgate-server-engineer, os seis
documentos E3-A e os seis E2, scripts E3, eventos, manifesto, falha, análise
C02 e registros finais E2. O monitor contém status histórico de 01/11,
sem pendentes declarados; isso não homologa os erros antigos do jogo.

## Objetivo e recorte

Corrigir a ambiguidade entre saída do processo original e identidade divergente,
com cópias isoladas e testes puros. Nenhuma execução de cluster, SQL,
crash, processo fixture real, socket real ou instalação está incluída.

## Implementação e arquivos

Em [tools/org03_e3a_r1](../tools/org03_e3a_r1):

- `lifecycle.py`: estados explícitos e encerramento serializado pelo objeto original.
- `lab_runtime.py`: cópia do original, integrando lifecycle e captura de stderr.
- `relay.py`: loop TCP extraído e instrumentado, exercitado com sockets simulados.
- `test_harness.py`: 23 testes determinísticos, incluindo o adaptador Lab/Relay.
- `*.diff`: diferenças integrais contra os dois originais.
- Manifestos antes/depois e resultados da validação.

Não se copiou `runtime.py`, runner SQL ou DDL. O adaptador depende do runtime
somente na futura integração; seus testes usam módulo inerte. O relay R1
recusa ativação por CLI. Este pacote é de correção/preparação e não é um
runner de rodada real. A conexão do loop instrumentado ao listener e a
parametrização de run-id futuro terão revisão estática antes da execução.

## Critérios e validação

Os 15 requisitos de teste foram exercitados com mocks. Saída confirmada pelo
handle não envia terminate. Ausência CIM com handle inconclusivo bloqueia.
Divergência não nula bloqueia mesmo se o handle encerrar depois da consulta.
Erro ou ausência de metadata não equivale a sucesso. Saída não zero ou saída
zero sem EOF esperado do cliente interrompe a lógica de cenário.

Ver [testes](ORG-03-E3-A-R1-TESTES-HARNESS.md),
[análise](ORG-03-E3-A-R1-ANALISE-RELAY.md),
[relatório](ORG-03-E3-A-R1-RELATORIO-FINAL.md) e
[proposta](ORG-03-E3-A-R1-PROPOSTA-REEXECUCAO.md).

## Compatibilidade e reversão

Nenhuma mudança em produção Java/POM, protocolos 6/7, habilidades, gameplay,
saves, contas, offline/Coop, HUD, assets, World Studio, launcher ou A-04.
Sem commit, push ou publicação. Reversão: retirar somente os cinco documentos
R1 e a pasta própria após revisão, preservando evidências e trabalho posterior;
nunca reset/clean. Não há dados ou migration a reverter.
