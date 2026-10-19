# TASK-009 - Feedback contextual de habilidades

18/10/2026. Base: TASK-008, 177 testes aprovados em mvn clean test antes das alterações. Escopo restrito à comunicação do estado real das habilidades; regras de dano, custo, duração, regeneração, cooldown, movimento e fila preservadas.

## Divisão de responsabilidades

DebugSnapshot acrescenta views imutáveis para Criostase e Passo Relâmpago. SkillHudMapper produz dados renderizáveis na simulação, incluindo tecla, nome, custo, vigor, cooldown, estado e feedback. Os estados distinguem READY, COOLDOWN, INSUFFICIENT_STAMINA, DEAD, ACTIVE e AWAITING_LANDING. A prioridade é morto, espera de saída, dash ativo, recarga, vigor insuficiente e pronto. Feedback transitório da última tentativa fica separado do estado atual.

DebugWorld usa os resultados reais de cast para publicar aceitações e recusas. O feedback dura 1,2 s no relógio da simulação, e não usa timers de Swing. A expiração acontece no tick; snapshots anteriores continuam imutáveis. lastEvent foi preservado para a regressão histórica. Manter a tecla não cria novas notificações: a fila e os bindings continuam sendo os já existentes.

O movimento do dash publica uma flag informativa Step.blocked, calculada antes da eventual recuperação da saída. Ela compara deslocamento pedido e efetivamente aplicado pela física, com tolerância numérica, sem alterar posições ou relógios. Na arena, a política seletiva ignora atores e leves; por isso o feedback se refere a sólido ou borda. O texto não afirma que o alvo do mouse está atrás de uma parede nem confunde recuo por arbusto com bloqueio por parede.

Os construtores anteriores de Step continuam disponíveis. Nenhum algoritmo de colisão foi alterado. O HUD só desenha as strings, estados, valores e cores publicados; não consulta componentes, sorteia, consome vigor ou modifica o mundo. As linhas anteriores de Q/Shift foram substituídas por apresentação contextual, preservando controles e espaço visual.

## Validação e limites

Testes devem comprovar mapeamento, lançamento aceito e recusado, cooldown, vigor insuficiente, morte, espera/liberação, reinício, feedback transitório, colisão real e renderização pura. Também é verificado que recuperar um destino dentro de arbusto não produz um aviso falso de parede. Não se usam screenshots pixel a pixel como oráculo de gameplay.

AUTOMATIZADO APROVADO: mvn clean test final, 189 testes, zero falhas/erros/ignorados. HudSkillFeedbackTest acrescenta 12 casos; todos os 177 anteriores continuam verdes. Logs TASK-009-inicial.log e TASK-009-final.log; resumo TASK-009-testes.txt. Captura TASK-009-hud.png gerada e inspecionada em quatro quadros: pronto, sem vigor, ativo e bloqueio real. Renderizar não alterou o mundo.

Espera/liberação foram exercitadas no mapper com estados controlados, além dos testes anteriores de recuperação pendente do domínio. O teste novo de reinício cria outro mundo pela fábrica; a integração determinística do comando R será coberta na TASK-010. Feedback expira após no máximo um tick adicional de arredondamento de float. Nenhum teste depende de tempo do sistema ou JFrame.

Arquivos: novo SkillHudMapper e HudSkillFeedbackTest; alterações em DebugWorld, DebugSnapshot, DebugPanel e LightningStepService somente para publicar informação de contato. Relatório/PDF, testes/logs, captura e diff. Os construtores anteriores de Step foram preservados; fórmulas e clocks de gameplay não foram alterados.

A label de colisão deliberadamente não diferencia parede de borda quando a evidência disponível só comprova um bloqueio sólido. A espera de saída continua obedecendo à regra anterior: se toda a trajetória estiver ocupada, o estado pode persistir até haver mudança no espaço ou reinício. O HUD não tenta resolver essa situação.

HOMOLOGACAO HUMANA PENDENTE: conferir legibilidade das linhas Q/Shift, custo e vigor, transição de pronto para recarga, recusa por recurso, aviso breve de alvo inválido, indicador durante dash e bloqueio real em parede/borda. Usar R e verificar limpeza. Sem sons, assets, novas teclas ou novas habilidades.
