package com.projetok.rpg;

import com.projetok.domain.entity.GameEntity;
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

/** Contrato ALPHA-002: corpo fixo, controles por execução e relógio determinístico sem disco. */
class Alpha002LightningTest {
    private static final Position ORIGIN=new Position(140,300);

    /** Encontro real; somente posições e HP das vítimas são fixtures controladas. */
    private static final class Fixture {
        final RpgSkillEngine engine=new RpgSkillEngine();
        final Map<Integer,RpgWorld> players=new LinkedHashMap<>();
        final RpgWorld world;
        final RpgSceneSet scenes;
        final SharedRpgEncounter encounter;
        long tick;
        Fixture(){this(List.of());}
        Fixture(List<CollisionService.Obstacle> obstacles){
            var space=new CollisionService.Space(960,640,obstacles,List.of());
            var definitions=Map.of("a_inside","slime|forest|185|300","b_outside","slime|forest|360|300",
                    "c_cero","mob_alado|forest|200|350");
            scenes=new RpgSceneSet("forest",Map.of("forest",new RpgSceneSet.Geometry(space,ORIGIN),
                    "village",new RpgSceneSet.Geometry(space,new Position(140,350))),definitions,
                    Map.of(),Map.of(),Map.of("escape","forest|140|300|village|140|350"));
            world=new RpgWorld(CharacterIdentity.create("Raio","reaper","azure"),scenes);
            world.useNetworkFootprint();world.awardExperience(RpgProgression.builtin().threshold(20));
            assertTrue(world.chooseDomain("thunder"));
            encounter=new SharedRpgEncounter(Map.of("forest",space,"village",space),definitions,
                    SharedRpgEncounter.RewardPolicy.SYNTHETIC_PARTICIPANTS);
            encounter.join(1,world);players.put(1,world);
            world.skillEnemies().forEach(e->e.hp=1000);
        }
        EnemyService.Enemy enemy(String spawn){return world.skillEnemies().stream().filter(e->e.spawn.equals(spawn)).findFirst().orElseThrow();}
        void cast(){assertTrue(engine.cast(1,world,new RpgSkillEngine.Cast(5,0,0,0,0),players));}
        void advance(int count){for(int i=0;i<count;i++){
            tick++;players.values().forEach(w->w.tick(0,0));encounter.step(tick);engine.clock(tick);engine.tick(players);
        }}
        RpgWorld ally(){
            var other=new RpgWorld(CharacterIdentity.create("Aliado","human","azure"),scenes);
            other.useNetworkFootprint();assertTrue(other.skillMove(new Position(140,370)));
            encounter.join(2,other);players.put(2,other);return other;
        }
    }

    @Test void fixedBodyKeepsCollisionAndRejectsExtraActionsUntilConfiguredEnd(){
        var f=new Fixture();var ally=f.ally();var origin=f.world.skillPosition();
        var identity=f.world.networkState().identity();int hp=f.world.networkState().hp();
        f.cast();assertEquals(600,f.world.networkState().stamina());
        assertEquals(270,f.engine.state(1).cooldowns().get(4));
        assertFalse(f.world.skillIntangible());assertFalse(f.world.skillInDash());
        assertFalse(f.world.skillMove(new Position(150,300)));
        assertFalse(f.world.attack());assertFalse(f.world.castForwardAbility());
        assertFalse(f.engine.cast(1,f.world,new RpgSkillEngine.Cast(1,1_000_100,1,0,0),f.players));
        for(int i=0;i<31;i++){
            f.world.receiveDamage(10);assertEquals(hp,f.world.networkState().hp());
            f.world.tick(1,1);f.tick++;ally.tick(0,0);f.encounter.step(f.tick);f.engine.clock(f.tick);f.engine.tick(f.players);
            assertEquals(origin,f.world.skillPosition());assertEquals(identity,f.world.networkState().identity());
        }
        ally.setSyntheticBodies(List.of(CollisionService.Body.block(new GameEntity(1,"Raio",origin){},f.world.physicalRadius())));
        ally.skillMove(origin);assertNotEquals(origin,ally.skillPosition());
        var inside=f.enemy("a_inside");
        f.world.skillDisplace(inside,origin);
        assertTrue(Math.hypot(inside.getPosition().x()-origin.x(),inside.getPosition().y()-origin.y())>=f.world.physicalRadius()+19-.001);
        f.advance(1);assertFalse(f.world.skillFixedExecution());assertEquals(origin,f.world.skillPosition());
        assertEquals(238,f.engine.state(1).cooldowns().get(4));
        int after=f.world.networkState().hp();f.world.receiveDamage(1);assertEquals(after-1,f.world.networkState().hp());
    }

    @Test void areaStopsMovementAttacksAndCeroWhileOutsideAiAndWorldKeepAdvancing(){
        var f=new Fixture();var inside=f.enemy("a_inside");var outside=f.enemy("b_outside");var cero=f.enemy("c_cero");
        var position=inside.getPosition();var farPosition=outside.getPosition();var ceroPosition=cero.getPosition();
        f.cast();f.advance(20);
        assertEquals(position,inside.getPosition());assertEquals(0,inside.attackTick);
        assertEquals(ceroPosition,cero.getPosition());assertEquals(0,cero.attackTick);
        assertNotEquals("PREPARE",cero.behavior);assertNotEquals(farPosition,outside.getPosition());
        assertEquals(20,f.world.skillTick());assertTrue(f.world.skillLightningControls(inside));
        f.advance(12);assertFalse(f.world.skillLightningControls(inside));
        assertEquals(0,inside.skillStunUntil,"A área não deixa stun residual");
        f.advance(1);assertTrue(inside.attackTick>0);assertEquals("PREPARE",cero.behavior);
    }

