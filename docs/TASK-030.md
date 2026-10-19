# TASK-030 - Estabilidade das animações

Objetivo: validar transições direcionais sem alterar combate.
Base aprovada: TASK-029, 231 JUnit e 25 Python.
Diagnóstico: sequenciador já trata prioridades e pausa por Freeze; não foi
encontrada regressão reproduzível que justifique alterar sua política.
Política existente preservada: morte vence tudo e usa FRONT; HIT vence ATTACK;
animações pontuais persistem até sua duração; direção nova reinicia o frame;
Freeze segura tempo/estado enquanto não há sinal novo; R limpa tracks; WALK
exige deslocamento real. RUN continua sem emissão porque não existe corrida real.
Contrato temporal aceita dt finito entre 0 e 0.1; valores grandes são recusados
explicitamente. Não há avanço silencioso ou alteração do relógio de combate.

Implementação: ampliados testes em DirectionalAnimationTest.java, sem mudança
funcional desnecessária. Cobrem deltas variáveis, ciclos completos, limites e
grandes/NaN/infinito, ataque caminhando, dano durante ataque, morte, direção e R.
Testes anteriores mantêm Freeze/retomada, KO final, sinais reais e fallback atlas.
Suíte completa e totais: TASK-030-qualidade.json, TASK-030-testes.txt e logs.
Reprodução: python tools/qualidade.py --offline --task 030
Diff/checkpoints padronizados; próximo passo TASK-031.
Limites: sem nova mecânica de corrida ou arte final. Homologação artística e
interativa humana permanece pendente. Aceite técnico depende dos logs aprovados.

## Resultado final observado
Validação local aprovada: 233 JUnit e 25 Python; zero falhas.
