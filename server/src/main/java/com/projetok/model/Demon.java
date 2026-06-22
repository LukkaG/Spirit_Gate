package com.projetok.model;

public class Demon extends Entity {

    public Demon(int id, String name, int health, int strength, int inteligency, int agility, int level) {
        super(id, name, health, strength, inteligency, agility, level);
    }

    @Override
    protected int calculateMaxStamina() {
        return this.getInteligency() * 10;
    }

    @Override
    protected int calculateMaxHealth() {
        return this.getHealth() * 20;
    }

}
