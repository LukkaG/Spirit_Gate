package com.projetok.rpg;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ArchetypesTest {
    @Test void guardianMovesMysticHealsAndCostsSurviveSave() {
        var g=new RpgWorld(CharacterIdentity.create("Guardiao","guardian","amber"));float x=g.snapshot().x();assertTrue(g.castAbility());assertTrue(g.snapshot().x()>x);assertFalse(g.castAbility());
        var m=new RpgWorld(CharacterIdentity.create("Mistico","mystic","azure"));assertFalse(m.castAbility());m.receiveDamage(60);assertTrue(m.castAbility());assertEquals(75,m.save().hp());
        assertFalse(RpgWorld.restore(m.save()).castAbility());assertEquals(m.save(),RpgWorld.restore(m.save()).save());
    }
}
