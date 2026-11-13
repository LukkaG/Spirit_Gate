# TASK-225 — Projéteis, trajetórias e colisões

Referência: 03/11/2026, branch `beco`, HEAD `6d72c65`. Escopo exclusivo do pedido anexado; nenhuma execução da TASK-226.

## Contexto e motivação

A TASK-223 identificou diferença entre projétil dirigido autoritativo e segmento visual inicial (A-03), além de espera sem limite para materialização de Quatro relâmpagos (A-04). A TASK-224 acrescentou feedback de conjuração sem mudar a física. Esta tarefa investiga e testa essas condições preservando os oito kits e suas 40 habilidades.

## Estado atual verificado

Java 17 e Maven disponíveis; único POM em `server/pom.xml`, JUnit 5.11.4 e Surefire 3.5.2 já no cache local. Aplicações 6/7 negociadas sobre TCP. O Git inicial tinha 54 modificações, 2.432 exclusões e 321 entradas não rastreadas; isso inclui fontes anteriores e distribuições portáteis removidas antes desta task. Nenhuma dessas alterações foi descartada. O status consultado do monitor de erros tinha listas de pendências/falhas vazias; seu timestamp antigo não comprova processo ativo.

Checkpoint: `.task-checkpoints/CORUJAO-IX/TASK-225/`. `protected-hashes.json` registra 2.594 arquivos locais, incluindo 550 fontes/testes e 186 arquivos de skills. Fontes, documentação e capturas anteriores têm cópia em `baseline/`; Git, diff prévio e pedido integral também foram preservados. Não confundir HEAD com o conteúdo local anterior das TASKs 223/224.

## Objetivo

Verificar contato contínuo, ordem de vítimas, bloqueio por cenário, limites dos ataques múltiplos, destinos de deslocamentos e coerência visual. Corrigir somente divergências comprovadas que caibam no contrato vigente; apresentar separadamente qualquer mudança de imunidade/materialização ou de protocolo.

## Escopo incluído

Auditoria das dez famílias, testes isolados de geometria/geração e lifecycle, dois clientes TCP reais locais, aplicações 6/7, reprodução visual do alvo lateral, investigação limitada e medição sintética do A-04, regressões Java/Swing e documentação de evidências.

## Fora do escopo

TASK-226 e posteriores; mudança de dano, energia, recargas, imunidade, duração, perfil físico, saves, catálogo ou protocolo; arte nova; publicação, updates, build privada, commit/push; homologação física Radmin ou de launcher nativo. A aprovação da implementação anterior não é assumida como homologação humana.

## Arquivos/módulos envolvidos (confirmados)

Produção em `server/src/main/java/com/projetok/`:

- `rpg/RpgSkillEngine.java`, `RpgWorld.java`, `RpgSkillCatalog.java`: execução, pontos de contato e regras reais, inspecionados.
- `multiplayer/RpgSessionWorld.java`, `SkillProtocol.java`, `RemoteTimeline.java`, `NetworkSkillPreview.java`: contratos, lifecycle e prévias, inspecionados.
- `domain/movement/CollisionService.java`, `MovementService.java`, `RpgCollisionProfile.java`: geometria existente, inspecionada.
- `multiplayer/NetworkSkillPresentation.java`: único arquivo de produção modificado, para limpeza dos efeitos do lançador removido/morto.

Novos testes ficam em `server/src/test/java/com/projetok/{rpg,multiplayer}/Task225*.java`; capturas e medições em `docs/skills-preview-task225/`. Inventário final em `TASK-225-RELATORIO-FINAL.md`.

## Pré-requisitos e dependências

TASKs 223/224 locais e cache Maven existente. A skill [spiritgate-server-engineer](../.agents/skills/spiritgate-server-engineer/SKILL.md) orientou a inspeção e preservação dos contratos. Nenhuma instalação nem banco foi necessário. Os cenários são descartáveis, em memória, sem personagens pessoais.

## Plano de implementação incremental

1. Preservar baseline, localizar fontes e auditar as famílias e exceções.
2. Reproduzir limpeza visual incorreta antes de corrigir; manter sequência e comparar tick do snapshot com tick do lançamento.
3. Exercitar primeiro contato, paredes finas, geração, ataques múltiplos e investidas, sem aumentar hitboxes.
4. Reproduzir A-03 com engine e render reais, registrar diferença e limite de dados; não adivinhar ID de alvo.
5. Investigar A-04 com limite externo, quatro pulsos únicos, reentrada e medição da busca completa; documentar gate de autorização.
6. Executar TCP local, suíte e complemento gráfico, conferir integridade e restaurar somente evidências históricas regeneradas pelos testes.

## Critérios de aceite

Matriz das 40 habilidades/dez famílias; testes de alvos fixos/móveis, primeiro contato, cenário, múltiplos e deslocamentos; impacto visual nas coordenadas confirmadas; cancelamento visual sem retransmissão; A-03 residual e A-04 com decisão explícita; regressões e limites registrados. Divergência que exige dados novos ou decisão de regra deve permanecer identificada, sem alteração silenciosa da aplicação 7.

## Plano de testes

Comandos, contagens e cenários executados em [TASK-225-TESTES.md](TASK-225-TESTES.md). Limites externos dos testes isolados/TCP e do processo Maven evitam investigação indefinida. Tempos sintéticos da busca não são benchmark de carga/FPS. Capturas do renderer são evidência técnica, não homologação estética ou de rede física.

## Riscos e compatibilidade

Servidor continua decidindo trajetória, colisão, dano e término. O cliente não recebe ID/geração do alvo nem pontos intermediários; a curva exata do A-03 exige proposta negociada. A-04 não pode receber redução de imunidade, timeout de regra ou teleporte sem autorização específica, conforme o próprio pedido. A investigação não autoriza passar ao próximo gate.

## Plano de rollback

Comparar `NetworkSkillPresentation.java` com a cópia do checkpoint e reverter somente o bloco de limpeza em `actors()` e seu Javadoc, preservando alterações posteriores. Os arquivos `Task225*.java` e os novos documentos/capturas podem ser retirados individualmente após conferir dependências. Progresso/pendências têm cópia anterior. Nunca usar restauração global, reset/clean; não há migração nem novo formato wire/save a reverter.

## Entregáveis e relatório final

Este plano, [testes](TASK-225-TESTES.md), [relatório](TASK-225-RELATORIO-FINAL.md), [matriz](TASK-225-MATRIZ-COLISOES.md), investigação A-04, capturas, proposta de trajetória e atualização de progresso/pendências. Checkpoints anteriores preservados.
