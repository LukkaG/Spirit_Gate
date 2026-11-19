# ALPHA-002-A-SERVER — Quatro Relâmpagos

## Baseline e plano antes da implementação

Pedido específico aprovado: corpo fixo com ocupação normal, imunidade e controle de área somente durante a execução; término finito sem materialização. Esse contrato substitui a proposta anterior de retirada/recuperação para `thunder_5`.

Branch `beco`; HEAD `c3d484e76f17c2d93bf0c68ad3ff54280ee8ad39`. Baseline de arquivos e diff anterior preservados em `.task-checkpoints/ALPHA-002-A-SERVER/`. A correção local de handshake de `LocalSimulationServer`, ferramentas, documentos e alterações ORG-03 são preexistentes e serão preservados.

Consultados: AGENTS.md, skill `spiritgate-server-engineer`, Plano Diretor em `.task-checkpoints/CORUJAO-IX/TASK-223/user-directive.txt`, relatórios TASK-225/R1/R2/R3 e contrato A04 de 18/11. O monitor tem status antigo sem pendências declaradas; os erros locais já observados de World Studio não pertencem a esta correção.

| Aspecto | Antes | Comportamento aprovado a implementar |
| --- | --- | --- |
| Posição | Não anda durante a fase; pode buscar outra posição ao terminar. | Centro físico fixo; nenhuma busca/teleporte. |
| Colisão | Intangibilidade/dash excluem o corpo da ocupação de IA e jogadores. | Corpo normal permanece presente em todos esses cálculos. |
| Invisibilidade | Marcador inicial e terminal de skill; cliente possui fusível de cinco segundos. | Mesmos eventos; ocultação exclusivamente visual, sem alterar snapshots físicos. |
| Dano recebido | Ignorado enquanto intangível, potencialmente sem limite. | Ignorado apenas na execução válida e finita. |
| Duração | 32 ticks mais espera por apoio. | 32 ticks definidos no catálogo, sem espera adicional. |
| Quatro cortes | Quatro danos no término dos 32 ticks, uma vez. | Mesmo cálculo, seleção, ledger e instante de impacto. |
| Paralisia | STUN de 30 ticks aplicado pelos impactos. | Controle de área durante a execução substitui esse STUN posterior só nesta skill; controles independentes permanecem. |
| Encerramento | Depende de `skillReappear()`. | Limpeza idempotente da execução; nenhuma posição alternativa. |
| Mundo/revisão/cancelamento | Engine remove pendências e flags. | Remoção invalida a identidade da execução e sua contribuição; sem flags em mobs para vazar. |
| Reconexão | Nova participação efêmera; não há recuperação completa. | Nenhuma saída/reconexão causada pela skill; limitação geral de reconexão continua registrada, sem implantar alternativa B. |

Plano localizado: execução identificada no `RpgWorld`, controle consultado no `SharedRpgEncounter` antes da IA e encerramento no `RpgSkillEngine`. Rejeitar entrada de movimento durante a janela no coordenador para não enfileirar caminhada extra. Preservar `skillReappear()` para outras técnicas. Não alterar cliente, wire, saves ou arte.

Riscos: borda de expiração no ciclo mundo → encontro → engine; controles simultâneos; mensagens/eventos antigos sem ID explícito de execução no wire; limitação de reconexão existente. Validação: testes determinísticos e TCP loopback em cópia isolada, sem instalar dependências nem sobrescrever evidência histórica.

## Resultado

**Implementado e testado localmente no servidor.** Duas rodadas finais, com seleções disjuntas, executaram **242 casos JUnit: 240 de regressão e 2 adicionais de coordenador, zero falhas, erros ou ignorados**. Java 17/Maven offline, em cópias temporárias de fontes/assets, sem banco ou saves pessoais.

### Arquitetura e arquivos alterados

- `server/src/main/java/com/projetok/rpg/RpgWorld.java`: estado `FixedLightning` por token da execução, região, domínio, deadline privado e predicado de área. Imunidade e bloqueios consultam a janela válida; a skill não define `skillIntangible` nem `skillDashing` e mantém ocupação normal. Cancelamento antigo não remove execução substituta.
- `server/src/main/java/com/projetok/rpg/RpgSkillEngine.java`: `LightningField` inicia o estado identificado, preserva quatro impactos únicos no tick terminal e encerra sem `skillReappear()`. Cancelamento de pendências libera o token; impacto atrasado de token substituído é recusado. A seleção de área foi extraída para um predicado compartilhado, preservando tangência, elegibilidade e linha de visão.
- `server/src/main/java/com/projetok/rpg/SharedRpgEncounter.java`: consulta a contribuição das áreas válidas antes de executar a IA de cada inimigo, com a mesma supressão de movimento/ataque/preparação de skill que o caminho STUN existente. Não grava flags nos inimigos nem altera seus deadlines independentes; saída da área/execução remove somente essa contribuição. Não paralisa jogadores/NPCs, que não percorrem esse laço de IA.
- `server/src/main/java/com/projetok/rpg/RpgSkillCatalog.java`: descrição corrigida e remoção do STUN residual de 30 ticks de `thunder_5`. Raio 110, energia 40, recarga 270, duração 32, quatro impactos e dano base 14 preservados. O controle novo vem da área ativa, não de efeitos após o término.
- `server/src/main/java/com/projetok/multiplayer/RpgSessionWorld.java`: consome sequência de comandos recusados, bloqueia ações e movimento durante a janela e limpa intenção anterior ao iniciar, evitando movimento guardado para o fim. Consulta de atributos/energia e seleção de alvo continuam disponíveis; não constituem ação ofensiva ou deslocamento.
- Testes existentes atualizados: `RpgSkillEngineTest.java` e `Task225MaterializationTest.java`. Expectativas antigas de intangibilidade, deslocamento e stun residual foram substituídas pelo contrato aprovado. A reprodução original foi preservada nos documentos/evidências e no backup do teste no checkpoint.
- Novos testes: `Alpha002LightningTest.java` (13 casos), `Alpha002LightningTcpTest.java` (3 casos) e `Alpha002LightningSessionTest.java` (2 casos). São 18 novos casos, já incluídos nos 242; não somar novamente.

