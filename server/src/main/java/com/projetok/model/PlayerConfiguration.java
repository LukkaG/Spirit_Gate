package com.projetok.model;

public class PlayerConfiguration {

    public int getPointsForLevel(int level) {
        if (level < 10) {
            return 5;
        } else if (level < 20) {
            return 3;
        } else {
            return 1;
        }
    }
}
