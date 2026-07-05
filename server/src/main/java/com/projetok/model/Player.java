package com.projetok.model;

public abstract class Player implements Playable {

    private String username;
    private Entity race;

    public Player(String username) {
        this.username = username;
    }

    public int getDefense() {
        return race.currentDefense();
    }
}
