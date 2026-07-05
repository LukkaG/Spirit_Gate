package com.projetok.model;

public class Human extends Entity {

    private Sword sword;
    private SwordStyle style;
    private int damage;

    public Human(int id, String name, int health, int strength, int inteligency, int agility, int level) {
        super(id, name, health, strength, inteligency, agility, level);
        this.style = style;
        this.sword = sword;
    }

    public void attack(Entity Target) {
        if (style != null) {
            style.executeAttack(this);
            int totalDamage = sword.getDamage() + damage;
        }
    }

    @Override
    protected int calculateMaxStamina() {
        return this.getInteligency() * 15;
    }

    @Override
    protected int calculateMaxHealth() {
        return this.getHealth() * 10;
    }

    @Override
    public int getWeaponDamage() {
        if (this.sword != null) {
            return this.sword.getDamage();
        }
        return 0;
    }

    public SwordStyle getStyle() {
        return style;
    }

    public void setStyle(SwordStyle style) {
        this.style = style;
    }

    public Sword getSword() {
        return sword;
    }

    public void setSword(Sword sword) {
        this.sword = sword;
    }

    public double getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }
}
