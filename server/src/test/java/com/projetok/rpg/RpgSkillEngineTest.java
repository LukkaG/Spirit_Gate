package com.projetok.rpg;

import com.projetok.domain.entity.Position;
import com.projetok.domain.movement.CollisionService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import static org.junit.jupiter.api.Assertions.*;

/** Exercita as quarenta técnicas no encontro real, sem rede, banco ou arquivos pessoais. */
class RpgSkillEngineTest {
    private static final int ENEMY = 1_000_100;

    /** Mundo durável de teste permite observar todos os pulsos sem matar o alvo no primeiro golpe. */
    private static final class Fixture {
        final RpgSkillEngine engine = new RpgSkillEngine();
        final SharedRpgEncounter encounter;
        final RpgWorld world;
        final Map<Integer, RpgWorld> players = new LinkedHashMap<>();
        final EnemyService.Enemy enemy;
        long tick;
        Fixture(String kit) { this(kit, List.of()); }
        Fixture(String kit, List<CollisionService.Obstacle> obstacles) {
            this(kit, obstacles, Map.of("a_target", "slime|forest|185|300"));
        }
        Fixture(String kit, List<CollisionService.Obstacle> obstacles, Map<String, String> definitions) {
            var space = new CollisionService.Space(960, 640, obstacles, List.of());
            var scenes = new RpgSceneSet("forest", Map.of("forest", new RpgSceneSet.Geometry(space, new Position(140, 300))), definitions, Map.of(), Map.of(), Map.of());
            String race = switch (kit) { case "water", "wind", "thunder", "fire" -> "reaper"; case "spirit" -> "spirit"; case "aetheri" -> "aetheri"; default -> "human"; };
            world = new RpgWorld(CharacterIdentity.create("Lançador", race, "azure"), scenes);
            world.useNetworkFootprint();
            world.awardExperience(RpgProgression.builtin().threshold(15));
            String domain = switch (kit) { case "aetheri" -> "bow_long"; case "spirit" -> "darkness_control"; default -> kit; };
            assertTrue(world.chooseDomain(domain));
            encounter = new SharedRpgEncounter(Map.of("forest", space), definitions, SharedRpgEncounter.RewardPolicy.DISABLED);
            encounter.join(1, world); players.put(1, world);
            enemy = world.skillEnemies().get(0); world.skillEnemies().forEach(e -> e.hp = 1000);
        }
        /** Aliado usa outro apoio e não divide a instância privada de personagem do lançador. */
        RpgWorld ally() {
            var space = new CollisionService.Space(960, 640, List.of(), List.of());
            var scenes = new RpgSceneSet("forest", Map.of("forest", new RpgSceneSet.Geometry(space, new Position(140, 350))), Map.of(), Map.of(), Map.of(), Map.of());
            var ally = new RpgWorld(CharacterIdentity.create("Aliado", "human", "azure"), scenes);
            ally.useNetworkFootprint(); encounter.join(2, ally); players.put(2, ally); return ally;
        }
        /** Avança a mesma ordem de relógio usada no coordenador de rede. */
        void advance(int count) {
            for (int i = 0; i < count; i++) {
                tick++; refreshBodies(); players.values().forEach(w -> w.tick(0, 0)); encounter.step(tick); refreshBodies(); engine.clock(tick); engine.tick(players);
            }
        }
        /** Colisões de movimento consultam mobs e aliados atuais, como na sessão multiplayer. */
        void refreshBodies(){
            for(var moving:players.values()){
                var bodies=new java.util.ArrayList<CollisionService.Body>();
                for(var other:players.values())if(other!=moving&&other.networkState().hp()>0&&!other.skillIntangible())bodies.add(CollisionService.Body.block(new com.projetok.domain.entity.GameEntity(-1,"Aliado",other.skillPosition()){},other.physicalRadius()));
                for(var enemy:moving.skillEnemies())if(enemy.hp>0)bodies.add(new CollisionService.Body(enemy,19));
                moving.setSyntheticBodies(bodies);
            }
        }
        boolean cast(int slot) { engine.clock(tick); return engine.cast(1, world, new RpgSkillEngine.Cast(slot, ENEMY, enemy.generation, 220, 300), players); }
        /** Espera o contato do skillshot, sem avançar os ticks seguintes ao empurrão. */
        void repulsionContact(){for(int count=0;count<30&&!engine.state(1).windCheckpoint();count++)advance(1);assertTrue(engine.state(1).windCheckpoint());}
    }

    @TestFactory Stream<DynamicTest> everyRequestedSkillExecutesItsOwnSlotAndConsumesOnlyItsDeclaredCost() {
        return RpgSkillCatalog.all().stream().map(skill -> DynamicTest.dynamicTest(skill.id() + " — " + skill.name(), () -> {
            var f = new Fixture(skill.kit());
            int target = ENEMY;
            if (skill.id().equals("wind_5")) {assertTrue(f.cast(4));f.repulsionContact();}
            if (skill.kit().equals("light")) {
                var ally = f.ally();
                if (skill.slot() == 3) ally.receiveDamage(10000); else ally.receiveDamage(20);
                if (skill.targeting() == RpgSkillCatalog.Targeting.ALLY || skill.slot() == 1 || skill.slot() == 5) target = 2;
            }
            int energy = f.world.networkState().stamina();
            assertTrue(f.engine.cast(1, f.world, new RpgSkillEngine.Cast(skill.slot(), target, f.enemy.generation, 220, 300), f.players));
            assertEquals(energy - skill.energyCost() * 10, f.world.networkState().stamina());
            assertEquals(skill.cooldownTicks(), f.engine.state(1).cooldowns().get(skill.slot() - 1));
            if(skill.id().equals("martial_2"))f.advance(1);
            if(skill.mechanics().pattern()==RpgSkillCatalog.Pattern.SENTRY)assertEquals(4,f.engine.state(1).sentryShots());
            else assertTrue(f.engine.events().stream().anyMatch(e -> e.skill().equals(skill.id())));
            f.advance(40);
        }));
    }

    @Test void projectileTravelsBeforeDamageAndOldGenerationCannotBeHit() {
        var f = new Fixture("water");
        assertTrue(f.cast(1)); assertEquals(1000, f.enemy.hp);
        f.advance(1); assertEquals(1000, f.enemy.hp);
        f.advance(1); assertEquals(980, f.enemy.hp); assertTrue(f.enemy.skillSlowUntil > f.tick);
        assertFalse(f.cast(1));
        var stale = new Fixture("water"); assertTrue(stale.cast(1)); stale.enemy.generation++; stale.advance(10); assertEquals(1000, stale.enemy.hp);
    }

    @Test void rootPreservesEnemyAttackAndExpiresAfterSixtyGlobalTicks() {
        var f = new Fixture("water"); int hp = f.world.networkState().hp();
        assertTrue(f.cast(3)); assertEquals(60, f.enemy.skillRootUntil);
        Position original = f.enemy.getPosition(); f.advance(1);
        assertTrue(f.world.networkState().hp() < hp); assertEquals(original, f.enemy.getPosition());
        assertTrue(f.world.skillMove(new Position(90, 300))); f.advance(58);
        assertEquals(original, f.enemy.getPosition()); f.advance(1); assertNotEquals(original, f.enemy.getPosition());
    }

