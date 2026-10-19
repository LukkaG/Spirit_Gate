package com.projetok.domain.movement;

import java.util.Objects;

/** Seleciona categorias bloqueantes sem modificar a geometria da física. */
public enum CollisionPolicy {
    /** Movimento tradicional, bloqueado por todas as categorias. */
    SOLID,
    /** Atravessa atores e obstáculos leves; conserva paredes e limites do mundo. */
    PHASE_THROUGH_ACTORS_AND_SOFT;

    /** Informa se a categoria participa da detecção e da resolução deste movimento. */
    public boolean blocks(CollisionCategory category) {
        Objects.requireNonNull(category, "category");
        return this == SOLID || category == CollisionCategory.SOLID_OBSTACLE
                || category == CollisionCategory.WORLD_BOUNDARY;
    }
}
