package com.projetok.rpg;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RpgProgressionTest {
    @Test void multipleLevelsNoFinalCapAndRestoration() {
        var w=new RpgWorld();w.receiveDamage(20);int oldHp=w.save().hp();
        w.awardExperience(360);assertEquals(4,w.level());assertEquals(oldHp+36,w.save().hp());
        assertEquals(w.attackPower(),RpgWorld.restore(w.save()).attackPower());
        w.awardExperience(1_000_000);assertTrue(w.level()>20);
        var before=w.save();assertThrows(IllegalArgumentException.class,()->w.awardExperience(-1));assertEquals(before,w.save());
    }
    @Test void rewardNeverReducesPreviouslyAcceptedExperience() {
        var legacyCapped=new RpgProgression(20,60,12,3);
        assertEquals(50000,legacyCapped.award(50000,3));
        assertEquals(50003,RpgProgression.builtin().award(50000,3));
        assertEquals(1000000,RpgProgression.builtin().award(999999,3));
    }
}
