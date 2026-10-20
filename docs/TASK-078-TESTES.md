# TASK-078 — Testes

Comando final: `python tools/qualidade.py --offline --task 078`, na raiz, exit 0. Executa Maven offline no POM `server/pom.xml`, com repositório local do projeto, `clean test`, e depois todos os unittest Python existentes.

| Resultado final | Testes | Falhas | Erros | Ignorados |
| --- | ---: | ---: | ---: | ---: |
| Java | 307 | 0 | 0 | 0 |
| Python | 25 | 0 | 0 | 0 |

São os 297 casos Java anteriores mais dez novos casos; nenhuma remoção ou alteração dos testes antigos. Logs em `docs/TASK-078-java.log`, `TASK-078-python.log` e `TASK-078-qualidade.json`. XMLs finais copiados para `.task-checkpoints/TASK-078/junit/`.

## Testes adicionais

`RpgPlayerSpritesTest` cobre:

1. As quatro direções, Walk 1→2→1 e parada no Idle da direção correta.
2. Reinício em mudança de estado/direção e estabilidade em repaints do mesmo snapshot.
3. Duração configurável independente dos 100 ms do atlas oficial.
4. Ausência de animação quando a colisão impede deslocamento, preservando última direção efetiva.
5. Fallback do conjunto em arquivo ausente, corrompido e substituído por frame válido de outro nome.
6. Capitalização exata, inclusive em filesystem Windows.
7. Renderização dos 12 frames com pés ancorados e transparência do canvas.
8. Cache após remoção dos arquivos de origem, mundo não modificado, inimigo/morte recusados pelo hook.
9. Opção desativada por padrão.
10. Comparação pixel a pixel entre renderer sem hook e hook que recusa desenhar, preservando o caminho anterior.

## Primeira falha preservada

A primeira execução teve 307 casos Java, uma falha e zero erros; Python passou em 25. O novo teste de borda esperava x=18 exatamente. O serviço existente termina o movimento discretizado em x≈18,1667 e recusa o próximo passo; não promete chegar exatamente à tangência. O teste novo foi corrigido para verificar posição dentro de [18,19), ausência de deslocamento na nova tentativa e Idle. Não se alterou velocidade, colisão ou teste anterior. Primeiros logs/XMLs preservados em `.task-checkpoints/TASK-078/first-test-run/`.

## Cliente real e regressões

Cliente Swing real executado com sprites e também em modo geométrico. A sonda externa usa os bindings reais de movimento, snapshots reais e captura `JFrame.printAll`; não injeta posições, recompensas ou resultados na simulação. As evidências não são montagens de tela nem aprovação humana. Primeiro ensaio capturou direções, mas não terminou a etapa do obstáculo após perda/reset de entrada; foi interrompido e preservado em `visual-first/` e `visual-first-run.log`. O harness passou a reenviar o binding durante navegação e recebeu limite de execução. Segunda execução terminou com exit 0 e `VISUAL_PROBE_FINISHED`.

Regressão completa inclui salvamentos, inventário, equipamento, missões, combate, arena e rede local. `CorruptBackupProtectionTest` continua passando. Código de mundo/runtime/save/protocolo e testes anteriores têm os mesmos hashes; somente apresentação foi alterada. Não foi repetida a homologação física LAN, que permanece pendente desde a TASK-071.
