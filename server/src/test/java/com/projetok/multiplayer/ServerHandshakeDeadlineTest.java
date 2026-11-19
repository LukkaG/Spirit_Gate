package com.projetok.multiplayer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

/** Regressao do prazo absoluto de negociacao, usando somente TCP loopback e estado em memoria. */
@Timeout(12)
class ServerHandshakeDeadlineTest {
    /** Fixture sem conteudo, saves ou servicos; a simulacao continua proprietaria dos atores. */
    private static final class World implements SessionSimulation {
        private final Map<Integer, Protocol.Actor> actors = new TreeMap<>();
        private long tick;

        /** Admite um nome ficticio sem carregar personagens ou dados externos. */
        public boolean join(int id, String name) {
            actors.put(id, new Protocol.Actor(id, name, 140, 300, 100, 100, 100, 0, false));
            return true;
        }

        /** Usa a mesma segunda mensagem exigida pelas aplicacoes 5/6/7 reais. */
        public boolean supportsCharacterCreation() { return true; }
        public String contentHash() { return "a".repeat(64); }
        public Protocol.CombatState combatSnapshot() {
            return new Protocol.CombatState(new Protocol.SessionState(snapshot(), java.util.List.of()),
                    java.util.List.of());
        }
        public boolean join(int id, SessionCharacterSelection selection, int application) {
            return join(id, selection.name());
        }
        public void leave(int id) { actors.remove(id); }
        public boolean accept(int id, Protocol.Intent intent) { return actors.containsKey(id); }
        public void step() { tick++; }
        public Protocol.State snapshot() { return new Protocol.State(tick, new ArrayList<>(actors.values())); }
    }

    /** Parcelas frequentes nao devem renovar indefinidamente a reserva de uma vaga. */
    @Test void incompleteHelloReleasesSlotEvenWithContinuousPartialInput() throws Exception {
        try (var server = new LocalSimulationServer(InetAddress.getLoopbackAddress(), 0, new World())) {
            server.start();
            try (var partial = new Socket(InetAddress.getLoopbackAddress(), server.port())) {
                var out = new DataOutputStream(partial.getOutputStream());
                out.writeInt(64);
                out.flush();
                assertDeadline(server, partial, out, 3300);
                try (var normal = new Socket(InetAddress.getLoopbackAddress(), server.port())) {
                    normal.setSoTimeout(2000);
                    Protocol.write(normal.getOutputStream(), new Protocol.Hello(1, "Valido"));
                    assertInstanceOf(Protocol.Welcome.class, Protocol.read(normal.getInputStream()));
                }
                assertNull(server.failure());
            }
        }
    }

    /** O prazo e compartilhado por Hello e CharacterCreate, sem alterar o formato da aplicacao 5. */
    @Test void incompleteCharacterChoiceSharesTheHelloDeadline() throws Exception {
        try (var server = new LocalSimulationServer(InetAddress.getLoopbackAddress(), 0, new World())) {
            server.start();
            try (var partial = new Socket(InetAddress.getLoopbackAddress(), server.port())) {
                var out = new DataOutputStream(partial.getOutputStream());
                Protocol.write(out, new Protocol.Hello(Protocol.CHARACTER_APPLICATION, "Criacao"));
                Thread.sleep(1000); // Consome parte do prazo antes de iniciar a segunda mensagem.
                out.writeInt(64);
                out.flush();
                assertDeadline(server, partial, out, 1500);
                assertNull(server.failure());
            }
        }
    }

    /** Uma escolha valida continua admitida pelo mesmo contrato existente. */
    @Test void completeCharacterChoiceIsAdmittedNormally() throws Exception {
        try (var server = new LocalSimulationServer(InetAddress.getLoopbackAddress(), 0, new World())) {
            server.start();
            try (var socket = new Socket(InetAddress.getLoopbackAddress(), server.port())) {
                socket.setSoTimeout(2000);
                Protocol.write(socket.getOutputStream(), new Protocol.Hello(5, "Completo"));
                Protocol.write(socket.getOutputStream(), new Protocol.CharacterCreate("human", "none", "none"));
                assertInstanceOf(Protocol.Welcome.class, Protocol.read(socket.getInputStream()));
                assertNull(server.failure());
            }
        }
    }

    /** Fragmentacao valida dentro do prazo continua aceita pelo transporte real. */
    @Test void fragmentedValidHelloWithinDeadlineIsAccepted() throws Exception {
        try (var server = new LocalSimulationServer(InetAddress.getLoopbackAddress(), 0, new World())) {
            server.start();
            try (var socket = new Socket(InetAddress.getLoopbackAddress(), server.port())) {
                socket.setSoTimeout(2000);
                var bytes = new java.io.ByteArrayOutputStream();
                Protocol.write(bytes, new Protocol.Hello(1, "Fragmentado"));
                for (byte part : bytes.toByteArray()) {
                    socket.getOutputStream().write(part);
                    socket.getOutputStream().flush();
                    Thread.sleep(2);
                }
                assertInstanceOf(Protocol.Welcome.class, Protocol.read(socket.getInputStream()));
                assertNull(server.failure());
            }
        }
    }

    /** Mantem trafego parcial abaixo de SO_TIMEOUT e exige fechamento dentro de uma margem limitada. */
    private static void assertDeadline(LocalSimulationServer server, Socket socket, DataOutputStream out,
                                       long maximumMillis)
            throws Exception {
        long acceptedBy = System.nanoTime() + Duration.ofSeconds(2).toNanos();
        while (server.connections() == 0 && System.nanoTime() < acceptedBy) Thread.sleep(10);
        assertEquals(1, server.connections(), "O socket deve estar reservado antes de medir sua liberacao");
        var active = new AtomicBoolean(true);
        var sender = new Thread(() -> {
            try {
                while (active.get()) {
                    out.writeByte(1);
                    out.flush();
                    Thread.sleep(150);
                }
            } catch (IOException expectedOnClose) {
                // O teste espera a interrupcao do socket ao expirar a negociacao.
            } catch (InterruptedException stopped) {
                Thread.currentThread().interrupt();
            }
        }, "handshake-test-fragments");
        sender.setDaemon(true);
        sender.start();
        long deadline = System.nanoTime() + Duration.ofMillis(maximumMillis).toNanos();
        boolean closed = false;
        try {
            socket.setSoTimeout(3500);
            while (System.nanoTime() < deadline) {
                if (server.connections() == 0) { closed = true; break; }
                Thread.sleep(10);
            }
            assertTrue(closed, "A negociacao incompleta reteve a vaga alem do prazo total");
        } finally {
            active.set(false);
            socket.close();
            sender.interrupt();
            sender.join(1000);
        }
    }
}

