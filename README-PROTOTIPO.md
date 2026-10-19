# Spirit Gate — Arena de debug jogável (Java 17)

Este protótipo isola uma arena local do MMORPG e **não substitui o login de produção**.
Usa PlayerFactory, MobFactory, VitalityService, RaceRegistry e CombatService do domínio
existente. Não exige PostgreSQL em execução, imagens externas nem uma nova dependência gráfica.

## 1. Executar no Windows

**Mais rápido:** dê duplo clique em `rodar-prototipo.cmd` na raiz do projeto.
O script executa os testes e abre a janela.

Alternativamente, abra o CMD na pasta `Projeto-K\server` e confirme `java -version` (17) e `mvn -version`.

```cmd
mvn clean test
mvn exec:java -Dexec.mainClass=com.projetok.prototype.DebugLauncher
```

Na primeira execução, Maven poderá baixar JUnit 5 e plugins. Mantenha a janela aberta.
Para sair, feche a janela. Não execute `KServer` ou `Kclient` para abrir esta arena.

## 2. Como jogar

- `W/A/S/D`: movimento livre 8 direções (diagonal normalizada).
- `ESPAÇO` (segurar): ataque desarmado, cone de 90°, cooldown de 0,38 s.
- `H`: mostrar/ocultar hitboxes, obstáculos e alcance de ataque.
- `R`: reiniciar arena após a morte ou a vitória.
- Encoste nos inimigos para que eles tentem atacar. Elimine os três espíritos.

O servidor local mantém a posição e resolve colisões com **paredes (círculo x
retângulo)**, **outros atores (círculo x círculo)** e **limites do cenário**.
A IA dos mobs persegue em um raio limitado e usa CombatService para causar dano.
As barras de HP e números de dano são derivados do estado do mundo (não desenhados por estimativa).

## 3. O que esta arena NÃO é

- Não é um multiplayer conectado ao Kryonet (execução local e isolada).
- Não autentica usuários ou persiste personagens no PostgreSQL.
- Não implementa zonas persistentes de servidor, loot, XP, navmesh ou pathfinding avançado.
- O loop é de prototipagem a 30 Hz, não o game loop definitivo de produção.

## 4. Separação de threads

- `DebugRuntime`: thread exclusiva de simulação, aplica `DebugInput` a cada tick.
- `DebugWorld`: valida movimento, colisão, alcance, cooldown e chama os serviços de domínio.
- `DebugSnapshot`: estado imutável enviado à GUI.
- `DebugPanel`: desenha o snapshot no EDT do Swing, 60 atualizações visuais/s.
- `DebugLauncher`: reúne o controlador e a janela.

## 5. Testes

Com Maven: `mvn test` (JUnit 5, incluindo `DebugWorldTest`).
O arquivo `DebugWorldSelfTest` oferece validação adicional sem bibliotecas externas.

## 6. Segurança do repositório original

`DatabaseManager` passou a exigir `SPIRIT_DB_URL`, `SPIRIT_DB_USER` e
`SPIRIT_DB_PASSWORD` via variáveis de ambiente. **A senha antes inserida no repositório
precisa ser rotacionada, inclusive porque a cópia histórica do Git pode continuar contendo-a.**
O `KServer` legado ainda compara senhas em texto puro e não autentica corretamente;
**não o exponha à internet**. Esse fluxo está fora da arena local.

## 7. Estado de validação

Estado consolidado da rodada TASK-007 a TASK-011: **202 testes aprovados** com
`mvn clean test`, incluindo repetição offline estável. O EXE portátil foi aberto
fora dos fontes usando seu Java integrado. A homologação em outro Windows e a
revisão humana jogando continuam pendentes. Veja
[relatório consolidado](docs/RELATORIO-FINAL-RODADA.md) e [changelog RC](docs/CHANGELOG-RC.md).


## TASK-001 ? Anima??o de ataque b?sico

Cada golpe aceito pelo cooldown mostra um arco de corte ciano/branco durante
0,18 s, inclusive ao golpear o ar. O arco avan?a e desvanece no cone frontal;
a posi??o e a dire??o (incluindo diagonais) s?o capturadas no in?cio do golpe.
Segurar Espa?o continua repetindo golpes a cada 0,38 s. Movimenta??o permanece livre.

`DebugWorld` publica um `AttackView` tempor?rio no snapshot imut?vel e
`DebugPanel` desenha o efeito com Java2D, sem assets adicionais. Alcance,
sele??o de alvo, dano e CombatService mant?m as regras existentes.

