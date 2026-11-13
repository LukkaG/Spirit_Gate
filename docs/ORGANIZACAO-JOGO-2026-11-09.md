# Organização do jogo e preparação para PostgreSQL

## Estado verificado

Branch `beco`, HEAD `13b91dc`. Há milhares de alterações locais anteriores;
nenhum descarte, movimentação coletiva, commit ou publicação foi realizado.
Java 17, módulo Maven `server/pom.xml`, sem dependência de produção JDBC no POM.
O PostgreSQL histórico foi removido; persistência atual inclui saves locais e
laboratório sintético em arquivos, sem comprovação de persistência online de contas.

## Responsabilidades e caminhos reais

| Área | Código existente | Responsabilidade |
| --- | --- | --- |
| Skills | `domain/skill`, `rpg/RpgSkillCatalog`, `rpg/RpgSkillEngine` | Definições e execução autoritativa; UI em `multiplayer/NetworkSkill*` |
| Player | `domain/entity/Player`, `domain/factory/PlayerFactory`, `rpg/CharacterIdentity`, `rpg/RpgWorld` | Identidade, atributos e estado do personagem; não confundir conexão com personagem |
| Mob | `domain/mob`, `rpg/EnemyService`, `rpg/MobNavigation`, `domain/entity/Mob`, `domain/factory/MobFactory` | Espécie separada da instância; runtime RPG e modelo por componentes ainda distintos |
| Sistemas | `domain/combat`, `domain/movement`, `domain/progression`, `domain/event`, `multiplayer`, `studio` | Regras compartilhadas, sessão/rede e autoria com responsabilidades separadas |
| Missões | `rpg/QuestBook`, `rpg/QuestDocuments`, `rpg/QuestService`, `rpg/QuestRewards`, `rpg/AuthoredQuestSession` | Definição, validação, objetivos, progresso e recompensa |
| Raças | `domain/race`, `rpg/CharacterRaces`, `rpg/Domains` | Catálogos atuais distintos; raça e domínio não são a mesma entidade |

Todos os caminhos Java acima são relativos a `server/src/main/java/com/projetok/`.
Mover arquivos sem atualizar acessos package-private, reflexão, scripts e recursos
quebraria esses contratos. A divisão atual é mapeada, não declarada já migrada.

## Primeira etapa implementada

`domain/mob/MobDefinition` representa a espécie imutável com limites existentes.
`domain/mob/MobCatalog` valida IDs e dados, sem dependência de arquivo ou banco.
`EnemyService` usa o catálogo e mantém suas instâncias, IA, geração, dano, respawn
e piso de HP por encontro. Seu tipo Definition foi preservado para compatibilidade.

Recursos existentes continuam em `server/src/main/resources/rpg/`: `enemies.properties`
define espécies; `spawns.properties` posiciona instâncias; `loot.properties` define
loot. Sprites permanecem em `rpg/mobs/`. Nenhuma cópia concorrente dos assets foi criada.
Os seis mobs experimentais já constam do catálogo, sem inventar recompensas ou níveis.

`database/drafts/001_mob_catalog.sql` é uma proposta não executada para PostgreSQL próprio.
Não constitui integração JDBC nem esquema completo do jogo.

## Sequência de implementação restante

1. Extrair definições de player, skills e missões em recortes pequenos, preservando
   adaptadores nos pacotes atuais e verificando cada dependente antes de mover.
2. Resolver a coexistência dos dois modelos de mob com testes de equivalência;
   definir níveis, XP, loot, skills e raças dos novos mobs com balanceamento explícito.
3. Consolidar IDs de raças, sem tratar aliases legados como novas raças.
4. Definir snapshot persistente completo de personagem e recibos de recompensas.
5. Completar e validar schema em PostgreSQL descartável; só depois incluir driver,
   migrations e repositórios transacionais fora do tick, com revisão e testes de falha.

Criar um pacote genérico `sistema` para todo o código não é a divisão proposta:
combate, movimento, progressão, rede, autoria e persistência têm limites próprios.
Não há autorização implícita para instalar dependências, migrar saves ou publicar conteúdo.
A curva de XP discutida permanece uma definição de produto ainda não aplicada.

## Validação e limitações