### Preservação comprovada

Dos **3.849 arquivos do baseline**, **3.842 permaneceram idênticos**, com sete alterações intencionais em fontes/testes existentes. Os arquivos novos estão listados acima e nos entregáveis. O hash da correção preexistente em `LocalSimulationServer.java` permaneceu igual. Não houve alterações em Protocol/GameplayProtocol/SkillProtocol, cliente visual, assets do checkout, ORG-03, D33, banco ou persistência 193–196.

O registro `preservacao.json`, os hashes e os backups estão em `.task-checkpoints/ALPHA-002-A-SERVER/`. Esse diretório é evidência local, não publicação. Nenhuma ferramenta foi instalada e nenhum commit, push, pacote ou release foi criado.

### Semântica temporal e controles

Janela `[início, início + 32)`. No tick terminal, o controle da área e a imunidade já expiraram, e a IA pode agir antes dos impactos porque a ordem preexistente é mundo → encontro → engine. O lançador não anda nesse último avanço de mundo: a consulta de movimento ocorre antes de incrementar seu tick. A ação normal volta no ciclo seguinte. O prazo é da simulação; não representa garantia de latência de parede.

Paralisia usa o mesmo círculo de dano (raio da skill + raio de corpo 19 e visibilidade). Alvos que entram/saem são reavaliados antes da atualização de IA, sem serem movidos pelo controle. Duas execuções sobrepostas somam contribuições: cancelar uma não libera o alvo ainda coberto pela outra. Timers independentes de ROOT/STUN continuam correndo e permanecem intactos no término/cancelamento individual. O `clear()` global mantém a semântica preexistente de limpeza global ao substituir revisão; não foi redesenhado.

### Situação A-04 e limites

A espera autoritativa sem apoio de `thunder_5` foi removida. A câmara artificial antiga encerra aos 32 ticks na origem, conserva a sessão e continua sem repetição dos quatro cortes aos 180 ticks. A sobreposição deliberadamente injetada nessa fixture não é uma admissão permitida pelo mundo real; os testes de colisão separados verificam que o corpo permanece ocupando o centro.

Custos e recargas na mesma sessão, progressão, inventário e contribuição/recompensa única passaram nos testes relevantes. Saída, morte forçada de fixture, região, revisão, substituição, cancelamento e token antigo não transferem a execução. TCP real com observadores 6/7 confirmou corpo presente, 32 ticks, quatro impactos e conexão mantida; saída durante a execução não deixou dano tardio.

**O requisito geral de não resetar progresso/cooldown ao reconectar não está atendido pelo sistema atual.** A correção não provoca desconexão, mas uma reconexão voluntária continua criando personagem efêmero. Resolver isso exige identidade/continuidade fora deste recorte, expressamente sem implantar alternativa B nesta tarefa. O fluxo preexistente de troca de revisão também limpa sessões do engine; o teste adicional verifica ausência de fase/dano antigo, não preservação de recargas nessa troca.

O cliente antigo não foi atualizado e ainda pode manter ocultação por até cinco segundos na ausência de terminal. O [contrato visual](ALPHA-002-A-CONTRATO-VISUAL.md) especifica os dados disponíveis e limites de correlação/cancelamento. A frente visual, testes físicos/Radmin, launcher nativo e aprovação comercial permanecem pendentes.

### Falhas intermediárias e incidente de testes

As rodadas intermediárias detectaram expectativas do contrato antigo, tipo incorreto de XP na fixture nova, spawn de chefe na região errada, alvo externo sem motivo para caminhar e bootstrap de revisão sem ACK. Foram corrigidos nos testes sem relaxar a regra de produção para fazê-los passar. Os logs/XML estão arquivados; detalhes no [relatório de testes](ALPHA-002-A-SERVER-TESTES.md).

Uma seleção indevida de teste legado gerou 12 PNGs na cópia temporária. A rodada foi interrompida, os arquivos gerados removidos e a suíte final executada sem exportadores de galeria. Nenhum asset do checkout foi alterado. O incidente e as limitações estão em [pendências e rollback](ALPHA-002-A-SERVER-PENDENCIAS.md).

## Próximo passo

Revisar os quatro documentos da entrega e tratar a frente visual em solicitação própria, usando os eventos existentes antes de propor extensão wire. Não iniciar automaticamente arte, publicação ou recuperação de personagem.
