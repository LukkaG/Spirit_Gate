package com.projetok.prototype;

import com.projetok.domain.command.SimulationCommandQueue;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/** Regressões determinísticas das transições entre dois ticks. */
class DebugCommandBufferTest {
    private static final DebugInput E = new DebugInput(false, false, false, false, false, true);

    /** Pressão e soltura completas no mesmo lote não são perdidas. */
    @Test
    void preservaToqueRapidoDeEsforco() {
        var commands = new DebugCommandBuffer();
        commands.submit(E);
        commands.submit(DebugInput.NONE);
        var frame = commands.drain();
        assertFalse(frame.input().effort());
        assertEquals(1, frame.effortPresses());
        var world = new DebugWorld();
        world.tickQueued(frame.input(), 1f / 30f, frame.effortPresses());
        assertEquals(80, world.snapshot().stamina());
        var next = commands.drain();
        world.tickQueued(next.input(), 1f / 30f, next.effortPresses());
        assertEquals(80.5f, world.snapshot().stamina());
    }

    /** Cada nova pressão gasta uma vez; repetir evento de tecla mantida não gasta. */
    @Test
    void preservaMultiplasPressoesSemRepetirTeclaMantida() {
        var commands = new DebugCommandBuffer();
        for (int i = 0; i < 5; i++) {
            commands.submit(E); commands.submit(E); commands.submit(DebugInput.NONE);
        }
        var frame = commands.drain();
        assertEquals(5, frame.effortPresses());
        var world = new DebugWorld();
        world.tickQueued(frame.input(), 1f / 30f, frame.effortPresses());
        assertEquals(0, world.snapshot().stamina());
        assertEquals(0, commands.drain().effortPresses());
    }

    /** Movimento e ataque rápidos duram um tick, sem prolongar uma soltura normal. */
    @Test
    void toqueDeMovimentoEAtaqueESolturaNormal() {
        var commands = new DebugCommandBuffer();
        var rightAttack = new DebugInput(false, false, false, true, true);
        commands.submit(rightAttack); commands.submit(DebugInput.NONE);
        var frame = commands.drain();
        assertTrue(frame.input().right()); assertTrue(frame.input().attack());
        var world = new DebugWorld();
        world.tickQueued(frame.input(), 1f / 30f, 0);
        assertNotNull(world.snapshot().attack());
        assertTrue(world.snapshot().actors().get(0).x() > 135);
        assertEquals(DebugInput.NONE, commands.drain().input());
        commands.submit(rightAttack); commands.drain();
        commands.submit(DebugInput.NONE);
        assertEquals(DebugInput.NONE, commands.drain().input());
    }

    /** Reinício descarta ações anteriores e preserva apenas novas transições posteriores. */
    @Test
    void reinicioOrdenadoComEntradas() {
        var commands = new DebugCommandBuffer();
        commands.submit(E); commands.restart();
        var reset = commands.drain();
        assertTrue(reset.restart()); assertEquals(0, reset.effortPresses());
        assertTrue(reset.input().effort());
        commands.submit(E);
        assertEquals(0, commands.drain().effortPresses());
        commands.submit(DebugInput.NONE); commands.submit(E);
        assertEquals(1, commands.drain().effortPresses());
        commands.restart(); commands.submit(DebugInput.NONE); commands.submit(E);
        var afterReset = commands.drain();
        assertTrue(afterReset.restart()); assertEquals(1, afterReset.effortPresses());
    }

    /** Fila genérica preserva FIFO e drena atomicamente sem repetir comandos. */
    @Test
    void filaFifoEValidacao() {
        var queue = new SimulationCommandQueue<Integer>();
        queue.submit(1); queue.submit(2); queue.submit(3);
        assertEquals(List.of(1, 2, 3), queue.drain());
        assertTrue(queue.drain().isEmpty());
        assertThrows(NullPointerException.class, () -> queue.submit(null));
    }

    /** Produtores concorrentes não perdem comandos e mantêm sua ordem individual. */
    @Test
    void filaComProdutoresConcorrentes() throws Exception {
        var queue = new SimulationCommandQueue<String>();
        var executor = Executors.newFixedThreadPool(2);
        try {
            for (int producer = 0; producer < 2; producer++) {
                int id = producer;
                executor.submit(() -> { for (int i = 0; i < 100; i++) queue.submit(id + ":" + i); });
            }
        } finally { executor.shutdown(); }
        assertTrue(executor.awaitTermination(3, TimeUnit.SECONDS));
        List<String> values = queue.drain();
        assertEquals(200, values.size());
        for (int producer = 0; producer < 2; producer++) {
            List<String> actual = new ArrayList<>(), expected = new ArrayList<>();
            for (String value : values) if (value.startsWith(producer + ":")) actual.add(value);
            for (int i = 0; i < 100; i++) expected.add(producer + ":" + i);
            assertEquals(expected, actual);
        }
    }
}