    @Test void enteringLeavingTangentAndIndependentControlsAreEvaluatedWithoutClearingDebuffs(){
        var f=new Fixture();var enemy=f.enemy("b_outside");f.cast();
        assertFalse(f.world.skillLightningControls(enemy));
        int radius=RpgSkillCatalog.get("thunder_5").radius();
        enemy.setPosition(new Position(ORIGIN.x()+radius+19,300));assertTrue(f.world.skillLightningControls(enemy));
        enemy.setPosition(new Position(ORIGIN.x()+radius+19+.1f,300));assertFalse(f.world.skillLightningControls(enemy));
        enemy.setPosition(new Position(185,300));enemy.skillStunUntil=100;enemy.skillRootUntil=200;
        f.advance(1);assertEquals(100,enemy.skillStunUntil);assertEquals(200,enemy.skillRootUntil);
        enemy.setPosition(new Position(500,300));assertFalse(f.world.skillLightningControls(enemy));
        f.engine.remove(1);f.advance(1);
        assertEquals(new Position(500,300),enemy.getPosition());assertEquals(100,enemy.skillStunUntil);
        assertEquals(200,enemy.skillRootUntil);
    }

    @Test void wallUsesTheSameEligibilityForParalysisAndDamage(){
        var f=new Fixture(List.of(new CollisionService.Obstacle(164,250,1,100)));
        f.cast();var enemy=f.enemy("a_inside");assertFalse(f.world.skillLightningControls(enemy));
        enemy.skillRootUntil=100;f.advance(32);assertEquals(1000,enemy.hp);
    }

    @Test void fourImpactsAndRewardLedgerCannotRepeatWhenTickIsRepeated(){
        var f=new Fixture();var enemy=f.enemy("a_inside");enemy.hp=56;
        long xp=f.world.progressView().xp();f.cast();f.advance(32);
        assertEquals(0,enemy.hp);
        var events=List.copyOf(f.encounter.events());var inventory=f.world.inventory().snapshot();
        long after=f.world.progressView().xp();assertTrue(after>xp);
        assertEquals(4,events.stream().filter(e->e.kind().equals("HIT")&&e.enemy()==enemy.getId()).count());
        f.engine.tick(f.players);f.engine.tick(f.players);
        assertEquals(events,f.encounter.events());assertEquals(after,f.world.progressView().xp());
        assertEquals(inventory,f.world.inventory().snapshot());
        assertEquals(2,f.engine.events().stream().filter(e->e.skill().equals("thunder_5")).count());
    }

    @TestFactory Stream<DynamicTest> lifecycleInvalidatesAreaAndNeverTransfersImmunity(){
        return Stream.of("remove","clear","leave","replacement","region","death").map(action->DynamicTest.dynamicTest(action,()->{
            var f=new Fixture();f.cast();f.advance(5);var enemy=f.enemy("a_inside");
            switch(action){
                case "remove"->f.engine.remove(1);
                case "clear"->f.engine.clear();
                case "leave"->{f.encounter.leave(f.world);f.players.remove(1);f.engine.tick(f.players);}
                case "replacement"->{var next=new Fixture();f.players.put(1,next.world);f.engine.tick(f.players);assertFalse(next.world.skillFixedExecution());}
                case "region"->{assertTrue(f.world.travel("escape"));f.engine.tick(f.players);}
                case "death"->{var field=RpgWorld.class.getDeclaredField("hp");field.setAccessible(true);field.setInt(f.world,0);f.engine.tick(f.players);}
                default->fail(action);
            }
            assertFalse(f.world.skillFixedExecution());assertFalse(f.world.skillLightningControls(enemy));
            assertFalse(f.world.skillIntangible());assertEquals(1000,enemy.hp);
            assertTrue(f.engine.events().isEmpty());
        }));
    }

    @Test void oldCancellationAndLatePendingCannotAffectReplacementExecution(){
        var f=new Fixture();f.cast();
        Object next=new Object();f.world.skillBeginLightning(next,100,e->false);
        f.advance(32);
        assertTrue(f.world.skillFixedExecution());assertEquals(1000,f.enemy("a_inside").hp);
        f.world.skillEndLightning(new Object());assertTrue(f.world.skillFixedExecution());
        f.world.skillEndLightning(next);assertFalse(f.world.skillFixedExecution());
    }

    @Test void overlappingExecutionsRemoveOnlyTheirOwnAreaContribution(){
        var f=new Fixture();var other=new RpgWorld(CharacterIdentity.create("Segundo","reaper","azure"),f.scenes);
        other.useNetworkFootprint();other.skillMove(new Position(140,370));
        other.awardExperience(RpgProgression.builtin().threshold(20));assertTrue(other.chooseDomain("thunder"));
        f.encounter.join(2,other);f.players.put(2,other);f.cast();
        assertTrue(f.engine.cast(2,other,new RpgSkillEngine.Cast(5,0,0,0,0),f.players));
        var enemy=f.enemy("a_inside");var position=enemy.getPosition();
        f.engine.remove(1);assertFalse(f.world.skillLightningControls(enemy));
        assertTrue(other.skillLightningControls(enemy));f.advance(20);assertEquals(position,enemy.getPosition());
        f.advance(12);assertFalse(other.skillLightningControls(enemy));assertEquals(944,enemy.hp);
    }
}
