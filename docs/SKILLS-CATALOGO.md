# Habilidades — oito kits e quarenta técnicas

O pedido do desenvolvedor define as mecânicas abaixo. `RpgSkillCatalog` mantém as quarenta definições únicas; `RpgSkillEngine` as executa somente em sessões sintéticas autoritativas, usando energia, colisão, HP e ledger de contribuição já existentes. O catálogo não concede domínio e não altera saves. O servidor continua exigindo domínio escolhido pela progressão existente, disponível no nível 15.

Este catálogo não constitui homologação visual, de balanceamento, multiplayer físico ou Radmin. A integração de rede e apresentação deve ser validada com seus testes próprios.

## Convenções e valores provisórios

- Tempos em ticks de 30 Hz; 30 ticks = 1 segundo. Alcance e raio usam unidades do mundo. Energia está em unidades inteiras nesta tabela; o executor converte para os décimos usados pelo mundo.
- **Todos os custos, recargas, danos, curas, alcances, raios, velocidades e durações são provisórios**, exceto os requisitos explícitos: prisão de água por 2 segundos, três cortes de vento, até quatro impactos da corrente de raio, quatro dashes da habilidade Raio 5 e cinco tiros sentry.
- Dano listado é por impacto, antes do bônus de atributos e demais regras aplicáveis. Sangramento/queimadura: proposta de 3 de dano a cada 30 ticks, por 90 ticks; renova sem empilhar relógios do mesmo lançador/tipo/alvo/geração. Lentidão: proposta de preservar 50% da velocidade. Stun: proposta de 30 ticks.
- `ROOT` impede deslocamento e permite ataques/habilidades. `STUN` impede movimento e ações ofensivas. Dashes não são permitidos durante ROOT/STUN. Áreas e controles nunca transferem efeitos para a próxima geração de um mob derrotado.
- Projéteis têm percurso temporal, com proposta de 18 unidades por tick, em vez de aplicar dano ao lançar. Skillshots testam posições atuais dos inimigos no segmento; obstáculos bloqueiam a aplicação. Dashes avançam por varredura contínua, sem aceitar posição final do cliente.
- LINE e CONE usam a direção confirmada do personagem no servidor; coordenadas do mouse não substituem esse facing. GROUND preserva o ponto selecionado, validado por distância, limites do mapa e visibilidade.
- `SELF` acompanha o usuário nos golpes próximos/vórtices. `GROUND` permanece na área selecionada. `LINE` usa uma direção e um segmento. `CONE` parte do apoio atual do jogador. `TARGET` exige alvo exato e geração; curas híbridas distinguem aliados de mobs. `ALLY` aceita aliado, incluindo o próprio usuário quando aplicável; Reviver exige outro personagem derrotado.
- Curas Luz 1/5 ferem somente mobs. Não causam dano a jogadores. Não há autorização implícita para combate entre jogadores neste conjunto.

## Domínios e compatibilidade

| IDs existentes de domínio | Kit consultado | IDs das técnicas |
|---|---|---|
| `water` | `water` | `water_1` a `water_5` |
| `wind` | `wind` | `wind_1` a `wind_5` |
| `thunder` | `thunder` | `thunder_1` a `thunder_5` |
| `fire` | `fire` | `fire_1` a `fire_5` |
| `darkness_debuff`, `darkness_control` | `spirit` | `spirit_1` a `spirit_5` |
| `bow_long`, `bow_short` | `aetheri` | `aetheri_1` a `aetheri_5` |
| `light` | `light` | `light_1` a `light_5` |
| `martial` | `martial` | `martial_1` a `martial_5` |

Os aliases não duplicam habilidades e não renomeiam IDs persistidos. `forDomain("none")` retorna vazio. `all()`, `get(id)`, `forKit(kit)` e `forDomain(domain)` retornam definições e coleções imutáveis.

## Água

| ID / nome | Alvo e comportamento | Efeito | Alcance / raio | Energia / recarga | Dano / cura |
|---|---|---|---|---|---|
| `water_1` Corte de água | TARGET; corte que viaja até o inimigo | Lentidão 60 ticks | 240 / 0 | 12 / 45 | 20 / 0 |
| `water_2` Corte de maré | SELF; corte corpo a corpo em área presa ao usuário | Lentidão 60 ticks | 0 / 82 | 16 / 75 | 22 / 0 |
| `water_3` Prisão de água | TARGET; prisão permite atacar e usar habilidades | ROOT **60 ticks, requisito do pedido** | 220 / 0 | 20 / 180 | 0 / 0 |
| `water_4` Vórtice de água | SELF; giro da espada e redemoinho; 4 pulsos a cada 10 ticks | Sem efeito adicional | 0 / 100 | 25 / 180 | 9 por pulso / 0 |
| `water_5` Investida de água | LINE; corte frontal sem alvo, atravessa mobs; duração 8 ticks | Um acerto por inimigo atravessado | 240 / 22 | 25 / 150 | 30 / 0 |