Valida??o: `mvn test` na pasta `server`. Os testes cobrem golpe no ar,
cooldown sem reiniciar o efeito, dire??o diagonal fixada, expira??o e novo golpe,
al?m de renderiza??o sem janela e regress?o do dano existente.
Para conferir visualmente, execute `rodar-prototipo.cmd`, pressione Espa?o parado,
mova nas quatro dire??es e diagonais, ataque e vire durante o corte; confira que
o efeito desaparece e que o dano continua ocorrendo apenas em alvos eleg?veis.


## Servicos reutilizaveis de movimento e colisao

O pacote `com.projetok.domain.movement` pode ser usado pela simulacao local ou
por um futuro servidor, sem Swing, DebugInput ou DebugSnapshot:

- `MovementService.direction(x, y)` normaliza a intencao de movimento.
- `MovementService.moveDirected(...)` calcula direção × velocidade × dt;
  `move(entity, dx, dy, radius, space)` delega um deslocamento pronto.
- `CollisionService.resolve(...)` resolve X antes de Y para preservar o deslizamento.
- `CollisionService.isFree(...)` consulta limites, circulo contra retangulo e
  circulo contra circulo. Tangencia e permitida; o proprio ator e mortos nao bloqueiam.
- `CollisionService.Space` recebe dimensoes, obstaculos do dominio e corpos com
  entidade e raio. As listas sao copiadas; posicoes e HP sao consultados ao vivo.
  Corpos mortos são ignorados; NPCs sem `HealthComponent` continuam sólidos.

O chamador fornece direção normalizada, velocidade e delta de tempo, mantém a direção
de olhar e executa os servicos na thread proprietaria das entidades. `DebugWorld`
coordena jogador, IA e combate e converte obstaculos do dominio para o snapshot.
O painel continua recebendo apenas dados visuais imutaveis.

As regras anteriores foram preservadas, inclusive a verificacao apenas da posicao
candidata por eixo (sem colisao continua para grandes deslocamentos). O prototipo
continua usando ticks de no maximo 0,1 s; o futuro servidor devera controlar seus
proprios passos de simulacao. Nao ha implementacao de multiplayer nesta extracao.

Validacao: `mvn test` na pasta `server`. `MovementServiceTest` cobre normalizacao,
limites, tangencia, cantos, deslizamento, ordem X/Y, atores vivos/mortos e posicoes
atualizadas. `MovementReplayTest` compara o estado completo de tres sequencias de
600 ticks com resultados capturados antes da extracao, incluindo movimento, IA,
combate e efeitos. Os testes anteriores da arena permanecem ativos.

### Entrega TASK-002

Validação final: `mvn clean test` com **15 testes aprovados**, sem falhas, erros
ou testes ignorados. Serviços documentados em português, com regressões de
movimento e combate executadas sem abrir janela Swing.

- [Decisões, arquivos alterados e resultados](docs/TASK-002.md)
- [Histórico técnico](docs/HISTORICO-TECNICO.md)
- [Documentação em PDF](docs/Spirit_Gate_TASK-002.pdf)

Regenerar o PDF: `python tools/gerar_pdf_task002.py` na pasta `Projeto-K`.

## TASK-003 - Vigor físico

O jogador começa com **100/100** de vigor e regenera **15 pontos/s** enquanto vivo.
Pressione **E** para um esforço provisório de **20 pontos**, sem dano. Segurar E
não repete o gasto: solte e pressione novamente. Se o saldo for insuficiente,
a ação é recusada sem consumo e o log informa o motivo. A regeneração ocorre
antes das ações do tick, sem atraso adicional; nunca ultrapassa 100.

Ataque básico e movimentação continuam gratuitos. Mortos não agem nem regeneram.
R reinicia com vigor cheio; manter E durante o reinício exige soltar e pressionar
novamente. Esses números são provisórios e não são balanceamento definitivo.

`StaminaComponent` mantém os limites e `StaminaService` controla consumo e
regeneração no domínio, sem Swing ou Kryonet. O construtor anterior de
`DebugInput` permanece compatível. Movimento, colisões e CombatService estão intactos.

Validação final: `mvn clean test`, **30 testes aprovados**, sem falhas ou erros.
Os três baselines anteriores do replay foram preservados mediante projeção
verificada dos campos antigos do snapshot.