    @Test void stunStopsPlayerMovementAndAbilitiesWhileRootStillAllowsAttacking() {
        var f = new Fixture("martial");
        f.world.skillApplyControl(new RpgSkillCatalog.Effect(RpgSkillCatalog.EffectType.ROOT, 60, 0, 0, 0));
        Position start = f.world.skillPosition(); f.world.tick(1, 0); assertEquals(start, f.world.skillPosition());
        assertTrue(f.cast(1));
        f.world.skillApplyControl(new RpgSkillCatalog.Effect(RpgSkillCatalog.EffectType.STUN, 60, 0, 0, 0));
        assertFalse(f.cast(2)); assertFalse(f.world.attack()); assertFalse(f.world.castForwardAbility());
        f.world.tick(1, 0); assertEquals(start, f.world.skillPosition());
    }

    @Test void chainHasFourTimedHitsAndDecreasingFallbackDamage() {
        var f = new Fixture("thunder"); assertTrue(f.cast(3));
        f.advance(3); assertEquals(980, f.enemy.hp);
        f.advance(8); assertEquals(965, f.enemy.hp);
        f.advance(8); assertEquals(954, f.enemy.hp);
        f.advance(8); assertEquals(946, f.enemy.hp);
        f.advance(20); assertEquals(946, f.enemy.hp);
        assertEquals(4, f.encounter.events().stream().filter(e -> e.kind().equals("HIT")).count());
    }

    @Test void chainSpreadsToFourDifferentEnemiesBeforeRepeating() {
        var f = new Fixture("thunder", List.of(), Map.of("a_target", "slime|forest|185|300",
                "b_target", "slime|forest|245|300", "c_target", "slime|forest|305|300", "d_target", "slime|forest|365|300"));
        assertTrue(f.cast(3)); f.advance(30);
        assertTrue(f.world.skillEnemies().stream().allMatch(e -> e.hp == 980));
        assertEquals(4, f.encounter.events().stream().filter(e -> e.kind().equals("HIT")).map(SharedRpgEncounter.Event::enemy).distinct().count());
    }

    @Test void multihitAndBleedUseSeparateClocksWithoutStackingDots() {
        var f = new Fixture("wind"); assertTrue(f.cast(2));
        f.advance(13); assertEquals(970, f.enemy.hp);
        f.advance(17); assertEquals(970, f.enemy.hp);
        f.advance(1); assertEquals(970, f.enemy.hp);
        f.advance(1); assertEquals(967, f.enemy.hp);
        f.advance(30); assertEquals(964, f.enemy.hp);
    }

    @Test void controlOnlyAreasNeverCauseDamageAndFollowupConsumesCheckpointOnce() {
        for (int slot : List.of(4, 5)) {
            var f = new Fixture("spirit"); assertTrue(f.cast(slot)); f.advance(50); assertEquals(1000, f.enemy.hp);
        }
        var f = new Fixture("wind"); int energy = f.world.networkState().stamina();
        assertFalse(f.cast(5)); assertEquals(energy, f.world.networkState().stamina());
        assertTrue(f.cast(4)); assertFalse(f.engine.state(1).windCheckpoint());f.repulsionContact();
        float pushed = f.enemy.getPosition().x(); assertTrue(pushed > 185);
        assertTrue(f.cast(5)); assertFalse(f.engine.state(1).windCheckpoint()); assertTrue(f.enemy.getPosition().x() < pushed);
        assertFalse(f.cast(5));
        var expired = new Fixture("wind"); assertTrue(expired.cast(4));expired.repulsionContact();expired.advance(150);assertFalse(expired.cast(5));
    }

    @Test void frontalRepulsionNeedsNoTargetAndOnlyCreatesCheckpointOnActualContact(){
        var f=new Fixture("wind",List.of(),Map.of("a_target","slime|forest|300|300"));f.enemy.skillRootUntil=1000;
        assertTrue(f.engine.cast(1,f.world,new RpgSkillEngine.Cast(4,0,0,0,600),f.players));
        assertEquals(1000,f.enemy.hp);assertFalse(f.engine.state(1).windCheckpoint());
        f.advance(5);assertEquals(1000,f.enemy.hp);assertFalse(f.engine.state(1).windCheckpoint());
        f.repulsionContact();assertEquals(984,f.enemy.hp);assertEquals(390,f.enemy.getPosition().x(),.001);
        var launch=f.engine.events().stream().filter(e->e.skill().equals("wind_4")).findFirst().orElseThrow();
        assertEquals(300,launch.y());assertEquals(380,launch.x(),.001,"A mira enviada não muda o facing frontal");
        Position checkpoint=f.enemy.getPosition();assertTrue(f.cast(5));
        var returned=f.engine.events().stream().filter(e->e.skill().equals("wind_5")).toList();assertEquals(1,returned.size());
        assertEquals(checkpoint.x(),returned.get(0).fromX());assertEquals(f.world.skillPosition().x(),returned.get(0).x());
    }
    @Test void frontalRepulsionMissesAnOffAxisSelectedEnemyWithoutGrantingReturn(){
        var f=new Fixture("wind",List.of(),Map.of("a_target","slime|forest|300|360"));f.enemy.skillRootUntil=1000;
        assertTrue(f.cast(4));f.advance(20);assertEquals(1000,f.enemy.hp);assertFalse(f.engine.state(1).windCheckpoint());
        int energy=f.world.networkState().stamina();assertFalse(f.cast(5));assertEquals(energy,f.world.networkState().stamina());
        assertEquals(1,f.engine.events().stream().filter(e->e.skill().equals("wind_4")).count(),"Errar no alcance não cria impacto falso");
    }
    @Test void frontalRepulsionStopsAtWallAndNeverPushesTheEnemyBehindIt(){
        var f=new Fixture("wind",List.of(new CollisionService.Obstacle(210,250,1,100)),Map.of("a_target","slime|forest|300|300"));f.enemy.skillRootUntil=1000;
        assertTrue(f.engine.cast(1,f.world,new RpgSkillEngine.Cast(4,0,0,0,0),f.players));f.advance(20);
        assertEquals(1000,f.enemy.hp);assertEquals(300,f.enemy.getPosition().x());assertFalse(f.engine.state(1).windCheckpoint());
        assertTrue(f.engine.events().stream().anyMatch(e->e.skill().equals("wind_4")&&e.fromX()==e.x()&&e.x()<210));
    }
    @Test void returnIgnoresSelectedEnemyAndNeverPullsAnUnmarkedEnemy(){
        var f=new Fixture("wind",List.of(),Map.of("a_target","slime|forest|185|300","b_target","slime|forest|300|400"));
        assertTrue(f.cast(4));f.repulsionContact();int energy=f.world.networkState().stamina();var second=f.world.skillEnemies().get(1);
        Position untouched=second.getPosition();
        assertTrue(f.engine.cast(1,f.world,new RpgSkillEngine.Cast(5,1_000_000+second.getId(),second.generation,0,0),f.players));
        assertFalse(f.engine.state(1).windCheckpoint());assertEquals(energy-180,f.world.networkState().stamina());
        assertEquals(untouched,second.getPosition());assertEquals(1000,second.hp);assertEquals(968,f.enemy.hp);
    }
    @Test void returnRejectsRespawnedMarkedEnemyWithoutSpending(){
        var f=new Fixture("wind");assertTrue(f.cast(4));f.repulsionContact();int energy=f.world.networkState().stamina();
        f.enemy.generation++;assertFalse(f.engine.cast(1,f.world,new RpgSkillEngine.Cast(5,0,0,0,0),f.players));
        assertEquals(energy,f.world.networkState().stamina());
    }
    /** O retorno acompanha as marcas, mesmo se as vítimas se separaram depois do empurrão. */
    @Test void untargetedReturnPullsAllMarkedEnemiesButLeavesNearbyUnmarkedEnemyAlone(){
        var f=new Fixture("wind",List.of(),Map.of("a_target","slime|forest|300|300","b_target","slime|forest|300|340","c_target","slime|forest|500|300"));
        f.world.skillEnemies().forEach(e->e.skillRootUntil=1000);
        assertTrue(f.cast(4));f.repulsionContact();var second=f.world.skillEnemies().get(1);var third=f.world.skillEnemies().get(2);
        assertEquals(984,second.hp);second.setPosition(new Position(200,500));third.setPosition(new Position(390,340));
        Position untouched=third.getPosition();double before=Math.hypot(second.getPosition().x()-140,second.getPosition().y()-300);
        assertTrue(f.engine.cast(1,f.world,new RpgSkillEngine.Cast(5,0,0,0,0),f.players));
        assertEquals(968,f.enemy.hp);assertEquals(968,second.hp);assertEquals(1000,third.hp);assertEquals(untouched,third.getPosition());
        assertTrue(Math.hypot(second.getPosition().x()-140,second.getPosition().y()-300)<before);
        assertEquals(2,f.engine.events().stream().filter(e->e.skill().equals("wind_5")).count());
        assertFalse(f.engine.state(1).windCheckpoint());assertFalse(f.cast(5));
    }
    @Test void stalePrimaryMarkDoesNotPreventTheOtherMarkedEnemyFromReturning(){
        var f=new Fixture("wind",List.of(),Map.of("a_target","slime|forest|300|300","b_target","slime|forest|300|340"));
        f.world.skillEnemies().forEach(e->e.skillRootUntil=1000);assertTrue(f.cast(4));f.repulsionContact();
        f.enemy.generation++;var second=f.world.skillEnemies().get(1);
        assertTrue(f.engine.cast(1,f.world,new RpgSkillEngine.Cast(5,0,0,0,0),f.players));
        assertEquals(984,f.enemy.hp);assertEquals(968,second.hp);
    }
    @Test void repulsionDamagesThePrimaryEvenAfterItTravelsBeyondTheOriginalArea() {
        var f = new Fixture("wind"); assertTrue(f.cast(4));f.repulsionContact();
        assertEquals(275, f.enemy.getPosition().x(), .001);
        assertEquals(984, f.enemy.hp);
        assertTrue(f.engine.state(1).windCheckpoint());
    }

