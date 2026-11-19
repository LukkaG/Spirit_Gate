package com.projetok.multiplayer;

import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Invariantes geradas com semente fixa, sem dependencia de bibliotecas de propriedades adicionais. */
class ProtocolPropertiesTest {
    /** Comandos validos mantem todos os campos apos a serializacao. */
    @Test void generatedValidCommandsRoundTripWithoutChangingValues() throws Exception {
        var random = new Random(17112026L);
        for (int index = 1; index <= 1000; index++) {
            var intent = new Protocol.Intent(index, Protocol.Action.MOVE,
                    random.nextInt(3) - 1, random.nextInt(3) - 1);
            assertEquals(intent, decode(encode(intent)));
            var request = new SkillProtocol.Request(index, 1 + random.nextInt(5),
                    0, 0, random.nextInt(961), random.nextInt(641));
            assertEquals(request, decode(encode(request)));
        }
    }

    /** Aplicacoes 6/7 preservam o envelope e nao permitem cauda invisivel no frame. */
    @Test void negotiatedApplicationsPreserveTheirBytesAndRejectTrailingPayload() throws Exception {
        for (int application : List.of(6, 7)) {
            var hello = new Protocol.Hello(application, "Viajante");
            byte[] bytes = encode(hello);
            assertEquals(hello, decode(bytes));
            var payload = Arrays.copyOfRange(bytes, 4, bytes.length + 1);
            var malformed = new ByteArrayOutputStream();
            var out = new DataOutputStream(malformed);
            out.writeInt(payload.length);
            out.write(payload);
            assertThrows(IOException.class, () -> decode(malformed.toByteArray()));
        }
    }

    /** Valores nao finitos nao atravessam a fronteira entre rede e geometria. */
    @Test void nonFiniteCoordinatesNeverBecomeCommands() {
        for (float value : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new SkillProtocol.Request(1, 1, 0, 0, value, 10));
        }
    }

    /** Codifica no contrato real, sem reproduzir manualmente seu formato. */
    static byte[] encode(Protocol.Message message) throws IOException {
        var output = new ByteArrayOutputStream();
        Protocol.write(output, message);
        return output.toByteArray();
    }

    /** Decodifica somente memoria local limitada. */
    static Protocol.Message decode(byte[] input) throws IOException {
        return Protocol.read(new ByteArrayInputStream(input));
    }
}

