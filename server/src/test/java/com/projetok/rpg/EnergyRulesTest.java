package com.projetok.rpg;

import com.projetok.domain.combat.EnergyRules;
import com.projetok.domain.entity.Position;
import com.projetok.domain.entity.component.StatsComponent;
import com.projetok.domain.movement.*;
import com.projetok.domain.race.AttributeGrowth;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EnergyRulesTest {
    private RpgSceneSet scenes(){return new RpgSceneSet("forest",Map.of("forest",new RpgSceneSet.Geometry(new CollisionService.Space(960,640,List.of(),List.of()),new Position(140,300))),Map.of(),Map.of(),Map.of(),Map.of());}
    private RpgWorld world(int energy){var world=new RpgWorld(CharacterIdentity.create("Energy Reaper","reaper","azure"),scenes());world.useNetworkFootprint();world.awardExperience(RpgProgression.builtin().threshold(50));for(int i=0;i<energy;i++)assertTrue(world.allocateAttribute("energy"));return world;}
    @Test void energySharesThePointPoolAndSurvivesAllOtherStatOperations(){
        var old=new StatsComponent(1,2,3,1,2);assertEquals(0,old.energy());var trained=old.spendOnEnergy();assertEquals(1,trained.energy());assertEquals(1,trained.unspentPoints());assertEquals(1,trained.spendOnStrength().energy());assertEquals(1,trained.spendOnIntelligence().energy());assertEquals(1,trained.spendOnAgility().energy());assertEquals(1,trained.applyLevelUp(5,AttributeGrowth.NONE).energy());assertThrows(IllegalStateException.class,()->trained.spendOnEnergy().spendOnEnergy());
        assertEquals(1000,EnergyRules.maximum(0));assertEquals(3000,EnergyRules.maximum(100));assertEquals(30,EnergyRules.regenerationPerSecond(100));assertEquals(30,EnergyRules.regenerationPerSecond(10000));assertThrows(IllegalArgumentException.class,()->EnergyRules.maximum(-1));
    }
    @Test void investmentRaisesCapacityWithoutGivingFreeEnergyAndKeepsSkillCost(){
        var world=world(1);assertEquals(1020,world.maximumEnergy());assertEquals(1000,world.networkState().stamina());int hp=world.networkState().hp();assertTrue(world.castForwardAbility());assertEquals(750,world.networkState().stamina());assertEquals(90,world.abilityView().shunpoTicks());for(int i=0;i<20;i++)world.tick(0,0);assertEquals(851,world.networkState().stamina());assertEquals(hp,world.networkState().hp());assertEquals(15.15,world.energyRegenerationPerSecond(),.00001);
        var base=world(0);assertTrue(base.castForwardAbility());for(int i=0;i<20;i++)base.tick(0,0);assertEquals(850,base.networkState().stamina());
    }
    @Test void regenerationIsCappedDoesNotBankAtFullAndStopsWhenDead(){
        var world=world(101);assertEquals(3020,world.maximumEnergy());assertTrue(world.castForwardAbility());for(int i=0;i<30;i++)world.tick(0,0);assertEquals(1050,world.networkState().stamina());for(int i=0;i<1000;i++)world.tick(0,0);assertEquals(3020,world.networkState().stamina());assertTrue(world.castForwardAbility());assertEquals(2770,world.networkState().stamina());world.tick(0,0);assertEquals(2780,world.networkState().stamina());world.receiveDamage(world.maxHp());for(int i=0;i<100;i++)world.tick(0,0);assertEquals(2780,world.networkState().stamina());assertTrue(world.returnHome());assertEquals(3020,world.networkState().stamina());
    }
    @Test void revisionForkKeepsCapacityPointsAndFractionalRegenerationWithoutChangingLegacySave(){
        var world=world(1);assertTrue(world.castForwardAbility());world.tick(0,0);var copy=world.forkSyntheticRevision(scenes(),SessionCollisionProfile.DEFAULT);assertEquals(world.maximumEnergy(),copy.maximumEnergy());assertEquals(world.manualAttributes(),copy.manualAttributes());for(int i=0;i<19;i++){world.tick(0,0);copy.tick(0,0);}assertEquals(851,copy.networkState().stamina());assertEquals(world.networkState().stamina(),copy.networkState().stamina());for(int i=0;i<100;i++)copy.tick(0,0);assertEquals(1020,copy.networkState().stamina());assertEquals(1000,copy.normalizedEnergy());String stamina=copy.save().flags().stream().filter(f->f.startsWith("stamina:")).findFirst().orElseThrow();assertEquals("stamina:1000",stamina);
    }
}
