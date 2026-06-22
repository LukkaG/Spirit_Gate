package com.projetok.model;

public interface SwordStyle {

    void executeAttack(Human player);

    String getStyleName();

    int getStaminaCost();

    int getBaseDamage();
}
