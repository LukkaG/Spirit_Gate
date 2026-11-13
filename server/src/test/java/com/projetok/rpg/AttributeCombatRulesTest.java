package com.projetok.rpg;

import com.projetok.domain.combat.AttributeCombatRules;
import com.projetok.domain.combat.AttributeCombatRules.DamageKind;
import com.projetok.domain.combat.style.*;
import com.projetok.domain.entity.Position;
import com.projetok.domain.entity.component.StatsComponent;
import com.projetok.domain.movement.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AttributeCombatRulesTest {
    private RpgWorld world(int agility){var space=new CollisionService.Space(960,640,List.of(),List.of());var scenes=new RpgSceneSet("forest",Map.of("forest",new RpgSceneSet.Geometry(space,new Position(140,300))),Map.of("test_enemy","slime|forest|220|300"),Map.of(),Map.of(),Map.of());var w=new RpgWorld(CharacterIdentity.create("Atributos Reaper","reaper","azure"),scenes);w.useNetworkFootprint();w.awardExperience(RpgProgression.builtin().threshold(49));for(int i=0;i<agility;i++)assertTrue(w.allocateAttribute("agility"));return w;}
    @Test void strengthAndIntelligenceScaleOnlyTheirDamageKinds(){
        assertEquals(2,AttributeCombatRules.damageBonus(DamageKind.BASIC,4,10));assertEquals(2,AttributeCombatRules.damageBonus(DamageKind.UNARMED_SKILL,4,10));assertEquals(5,AttributeCombatRules.damageBonus(DamageKind.DOMAIN_SKILL,4,10));assertEquals(0,AttributeCombatRules.damageBonus(DamageKind.OTHER,4,10));
        var stats=new StatsComponent(4,10,100,1,0);assertEquals(2,new UnarmedStrike().computeDamage(stats));assertEquals(55,new WaterStyle().computeDamage(stats));assertEquals(55,new WaterStyle().computeDamage(new StatsComponent(100,10,0,1,0)));
        var w=world(0);int basic=w.attackPower();assertTrue(w.allocateAttribute("intelligence"));assertTrue(w.allocateAttribute("intelligence"));assertEquals(basic,w.attackPower());assertEquals(1,w.skillAttributeBonus(DamageKind.DOMAIN_SKILL));assertEquals(0,w.skillAttributeBonus(DamageKind.UNARMED_SKILL));
    }
    @Test void fractionalCadenceHonorsSmallBonusesCapsAndNoIdleCredit(){
        assertEquals(12,AttributeCombatRules.basicInterval(12,0));assertEquals(6,AttributeCombatRules.basicInterval(12,100));assertEquals(6,AttributeCombatRules.basicInterval(12,10000));assertEquals(.25,AttributeCombatRules.basicDodgeChance(10000));assertEquals(.005,AttributeCombatRules.basicDodgeChance(1));
        double ready=Double.NaN;long now=0;for(int i=0;i<1000;i++){ready=AttributeCombatRules.nextBasicReady(ready,now,12,1);now=(long)Math.ceil(ready);}assertEquals((long)Math.ceil(12000/1.01),now);assertEquals(112,Math.ceil(AttributeCombatRules.nextBasicReady(1.5,100,12,1)));
        assertThrows(IllegalArgumentException.class,()->AttributeCombatRules.basicSpeedMultiplier(-1));
    }
    @Test void actualBasicAttackUsesFasterCooldownWithoutAddingAgilityDamage(){
        var w=world(100);int attack=w.attackPower();assertTrue(w.attack());assertEquals(6,w.attackRemaining());assertFalse(w.attack());for(int i=0;i<5;i++)w.tick(0,0);assertEquals(1,w.attackRemaining());w.tick(0,0);assertEquals(0,w.attackRemaining());assertEquals(attack,w.attackPower());assertEquals(1000,w.networkState().stamina());assertEquals(0,w.abilityView().shunpoTicks());
    }
    @Test void dodgeRollAppliesOnlyToBasicHitsAndNeverToSkills(){
        var w=world(100);int hp=w.networkState().hp();var rolls=new int[]{0};w.evasionRandom(new Random(){@Override public double nextDouble(){rolls[0]++;return 0;}});
        assertTrue(w.receiveBasicDamage(10));assertEquals(hp,w.networkState().hp());assertEquals(1,rolls[0]);w.receiveDamage(10);assertEquals(hp-10,w.networkState().hp());assertEquals(1,rolls[0]);assertFalse(w.receiveBasicDamage(0));assertEquals(1,rolls[0]);
        w.evasionRandom(new Random(){@Override public double nextDouble(){return .25;}});assertFalse(w.receiveBasicDamage(10));assertEquals(hp-20,w.networkState().hp());
        var zero=world(0);zero.evasionRandom(new Random(){@Override public double nextDouble(){throw new AssertionError("AGI zero não deve sortear");}});int previous=zero.networkState().hp();assertFalse(zero.receiveBasicDamage(10));assertEquals(previous-10,zero.networkState().hp());
    }
    @Test void enemySourcesDistinguishBasicAndAreaDamage(){
        var space=new CollisionService.Space(960,640,List.of(),List.of());var basic=new EnemyService(RpgContent.builtin(),space,Map.of("test_enemy","slime|forest|220|300"),r->space);var hits=new ArrayList<Boolean>();
        basic.updateSelected("forest",1,new Position(185,300),true,new MovementService(new CollisionService()),space,(amount,isBasic)->hits.add(isBasic),p->true,true,e->true);assertEquals(List.of(true),hits);
        var boss=new EnemyService(RpgContent.builtin(),space,Map.of("test_boss","warden|ruins|220|300"),r->space);hits.clear();boss.updateSelected("ruins",1,new Position(140,300),true,new MovementService(new CollisionService()),space,(amount,isBasic)->hits.add(isBasic),p->true,true,e->true);boss.updateSelected("ruins",31,new Position(140,300),true,new MovementService(new CollisionService()),space,(amount,isBasic)->hits.add(isBasic),p->true,true,e->true);assertEquals(List.of(false),hits);
    }
    @Test void sharedEncounterUsesTheDefendersServerSideDodgeRoll(){
        var w=world(100);var space=new CollisionService.Space(960,640,List.of(),List.of());var encounter=new SharedRpgEncounter(space,SharedRpgEncounter.RewardPolicy.DISABLED);encounter.join(1,w);int hp=w.networkState().hp();int[] rolls={0};w.evasionRandom(new Random(){@Override public double nextDouble(){rolls[0]++;return 0;}});
        for(int tick=1;tick<=120;tick++){w.tick(0,0);encounter.step(tick);}assertTrue(rolls[0]>0);assertEquals(hp,w.networkState().hp());assertTrue(w.networkMessage().contains("Esquiva por agilidade"));w.receiveDamage(10);assertEquals(hp-10,w.networkState().hp());
    }
}
