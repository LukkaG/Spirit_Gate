package com.projetok.domain.ai;

import com.projetok.domain.entity.GameEntity;

/** Para NPCs/mobs que nunca iniciam combate sozinhos (ex.: vendedores, animais neutros). */
public final class PassiveBehavior implements AiBehavior {

    @Override
    public void tick(GameEntity self, GameWorldView world) {
        // Intencionalmente vazio.
    }
}
