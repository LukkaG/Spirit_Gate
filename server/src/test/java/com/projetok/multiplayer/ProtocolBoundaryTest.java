package com.projetok.multiplayer;

import java.io.*;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Bytes V1 permanecem estáveis; texto inválido não é normalizado silenciosamente. */
class ProtocolBoundaryTest {
    @Test void historicalHelloMatchesGoldenWireBytes()throws Exception{var out=new ByteArrayOutputStream();Protocol.write(out,new Protocol.Hello(1,"Teste"));assertEquals("0000000c010100000001055465737465",HexFormat.of().formatHex(out.toByteArray()));}
    @Test void malformedUtf8AndTrailingBytesAreRejected(){for(String bytes:new String[]{"00000005010602c328","00000003010500","0000000c0101000000010554657374"})assertThrows(IOException.class,()->Protocol.read(new ByteArrayInputStream(HexFormat.of().parseHex(bytes))));}
}
