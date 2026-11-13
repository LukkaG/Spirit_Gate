# TASK-224 — Relatório final

Entrega local de 02/11/2026, branch `beco`, HEAD `6d72c65`. Escopo exclusivo da autorização de continuidade; **TASK-225 não iniciada**.

## Resultado para o jogador

- TAB seleciona alvos apenas com foco no viewport. Na HUD, TAB/SHIFT+TAB navegam; ENTER usa o controle focado. SPACE conserva o básico durante gameplay mesmo com outro botão destacado. Não há nova tecla nem outro roteador.
- Nove comandos permanecem: básico, dois Shunpos, poção e cinco skills. Default e remapeamento são preservados.
- Alvo exibe nome, tipo, HP e distância. A habilidade inspecionada informa requisitos e avisos estimados LONGE/LOS/CHEIO. Aliados derrotados continuam destinados ao revive; NPC não vira inimigo.
- Frontal continua smartcast imediato. Área tem centro/raio, alcance, limites e aviso de possível obstáculo; mouse, WASD/setas, ENTER/mesma tecla e ESC preservados.
- Faixa navy/ciano torna o feedback visível sem abrir Habilidades: enviada/aguardando, evento ou estado recebido, resultado desconhecido e estados locais de indisponibilidade. Desconexão cancela preparação; contexto anterior não é atribuído à nova sessão.

## O que os avisos significam

A interface não conhece todo o estado impeditivo autoritativo nem recebe um ACK por intenção. Portanto, não anuncia aceite ao enfileirar TCP nem transforma silêncio em rejeição. Eventos/cooldowns do próprio jogador são descritos como dados observados, com a limitação de correlação explícita. A faixa resume a última intenção; não é histórico confiável por comando.

Alcance e visibilidade são prévias do snapshot/revisão recebidos. Podem mudar até o tick do servidor. Avisos LONGE/LOS não impedem o envio por si; requisitos locais anteriores de nível, energia, cooldown e tipo de alvo continuam aplicados. A cor do retículo não é confirmação do servidor.

O que falta para feedback completo está na [proposta técnica separada](TASK-224-PROPOSTA-FEEDBACK-PROTOCOLO.md), sem implementação ou negociação nova. Stun, imunidade, marcações individuais de Vento e outros dados ausentes não foram inventados no cliente.

## Arquivos alterados

Cinco arquivos de produção existentes: GameInputRouter, NetworkSkillControls, NetworkGameHud, NetworkTargetPanel e RpgNetworkClientLauncher. Dois auxiliares novos: NetworkSkillPreview e NetworkSkillSubmission. Todos estão no pacote multiplayer.

Testes novos: Task224SkillFeedbackTest e Task224CastingGuiTest. Ajustados GameInputRoutingTest e TargetCombatGuiTest para a política contextual; helpers SwingKeys e GamePanelsIntegrationTest passaram a aguardar foco real. Plano e critérios detalhados em [TASK-224.md](TASK-224.md).

## Evidências

Suíte geral: **939 casos, zero falhas/erros, 54 skips headless**. Complemento gráfico: **106 execuções, sem falhas/erros/skips**. As rodadas específicas/capturas adicionais cobrem seleção distante, obstáculo, mudança de região, perda de alvo por desconexão e reconexão sem mira/tecla herdada. Agregado final e XML em `verification.json`/`final-reports/` do checkpoint, sem somar repetições.

O agregado final reúne **939 aprovados em 250 classes**, sem skips remanescentes. Integridade: 152 PNGs de skills preservados, nenhum arquivo protegido alterado fora dos nove previstos (cinco de produção e quatro de teste/helper), 89 capturas anteriores restauradas e 21 novas imagens na pasta da TASK-224.

[Matriz dos vinte requisitos e histórico dos testes](TASK-224-TESTES.md). [Capturas](skills-preview-task224/README.md). As ocorrências intermediárias de foco AWT/fixtures estão registradas, não omitidas nem tratadas como falha do servidor.

## Compatibilidade e risco A-04

Catálogo, dano/cura, energia/regeneração, recargas, níveis, engine, servidor, colisão, imunidade, movimentação especial, sprites e protocolo não foram alterados. Nenhuma instalação, migração, mudança de save, launcher, World Studio, PvP ou publicação. Offline/Coop e protocolos anteriores foram exercitados na regressão existente. Mudanças de navegação contextual se restringem ao roteador da partida multiplayer e conservam o acesso às ações anteriores.

A-04 permanece registrado e com o mesmo contrato: eventual espera sem limite para materializar Quatro relâmpagos e fusível visual. Não houve investigação aprofundada ou mudança desse estado nesta task. A suíte atual não reproduziu falha crítica; isso não comprova que o risco condicional não exista.

## Pendências e reversão

Radmin entre computadores, teste físico/hardware distinto e revisão estética humana permanecem pendentes. Feedback por intenção/estado completo exige aprovação arquitetural específica. A investigação de física/status/combo permanece nas próximas tarefas não iniciadas.

Checkpoint: `.task-checkpoints/CORUJAO-IX/TASK-224/`. Restaurar somente os trechos/arquivos desta entrega depois de comparar trabalho posterior; auxiliares novos podem ser removidos junto da integração. Não usar reset/clean. Progresso/pendências anteriores e fontes têm cópia no baseline. Sem commit, push, release ou build privada.
