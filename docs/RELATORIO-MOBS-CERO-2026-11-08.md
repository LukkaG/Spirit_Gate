# Integração local dos mobs e Cero — 08/11/2026

## Resultado

Importados Guardiao, Alado, Rastreador, Tecela, Predador e Sentinela da pasta
`C:/Users/lucas/OneDrive/Desktop/Novas Sprites/Mobs`, preservando os originais.
Sprites nativas de 64x64, quatro direções, idle e cinco quadros de ataque.
Os seis IDs `mob_*` ficam disponíveis no catálogo do World Studio, na região
`forest`, e no renderizador multiplayer. Os spawns existentes não foram alterados.

Alado e Sentinela usam Cero básico a até 240 px. A simulação valida alcance,
visibilidade e alvo vivo; o cliente apenas apresenta o resultado. A carga acompanha
o instante do terceiro quadro: Alado 220 ms (7 ticks) e Sentinela 320 ms (10 ticks),
arredondados para o tick seguinte a 30 Hz. Os demais tipos conservam a IA corpo a corpo.
O antigo `sentinel` continua separado e corpo a corpo.

A saída do Cero foi corrigida por ancoragem e recorte no renderizador: a carga
fica à frente da boca; o feixe e sua ponta começam na bola e avançam. O efeito
não é desenhado atrás da origem. Carga, ponta, feixe e impacto reutilizam a arte
fornecida, sem redesenhar os corpos ou sobrescrever imagens aprovadas.

## Como testar

Execute `testar-mobs.cmd` na raiz. A arena é descartável e não usa saves, banco,
servidor externo ou publicação. WASD/setas movem o alvo laranja; ESPAÇO aplica
um golpe próximo; R reposiciona o alvo e zera o dano recebido. O alvo é imortal.
Mobs derrotados reaparecem. Esta arena usa EnemyService e MobSprites reais, mas
o movimento do alvo é simplificado e não homologa colisão ou controles multiplayer.

Para mapas multiplayer, os novos tipos podem ser selecionados como referências
ENEMY do editor. Nenhum mapa foi publicado ou promovido automaticamente.
Clientes precisam ser recompilados com os novos recursos para mostrar a arte.

## Arquivos

- `server/src/main/resources/rpg/mobs/`: seis sheets, manifests e quatro sheets direcionais do Cero.
- `rpg/content.properties`, `enemies.properties`, `loot.properties`: registros novos.
- `rpg/EnemyService.java`: carga e ataque básico à distância restritos aos dois novos IDs.
- `multiplayer/RpgSessionWorld.java`: sinal de animação nos flags existentes dos atores.
- `multiplayer/RpgNetworkClientLauncher.java`: integração do renderizador dos novos tipos.
- `prototype/rpg/MobSprites.java`: corpos, tempos e Cero; sem decisões de dano.
- `rpg/MobPlaytest.java`, `tools/launch-local.ps1`, `testar-mobs.cmd`: arena executável.
- `MobCeroTest.java`, `MobSpritesTest.java`: testes focalizados.

## Validação

JDK Temurin 17.0.20.1. Compilação das 277 fontes com `tools/compile-local.ps1`.
JUnit compilado pelo executor local, sem instalar dependências:

- `PYTHONIOENCODING=utf-8 python tools/run-local-tests.py com.projetok.rpg`: 300 aprovados.
- `LocalJunitRunner com.projetok.prototype.rpg`, com diretório de trabalho `server`: 70 aprovados, 11 abortados por headless.
- `LocalJunitRunner com.projetok.multiplayer`, com diretório de trabalho `server`: 282 aprovados, 34 abortados por headless.

Total: 652 aprovados, 45 abortados por ausência de interface, zero falhas.
O pacote de sprites foi repetido após a adição do impacto. Capturas offscreen
inspecionadas: `temp/mobs-playtest.png` e `temp/mobs-cero-playtest.png`.
O teste raster verifica ausência de pixels atrás da bola; os de IA verificam
carga, alcance, obstrução e preservação do sentinel antigo. `git diff --check`
não apontou problemas nos caminhos envolvidos.

Tentativas iniciais do executor tiveram problemas de encoding cp1252 e de
diretório de trabalho; as execuções finais acima corrigiram essas condições.
Não houve validação física de Radmin, launcher nativo ou partida remota.

## Compatibilidade, pendências e reversão

Sem alterações de mensagens, serialização, versão de protocolo, schema ou saves.
Sem mudanças nos spawns da campanha, no offline/Coop existente, no launcher de
distribuição ou em publicação. Branch `beco`, HEAD inicial `13b91dc`; checkout já
possuía muitas alterações locais, preservadas. Não houve commit ou push.