## Vento

| ID / nome | Alvo e comportamento | Efeito | Alcance / raio | Energia / recarga | Dano / cura |
|---|---|---|---|---|---|
| `wind_1` Corte de vento | TARGET; corte que viaja até o alvo | Sem efeito adicional | 260 / 0 | 12 / 45 | 22 / 0 |
| `wind_2` Três cortes de vento | TARGET; **3 projéteis**, lançados nos ticks 0, 3 e 6 (0,1s entre cortes) | Sangramento 90 ticks após contato | 240 / 0 | 20 / 120 | 10 por impacto / 0 |
| `wind_3` Passo cortante | LINE; corte frontal sem alvo, atravessa mobs; duração 8 ticks | Um acerto por inimigo atravessado | 240 / 20 | 22 / 120 | 28 / 0 |
| `wind_4` Vendaval de repulsão | LINE; três lâminas frontais viajam até o primeiro contato e empurram inimigos próximos | Checkpoint 150 ticks **a partir do acerto**, exclusivo do lançador; errar não concede retorno | 240 / 70 | 22 / 150 | 16 / 0 |
| `wind_5` Vendaval de retorno | SELF, sem seleção; puxa todos os inimigos marcados pelo empurrão anterior ainda ao alcance | **Exige contato de `wind_4` anterior**; consome o conjunto de marcas uma vez; revalida geração e paredes | 300 / 70 | 18 / 120 | 16 / 0 |

Checkpoint vencido, de outro lançador ou de outra geração de alvo não permite a habilidade 5. Repulsão e atração respeitam cenário e corpos móveis; a posição do checkpoint não teleporta a vítima.

## Raio

| ID / nome | Alvo e comportamento | Efeito | Alcance / raio | Energia / recarga | Dano / cura |
|---|---|---|---|---|---|
| `thunder_1` Corte de raio | TARGET; corte elétrico que viaja até o alvo | Sem efeito adicional | 240 / 0 | 12 / 45 | 22 / 0 |
| `thunder_2` Lança de raio | LINE; lança em linha reta | STUN 30 ticks | 280 / 18 | 20 / 120 | 24 / 0 |
| `thunder_3` Corrente de raio | TARGET; **até 4 impactos**, intervalo 8 ticks; procura outros inimigos | Sem outro alvo, repete no atual com 75% do dano anterior | 240 / 130 entre alvos | 25 / 150 | 20 inicial / 0 |
| `thunder_4` Investida relâmpago | LINE; corte frontal sem alvo, atravessa mobs; duração 8 ticks | STUN 30 ticks no contato real, uma vez por inimigo | 240 / 20 | 25 / 150 | 30 / 0 |
| `thunder_5` Quatro relâmpagos | SELF; invisível, sem colisão e imune a dano por 32 ticks; quatro cortes no encerramento | STUN 30 ticks; quatro pulsos em todo o círculo, respeitando paredes | 0 / 110 | 40 / 270 | 14 por pulso / 0 |

A corrente prefere inimigos ainda não visitados, com desempate por distância e ID. Na ausência de outro alvo elegível, reduz progressivamente o dano no mesmo sobrevivente; encerra se não restar vítima viva da geração válida. O circuito de quatro dashes usa quatro segmentos formando uma volta; não é uma teletransportação instantânea nem um círculo matematicamente contínuo. Uma parede ou corpo bloqueia somente o avanço daquele trecho; os demais trechos continuam. O contato provisório considera raio do jogador + raio 19 do mob + rastro elétrico 16, totalizando 47 com o jogador padrão de raio 12. A linha de visão é obrigatória; o raio visual 110 não causa dano irrestrito através de obstáculos.

## Fogo

| ID / nome | Alvo e comportamento | Efeito | Alcance / raio | Energia / recarga | Dano / cura |
|---|---|---|---|---|---|
| `fire_1` Corte de fogo | TARGET; corte que viaja até o alvo | Sem efeito adicional | 240 / 0 | 12 / 45 | 22 / 0 |
| `fire_2` Fissura ardente | LINE; fissura no chão em linha reta, área por 90 ticks | Queimadura 90 ticks | 260 / 24 | 20 / 120 | 22 inicial / 0 |
| `fire_3` Vórtice de fogo | SELF; vórtice móvel, 4 pulsos a cada 10 ticks | Queimadura 90 ticks | 0 / 100 | 25 / 180 | 9 por pulso / 0 |
| `fire_4` Erupção de fogo | LINE; frente de fissura viajante e grande erupção somente ao alcançar a extremidade ou a parede | Sem efeito adicional | 280 / 85 na erupção | 30 / 210 | 25 por fase / 0 |
| `fire_5` Chuva de cortes de fogo | GROUND; 5 pulsos a cada 12 ticks na área selecionada | Queimadura 90 ticks | 280 / 100 | 40 / 270 | 10 por pulso / 0 |

