# Corujão IX — matriz auditada das 40 skills

TASK-223 somente. Referência: 02/11/2026, branch `beco`, HEAD `6d72c65`. Valores extraídos de classes compiladas nesta auditoria; nenhum valor foi alterado.

## Como interpretar

- **Funcional e testada**: execução local existente, cast/custo/recarga exercitados pela fábrica de 40 casos e família/arte coberta pelos testes indicados; não significa todos os cenários de rede ou homologação física.
- **Funcional com ressalvas**: execução presente e testada, mas com divergência, exceção de alto risco ou ambiguidade específica listada. Não significa skill apenas documentada.
- Não foram encontradas habilidades apenas documentadas, indisponíveis ou parcialmente sem executor entre estes 40 IDs. Não foi inventada implementação para os previews históricos de `Domains`.
- Gaps comuns de feedback de alvo/status da hotbar (A-01), valores provisórios e homologação física pendente também se aplicam às fichas classificadas como funcionais e testadas.
- T0 = [fábrica de 40 casts](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L70): valida lançamento, custo em décimos, recarga, evento e avanço de 40 ticks. Não mede sozinho o dano total de todas as habilidades.
- V0 = [loader/alpha dos 40 recortes](../server/src/test/java/com/projetok/multiplayer/NetworkSkillPresentationTest.java#L45). Testes de família não equivalem a 40 sessões TCP individuais.
- Autoridade comum: [RpgSessionWorld.skill](../server/src/main/java/com/projetok/multiplayer/RpgSessionWorld.java#L82) → [RpgSkillEngine.cast](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L74).
- Cliente comum: [NetworkSkillControls.activate](../server/src/main/java/com/projetok/multiplayer/NetworkSkillControls.java#L129) → [update/draw](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L119); loader [load](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L409).
- Assets resolvidos em ordem: manifesto base e overlays refined-v2 até refined-v8. Há 71 clips carregados (40 principais e 31 componentes); pivô/escala/tempo pertencem ao manifesto vencedor.
- Aliases: Água/Vento/Raio/Fogo = Reaper; Luz/Marcial = Human; bow_long e bow_short = kit Aetheri; darkness_control e darkness_debuff = kit Spirit. Nível 15 em todos os slots.

## Visão geral

| ID | Estilo | Skill | Slot | EN | Recarga | Conjuração | Classificação |
|---|---|---|---:|---:|---|---|---|
| `water_1` | Água | Corte de água | 1 | 12 | 1,5 s | Alvo selecionado | Funcional com ressalvas |
| `water_2` | Água | Corte de maré | 2 | 16 | 2,5 s | Direta no usuário / sem alvo | Funcional e testada |
| `water_3` | Água | Prisão de água | 3 | 20 | 6 s | Alvo selecionado | Funcional e testada |
| `water_4` | Água | Vórtice de água | 4 | 25 | 6 s | Direta no usuário / sem alvo | Funcional e testada |
| `water_5` | Água | Investida de água | 5 | 25 | 5 s | Frontal / smartcast | Funcional e testada |
| `wind_1` | Vento | Corte de vento | 1 | 12 | 1,5 s | Alvo selecionado | Funcional com ressalvas |
| `wind_2` | Vento | Três cortes de vento | 2 | 20 | 4 s | Alvo selecionado | Funcional com ressalvas |
| `wind_3` | Vento | Passo cortante | 3 | 22 | 4 s | Frontal / smartcast | Funcional e testada |
| `wind_4` | Vento | Vendaval de repulsão | 4 | 22 | 5 s | Frontal / smartcast | Funcional com ressalvas |
| `wind_5` | Vento | Vendaval de retorno | 5 | 18 | 4 s | Direta no usuário / sem alvo | Funcional com ressalvas |
| `thunder_1` | Raio | Corte de raio | 1 | 12 | 1,5 s | Alvo selecionado | Funcional com ressalvas |
| `thunder_2` | Raio | Lança de raio | 2 | 20 | 4 s | Frontal / smartcast | Funcional e testada |
| `thunder_3` | Raio | Corrente de raio | 3 | 25 | 5 s | Alvo selecionado | Funcional com ressalvas |
| `thunder_4` | Raio | Investida relâmpago | 4 | 25 | 5 s | Frontal / smartcast | Funcional e testada |
| `thunder_5` | Raio | Quatro relâmpagos | 5 | 40 | 9 s | Direta no usuário / sem alvo | Funcional com ressalvas |
| `fire_1` | Fogo | Corte de fogo | 1 | 12 | 1,5 s | Alvo selecionado | Funcional com ressalvas |
| `fire_2` | Fogo | Fissura ardente | 2 | 20 | 4 s | Frontal / smartcast | Funcional com ressalvas |
| `fire_3` | Fogo | Vórtice de fogo | 3 | 25 | 6 s | Direta no usuário / sem alvo | Funcional e testada |
| `fire_4` | Fogo | Erupção de fogo | 4 | 30 | 7 s | Frontal / smartcast | Funcional com ressalvas |
| `fire_5` | Fogo | Chuva de cortes de fogo | 5 | 40 | 9 s | Área selecionável | Funcional e testada |
| `spirit_1` | Spirit | Poça espiritual | 1 | 22 | 5 s | Área selecionável | Funcional e testada |
| `spirit_2` | Spirit | Teia espiritual | 2 | 18 | 4 s | Frontal / smartcast | Funcional e testada |
| `spirit_3` | Spirit | Feixe espiritual | 3 | 20 | 3 s | Frontal / smartcast | Funcional e testada |
| `spirit_4` | Spirit | Prisão espiritual | 4 | 25 | 6 s | Área selecionável | Funcional e testada |
| `spirit_5` | Spirit | Areia movediça | 5 | 25 | 6 s | Área selecionável | Funcional e testada |
| `aetheri_1` | Aetheri | Flecha precisa | 1 | 10 | 1,2 s | Alvo selecionado | Funcional com ressalvas |
| `aetheri_2` | Aetheri | Sentinela | 2 | 22 | 5 s | Direta no usuário / sem alvo | Funcional com ressalvas |
| `aetheri_3` | Aetheri | Chuva de flechas | 3 | 30 | 6 s | Área selecionável | Funcional e testada |
| `aetheri_4` | Aetheri | Flecha de energia | 4 | 25 | 5 s | Frontal / smartcast | Funcional e testada |
| `aetheri_5` | Aetheri | Leque de flechas | 5 | 30 | 6 s | Cone frontal / smartcast | Funcional com ressalvas |
| `light_1` | Luz | Cura básica | 1 | 12 | 2 s | Alvo selecionado | Funcional e testada |
| `light_2` | Luz | Cura em área | 2 | 25 | 5 s | Direta no usuário / sem alvo | Funcional e testada |
| `light_3` | Luz | Reviver | 3 | 40 | 30 s | Aliado selecionado (ou usuário, quando permitido) | Funcional e testada |
| `light_4` | Luz | Purificação | 4 | 18 | 6 s | Aliado selecionado (ou usuário, quando permitido) | Funcional com ressalvas |
| `light_5` | Luz | Cura superior | 5 | 30 | 7 s | Alvo selecionado | Funcional e testada |
| `martial_1` | Artista Marcial | Soco preciso | 1 | 10 | 1,2 s | Alvo selecionado | Funcional e testada |
| `martial_2` | Artista Marcial | Sequência de socos | 2 | 18 | 3,33 s | Alvo selecionado | Funcional e testada |
| `martial_3` | Artista Marcial | Impacto sísmico | 3 | 22 | 5 s | Direta no usuário / sem alvo | Funcional e testada |
| `martial_4` | Artista Marcial | Body slam | 4 | 25 | 6 s | Alvo selecionado | Funcional com ressalvas |
| `martial_5` | Artista Marcial | Combo de punhos e chutes | 5 | 35 | 8 s | Cone frontal / smartcast | Funcional e testada |

## `water_1` — Corte de água

- **Identificação:** Água; slot 1; requisito nível 15; **Funcional com ressalvas**.
- **Valores atuais:** 12 EN; recarga 45 ticks / 1,5 s; alcance 240; raio 0; dano-base 20; cura/vida restaurada-base 0; escopo `ENEMIES`.
- **Quantidade e tempo:** 1 impacto(s)/pulso(s) declarado(s); contato e sobrevivência condicionam o resultado. Metadados: hits=1, intervalo=0 ticks, duração=0 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `TARGET` — Alvo selecionado. Reduz a velocidade em 50% por 2 s.
- **Sprite/loader:** `assets/skills/refined-v4/water_1.png`; manifesto efetivo `assets/skills/refined-v4/manifest.properties`; 9 frames de 256x160 px, desenho 128x80 px, 65 ms/frame.
- **Componentes visuais:** `.impact`: `assets/skills/refined-v4/impact.png` (6 frames)
- **Servidor:** padrão `PROJECTILE`, [class Projectile](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L366). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [public void draw(](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L200) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; [projectileTravelsBeforeDamageAndOldGenerationCannotBeHit](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L91), [aTargetedCutHitsAnInterveningBodyBeforeItsSelectedTarget](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L571).
- **Limitações verificadas:** A-03: perseguição no servidor e segmento visual inicial podem divergir com alvo móvel. Gaps comuns A-01 e homologação física pendente.

## `water_2` — Corte de maré

- **Identificação:** Água; slot 2; requisito nível 15; **Funcional e testada**.
- **Valores atuais:** 16 EN; recarga 75 ticks / 2,5 s; alcance 0; raio 82; dano-base 22; cura/vida restaurada-base 0; escopo `ENEMIES`.
- **Quantidade e tempo:** 1 impacto(s)/pulso(s) declarado(s); contato e sobrevivência condicionam o resultado. Metadados: hits=1, intervalo=0 ticks, duração=0 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `SELF` — Direta no usuário / sem alvo. Reduz a velocidade em 50% por 2 s.
- **Sprite/loader:** `assets/skills/refined-v6/water_2.png`; manifesto efetivo `assets/skills/refined-v6/manifest.properties`; 12 frames de 320x320 px, desenho 200x200 px, 50 ms/frame.
- **Componentes visuais:** sem atlas auxiliar específico; usa o clip principal/fallback existente.
- **Servidor:** padrão `MELEE_AREA`, [case MELEE_AREA, GROUND_STOMP](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L149). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [public void draw(](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L200) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; sem cenário unitário exclusivo deste ID identificado: a evidência específica é de família/cast e loader.
- **Limitações verificadas:** Giro de efeito sobre o corpo existente; não é uma nova animação corporal. Gaps comuns A-01 e homologação física pendente.

## `water_3` — Prisão de água

- **Identificação:** Água; slot 3; requisito nível 15; **Funcional e testada**.
- **Valores atuais:** 20 EN; recarga 180 ticks / 6 s; alcance 220; raio 0; dano-base 0; cura/vida restaurada-base 0; escopo `NONE`.
- **Quantidade e tempo:** 1 impacto(s)/pulso(s) declarado(s); contato e sobrevivência condicionam o resultado. Metadados: hits=1, intervalo=0 ticks, duração=0 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `TARGET` — Alvo selecionado. Imobiliza por 2 s; permite atacar e usar habilidades.
- **Sprite/loader:** `assets/skills/refined-v4/water_3.png`; manifesto efetivo `assets/skills/refined-v4/manifest.properties`; 9 frames de 256x256 px, desenho 128x128 px, 100 ms/frame.
- **Componentes visuais:** sem atlas auxiliar específico; usa o clip principal/fallback existente.
- **Servidor:** padrão `PRISON`, [case PRISON](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L150). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [public void draw(](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L200) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; [rootPreservesEnemyAttackAndExpiresAfterSixtyGlobalTicks](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L100).
- **Limitações verificadas:** Root permite ataques; não equivale a stun. Dano direto zero. Gaps comuns A-01 e homologação física pendente.

## `water_4` — Vórtice de água

- **Identificação:** Água; slot 4; requisito nível 15; **Funcional e testada**.
- **Valores atuais:** 25 EN; recarga 180 ticks / 6 s; alcance 0; raio 100; dano-base 9; cura/vida restaurada-base 0; escopo `ENEMIES`.
- **Quantidade e tempo:** 4 impacto(s)/pulso(s) declarado(s); contato e sobrevivência condicionam o resultado. Metadados: hits=4, intervalo=10 ticks, duração=40 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `SELF` — Direta no usuário / sem alvo. Sem status adicional declarado.
- **Sprite/loader:** `assets/skills/refined-v5/water_4.png`; manifesto efetivo `assets/skills/refined-v5/manifest.properties`; 12 frames de 320x400 px, desenho 192x240 px, 110 ms/frame.
- **Componentes visuais:** sem atlas auxiliar específico; usa o clip principal/fallback existente.
- **Servidor:** padrão `VORTEX`, [class Zone](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L557). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [public void draw(](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L200) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; sem cenário unitário exclusivo deste ID identificado: a evidência específica é de família/cast e loader.
- **Limitações verificadas:** Quatro pulsos; ausência de lentidão neste slot é o contrato atual do catálogo. Gaps comuns A-01 e homologação física pendente.

## `water_5` — Investida de água

- **Identificação:** Água; slot 5; requisito nível 15; **Funcional e testada**.
- **Valores atuais:** 25 EN; recarga 150 ticks / 5 s; alcance 240; raio 22; dano-base 30; cura/vida restaurada-base 0; escopo `ENEMIES`.
- **Quantidade e tempo:** 1 por inimigo atravessado, nunca por frame desenhado. Metadados: hits=1, intervalo=0 ticks, duração=8 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `LINE` — Frontal / smartcast. Sem status adicional declarado.
- **Sprite/loader:** `assets/skills/refined-v4/water_5.png`; manifesto efetivo `assets/skills/refined-v4/manifest.properties`; 9 frames de 320x128 px, desenho 240x96 px, 45 ms/frame.
- **Componentes visuais:** `.travel`: `assets/skills/refined-v8/water_5.travel.png` (6 frames)
- **Servidor:** padrão `DASH`, [class Dash](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L601). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [private void drawDashes](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L264) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; [dashMovesOverTimeAndCannotCrossWallsOrMoveWhileRooted](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L376).
- **Limitações verificadas:** Exceção física: atravessa mobs, bloqueia cenário/aliados e exige apoio livre no término. Gaps comuns A-01 e homologação física pendente.

## `wind_1` — Corte de vento

- **Identificação:** Vento; slot 1; requisito nível 15; **Funcional com ressalvas**.
- **Valores atuais:** 12 EN; recarga 45 ticks / 1,5 s; alcance 260; raio 0; dano-base 22; cura/vida restaurada-base 0; escopo `ENEMIES`.
- **Quantidade e tempo:** 1 impacto(s)/pulso(s) declarado(s); contato e sobrevivência condicionam o resultado. Metadados: hits=1, intervalo=0 ticks, duração=0 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `TARGET` — Alvo selecionado. Sem status adicional declarado.
- **Sprite/loader:** `assets/skills/wind_1.png`; manifesto efetivo `assets/skills/manifest.properties`; 9 frames de 128x128 px, desenho 128x128 px, 80 ms/frame.
- **Componentes visuais:** `.impact`: `assets/skills/refined-v4/impact.png` (6 frames)
- **Servidor:** padrão `PROJECTILE`, [class Projectile](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L366). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [public void draw(](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L200) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; sem cenário unitário exclusivo deste ID identificado: a evidência específica é de família/cast e loader.
- **Limitações verificadas:** A-03: projétil dirigido pode curvar no servidor, mas a apresentação parte de um segmento fixo. Gaps comuns A-01 e homologação física pendente.

## `wind_2` — Três cortes de vento

- **Identificação:** Vento; slot 2; requisito nível 15; **Funcional com ressalvas**.
- **Valores atuais:** 20 EN; recarga 120 ticks / 4 s; alcance 240; raio 0; dano-base 10; cura/vida restaurada-base 0; escopo `ENEMIES`.
- **Quantidade e tempo:** 3 projéteis, com intervalo de 3 ticks (0,1 s), mais sangramento separado. Metadados: hits=3, intervalo=3 ticks, duração=6 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `TARGET` — Alvo selecionado. Sangramento por 3 s: 3 de dano-base a cada 1 s.
- **Sprite/loader:** `assets/skills/wind_1.png`; manifesto efetivo `assets/skills/refined-v4/manifest.properties`; 9 frames de 128x128 px, desenho 128x128 px, 80 ms/frame.
- **Componentes visuais:** `.impact`: `assets/skills/refined-v4/impact.png` (6 frames); `.travel`: `assets/skills/wind_1.png` (9 frames)
- **Servidor:** padrão `MULTI_HIT`, [class WindCuts](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L483). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [public void draw(](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L200) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; [multihitAndBleedUseSeparateClocksWithoutStackingDots](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L137), [windCutsLaunchAtZeroThreeAndSixTicksFromTheCurrentCasterPosition](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L585).
- **Limitações verificadas:** A-03: três projéteis dirigidos lançados nos ticks 0, 3 e 6; DoT separado por lançador. Gaps comuns A-01 e homologação física pendente.

## `wind_3` — Passo cortante

- **Identificação:** Vento; slot 3; requisito nível 15; **Funcional e testada**.
- **Valores atuais:** 22 EN; recarga 120 ticks / 4 s; alcance 240; raio 20; dano-base 28; cura/vida restaurada-base 0; escopo `ENEMIES`.
- **Quantidade e tempo:** 1 por inimigo atravessado, nunca por frame desenhado. Metadados: hits=1, intervalo=0 ticks, duração=8 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `LINE` — Frontal / smartcast. Sem status adicional declarado.
- **Sprite/loader:** `assets/skills/wind_3.png`; manifesto efetivo `assets/skills/manifest.properties`; 9 frames de 128x128 px, desenho 128x128 px, 55 ms/frame.
- **Componentes visuais:** `.travel`: `assets/skills/refined-v8/wind_3.travel.png` (6 frames)
- **Servidor:** padrão `DASH`, [class Dash](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L601). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [private void drawDashes](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L264) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; sem cenário unitário exclusivo deste ID identificado: a evidência específica é de família/cast e loader.
- **Limitações verificadas:** Investida sem alvo; um acerto por identidade/geração durante a passagem. Gaps comuns A-01 e homologação física pendente.

## `wind_4` — Vendaval de repulsão

- **Identificação:** Vento; slot 4; requisito nível 15; **Funcional com ressalvas**.
- **Valores atuais:** 22 EN; recarga 150 ticks / 5 s; alcance 240; raio 70; dano-base 16; cura/vida restaurada-base 0; escopo `ENEMIES`.
- **Quantidade e tempo:** 1 impacto(s)/pulso(s) declarado(s); contato e sobrevivência condicionam o resultado. Metadados: hits=1, intervalo=0 ticks, duração=0 ticks, janela de checkpoint=150 ticks.
- **Conjuração/status:** `LINE` — Frontal / smartcast. Sem status adicional declarado.
- **Sprite/loader:** `assets/skills/refined-v6/wind_4.png`; manifesto efetivo `assets/skills/refined-v6/manifest.properties`; 12 frames de 320x224 px, desenho 160x112 px, 45 ms/frame.
- **Componentes visuais:** `.impact`: `assets/skills/refined-v6/wind_4.impact.png` (6 frames); `.travel`: `assets/skills/refined-v6/wind_4.png` (12 frames)
- **Servidor:** padrão `KNOCKBACK_CHECKPOINT`, [private void repel](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L411). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [public void draw(](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L200) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; [frontalRepulsionNeedsNoTargetAndOnlyCreatesCheckpointOnActualContact](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L159), [frontalRepulsionStopsAtWallAndNeverPushesTheEnemyBehindIt](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L177).
- **Limitações verificadas:** A-02: marcas individuais não são transmitidas/desenhadas; o estado disponível é um booleano por lançador. Gaps comuns A-01 e homologação física pendente.

## `wind_5` — Vendaval de retorno

- **Identificação:** Vento; slot 5; requisito nível 15; **Funcional com ressalvas**.
- **Valores atuais:** 18 EN; recarga 120 ticks / 4 s; alcance 300; raio 70; dano-base 16; cura/vida restaurada-base 0; escopo `ENEMIES`.
- **Quantidade e tempo:** 1 por inimigo marcado elegível; conjunto consumido uma única vez. Metadados: hits=1, intervalo=0 ticks, duração=0 ticks, janela de checkpoint=150 ticks.
- **Conjuração/status:** `SELF` — Direta no usuário / sem alvo. Sem status adicional declarado.
- **Sprite/loader:** `assets/skills/refined-v6/wind_4.png`; manifesto efetivo `assets/skills/refined-v6/manifest.properties`; 12 frames de 320x224 px, desenho 160x112 px, 45 ms/frame.
- **Componentes visuais:** `.travel`: `assets/skills/refined-v6/wind_4.png` (12 frames)
- **Servidor:** padrão `CHECKPOINT_PULL`, [private List<EnemyService.Enemy> windVictims](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L401). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [public void draw(](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L200) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; [untargetedReturnPullsAllMarkedEnemiesButLeavesNearbyUnmarkedEnemyAlone](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L197), [stalePrimaryMarkDoesNotPreventTheOtherMarkedEnemyFromReturning](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L209).
- **Limitações verificadas:** A-02: readiness pode conservar marcas mortas/inválidas até expirar, embora cast revalide. Retorno coletivo já é SELF. Gaps comuns A-01 e homologação física pendente.

## `thunder_1` — Corte de raio

- **Identificação:** Raio; slot 1; requisito nível 15; **Funcional com ressalvas**.
- **Valores atuais:** 12 EN; recarga 45 ticks / 1,5 s; alcance 240; raio 0; dano-base 22; cura/vida restaurada-base 0; escopo `ENEMIES`.
- **Quantidade e tempo:** 1 impacto(s)/pulso(s) declarado(s); contato e sobrevivência condicionam o resultado. Metadados: hits=1, intervalo=0 ticks, duração=0 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `TARGET` — Alvo selecionado. Sem status adicional declarado.
- **Sprite/loader:** `assets/skills/refined-v8/thunder_1.png`; manifesto efetivo `assets/skills/refined-v8/manifest.properties`; 6 frames de 192x64 px, desenho 192x64 px, 70 ms/frame.
- **Componentes visuais:** `.impact`: `assets/skills/refined-v8/thunder_1.impact.png` (6 frames)
- **Servidor:** padrão `PROJECTILE`, [class Projectile](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L366). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [public void draw(](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L200) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; sem cenário unitário exclusivo deste ID identificado: a evidência específica é de família/cast e loader.
- **Limitações verificadas:** A-03: mesmas ressalvas de trajetória dirigida e apresentação linear. Gaps comuns A-01 e homologação física pendente.

## `thunder_2` — Lança de raio

- **Identificação:** Raio; slot 2; requisito nível 15; **Funcional e testada**.
- **Valores atuais:** 20 EN; recarga 120 ticks / 4 s; alcance 280; raio 18; dano-base 24; cura/vida restaurada-base 0; escopo `ENEMIES`.
- **Quantidade e tempo:** 1 impacto(s)/pulso(s) declarado(s); contato e sobrevivência condicionam o resultado. Metadados: hits=1, intervalo=0 ticks, duração=0 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `LINE` — Frontal / smartcast. Atordoa por 1 s; impede movimento, ataques e habilidades.
- **Sprite/loader:** `assets/skills/refined-v8/thunder_2.png`; manifesto efetivo `assets/skills/refined-v8/manifest.properties`; 9 frames de 256x64 px, desenho 224x56 px, 70 ms/frame.
- **Componentes visuais:** `.impact`: `assets/skills/refined-v8/thunder_1.impact.png` (6 frames)
- **Servidor:** padrão `LINE_PROJECTILE`, [class TravelingLine](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L427). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [public void draw(](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L200) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; [directionalSkillshotUsesServerFacingAndHitsWhenTheFrontActuallyArrives](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L534), [skillshotNeverHitsAnEnemyThatEntersThePartItAlreadyTraversed](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L544).
- **Limitações verificadas:** Para no primeiro corpo; stun de 30 ticks. Não é um feixe perfurante. Gaps comuns A-01 e homologação física pendente.

## `thunder_3` — Corrente de raio

- **Identificação:** Raio; slot 3; requisito nível 15; **Funcional com ressalvas**.
- **Valores atuais:** 25 EN; recarga 150 ticks / 5 s; alcance 240; raio 130; dano-base 20; cura/vida restaurada-base 0; escopo `ENEMIES`.
- **Quantidade e tempo:** Até 4 impactos; repetição 100%, 75%, 56,25%, 42,1875% antes dos arredondamentos. Metadados: hits=4, intervalo=8 ticks, duração=24 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `TARGET` — Alvo selecionado. Sem status adicional declarado.
- **Sprite/loader:** `assets/skills/refined-v8/thunder_1.png`; manifesto efetivo `assets/skills/refined-v8/manifest.properties`; 6 frames de 192x64 px, desenho 192x64 px, 85 ms/frame.
- **Componentes visuais:** `.impact`: `assets/skills/refined-v8/thunder_1.impact.png` (6 frames)
- **Servidor:** padrão `CHAIN`, [class Chain](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L530). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [public void draw(](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L200) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; [chainHasFourTimedHitsAndDecreasingFallbackDamage](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L119), [chainSpreadsToFourDifferentEnemiesBeforeRepeating](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L129).
- **Limitações verificadas:** A-07: 4 impactos são condicionais à sobrevivência e visibilidade, não quatro acertos garantidos. Gaps comuns A-01 e homologação física pendente.

## `thunder_4` — Investida relâmpago

- **Identificação:** Raio; slot 4; requisito nível 15; **Funcional e testada**.
- **Valores atuais:** 25 EN; recarga 150 ticks / 5 s; alcance 240; raio 20; dano-base 30; cura/vida restaurada-base 0; escopo `ENEMIES`.
- **Quantidade e tempo:** 1 por inimigo atravessado, nunca por frame desenhado. Metadados: hits=1, intervalo=0 ticks, duração=8 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `LINE` — Frontal / smartcast. Atordoa por 1 s; impede movimento, ataques e habilidades.
- **Sprite/loader:** `assets/skills/refined-v8/thunder_4.png`; manifesto efetivo `assets/skills/refined-v8/manifest.properties`; 9 frames de 256x96 px, desenho 256x96 px, 45 ms/frame.
- **Componentes visuais:** `.travel`: `assets/skills/refined-v8/thunder_4.png` (9 frames)
- **Servidor:** padrão `DASH`, [class Dash](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L601). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [private void drawDashes](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L264) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; [frontalDashNeedsNoTargetAndCutsTwoCrossedEnemiesOnlyOnce](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L412), [dashTerminationReportsTheActualPositionAndStillStopsWhenRootedMidway](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L383).
- **Limitações verificadas:** Investida frontal; conserva stun, mas não recebe a imunidade exclusiva do slot 5. Gaps comuns A-01 e homologação física pendente.

## `thunder_5` — Quatro relâmpagos

- **Identificação:** Raio; slot 5; requisito nível 15; **Funcional com ressalvas**.
- **Valores atuais:** 40 EN; recarga 270 ticks / 9 s; alcance 0; raio 110; dano-base 14; cura/vida restaurada-base 0; escopo `ENEMIES`.
- **Quantidade e tempo:** 4 pulsos de área no encerramento de 32 ticks; animação dos cortes é escalonada. Metadados: hits=4, intervalo=8 ticks, duração=32 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `SELF` — Direta no usuário / sem alvo. Atordoa por 1 s; impede movimento, ataques e habilidades.
- **Sprite/loader:** `assets/skills/refined-v8/thunder_5.png`; manifesto efetivo `assets/skills/refined-v8/manifest.properties`; 9 frames de 256x256 px, desenho 240x240 px, 115 ms/frame.
- **Componentes visuais:** `.travel`: `assets/skills/refined-v8/thunder_4.png` (9 frames) Raio 5 compõe os cortes com `thunder_2`, além da delimitação da fase; o clip principal ainda atende ícone/fallback.
- **Servidor:** padrão `CIRCULAR_DASH`, [class LightningField](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L620). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [private void drawFinalCuts](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L318) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; [lightningFieldIsImmuneUntilFourAreaHitsAndThenRestoresDamage](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L391), [lightningFieldReappearsBesideAnAllyWhoOccupiedItsOriginAndClearRestoresState](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L403).
- **Limitações verificadas:** A-04: fase pode aguardar indefinidamente apoio livre; cliente tem fusível de 5 s. Necessita cenário extremo e matriz portal/desconexão. Gaps comuns A-01 e homologação física pendente.

## `fire_1` — Corte de fogo

- **Identificação:** Fogo; slot 1; requisito nível 15; **Funcional com ressalvas**.
- **Valores atuais:** 12 EN; recarga 45 ticks / 1,5 s; alcance 240; raio 0; dano-base 22; cura/vida restaurada-base 0; escopo `ENEMIES`.
- **Quantidade e tempo:** 1 impacto(s)/pulso(s) declarado(s); contato e sobrevivência condicionam o resultado. Metadados: hits=1, intervalo=0 ticks, duração=0 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `TARGET` — Alvo selecionado. Sem status adicional declarado.
- **Sprite/loader:** `assets/skills/fire_1.png`; manifesto efetivo `assets/skills/manifest.properties`; 9 frames de 128x128 px, desenho 128x128 px, 80 ms/frame.
- **Componentes visuais:** `.impact`: `assets/skills/refined-v4/impact.png` (6 frames)
- **Servidor:** padrão `PROJECTILE`, [class Projectile](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L366). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [public void draw(](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L200) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; sem cenário unitário exclusivo deste ID identificado: a evidência específica é de família/cast e loader.
- **Limitações verificadas:** A-03: corte dirigido; este slot não declara queimadura. Gaps comuns A-01 e homologação física pendente.

## `fire_2` — Fissura ardente

- **Identificação:** Fogo; slot 2; requisito nível 15; **Funcional com ressalvas**.
- **Valores atuais:** 20 EN; recarga 120 ticks / 4 s; alcance 260; raio 24; dano-base 22; cura/vida restaurada-base 0; escopo `ENEMIES`.
- **Quantidade e tempo:** 1 contato da frente por vítima; trilha reaplica status sem novo dano direto. Metadados: hits=1, intervalo=0 ticks, duração=90 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `LINE` — Frontal / smartcast. Queimadura por 3 s: 3 de dano-base a cada 1 s.
- **Sprite/loader:** `assets/skills/refined-v4/fire_2.png`; manifesto efetivo `assets/skills/refined-v4/manifest.properties`; 9 frames de 384x160 px, desenho 288x120 px, 80 ms/frame.
- **Componentes visuais:** `.ground`: `assets/skills/refined-v4/fire_2.png` (9 frames); `.travel`: `assets/skills/refined-v2/fire_2.travel.png` (9 frames)
- **Servidor:** padrão `FISSURE`, [class TravelingLine](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L427). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [public void draw(](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L200) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; [fireFissureTravelsFromThePlayerAndEruptsOnlyAtTheActualEnd](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L619).
- **Limitações verificadas:** A-06: contato direto e trilha que renova queimadura; não multiplicar dano-base pelo tempo nominal da área. Gaps comuns A-01 e homologação física pendente.

## `fire_3` — Vórtice de fogo

- **Identificação:** Fogo; slot 3; requisito nível 15; **Funcional e testada**.
- **Valores atuais:** 25 EN; recarga 180 ticks / 6 s; alcance 0; raio 100; dano-base 9; cura/vida restaurada-base 0; escopo `ENEMIES`.
- **Quantidade e tempo:** 4 impacto(s)/pulso(s) declarado(s); contato e sobrevivência condicionam o resultado. Metadados: hits=4, intervalo=10 ticks, duração=40 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `SELF` — Direta no usuário / sem alvo. Queimadura por 3 s: 3 de dano-base a cada 1 s.
- **Sprite/loader:** `assets/skills/refined-v5/fire_3.png`; manifesto efetivo `assets/skills/refined-v5/manifest.properties`; 12 frames de 320x400 px, desenho 192x240 px, 110 ms/frame.
- **Componentes visuais:** sem atlas auxiliar específico; usa o clip principal/fallback existente.
- **Servidor:** padrão `VORTEX`, [class Zone](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L557). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [public void draw(](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L200) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; sem cenário unitário exclusivo deste ID identificado: a evidência específica é de família/cast e loader.
- **Limitações verificadas:** Quatro pulsos e renovação de queimadura; dano periódico tem relógio separado. Gaps comuns A-01 e homologação física pendente.

## `fire_4` — Erupção de fogo

- **Identificação:** Fogo; slot 4; requisito nível 15; **Funcional com ressalvas**.
- **Valores atuais:** 30 EN; recarga 210 ticks / 7 s; alcance 280; raio 85; dano-base 25; cura/vida restaurada-base 0; escopo `ENEMIES`.
- **Quantidade e tempo:** 1 contato da frente por vítima + até 2 pulsos de erupção para quem permanecer na área final. Metadados: hits=2, intervalo=15 ticks, duração=15 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `LINE` — Frontal / smartcast. Sem status adicional declarado.
- **Sprite/loader:** `assets/skills/refined-v5/fire_4.png`; manifesto efetivo `assets/skills/refined-v5/manifest.properties`; 12 frames de 384x448 px, desenho 240x280 px, 100 ms/frame.
- **Componentes visuais:** `.impact`: `assets/skills/refined-v5/fire_4.png` (12 frames); `.ground`: `assets/skills/refined-v4/fire_2.png` (9 frames); `.travel`: `assets/skills/refined-v2/fire_4.travel.png` (9 frames)
- **Servidor:** padrão `FISSURE_ERUPTION`, [class TravelingLine](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L427). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [public void draw(](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L200) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; [fireFissureTravelsFromThePlayerAndEruptsOnlyAtTheActualEnd](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L619).
- **Limitações verificadas:** A-06: contato da frente e dois pulsos finais; o número 2 não descreve sozinho todos os contatos possíveis. Gaps comuns A-01 e homologação física pendente.

## `fire_5` — Chuva de cortes de fogo

- **Identificação:** Fogo; slot 5; requisito nível 15; **Funcional e testada**.
- **Valores atuais:** 40 EN; recarga 270 ticks / 9 s; alcance 280; raio 100; dano-base 10; cura/vida restaurada-base 0; escopo `ENEMIES`.
- **Quantidade e tempo:** 5 impacto(s)/pulso(s) declarado(s); contato e sobrevivência condicionam o resultado. Metadados: hits=5, intervalo=12 ticks, duração=60 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `GROUND` — Área selecionável. Queimadura por 3 s: 3 de dano-base a cada 1 s.
- **Sprite/loader:** `assets/skills/refined-v6/fire_5.png`; manifesto efetivo `assets/skills/refined-v6/manifest.properties`; 12 frames de 192x192 px, desenho 96x96 px, 28 ms/frame.
- **Componentes visuais:** sem atlas auxiliar específico; usa o clip principal/fallback existente.
- **Servidor:** padrão `RAIN`, [class Zone](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L557). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [private static void drawFireRain](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L368) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; sem cenário unitário exclusivo deste ID identificado: a evidência específica é de família/cast e loader.
- **Limitações verificadas:** Cinco pulsos de área, não dano por cada X desenhado. Gaps comuns A-01 e homologação física pendente.

## `spirit_1` — Poça espiritual

- **Identificação:** Spirit; slot 1; requisito nível 15; **Funcional e testada**.
- **Valores atuais:** 22 EN; recarga 150 ticks / 5 s; alcance 240; raio 85; dano-base 8; cura/vida restaurada-base 0; escopo `ENEMIES`.
- **Quantidade e tempo:** 4 impacto(s)/pulso(s) declarado(s); contato e sobrevivência condicionam o resultado. Metadados: hits=4, intervalo=15 ticks, duração=60 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `GROUND` — Área selecionável. Imobiliza por 1 s; permite atacar e usar habilidades.
- **Sprite/loader:** `assets/skills/spirit_1.png`; manifesto efetivo `assets/skills/manifest.properties`; 12 frames de 128x128 px, desenho 128x128 px, 110 ms/frame.
- **Componentes visuais:** sem atlas auxiliar específico; usa o clip principal/fallback existente.
- **Servidor:** padrão `DAMAGE_POOL`, [class Zone](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L557). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [public void draw(](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L200) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; sem cenário unitário exclusivo deste ID identificado: a evidência específica é de família/cast e loader.
- **Limitações verificadas:** Poça fixa; quatro pulsos de dano, root renovado nos contatos. Não é uma prisão exclusiva sem dano. Gaps comuns A-01 e homologação física pendente.

## `spirit_2` — Teia espiritual

- **Identificação:** Spirit; slot 2; requisito nível 15; **Funcional e testada**.
- **Valores atuais:** 18 EN; recarga 120 ticks / 4 s; alcance 260; raio 22; dano-base 0; cura/vida restaurada-base 0; escopo `NONE`.
- **Quantidade e tempo:** 1 impacto(s)/pulso(s) declarado(s); contato e sobrevivência condicionam o resultado. Metadados: hits=1, intervalo=0 ticks, duração=0 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `LINE` — Frontal / smartcast. Imobiliza por 2 s; permite atacar e usar habilidades.
- **Sprite/loader:** `assets/skills/spirit_2.png`; manifesto efetivo `assets/skills/manifest.properties`; 12 frames de 128x128 px, desenho 128x128 px, 80 ms/frame.
- **Componentes visuais:** `.impact`: `assets/skills/refined-v4/impact.png` (6 frames)
- **Servidor:** padrão `WEB`, [class TravelingLine](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L427). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [public void draw(](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L200) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; sem cenário unitário exclusivo deste ID identificado: a evidência específica é de família/cast e loader.
- **Limitações verificadas:** Teia para no primeiro corpo; não causa dano direto. Gaps comuns A-01 e homologação física pendente.

## `spirit_3` — Feixe espiritual

- **Identificação:** Spirit; slot 3; requisito nível 15; **Funcional e testada**.
- **Valores atuais:** 20 EN; recarga 90 ticks / 3 s; alcance 280; raio 20; dano-base 30; cura/vida restaurada-base 0; escopo `ENEMIES`.
- **Quantidade e tempo:** 1 impacto(s)/pulso(s) declarado(s); contato e sobrevivência condicionam o resultado. Metadados: hits=1, intervalo=0 ticks, duração=0 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `LINE` — Frontal / smartcast. Sem status adicional declarado.
- **Sprite/loader:** `assets/skills/spirit_3.png`; manifesto efetivo `assets/skills/manifest.properties`; 4 frames de 128x128 px, desenho 128x128 px, 100 ms/frame.
- **Componentes visuais:** `.impact`: `assets/skills/refined-v4/impact.png` (6 frames)
- **Servidor:** padrão `BEAM`, [class TravelingLine](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L427). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [public void draw(](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L200) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; sem cenário unitário exclusivo deste ID identificado: a evidência específica é de família/cast e loader.
- **Limitações verificadas:** Feixe tem progressão de projétil e primeiro contato; a arte não concede perfuração. Gaps comuns A-01 e homologação física pendente.

## `spirit_4` — Prisão espiritual

- **Identificação:** Spirit; slot 4; requisito nível 15; **Funcional e testada**.
- **Valores atuais:** 25 EN; recarga 180 ticks / 6 s; alcance 240; raio 95; dano-base 0; cura/vida restaurada-base 0; escopo `NONE`.
- **Quantidade e tempo:** Consultas periódicas de controle até a duração; nenhum impacto de dano. Metadados: hits=1, intervalo=0 ticks, duração=60 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `GROUND` — Área selecionável. Imobiliza por 2 s; permite atacar e usar habilidades.
- **Sprite/loader:** `assets/skills/spirit_4.png`; manifesto efetivo `assets/skills/manifest.properties`; 20 frames de 128x128 px, desenho 160x160 px, 95 ms/frame.
- **Componentes visuais:** sem atlas auxiliar específico; usa o clip principal/fallback existente.
- **Servidor:** padrão `AREA_PRISON`, [class Zone](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L557). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [public void draw(](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L200) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; sem cenário unitário exclusivo deste ID identificado: a evidência específica é de família/cast e loader.
- **Limitações verificadas:** Prisão reaplica root dentro da duração da área; múltiplas consultas não significam dano. Gaps comuns A-01 e homologação física pendente.

## `spirit_5` — Areia movediça

- **Identificação:** Spirit; slot 5; requisito nível 15; **Funcional e testada**.
- **Valores atuais:** 25 EN; recarga 180 ticks / 6 s; alcance 260; raio 110; dano-base 0; cura/vida restaurada-base 0; escopo `NONE`.
- **Quantidade e tempo:** Consultas periódicas de controle até a duração; nenhum impacto de dano. Metadados: hits=1, intervalo=0 ticks, duração=120 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `GROUND` — Área selecionável. Reduz a velocidade em 50% por 1 s.
- **Sprite/loader:** `assets/skills/spirit_5.png`; manifesto efetivo `assets/skills/manifest.properties`; 15 frames de 128x128 px, desenho 160x160 px, 120 ms/frame.
- **Componentes visuais:** sem atlas auxiliar específico; usa o clip principal/fallback existente.
- **Servidor:** padrão `QUICKSAND`, [class Zone](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L557). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [public void draw(](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L200) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; sem cenário unitário exclusivo deste ID identificado: a evidência específica é de família/cast e loader.
- **Limitações verificadas:** Slow de área sem dano. Aliados não são vítimas: previews históricos de Domains divergem (A-09). Gaps comuns A-01 e homologação física pendente.

## `aetheri_1` — Flecha precisa

- **Identificação:** Aetheri; slot 1; requisito nível 15; **Funcional com ressalvas**.
- **Valores atuais:** 10 EN; recarga 36 ticks / 1,2 s; alcance 280; raio 0; dano-base 22; cura/vida restaurada-base 0; escopo `ENEMIES`.
- **Quantidade e tempo:** 1 impacto(s)/pulso(s) declarado(s); contato e sobrevivência condicionam o resultado. Metadados: hits=1, intervalo=0 ticks, duração=0 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `TARGET` — Alvo selecionado. Sem status adicional declarado.
- **Sprite/loader:** `assets/skills/refined-v3/aetheri_1.png`; manifesto efetivo `assets/skills/refined-v3/manifest.properties`; 9 frames de 256x96 px, desenho 96x36 px, 70 ms/frame.
- **Componentes visuais:** `.impact`: `assets/skills/refined-v4/impact.png` (6 frames)
- **Servidor:** padrão `ARROW`, [class Projectile](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L366). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [public void draw(](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L200) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; sem cenário unitário exclusivo deste ID identificado: a evidência específica é de família/cast e loader.
- **Limitações verificadas:** A-03: flecha da skill é dirigida; o básico frontal reutiliza seu ID/arte com regra de ataque básico. Gaps comuns A-01 e homologação física pendente.

## `aetheri_2` — Sentinela

- **Identificação:** Aetheri; slot 2; requisito nível 15; **Funcional com ressalvas**.
- **Valores atuais:** 22 EN; recarga 150 ticks / 5 s; alcance 280; raio 0; dano-base 12; cura/vida restaurada-base 0; escopo `ENEMIES`.
- **Quantidade e tempo:** Quantidade dinâmica por entradas de ataque e AGI durante 120 ticks; hits=1 não é munição. Metadados: hits=1, intervalo=10 ticks, duração=120 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `SELF` — Direta no usuário / sem alvo. Sem status adicional declarado.
- **Sprite/loader:** `assets/skills/refined-v3/aetheri_2.png`; manifesto efetivo `assets/skills/refined-v3/manifest.properties`; 9 frames de 256x96 px, desenho 96x36 px, 70 ms/frame.
- **Componentes visuais:** `.impact`: `assets/skills/refined-v4/impact.png` (6 frames)
- **Servidor:** padrão `SENTRY`, [public boolean sentryAttack](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L214). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [public void draw(](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L200) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; [sentryKeepsFiringForFourSecondsWithoutFixedAmmo](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L261), [agilityIncreasesTheNumberOfTargetShotsInsideTheSameFourSeconds](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L316), [sentryExpiryAndDisconnectReleaseOnlyTheirOwnMovementLock](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L304).
- **Limitações verificadas:** A-05: quantidade 1 do catálogo e campo wire sentryShots não são munição; são estado/janela de 4 s. Gaps comuns A-01 e homologação física pendente.

## `aetheri_3` — Chuva de flechas

- **Identificação:** Aetheri; slot 3; requisito nível 15; **Funcional e testada**.
- **Valores atuais:** 30 EN; recarga 180 ticks / 6 s; alcance 300; raio 100; dano-base 10; cura/vida restaurada-base 0; escopo `ENEMIES`.
- **Quantidade e tempo:** 5 impacto(s)/pulso(s) declarado(s); contato e sobrevivência condicionam o resultado. Metadados: hits=5, intervalo=12 ticks, duração=60 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `GROUND` — Área selecionável. Sem status adicional declarado.
- **Sprite/loader:** `assets/skills/refined-v3/aetheri_3.png`; manifesto efetivo `assets/skills/refined-v3/manifest.properties`; 9 frames de 256x256 px, desenho 220x220 px, 70 ms/frame.
- **Componentes visuais:** `.impact`: `assets/skills/refined-v4/impact.png` (6 frames); `.travel`: `assets/skills/refined-v3/aetheri_1.png` (9 frames)
- **Servidor:** padrão `RAIN`, [class ArrowRain](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L499). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [private static void drawArrowRain](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L397) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; [arrowRainDamagesOnlyOnGroundArrivalAndCompletesAllFiveWaves](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L592).
- **Limitações verificadas:** Cinco ondas; cada chegada aplica um pulso, independentemente das cinco flechas visuais por onda. Gaps comuns A-01 e homologação física pendente.

## `aetheri_4` — Flecha de energia

- **Identificação:** Aetheri; slot 4; requisito nível 15; **Funcional e testada**.
- **Valores atuais:** 25 EN; recarga 150 ticks / 5 s; alcance 320; raio 28; dano-base 34; cura/vida restaurada-base 0; escopo `ENEMIES`.
- **Quantidade e tempo:** 1 impacto(s)/pulso(s) declarado(s); contato e sobrevivência condicionam o resultado. Metadados: hits=1, intervalo=0 ticks, duração=0 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `LINE` — Frontal / smartcast. Sem status adicional declarado.
- **Sprite/loader:** `assets/skills/refined-v3/aetheri_4.png`; manifesto efetivo `assets/skills/refined-v3/manifest.properties`; 9 frames de 256x96 px, desenho 240x90 px, 70 ms/frame.
- **Componentes visuais:** `.impact`: `assets/skills/refined-v4/impact.png` (6 frames)
- **Servidor:** padrão `LINE_PROJECTILE`, [class TravelingLine](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L427). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [public void draw(](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L200) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; sem cenário unitário exclusivo deste ID identificado: a evidência específica é de família/cast e loader.
- **Limitações verificadas:** Energia azul, sem slow/stun de gelo no catálogo. Para no primeiro contato. Gaps comuns A-01 e homologação física pendente.

## `aetheri_5` — Leque de flechas

- **Identificação:** Aetheri; slot 5; requisito nível 15; **Funcional com ressalvas**.
- **Valores atuais:** 30 EN; recarga 180 ticks / 6 s; alcance 220; raio 80; dano-base 28; cura/vida restaurada-base 0; escopo `ENEMIES`.
- **Quantidade e tempo:** 7 projéteis; no máximo 1 acerto por vítima/geração por ativação. Metadados: hits=7, intervalo=0 ticks, duração=0 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `CONE` — Cone frontal / smartcast. Sem status adicional declarado.
- **Sprite/loader:** `assets/skills/refined-v3/aetheri_5.png`; manifesto efetivo `assets/skills/refined-v3/manifest.properties`; 9 frames de 256x192 px, desenho 224x168 px, 70 ms/frame.
- **Componentes visuais:** `.impact`: `assets/skills/refined-v4/impact.png` (6 frames); `.travel`: `assets/skills/refined-v3/aetheri_5.travel.png` (9 frames)
- **Servidor:** padrão `CONE_VOLLEY`, [case CONE_VOLLEY ->](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L175). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [public void draw(](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L200) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; [coneVolleyLaunchesSevenDifferentTravelingArrowsWithoutSevenfoldDamage](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L607).
- **Limitações verificadas:** A-06: sete raios, com sharedHits por identidade/geração; uma vítima recebe no máximo um acerto por ativação. Gaps comuns A-01 e homologação física pendente.

## `light_1` — Cura básica

- **Identificação:** Luz; slot 1; requisito nível 15; **Funcional e testada**.
- **Valores atuais:** 12 EN; recarga 60 ticks / 2 s; alcance 220; raio 0; dano-base 20; cura/vida restaurada-base 30; escopo `MOBS_ONLY`.
- **Quantidade e tempo:** 1 aplicação por destinatário elegível. Metadados: hits=1, intervalo=0 ticks, duração=0 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `TARGET` — Alvo selecionado. Sem status adicional declarado.
- **Sprite/loader:** `assets/skills/light_1.png`; manifesto efetivo `assets/skills/manifest.properties`; 15 frames de 128x128 px, desenho 128x128 px, 85 ms/frame.
- **Componentes visuais:** sem atlas auxiliar específico; usa o clip principal/fallback existente.
- **Servidor:** padrão `HEAL`, [case HEAL ->](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L185). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [public void draw(](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L200) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; [lightHealsAlliesDamagesOnlyMobsRevivesAndCleanses](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L341), [fullHealthHealingAndNpcTargetsCannotConsumeResources](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L674).
- **Limitações verificadas:** Híbrida: autocura sem seleção, cura aliado ou dano apenas a mob; não implementa PvP. Gaps comuns A-01 e homologação física pendente.

## `light_2` — Cura em área

- **Identificação:** Luz; slot 2; requisito nível 15; **Funcional e testada**.
- **Valores atuais:** 25 EN; recarga 150 ticks / 5 s; alcance 0; raio 110; dano-base 0; cura/vida restaurada-base 24; escopo `NONE`.
- **Quantidade e tempo:** 1 aplicação por destinatário elegível. Metadados: hits=1, intervalo=0 ticks, duração=0 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `SELF` — Direta no usuário / sem alvo. Sem status adicional declarado.
- **Sprite/loader:** `assets/skills/light_2.png`; manifesto efetivo `assets/skills/manifest.properties`; 20 frames de 128x128 px, desenho 192x192 px, 95 ms/frame.
- **Componentes visuais:** sem atlas auxiliar específico; usa o clip principal/fallback existente.
- **Servidor:** padrão `AREA_HEAL`, [case AREA_HEAL ->](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L189). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [public void draw(](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L200) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; [lightHealsAlliesDamagesOnlyMobsRevivesAndCleanses](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L341).
- **Limitações verificadas:** Cura cada aliado ferido elegível uma vez, respeita HP máximo e linha de visão. Gaps comuns A-01 e homologação física pendente.

## `light_3` — Reviver

- **Identificação:** Luz; slot 3; requisito nível 15; **Funcional e testada**.
- **Valores atuais:** 40 EN; recarga 900 ticks / 30 s; alcance 160; raio 0; dano-base 0; cura/vida restaurada-base 40; escopo `NONE`.
- **Quantidade e tempo:** 1 aplicação por destinatário elegível. Metadados: hits=1, intervalo=0 ticks, duração=0 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `ALLY` — Aliado selecionado (ou usuário, quando permitido). Sem status adicional declarado.
- **Sprite/loader:** `assets/skills/light_3.png`; manifesto efetivo `assets/skills/manifest.properties`; 20 frames de 128x128 px, desenho 160x160 px, 110 ms/frame.
- **Componentes visuais:** sem atlas auxiliar específico; usa o clip principal/fallback existente.
- **Servidor:** padrão `REVIVE`, [case REVIVE ->](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L196). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [public void draw(](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L200) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; [reviveRefusesAnOccupiedBodyBeforeSpendingEnergy](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L356), [healingAndRevivalEffectsAreEmittedAtTheRecipientsFeet](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L684).
- **Limitações verificadas:** Revive não restaura energia, não transporta corpo e exige apoio livre. Gaps comuns A-01 e homologação física pendente.

## `light_4` — Purificação

- **Identificação:** Luz; slot 4; requisito nível 15; **Funcional com ressalvas**.
- **Valores atuais:** 18 EN; recarga 180 ticks / 6 s; alcance 220; raio 0; dano-base 0; cura/vida restaurada-base 0; escopo `NONE`.
- **Quantidade e tempo:** 1 aplicação por destinatário elegível. Metadados: hits=1, intervalo=0 ticks, duração=0 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `ALLY` — Aliado selecionado (ou usuário, quando permitido). Sem status adicional declarado.
- **Sprite/loader:** `assets/skills/light_4.png`; manifesto efetivo `assets/skills/manifest.properties`; 4 frames de 128x128 px, desenho 128x128 px, 75 ms/frame.
- **Componentes visuais:** sem atlas auxiliar específico; usa o clip principal/fallback existente.
- **Servidor:** padrão `CLEANSE`, [case CLEANSE ->](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L197). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [public void draw(](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L200) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; [lightHealsAlliesDamagesOnlyMobsRevivesAndCleanses](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L341).
- **Limitações verificadas:** A-08: remove root/stun/slow do jogador; não há canal público de DoT de skills contra jogadores nesta extensão. Gaps comuns A-01 e homologação física pendente.

## `light_5` — Cura superior

- **Identificação:** Luz; slot 5; requisito nível 15; **Funcional e testada**.
- **Valores atuais:** 30 EN; recarga 210 ticks / 7 s; alcance 240; raio 0; dano-base 40; cura/vida restaurada-base 65; escopo `MOBS_ONLY`.
- **Quantidade e tempo:** 1 aplicação por destinatário elegível. Metadados: hits=1, intervalo=0 ticks, duração=0 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `TARGET` — Alvo selecionado. Sem status adicional declarado.
- **Sprite/loader:** `assets/skills/light_5.png`; manifesto efetivo `assets/skills/manifest.properties`; 15 frames de 128x128 px, desenho 176x176 px, 70 ms/frame.
- **Componentes visuais:** sem atlas auxiliar específico; usa o clip principal/fallback existente.
- **Servidor:** padrão `HEAL`, [case HEAL ->](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L185). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [public void draw(](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L200) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; [lightHealsAlliesDamagesOnlyMobsRevivesAndCleanses](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L341).
- **Limitações verificadas:** 65 de cura-base ou 40 de dano-base a mob; não confundir cura com dano. Gaps comuns A-01 e homologação física pendente.

## `martial_1` — Soco preciso

- **Identificação:** Artista Marcial; slot 1; requisito nível 15; **Funcional e testada**.
- **Valores atuais:** 10 EN; recarga 36 ticks / 1,2 s; alcance 65; raio 0; dano-base 24; cura/vida restaurada-base 0; escopo `ENEMIES`.
- **Quantidade e tempo:** 1 impacto(s)/pulso(s) declarado(s); contato e sobrevivência condicionam o resultado. Metadados: hits=1, intervalo=0 ticks, duração=0 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `TARGET` — Alvo selecionado. Sem status adicional declarado.
- **Sprite/loader:** `assets/skills/refined-v4/martial_1.png`; manifesto efetivo `assets/skills/refined-v4/manifest.properties`; 6 frames de 192x128 px, desenho 96x64 px, 40 ms/frame.
- **Componentes visuais:** sem atlas auxiliar específico; usa o clip principal/fallback existente.
- **Servidor:** padrão `PUNCH`, [case PUNCH ->](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L198). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [public void draw(](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L200) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; sem cenário unitário exclusivo deste ID identificado: a evidência específica é de família/cast e loader.
- **Limitações verificadas:** Golpe em alvo próximo; efeito visual não é pose completa nova. Gaps comuns A-01 e homologação física pendente.

## `martial_2` — Sequência de socos

- **Identificação:** Artista Marcial; slot 2; requisito nível 15; **Funcional e testada**.
- **Valores atuais:** 18 EN; recarga 100 ticks / 3,33 s; alcance 65; raio 0; dano-base 9; cura/vida restaurada-base 0; escopo `ENEMIES`.
- **Quantidade e tempo:** 4 impacto(s)/pulso(s) declarado(s); contato e sobrevivência condicionam o resultado. Metadados: hits=4, intervalo=4 ticks, duração=12 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `TARGET` — Alvo selecionado. Sem status adicional declarado.
- **Sprite/loader:** `assets/skills/refined-v4/martial_1.png`; manifesto efetivo `assets/skills/refined-v4/manifest.properties`; 6 frames de 192x128 px, desenho 96x64 px, 40 ms/frame.
- **Componentes visuais:** sem atlas auxiliar específico; usa o clip principal/fallback existente.
- **Servidor:** padrão `MULTI_HIT`, [class Multi](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L514). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [public void draw(](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L200) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; sem cenário unitário exclusivo deste ID identificado: a evidência específica é de família/cast e loader.
- **Limitações verificadas:** Quatro impactos em alvo/geração revalidados; não há deslocamento artificial até o alvo. Gaps comuns A-01 e homologação física pendente.

## `martial_3` — Impacto sísmico

- **Identificação:** Artista Marcial; slot 3; requisito nível 15; **Funcional e testada**.
- **Valores atuais:** 22 EN; recarga 150 ticks / 5 s; alcance 0; raio 110; dano-base 28; cura/vida restaurada-base 0; escopo `ENEMIES`.
- **Quantidade e tempo:** 1 impacto(s)/pulso(s) declarado(s); contato e sobrevivência condicionam o resultado. Metadados: hits=1, intervalo=0 ticks, duração=0 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `SELF` — Direta no usuário / sem alvo. Sem status adicional declarado.
- **Sprite/loader:** `assets/skills/refined-v7/martial_3.png`; manifesto efetivo `assets/skills/refined-v7/manifest.properties`; 16 frames de 320x240 px, desenho 240x180 px, 56 ms/frame.
- **Componentes visuais:** sem atlas auxiliar específico; usa o clip principal/fallback existente.
- **Servidor:** padrão `GROUND_STOMP`, [case MELEE_AREA, GROUND_STOMP](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L149). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [public void draw(](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L200) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; sem cenário unitário exclusivo deste ID identificado: a evidência específica é de família/cast e loader.
- **Limitações verificadas:** Área imediata ao redor do usuário; fissura visual não cria hits extras. Gaps comuns A-01 e homologação física pendente.

## `martial_4` — Body slam

- **Identificação:** Artista Marcial; slot 4; requisito nível 15; **Funcional com ressalvas**.
- **Valores atuais:** 25 EN; recarga 180 ticks / 6 s; alcance 55; raio 35; dano-base 34; cura/vida restaurada-base 0; escopo `ENEMIES`.
- **Quantidade e tempo:** 1 impacto(s)/pulso(s) declarado(s); contato e sobrevivência condicionam o resultado. Metadados: hits=1, intervalo=0 ticks, duração=0 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `TARGET` — Alvo selecionado. Atordoa por 1 s; impede movimento, ataques e habilidades.
- **Sprite/loader:** `assets/skills/refined-v8/martial_4.png`; manifesto efetivo `assets/skills/refined-v8/manifest.properties`; 8 frames de 192x192 px, desenho 96x96 px, 25 ms/frame.
- **Componentes visuais:** `.impact`: `assets/skills/refined-v8/martial_4.impact.png` (8 frames); `.travel`: `assets/skills/refined-v8/martial_4.png` (8 frames)
- **Servidor:** padrão `BODY_SLAM`, [case BODY_SLAM ->](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L199). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [public void draw(](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L200) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; [bodySlamPublishesGripAndTheCollisionResolvedLandingWithoutChangingDamage](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L426), [bodySlamReadsTheOtherPlayersCurrentPositionBeforeMovingTheVictim](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L497).
- **Limitações verificadas:** A-10: dano/stun/deslocamento são imediatos; pegada e pancada se separam apenas visualmente. Gaps comuns A-01 e homologação física pendente.

## `martial_5` — Combo de punhos e chutes

- **Identificação:** Artista Marcial; slot 5; requisito nível 15; **Funcional e testada**.
- **Valores atuais:** 35 EN; recarga 240 ticks / 8 s; alcance 170; raio 95; dano-base 8; cura/vida restaurada-base 0; escopo `ENEMIES`.
- **Quantidade e tempo:** 6 impacto(s)/pulso(s) declarado(s); contato e sobrevivência condicionam o resultado. Metadados: hits=6, intervalo=4 ticks, duração=20 ticks, janela de checkpoint=0 ticks.
- **Conjuração/status:** `CONE` — Cone frontal / smartcast. Sem status adicional declarado.
- **Sprite/loader:** `assets/skills/refined-v3/martial_5.png`; manifesto efetivo `assets/skills/refined-v3/manifest.properties`; 9 frames de 320x192 px, desenho 220x180 px, 70 ms/frame.
- **Componentes visuais:** sem atlas auxiliar específico; usa o clip principal/fallback existente.
- **Servidor:** padrão `CONE_COMBO`, [class Zone](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java#L557). Reutiliza dano/loot/controles de RpgWorld e SharedRpgEncounter.
- **Cliente:** [public void draw(](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java#L200) e integrações comuns acima. O efeito não aplica dano.
- **Testes:** T0 + V0; [martialConeDeliversAllSixHitsIncludingTheLastDurationBoundary](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L252), [martialComboReachesFurtherWhileKeepingItsFrontalCone](../server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java#L334).
- **Limitações verificadas:** Seis pulsos; a origem acompanha o usuário e a direção fica a da conjuração. Gaps comuns A-01 e homologação física pendente.

## Resultado da classificação

- Funcional com ressalvas: **16**.
- Funcional e testada: **24**.
- Parcialmente implementada: 0; apenas documentada: 0; indisponível: 0.

Detalhes e severidade dos achados A-01 a A-10 em [TASK-223-RELATORIO-FINAL.md](TASK-223-RELATORIO-FINAL.md). Resultados executados em [TASK-223-TESTES.md](TASK-223-TESTES.md).
