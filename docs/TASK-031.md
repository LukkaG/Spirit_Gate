# TASK-031 - Controles, foco e mira

Objetivo: preservar entrada sob resize, zoom, câmera e perda de foco.
Base: TASK-030, 233 JUnit e 25 Python aprovados.
Diagnóstico: desativação já soltava held, porém mira persistia e perda de foco
sem desativação não tinha callback explícito. Implementação mínima:
configureKeys retorna rotina de liberação testável; limpa held, invalida mira e
publica NONE. Usada em windowDeactivated e windowLostFocus. Mantidos bindings,
pressão única de Q/Shift e fila FIFO, sem descartar casts já enfileirados.
Testes novos sem JFrame: soltar movimento/E, mira inválida após perda de foco,
nova pressão aceita, HUD/letterbox rejeitados em zoom 1-4 e resize.
Regressões anteriores verificam Q/Shift, restart e competição por vigor.
Arquivos: DebugLauncher.java e FocusControlsTest.java.
Reprodução: python tools/qualidade.py --offline --task 031
Totais, logs e resultado: TASK-031-qualidade.json e TASK-031-testes.txt.
Checkpoints/diff padronizados; próxima TASK-032.
Limites: eventos de janela são ligados ao callback real, mas alternância física
de janelas requer homologação humana. Uma transição anterior ao foco mantém sua
posição FIFO; não são apagados comandos para produzir aprovação artificial.

## Resultado final observado
Validação local aprovada: 235 JUnit e 25 Python; zero falhas.
