# ORG-03-R3 — Read models autoritativos e extração sem efeitos colaterais

## Contexto e estado verificado

Execução de 11/11/2026 sobre `beco`, HEAD
`13b91dc test: validar continuidade sintetica do personagem na TASK-225-R3`.
O checkout já continha muitas alterações e arquivos não rastreados. O status
completo e o diff inicial estão em `temp/org03-r3/git-status-before.txt` e
`git-diff-before.patch`. O diff contra HEAD inclui trabalho anterior e não
representa sozinho a entrega R3.

Foram consultados AGENTS.md, a skill local de engenharia do servidor, contrato
ORG-03, matriz R1, matriz/lacunas/testes R2, fontes e testes envolvidos. Java
Temurin 17.0.20.1 e Maven 3.10.0; o único POM encontrado é `server/pom.xml`.
`logs/monitor/status.json` não apontava pendências, mas seu registro é antigo;
os TXT foram consultados e contêm erros anteriores de IDE/World Studio. Não
foram corrigidos nesta tarefa, que exclui World Studio e instalação de ferramentas.

## Objetivo e escopo executado

Adicionar APIs de pacote que copiam estado existente sem executar gameplay.
RpgWorld permanece independente de RecoverableCharacterSnapshot. A conversão
experimental permanece exclusivamente em `src/test`, no adaptador autorizado R2.
Nenhum chamador de produção foi conectado às APIs novas.

1. Preservação de fontes afetados em bytes, inventário Git e hashes.
2. Contexto de leitura declarado no owner, read model de mundo, ledger e timing.
3. Testes de precisão, pureza, imutabilidade, ausência e recusas.
4. Adaptação do laboratório: atributos/recursos realmente disponíveis no candidato;
   estado exato, ledgers e timing separados nas observações.
5. Regressões dirigidas, suíte geral headless e comparação de preservação.

## Arquivos envolvidos

Produção existente: `RpgWorld.java`, `RpgSkillEngine.java`, `QuestService.java`
e `AuthoredQuestSession.java`, em `server/src/main/java/com/projetok/rpg/`.
Novos: `CharacterReadContext.java` e `CharacterReadState.java` no mesmo pacote.
Teste existente: `CharacterSnapshotCapture.java` e
`CharacterSnapshotCaptureTest.java`; novo: `CharacterReadModelsTest.java`, em
`server/src/test/java/com/projetok/rpg/`.

As alterações nas quatro classes de produção existentes são adições de leitura;
as regras e métodos anteriores permanecem preservados. Não há dependência nova.

## Autoridade e coerência

`CharacterReadContext` registra thread, instância de mundo, identidade observada,
região, domínio e executor declarados pelo coordenador. Recusa outra thread
antes de ler o mundo, outro objeto, contexto de região/domínio alterado e escopo
retirado. Seu `retire()` invalida somente o escopo, sem modificar gameplay.

Esse contexto **não descobre o owner de produção nem autentica o chamador**.
Deve ser criado no ponto serializado pelo coordenador que já detém a participação.
Não há registro global novo de owner, lock, revisão durável ou geração inventada.
Todos os escritores da fixture estão confinados à sua thread; o contexto não
substitui esse confinamento e não permite leitura concorrente de escritores externos.

O timing verifica presença real no mapa do executor, mundo por identidade de
objeto, região, domínio, vida e participação no mapa entregue pelo owner. A
associação ao executor é verificada pelo contexto. Ausência tem status próprio
e deadlines nulos; presença com cinco prazos zero é estado diferente.

A revisão conhecida pelo adaptador continua sendo metadado **sintético** do
controlador R2, comparado por igualdade do LaboratoryContext. Tick não se torna
revisão. Uma referência RpgWorld retida não detecta sozinha `RpgSessionWorld.leave`;
o laboratório invalida Source e contexto explicitamente. O mapa de participação
usado no adaptador é local à fixture e não prova admissão real.

## Exclusões e autorização futura

Sem captura TCP, comando no LocalSimulationServer, alteração de SessionSimulation
ou RpgSessionWorld, recuperação, importação, PostgreSQL, persistência, wire6/7,
World Studio, launcher, HUD, assets, recompensas ou A-04. Não se iniciou ORG-04
nem TASK-226. Sem commit, push ou publicação.

Uma futura integração `coop-world` precisa de autorização separada e proposta
de assinatura, owner, ordenação, validade da sessão, desconexão, orçamento por
tick, revisão de conteúdo e testes de concorrência. Não há implementação parcial
desse caminho nesta entrega.

## Critérios de aceite e evidências

As APIs não chamam manualAttributes/save/snapshot/applyLevelUp, não concedem
recursos, não limpam controles e não avançam relógios. Cópias permanecem estáveis
após mudanças reais posteriores. Restrições externas, origens de efeitos e
políticas de recuperação continuam desconhecidas. O candidato continua sem
autorização de ativação, conforme o validador R1.

Ver [matriz](ORG-03-R3-MATRIZ-READ-MODELS.md),
[gate econômico](ORG-03-R3-GATE-ECONOMICO.md),
[testes](ORG-03-R3-TESTES.md) e [relatório](ORG-03-R3-RELATORIO-FINAL.md).