19 testes dirigidos aprovados: MobCatalogTest, EnemyRpgTest, EnemyAiTest,
MobCollisionTest, MobCeroTest, EnemyDurabilityTest e Task208ProgressionTest.
Comando: `mvn -o -f server/pom.xml -Dmaven.repo.local=.maven-repository
-Dtest=MobCatalogTest,EnemyRpgTest,EnemyAiTest,MobCollisionTest,MobCeroTest,EnemyDurabilityTest,Task208ProgressionTest test`.

A execução inicial compilou produção e testes, mas falhou por recursos `.properties`
ausentes em target/classes. Após copiar os `.properties` e `.sgquest` locais para
target/classes/rpg, `surefire:test` executou os 19 casos sem falhas.
Naquela entrega, o lifecycle Maven sem essa preparação ficou pendente de investigação.
Não houve teste PostgreSQL, homologação física, Radmin ou launcher nativo.

Reversão: desfazer somente a troca de leitura do catálogo em EnemyService e remover
MobCatalog, MobDefinition e seus testes; preservar os demais diffs locais do arquivo.
Os arquivos SQL e de documentação são independentes do runtime.

## Adendo ORG-02 — estabilização verificada

O lifecycle padrão `mvn test` recuperou os14 arquivos de dados removidos da saída
e aprovou os19 testes anteriores. Também compilou com classes/test-classes/maven-status
inicialmente ausentes, sem cópia manual de recursos:23 testes dirigidos aprovados.
A causa da falha histórica não se reproduziu; POM e fontes de produção foram mantidos.

Suíte geral:1064 testes,3 falhas gráficas e nenhum erro/skip. As duas classes envolvidas
passaram isoladamente (4 testes); a execução conjunta continua registrada como falha.
Novos testes de recursos, limites e adaptação de mobs protegem o recorte extraído.

O SQL foi comparado estaticamente com MobDefinition. Foi preparado gerador para
teste em PostgreSQL descartável com10 espécies reais,30 rejeições e roundtrip CSV;
execução em servidor permanece pendente. Nenhum banco foi conectado ou migrado.

[Escopo ORG-02](ORG-02.md) · [Testes](ORG-02-TESTES.md) ·
[Matriz dos modelos](ORG-02-MATRIZ-MODELOS-MOB.md) ·
[Relatório final](ORG-02-RELATORIO-FINAL.md).
As etapas futuras de Player, Skills, Quest e persistência são recomendações;
esta rodada não as inicia nem altera TASK-225/A-04.

## Adendo ORG-02-R1 — regressão geral e limite de estabilização

09/11/2026, mesma branch `beco` e HEAD `13b91dc`. Duas execuções completas
do lifecycle Maven aprovaram 1068 casos cada, incluindo os quatro testes novos
da ORG-02, com zero falhas, erros e skips. Os 23 testes dirigidos de mob/recursos
também passaram, sem cópia manual de resources ou alteração do POM.

Os XMLs originais mostram que os nomes `hudDomainSkill3/1` eram mensagens
da asserção de aquisição de foco, não estados booleanos de disponibilidade.
As três falhas históricas não foram reproduzidas com o mesmo stack trace.
Duas rodadas dirigidas desta etapa falharam por ausência de destinatário AWT;
as demais ordens/repetições passaram. Um trace comprovou perda de foco e
desativação nativa com sessão conectada, sem identificar o causador.

**Estabilização com bloqueio de causa raiz:** regressão geral verde não significa
intermitência corrigida. Foi adicionada somente uma extensão observacional
às duas classes de teste, mantendo todas as asserções. Nenhum patch de produção,
mudança de regras/skills, protocolo, SQL ou publicação foi aplicado. Comparação
de 1605 arquivos anteriores: somente as duas anotações de teste mudaram;
produção, POM, assets, recursos e ferramentas inventariadas permaneceram idênticos.

Teclado físico, Radmin e launcher nativo permanecem pendentes. A menor próxima
investigação é repetir os quatro casos em desktop reservado, correlacionando
foco AWT e janela nativa no instante da falha. Próxima organização não iniciada.

[Escopo](ORG-02-R1.md) · [Testes e rodadas intermediárias](ORG-02-R1-TESTES.md) ·
[Análise GUI](ORG-02-R1-ANALISE-FALHAS-GUI.md) ·
[Relatório final e rollback](ORG-02-R1-RELATORIO-FINAL.md).

