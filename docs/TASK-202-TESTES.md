# TASK-202 — testes e evidências

31/10/2026. Java 17; dependências JUnit já presentes no cache, sem instalação.
Compilação de 244 fontes e 210 arquivos de testes.

| Execução | Resultado |
|---|---|
| `python tools/run-local-tests.py com.projetok.multiplayer` — rodada final | 169 executados, 168 aprovados, 1 aborto por GUI, zero falhas/skips |
| `LocalJunitRunner com.projetok.prototype.rpg`, headless | 79 executados, 68 aprovados, 11 abortos por GUI, zero falhas/skips |
| Complemento Swing, seis classes | 23 executados/aprovados, zero abortos/falhas/skips |
| Integridade assets/saves | 712 arquivos conferidos por SHA-256, nenhuma divergência |
| `git diff --check` | Sem erros de whitespace |

O complemento gráfico cobre os 12 abortos e repete dez casos já aprovados.
Classes: SessionCharacterCreationTest (4), Task201SessionWindowFlowTest (1),
Task201RadminCancellationTest (7), RadminLauncherUiTest (1),
SessionGameReconnectCloseTest (3), Task191LifecycleTest (7).
Não somar repetições como novos testes únicos. Não foi executada toda a suite
do repositório; foram selecionados os pacotes multiplayer e prototype.rpg.

Os três testes novos verificam:

- Cada quadro Idle/Walk de cada direção, incluindo atlas emprestados,
  alinhamento ao solo, metadados, acessórios e ausência das ações sem arte.
- Reaper ausente sem empréstimo silencioso Human, ingresso indisponível e
  fallback geométrico informado; ausência de ambos os atlas.
- Servidor TCP real com dois clientes, Human e Reaper: ambos recebem as raças
  autoritativas e renderizam pixels diferentes com suas respectivas cadências.

A primeira rodada multiplayer teve uma falha no teste novo: o aviso ainda
não descrevia o fallback geométrico. O texto foi corrigido; a repetição do
pacote inteiro passou. O log inicial foi mantido, sem ser contado como aprovação.

Evidências em `.task-checkpoints/TASK-202/corujao-vii/`: `multiplayer-final.log`,
`rpg-headless.log`, seis arquivos `*-gui.log`, `preservation-before.json`,
`preservation-final.json`, fontes anteriores, arquivos novos e patch da mudança.
Runner e classes finais: `server/target/local-tests/run-71881onq/`.
O runner gráfico usa seleção por classe e `-Djava.awt.headless=false`.
A suite RPG headless foi executada na compilação anterior à correção do aviso;
o complemento gráfico usa a compilação final, incluindo o teste de atlas ausente.

Sem homologação física entre dois computadores/Radmin ou aceite artístico
humano. As provas TCP são locais e os testes de imagem desenham em BufferedImage.
Sem alteração do EXE oficial ou publicação de pacote.
