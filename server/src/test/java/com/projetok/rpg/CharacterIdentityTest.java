package com.projetok.rpg;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CharacterIdentityTest {
    @Test void validatesNamesAndRestoresStableIdentity() {
        for (String name : new String[]{"", "ab", "<script>", "A\nB", "a".repeat(25)})
            assertThrows(IllegalArgumentException.class, () -> CharacterIdentity.create(name,"reaper","azure"));
        var original = CharacterIdentity.create("  Joao do Vale  ","reaper","azure");
        var restored = new CharacterIdentity(original.id(),original.name(),original.archetype(),original.appearance(),original.initialRegion(),original.baseHp(),original.baseAttack());
        assertEquals(original,restored);
        assertEquals("Joao do Vale",new RpgWorld(restored).snapshot().identity().name());
    }
}
