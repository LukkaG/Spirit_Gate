package com.projetok.model;

public abstract class Entity {

    private int strength;
    private int inteligency;
    private int agility;
    private int health;
    private int id;
    private String name;
    private int level;
    private int points;
    private int maxHealth;
    private int maxStamina;
    private int currentHealth;
    private int currentStamina;

    public Entity(int id, String name, int health, int strength, int inteligency, int agility, int level) {
        this.id = id;
        this.name = name;
        this.health = health;
        this.strength = strength;
        this.inteligency = inteligency;
        this.agility = agility;
        this.level = level;
        updateStats();
    }

    public void levelUp(PlayerConfiguration config) {
        this.level++;
        int pontosGanhos = config.getPointsForLevel(this.level);
        this.points += pontosGanhos;

        updateStats();

        System.out.println("Parabéns! Nível " + this.level + " alcançado. Você ganhou " + pontosGanhos + " pontos.");
    }

    public void addStrength() {
        if (this.points > 0) {
            this.strength++;
            this.points--;
            updateStats();
        }
    }

    public void addInteligency() {
        if (this.points > 0) {
            this.inteligency++;
            this.points--;
            updateStats();
        }
    }

    public void addAgility() {
        if (this.points > 0) {
            this.agility++;
            this.points--;
            updateStats();
        }
    }

    public void addHealth() {
        if (this.points > 0) {
            this.health++;
            this.points--;
            updateStats();
        }
    }

    public final void updateStats() {
        int oldMaxHealth = this.maxHealth;

        this.maxHealth = calculateMaxHealth();
        this.maxStamina = calculateMaxStamina();

        this.currentHealth += (this.maxHealth - oldMaxHealth);
    }

    protected abstract int calculateMaxHealth();

    protected abstract int calculateMaxStamina();

    public void takeDamage() {

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }

    public int getInteligency() {
        return inteligency;
    }

    public void setInteligency(int inteligency) {
        this.inteligency = inteligency;
    }

    public int getAgility() {
        return agility;
    }

    public void setAgility(int agility) {
        this.agility = agility;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
    }

    public int getMaxStamina() {
        return maxStamina;
    }

    public void setMaxStamina(int maxStamina) {
        this.maxStamina = maxStamina;
    }

    public int getCurrentHealth() {
        return currentHealth;
    }

    public void setCurrentHealth(int currentHealth) {
        this.currentHealth = currentHealth;
    }

    public int getCurrentStamina() {
        return currentStamina;
    }

    public void setCurrentStamina(int currentStamina) {
        this.currentStamina = currentStamina;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }
}
