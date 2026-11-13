# TASK-224 — Seleção, conjuração e feedback das habilidades

## Contexto e motivação

Autorização exclusiva recebida em 02/11/2026 após aprovação técnica da TASK-223. Melhorar A-01 com informações existentes, preservando aplicação 7, input, combate e balanceamento. A autorização integral está em `.task-checkpoints/CORUJAO-IX/TASK-224/user-directive.txt`.

## Estado atual verificado

Branch `beco`, HEAD `6d72c65`; baseline: 54 M, 2.432 D, 310 entradas não rastreadas. Nenhuma mudança anterior descartada. Foram inspecionados GameInputRouter, NetworkSkillControls, NetworkSkillPresentation, NetworkGameHud, NetworkHotbar, RpgNetworkClientLauncher, RpgSkillCatalog, RpgSkillEngine, RpgSessionWorld, SkillProtocol, GamePanels e os componentes de alvo/colisão pertinentes.

Havia quatro ações históricas (básico, Shunpo ofensivo, Shunpo defensivo e poção) junto aos cinco slots de domínio. O TAB de alvos interceptava também botões focados da HUD. Envio não tinha ACK por intenção; geometria da revisão já estava disponível ao cliente. O servidor validava contatos/recursos corretamente, mas a interface não expunha toda essa diferença.

## Objetivo

Separar navegação e seleção pelo contexto de foco, esclarecer destinatários e exibir preparação, envio e dados observados sem fingir confirmação/rejeição não existente.

## Escopo incluído

- Política contextual sem novo atalho padrão ou KeyEventDispatcher.
- Informação de alvo e prévias de alcance/linha de visão/área usando snapshot e cenário da revisão ativa.
- Feedback de nível, energia, recarga, alvo e resultado desconhecido.
- Eventos e estado de cooldown/Sentinela descritos como dados recebidos, sem correlação de ACK inventada.
- Faixa navy/ciano, preservando nove comandos, foco, atalhos e layout mínimo.
- Testes AWT, Swing, TCP, regressão geral e documentação de uma proposta separada para dados ausentes.

## Fora do escopo

TASK-225 e posteriores; qualquer mudança de servidor, dano/cura, energia/regeneração, cooldown, nível, status, imunidade, colisão, movimentação especial, inventário, save, missão, World Studio, launcher ou protocolo. Sem arte nova, dependência, PvP, release ou publicação.

## Arquivos/módulos envolvidos (confirmados)

- `multiplayer/GameInputRouter.java`: TAB contextual.
- `multiplayer/NetworkSkillPreview.java`: nova consulta de prévia, não autoritativa.
- `multiplayer/NetworkSkillSubmission.java`: novo feedback da última intenção, sem ACK.
- `multiplayer/NetworkSkillControls.java`: estados, avisos por slot e área.
- `multiplayer/NetworkGameHud.java`, `NetworkTargetPanel.java`: apresentação navy/ciano, HP e ajuda contextual.
- `multiplayer/RpgNetworkClientLauncher.java`: fornece a geometria da revisão e integra o feedback ao loop existente.
- Testes novos Task224SkillFeedbackTest/Task224CastingGuiTest, ajustes de expectativas em GameInputRoutingTest/TargetCombatGuiTest e estabilização de espera de foco nos helpers de teste.

Todos os caminhos de produção acima pertencem a `server/src/main/java/com/projetok/`; testes em `server/src/test/java/com/projetok/multiplayer/`.

## Pré-requisitos e dependências

JDK 17, Maven e cache existentes, Swing local. Nenhuma instalação. Mantida a aplicação 7 sobre o protocolo vigente. A skill [spiritgate-server-engineer](../.agents/skills/spiritgate-server-engineer/SKILL.md) orientou a revisão de autoridade/compatibilidade; ela não exigiu nova autorização para este escopo.

## Plano de implementação incremental

1. Capturar Git/hash/diff e copiar fontes/evidências anteriores.
2. Fazer TAB selecionar só no viewport; preservar navegação e os bindings existentes.
3. Consultar destinatário, alcance e cenário local para avisos, mantendo validação no servidor.
4. Mostrar envio/espera, evento ou estado recebido e resultado desconhecido sem timeout virar rejeição.
5. Expor feedback no jogo e preservar as nove ações em resize.
6. Testar os vinte casos requeridos, registrar falhas/limites, executar regressões e restaurar artefatos históricos dos testes.

## Política contextual implementada

