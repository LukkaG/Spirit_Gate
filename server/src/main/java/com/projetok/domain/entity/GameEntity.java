package com.projetok.domain.entity;

import com.projetok.domain.entity.component.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Sucessora de Entity.java. Não guarda mais stats/HP direto em campos — é um container de
 * componentes. "O que este ser é capaz de fazer" vira dado anexado (Component), não posição
 * na árvore de herança. Player, Npc e Mob decidem "quem controla o ser", nunca "o que ele tem".
 */
public abstract class GameEntity {

    private final int id;
    private final String name;
    private Position position;
    private final Map<Class<? extends Component>, Component> components = new HashMap<>();

    protected GameEntity(int id, String name, Position position) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name");
        this.position = Objects.requireNonNull(position, "position");
    }

    public final <T extends Component> void attach(T component) {
        Objects.requireNonNull(component, "component");
        components.put(component.getClass(), component);
    }

    public final <T extends Component> Optional<T> get(Class<T> type) {
        return Optional.ofNullable(type.cast(components.get(type)));
    }

    /** Lança IllegalStateException se o componente não existir — falha ruidosa, nunca um null silencioso. */
    public final <T extends Component> T require(Class<T> type) {
        return get(type).orElseThrow(() -> new IllegalStateException(
            name + " (id=" + id + ") não possui componente " + type.getSimpleName()));
    }

    public final int getId() {
        return id;
    }

    public final String getName() {
        return name;
    }

    public final Position getPosition() {
        return position;
    }

    public final void setPosition(Position position) {
        this.position = Objects.requireNonNull(position, "position");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof GameEntity other)) return false;
        return this.id == other.id && this.getClass() == other.getClass();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, getClass());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + ", name='" + name + "'}";
    }
}
