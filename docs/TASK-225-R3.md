# TASK-225-R3 — Continuidade segura do personagem

## Contexto e motivação

R2 validou retirada coordenada equivalente, mas a readmissão normal cria novo personagem efêmero. Investigar continuidade antes de ativar o prazo 32+60 de Quatro relâmpagos, sem perda silenciosa de progresso.

## Estado atual verificado

Execução iniciada em 03/11/2026, concluída após a virada para 04/11. Branch `beco`, HEAD inicial `6d72c65`; remoto `origin/beco` consultado com o mesmo HEAD. Checkout com alterações locais preexistentes e índice inicialmente vazio. Baseline R2: 1.013 casos aprovados. Monitor sem pendências declaradas. JDK 17/Maven e cache existentes; `server/pom.xml` é o módulo envolvido.

Save builtin, cópias de revisão e laboratório 193–196 não são recuperação completa de multiplayer. Atributos manuais, energia exata, autoria, cinco cooldowns e autoridade de rede exigem contratos próprios. Auditoria detalhada na [ADR](TASK-225-R3-ADR-CONTINUIDADE.md).

## Objetivo

Comparar A/B/C, recomendar um caminho seguro e exercitar B em protótipo sintético exclusivamente de teste, sem integrar persistência ao runtime.

## Escopo incluído

Auditoria de personagem/sessão/mundo/conexão; registro local com prova opaca, geração, envelope limitado/schema, expiração, consumo único, spawn e deadlines; testes negativos e regressão; proposta separada de negociação/autenticação.

## Fora do escopo

Contas/login público, persistência definitiva, alteração de saves, importador multiplayer, recuperação por TCP, timeout no engine, valores das skills, capacidade, ACK/trajetória, TASK-226, releases e publicação. Commit/push do recorte R3 foram autorizados diretamente pelo usuário, como exceção à restrição de Git do anexo.

## Arquivos/módulos envolvidos (confirmados)

Novos em `server/src/test/java/com/projetok/rpg/`: `Task225R3RecoveryLab.java` e `Task225R3RecoveryLabTest.java`. Quatro documentos `docs/TASK-225-R3*.md`; atualizações em `CORUJAO-IX-PROGRESSO.md` e `CORUJAO-IX-PENDENCIAS.md`. Evidências locais no checkpoint R3.

Auditados: `RpgSessionWorld`, `RpgSkillEngine`, `RpgWorld`, `SharedRpgEncounter`, `SessionSimulation`, `LocalSimulationServer`, `LocalClient`, `GameplayProtocol`, `InventoryService`, `QuestService`, `AuthoredQuestSession`, `QuestRewards`, `DomainProgression`, `RpgProgression`, `RpgSave`, `SyntheticSessionLaboratory`, `SyntheticOnlineRepository` e testes 193–196. Nenhuma alteração nesses fontes de produção.

## Pré-requisitos e dependências

Cache Maven offline e JDK já instalados. O protótipo usa tipos/APIs já presentes no HEAD, sem depender das classes locais de skills ainda não versionadas. Saves usados são fixtures em memória ou pastas `online-synthetic-*` sob `@TempDir`; nenhum save pessoal é aberto.

## Plano de implementação incremental

1. Preservar Git, fontes, docs e assets recebidos por hashes/cópias.
2. Auditar propriedade e lacunas; separar modelo de recuperação de restauração real.
3. Implementar envelope imutável e transição atômica local, com falhas controladas e prazo de teste.
4. Exercitar matriz positiva/negativa, concorrência real de dois threads e os replays reais builtin de 196.
5. Repetir focalizados, regressão completa e complemento gráfico; conferir imutabilidade e validar o recorte sobre HEAD isolado antes de subir ao Git.
6. Entregar ADR/relatórios e commit/push restritos à R3, sem incluir modificações de runtime anteriores.

## Critérios de aceite

Um vencedor por ticket, rejeição de prova/geração/contexto incorretos e expiração definida; preservação exata do snapshot autorizado e recargas sem reset; transitórios ausentes; recusa de spawn inválido sem consumo; falhas de pré/pós-commit sem reaplicação; declaração explícita de limites do modelo e requisitos futuros de protocolo.

## Plano de testes

Ver [registro de testes](TASK-225-R3-TESTES.md). Nenhum teste existente desabilitado. Novos casos com timeout JUnit de 15 segundos, esperas de concorrência limitadas e Maven com limite externo de 600 segundos. Sem instalações, banco ou Maven concorrente.

## Riscos e compatibilidade

O protótipo não instala estado no multiplayer, não persiste o envelope e não autentica peers. Cópia de DTO não homologa restauração de atributos/autoria/energia no engine. Protótipo de consumo único em memória não oferece exactly-once distribuído. A-04 continua aberto; recomendar B não autoriza sua implementação/ativação.

## Plano de rollback

Remover apenas os dois fontes de teste e quatro documentos R3; reverter somente os acréscimos R3 nos dois documentos do Corujão, conferindo trabalho posterior com o baseline. Após commit, reversão deve ser outro commit do recorte, nunca reset/force push. Sem formato pessoal/wire/banco para migrar.

## Entregáveis e relatório final

[ADR](TASK-225-R3-ADR-CONTINUIDADE.md), [testes](TASK-225-R3-TESTES.md) e [relatório](TASK-225-R3-RELATORIO-FINAL.md). A entrega prepara decisão do responsável; não encerra A-04 nem inicia TASK-226.