Atributos provisórios: 100 HP, ataque 7, velocidade 65, aggro 300, leash 500,
respawn 240 ticks; sessões multiplayer com conteúdo mantêm seu piso existente
de 10000 HP. Sem XP e sem loot até definição de balanceamento. Os manifests
originais são conservados como evidência; o Alado veio marcado como melee, mas
o pedido atual define seu comportamento em runtime como ranged Cero.

Faltam avaliação manual da arte, balanceamento e teste com dois clientes.
Para reverter, remover apenas os arquivos novos e os trechos desta integração,
preservando os diffs anteriores dos arquivos compartilhados. Não usar restore
global neste checkout. Próximo passo sugerido: testar a arena e ajustar os
offsets de boca, se necessário, antes de escolher spawns definitivos.


## Adendo ? caminhada e voo

Adicionadas as seis sheets de movimento e seus `movimento.json`, copiadas da
mesma pasta externa sem alterar os originais ou as sheets de ataque aprovadas.
Cinco mobs usam `andar_384x256.png` (100 ms/quadro); Alado usa
`voar_384x256.png` (80 ms/quadro). Todas t?m seis quadros, quatro dire??es e
piv? [32,59]. Os offsets de registro j? est?o incorporados aos PNGs fornecidos.

`MobSprites.java` seleciona movimento apenas quando h? deslocamento entre
estados confirmados, preserva prioridade do ataque, reinicia o ciclo ao come?ar
a mover e volta ao idle ao parar. O primeiro snapshot n?o simula deslocamento.
A integra??o vale para o renderizador multiplayer e para a arena local j? criada.
Sem altera??es na IA, velocidade, alcance, Cero, protocolo, saves ou spawns.

Valida??o: `PYTHONIOENCODING=utf-8 python tools/run-local-tests.py
com.projetok.prototype.rpg`: 71 aprovados, 11 abortados por headless, zero falhas.
Novo caso em `MobSpritesTest.java` compara pixels dos frames reais nas quatro
dire??es dos seis tipos, verifica parada, prioridade de ataque e fechamento dos
loops de 600/480 ms. Build local Java 17 tamb?m executado. Testes f?sicos e
Radmin permanecem pendentes. Revers?o: remover somente os recursos de movimento
e os trechos deste adendo no carregamento/renderizador/teste, mantendo a integra??o
anterior dos mobs e Cero. Pr?ximo passo: avalia??o visual pela arena local.


## Adendo ? velocidade e colis?o

Pedido: aumentar um pouco a velocidade e adicionar colis?o. Os seis novos IDs
`mob_*` passaram de 65 para 80 unidades/s (+23%). Alcan?e, dano, cooldown e
anima??es n?o foram alterados. A patrulha mant?m seu fator original de 30%.

EnemyService agora comp?e os corpos vivos da regi?o no espa?o de movimento
somente dos novos tipos. Usa a varredura cont?nua existente, raio 19 e refer?ncias
reais das entidades, sem empurrar ou atravessar outros mobs. O ajuste final ao
ponto de origem tamb?m verifica se o destino est? livre. Os tipos legados mant?m
seu fluxo anterior. No multiplayer, os corpos dos jogadores e o cen?rio j? eram
fornecidos pelo encontro; a lacuna era a colis?o dos mobs entre si.

Na arena MobPlaytest, o alvo passa pelo MovementService (raio 12), em vez de mover
por atribui??o direta. H? colis?o bilateral com mobs vivos, limites e um obst?culo
retangular central exclusivo deste teste. Reposicionamento por R e respawn
respeitam a ocupa??o. Mortos n?o bloqueiam. N?o h? novo pathfinding: um mob pode
parar atr?s de um bloqueio at? haver caminho direto livre.

Arquivos: enemies.properties, EnemyService.java, MobPlaytest.java e novo
MobCollisionTest.java. Testes de converg?ncia verificam aus?ncia de sobreposi??o
entre mobs e alvo durante 300 ticks; tamb?m se verificam velocidade, libera??o de
passagem ao morrer e movimento do jogador contra mobs e parede. O primeiro trajeto
do teste foi corrigido porque encontrava o Predador antes da parede (colis?o real,
n?o falha de implementa??o).

Java 17: build local aprovado; LocalJunitRunner com.projetok.rpg: 303 aprovados;
LocalJunitRunner com.projetok.prototype.rpg: 71 aprovados, 11 abortados por headless.
Zero falhas nas execu??es finais. Classes/testes em server/target/local-tests/run-cv5a0f5q.
Sem valida??o f?sica ou Radmin. Nenhuma mudan?a em protocolo, saves, publica??o
ou spawns de campanha. Nenhum commit/push. Revers?o: restaurar apenas o valor 65
dos seis novos registros e retirar os trechos deste adendo, preservando o trabalho
anterior. Pr?ximo passo: avaliar contato e ritmo na arena atualizada.


