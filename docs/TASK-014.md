# TASK-014 - Animacao direcional com fallback

Baseline limpo: 202 testes, zero falhas/erros/ignorados. Arquivos previstos: DebugWorld, DebugRuntime, DebugPanel, DebugLauncher, sequenciador, atlas e testes. Risco: sincronizacao de apresentacao e sinais visuais; regras do dominio preservadas.

DirectionalAnimation avanca por dt na thread de simulacao; IDLE/WALK/ATTACK/HIT/FAINT e quatro direcoes. RUN existe no contrato, mas nunca e emitido pelo mundo atual, que nao possui corrida. Movimento observado define facing dos mobs, facing autoritativo orienta player; empate de eixo estavel e ultima direcao parada. Ataque e dano positivo publicam sinais no ponto real do combate; morte real tem prioridade e usa KO frontal. Freeze pausa o frame; R recria a linha temporal.

Presentation publica atomicamente snapshot historico e lista imutavel de frames. API e construtores antigos preservados; campos e baselines de DebugSnapshot intactos. Renderer recebe apenas valores, sem avancar tempo nem consultar mundo mutavel. Timer Swing preexistente apenas repinta; nenhum timer de animacao foi adicionado.

ValidatedSpriteAtlas carrega exportacao Properties VALIDADO da TASK-013, confere SHA e grade/recortes, usa nearest-neighbor, pivot e offset constantes; leitura de arquivo e cache somente na carga. Frame ausente retorna ao debug original. Sem arte aprovada, launcher permanece com fallback. Nada foi declarado visual final integrado.

Testes novos cobrem quatro direcoes, timing, loop, pausa, freeze/retorno, R, ataque, sinais reais de dano e morte, KO limitado ao ultimo frame, ausencia de corrida, rejeicao de asset sem aprovacao ou adulterado, frame ausente, pivot e render sem mutacao. Captura TASK-014-sintetico.png e FIXTURE SINTETICA magenta, nao arte aprovada do jogo. Os tres replays historicos e demais regressoes executam sem editar baselines.

Falha intermediaria de compilacao em teste: variavel capturada por lambda nao era efetivamente final; corrigida no novo teste, registro TASK-014-falha-intermediaria.log. Execucao parcial: 209 testes verdes; teste adicional de Freeze levou a suite final a 210, zero falhas/erros/ignorados, registrada em TASK-014-final.log e TASK-014-testes.txt.

Reproducao: mvn.cmd -Dmaven.repo.local=.maven-repository -f server/pom.xml clean test. PDF, diff e checkpoints locais anexos. Nenhuma regra de colisao, vigor, dano, cooldown, FIFO, rede ou protocolo alterada.

O QUE FOI REALMENTE EXECUTADO: suite completa, replays e render sintetico. O QUE AINDA EXIGE O USUARIO: arte de personagem 64x64 aprovada e revisao humana de leitura visual. Fallback permanece ativo.
