package com.projetok.rpg;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ArchetypesTest {
    @Test void racialCreationHasNoDomainOrFreeShunpo() {
        for(var race:CharacterRaces.all()) {
            var world=new RpgWorld(CharacterIdentity.create("Aprendiz",race.id(),"amber"));
            assertEquals(race.id(),world.save().identity().race());
            assertEquals(DomainProgression.NONE,world.domainProgression().domain());
            assertEquals(0,world.domainProgression().xpHalfUnits());
            assertFalse(world.castAbility());assertFalse(world.castSecondaryAbility());
            assertEquals(race.id().equals("reaper"),world.save().equipment().containsKey("weapon"));
            assertEquals(world.save(),RpgWorld.restore(world.save()).save());
        }
        assertThrows(IllegalArgumentException.class,()->CharacterIdentity.create("Antigo","guardian","azure"));
        assertThrows(IllegalArgumentException.class,()->CharacterIdentity.create("Antigo","mystic","azure"));
    }
}