## Adendo ORG-03 — Player e estado recuperável

09/11/2026, branch `beco`, HEAD `13b91dc`. Referência anterior atualizada:
a [ORG-02-R2](ORG-02-R2-RELATORIO-FINAL.md) executou a matriz controlada e uma
suíte de 1068 casos aprovada; a perda anormal de foco não foi reproduzida e
continua sem causa comprovada. Essa investigação foi encerrada no limite,
sem correção declarada. Os adendos anteriores são registros históricos.

A ORG-03 inicial entrega **auditoria e proposta**, sem mudar runtime ou banco.
Player por componentes e RpgWorld continuam distintos; a admissão RPG cria UUID
derivado da instância/peer e a reconexão atual recria personagem efêmero.
Saves locais e laboratório sintético não cobrem automaticamente atributos,
recargas e ledger autorado do multiplayer. A ADR R3 da TASK-225 foi considerada
como referência adicional, sem integrar sua recuperação ao jogo.

O contrato proposto separa identidade durável, participação/conexão e execução
temporária; exige snapshot completo, recibos econômicos, revisão/fence, relógio
autoritativo e spawn validado. Exclui intangibilidade/projéteis/input antigo,
sem assumir que logout pode apagar penalidades ou zerar cooldowns.
Persistência PostgreSQL futura será exclusivamente no servidor, fora do tick,
com confirmação, backpressure e reconciliação de falhas. Nenhum SQL foi criado.

Validação desta ORG-03: **87 testes dirigidos aprovados em 20 classes**, sem
falhas/erros/skips, sem banco ou saves pessoais. Suíte geral não repetida porque
a entrega é documental. Identidade/reconexão/saves/banco em produção continuam
dependentes de autorização própria; A-04 permanece sem correção de runtime.

[Escopo](ORG-03.md) · [Matriz](ORG-03-MATRIZ-ESTADO-PLAYER.md) ·
[ADR](ORG-03-ADR-IDENTIDADE-E-RECONEXAO.md) ·
[Snapshot](ORG-03-CONTRATO-SNAPSHOT.md) ·
[Persistência](ORG-03-PLANO-PERSISTENCIA.md) ·
[Testes](ORG-03-TESTES.md) · [Relatório final](ORG-03-RELATORIO-FINAL.md).

Próxima implementação recomendada: DTO/validação pura isolada após aprovação
das políticas. ORG-04, integração PostgreSQL e TASK-226 não foram iniciadas.

## Adendo ORG-03-R1 — Contrato Java imutável e validação pura

10/11/2026. Criados `RecoverableCharacterSnapshot`, `SnapshotValidationContext`
e `CharacterSnapshotValidator` em `com.projetok.rpg.recovery`, sem integração
às classes existentes. O contrato usa records e coleções copiadas/limitadas;
valida identidade, revisões, catálogos, progressão, atributos, economia,
missões/recibos, recursos, deadlines e localização sem instalar estado.

Os catálogos que leem recursos são carregados pelo chamador antes da validação.
Resultado estrutural válido não concede posse nem aprova recuperação:
autenticação, mundo/spawn, durabilidade e política de relógio são pendências
explícitas. Restrições adversas não são apagadas por omissão; efeitos em execução,
intangibilidade, dash, projéteis e conexão não fazem parte do schema.

Validação final: **47 novos testes e 87 anteriores, totalizando 134 dirigidos
aprovados**. Suíte geral headless: **1.115 casos reportados, 1.058 executados e
57 excluídos, sem falhas ou erros**. Exclusões não homologam interface/foco.
Um caso novo de ID interno nulo reproduziu erro no validador e passou após
correção localizada; log e XML intermediários foram preservados.

Nenhuma classe anterior, POM, protocolo, save, regra ou asset foi alterado.
Sem recuperação no runtime, persistência PostgreSQL, SQL, dependências ou contas.
A-04 e seu laboratório R3 permanecem inalterados; ORG-04/TASK-226 não iniciadas.

