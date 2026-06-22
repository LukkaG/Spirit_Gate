package com.projetok.model;

import com.projetok.model.Human;

public class WaterStyle implements SwordStyle {

    private final int staminaCost = 20;
    private final int baseDamage = 50;
    private final String styleName = "Arte da Água";

    @Override
    public void executeAttack(Human player) {
        damagetaken();
    }

    private void damagetaken() {
        throw new UnsupportedOperationException("Unimplemented method 'damagetaken'");
    }

    @Override
    public String getStyleName() {
        return this.styleName;
    }

    @Override
    public int getStaminaCost() {
        return this.staminaCost;
    }

    @Override
    public int getBaseDamage() {
        return this.baseDamage;
    }

}
