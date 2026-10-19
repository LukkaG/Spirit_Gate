package com.projetok.rpg;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BossEncounterTest {
    static RpgWorld ruins() {var s=new RpgWorld().save();var flags=new java.util.TreeSet<String>();for(String flag:s.flags()){if(flag.startsWith("mob:ruins_guard_")){var v=flag.split(":");v[2]="0";flag=String.join(":",v);}flags.add(flag);}return RpgWorld.restore(new RpgSave(s.identity(),"ruins",650,320,s.hp(),s.xp(),s.coins(),s.inventory(),s.equipment(),s.quests(),flags,s.ticks()));}
    private EnemyService.View boss(RpgWorld w){return w.snapshot().enemies().stream().filter(EnemyService.View::boss).findFirst().orElseThrow();}
    @Test void telegraphsThenExecutesAndNeverAttacksAfterDeath() {
        var w=ruins();w.tick(0,0);assertEquals("PREPARE",boss(w).state());int hp=w.save().hp();
        for(int i=0;i<29;i++)w.tick(0,0);assertEquals(hp,w.save().hp());w.tick(0,0);assertTrue(w.save().hp()<hp);
        w.awardExperience(3600);for(int i=0;i<12&&boss(w).hp()>0;i++){w.attack();for(int t=0;t<12;t++)w.tick(0,0);}
        assertEquals(0,boss(w).hp());assertTrue(w.save().flags().contains("boss-defeated"));
        long xp=w.save().xp();hp=w.save().hp();for(int t=0;t<180;t++)w.tick(0,0);assertEquals(hp,w.save().hp());assertEquals(xp,w.save().xp());
    }
}
