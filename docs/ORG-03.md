# ORG-03 — Arquitetura de Player e estado recuperável

## Contexto e motivação

Entrega inicial de auditoria e proposta, em 09/11/2026. Separar personagem,
participação e conexão antes de prometer progresso permanente. A ORG-02-R2
encerrou a investigação no limite, sem causa de foco demonstrada; sua suíte
geral de 1068 casos é baseline anterior, não teste executado nesta etapa.

## Estado atual verificado

Branch `beco`, HEAD `13b91dc`. Git inicial: 69 entradas modificadas, 2429
excluídas e 385 não rastreadas, anteriores a esta entrega. A lista integral
está em `temp/org03/git-status-before.txt`; nenhuma foi descartada.
Java 17.0.20.1, Maven 3.10.0, único POM encontrado em `server/pom.xml`.
O POM declara JUnit em teste e não declara driver JDBC de produção.

Há dois modelos: `domain/entity/Player` com componentes/conexão e `rpg/RpgWorld`
com identidade, inventário e progressão próprios. `RpgSessionWorld` usa o segundo
com um `GameEntity` proxy, não instancia o primeiro. `PlayerFactory` aparece
em Coop, debug e demo; trocar seu papel não migraria o multiplayer RPG.

O UUID da admissão RPG deriva de `instance + ':' + id` e muda na readmissão.
`LocalClient.reconnect()` refaz o handshake com a escolha anterior; não recupera
um personagem. Saves locais V2, repositório sintético e laboratório R3 têm
escopos diferentes. Nenhum deles já constitui persistência online completa.

## Objetivo

Entregar inventário verificável, fronteiras de responsabilidade e contratos
propostos de recuperação, idempotência e persistência futura em PostgreSQL próprio.

## Escopo incluído

- Auditoria dos 13 componentes solicitados e contratos dependentes reais.
- Matriz por campo, com origem, validação, mutabilidade, escopo e recuperação.
- ADR de identidade/reconexão e contrato de snapshot independente de rede/UI.
- Política proposta de recursos/recargas e exclusão de execução temporária.
- Plano conceitual de armazenamento, concorrência e indisponibilidade.
- Validação dirigida existente e evidência de preservação por hashes.

## Fora do escopo

Sem Java novo ou alterado, DTO executável, contas/tokens, importador no runtime,
recuperação automática, SQL/DDL/migration/JDBC/pool, ligação ao PostgreSQL,
mudança wire 6/7 ou ativação de A-04. Não iniciar ORG-04/TASK-226.

## Arquivos/módulos envolvidos (confirmados)

Todos os fontes abaixo são relativos a `server/src/main/java/com/projetok/`:

| Área | Componentes analisados |
| --- | --- |
| Entidade por componentes | `domain/entity/Player`, `GameEntity`, `domain/factory/PlayerFactory`, `domain/entity/component/{Stats,Health,Stamina,Inventory,Equipment}Component`, `domain/network/PlayerConnection` |
| Personagem RPG | `rpg/CharacterIdentity`, `RpgWorld`, `CharacterRaces`, `DomainProgression`, `RpgProgression`, `InventoryService`, `ItemCatalog`, `LootService` |
| Missões | `rpg/QuestBook`, `QuestService`, `QuestRewards`, `AuthoredQuestSession` |
| Participação e execução | `multiplayer/RpgSessionWorld`, `SessionSimulation`, `RevisionCoordinator`, `SessionCharacterSelection`; `rpg/RpgSkillEngine`, `SharedRpgEncounter` |
| Transporte e leitura do cliente | `multiplayer/LocalSimulationServer`, `LocalClient`, `Protocol`, `GameplayProtocol`, `SkillProtocol` |
| Persistência existente | `rpg/RpgSave`, `LocalSaveStore`, `CharacterSaves`, `SyntheticOnlineRepository`, `SyntheticSessionLaboratory` |
| Referência exclusiva de teste | `server/src/test/java/com/projetok/rpg/Task225R3RecoveryLab.java`, `Task196ResumeHarness.java` |

Chaves na primeira linha indicam cinco nomes de arquivos confirmados, não um
diretório literal. Fontes e símbolos detalhados estão na matriz de estados.

## Pré-requisitos e dependências

R1/R2 da TASK-225 e ADR R3 presentes foram consultadas. A R3 é referência
adicional mais recente: comprova um envelope e posse locais em laboratório,
sem importação multiplayer, autenticação de rede ou sobrevivência a restart.
Sua autorização histórica de Git não autoriza commit/push desta ORG-03.

## Plano de implementação incremental

Esta entrega executa somente auditoria/documentação. Próximos recortes propostos:

1. Aprovar identidade, política de recursos/efeitos, recargas e perda tolerada.
2. Autorizar DTO/validador puros e testes de contrato, sem runtime/banco.
3. Projetar captura completa no owner e candidato de instalação em laboratório.
4. Validar recibos, gerações e falhas entre memória/armazenamento, ainda isolado.
5. Só com autorização própria, projetar migrations e adaptador PostgreSQL em
   ambiente descartável, depois autenticação/protocolo e integração de runtime.

Não executar os passos futuros por existência deste plano.

## Critérios de aceite

Inventário confiável; fronteiras entre personagem/sessão/conexão; snapshot
proposto; identidade e concorrência; persistência futura; evidências de
preservação. Os seis itens são tratados nos documentos vinculados abaixo.
Aceite arquitetural não significa política aprovada ou recuperação implementada.

## Plano de testes

87 casos dirigidos existentes, em 20 classes, aprovados, sem falhas/erros/skips.
Sem banco ou saves pessoais. Revisão conceitual cobre os 12 cenários pedidos,
distinguindo evidência atual de teste futuro do novo contrato. Não repetir a
suíte GUI para uma entrega exclusivamente documental. Ver relatório de testes.

## Riscos e compatibilidade

Snapshot parcial pode zerar recargas/atributos; identidade efêmera pode mascarar
perda; recompensa sem recibo atômico pode duplicar; restauração de flags/mobs
individuais pode corromper encontro compartilhado. Política de status negativos
e confirmação durável precisa ser aprovada antes da implementação.
Offline/Coop, formatos locais, gameplay e protocolo permanecem inalterados.

## Plano de rollback

Remover apenas os sete novos documentos e o adendo ORG-03 do documento principal;
preservar evidências e todo trabalho anterior. Cópia do principal anterior em
`temp/org03/ORGANIZACAO-before.md`. Sem restauração global do Git.

## Entregáveis e relatório final

- [Matriz de estado](ORG-03-MATRIZ-ESTADO-PLAYER.md).
- [ADR de identidade/reconexão](ORG-03-ADR-IDENTIDADE-E-RECONEXAO.md).
- [Contrato de snapshot](ORG-03-CONTRATO-SNAPSHOT.md).
- [Plano de persistência](ORG-03-PLANO-PERSISTENCIA.md).
- [Testes e limites](ORG-03-TESTES.md).
- [Relatório final](ORG-03-RELATORIO-FINAL.md).