## Spirit

| ID / nome | Alvo e comportamento | Efeito | Alcance / raio | Energia / recarga | Dano / cura |
|---|---|---|---|---|---|
| `spirit_1` Poça espiritual | GROUND; poça por 60 ticks, 4 pulsos a cada 15 ticks | ROOT 30 ticks por aplicação | 240 / 85 | 22 / 150 | 8 por pulso / 0 |
| `spirit_2` Teia espiritual | LINE; teia como skillshot | ROOT 60 ticks | 260 / 22 | 18 / 120 | 0 / 0 |
| `spirit_3` Feixe espiritual | LINE; feixe de luz que causa dano | Sem efeito adicional | 280 / 20 | 20 / 90 | 30 / 0 |
| `spirit_4` Prisão espiritual | GROUND; prisão em área por 60 ticks | ROOT 60 ticks; **sem dano** | 240 / 95 | 25 / 180 | **0** / 0 |
| `spirit_5` Areia movediça | GROUND; área por 120 ticks | Lentidão renovada de 30 ticks; **sem dano** | 260 / 110 | 25 / 180 | **0** / 0 |

Prisões e areia reavaliam a presença de inimigos na área; não causam dano indireto por terem raio ou animação. Não foi acrescentado fogo amigo aos antigos domínios de trevas.

## Aetheri

| ID / nome | Alvo e comportamento | Efeito | Alcance / raio | Energia / recarga | Dano / cura |
|---|---|---|---|---|---|
| `aetheri_1` Flecha precisa | TARGET; uma flecha | Sem efeito adicional | 280 / 0 | 10 / 36 | 22 / 0 |
| `aetheri_2` Sentinela | SELF; imóvel por 120 ticks/4s; SPACE dispara flecha target; conjurar novamente cancela | Sem munição fixa; intervalo 10/(1+min(AGI,100)/100) ticks | 280 / 0 | 22 ao ativar / 150; cancelar não cobra | 12 por tiro / 0 |
| `aetheri_3` Chuva de flechas | GROUND; 5 ondas, cada uma cai por 12 ticks antes do impacto no chão | Dano nos ticks 12, 24, 36, 48 e 60; dissipação ao chegar | 300 / 100 | 30 / 180 | 10 por pulso / 0 |
| `aetheri_4` Flecha de energia | LINE; grande flecha de energia azul frontal que para no primeiro contato | Sem efeito adicional; não concede stun | 320 / 28 | 25 / 150 | 34 / 0 |
| `aetheri_5` Leque de flechas | CONE; sete flechas viajantes em trajetórias diferentes, cobrindo aproximadamente 40° | Cada flecha para no primeiro corpo; uma vítima recebe dano uma vez por lançamento | 220 / 80 de abertura lateral | 30 / 180 | 28 por vítima / 0 |

Sentinela revalida alvo, geração, alcance e intervalo a cada tiro. A ativação não exige alvo e não dispara sozinha. Mais agilidade aumenta a quantidade de tiros possível na mesma janela; a fração do intervalo é preservada, usando a regra de atributos existente. Cancelamento, expiração, saída, morte ou troca de domínio liberam sua imobilidade; cancelar não limpa stun/root independentes. Os números provisórios permitem 12 tiros com AGI 0 e até 24 com AGI 100 se o usuário atacar continuamente.

Fora da Sentinela, o básico de arco na aplicação 7 é frontal, para no primeiro corpo e usa alcance, dano e recarga básica, sem energia nem recarga da técnica 1. Selecionar um inimigo não o transforma em target. A técnica 1 mantém seu alvo próprio. Todas as animações de Aetheri usam energia azul, com a versão de gelo preservada no pacote anterior.

## Human — Luz

| ID / nome | Alvo e comportamento | Efeito | Alcance / raio | Energia / recarga | Dano / cura |
|---|---|---|---|---|---|
| `light_1` Cura básica | TARGET; aliado/usuário recebe cura, mob recebe dano | Dano limitado a mobs | 220 / 0 | 12 / 60 | 20 / 30 |
| `light_2` Cura em área | SELF; cura usuário e aliados próximos | Sem dano | 0 / 110 | 25 / 150 | 0 / 24 |
| `light_3` Reviver | ALLY; revive aliado derrotado no apoio em que caiu | Exige apoio livre; não teleporta nem restaura energia | 160 / 0 | 40 / 900 | 0 / 40 |
| `light_4` Purificação | ALLY; remove status negativos de controle | Limpa ROOT, STUN e SLOW de sessão | 220 / 0 | 18 / 180 | 0 / 0 |
| `light_5` Cura superior | TARGET; versão mais forte da cura básica | Dano limitado a mobs | 240 / 0 | 30 / 210 | 40 / 65 |

