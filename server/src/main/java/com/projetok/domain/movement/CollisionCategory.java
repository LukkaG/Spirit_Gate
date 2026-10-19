package com.projetok.domain.movement;

/** Classifica a participação de cada corpo nas políticas de colisão. */
public enum CollisionCategory {
    /** Jogadores, mobs e NPCs com corpo sólido. */
    ACTOR,
    /** Obstáculos atravessáveis apenas pelas habilidades que permitem essa passagem. */
    SOFT_OBSTACLE,
    /** Paredes, construções e obstáculos sólidos tradicionais. */
    SOLID_OBSTACLE,
    /** Bordas do cenário, sempre respeitadas pelas políticas disponíveis. */
    WORLD_BOUNDARY
}
