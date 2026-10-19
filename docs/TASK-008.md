# TASK-008 - Obstáculos leves visíveis

18/10/2026. Implementação sobre a TASK-007, sem alterar os algoritmos de colisão, recuperação, dano ou habilidade.

## Decisão e cenário

DebugWorld.demo() e demo(RandomGenerator) criam a arena demonstrativa. DebugRuntime usa essa fábrica na inicialização e no reinício R; o launcher e os controles permanecem. Os construtores anteriores de DebugWorld preservam o mapa original para testes/replays e ferramentas existentes. Não foi criado menu ou atalho.

O demo mantém os três sólidos anteriores e acrescenta dois arbustos e um poste, todos SOFT_OBSTACLE. Hitboxes retangulares explícitas em unidades de mundo:

- Arbusto 1: x=70, y=420, largura=36, altura=40.
- Arbusto 2: x=160, y=420, largura=36, altura=40.
- Poste: x=250, y=425, largura=14, altura=30.

O mesmo descritor fornece a geometria à física e os dados visuais ao snapshot. SoftObstacleView informa tipo e retângulo; sua lista é imutável. O record visual Obstacle anterior continua com quatro campos, conservando sua projeção histórica. DebugPanel desenha arbustos verdes e poste com Java2D e destaca o retângulo real ao usar H. A arte não redefine colisões.

WASD e dash SOLID bloqueiam esses objetos; PHASE_THROUGH_ACTORS_AND_SOFT atravessa. Destino ocupado é corrigido pela implementação anterior. Paredes e limites continuam sólidos. Custo 35, duração 0,25 s, alcance 150 e cooldown 4 s preservados; a travessia não aplica dano. Nenhuma dependência ou sprite externo foi adicionado.

## Validação

Base: 171 testes aprovados. A primeira execução inicial de mvn clean test falhou ao remover EXE temporário somente leitura da TASK-007. O problema do build foi corrigido antes de implementar este cenário e a base passou novamente. Log da falha: TASK-008-inicial-limpeza-falhou.log; repetição aprovada: TASK-008-inicial.log.

Testes novos exercitam as formas reais do demo e a integração com o mundo, além da imutabilidade e renderização. A suíte final, resumo, captura e diff acompanham esta entrega. Testes e baselines anteriores não foram regravados.

AUTOMATIZADO APROVADO: mvn clean test final, 177 testes, zero falhas/erros/ignorados (171 anteriores e 6 de SoftObstacleDemoTest). Log TASK-008-final.log e resumo TASK-008-testes.txt. Captura TASK-008-demo.png gerada pelo renderizador real e inspecionada, mostrando passagem no arbusto e pouso após o poste. Sem sessão humana de jogo nesta etapa. Conferida diretamente a preservação dos 92 fontes de domínio e testes anteriores.

Arquivos modificados: DebugWorld, DebugRuntime, DebugSnapshot e DebugPanel; novo SoftObstacleDemoTest. Documentação, captura, PDF, logs e diff; ajuste da ferramenta de checkpoints para incluir .gitignore. Nenhum arquivo de combate, habilidade, colisão ou teste anterior foi alterado.

## Roteiro de três minutos

HOMOLOGACAO HUMANA PENDENTE. Abrir a arena pelo launcher; usar H para mostrar as hitboxes. Caminhar para a região ao sul do ponto inicial, perto de y=420. Tentar entrar nos dois arbustos e no poste com WASD: devem bloquear. Apontar o mouse além de cada objeto e pressionar Shift uma vez: o dash deve passar, gastar 35, respeitar a recarga e terminar fora da hitbox. Retomar WASD e observar que o objeto voltou a bloquear. Usar as paredes grandes como contraste: Shift continua bloqueado. Testar bordas, ataque básico e R; os três objetos leves devem reaparecer.

O demo não é um novo mapa de multiplayer. A fábrica original é mantida exclusivamente para preservar cenários anteriores e fornecer uma referência reproduzível. O pacote da TASK-007 permanece histórico; a distribuição consolidada será reconstruída na TASK-011.
