package com.projetok.domain.entity;

public record Position(float x, float y) {

    public Position translate(float dx, float dy) {
        return new Position(x + dx, y + dy);
    }
}
