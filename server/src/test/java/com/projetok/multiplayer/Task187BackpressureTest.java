package com.projetok.multiplayer;

import com.projetok.studio.*;
import java.io.*;
import java.lang.reflect.Field;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Pressão TCP local limitada, sem alterar buffers/configurações globais do sistema. */
class Task187BackpressureTest {
    static Object field(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    static LiveRevision padded() throws Exception {
        var maps = new TreeMap<>(WorldContent.baseline().bundle().maps());
        var metadata = new TreeMap<String,String>();
        for (int i = 0; i < 120; i++) metadata.put("lab_padding_" + i, "x".repeat(4096));
        maps.put("forest", maps.get("forest").withMetadata(metadata));
        return LiveRevision.prepare(0, "forest", maps, WorldRevisionTest.EMPTY);
    }

    @Test void stalledReaderKeepsBoundedQueuesAndOtherClientRecovers() throws Exception {
        var revision = padded();
        int descriptorBytes = WorldContent.encode(revision).length;
        var world = new RpgSessionWorld(List.of("reaper", "human"), null, true);
        world.enableRevisions(revision);
        String previous = System.getProperty(NetworkDiagnostics.PROPERTY);
        LocalSimulationServer created;
        System.setProperty(NetworkDiagnostics.PROPERTY, "true");
        try { created = new LocalSimulationServer(InetAddress.getLoopbackAddress(), 0, world); }
        finally {
            if (previous == null) System.clearProperty(NetworkDiagnostics.PROPERTY);
            else System.setProperty(NetworkDiagnostics.PROPERTY, previous);
        }
        try (var server = created; var stalled = new Socket()) {
            server.start();
            stalled.setReceiveBufferSize(1024);
            stalled.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), server.port()), 2000);
            stalled.setSoTimeout(3000);
            @SuppressWarnings("unchecked")
            var connections = (Map<Integer,?>)field(server, "connections");
            WorldRevisionTest.waitFor(() -> !connections.isEmpty());
            Object peer = connections.values().iterator().next();
            ((Socket)field(peer, "socket")).setSendBufferSize(1024);
            Protocol.write(stalled.getOutputStream(), new Protocol.Hello(4, "Leitura suspensa"));
            assertInstanceOf(Protocol.Welcome.class,Protocol.read(stalled.getInputStream()));
            var outgoing = (ArrayBlockingQueue<?>)field(peer, "outgoing");
            var controls = (ArrayBlockingQueue<?>)field(peer, "controls");
            assertEquals(1, outgoing.size() + outgoing.remainingCapacity());
            assertEquals(4, controls.size() + controls.remainingCapacity());
            try (var healthy = Task183NetworkLabTest.peer(server.port(), "Leitura normal")) {
                long tick = server.snapshot().tick();
                int maxOut = 0, maxControls = 0, maxCommands = 0, blockedSamples = 0;
                long nextPing = 0;
                long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
                while (System.nanoTime() < until) {
                    maxOut = Math.max(maxOut, outgoing.size());
                    maxControls = Math.max(maxControls, controls.size());
                    maxCommands = Math.max(maxCommands, server.pendingCommands());
                    assertTrue(outgoing.size() <= 1);
                    assertTrue(controls.size() <= 3, "Conteudo conserva uma posicao para Pong");
                    assertTrue(server.pendingCommands() <= 64);
                    Thread currentWriter = (Thread)field(peer,"writer");
                    if (currentWriter != null && controls.size() == 3 && Arrays.stream(currentWriter.getStackTrace())
                        .anyMatch(s -> s.getClassName().startsWith("sun.nio.ch.") && s.getMethodName().toLowerCase(Locale.ROOT).contains("write")))
                        blockedSamples++;
                    // Perfil normal: não disputar controles com CHUNK durante preparo.
                    if (!healthy.revisions().blocked() && System.nanoTime() >= nextPing) {
                        healthy.measureLatency();
                        nextPing = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(500);
                    }
                    Thread.sleep(10);
                }
                assertTrue(blockedSamples >= 10, "Writer retido em IO com tres controles de conteudo por amostras repetidas: " + blockedSamples);
                assertEquals(3,maxControls);
                assertTrue(server.diagnostics().ticks.count() > 10, "Owner continua executando; gameplay fica congelado na barreira");
                assertTrue(healthy.connected(), healthy.problem());
                Thread writer = (Thread)field(peer, "writer");
                assertTrue(writer.isAlive());
                assertTrue(writer.getName().startsWith("coop-writer-"));
                // Ler alguns frames após a retenção também verifica enquadramento íntegro.
                for (int i = 0; i < 4; i++) assertInstanceOf(Protocol.Content.class, Protocol.read(stalled.getInputStream()));
                stalled.close();
                WorldRevisionTest.waitFor(() -> server.players() == 1 && !healthy.revisions().blocked()
                    && healthy.snapshot() != null);
                assertEquals(revision.hash(), healthy.revisions().hash());
                assertTrue(healthy.send(Protocol.Action.MOVE, -1, 0));
                assertNull(server.failure());
                System.out.printf(Locale.ROOT,
                    "TASK187 descriptorBytes=%d peak(out/control/cmd)=%d/%d/%d blockedSamples=%d replaced=%d bytesWritten=%d ticksAfterRecovery=%d recovered=true%n",
                    descriptorBytes, maxOut, maxControls, maxCommands, blockedSamples, server.diagnostics().replacedSnapshots,
                    server.diagnostics().bytesWritten.get(), server.snapshot().tick()-tick);
            }
        }
    }

    static byte[] frame(int opcode, String hexPayload) {
        byte[] data = HexFormat.of().parseHex(hexPayload);
        return ByteBuffer.allocate(data.length + 6).putInt(data.length + 2).put((byte)1).put((byte)opcode).put(data).array();
    }

    @Test void fifteenOpcodesMatchIndependentGoldenFramesAndRejectRecursiveWrappers() throws Exception {
        String one = "0000000000000001", hash = "40" + "61".repeat(64);
        var state = new Protocol.State(1, List.of());
        var session = new Protocol.SessionState(state, List.of());
        var combat = new Protocol.CombatState(session, List.of());
        var intent = new Protocol.Intent(1, Protocol.Action.MOVE, 1, 0);
        var messages = List.<Protocol.Message>of(new Protocol.Hello(1,"Teste"),new Protocol.Welcome(1),intent,state,
            new Protocol.Bye(),new Protocol.Error("Teste"),new Protocol.AppearanceRequest(1,"none","none"),
            session,combat,new Protocol.WorldRequest(1,Protocol.WorldAction.RESPAWN,"teste"),new Protocol.Ping(1),
            new Protocol.Pong(1),new Protocol.Content(Protocol.ContentKind.READY,1,"a".repeat(64),0,new byte[0]),
            new Protocol.RevisionState("a".repeat(64),combat),new Protocol.RevisionCommand("a".repeat(64),intent));
        String stateFrame = HexFormat.of().formatHex(frame(4, one+"00"));
        String sessionFrame = HexFormat.of().formatHex(frame(8,stateFrame+"00"));
        String combatFrame = HexFormat.of().formatHex(frame(9,sessionFrame+"00"+"06666f72657374"+"00"));
        String intentFrame = HexFormat.of().formatHex(frame(3,one+"01"+"3f80000000000000"));
        var payloads = List.of("00000001055465737465","00000001",one+"01"+"3f80000000000000",one+"00","",
            "055465737465",one+"046e6f6e65046e6f6e65",stateFrame+"00",sessionFrame+"00"+"06666f72657374"+"00",
            one+"00057465737465",one,one,"03"+one+hash+"000000000000",hash+combatFrame,hash+intentFrame);
        for (int i = 0; i < messages.size(); i++) {
            var out = new ByteArrayOutputStream();
            Protocol.write(out, messages.get(i));
            assertArrayEquals(frame(i+1,payloads.get(i)), out.toByteArray(), "Opcode " + (i+1));
            var decoded = Protocol.read(new ByteArrayInputStream(out.toByteArray()));
            var again = new ByteArrayOutputStream();
            Protocol.write(again,decoded);
            assertArrayEquals(out.toByteArray(),again.toByteArray());
            assertTrue(out.size() <= Protocol.MAX_FRAME + 4);
        }
        for (int app = 1; app <= 4; app++) {
            var out = new ByteArrayOutputStream();
            Protocol.write(out,new Protocol.Hello(app,"Teste"));
            assertEquals(new Protocol.Hello(app,"Teste"),Protocol.read(new ByteArrayInputStream(out.toByteArray())));
        }
        for (byte[] bad : List.of(frame(8,sessionFrame+"00"),frame(14,hash+HexFormat.of().formatHex(frame(14,hash+combatFrame))),
            frame(15,hash+HexFormat.of().formatHex(frame(15,hash+intentFrame))),ByteBuffer.allocate(4).putInt(4097).array()))
            assertThrows(IOException.class, () -> Protocol.read(new ByteArrayInputStream(bad)));
        System.out.println("TASK187 goldenOpcodes=1..15 applications=1..4 malformedWrappersRejected=true");
    }
}
