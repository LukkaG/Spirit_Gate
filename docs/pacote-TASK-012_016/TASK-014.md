# TASK-014 - Maquina de animacao direcional com fallback

**Prioridade P1 | Somente apresentacao, sem mudar mecanicas**

## Objetivo
Preparar no prototipo o controle de estados de animacao para player/mobs em quatro direcoes, aproveitando as views imutaveis e o pipeline de TASK-013.

## Implementacao autorizada
1. Inspecione `DebugWorld`, `DebugRuntime`, `DebugSnapshot`, `DebugPanel` e os controles; preserve API e baselines.
2. Defina estado visual dirigido pela simulacao (IDLE/WALK/ATTACK/HIT/FAINT; RUN apenas quando houver **sinal real** de corrida; nao crie sprint para justificar arte). Distinga direcao FRONT/BACK/LEFT/RIGHT, selecionada de forma estavel de inputs/movimento ou ultimo facing.
3. Sequenciador deterministico por tick/dt, sem `System.currentTimeMillis`, sem timers Swing, sem mudar dano, colisao, stamina, cooldown, ataque ou rede.
4. Introduza renderer de sprites por manifest **apenas para assets VALIDADO da TASK-013**; desenhe com `nearest-neighbor` e pivô constante. Caso nao haja arte de personagem aprovada, renderize o debug atual (fallback) e teste o sequenciador com assets sinteticos de teste.
5. Evite depender da posicao visual da cabeca/pes para mudar a hitbox; a geometria continua no dominio.
6. Frames de HIT devem vir de evento/estado real, e KO de morte real; nao inferir dano por cor/transicao no painel.

## Aceite
- Testes para quatro direcoes, timing e sequencia, pausa/retorno, R, ataque, hit e KO, incluindo frame ausente/fallback.
- Sem alocacoes excessivas por frame nem estado mutavel lido diretamente do renderizador.
- Comparacao de snapshots e 3 replays historicos sem mudancas injustificadas.
- `mvn clean test` aprovado, screenshot sintetico ou real distinguido claramente, diff e PDF.
