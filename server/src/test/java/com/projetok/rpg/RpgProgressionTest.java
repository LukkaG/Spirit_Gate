package com.projetok.rpg;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RpgProgressionTest {
    @Test void multipleLevelsCapAndRestoration() {
        var w=new RpgWorld();w.receiveDamage(20);int oldHp=w.save().hp();
        w.awardExperience(360);assertEquals(4,w.level());assertEquals(oldHp+36,w.save().hp());
        assertEquals(w.attackPower(),RpgWorld.restore(w.save()).attackPower());
        w.awardExperience(1_000_000);assertEquals(20,w.level());
        var before=w.save();assertThrows(IllegalArgumentException.class,()->w.awardExperience(-1));assertEquals(before,w.save());
    }
}