[Escopo R1](ORG-03-R1.md) · [Validações](ORG-03-R1-MATRIZ-VALIDACOES.md) ·
[Testes R1](ORG-03-R1-TESTES.md) · [Relatório R1](ORG-03-R1-RELATORIO-FINAL.md).

## Adendo ORG-03-R2 — Leitura parcial no owner de laboratório

11/11/2026. Investigados campos do contrato R1 e suas fontes reais. Criado
CharacterSnapshotCapture **somente em src/test**, com dois arquivos novos de
teste; nenhuma classe de produção ou arquivo R1 foi alterado. A leitura ocorre
em um único owner de fixture, com cópias imutáveis e recusa de fonte/contexto
incompatíveis. Não é captura integrada a coop-world nem recuperação funcional.

Progresso, bolsa/equipamentos, moedas e localização são copiáveis. Atributos
brutos, fração de energia, deadlines exatos, ledgers/recibos/direitos completos
e restrições exigem APIs/informações ainda ausentes. manualAttributes/save/
snapshot não são usados pelo capturador devido aos efeitos de materialização
ou escrita de flags. Não há campos preenchidos por zeros/recibos inventados.

Todos os candidatos reais do protótipo são incompletos e seguem rejeitados pela
R1, mantendo readyForActivation=false. Metadados de identidade/contexto são
sintéticos explícitos do laboratório, sem prova de proprietário ou persistência.
API read-only futura proposta precisa de autorização própria antes de edição.

Validação: **270 dirigidos aprovados**, incluindo os 47 R1 e 21 novos. Geral
headless: **1136 reportados, 1079 executados aprovados, 57 excluídos, sem falhas
ou erros**. As exclusões não homologam UI/foco. Falha inicial de preparo de
fixture foi corrigida usando interação NPC existente, sem mudar gameplay.

[Escopo R2](ORG-03-R2.md) · [Extração](ORG-03-R2-MATRIZ-EXTRACAO.md) ·
[Lacunas/APIs](ORG-03-R2-LACUNAS-DO-RUNTIME.md) ·
[Testes R2](ORG-03-R2-TESTES.md) · [Relatório R2](ORG-03-R2-RELATORIO-FINAL.md).

## Adendo ORG-03-R3 — Observação imutável do estado real

APIs de pacote adicionais em RpgWorld, RpgSkillEngine, QuestService e
AuthoredQuestSession copiam estado bruto, prazos absolutos e ledgers sem
inicialização tardia, concessão, refresh, avanço ou limpeza. CharacterReadContext
verifica o owner declarado e o contexto; não autentica nem descobre admissão
real. CharacterReadState é separado do contrato recuperável.

O adaptador permanece em src/test. Atributos/recursos disponíveis passaram ao
candidato; ledgers e timing exatos permanecem observações. Recibos, revisão
econômica e políticas de efeitos/clock não foram fabricados. Captura segue
incompleta e sem ativação autorizada. Não há integração TCP, recuperação,
persistência ou alteração de A-04.

[Escopo R3](ORG-03-R3.md) · [Read models](ORG-03-R3-MATRIZ-READ-MODELS.md) ·
[Gate econômico](ORG-03-R3-GATE-ECONOMICO.md) · [Testes R3](ORG-03-R3-TESTES.md) ·
[Relatório R3](ORG-03-R3-RELATORIO-FINAL.md).

## Adendo ORG-03-R4 — Captura interna no owner multiplayer real

RpgSessionWorld pode combinar os read models R3 de uma participação admitida
em seu owner real, registrado pelo LocalSimulationServer. A operação de pacote
revalida Session, peer ativo, conteúdo, região, domínio, transição e binding de
skills; recusas não entregam observação parcial. A ponte entre pacotes publica
somente tipos imutáveis em Java, mantendo as leitoras R3 internas.

Ensaios usam TCP loopback real, aplicações 6/7 e até dois participantes. Não há
novo comando TCP, fila de captura ou resultado enviado ao jogador. Relógios
mantêm suas origens; época durável, identidade/posse e prova econômica seguem
ausentes. Gates econômico e de ativação permanecem fechados. Sem importação,
recuperação, persistência ou alteração de A-04.

