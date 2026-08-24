package com.projetok.domain.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Desacopla morte/level-up/etc. de quem reage a isso (loot, XP, log, quests). */
public final class GameEventBus {

    private final Map<Class<?>, List<Consumer<Object>>> listeners = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, type -> new CopyOnWriteArrayList<>())
                 .add((Consumer<Object>) listener);
    }

    public void publish(Object event) {
        listeners.getOrDefault(event.getClass(), List.of()).forEach(listener -> listener.accept(event));
    }
}
