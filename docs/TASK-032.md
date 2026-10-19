# TASK-032 - HUD e ajuda

Objetivo: tornar controles e estados existentes compreensíveis ao testador.
Base aprovada: TASK-031, 235 JUnit e 25 Python.
Diagnóstico: HUD já publicava HP, vigor, recargas, indisponibilidade, Freeze e
colisão sólida; evitada duplicação de regras. Adicionado painel F1, sem conflito
com atalhos, com controles, significado dos recursos, dash seletivo e estados reais.
Ajuda usa o mesmo canvas escalonado da viewport e não pausa a simulação.
Arquivos: DebugPanel.java, DebugLauncher.java, HelpPresentationTest.java.
Teste verifica binding, alternância, valores publicados, três dimensões e igualdade
com runtime de referência. Regressões HUD anteriores permanecem intactas.
Comando: python tools/qualidade.py --offline --task 032
Totais reais, logs e aceite técnico: TASK-032-qualidade.json e TASK-032-testes.txt.
Checkpoints/diff padronizados; próxima TASK-033.
Limites: fonte acompanha escala da janela, podendo ficar pequena em janelas muito
reduzidas; legibilidade final e homologação humana continuam pendentes. Sem novos
custos, cooldowns, comandos de gameplay ou dependências.

## Resultado final observado
Validação local aprovada: 236 JUnit e 25 Python; zero falhas.