    @Test void repulsionCheckpointUsesTheReachablePointBeforeAWall() {
        var f = new Fixture("wind", List.of(new CollisionService.Obstacle(250, 250, 1, 100)));
        assertTrue(f.cast(4));f.repulsionContact();float stopped = f.enemy.getPosition().x();
        // A geometria própria do mob permite tangência, sem a dilatação de cenário do jogador.
        assertEquals(250 - 19, stopped, .001);
        var raw = new CollisionService.Space(960, 640, List.of(new CollisionService.Obstacle(250, 250, 1, 100)), List.of());
        assertTrue(new CollisionService().isFree(f.enemy, stopped, f.enemy.getPosition().y(), 19, raw));
        assertTrue(f.engine.state(1).windCheckpoint());
        assertTrue(f.cast(5));
        assertTrue(f.enemy.getPosition().x() < stopped);
        assertEquals(968, f.enemy.hp);
        assertFalse(f.engine.state(1).windCheckpoint());
    }

    @Test void returnKeepsItsOriginalAreaAndUsesCurrentBodiesForBothVictims() {
        var f = new Fixture("wind", List.of(), Map.of("a_target", "slime|forest|300|300", "b_target", "slime|forest|300|340"));
        var second = f.world.skillEnemies().get(1);
        // Isola os corpos usados pela varredura: a perseguição da IA não deve sobrepô-los durante o voo.
        f.world.skillEnemies().forEach(e->e.skillRootUntil=1000);
        assertTrue(f.cast(4));f.repulsionContact();assertEquals(984, f.enemy.hp); assertEquals(984, second.hp);
        Position beforeFirst = f.enemy.getPosition(), beforeSecond = second.getPosition();
        assertTrue(f.engine.cast(1,f.world,new RpgSkillEngine.Cast(5,0,0,0,0),f.players));
        assertEquals(968, f.enemy.hp); assertEquals(968, second.hp);
        assertTrue(f.enemy.getPosition().x() < beforeFirst.x());
        assertTrue(second.getPosition().x() < beforeSecond.x());
        double separation = Math.hypot(f.enemy.getPosition().x() - second.getPosition().x(), f.enemy.getPosition().y() - second.getPosition().y());
        assertTrue(separation >= 38 - .001, "Os dois corpos circulares de raio 19 precisam permanecer separados");
    }

    @Test void martialConeDeliversAllSixHitsIncludingTheLastDurationBoundary() {
        var f = new Fixture("martial"); assertTrue(f.cast(5));
        assertEquals(992, f.enemy.hp);
        f.advance(19); assertEquals(960, f.enemy.hp);
        f.advance(1); assertEquals(952, f.enemy.hp);
        assertEquals(6, f.encounter.events().stream().filter(e -> e.kind().equals("HIT")).count());
        f.advance(10); assertEquals(952, f.enemy.hp);
    }

    @Test void sentryKeepsFiringForFourSecondsWithoutFixedAmmo() {
        var f = new Fixture("aetheri"); assertTrue(f.cast(2));
        assertEquals(4, f.engine.state(1).sentryShots()); int energy = f.world.networkState().stamina();
        var shot = new RpgSkillEngine.Cast(2, ENEMY, f.enemy.generation, 185, 300);
        for (int i = 0; i < 12; i++) {
            assertTrue(f.engine.sentryAttack(1, f.world, shot, f.players));
            assertFalse(f.engine.sentryAttack(1, f.world, shot, f.players)); f.advance(10);
        }
        assertEquals(0, f.engine.state(1).sentryShots()); assertEquals(856, f.enemy.hp);
        assertFalse(f.engine.sentryAttack(1, f.world, shot, f.players));
        assertTrue(f.world.networkState().stamina() >= energy);
    }

