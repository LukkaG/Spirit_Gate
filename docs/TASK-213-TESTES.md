# TASK-213 — testes e evidências

Refinamento posterior: cinco execuções dirigidas aprovadas, incluindo dois
clientes TCP e três resoluções; [adendo e evidências](CORUJAO-VIII-ADENDO-HUD.md).
Os números da suite completa abaixo pertencem à entrega anterior da TASK-213.

31/10/2026. Java 17, ferramentas/dependências já instaladas.

| Execução | Resultado |
|---|---|
| Suite Java completa | 255 fontes, 223 arquivos de testes; 692 executados, 670 aprovados, 22 abortos headless, zero falhas/skips |
| Complemento gráfico, dez classes | 30 execuções aprovadas, zero falhas/abortos/skips |
| Testes novos dirigidos | 2 testes de viewport/recursos e 1 jornada gráfica TCP, aprovados |
| Regressões dirigidas iniciais | HUD anterior (1), progressão TCP (2), câmera pixel-perfect (5), todos aprovados |
| Integridade | 4.405 arquivos baseline; 3.912 arquivos protegidos intactos; zero ausentes |
| Estabilidade dos fontes | Nenhuma alteração durante a rodada final |
| Whitespace | git diff --check sem erros nos arquivos alterados |

O complemento cobre os 22 abortos headless e repete oito casos já aprovados.
Não somar rodadas repetidas como casos únicos adicionais. As dez classes:
Task191LifecycleTest (7), Task212PlayableUiTest (1), Task213HudSwingTest (1),
RadminLauncherUiTest (1), SessionGameReconnectCloseTest (3),
Task201RadminCancellationTest (7), Task201SessionWindowFlowTest (1),
QuestBuilderTest (1), StudioGestureSafetyTest (1), StudioUsabilityTest (7).

## Provas específicas

- Dois clientes TCP reais, Reaper/Human, recebem estado e ficha do servidor e
  abrem a HUD de produção sobre os mapas sincronizados.
- Resoluções 1366×768, 1000×700 e 640×480: minimapa, perfil, inventário,
  livro, menu, configurações e slots ficam dentro da superfície da janela.
- Minimap pode ser ocultado/reaberto. Transformações de câmera/projeção têm
  ida/volta coerente, sem alterar coordenadas do mundo.
- HP 37/125 e energia 640/1000 mantêm textos corretos na apresentação nova.
  Snapshot ausente mostra aguardando, sem preencher dados fictícios.
- Botão de ataque envia a intenção e recebe evento HIT confirmado pelo servidor.
- Binding S pressionado/solto movimenta o personagem; o segundo cliente observa
  o movimento, demonstrando que o novo layout não rompe o input existente.
- Perfil, inventário, missão e menu abrem/fecham pela HUD. Configurações abrem
  o diálogo real; fechar sem Aplicar não cria arquivo de preferências.
- Ao desconectar, os quatro slots ficam desabilitados.
- Regressões completas incluem protocolo, inventário, progressão, combate,
  offline, Coop, publicação, rollback, colisões e persistência existentes.

A primeira execução de Task212PlayableUiTest falhou porque exigia o painel de
missão inline. O teste foi atualizado para abrir o livro/J e verificar a janela
real, conservando a prova da funcionalidade. O log anterior foi preservado em
Task212PlayableUiTest-before-layout-update.log. Nenhum teste foi desativado.

## Evidências e reprodução

`.task-checkpoints/TASK-213/corujao-viii/` contém baseline, fontes anteriores,
java-full-final.log, preservation-final.json, source-hashes-before-final.json,
task213-source.patch, testes dirigidos e ui/client-*-*.png.
O complemento está em `TASK-213-GUI/corujao-viii/verification-gui.json`.

Comando dirigido (uma linha):
`python tools/corujao7_verify.py TASK-213 com.projetok.multiplayer.Task213HudTest com.projetok.multiplayer.Task213HudSwingTest --gui --cycle corujao-viii`.
Suite completa: `python tools/run-local-tests.py`.

As capturas foram inspecionadas. Três PNGs históricos regenerados pela suite e
dois documentos de mapa com timestamp foram arquivados na evidência desta tarefa;
os arquivos dos checkpoints anteriores e as exclusões históricas foram preservados.
Assets, saves e todos os pacotes anteriores foram comparados por SHA-256.

Não houve repetição da suite Python inteira ou dos testes PowerShell: launcher,
atualizador e protocolo não foram alterados. O helper Python novo foi exercitado
pelos comandos reais de compilação/teste. Não houve teste físico entre dois
computadores nem aceite visual humano nesta execução.


## Adendo obrigatório de janelas e teclado - 31/10/2026

Implementado no V6: cinco painéis internos translúcidos, roteamento contextual de teclado e configurações com Aplicar/Cancelar. Base das TASKs 214-217 adiantada por determinação do novo adendo; seus escopos completos continuam pendentes. TASK-216 tem implementação parcial de apresentação e controles. Regressão: 695 testes Java (670 aprovados, 25 abortos gráficos), complementada por 33 execuções GUI aprovadas, sem falhas. Teclado físico e revisão visual humana permanecem pendentes. Ver [relatório do adendo](ADENDO-JANELAS-HOTKEYS-RELATORIO.md) e [PDF](Spirit_Gate_Adendo_Janelas_Hotkeys.pdf).
