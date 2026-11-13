package com.projetok.multiplayer;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Verifica a limpeza visual de voos cancelados pela saída ou morte do lançador. */
class Task225ProjectilePresentationTest {
    private static final String HASH = "a".repeat(64);

    /** A ausência do lançador cancela a simulação; o observador deve descartar o voo já recebido. */
    @ParameterizedTest
    @ValueSource(strings = {"water_1", "wind_2", "aetheri_4", "aetheri_5"})
    void casterRemovalClearsTheFlightWithoutReplayingTheRepeatedEvent(String skill) {
        var time = new AtomicLong();
        var view = new NetworkSkillPresentation(time::get);
        var launch = new SkillProtocol.Event(1, 1, 1, skill, 140, 300, 340, 300);
        var snapshot = new SkillProtocol.State(HASH, 1, List.of(launch), List.of(0, 0, 0, 0, 0), 0, false);
        view.update(snapshot);
        view.actors(actors(1, 100));
        assertEquals(1, view.activeCount());
        time.set(50);
        view.actors(new Protocol.State(2, List.of()));
        assertEquals(0, view.activeCount(), "O voo não deve permanecer após a saída confirmada do lançador");
        view.update(snapshot);
        assertEquals(0, view.activeCount(), "Snapshot repetido não ressuscita o projétil cancelado");
    }

    /** A morte observada também invalida os efeitos pendentes desse lançador no executor. */
    @ParameterizedTest
    @ValueSource(strings = {"water_1", "aetheri_4"})
    void casterDeathClearsTheFlightWhileOtherCastersRemain(String skill) {
        var view = new NetworkSkillPresentation(() -> 0L);
        view.update(new SkillProtocol.State(HASH, 1, List.of(
                new SkillProtocol.Event(1, 1, 1, skill, 140, 300, 340, 300),
                new SkillProtocol.Event(2, 1, 2, skill, 140, 350, 340, 350)), List.of(0, 0, 0, 0, 0), 0, false));
        view.actors(actors(1, 100));
        assertEquals(2, view.activeCount());
        view.actors(actors(2, 0));
        assertEquals(1, view.activeCount(), "A limpeza deve conservar o voo do outro lançador vivo");
        view.update(null);
        assertEquals(0, view.activeCount());
    }

    /** Frames de atores e skills chegam separadamente; ausência antiga não confirma cancelamento. */
    @Test void anOlderActorSnapshotDoesNotCancelANewerLaunch() {
        var view = new NetworkSkillPresentation(() -> 0L);
        var launch = new SkillProtocol.Event(1, 10, 1, "water_1", 140, 300, 340, 300);
        view.update(new SkillProtocol.State(HASH, 10, List.of(launch), List.of(0, 0, 0, 0, 0), 0, false));
        view.actors(new Protocol.State(9, List.of()));
        assertEquals(1, view.activeCount());
        view.actors(actors(10, 100));
        assertEquals(1, view.activeCount());
        view.actors(new Protocol.State(11, List.of()));
        assertEquals(0, view.activeCount());
    }

    /** Dois lançadores distinguem a limpeza individual de um descarte global indevido. */
    private Protocol.State actors(long tick, int hp) {
        return new Protocol.State(tick, List.of(
                new Protocol.Actor(1, "Lançador", 140, 300, hp, 100, 1000, 0, false),
                new Protocol.Actor(2, "Observador", 140, 350, 100, 100, 1000, 0, false)));
    }
}