    @Test void basicArrowUsesBasicCooldownAndDamageWithoutSpendingSkillResources() {
        var f=new Fixture("aetheri");int energy=f.world.networkState().stamina();
        assertTrue(f.engine.basicArrow(1,f.world,f.players));assertFalse(f.engine.basicArrow(1,f.world,f.players));
        assertEquals(energy,f.world.networkState().stamina());assertEquals(0,f.engine.state(1).cooldowns().get(0));
        assertEquals(1000,f.enemy.hp);f.advance(2);assertEquals(1000-f.world.attackPower(),f.enemy.hp);
    }
    @Test void basicArrowDoesNotTurnTowardsAnEnemyOutsideItsFrontalLine() {
        var f=new Fixture("aetheri");f.world.faceSyntheticDirection(0,-1);
        assertTrue(f.engine.basicArrow(1,f.world,f.players));var event=f.engine.events().get(0);
        assertEquals(event.fromX(),event.x());assertTrue(event.y()<event.fromY());
        f.advance(16);assertEquals(1000,f.enemy.hp);
    }
    @Test void basicArrowCannotBypassSentryTargetRequirement() {
        var f=new Fixture("aetheri");assertTrue(f.cast(2));
        assertFalse(f.engine.basicArrow(1,f.world,f.players));
        assertFalse(f.engine.sentryAttack(1,f.world,new RpgSkillEngine.Cast(2,0,0,0,0),f.players));
        assertTrue(f.engine.events().isEmpty());
    }
    @Test void sentryStartsWithoutTargetLocksMovementAndCancelsWithoutSecondCost() {
        var f=new Fixture("aetheri");
        assertTrue(f.engine.cast(1,f.world,new RpgSkillEngine.Cast(2,0,0,0,0),f.players));
        assertTrue(f.engine.events().isEmpty(),"Ativação não deve disparar uma flecha sozinha");
        int energy=f.world.networkState().stamina();
        assertFalse(f.world.skillMove(new Position(130,300)));
        assertTrue(f.engine.cast(1,f.world,new RpgSkillEngine.Cast(2,0,0,0,0),f.players));
        assertEquals(energy,f.world.networkState().stamina());assertEquals(0,f.engine.state(1).sentryShots());
        assertEquals(150,f.engine.state(1).cooldowns().get(1));
        assertTrue(f.world.skillMove(new Position(130,300)));
        assertFalse(f.cast(2),"Cancelar não pode zerar a recarga");
    }
    @Test void sentryExpiryAndDisconnectReleaseOnlyTheirOwnMovementLock() {
        var f=new Fixture("aetheri");assertTrue(f.cast(2));f.advance(119);
        assertTrue(f.world.skillMovementBlocked());assertEquals(1,f.engine.state(1).sentryShots());
        f.advance(1);assertFalse(f.world.skillMovementBlocked());assertEquals(0,f.engine.state(1).sentryShots());
        var g=new Fixture("aetheri");assertTrue(g.cast(2));g.engine.remove(1);assertFalse(g.world.skillMovementBlocked());
    }
    @Test void cancelingSentryDuringStunDoesNotCleanseTheStun(){
        var f=new Fixture("aetheri");assertTrue(f.cast(2));
        f.world.skillApplyControl(new RpgSkillCatalog.Effect(RpgSkillCatalog.EffectType.STUN,10,0,0,0));
        assertTrue(f.cast(2));assertEquals(0,f.engine.state(1).sentryShots());assertTrue(f.world.skillMovementBlocked());
        f.advance(10);assertFalse(f.world.skillMovementBlocked());
    }
    @Test void agilityIncreasesTheNumberOfTargetShotsInsideTheSameFourSeconds() {
        int[] counts=new int[2];
        for(int variant=0;variant<2;variant++){
            var f=new Fixture("aetheri");f.world.manualAttributes();
            if(variant==1)for(int i=0;i<20;i++)assertTrue(f.world.allocateAttribute("agility"));
            assertTrue(f.cast(2));var shot=new RpgSkillEngine.Cast(2,ENEMY,f.enemy.generation,0,0);
            for(int tick=0;tick<120;tick++){if(f.engine.sentryAttack(1,f.world,shot,f.players))counts[variant]++;f.advance(1);}
            assertEquals(0,f.engine.state(1).sentryShots());
        }
        assertEquals(12,counts[0]);assertTrue(counts[1]>counts[0],java.util.Arrays.toString(counts));
    }
    @Test void smartcastUsesOnlyFourDirectionsEvenAfterDiagonalFacing() {
        for(var heading:List.of(new Position(1,1),new Position(-1,1),new Position(.2f,1),new Position(.2f,-1))){
            var f=new Fixture("thunder");f.world.faceSyntheticDirection(heading.x(),heading.y());assertTrue(f.cast(2));
            var event=f.engine.events().get(0);assertTrue(event.fromX()==event.x()||event.fromY()==event.y());
            assertTrue(Math.hypot(event.x()-event.fromX(),event.y()-event.fromY())>0);
        }
    }
    @Test void martialComboReachesFurtherWhileKeepingItsFrontalCone() {
        var f=new Fixture("martial",List.of(),Map.of("a_target","slime|forest|285|300"));
        assertTrue(f.cast(5));assertEquals(992,f.enemy.hp);
        var rear=new Fixture("martial",List.of(),Map.of("a_target","slime|forest|40|300"));
        assertTrue(rear.cast(5));assertEquals(1000,rear.enemy.hp);
    }

    @Test void lightHealsAlliesDamagesOnlyMobsRevivesAndCleanses() {
        var f = new Fixture("light"); var ally = f.ally(); ally.receiveDamage(60);
        int before = ally.networkState().hp();
        assertTrue(f.engine.cast(1, f.world, new RpgSkillEngine.Cast(1, 2, 0, 170, 300), f.players));
        assertEquals(before + 30, ally.networkState().hp()); assertEquals(1000, f.enemy.hp);
        assertTrue(f.cast(5)); assertEquals(960, f.enemy.hp);
        ally.skillApplyControl(new RpgSkillCatalog.Effect(RpgSkillCatalog.EffectType.STUN, 60, 0, 0, 0)); assertTrue(ally.skillActionsBlocked());
        assertTrue(f.engine.cast(1, f.world, new RpgSkillEngine.Cast(4, 2, 0, 170, 300), f.players)); assertFalse(ally.skillActionsBlocked());
        ally.receiveDamage(10000);
        // Recupera energia por relógio real; não injeta um saldo fictício para permitir o revive.
        f.advance(180);
        assertTrue(f.engine.cast(1, f.world, new RpgSkillEngine.Cast(3, 2, 0, 170, 300), f.players));
        assertEquals(40, ally.networkState().hp());
    }

    @Test void reviveRefusesAnOccupiedBodyBeforeSpendingEnergy() {
        var f = new Fixture("light"); var ally = f.ally();
        assertTrue(ally.skillMove(f.enemy.getPosition())); ally.receiveDamage(10000);
        int energy = f.world.networkState().stamina();
        assertFalse(f.engine.cast(1, f.world, new RpgSkillEngine.Cast(3, 2, 0, 185, 300), f.players));
        assertEquals(energy, f.world.networkState().stamina()); assertEquals(0, f.engine.state(1).cooldowns().get(2));
        assertEquals(0, ally.networkState().hp());
    }

