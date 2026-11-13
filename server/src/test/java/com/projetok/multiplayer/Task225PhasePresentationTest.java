package com.projetok.multiplayer;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Expõe o limite visual do A-04 sem transformar a apresentação em autoridade física. */
class Task225PhasePresentationTest {
    private static final String HASH = "a".repeat(64);
    private static final SkillProtocol.Event START = new SkillProtocol.Event(1, 0, 1, "thunder_5", 140, 300, 141, 300);

    /** Estado reduzido com os mesmos campos da aplicação 7 existente. */
    private static SkillProtocol.State state(String hash, long tick, SkillProtocol.Event... events) {
        return new SkillProtocol.State(hash, tick, List.of(events), List.of(0, 0, 0, 0, 0), 0, false);
    }

    /** Confirma a expiração em cinco segundos mesmo sem término recebido; retransmissão não reinicia. */
    @Test void lostTerminalEventUsesExistingVisualFuseAndRepeatedSnapshotDoesNotRehide() {
        var clock = new AtomicLong();
        var view = new NetworkSkillPresentation(clock::get);
        view.update(state(HASH, 0, START));
        assertTrue(view.hidden(1));
        clock.set(4999);
        view.update(state(HASH, 149, START));
        assertTrue(view.hidden(1));
        clock.set(5000);
        assertFalse(view.hidden(1));
        view.update(state(HASH, 180, START));
        assertFalse(view.hidden(1), "Snapshot repetido não representa uma nova fase");
        var terminal = new SkillProtocol.Event(2, 181, 1, "thunder_5", 140, 300, 140, 300);
        view.update(state(HASH, 181, START, terminal));
        assertFalse(view.hidden(1));
    }

    /** Revisão, região, desconexão e ausência/morte do ator descartam a fase antiga imediatamente. */
    @Test void lifecycleChangesDiscardVisualPhaseWithoutWaitingForFiveSeconds() {
        for (String action : List.of("revisão", "região", "desconexão", "ausência", "morte")) {
            var view = new NetworkSkillPresentation(() -> 0L);
            view.update(state(HASH, 0, START));
            assertTrue(view.hidden(1), action);
            switch (action) {
                case "revisão" -> view.update(state("b".repeat(64), 0));
                case "região" -> view.region("village");
                case "desconexão" -> view.update(null);
                case "ausência" -> view.actors(new Protocol.State(1, List.of()));
                case "morte" -> view.actors(new Protocol.State(1,
                        List.of(new Protocol.Actor(1, "Fase", 140, 300, 0, 100, 1000, 0, false))));
                default -> fail(action);
            }
            assertFalse(view.hidden(1), action);
            assertEquals(0, view.activeCount(), action);
        }
    }
}
