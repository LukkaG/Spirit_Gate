package com.projetok.domain.mob;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Verifica as fronteiras do contrato usado pelo catálogo e pelo rascunho SQL. */
class MobDefinitionTest {
    /** Cada campo é testado independentemente para não esconder uma validação ausente. */
    @Test void numericBoundariesMatchCatalogContract() {
        int[] minimum = {1, 1, 1, 40, 40, 0, 1};
        int[] maximum = {10000, 100, 200, 800, 900, 9000, 1};
        assertDoesNotThrow(() -> definition("a", minimum));
        assertDoesNotThrow(() -> definition("a".repeat(40), maximum));
        for (int index = 0; index < minimum.length; index++) {
            int[] below = minimum.clone(); below[index]--;
            int[] above = maximum.clone(); above[index]++;
            assertThrows(IllegalArgumentException.class, () -> definition("mob", below));
            assertThrows(IllegalArgumentException.class, () -> definition("mob", above));
        }
        assertThrows(IllegalArgumentException.class,
                () -> definition("mob", new int[]{40, 6, 65, 230, 229, 240, 1}));
    }

    /** IDs são estáveis, limitados e não aceitam espaço, quebra de linha ou nulidade. */
    @Test void identifierBoundaries() {
        int[] values = {40, 6, 65, 230, 360, 240, 1};
        for (String id : new String[]{null, "", "1mob", "Mob", "mob x", "mob\n", "a".repeat(41)})
            assertThrows(IllegalArgumentException.class, () -> definition(id, values));
        assertDoesNotThrow(() -> definition("mob_1-a", values));
    }

    /** Constrói cada caso sem alterar os limites de produção. */
    private static MobDefinition definition(String id, int[] n) {
        return new MobDefinition(id, n[0], n[1], n[2], n[3], n[4], n[5], n[6]);
    }
}
