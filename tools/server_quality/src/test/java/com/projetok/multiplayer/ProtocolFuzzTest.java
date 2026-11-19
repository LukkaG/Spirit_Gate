package com.projetok.multiplayer;

import com.code_intelligence.jazzer.junit.FuzzTest;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

/** Fuzzing do parser real, com corpus e saidas mantidos exclusivamente na copia temporaria. */
class ProtocolFuzzTest {
    /** Inclui mensagens validas para exercitar a decodificacao alem da rejeicao inicial. */
    static Stream<byte[]> seeds() throws IOException {
        return Stream.of(
                ProtocolPropertiesTest.encode(new Protocol.Hello(6, "Sessao")),
                ProtocolPropertiesTest.encode(new Protocol.Hello(7, "Sessao")),
                ProtocolPropertiesTest.encode(new Protocol.Intent(1, Protocol.Action.MOVE, 1, -1)),
                ProtocolPropertiesTest.encode(new SkillProtocol.Request(1, 1, 0, 0, 140, 300)),
                ProtocolPropertiesTest.encode(new SkillProtocol.State("a".repeat(64), 1, List.of(),
                        List.of(0, 0, 0, 0, 0), 0, false)),
                new byte[]{0, 0, 0, 2, 1, 99});
    }

    /** Todo frame aceito possui uma representacao canonica estavel; erros de formato sao esperados. */
    @MethodSource("seeds")
    @FuzzTest(maxDuration = "20s", maxExecutions = 100000)
    void acceptedFramesHaveStableCanonicalSerialization(byte[] bytes) throws IOException {
        if (bytes.length > Protocol.MAX_FRAME + 4) return;
        Protocol.Message accepted;
        try {
            accepted = ProtocolPropertiesTest.decode(bytes);
        } catch (IOException malformed) {
            return;
        }
        byte[] canonical = ProtocolPropertiesTest.encode(accepted);
        assertTrue(canonical.length <= Protocol.MAX_FRAME + 4);
        assertArrayEquals(canonical,
                ProtocolPropertiesTest.encode(ProtocolPropertiesTest.decode(canonical)));
    }
}