    @Test void skillKillUsesTheExistingDeathLedgerAndRespawnClearsControls() {
        var f = new Fixture("water"); f.enemy.hp = 20;
        assertTrue(f.cast(3)); assertTrue(f.cast(1)); long generation = f.enemy.generation;
        f.advance(3); assertEquals(0, f.enemy.hp); assertEquals(0, f.enemy.skillRootUntil);
        assertEquals(1, f.encounter.events().stream().filter(e -> e.kind().equals("DEATH")).count());
        assertEquals(1, f.encounter.events().stream().filter(e -> e.kind().equals("REWARD_REFUSED")).count());
        f.engine.tick(f.players); assertEquals(1, f.encounter.events().stream().filter(e -> e.kind().equals("DEATH")).count());
        f.advance(240); assertTrue(f.enemy.hp > 0); assertEquals(generation + 1, f.enemy.generation);
        assertEquals(0, f.enemy.skillRootUntil); assertEquals(0, f.enemy.skillStunUntil); assertEquals(0, f.enemy.skillSlowUntil);
    }

    @Test void dashMovesOverTimeAndCannotCrossWallsOrMoveWhileRooted() {
        var f = new Fixture("thunder"); Position before = f.world.skillPosition(); assertTrue(f.cast(4)); assertEquals(before, f.world.skillPosition());
        f.advance(1); assertTrue(f.world.skillPosition().x() > before.x()); f.advance(7); assertEquals(970, f.enemy.hp); assertTrue(f.enemy.skillStunUntil > f.tick);
        var blocked = new Fixture("water", List.of(new CollisionService.Obstacle(164, 250, 1, 100)));
        assertTrue(blocked.cast(5)); blocked.advance(8); assertTrue(blocked.world.skillPosition().x()<164); assertEquals(1000,blocked.enemy.hp);
        var rooted = new Fixture("water"); rooted.world.skillApplyControl(new RpgSkillCatalog.Effect(RpgSkillCatalog.EffectType.ROOT, 60, 0, 0, 0)); assertFalse(rooted.cast(5));
    }
    @Test void dashTerminationReportsTheActualPositionAndStillStopsWhenRootedMidway(){
        var f=new Fixture("thunder",List.of(),Map.of("a_target","slime|forest|300|300"));assertTrue(f.cast(4));f.advance(2);
        Position stopped=f.world.skillPosition();f.world.skillApplyControl(new RpgSkillCatalog.Effect(RpgSkillCatalog.EffectType.ROOT,60,0,0,0));f.advance(1);
        assertEquals(stopped,f.world.skillPosition());
        var end=f.engine.events().stream().filter(e->e.skill().equals("thunder_4")&&e.fromX()==e.x()&&e.fromY()==e.y()).findFirst().orElseThrow();
        assertEquals(stopped.x(),end.x());assertEquals(stopped.y(),end.y());assertEquals(1000,f.enemy.hp);
    }
    /** O especial reserva a área sem mover o corpo e só causa dano no encerramento. */
    @Test void lightningFieldIsImmuneUntilFourAreaHitsAndThenRestoresDamage(){
        var f=new Fixture("thunder");Position start=f.world.skillPosition();assertTrue(f.cast(5));
        assertTrue(f.world.skillIntangible());int hp=f.world.networkState().hp();
        f.world.receiveDamage(100);assertEquals(hp,f.world.networkState().hp());
        f.advance(31);assertEquals(start,f.world.skillPosition());assertEquals(1000,f.enemy.hp);
        f.advance(1);assertEquals(944,f.enemy.hp);assertFalse(f.world.skillIntangible());
        f.world.receiveDamage(10);assertEquals(hp-10,f.world.networkState().hp());
        var events=f.engine.events().stream().filter(e->e.skill().equals("thunder_5")).toList();
        assertEquals(1,events.stream().filter(e->e.fromX()!=e.x()||e.fromY()!=e.y()).count());
        assertEquals(1,events.stream().filter(e->e.fromX()==e.x()&&e.fromY()==e.y()).count());
        assertTrue(f.world.skillReady());
    }
    @Test void lightningFieldReappearsBesideAnAllyWhoOccupiedItsOriginAndClearRestoresState(){
        var f=new Fixture("thunder");var ally=f.ally();assertTrue(f.cast(5));
        ally.setSyntheticBodies(List.of());assertTrue(ally.skillMove(f.world.skillPosition()));
        f.advance(32);assertFalse(f.world.skillIntangible());
        assertTrue(Math.abs(f.world.skillPosition().x()-ally.skillPosition().x())>=f.world.physicalRadius()+ally.physicalRadius()-.001
                ||Math.abs(f.world.skillPosition().y()-ally.skillPosition().y())>=f.world.physicalRadius()+ally.physicalRadius()-.001);
        var cancel=new Fixture("thunder");assertTrue(cancel.cast(5));cancel.engine.clear();
        assertFalse(cancel.world.skillIntangible());assertTrue(cancel.world.skillReady());
    }
    @Test void frontalDashNeedsNoTargetAndCutsTwoCrossedEnemiesOnlyOnce(){
        var f=new Fixture("thunder",List.of(),Map.of("a_target","slime|forest|185|300","b_target","slime|forest|275|300"));
        assertTrue(f.engine.cast(1,f.world,new RpgSkillEngine.Cast(4,0,0,0,0),f.players));
        f.advance(8);for(var enemy:f.world.skillEnemies())assertEquals(970,enemy.hp);
        assertTrue(f.world.skillPosition().x()>300);assertFalse(f.world.skillInDash());
    }
    /** Na borda, o início da fase não pode ser confundido com um término pontual. */
    @Test void lightningFieldPublishesDistinctStartEvenFacingOutOfTheMap(){
        var f=new Fixture("thunder");assertTrue(f.world.skillMove(new Position(18,300)));
        f.world.faceSyntheticDirection(-1,0);assertTrue(f.cast(5));
        var start=f.engine.events().get(0);assertNotEquals(start.fromX(),start.x());
        assertEquals(new Position(18,300),f.world.skillPosition());assertTrue(f.world.skillIntangible());
        f.advance(32);assertFalse(f.world.skillIntangible());assertTrue(f.world.skillReady());
    }
    @Test void bodySlamPublishesGripAndTheCollisionResolvedLandingWithoutChangingDamage(){
        var f=new Fixture("martial",List.of(new CollisionService.Obstacle(200,250,30,100)),Map.of("a_target","slime|forest|181|300"));assertTrue(f.cast(4));
        var events=f.engine.events().stream().filter(e->e.skill().equals("martial_4")).toList();assertEquals(2,events.size());
        assertNotEquals(events.get(0).fromX(),events.get(0).x());assertEquals(events.get(1).fromX(),events.get(1).x());
        assertEquals(f.enemy.getPosition().x(),events.get(1).x());assertEquals(966,f.enemy.hp);assertTrue(f.enemy.skillStunUntil>f.tick);
    }

