package com.projetok.model;

public class Player implements Playable {

    private String username;
    private Entity race; // Aqui está o segredo: o Player tem uma raça (Humano, Oni, etc.)

    public Player(String username, Entity race) {
        this.username = username;
        this.race = race;
    }

    // Agora, métodos como 'getDefense' seriam delegados à raça:
    public int getDefense() {
        return race.currentDefense();
    }
}
