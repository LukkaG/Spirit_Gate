package com.projetok.domain.progression;

/** Sucessora de PlayerConfiguration.java — mesma regra, nome mais claro sobre o que faz. */
public final class LevelingRules {

    public int pointsForLevel(int level) {
        if (level < 10) {
            return 5;
        } else if (level < 20) {
            return 3;
        } else {
            return 1;
        }
    }
}