    @Test void diagonalBodySlamUsesTheSameCircularPlayerContactAsEnemyMovement() {
        var f = new Fixture("martial", List.of(), Map.of("a_target", "slime|forest|165|325"));
        Position before = f.enemy.getPosition();
        double initialDistance = Math.hypot(before.x() - 140, before.y() - 300);
        assertTrue(initialDistance > 19 + f.world.physicalRadius());
        assertDoesNotThrow(() -> assertTrue(f.cast(4)));
        assertNotEquals(before, f.enemy.getPosition());
        assertEquals(966, f.enemy.hp); assertEquals(750, f.world.networkState().stamina());
        assertTrue(f.enemy.skillStunUntil > f.tick);
    }

    @Test void bodySlamNearAWallKeepsTheEnemyFootprintAndStopsAtRawGeometry() {
        var obstacle = new CollisionService.Obstacle(200, 250, 30, 100);
        var f = new Fixture("martial", List.of(obstacle), Map.of("a_target", "slime|forest|181|300"));
        var collision = new CollisionService();
        var raw = new CollisionService.Space(960, 640, List.of(obstacle), List.of());
        Position before = f.enemy.getPosition();
        assertTrue(collision.isFree(f.enemy, before.x(), before.y(), 19, raw));
        assertFalse(collision.isFree(f.enemy, before.x(), before.y(), 19, f.world.skillSpace()));
        assertDoesNotThrow(() -> assertTrue(f.cast(4)));
        assertEquals(before, f.enemy.getPosition());
        assertTrue(collision.isFree(f.enemy, before.x(), before.y(), 19, raw));
        assertEquals(966, f.enemy.hp); assertTrue(f.enemy.skillStunUntil > f.tick);
    }

    @Test void repulsionUsesRawGeometryToSlideAlongTheWallInsteadOfOnlyGuardingTheOrigin() {
        var obstacle = new CollisionService.Obstacle(200, 320, 30, 160);
        var f = new Fixture("wind", List.of(obstacle), Map.of("a_target", "slime|forest|181|360"));
        var raw = new CollisionService.Space(960, 640, List.of(obstacle), List.of());
        var collision = new CollisionService();
        assertTrue(collision.isFree(f.enemy, 181, 360, 19, raw));
        assertFalse(collision.isFree(f.enemy, 181, 360, 19, f.world.skillSpace()));
        // Skillshot agora segue um lado cardinal: alinhar o lançador com o corredor vertical.
        assertTrue(f.world.skillMove(new Position(181,300)));f.world.skillFace(f.enemy.getPosition());
        assertDoesNotThrow(() -> assertTrue(f.cast(4)));
        f.repulsionContact();
        Position pushed = f.enemy.getPosition();
        assertEquals(181, pushed.x(), .001);
        assertTrue(pushed.y() > 360, "A parede bloqueia X, mas não o deslocamento paralelo em Y");
        assertTrue(collision.isFree(f.enemy, pushed.x(), pushed.y(), 19, raw));
        assertEquals(984, f.enemy.hp);
        assertDoesNotThrow(() -> assertTrue(f.cast(5)));
        Position pulled = f.enemy.getPosition();
        assertTrue(pulled.y() < pushed.y());
        assertTrue(collision.isFree(f.enemy, pulled.x(), pulled.y(), 19, raw));
        assertEquals(968, f.enemy.hp);
    }

    @Test void overlappingMobsBlockDisplacementWithoutCrashingOrSkippingTheHit() {
        var spawns = Map.of("a_target", "slime|forest|185|300", "b_target", "slime|forest|185|300");
        var martial = new Fixture("martial", List.of(), spawns);
        Position original = martial.enemy.getPosition();
        assertDoesNotThrow(() -> assertTrue(martial.cast(4)));
        assertEquals(original, martial.enemy.getPosition());
        assertEquals(original, martial.world.skillEnemies().get(1).getPosition());
        assertEquals(966, martial.enemy.hp); assertEquals(1000, martial.world.skillEnemies().get(1).hp);
        assertTrue(martial.enemy.skillStunUntil > martial.tick);
        var wind = new Fixture("wind", List.of(), spawns);
        assertDoesNotThrow(() -> assertTrue(wind.cast(4)));
        wind.repulsionContact();
        assertDoesNotThrow(() -> assertTrue(wind.cast(5)));
        assertTrue(wind.world.skillEnemies().stream().allMatch(e -> e.getPosition().equals(original) && e.hp == 968));
    }

    @Test void bodySlamReadsTheOtherPlayersCurrentPositionBeforeMovingTheVictim() {
        var f = new Fixture("martial"); var ally = f.ally();
        // O encontro conhece a nova posição mesmo sem reconstruir proxies privados do lançador.
        assertTrue(ally.skillMove(new Position(220, 300)));
        assertDoesNotThrow(() -> assertTrue(f.cast(4)));
        assertEquals(189, f.enemy.getPosition().x(), .001);
        double separation = Math.hypot(f.enemy.getPosition().x() - ally.skillPosition().x(), f.enemy.getPosition().y() - ally.skillPosition().y());
        assertTrue(separation >= 19 + ally.physicalRadius() - .001);
        assertEquals(966, f.enemy.hp);
    }

    @Test void bodySlamCannotMoveAnEnemyBeyondTheMapBoundary() {
        var f = new Fixture("martial", List.of(), Map.of("a_target", "slime|forest|19|300"));
        assertTrue(f.world.skillMove(new Position(60, 300)));
        assertDoesNotThrow(() -> assertTrue(f.cast(4)));
        assertEquals(19, f.enemy.getPosition().x(), .001);
        assertEquals(300, f.enemy.getPosition().y(), .001);
        assertEquals(966, f.enemy.hp);
    }

    @Test void rejectionLifecycleAndGlobalEventsDoNotLeakResourcesOrResetSequence() {
        var f = new Fixture("water");
        assertEquals(List.of(0, 0, 0, 0, 0), f.engine.state(99).cooldowns());
        int energy = f.world.networkState().stamina();
        assertFalse(f.engine.cast(1, f.world, new RpgSkillEngine.Cast(1, ENEMY, 99, 185, 300), f.players));
        assertFalse(f.engine.cast(1, f.world, new RpgSkillEngine.Cast(1, ENEMY, 1, Float.NaN, 300), f.players));
        assertEquals(energy, f.world.networkState().stamina()); assertTrue(f.engine.events().isEmpty());
        f.engine.clock(500); assertTrue(f.engine.cast(1, f.world, new RpgSkillEngine.Cast(1, ENEMY, 1, 185, 300), f.players));
        var first = f.engine.events().get(0); assertEquals(500, first.tick()); assertEquals(45, f.engine.state(1).cooldowns().get(0));
        f.engine.remove(1); assertTrue(f.engine.events().isEmpty());
        for (int i = 0; i < 10; i++) { f.world.tick(0, 0); f.engine.tick(f.players); }
        assertEquals(1000, f.enemy.hp);
        f.engine.clear(); assertTrue(f.engine.cast(1, f.world, new RpgSkillEngine.Cast(1, ENEMY, 1, 185, 300), f.players));
        assertTrue(f.engine.events().get(0).sequence() > first.sequence());
        assertEquals(45, f.engine.state(1).cooldowns().get(0));
    }