- [Decisões, arquivos e limitações](docs/TASK-003.md)
- [PDF](docs/Spirit_Gate_TASK-003.pdf)
- [Diff da tarefa](docs/TASK-003.diff)
- [Resumo dos testes](docs/TASK-003-testes.txt)
- [Imagem da barra de vigor](docs/TASK-003-vigor.png)

Gerar o PDF: `python -B tools/gerar_pdf_task003.py` na pasta `Projeto-K`.

## Pendências técnicas implementadas após TASK-003

- Serviços de movimento rejeitam NaN, infinitos, geometria inválida e overflow antes de alterar posições.
- A fila preserva pressionamentos rápidos de E, movimento e ataque entre ticks; E mantido não repete consumo. Reinício é ordenado junto às entradas.
- O contexto de colisão é reutilizado e possui índice espacial, atualizado após cada movimento.
- `moveContinuous`/`resolveContinuous` estão disponíveis para deslocamentos rápidos sem tunelamento. A arena continua usando a resolução discreta anterior, preservando suas mecânicas.

Validação: `mvn clean test`, **53 testes aprovados**; baselines anteriores do replay mantidos.
Não há implementação de multiplayer. Os relatos de limitações das TASK-002/003
são históricos; consulte o estado atualizado abaixo.

- [Decisões, testes, arquivos e limites](docs/PENDENCIAS-IMPLEMENTADAS.md)
- [PDF](docs/Spirit_Gate_Pendencias_Tecnicas.pdf)
- [Diff](docs/PENDENCIAS.diff)
- [Resultados dos testes](docs/PENDENCIAS-testes.txt)

Gerar PDF: `python -B tools/gerar_pdf_pendencias.py` na pasta `Projeto-K`.

## TASK-004 - Criostase

Aponte o mouse dentro da arena e pressione **Q**. A zona de gelo custa **55 de
vigor**, tem cooldown de **3 s**, alcance de centro **240**, raio **85** e duração
de **2,5 s**. Um alvo mais distante é limitado ao alcance; mouse fora da arena é
recusado sem gasto. Segurar Q não repete: solte e pressione novamente.

A zona fica fixa e aplica **7 de dano ICE** nos instantes **0, 0,5, 1, 1,5 e 2 s**,
somente em mobs vivos cujas hitboxes intersectem o círculo, incluindo tangência.
Não há sexto pulso, congelamento ou lentidão. Paredes não bloqueiam a habilidade.
O rodapé mostra o cooldown; R limpa zona e recarga. Controles anteriores permanecem.

Validação: `mvn clean test`, **73 testes aprovados**, com os baselines anteriores
do replay preservados. Domínio sem Swing/rede; dano somente por CombatService.

- [Relatório e arquivos alterados](docs/TASK-004.md)
- [PDF](docs/Spirit_Gate_TASK-004.pdf)
- [Testes](docs/TASK-004-testes.txt)
- [Captura da zona](docs/TASK-004-criostase.png)
- [Diff](docs/TASK-004.diff)

Gerar PDF: `python -B tools/gerar_pdf_task004.py` na pasta `Projeto-K`.

## TASK-005 - Congelamento probabilístico

Cada pulso da Criostase que causar dano positivo a um mob sobrevivente oferece
**30% de chance de Freeze por 1 segundo**. Sucessos renovam para 1 s sem acumular;
falhas não alteram o efeito. O mob congelado fica azul-gelo, para de se mover e
continua atacando se estiver em alcance. Não há dano adicional nem Freeze no jogador.

O relógio dos efeitos avança antes dos pulsos, garantindo duração completa para
efeitos novos. `StatusEffectsComponent` guarda o estado; `StatusEffectService`
aplica/atualiza; `ProbabilityService` usa `RandomGenerator` injetável. O painel
somente desenha o snapshot. Os parâmetros e os cinco pulsos da Criostase permanecem.

Validação: `mvn clean test`, **105 testes aprovados**, incluindo os 73 anteriores
e seus baselines de regressão intactos.

- [Relatório e arquivos alterados](docs/TASK-005.md)
- [PDF](docs/Spirit_Gate_TASK-005.pdf)
- [Testes](docs/TASK-005-testes.txt)
- [Captura do Freeze](docs/TASK-005-freeze.png)
- [Diff](docs/TASK-005.diff)

Gerar PDF: `python -B tools/gerar_pdf_task005.py` na pasta `Projeto-K`.

## Colisão mais maleável: deslizamento lento

