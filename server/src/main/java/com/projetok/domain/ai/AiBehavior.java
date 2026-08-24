package com.projetok.domain.ai;

import com.projetok.domain.entity.GameEntity;

public interface AiBehavior {
    void tick(GameEntity self, GameWorldView world);
}