    @Test void directionalSkillshotUsesServerFacingAndHitsWhenTheFrontActuallyArrives() {
        var f = new Fixture("thunder", List.of(), Map.of("a_target", "slime|forest|140|200"));
        f.world.faceSyntheticDirection(0, -1);
        assertTrue(f.engine.cast(1, f.world, new RpgSkillEngine.Cast(2, ENEMY, 1, 420, 300), f.players));
        var launch = f.engine.events().get(0);
        assertEquals(140, launch.x(), .001); assertTrue(launch.y() < 300);
        assertEquals(1000, f.enemy.hp); f.advance(3); assertEquals(1000, f.enemy.hp);
        f.advance(1); assertEquals(976, f.enemy.hp); assertTrue(f.enemy.skillStunUntil > f.tick);
    }

    @Test void skillshotNeverHitsAnEnemyThatEntersThePartItAlreadyTraversed() {
        var f = new Fixture("thunder", List.of(), Map.of("a_target", "slime|forest|230|400"));
        assertTrue(f.engine.cast(1, f.world, new RpgSkillEngine.Cast(2, ENEMY, 1, 420, 300), f.players));
        f.advance(10); assertEquals(1000, f.enemy.hp);
        f.enemy.setPosition(new Position(180, 300)); f.advance(12);
        assertEquals(1000, f.enemy.hp);
    }

    @Test void aThinWallStopsTheTravelingFrontWithoutRefusingTheWholeCast() {
        var obstacle = new CollisionService.Obstacle(230, 250, 1, 100);
        var f = new Fixture("thunder", List.of(obstacle), Map.of("a_target", "slime|forest|210|300", "b_target", "slime|forest|280|300"));
        assertTrue(f.cast(2)); assertEquals(800, f.world.networkState().stamina());
        var launch = f.engine.events().get(0); assertEquals(420, launch.x(), .01);
        f.advance(10); assertEquals(976, f.enemy.hp);
        assertEquals(1000, f.world.skillEnemies().get(1).hp);
        assertTrue(f.engine.events().stream().filter(e -> e.fromX() == e.x() && e.fromY() == e.y()).allMatch(e -> e.x() <= 212.01));
    }

    @Test void skillshotsStopAtTheFirstBodyInsteadOfPiercingSeveralEnemies(){
        for(String kit:List.of("thunder","spirit","aetheri")){
            var f=new Fixture(kit,List.of(),Map.of("a_target","slime|forest|200|300","b_target","slime|forest|280|300"));
            int slot=kit.equals("aetheri")?4:2;assertTrue(f.cast(slot));f.advance(12);
            assertEquals(1000,f.world.skillEnemies().get(1).hp,kit);
            if(kit.equals("spirit"))assertEquals(0,f.world.skillEnemies().get(1).skillRootUntil);
            assertTrue(f.engine.events().stream().anyMatch(e->e.fromX()==e.x()&&e.fromY()==e.y()),kit);
        }
    }
    @Test void aTargetedCutHitsAnInterveningBodyBeforeItsSelectedTarget(){
        var f=new Fixture("water",List.of(),Map.of("a_target","slime|forest|300|300","b_blocker","slime|forest|200|300"));
        assertTrue(f.cast(1));f.advance(12);
        assertEquals(1000,f.enemy.hp);assertEquals(980,f.world.skillEnemies().get(1).hp);
    }
    @Test void wallContactCreatesImpactButAnEmptyRangeDoesNot(){
        var obstacle=new CollisionService.Obstacle(230,250,1,100);
        var blocked=new Fixture("thunder",List.of(obstacle),Map.of("a_target","slime|forest|280|300"));
        assertTrue(blocked.cast(2));blocked.advance(20);assertEquals(1000,blocked.enemy.hp);
        assertTrue(blocked.engine.events().stream().anyMatch(e->e.fromX()==e.x()&&e.fromY()==e.y()&&Math.abs(e.x()-212)<.01));
        var missed=new Fixture("aetheri");missed.world.faceSyntheticDirection(0,-1);
        assertTrue(missed.engine.basicArrow(1,missed.world,missed.players));missed.advance(20);
        assertTrue(missed.engine.events().stream().allMatch(e->e.fromX()!=e.x()||e.fromY()!=e.y()));assertEquals(1000,missed.enemy.hp);
    }
    @Test void windCutsLaunchAtZeroThreeAndSixTicksFromTheCurrentCasterPosition(){
        var f=new Fixture("wind");assertTrue(f.cast(2));f.advance(3);
        assertTrue(f.world.skillMove(new Position(140,320)));f.advance(3);
        var launches=f.engine.events().stream().filter(e->e.skill().equals("wind_2")&&(e.fromX()!=e.x()||e.fromY()!=e.y())).toList();
        assertEquals(List.of(0L,3L,6L),launches.stream().map(RpgSkillEngine.Event::tick).toList());
        assertEquals(320,launches.get(2).fromY(),.01);
    }
    @Test void arrowRainDamagesOnlyOnGroundArrivalAndCompletesAllFiveWaves(){
        var f=new Fixture("aetheri");assertTrue(f.cast(3));f.advance(11);assertEquals(1000,f.enemy.hp);
        f.advance(1);assertEquals(990,f.enemy.hp);f.advance(48);assertEquals(950,f.enemy.hp);
        assertEquals(5,f.engine.events().stream().filter(e->e.skill().equals("aetheri_3")&&e.fromX()==e.x()&&e.fromY()==e.y()).count());
    }

    @Test void targetedProjectileFollowsTheMovingEnemyAndPublishesItsRealContact() {
        var f = new Fixture("water", List.of(), Map.of("a_target", "slime|forest|230|300"));
        assertTrue(f.cast(1)); f.advance(1); assertEquals(1000, f.enemy.hp);
        f.enemy.setPosition(new Position(230, 340)); f.advance(10);
        assertEquals(980, f.enemy.hp);
        assertEquals(1, f.encounter.events().stream().filter(e -> e.kind().equals("HIT")).count());
        assertTrue(f.engine.events().stream().anyMatch(e -> e.fromX() == e.x() && e.fromY() == e.y() && e.y() > 300));
    }

    @Test void coneVolleyLaunchesSevenDifferentTravelingArrowsWithoutSevenfoldDamage() {
        var f = new Fixture("aetheri");
        assertTrue(f.engine.cast(1, f.world, new RpgSkillEngine.Cast(5, 0, 0, 140, 80), f.players));
        assertEquals(1000, f.enemy.hp);
        var launches = f.engine.events().stream().filter(e -> e.fromX() != e.x() || e.fromY() != e.y()).toList();
        assertEquals(RpgSkillEngine.VOLLEY_ARROWS, launches.size());
        assertEquals(7, launches.stream().map(RpgSkillEngine.Event::y).distinct().count());
        assertTrue(launches.stream().allMatch(e -> e.fromX() == 140 && e.fromY() == 300 && e.x() > 140));
        f.advance(8); assertEquals(972, f.enemy.hp);
        assertEquals(1, f.encounter.events().stream().filter(e -> e.kind().equals("HIT")).count());
    }

