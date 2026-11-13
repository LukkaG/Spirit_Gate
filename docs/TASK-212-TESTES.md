# TASK-212 — testes finais

| Validação | Resultado comprovado |
|---|---|
| Java completo, 252 fontes/221 arquivos de testes | 689 executados; 668 aprovados; 21 abortos headless; 0 falhas/skips |
| Complemento Swing, nove classes | 29 execuções aprovadas; cobre 21 abortos e repete oito casos |
| Python completo | 97 testes aprovados; processo final saiu com código 0 |
| Atualizador PowerShell | 39 verificações aprovadas |
| Transporte GitHub simulado | 21 verificações aprovadas; sem acesso ao GitHub |
| Integridade adicional V6 | 4 execuções aprovadas: replay de equipamento/poção, cooldown e isolamento |
| Pacotes privados | 391 arquivos conferidos por papel; manifests comuns por SHA-256 |
| Smoke empacotado | Três processos com runtime/JAR dos pacotes, todos saíram 0 |
| Preservação | 2.805 arquivos baseline; 12 alterações previstas, zero ausentes; 2.320 arquivos protegidos intactos |

Os números gráficos não são somados como casos únicos adicionais. A suite
completa inclui regressões offline, Coop, saves, protocolo, publicação, colisão,
recuperação e gameplay. O complemento cobre cada classe abortada em headless.

Primeira regressão completa: 687 executados, 11 falhas relacionadas à troca
do baseline da fábrica histórica. Corrigida separando start/startExpanded,
sem desativar verificações de segurança. Testes dirigidos de publicação,
admissão e segurança passaram e a suite completa final aprovou sem falhas.
O log intermediário permanece como evidência, sem ser apresentado como aprovação.

O comando Python inicial via redirecionamento PowerShell terminou com código
de shell 1 apesar de “97 OK”, por tratamento de stderr esperado em teste negativo.
Reexecutado com captura subprocess stdout/stderr unificados: 97 OK, código 0.
O smoke de pacote inicial apontou nome de JAR errado no probe, não no pacote;
o probe foi ajustado ao nome normalizado e o smoke final passou.

Prova gráfica nova: duas janelas reais, mapa expandido, raças diferentes,
hotbar, missão, barras e abertura de inventário. Capturas inspecionadas em
`TASK-212/corujao-vii/ui/`. Provas de mapa em TASK-211.

Smoke empacotado executou runtime/bin/java.exe com app/spirit-gate-rpg.jar de
cada canal; dois clientes recebem ficha V6 e identidades distintas, e o modo
convidado é impedido de hospedar. Não executou separadamente o EXE nativo.
O launcher tem cobertura das suites Python, PowerShell e regressões Swing.

Evidências: java-full-final.log; TASK-212-GUI/corujao-vii/verification-gui.json;
python-tests-clean.log; updater/results.json; github-transport/results.json;
package-smoke-final/result.json; preservation-final.json; corujao7-source.patch;
new-source-files.json; source-hashes-before-final.json. Fontes estáveis durante
a rodada final e iguais às compiladas para o pacote.

Nenhum teste entre dois computadores/Radmin físico ou aceite artístico humano
foi declarado. Saves/PNGs e a distribuição oficial 0.201.0 permaneceram intactos.
Três capturas históricas regeneradas pela suite foram arquivadas na evidência,
mantendo as exclusões anteriores no diretório docs.
