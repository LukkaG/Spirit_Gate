package com.projetok.model;

public class DefenseCalculator {

    public Human human;

    public static int calculateDefese(Entity attacker, Entity target) {
        int targetDef = target.currentDefense();

        boolean isAttackerPlayer = attacker instanceof Playable;

        if (isAttackerPlayer) {

        }

        return (int) (targetDef);
    }

    public void calculatePlayerDefense(Playable attacker, Playable target) {

        int baseDefense = target.currentDefense();

        if (target instanceof Human) {

        }

    }
}
