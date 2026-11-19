package com.projetok.multiplayer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Comparacao isolada com Netty; nao instala pipeline no servidor de producao. */
class NettyFrameCompatibilityTest {
    /** Fragmentacao TCP em todas as bordas preserva o payload do framing atual. */
    @Test void nettyDecoderMatchesCurrentLengthPrefixAcrossEverySplit() throws Exception {
        for (int application : List.of(6, 7)) {
            byte[] frame = ProtocolPropertiesTest.encode(new Protocol.Hello(application, "Rede"));
            for (int split = 1; split < frame.length; split++) {
                var channel = new EmbeddedChannel(new LengthFieldBasedFrameDecoder(
                        Protocol.MAX_FRAME + 4, 0, 4, 0, 4));
                try {
                    channel.writeInbound(Unpooled.wrappedBuffer(Arrays.copyOfRange(frame, 0, split)));
                    assertNull(channel.readInbound(), "Frame parcial nao deve ser emitido");
                    channel.writeInbound(Unpooled.wrappedBuffer(Arrays.copyOfRange(frame, split, frame.length)));
                    ByteBuf payload = channel.readInbound();
                    assertNotNull(payload);
                    try {
                        byte[] actual = new byte[payload.readableBytes()];
                        payload.readBytes(actual);
                        assertArrayEquals(Arrays.copyOfRange(frame, 4, frame.length), actual);
                    } finally {
                        payload.release();
                    }
                } finally {
                    channel.finishAndReleaseAll();
                }
            }
        }
    }
}

