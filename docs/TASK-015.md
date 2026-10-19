# TASK-015 - Mapa local e colisao separada

Baseline limpo: 210 testes, zero falhas/erros/ignorados. Arquivos previstos: LocalArenaMap, factory opcional DebugWorld, seletor DebugLauncher, DebugPanel e testes. Risco: carga de dados de geometria; variante isolada e comparada com referencia a cada tick.

Formato Properties schemaVersion=1 com limites 960x640, spawn, lista de aparencias (ID, posicao, tamanho, camada e cor sintetica) e descritores fisicos independentes. Categorias SOFT_OBSTACLE e SOLID_OBSTACLE viram retangulos explicitos no dominio; WORLD_BOUNDARY e um unico descritor do limite do Space, sem converter a arena inteira em obstaculo. Rejeita contagens excessivas, numeros nao finitos, schema/chaves desconhecidos, IDs/chaves duplicados, limites invalidos, spawns ocupados/sobrepostos e obstaculos iniciais sobrepostos.

Mapa demonstrativo docs/mapa-demo.properties conserva geometria dos tres solidos e tres leves e spawn historico. Aparencias maiores que corpos mostram a separacao. Renderer com tiles sinteticos de 32x32 em cache, nearest-neighbor e aviso na captura. Nenhum PNG de Karakura ativado, pois TASK-013 teve zero validados. Formato e demonstracao nao equivalem ao cenario artistico final.

DebugWorld.demo() e padrao da janela preservados. Seletor explicito: DebugLauncher --map CAMINHO. No Maven: mvn.cmd -Dmaven.repo.local=.maven-repository -f server/pom.xml exec:java -Dexec.mainClass=com.projetok.prototype.DebugLauncher "-Dexec.args=--map ../docs/mapa-demo.properties" (o caminho depende do diretorio de execucao; prefira absoluto). R recria o mesmo mapa por factory; nada de banco, rede ou editor visual.

Seis testes novos: carga e repeticao, rejeicoes, spawn, dimensao visual independente da colisao, replay da variante vs demo incluindo dash, reinicio deterministico e render sem mutacao. Comparacao de snapshots por tick confirma fisica identica ao demo. Suite completa final: 216 testes, zero falhas/erros/ignorados; replays e baselines historicos preservados. Captura TASK-015-placeholders.png e demonstracao SINTETICA, sem arte final integrada.

Falhas intermediarias preservadas em TASK-015-falha-intermediaria.log: uma fixture usava spawn valido como negativo (corrigida para y=0); o teste de R detectou ausencia da amostra visual do spawn apos reset. Corrigido DebugRuntime para inicializar o sequenciador com o snapshot do novo mundo antes do primeiro tick, sem alterar mecanicas.

Evidencias: TASK-015-inicial.log, TASK-015-final.log, TASK-015-testes.txt, PDF, diff e checkpoint local. Reproducao: mvn.cmd -Dmaven.repo.local=.maven-repository -f server/pom.xml clean test.

O QUE FOI REALMENTE EXECUTADO: carga offline, rejeicoes, snapshots/replays, reinicio e render sintetico. O QUE AINDA EXIGE O USUARIO: homologar nova variante e fornecer/aprovar tiles; o padrao nao foi trocado pela variante.