## Adendo ? mobs presos em colis?es

Causa verificada: a persegui??o usava apenas dire??o direta e repetia a inten??o
contra o mesmo corpo/parede. O slide existente n?o escolhe lado em contato frontal
sim?trico. Implementada busca A* limitada em `rpg/MobNavigation.java`, usada por
EnemyService somente nos seis IDs `mob_*` e somente ap?s avan?o inferior a 20%
do passo solicitado. Mantidos velocidade 80, colis?o s?lida, dano, alcance e Cero.

A busca usa grade de 24 px, at? 1800 retiradas da fila por tentativa e intervalo
m?nimo de 15 ticks entre buscas. Cada trecho ? validado pelo CollisionService,
com raio 19 e corpos atuais. Rotas ficam apenas em mem?ria, s?o revalidadas ante
bloqueios e descartadas quando o alvo muda mais de 24 px ou no respawn. Aproxima??o
de alvo ocupado para a at? 42 px sem entrar no corpo, dentro do alcance corpo a
corpo j? existente (46 px). Nenhuma altera??o no motor de colis?o ou em pol?ticas
legadas. Root/stun continuam respeitados; n?o h? empurr?o ou atravessamento.

`MobCollisionTest.java` agora exige contorno de mob imobilizado, al?m de manter
checagem de n?o sobreposi??o. Casos novos: contorno de parede e passagem selada
sem teleport. Os testes de converg?ncia e controles da arena continuam passando.
Valida??o: build Java 17 aprovado (278 fontes); executor local do pacote RPG:
305 aprovados, zero falhas/abortados. Classes em
`server/target/local-tests/run-bcwzxz68`. `git diff --check` nos caminhos alterados.

Limita??es: busca limitada e discretizada, sem garantia para labirintos grandes
ou passagens estreitas incompat?veis com a grade; bloqueio completo continua
s?lido. Carga com muitos mobs e Radmin ainda n?o homologados. Sem altera??o em
saves/protocolo/publica??o/spawns. Revers?o: retirar MobNavigation e os campos e
chamadas de desvio no EnemyService, preservando velocidade e colis?es anteriores.
Pr?ximo passo: avalia??o manual na arena atualizada.


## Adendo ? sa?da aut?noma atr?s da Sentinela

Reproduzido com jogador s?lido parado em 460,300, Sentinela atirando parada em
300,300 e Guardi?o em 27 posi??es frontais/obl?quas (?ngulos 120 a 240 graus,
passo 15; raios 40, 50 e 70). A busca anterior rejeitava aproxima??es v?lidas ao
jogador: RpgSight recebia tamb?m o pr?prio corpo de destino e a visibilidade
ficava bloqueada antes do centro do alvo. O ensaio anterior registrou mobs presos.

MobNavigation agora exclui somente o ator viajante e o corpo exatamente no destino
da consulta de vis?o usada para avaliar a chegada. Todos os corpos continuam
presentes na valida??o de cada trecho f?sico; a Sentinela e outros bloqueadores
n?o s?o ignorados. Sem altera??es nas consultas de combate ou regras de dano.

EnemyService tamb?m monitora o avan?o ao seguir a rota: ap?s seis ticks com avan?o
inferior a 20% do passo, descarta o plano e libera nova busca. Esse estado ?
ef?mero e zerado no respawn. A busca continua limitada; o jogador n?o precisa
mudar de dire??o para disparar a recupera??o. Mantidos velocidade 80, raio,
Cero, colis?o s?lida, protocolos e saves. Alterados apenas MobNavigation.java,
EnemyService.java e MobCollisionTest.java nesta etapa, al?m deste relat?rio.

Novo teste mant?m jogador fixo e Sentinela im?vel por 240 ticks em cada um dos
27 cen?rios, verifica separa??o f?sica a cada tick e exige chegada e ATTACK do
Guardi?o. Todos passaram. Sonda independente MobEscapeProbe repetiu os casos sem
registrar presos depois da corre??o. Testes RPG: 306 aprovados, zero falhas ou
abortados (run-yjw8eilq). Build Java 17 aprovado, 278 fontes. Sem valida??o f?sica
Radmin. A arena atualizada foi aberta para avalia??o manual.

Continuam os limites de grade e or?amento da navega??o; uma passagem fechada n?o
permite fuga f?sica. Revers?o: retirar somente o filtro de vis?o de chegada e o
contador de falta de avan?o adicionados neste adendo, preservando demais mudan?as.
Pr?ximo passo: conferir o caso relatado e encontros com m?ltiplos bloqueadores.