    @Test void fireFissureTravelsFromThePlayerAndEruptsOnlyAtTheActualEnd() {
        var f = new Fixture("fire", List.of(), Map.of("a_target", "slime|forest|185|300", "b_target", "slime|forest|420|300"));
        var distant = f.world.skillEnemies().get(1);
        assertTrue(f.cast(4)); assertEquals(1000, f.enemy.hp); assertEquals(1000, distant.hp);
        f.advance(1); assertEquals(975, f.enemy.hp); assertEquals(1000, distant.hp);
        f.advance(12); assertEquals(1000, distant.hp);
        f.advance(1); assertEquals(975, distant.hp);
        f.advance(1); assertEquals(975, distant.hp);
        assertFalse(f.engine.events().stream().anyMatch(e -> e.fromX() == e.x() && e.fromY() == e.y()));
        f.advance(1); assertEquals(950, distant.hp);
        assertTrue(f.engine.events().stream().anyMatch(e -> e.fromX() == e.x() && e.fromY() == e.y() && Math.abs(e.x() - 420) < .001));
    }

    @Test void circularLightningAcceptsNearbyBodiesAndHitsThroughActualContact() {
        var f = new Fixture("thunder"); f.refreshBodies();
        assertTrue(f.cast(5)); assertEquals(600, f.world.networkState().stamina());
        f.advance(1); assertEquals(1000,f.enemy.hp); assertTrue(f.world.skillIntangible());
        f.advance(31); assertEquals(944,f.enemy.hp);
        assertTrue(f.engine.events().stream().filter(e -> e.skill().equals("thunder_5")).count() == 2);
        assertTrue(f.world.skillReady());
        assertTrue(Math.hypot(f.world.skillPosition().x() - f.enemy.getPosition().x(), f.world.skillPosition().y() - f.enemy.getPosition().y()) >= f.world.physicalRadius() + 19 - .001);
    }

    @Test void circularLightningKeepsWorkingWhenAWallBlocksItsFirstLeg() {
        var obstacle = new CollisionService.Obstacle(380, 180, 120, 120);
        var f = new Fixture("thunder", List.of(obstacle), Map.of("a_target", "slime|forest|358|305"));
        assertTrue(f.world.skillMove(new Position(358, 260))); f.world.faceSyntheticDirection(1, 0); f.refreshBodies();
        assertDoesNotThrow(() -> assertTrue(f.cast(5)));
        f.advance(1); assertEquals(358, f.world.skillPosition().x(), .001); assertEquals(1000,f.enemy.hp);
        f.advance(31); assertEquals(944,f.enemy.hp); assertTrue(f.world.skillReady());
        assertTrue(new CollisionService().isFree(new com.projetok.domain.entity.GameEntity(-1, "Apoio", f.world.skillPosition()) {}, f.world.skillPosition().x(), f.world.skillPosition().y(), f.world.physicalRadius(), f.world.skillSpace()));
    }

    @Test void circularLightningHitsStationaryEnemiesAtBasicAttackDistanceInAllFourDirectionsDespiteABlockedLeg() {
        for(var target:List.of(new Position(186,300),new Position(94,300),new Position(140,254),new Position(140,346))){
            boolean forward=target.x()>140;
            var obstacle=new CollisionService.Obstacle(forward?115:164,200,1,200);
            var f=new Fixture("thunder",List.of(obstacle),Map.of("a_target","slime|forest|"+target.x()+"|"+target.y()));
            f.world.faceSyntheticDirection(forward?-1:1,0);f.refreshBodies();
            assertTrue(f.cast(5));f.advance(32);
            assertEquals(944,f.enemy.hp,"O rastro deve atingir a distância 46 na direção "+target);
            assertTrue(f.enemy.skillStunUntil>f.tick);
            assertEquals(target,f.enemy.getPosition());
            f.advance(31);assertTrue(f.world.skillReady());
        }
    }

    @Test void circularLightningCannotHitAnEnemyOnTheOtherSideOfTheWall() {
        var obstacle=new CollisionService.Obstacle(164,250,1,100);
        var f=new Fixture("thunder",List.of(obstacle),Map.of("a_target","slime|forest|186|300"));
        f.refreshBodies();assertTrue(f.cast(5));f.advance(32);
        assertEquals(1000,f.enemy.hp);assertEquals(0,f.enemy.skillStunUntil);
        assertTrue(f.encounter.events().stream().noneMatch(e->e.kind().equals("HIT")));
    }

    @Test void fullHealthHealingAndNpcTargetsCannotConsumeResources() {
        var f = new Fixture("light"); int energy = f.world.networkState().stamina();
        assertFalse(f.engine.cast(1, f.world, new RpgSkillEngine.Cast(1, 0, 0, 140, 300), f.players));
        assertTrue(f.world.networkMessage().contains("cheia"));
        assertFalse(f.cast(2)); assertTrue(f.world.networkMessage().contains("feridos"));
        assertFalse(f.engine.cast(1, f.world, new RpgSkillEngine.Cast(5, 2_000_001, 0, 140, 300), f.players));
        assertEquals(energy, f.world.networkState().stamina());
        assertEquals(List.of(0, 0, 0, 0, 0), f.engine.state(1).cooldowns()); assertTrue(f.engine.events().isEmpty());
    }

    @Test void healingAndRevivalEffectsAreEmittedAtTheRecipientsFeet() {
        var f = new Fixture("light"); var ally = f.ally(); ally.receiveDamage(40);
        assertTrue(f.engine.cast(1, f.world, new RpgSkillEngine.Cast(1, 2, 0, 0, 0), f.players));
        var heal = f.engine.events().get(0);
        assertEquals(ally.skillPosition().x(), heal.x()); assertEquals(ally.skillPosition().y(), heal.y());
        assertEquals(heal.fromX(), heal.x()); assertEquals(heal.fromY(), heal.y());
        assertTrue(f.world.networkMessage().contains("+30"));
        ally.receiveDamage(10000);
        assertTrue(f.engine.cast(1,f.world,new RpgSkillEngine.Cast(3,2,0,0,0),f.players));
        var revive=f.engine.events().get(f.engine.events().size()-1);
        assertEquals("light_3",revive.skill());assertEquals(ally.skillPosition().x(),revive.x());assertEquals(ally.skillPosition().y(),revive.y());
        assertEquals(revive.fromX(),revive.x());assertEquals(revive.fromY(),revive.y());assertEquals(40,ally.networkState().hp());
    }

    @Test void groundAreasKeepTheirSelectedPositionAndInvalidPointsHaveNoEffects() {
        var f = new Fixture("spirit"); int energy = f.world.networkState().stamina();
        assertFalse(f.engine.cast(1, f.world, new RpgSkillEngine.Cast(1, 0, 0, 800, 300), f.players));
        assertEquals(energy, f.world.networkState().stamina()); assertTrue(f.engine.events().isEmpty());
        assertTrue(f.engine.cast(1, f.world, new RpgSkillEngine.Cast(1, 0, 0, 160, 500), f.players));
        assertEquals(160, f.engine.events().get(0).x()); assertEquals(500, f.engine.events().get(0).y());
        f.advance(40); assertEquals(1000, f.enemy.hp);
    }
}