Ao ficar completamente bloqueado por uma quina externa ou pelo contorno de um
mob, o personagem tenta escorregar pela tangente a até **45% da velocidade**.
O auxílio também vale para mobs. Não reduz hitboxes, não empurra outros atores
e verifica o percurso para impedir travessias. Movimento livre e deslizamento
normal nas paredes mantêm sua velocidade. Freeze continua bloqueando movimento.

Paredes retas, contato frontal perfeitamente alinhado e cantos sem saída
continuam bloqueando quando não há direção tangencial válida.

Validação: `mvn clean test -q`, **118 testes aprovados**. O replay histórico usa
explicitamente o modo STRICT com os hashes antigos; os testes novos cobrem o
modo SOFT_SLIDE, ativo por padrão na arena.

[Detalhes e arquivos](docs/DESLIZAMENTO-COLISOES.md),
[testes](docs/DESLIZAMENTO-testes.txt) e [diff](docs/DESLIZAMENTO.diff).

## TASK-006 - Passo Relâmpago

Aponte o mouse dentro da arena e pressione **Shift**. O dash custa **35 de vigor**,
tem cooldown de **4 s** e percorre até **150 px em 0,25 s** na direção capturada,
com varredura contínua e rastro elétrico amarelo. A distância pode ser reduzida
por colisões. O mouse define a direção; não limita a distância quando está próximo.

O deslocamento começa no tick seguinte ao lançamento e substitui WASD durante
o dash, inclusive no último passo fracionário. Depois retorna à caminhada normal.
Não causa dano, não concede invulnerabilidade e não aplica status. Q e Shift
respeitam a ordem da fila e o mesmo vigor. Segurar Shift não repete a ação; R
limpa o estado sem reativar a tecla mantida.

Validação final após a participação do usuário: `mvn clean test`, **147 testes
aprovados**, incluindo os 118 anteriores com baselines preservados. O teste
de direção zero foi escrito pelo usuário, revisado e aprovado sem alterações.

- [Relatório, API real e participação validada](docs/TASK-006.md)
- [PDF](docs/Spirit_Gate_TASK-006.pdf)
- [Resultados](docs/TASK-006-testes.txt)
- [Captura](docs/TASK-006-passo-relampago.png)
- [Diff](docs/TASK-006.diff)

Gerar PDF: `python -B tools/gerar_pdf_task006.py` na pasta `Projeto-K`.

## TASK-006.1 - Colisão seletiva do dash

Na arena, o Passo Relâmpago atravessa atores e obstáculos classificados como leves.
Paredes, construções sólidas e bordas continuam bloqueando a varredura, considerando
o raio do personagem. WASD mantém todas as colisões normais. Os três obstáculos
anteriores continuam sólidos; a grama decorativa não ganhou colisão.

Se terminar dentro de um corpo ou obstáculo leve, o jogador recua pela trajetória
real até a saída livre mais próxima do destino. Se corpos móveis ocuparem toda
a rota, o HUD indica espera por saída livre e WASD aguarda uma mudança do espaço,
sem prolongar o dash ou renovar custo/cooldown. Dano recebido continua permitido.

O construtor anterior de `LightningStepService` mantém `CollisionPolicy.SOLID`;
a arena seleciona explicitamente `PHASE_THROUGH_ACTORS_AND_SOFT` no novo construtor.

Validação: `mvn clean test`, **169 testes aprovados**, com os testes e baselines
anteriores preservados. O teste de parede reservado ao usuário e a conferência
interativa ainda estão pendentes. Nenhuma TASK-007 foi iniciada.

[Relatório e API para sua participação](docs/TASK-006-1.md),
[PDF](docs/Spirit_Gate_TASK-006-1.pdf), [resultados](docs/TASK-006-1-testes.txt),
[captura](docs/TASK-006-1-colisao-seletiva.png) e [diff](docs/TASK-006-1.diff).

Gerar PDF: `python -B tools/gerar_pdf_task006_1.py` na pasta `Projeto-K`.

## Rodada TASK-007 a TASK-011 - RC para homologação

As cinco tarefas foram implementadas: Windows portátil com Java integrado,
demo com dois arbustos e um poste leves, HUD contextual e regressão determinística.
Resultado consolidado: **202 testes aprovados**, sem alterar os baselines anteriores.

O ZIP `dist/SpiritGate-0.11.0-rc1-20261018T220040Z-b2e659ed-portable.zip` contém a
pasta inteira do aplicativo. Extraia tudo e abra `SpiritGate.exe`, mantendo
`app/` e `runtime/` ao lado. A abertura local com Java integrado foi conferida;
outro Windows sem Java/Maven e a avaliação jogando aguardam homologação humana.

