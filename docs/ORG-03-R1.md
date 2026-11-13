# ORG-03-R1 — Contrato recuperável e validação pura

## Contexto e motivação

10/11/2026. Implementação incremental autorizada a partir da ORG-03 aprovada
como auditoria/documentação. O contrato de dados não é recuperação funcional.

## Estado atual verificado

Branch `beco`, HEAD `13b91dc`. Git inicial: 69 entradas modificadas, 2429
excluídas e 392 não rastreadas, preservadas. Inventário anterior de 2343 arquivos
em `temp/org03-r1/before-sha256.json`; status e name-status também arquivados.
POM real: `server/pom.xml`, release Java17, JUnit5.11.4/Surefire3.5.2, sem
dependência de produção nova. Lidas matriz, ADR, contrato e plano da ORG-03.

## Objetivo

Representar estado candidato imutável, limitado e versionado e rejeitar
inconsistências sem executar gameplay, I/O, recompensa ou instalação parcial.

## Escopo incluído

Três arquivos Java novos em `com.projetok.rpg.recovery`, um teste JUnit novo,
quatro documentos e adendo na organização principal. Nenhuma hierarquia de
Player nova. Records e enums descrevem valores; não há serialização Java nativa.

## Fora do escopo

Captura/importação no owner, personagem ativo, autenticação, claim/lease/fence,
worker, persistência, relógio durável, SQL, dependências, protocolo, política
de efeitos, timeout/desconexão de A-04 e qualquer edição dos modelos existentes.
Não iniciar ORG-04, PostgreSQL ou TASK-226. Sem commit/push/release/publicação.

## Arquivos/módulos envolvidos (confirmados)

Relativos a `server/src/main/java/com/projetok/rpg/recovery/`:

- `RecoverableCharacterSnapshot.java`: DTO principal e records/enums aninhados.
- `SnapshotValidationContext.java`: evidência declarativa e catálogos pré-carregados.
- `CharacterSnapshotValidator.java`: validação com violações e pendências.

Teste: `server/src/test/java/com/projetok/rpg/recovery/CharacterSnapshotValidatorTest.java`.
Catálogos/regras existentes usados sem alteração: CharacterIdentity, CharacterRaces,
DomainProgression, Domains, RpgProgression, ItemCatalog, InventoryService,
QuestService.Definition, RpgSkillCatalog, StatsComponent, EnergyRules e LevelingRules.

## Pré-requisitos e dependências

O chamador deve carregar catálogos antes da validação. `RpgProgression.builtin`,
ItemCatalog/QuestService construídos a partir de recursos e `DomainProgression.validateFor`
não são chamados dentro de validate: fazem/levariam a leitura de recursos.
Usam-se instâncias já carregadas e funções puras. Não se chama RpgWorld.

## Plano de implementação incremental executado

1. Preservação, auditoria dos contratos e seleção de funções puras.
2. Records imutáveis com cópias e limites; null preservado como ausência explícita.
3. Contexto injetado e resultado estruturado, sem aprovação de ativação.
4. Testes novos, regressões dirigidas e suíte geral headless documentada.
5. Conferência de hashes, evidências regeneradas e limites pendentes.

## Critérios de aceite

Imutabilidade, rejeições estruturais testadas, ausência de dependência de UI/
socket/banco/mundo vivo, validação sem efeitos externos, limites e lacunas
documentados e código anterior preservado. `structurallyValid()` não equivale
a pronto para recuperação; `readyForActivation()` retorna false nesta entrega.

## Plano de testes

47 novos casos, mais os 87 dirigidos anteriores, aprovados. Matriz dos 30
requisitos em [ORG-03-R1-MATRIZ-VALIDACOES.md](ORG-03-R1-MATRIZ-VALIDACOES.md).
Resultados gerais e comandos em [ORG-03-R1-TESTES.md](ORG-03-R1-TESTES.md).
Fixtures determinísticas; nenhum save pessoal ou conta real.

## Riscos e compatibilidade

Estrutura válida não demonstra que XP/itens foram legitimamente concedidos,
que um recibo foi confirmado em banco ou que alguém possui o personagem.
Contexto injetado não se autentica sozinho. Posição válida em limites não
significa spawn desocupado. Clock estruturalmente descrito não é relógio durável.
Não conectar o modelo sem aprovação e testes dessas fronteiras.

## Plano de rollback

Remover exclusivamente os três arquivos Java novos, o teste novo, quatro docs
e adendo R1 no principal. Não reverter arquivos de produção existentes nem
alterações anteriores. Cópia do principal antes da R1 em
`temp/org03-r1/ORGANIZACAO-before.md`. Preservar evidências; sem reset/clean.

## Entregáveis e relatório final

[Matriz de validações](ORG-03-R1-MATRIZ-VALIDACOES.md),
[testes](ORG-03-R1-TESTES.md), [relatório final](ORG-03-R1-RELATORIO-FINAL.md)
e adendo no documento principal. Integração futura depende de autorização própria.
