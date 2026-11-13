# TASK-223 — Auditoria das 40 habilidades

## Contexto e motivação

Primeira etapa exclusiva do Corujão IX, conforme o plano diretor recebido em 02/11/2026. O guia ilustrado é uma referência da implementação local, não evidência suficiente de mecânica ou homologação multiplayer.

## Estado atual verificado

Branch `beco`, HEAD `6d72c65`. Estado inicial: 54 modificados, 2.432 excluídos e 302 entradas não rastreadas. Nenhum desses arquivos foi descartado. Não havia documentos TASK-223 a TASK-232 nem Corujão IX no inventário de documentação consultado.

O catálogo tem 40 IDs executáveis em oito kits e dez aliases de domínio. A extensão atual negocia aplicação 7 sobre o envelope existente; não deve ser confundida com os modos históricos V6. JDK 17 e Maven foram confirmados pelo projeto/ambiente. Existe um `pom.xml` em `server/`, sem agregador multimódulo necessário para esta validação.

## Objetivo

Estabelecer uma referência verificável de catálogo, execução, apresentação, protocolo e testes, classificar as skills e propor as próximas etapas sem alterar o jogo.

## Escopo incluído

- Inspeção do código, guia PDF e fontes auxiliares de documentação.
- Matriz individual das 40 habilidades, com valores reais, assets resolvidos, implementação e evidência de testes.
- Reexecução dos testes existentes, inclusive TCP local e complemento gráfico.
- Registro de divergências, limites e prioridades do Corujão IX.
- Checkpoint e comprovação de integridade dos arquivos protegidos.

## Fora do escopo

Implementar TASK-224 a TASK-232; corrigir gameplay/HUD/protocolo; mudar balanceamento; criar arte; atualizar o PDF; instalar dependências; gerar build privada ou publicar.

## Arquivos/módulos envolvidos (confirmados)

- `server/src/main/java/com/projetok/rpg/`: RpgSkillCatalog, RpgSkillDescriptions, RpgSkillEngine, RpgWorld, SharedRpgEncounter, DomainProgression e Domains.
- `server/src/main/java/com/projetok/multiplayer/`: RpgSessionWorld, SkillProtocol, Protocol, LocalClient, NetworkSkillControls, NetworkSkillPresentation, RpgNetworkClientLauncher, RemoteTimeline e GameInputRouter.
- Movimento/colisão em `server/src/main/java/com/projetok/domain/movement/`; combate/AGI em `domain/combat/`.
- `assets/skills/manifest.properties` e overlays `refined-v2` a `refined-v8`.
- `server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java` e testes multiplayer de apresentação, input, protocolo, deslocamento e ciclo de vida.
- Guia `docs/Spirit_Gate_Guia_Ilustrado_das_40_Skills.pdf`, HTML correspondente e `docs/skills-report/catalog.xml`.

## Pré-requisitos e dependências

JDK 17, cache Maven existente `.maven-repository` e ambiente gráfico local para Swing. Nenhuma instalação foi feita. Skill aplicada: [spiritgate-server-engineer](../.agents/skills/spiritgate-server-engineer/SKILL.md), para leitura de contratos e validação autoritativa.

## Plano de implementação incremental

1. Congelar referências de Git, hashes, fontes e evidências anteriores no checkpoint.
2. Mapear contratos do servidor, cliente, assets e formas de conjuração.
3. Extrair definições compiladas com uma sonda de leitura isolada no checkpoint.
4. Conferir PDF, catálogo e semântica dos contadores; mapear testes reais.
5. Rodar a suíte e o complemento gráfico, arquivar resultados e restaurar artefatos históricos gerados pelos testes.
6. Entregar matriz, achados e plano de sequência para aprovação. Encerrar a TASK-223 sem iniciar a próxima.

## Critérios de aceite

Quarenta fichas completas; referências de código verificáveis; distinção entre teste, inspeção e hipótese; preservação do baseline; plano técnico das próximas etapas e nenhuma alteração de comportamento ou valor.

## Plano de testes

Comandos e resultados em [TASK-223-TESTES.md](TASK-223-TESTES.md). As 40 execuções da fábrica não substituem todos os cenários específicos de cada skill; essa limitação está explícita na matriz.

## Riscos e compatibilidade

Achados A-01 a A-10 no relatório final. O estado atual tem mecanismos autoritativos e testes, mas ainda carece de algumas provas de ciclo de vida extremo e feedback de UI. Não foi reproduzida falha crítica nesta rodada. Radmin físico não foi executado.

## Plano de rollback

Esta entrega acrescenta somente documentação e sondas dentro do checkpoint. Para reverter a entrega, retirar individualmente os oito documentos do Corujão IX/TASK-223 após conferir alterações posteriores. Não restaurar fontes do baseline, pois não foram modificadas nesta auditoria. Não usar reset/clean.

## Entregáveis e relatório final

[Matriz](CORUJAO-IX-MATRIZ-40-SKILLS.md), [testes](TASK-223-TESTES.md), [relatório](TASK-223-RELATORIO-FINAL.md), plano, progresso, pendências e fechamento parcial do Corujão IX. Checkpoint: `.task-checkpoints/CORUJAO-IX/TASK-223/`.
