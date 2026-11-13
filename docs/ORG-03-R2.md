# ORG-03-R2 — Captura autoritativa e exportação controlada

## Contexto e motivação

11/11/2026. A R1 implementou contrato e validação pura; não implementou captura.
Esta etapa verifica o que pode ser lido do estado real sem comandos, I/O ou
valores inventados. Não instala, salva, recupera ou autentica personagem.

## Estado atual verificado

Branch `beco`, HEAD `13b91dc`. Git inicial: 69 entradas modificadas, 2429
excluídas e 398 não rastreadas. Status/name-status anteriores arquivados.
Inventário de 2351 arquivos em `temp/org03-r2/before-sha256.json`; checkpoint em
bytes de 11 fontes relevantes em `temp/org03-r2/checkpoint/`, antes das edições.
Java17/Maven, `server/pom.xml`, sem nova dependência.

`LocalSimulationServer` serializa departures/comandos, `world.step()` e snapshots
em executor `coop-world`. `RpgSessionWorld.Session` guarda RpgWorld/proxy e está
privada. Não existe API pública de captura integral nem geração durável de
personagem. Os DTOs/validador/contexto R1 foram preservados.

## Objetivo e escopo incluído

Matriz de extração de todos os campos; investigação de pureza/precisão;
protótipo **exclusivamente em src/test**, usando objetos reais descartáveis e
métodos de leitura existentes. Resultado diferencia recusa de fonte/contexto,
captura incompleta e invalidez estrutural; lacunas de API/política são explícitas.
Nenhuma classe de produção foi editada ou criada.

## Fora do escopo

Novos acessores de produção, owner real de TCP, captura integral, instalação,
I/O no loop, PostgreSQL/JDBC/migration, contas/tokens, worker/lease, mudança
de protocolo/reconexão, saves pessoais, habilidades/balanceamento e timeout A-04.
Não iniciar ORG-04/TASK-226 nem executar commit/push/release/publicação.

## Arquivos/módulos envolvidos (confirmados)

Novos arquivos de teste em `server/src/test/java/com/projetok/rpg/`:

- `CharacterSnapshotCapture.java`: adaptador parcial de laboratório e resultados.
- `CharacterSnapshotCaptureTest.java`: 21 casos, confinados a owner de fixture.

Auditados: RpgWorld, RpgSessionWorld, LocalSimulationServer, SessionSimulation,
RpgSkillEngine, InventoryService, AuthoredQuestSession, QuestService,
DomainProgression e os três arquivos de `rpg/recovery` da R1.
Documentação: cinco documentos R2 e adendo ao principal de organização.

## Pré-requisitos e dependências

O chamador do laboratório mantém **todos** os comandos e leituras na mesma
thread. Metadados de identidade sintética, hash, regras, época, revisão e horário
são explicitamente fornecidos; não são descobertos/autenticados em produção.
Construção de catálogos e mundo ocorre antes da captura, fora do componente.

## Plano incremental executado

1. Auditoria e checkpoint; exclusão de leituras com efeitos colaterais.
2. Leitura parcial em operação única do owner de laboratório.
3. Cópia imutável dos dados disponíveis e diagnóstico dos blocos indisponíveis.
4. Validação pela R1 sem alterar seu resultado ou readyForActivation.
5. Testes novos, regressões, conferência de hashes e preservação das evidências.

## Critérios de aceite e resultado permitido

Matriz confiável, leitura permitida testada sem comandos ou mutação, lacunas
explícitas e código anterior preservado. A conclusão desta investigação não
significa que exista um snapshot completo: todos os candidatos efetivamente
capturados permanecem incompletos e rejeitados como recuperação pela R1.

## Plano de testes

21 casos novos, 47 da R1 e regressões pertinentes: 270 aprovados em 28 classes,
sem falhas/erros/skips na rodada dirigida final. Suíte geral em modo headless;
totais/exclusões em [testes](ORG-03-R2-TESTES.md). Sem homologação de frontend.
Os casos que precisam de APIs inexistentes estão marcados como pendentes,
sem obter campos privados por reflexão ou reexecutar recompensas.

## Riscos e compatibilidade

`manualAttributes()` habilita atributos/concede pontos lazy. `save()` escreve
flags de domínio; `snapshot()` chama save. Esses métodos **não** são usados pelo
capturador. `attributesSnapshotFor/energySnapshotFor` também alcançam o caminho
lazy. `RpgSkillEngine.state` devolve zeros quando sessão ausente; não é prova de
presença nem leitura de deadlines. Não é usado para completar o candidato.

O confinamento do laboratório não é uma garantia de atomicidade do backend
real. Mudança concorrente de conteúdo, callback fora do owner ou remoção de
peer exigem integração própria e aprovação. Sem conversão de ticks em prazo
durável ou apagamento de restrições para simplificar exportação.

## Plano de rollback

Remover somente os dois arquivos novos de teste, os cinco documentos R2 e o
adendo R2 do principal. Preservar fontes anteriores, R1, logs e demais alterações.
Principal anterior em `temp/org03-r2/ORGANIZACAO-before.md`. Sem reset/clean.

## Entregáveis e relatório final

[Matriz de extração](ORG-03-R2-MATRIZ-EXTRACAO.md),
[lacunas/APIs propostas](ORG-03-R2-LACUNAS-DO-RUNTIME.md),
[testes](ORG-03-R2-TESTES.md), [relatório final](ORG-03-R2-RELATORIO-FINAL.md).
Qualquer accessor ou captura integral futura depende de autorização própria.
