package com.projetok.domain.race;

import com.projetok.domain.ai.GameWorldView;
import com.projetok.domain.entity.GameEntity;

/**
 * Ponto de extensão para comportamento único de raça (ex.: "espírito regenera vida na
 * sombra") sem precisar de subclasse de GameEntity/Player/Mob. Raças sem comportamento
 * especial simplesmente não registram nenhum trait (List.of()).
 */
public interface RacialTrait {

    default void onLevelUp(GameEntity entity) {
        // no-op por padrão
    }

    default void onTick(GameEntity entity, GameWorldView world) {
        // no-op por padrão
    }
}
