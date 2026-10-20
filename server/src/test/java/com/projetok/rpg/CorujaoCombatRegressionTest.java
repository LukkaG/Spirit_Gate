package com.projetok.rpg;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class CorujaoCombatRegressionTest {
    private RpgWorld setup(String archetype,boolean boss) {
        var s=new RpgWorld(CharacterIdentity.create("Avaliacao",archetype,"azure")).save();var flags=new TreeSet<String>();
        for(String f:s.flags()){if(boss&&f.startsWith("mob:ruins_guard_")){var v=f.split(":");v[2]="0";f=String.join(":",v);}flags.add(f);}
        return RpgWorld.restore(new RpgSave(s.identity(),boss?"ruins":"forest",boss?650:240,boss?320:360,s.hp(),s.xp(),s.coins(),s.inventory(),s.equipment(),s.quests(),flags,s.ticks()));
    }
    @Test void deterministicEncountersMeasureBothArchetypesAndBossWithoutVisualSideEffects() throws Exception {
        for(String archetype:new String[]{"guardian","mystic"})for(boolean boss:new boolean[]{false,true}) {
            var world=setup(archetype,boss);var control=RpgWorld.restore(world.save());
            var feedback=new com.projetok.prototype.rpg.RpgCombatFeedback();int casts=0,elapsed=0;
            String target=boss?"sanctuary":"forest_a";
            while(world.snapshot().enemies().stream().filter(e->e.spawn().equals(target)).findFirst().orElseThrow().hp()>0&&elapsed<600) {
                boolean used=world.castSecondaryAbility();assertEquals(used,control.castSecondaryAbility());
                if(used)casts++;else assertEquals(world.attack(),control.attack());
                world.tick(0,0);control.tick(0,0);elapsed++;
                feedback.update(world.snapshot());var image=new java.awt.image.BufferedImage(960,640,2);var g=image.createGraphics();feedback.draw(g);g.dispose();
                assertEquals(control.save(),world.save());assertTrue(world.save().hp()>0);
            }
            assertTrue(elapsed<600);assertTrue(world.save().xp()>0);
            int received=world.snapshot().events().stream().filter(e->e.kind()==RpgCombatEvent.Kind.HIT&&e.target()==1).mapToInt(RpgCombatEvent::amount).sum();
            int dealt=world.snapshot().events().stream().filter(e->e.kind()==RpgCombatEvent.Kind.HIT&&e.target()!=1).mapToInt(RpgCombatEvent::amount).sum();
            int spent=casts*RpgSecondaryAbility.forArchetype(archetype).cost();
            System.out.println("CORUJAO_METRIC {\"archetype\":\""+archetype+"\",\"encounter\":\""+(boss?"boss":"slime")+"\",\"ticks\":"+elapsed+",\"damageReceived\":"+received+",\"damageDealt\":"+dealt+",\"staminaSpent\":"+spent+",\"secondaryUses\":"+casts+",\"survived\":true}");
            var saved=world.save();assertEquals(saved,RpgWorld.restore(LocalSaveStore.decode(LocalSaveStore.encode(saved))).save());
        }
    }
    @Test void healingDefeatRecoveryAndResourcesRemainCompatible() throws Exception {
        for(String archetype:new String[]{"guardian","mystic"}) {
            var w=setup(archetype,false);w.receiveDamage(30);assertTrue(w.use("potion"));assertFalse(w.use("potion"));
            w.receiveDamage(10000);assertFalse(w.castSecondaryAbility());assertFalse(w.castAbility());assertFalse(w.attack());
            assertTrue(w.returnHome());assertEquals(w.maxHp(),w.save().hp());assertEquals("village",w.save().region());
            assertEquals(w.save(),RpgWorld.restore(LocalSaveStore.decode(LocalSaveStore.encode(w.save()))).save());
        }
    }
}