[Relatório consolidado](docs/RELATORIO-FINAL-RODADA.md),
[PDF consolidado](docs/Spirit_Gate_Relatorio_Final_TASK-007_011.pdf),
[changelog e hash](docs/CHANGELOG-RC.md) e
[roteiro por tarefa](docs/kit-homologacao/ROTEIRO-HOMOLOGACAO.md).

Reconstrução: `tools\build-windows-portable.cmd -Version 0.11.0-rc1 -TesterKitDirectory docs\kit-homologacao`.
O script usa o JDK 17 e executa testes antes de gerar o pacote. Não exige WiX/MSI.


## Rodada TASK-012 a TASK-016

ZIP canonico, pipeline de assets, animacao direcional com fallback e mapa local opcional.
Nenhuma arte final foi ativada: inventario de 228 PNGs, zero validados.
Suite consolidada: 216 testes; ver logs e relatorio para evidencias reais.

[Relatorio completo](docs/RELATORIO-FINAL-TASK-012_016.md),
[PDF](docs/Spirit_Gate_Relatorio_Final_TASK-012_016.pdf) e
[roteiro do tester](docs/kit-homologacao-012-016/ROTEIRO-HOMOLOGACAO.md).

Build: tools\build-windows-portable.ps1 -Version 0.16.0-rc1 -MavenRepository .maven-repository -TesterKitDirectory docs\kit-homologacao-012-016.
Python 3 (biblioteca padrao) e ferramenta de build/auditoria; nao dependencia do jogo.
Homologacao humana, outro Windows e arte final permanecem pendentes.
# Rodada TASK-017 a TASK-021 — 0.21.0-rc1

Relatórios individuais em `docs/TASK-017.md` a `docs/TASK-021.md`; kit humano em
`docs/kit-homologacao-017-021`. A janela aceita redimensionamento proporcional,
com nearest-neighbor e conversão de mira em coordenadas de mundo. HUD/margens
não recebem mira. Arena padrão continua 960x640; mapas maiores só nos testes.

Autoria: `python tools/revisar_sprites.py template NOVA_PASTA`. Revisão:
`python tools/revisar_sprites.py review RAIZ MANIFEST NOVA_PASTA`. Importação
Aseprite: `python tools/revisar_sprites.py aseprite PNG JSON NOVO_MANIFEST`.
Exportação aceita json-array, nomes ACTION.DIRECTION.INDEX, 100ms, 64x64,
sem trim/rotate/pack. Prévia e relatórios não aprovam arte.

Pacote visual opcional v1: `python tools/pacote_visual.py RAIZ MANIFEST NOVA_PASTA`.
Manifest JSON exige packageVersion=1, assetId estável e um assets[] character
validado pelo pipeline TASK-013, com review.origin e approvedSha256 do arquivo
exato aprovado pelo humano. Build admite `-VisualManifest MANIFEST -VisualRoot RAIZ`.
Sem pacote, ou com pacote inválido, o launcher usa fallback. Nenhuma arte
existente foi aprovada. Zoom 1 a 4: API `DebugPanel.setCameraZoom`, sem atalho novo.

Build: `powershell -NoProfile -ExecutionPolicy Bypass -File tools/build-windows-portable.ps1 -Version 0.21.0-rc1 -MavenRepository .maven-repository -TesterKitDirectory docs/kit-homologacao-017-021`.

# Rodada TASK-022 a TASK-036 - candidata 0.36.0-rc1

Qualidade completa: `python tools/qualidade.py --offline --task local` na raiz
Projeto-K (Java 17, Maven e Python; dependências no cache local).
Mapas: `python tools/mapas.py create exemplo.properties` e
`python tools/mapas.py validate exemplo.properties` (compilar antes).
V1 mantém 960x640; V2 é reservado a mapas ampliados até 8192x8192, 4096 objetos
por lista. Camadas 0-4 atrás e 5-10 à frente dos atores. Decoração não colide.
F1 abre ajuda; F2 salva diagnóstico local explícito com dados por whitelist.
Não há envio automático ou arte final aprovada. RUN não cria corrida de gameplay.
Relatório desta rodada em docs/RELATORIO-FINAL-TASK-022_036.md; kit humano em
docs/kit-homologacao-022-036. Homologação em outro Windows continua pendente.

