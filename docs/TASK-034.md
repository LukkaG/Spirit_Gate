# TASK-034 - Diagnóstico local explícito

Objetivo: pacote pequeno e revisável para relatos de bugs, sem coleta contínua.
Base aprovada: TASK-033, 237 JUnit e 25 Python.
Diagnóstico: não existia exportação técnica; copiar logs de build exporia caminhos.
Implementação: DiagnosticReport usa lista fechada de campos e F2 abre seletor
para salvar ZIP com único diagnostico.json. Operação CREATE_NEW recusa sobrescrita.
Inclui versão, família OS, arquitetura, versão JVM, janela, mapa DEFAULT/LOCAL,
etapa, ticks, contagem de atores, HP, gameOver e estados de habilidades.
Nenhuma rede, telemetria, leitura de ambiente/configuração, arquivo privado,
nome de ator, caminho local, mensagem/stack de erro ou gravação de entrada.
Erros são representados por marcador genérico; detalhes livres são deliberadamente
omitidos. JSON pode ser revisado antes de compartilhar; nada é enviado.
Arquivos: DiagnosticReport.java, DebugLauncher.java, DebugPanel.java e teste.
Testes: exclusão de nomes/caminhos/segredos injetados em evento e exceção, whitelist,
única entrada ZIP, dimensão inválida e não sobrescrita.
Reprodução: python tools/qualidade.py --offline --task 034
Resultados reais em TASK-034-qualidade.json, TASK-034-testes.txt e logs.
Checkpoints/diff padronizados; próxima TASK-035.
Limites: versão de desenvolvimento será consolidada na TASK-036. Não coleta crash
automaticamente. Seletor F2 requer teste humano; API de exportação foi automatizada.
Para relato, anexar passos de reprodução manualmente, após revisão pessoal do ZIP.

## Resultado final observado
Validação local aprovada: 239 JUnit e 25 Python; zero falhas.
