package com.projetok.domain.entity.component;

import com.projetok.domain.ai.AiBehavior;
import com.projetok.domain.ai.GameWorldView;
import com.projetok.domain.entity.GameEntity;

public final class AiComponent implements Component {

    private final AiBehavior behavior;

    public AiComponent(AiBehavior behavior) {
        this.behavior = behavior;
    }

    public void tick(GameEntity self, GameWorldView world) {
        behavior.tick(self, world);
    }

    public AiBehavior behavior() {
        return behavior;
    }
}
