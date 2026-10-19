# TASK-033 - Feedback visual de combate

Objetivo: tornar impactos e derrotas mais perceptíveis usando eventos reais.
Base: TASK-032, 236 JUnit e 25 Python aprovados.
Diagnóstico: já existem EffectView emitidos pelos golpes e pulsos de gelo,
Freeze visual, gelo/rastros e feedback de bloqueio no HUD. Reutilizados esses dados
sem criar outro fluxo de eventos ou duplicar dano. Acrescentados anéis de impacto
aos marcadores existentes e marca transitória de derrota no fallback, somente
quando ator morto coincide com impacto real ainda vigente. Ambos usam remaining
do snapshot, que expira no tick da simulação. Renderizador não altera o mundo.
DebugSnapshot passa a copiar todas as listas, reforçando publicação imutável.
Não foi inferida atribuição de gelo a partir da proximidade de uma zona; sinais
existentes continuam sendo a fonte de verdade. Nenhuma probabilidade mudou.
Arquivos: DebugPanel.java, DebugSnapshot.java, CombatFeedbackPresentationTest.java.
Testes usam dano real, imutabilidade, render repetido sem dano e expiração por
ticks inclusive após derrota. Suite completa: TASK-033-qualidade.json e logs.
Reprodução: python tools/qualidade.py --offline --task 033
Checkpoints/diff padronizados; próxima TASK-034.
Limites: anel de derrota é fallback geométrico e depende do impacto ainda visível;
atlas aprovado mantém KO próprio. Sem novos sprites ou aprovação artística.
Homologação visual humana pendente.

## Resultado final observado
Validação local aprovada: 237 JUnit e 25 Python; zero falhas.
