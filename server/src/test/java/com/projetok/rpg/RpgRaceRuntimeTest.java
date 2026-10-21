package com.projetok.rpg;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RpgRaceRuntimeTest {
    private static RpgWorld world(String race) {
        return new RpgWorld(CharacterIdentity.create("Aprendiz",race,"azure"));
    }
    @Test void beginnersHaveRacialBasicsAndNoDomainOrShunpo() {
        for(var race:CharacterRaces.all()) {
            var w=world(race.id());
            assertFalse(w.domainProgression().chosen());
            assertFalse(w.castAbility());assertFalse(w.castSecondaryAbility());
            assertEquals(1000,resource(w,"stamina"));
            assertFalse(w.chooseDomain(Domains.forRace(race.id()).get(0).id()));
            assertEquals(race.id().equals("reaper"),w.save().equipment().containsKey("weapon"));
            assertEquals(race.id().equals("spirit")?180:race.id().equals("reaper")?82:60,w.basicAttackRange());
        }
    }
    @Test void choiceAndFractionalMasteryPersistWithoutAllowingSwitch() throws Exception {
        for(var race:CharacterRaces.all()) {
            var w=world(race.id());w.awardExperience(RpgProgression.builtin().threshold(15));
            assertEquals(0,w.domainProgression().xpHalfUnits());
            var options=Domains.forRace(race.id());
            assertTrue(w.chooseDomain(options.get(0).id()));
            assertFalse(w.chooseDomain(options.get(1).id()));
            w.awardExperience(3);
            assertEquals(1.5,w.domainProgression().xp());
            var loaded=RpgWorld.restore(LocalSaveStore.decode(LocalSaveStore.encode(w.save())));
            assertEquals(w.save(),loaded.save());
            assertEquals(w.domainProgression(),loaded.domainProgression());
        }
    }
    @Test void oneLevelOneMobAwardsThreeAndOneAndAHalfMasteryOnce() {
        var w=world("reaper");w.awardExperience(RpgProgression.builtin().threshold(15));
        assertTrue(w.chooseDomain("water"));var s=w.save();
        w=RpgWorld.restore(new RpgSave(s.identity(),"forest",240,360,s.hp(),s.xp(),s.coins(),s.inventory(),s.equipment(),s.quests(),s.flags(),s.ticks()));
        assertTrue(w.attack());assertEquals(s.xp()+3,w.save().xp());
        assertEquals(1.5,w.domainProgression().xp());
        assertFalse(w.attack());assertEquals(s.xp()+3,w.save().xp());
    }
    @Test void shunpoLevelTwentyIsSharedAndDefensiveWindowSurvivesSave() {
        for(var race:CharacterRaces.all()) {
            var w=world(race.id());w.awardExperience(RpgProgression.builtin().threshold(20));
            assertTrue(w.canShunpo());assertTrue(w.castSecondaryAbility());
            int hp=w.save().hp();w.receiveDamage(10);assertEquals(hp,w.save().hp());
            w=RpgWorld.restore(w.save());w.receiveDamage(10);assertEquals(hp,w.save().hp());
            assertFalse(w.castAbility());assertFalse(w.castSecondaryAbility());
            for(int i=0;i<6;i++)w.tick(0,0);
            w.receiveDamage(10);assertEquals(hp-10,w.save().hp());
        }
    }
    @Test void hiddenNearestEnemyDoesNotBlockAVisibleEnemyInRange() {
        var s=world("spirit").save();
        var w=RpgWorld.restore(new RpgSave(s.identity(),"forest",360,180,s.hp(),s.xp(),s.coins(),s.inventory(),s.equipment(),s.quests(),s.flags(),s.ticks()));
        var enemies=w.snapshot().enemies();String hidden=enemies.get(0).spawn(),visible=enemies.get(1).spawn();
        var flags=new java.util.TreeSet<String>();
        for(String flag:w.save().flags()) {
            String[] p=flag.split(":");
            if(p[0].equals("mob")&&p[1].equals(hidden)){p[3]="520";p[4]="200";flag=String.join(":",p);}
            else if(p[0].equals("mob")&&p[1].equals(visible)){p[3]="360";p[4]="350";flag=String.join(":",p);}
            else if(p[0].equals("mob")&&p[1].equals(enemies.get(2).spawn())){p[2]="0";flag=String.join(":",p);}
            flags.add(flag);
        }
        s=w.save();w=RpgWorld.restore(new RpgSave(s.identity(),s.region(),s.x(),s.y(),s.hp(),s.xp(),s.coins(),s.inventory(),s.equipment(),s.quests(),flags,s.ticks()));
        int hiddenHp=w.snapshot().enemies().get(0).hp(),visibleHp=w.snapshot().enemies().get(1).hp();
        assertTrue(w.attack());
        assertEquals(hiddenHp,w.snapshot().enemies().get(0).hp());
        assertEquals(visibleHp-w.attackPower(),w.snapshot().enemies().get(1).hp());
    }
    private static int resource(RpgWorld w,String prefix) {
        return w.save().flags().stream().filter(f->f.startsWith(prefix+":"))
            .mapToInt(f->Integer.parseInt(f.substring(prefix.length()+1))).findFirst().orElseThrow();
    }
}
