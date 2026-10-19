package com.projetok.prototype;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MovementReplayTest {
    @Test
    void reproduzEstadoCompletoAnteriorAExtracao() {
        // Captured before extraction; compares every tick of the complete visual state.
        long[] expected = {-5020331805826811769L, 720261320915429601L, -6194199811086341680L};
        for (int scenario = 0; scenario < 3; scenario++) {
            // Esta referência conserva a resposta antiga; a nova mecânica tem regressões próprias.
            DebugWorld world = new DebugWorld(java.util.random.RandomGenerator.getDefault(),
                    com.projetok.domain.movement.MovementService.Response.STRICT);
            long fingerprint = 0;
            for (int i = 0; i < 600; i++) {
                DebugInput input;
                if (scenario == 0) input = new DebugInput(i % 160 < 80, i % 160 >= 80,
                        i % 240 >= 120, i % 240 < 120, i % 3 == 0);
                else if (scenario == 1) input = DebugInput.NONE;
                else {
                    var s = world.snapshot();
                    var p = s.actors().get(0);
                    var m = s.actors().get(1);
                    float dx = m.x() - p.x(), dy = m.y() - p.y();
                    input = new DebugInput(dy < -4, dy > 4, dx < -4, dx > 4, true);
                }
                world.tick(input, 1f / 30f);
                // Projeção validada contra toString antes de acrescentar vigor ao snapshot.
                fingerprint = fingerprint * 31 + legacyState(world.snapshot()).hashCode();
            }
            assertEquals(expected[scenario], fingerprint, "Scenario " + scenario);
        }
    }

    /** Projeta o contrato anterior à TASK-003, mantendo os baselines existentes. */
    private static String legacyState(DebugSnapshot s) {
        return "DebugSnapshot[actors=" + s.actors() + ", obstacles=" + s.obstacles()
                + ", effects=" + s.effects() + ", playerHp=" + s.playerHp()
                + ", playerMaxHp=" + s.playerMaxHp() + ", defeated=" + s.defeated()
                + ", gameOver=" + s.gameOver() + ", ticks=" + s.ticks()
                + ", lastEvent=" + s.lastEvent() + ", facingX=" + s.facingX()
                + ", facingY=" + s.facingY() + ", cooldown=" + s.cooldown()
                + ", attack=" + s.attack() + "]";
    }
}
