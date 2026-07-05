package com.projetok.model;

public class DefenseCalculator {

    public static int calculateDefese(Entity attacker, Entity target) {
        int targetDef = target.currentDefense();

        boolean isAttackerPlayer = attacker instanceof Playable;

    }
}
