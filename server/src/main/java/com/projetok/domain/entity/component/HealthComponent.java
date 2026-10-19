package com.projetok.domain.entity.component;

/**
 * HP é inteiramente derivado (raça + nível + itens, via VitalityService) — nunca "gasto"
 * manualmente. Não existe (e não deve existir) um addHealth() aqui, ao contrário do que
 * Entity.java fazia originalmente.
 */
public final class HealthComponent implements Component {

    private int maxHealth;
    private int currentHealth;

    public HealthComponent(int initialMax) {
        if (initialMax < 1) {
            throw new IllegalArgumentException("HP máximo inicial deve ser >= 1");
        }
        this.maxHealth = initialMax;
        this.currentHealth = initialMax;
    }

    /**
     * Único ponto de mudança do teto de HP (chamado só por VitalityService). Preserva o
     * dano já sofrido: se o máximo sobe 20, o atual sobe 20 junto; nunca cura/machuca de graça.
     */
    public void recalculateMax(int newMax) {
        if (newMax < 1) {
            throw new IllegalArgumentException("HP máximo deve ser >= 1");
        }
        int delta = newMax - this.maxHealth;
        this.maxHealth = newMax;
        this.currentHealth = Math.max(0, Math.min(this.maxHealth, this.currentHealth + delta));
    }

    public void applyDamage(int amount) {
        if (amount < 0) throw new IllegalArgumentException("Dano não pode ser negativo");
        this.currentHealth = Math.max(0, this.currentHealth - amount);
    }

    public void heal(int amount) {
        if (amount < 0) throw new IllegalArgumentException("Cura não pode ser negativa");
        this.currentHealth = Math.min(this.maxHealth, this.currentHealth + amount);
    }

    public boolean isDead() {
        return currentHealth <= 0;
    }

    public int current() {
        return currentHealth;
    }

    public int max() {
        return maxHealth;
    }
}