| Contexto | TAB / SHIFT+TAB | SPACE | ENTER | F5–F9 / remapeadas | Movimento / ESC |
|---|---|---|---|---|---|
| Viewport ativo e conectado | TAB troca alvo; SHIFT+TAB faz navegação Swing para a HUD | Básico | Sem ativação arbitrária de UI | Conjura slot elegível | Move; ESC abre/fecha contexto existente |
| Controle da HUD focado, sem formulário/modal | Ambos navegam; não alteram alvo | Básico, não clique no botão destacado | Ativa o controle focado | Mantém skills | Bindings existentes; SHIFT+TAB não dispara Shunpo |
| Área preparada | TAB não troca alvo; mantém navegação acessível | Básico conforme bindings existentes | Confirma a área | Mesma tecla da área confirma; outra skill segue seu próprio fluxo | WASD/setas ajustam retículo; ESC/botão direito cancela |
| Texto, combo, spinner ou slider editável | Navegação/edição Swing | Entrada do controle | Entrada do controle | Não dispara gameplay | Digitação não vira movimento |
| Configuração/modal/captura | Dentro do modal existente | Opera controle ou captura remapeamento | Opera controle | Capturada/retida pelo contexto existente | Gameplay bloqueado; ESC segue fechamento existente |
| Desconectado/sincronizando/derrotado | UI continua navegável | Não dispara combate | Controles válidos da UI | Sem conjuração | Preparação é cancelada; retorno/menus seguem elegibilidade existente |

Não foi criado modo alternável nem nova tecla. Voltar o foco ao viewport por clique ou navegação restaura TAB de alvos. Durante preparação, a prioridade de ENTER é confirmar a área; fora dela, ENTER ativa o controle de interface focado.

## Semântica da prévia e do envio

- IDs/snapshot delimitam inimigo, aliado e aliado derrotado. NPC não é alvo ofensivo. Geração e elegibilidade definitiva continuam no servidor/seleção existente.
- LONGE/LOS/CHEIO são avisos estimados, não códigos do servidor. Avisos de alcance/obstáculo não bloqueiam por si o envio; o servidor continua decidindo. Falta/tipo inválido de alvo, nível, energia e recarga usam os requisitos já existentes da interface.
- Área exibe centro, raio, alcance e limites; mouse/teclado são limitados à prévia. Ciano indica prévia geométrica sem obstrução detectada, não aceite; âmbar indica obstrução estimada/geometria ausente. Uma área obstruída pode ser enviada e recusada pelo servidor sem produzir evento.
- “Enviada; aguardando confirmação” significa enfileirada no cliente. “Evento recebido” ou “recarga atualizada” descrevem somente o snapshot/evento observado. Não são ACK por intenção, especialmente se houver ações concorrentes com o mesmo ID visual.
- Após três segundos sem dado identificável: “resultado desconhecido”. Não há rejeição confirmada disponível neste contrato. Viagem, revisão, troca de domínio e desconexão descartam a espera local anterior.
- Só a última intenção é resumida na faixa; não existe histórico confiável de resultados por comando sem extensão autorizada.

## Critérios de aceite e testes

Política explícita, quatro modos de conjuração preservados, nove comandos acessíveis, estados legíveis, autoridade mantida e nenhuma regressão crítica. Casos e evidências em [TASK-224-TESTES.md](TASK-224-TESTES.md), imagens em [prévia TASK-224](skills-preview-task224/README.md).

## Riscos e compatibilidade

Sem estado autoritativo completo de stun/dash/LOS por intenção, a prontidão é uma avaliação parcial. Não foi alterado o risco A-04: nenhuma mudança em imunidade/materialização. A investigação aprofundada permanece nas TASKs 225–227, ainda não iniciadas. Proposta não implementada em [TASK-224-PROPOSTA-FEEDBACK-PROTOCOLO.md](TASK-224-PROPOSTA-FEEDBACK-PROTOCOLO.md).

## Plano de rollback

Comparar somente os cinco arquivos de produção alterados e helpers/testes relacionados com `baseline/server/src/` do checkpoint; remover individualmente os dois auxiliares novos se for reverter a integração. Restaurar apenas trechos desta entrega caso haja trabalho posterior. A documentação do Corujão IX tem cópia prévia de progresso/pendências. Não usar reset/clean; nenhum rollback de protocolo/save é necessário.

## Entregáveis e relatório final

Este plano, testes, relatório final, proposta separada e capturas; progresso/pendências atualizados. Sem commit, push, release ou publicação. TASK-225 não iniciada.
