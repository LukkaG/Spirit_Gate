package com.projetok.domain.ai;

import com.projetok.domain.entity.GameEntity;

import java.util.Optional;

/**
 * Porta mínima entre IA de domínio e o estado do mundo (que vive na infraestrutura, junto
 * do GameLoop). Propositalmente enxuta — expanda conforme a IA evoluir, mas nunca deixe uma
 * implementação concreta (Kryonet, Postgres etc.) vazar para dentro do domínio.
 */
public interface GameWorldView {
    Optional<GameEntity> findNearestHostile(GameEntity origin, double range);
}