As habilidades ofensivas deste conjunto aplicam dano periódico somente em mobs; não foi criada uma fonte artificial de DoT em jogadores para simular purificação. O executor de cura evita reviver implicitamente um morto; somente Reviver tem essa finalidade.

As curas 1/5 recusam um aliado com vida cheia antes de consumir energia ou iniciar recarga, com aviso na ficha. Cura em área também recusa quando não houver aliados feridos e elegíveis. Sem alvo, as curas híbridas usam o próprio personagem; aliado selecionado recebe cura, mob selecionado recebe dano, NPC inválido não vira autocura. Os eventos de cura e revive são emitidos no apoio do destinatário para a apresentação do feixe vertical.

## Human — Martial Artist

| ID / nome | Alvo e comportamento | Efeito | Alcance / raio | Energia / recarga | Dano / cura |
|---|---|---|---|---|---|
| `martial_1` Soco preciso | TARGET; soco no alvo selecionado | Sem efeito adicional | 65 / 0 | 10 / 36 | 24 / 0 |
| `martial_2` Sequência de socos | TARGET; 4 socos, intervalo 4 ticks | Sem efeito adicional | 65 / 0 | 18 / 100 | 9 por impacto / 0 |
| `martial_3` Impacto sísmico | SELF; chute no chão e fissuras em área | Sem efeito adicional | 0 / 110 | 22 / 150 | 28 / 0 |
| `martial_4` Body slam | TARGET; agarrão próximo e impacto no chão | STUN 30 ticks | 55 / 35 | 25 / 180 | 34 / 0 |
| `martial_5` Combo de punhos e chutes | CONE; 6 impactos a cada 4 ticks, partindo do jogador | Sem efeito adicional | 170 / 95 de abertura lateral | 35 / 240 | 8 por impacto / 0 |

## Validação e reversão

Na revisão V4, projéteis e skillshots são consumidos pelo primeiro corpo inimigo ou obstáculo: o servidor publica o contato e a apresentação substitui o disparo por uma animação de impacto sem loop. Fim de alcance sem contato não produz impacto falso. Feixes/teias também param no primeiro corpo. As fissuras continuam sendo faixas de solo: abrem progressivamente até o alcance ou parede, atingem ao longo do caminho e não são consumidas por um mob. A corrente de raio conserva seus saltos próprios.

Vento 2 emite três cortes independentes do apoio atual do lançador, com geração e alcance revalidados a cada lançamento. Chuva de flechas publica separadamente queda e chegada ao chão; cada onda aplica um único pulso de área, sem multiplicar o dano pelas cinco flechas visuais. Os novos impactos e a arte detalhada de Martial/Água ficam na sobreposição `refined-v4`; as fontes anteriores foram preservadas.

No refinamento de 02/11/2026, as linhas passaram a testar somente o segmento realmente percorrido em cada tick. Um inimigo que entra atrás de uma frente já passada não recebe um acerto atrasado da linha inteira. Fissuras começam no lançador; sua frente avança a 18 unidades por tick, a trilha permanece depois do percurso e a erupção final só nasce na chegada real. O contato de um corte com a borda da hitbox pode ocorrer antes da chegada ao centro do alvo.

O formato de eventos foi preservado: `from != to` representa lançamento/trecho; `from == to` representa impacto ou fase local. Em Fogo 2/4, o evento local indica o término real do percurso, permitindo ao cliente manter a trilha e só então apresentar a área final. As sete trajetórias do leque são sete lançamentos confirmados, sem mensagem de rede adicional.

`RpgSkillCatalogTest` verifica unicidade, aliases, imutabilidade e os requisitos semânticos do pedido. `RpgSkillEngineTest` inclui lançamento das quarenta técnicas e casos de tempo de voo, geração antiga, prisão versus stun, corrente, DoT, checkpoint, sentry, cura, revive, purificação, dash e cancelamento.

Os arquivos anteriores à alteração do runtime foram copiados para `.task-checkpoints/SKILLS40/baseline/`, preservando os diffs que já existiam no checkout. Reversão deve restaurar somente os arquivos desta entrega a partir desse checkpoint, após conferir se não receberam trabalho posterior. Não usar `git reset` para isso.

O estado anterior ao refinamento V2 está em `.task-checkpoints/SKILLS40/refinamento-v2/baseline/`. Para reverter somente esse refinamento, comparar com esse baseline mais recente, preservando a correção anterior do deslocamento de inimigos.
