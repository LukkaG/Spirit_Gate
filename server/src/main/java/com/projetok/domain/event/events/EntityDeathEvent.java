package com.projetok.domain.event.events;

import com.projetok.domain.entity.GameEntity;

public record EntityDeathEvent(GameEntity entity) {
}
