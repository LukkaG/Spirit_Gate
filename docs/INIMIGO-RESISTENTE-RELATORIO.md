# Inimigo resistente para sequências de skills

Data: 03/11/2026. Branch `beco`, HEAD `6d72c65`. Alteração local, sem publicação.

O slime padrão tinha 40 HP e morria rapidamente nas sequências de habilidades.
O mundo multiplayer com revisão de conteúdo agora aplica um piso de **10.000 HP**
a seus inimigos. Para o slime, isso representa 250 vezes a vida anterior.
O inimigo continua recebendo dano e pode morrer; não foi adicionada invulnerabilidade.

## Arquivos e comportamento

- `server/src/main/java/com/projetok/multiplayer/RpgSessionWorld.java`: define
  o piso ao habilitar revisões. Inclui a prévia de skills e sessões compatíveis
  de conteúdo V4–V7, independentemente do nível sintético dos personagens.
- `server/src/main/java/com/projetok/rpg/EnemyService.java`: aceita o piso
  por encontro, mantendo os valores do catálogo como padrão nos construtores existentes.
- `server/src/main/java/com/projetok/rpg/SharedRpgEncounter.java`: transmite
  essa configuração à população e a conserva na troca de revisão.
- `server/src/test/java/com/projetok/rpg/EnemyDurabilityTest.java`: três testes
  de resistência, transmissão, dano, morte, respawn e continuidade entre revisões.

Offline, Coop e o combate multiplayer sem revisão conservam os HP anteriores.
O catálogo global, saves, atributos ofensivos e regras de dano não foram alterados.
O máximo de 10.000 HP já é aceito pelo protocolo, cujo formato permanece intacto.
Respawns recuperam a vida máxima; a troca de revisão preserva o dano já recebido
nos spawns que continuam no mapa. Novos spawns recebem o mesmo piso.

## Verificação

JDK 17 e Maven 3.10.0, cache local existente, modo offline:

```powershell
mvn -o -f server/pom.xml "-Dmaven.repo.local=C:/Users/lucas/Documents/GitHub/Projeto-K/.maven-repository" "-Djava.awt.headless=true" "-Dspiritgate.logs.dir=C:/Users/lucas/Documents/GitHub/Projeto-K/.task-checkpoints/ENEMY-DURABILITY/test-errors" "-Dtest=EnemyDurabilityTest,EnemyRpgTest,EnemyAiTest,CorujaoEnemyTest,Task205EnemyTargetsTest,Task206SharedLootTest,SharedRpgCombatTest,CooperativeEncounterTest,RpgSkillEngineTest,SkillProtocolTest,TargetCombatServerTest,Task212GameplayIntegrityTest,WorldRevisionTest" test
```

Resultado: **158 testes, zero falhas, erros ou skips; BUILD SUCCESS**.
Inclui duas conexões TCP locais nos testes de protocolo/skills e regressões de
combate, IA, recompensas, saves offline, Coop e revisões de conteúdo.
`git diff --check` dos fontes alterados passou. A revisão independente do delta
não encontrou regressões. Relatórios e cópias anteriores ficam em
`.task-checkpoints/ENEMY-DURABILITY/`.

## Uso, limites e reversão

Reinicie a sessão a partir dos fontes atuais. A prévia pode ser reaberta por
`tools/testar-skills.cmd`, que recompila o jogo. Uma sessão já aberta conserva
as classes anteriores. O balanceamento visual, a duração desejada do combate
e o teste físico/Radmin ainda dependem de conferência no jogo. Pacotes e EXEs
anteriores não foram atualizados.

Para reverter, compare os três fontes com suas cópias em
`.task-checkpoints/ENEMY-DURABILITY/before/` e retire somente o delta desta entrega,
preservando alterações posteriores. O teste e este relatório podem ser removidos
individualmente. Não restaure arquivos pelo HEAD: os fontes já tinham alterações
locais antes desta tarefa.