[Escopo R4](ORG-03-R4.md) · [Owner/captura](ORG-03-R4-MATRIZ-OWNER-CAPTURE.md) ·
[Autoridade pendente](ORG-03-R4-PENDENCIAS-AUTORIDADE.md) ·
[Testes R4](ORG-03-R4-TESTES.md) · [Relatório R4](ORG-03-R4-RELATORIO-FINAL.md).

## Adendo ORG-03-R5 — Economia e idempotência em laboratório

Contrato experimental somente em src/test: estado, recibo, revisão e índices
de concessão são confirmados por um CAS de imagem imutável em memória.
Retries consultam o ID antes de preparar efeitos; origens/concessões repetidas,
conteúdo divergente e contextos obsoletos são recusados. Resposta perdida é
incerteza reconciliável, sem reaplicar prêmio. Direitos pendentes conservam a
origem e o recibo da concessão.

Nenhum fonte econômico de produção mudou. A memória não garante durabilidade,
identidades/ofertas são sintéticas e flags antigas não geram recibos retroativos.
As alternativas de legado e integração durável permanecem propostas. Sem banco,
recuperação, autenticação, alteração do protocolo ou ativação de A-04.

[Escopo R5](ORG-03-R5.md) · [ADR econômico](ORG-03-R5-ADR-ECONOMIA.md) ·
[Recibos](ORG-03-R5-MATRIZ-RECIBOS.md) · [Integração futura](ORG-03-R5-PLANO-INTEGRACAO-FUTURA.md) ·
[Testes R5](ORG-03-R5-TESTES.md) · [Relatório R5](ORG-03-R5-RELATORIO-FINAL.md).

## Adendo ORG-03-R6 — Contratos antes da durabilidade

Consolidação documental de identidade/posse, admissão/fence, revisões e
economia durável. Os contratos futuros exigem uma cabeça canônica única e
confirmação conjunta dos efeitos, recibos e deduplicação; CAS em memória não
comprova durabilidade PostgreSQL. Não houve implementação ou conexão ao banco.

A matriz distingue 34 decisões com aprovação anterior, recomendação, aprovação
pendente ou bloqueio por evidência. Diferenças entre runtime e R5, nove canais
de cooldown, efeitos negativos, legado e retenção estão explícitos. A recusa
provisória de recuperação sem prova permanece; a alternativa C é candidata.
Os 30 cenários de falha são testes futuros, sem homologação nesta R6.

[Escopo R6](ORG-03-R6.md) · [Durabilidade](ORG-03-R6-ADR-DURABILIDADE.md) ·
[Políticas do jogador](ORG-03-R6-ADR-POLITICAS-PLAYER.md) ·
[Decisões](ORG-03-R6-MATRIZ-DECISOES.md) · [Falhas](ORG-03-R6-MATRIZ-FALHAS.md) ·
[Plano PostgreSQL](ORG-03-R6-PLANO-POSTGRESQL.md) ·
[Gates](ORG-03-R6-GATES-DE-IMPLEMENTACAO.md) · [Relatório R6](ORG-03-R6-RELATORIO-FINAL.md).

## Adendo ORG-03-R7 — Preparação do gate E1

Pacote pronto para revisão humana das decisões D01–D34, com alternativas,
impactos, testes e escopo de aceite por decisão. Os estados R6 permanecem
inalterados; E1 não foi encerrado e nenhuma recomendação foi aprovada pelo agente.

A proposta E2 descreve um laboratório PostgreSQL exclusivamente sintético,
cluster próprio e separado do serviço local existente, cinco autorizações
independentes, limites experimentais e 16 critérios verificáveis. O ambiente
foi inspecionado em leitura; não houve SQL, instalação, criação de cluster ou
execução do laboratório. E2 continua sem autorização e sem início.

[Escopo R7](ORG-03-R7.md) · [Quadro E1](ORG-03-R7-QUADRO-APROVACAO-E1.md) ·
[Proposta E2](ORG-03-R7-ESCOPO-E2.md) · [Ambiente](ORG-03-R7-AMBIENTE-POSTGRESQL.md) ·
[Critérios E2](ORG-03-R7-CRITERIOS-E2.md) · [Relatório R7](ORG-03-R7-RELATORIO-FINAL.md).

Sem integração de recuperação, PostgreSQL, protocolo, timeout/imunidade A-04,
publicação ou início de ORG-04/TASK-226.
