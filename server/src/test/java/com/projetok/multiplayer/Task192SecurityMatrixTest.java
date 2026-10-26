package com.projetok.multiplayer;

import com.projetok.prototype.rpg.LauncherSession;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class Task192SecurityMatrixTest {
    @TempDir Path root;
    @Test void boundedFloodIsolatesSenderAndPreservesOtherSession() throws Exception {
        try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,root);
            var healthy=new LocalClient(InetAddress.getLoopbackAddress(),server.port(),"Legitimo");
            var flood=new Socket(InetAddress.getLoopbackAddress(),server.port())) {
            healthy.await(s->!s.actors().isEmpty(),java.time.Duration.ofSeconds(3));
            flood.setSoTimeout(3000);Protocol.write(flood.getOutputStream(),new Protocol.Hello(1,"Flood sintetico"));
            assertInstanceOf(Protocol.Welcome.class,Protocol.read(flood.getInputStream()));
            long start=System.nanoTime();int maxQueue=0,sent=0;
            try {
                for(int i=1;i<=200;i++) {
                    Protocol.write(flood.getOutputStream(),new Protocol.Intent(i,Protocol.Action.MOVE,0,0));sent++;
                    maxQueue=Math.max(maxQueue,server.pendingCommands());Thread.sleep(2);
                }
            } catch(SocketException expected) { /* Fechamento defensivo pelo servidor. */ }
            WorldRevisionTest.waitFor(()->server.players()==1);
            assertTrue(System.nanoTime()-start<TimeUnit.SECONDS.toNanos(3),"Nao atribuir timeout de inatividade ao rate/fila");
            assertTrue(maxQueue<=64);assertTrue(healthy.connected(),healthy.problem());
            long tick=healthy.snapshot().tick();assertTrue(healthy.send(Protocol.Action.MOVE,-1,0));
            healthy.await(s->s.tick()>tick+3,java.time.Duration.ofSeconds(3));
            assertNull(server.failure());
            System.out.println("TASK192 boundedFlood sent="+sent+" peakCommands="+maxQueue+" senderIsolated=true healthyContinues=true");
        }
    }
    @Test void seededMalformedFramesRemainBoundedAndCannotCreateRecursiveObjects() throws Exception {
        var fixtures=List.<Protocol.Message>of(new Protocol.Hello(4,"Sintetico"),new Protocol.State(1,List.of()),
            new Protocol.RevisionCommand("a".repeat(64),new Protocol.Intent(1,Protocol.Action.MOVE,1,0)),
            new Protocol.Content(Protocol.ContentKind.CHUNK,1,"a".repeat(64),0,new byte[2800]));
        List<byte[]> encoded=new ArrayList<>();
        for(var message:fixtures){var bytes=new ByteArrayOutputStream();Protocol.write(bytes,message);encoded.add(bytes.toByteArray());}
        var random=new Random(192);int rejected=0,accepted=0;
        for(int i=0;i<600;i++) {
            byte[] frame=encoded.get(i%encoded.size()).clone();
            for(int mutation=0;mutation<1+i%3;mutation++)frame[random.nextInt(frame.length)]=(byte)random.nextInt(256);
            try {
                var parsed=Protocol.read(new ByteArrayInputStream(frame));var bytes=new ByteArrayOutputStream();Protocol.write(bytes,parsed);
                assertTrue(bytes.size()<=Protocol.MAX_FRAME+4);Protocol.read(new ByteArrayInputStream(bytes.toByteArray()));accepted++;
            }catch(IOException expected){rejected++;}
        }
        for(int size:List.of(Integer.MIN_VALUE,-1,0,1,4097,Integer.MAX_VALUE))
            assertThrows(IOException.class,()->Protocol.read(new ByteArrayInputStream(ByteBuffer.allocate(4).putInt(size).array())));
        assertTrue(rejected>0);assertEquals(600,rejected+accepted);
        System.out.println("TASK192 parserSeed=192 mutations=600 rejected="+rejected+" validMutations="+accepted+" bounded=true");
    }
    @Test void onePeerCannotAcknowledgeForAnotherOrStartPublication() throws Exception {
        try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,root);
            var a=new WorldRevisionTest.RawPeer(server);var b=new WorldRevisionTest.RawPeer(server)) {
            WorldRevisionTest.waitFor(()->a.resumed&&b.resumed);a.ready=true;b.ready=false;
            var managed=LauncherSession.managed(server);var target=managed.target();String old=target.activeHash();
            var next=new WorldRevisionTest().changed(1,500);long previous=Math.max(a.tx,b.tx);
            var pending=target.reserve(old,next);
            WorldRevisionTest.waitFor(()->a.tx>previous&&b.tx>previous&&target.status().phase()==RevisionCoordinator.Phase.WAITING_CLIENTS
                &&target.status().clients().values().stream().filter(s->s.equals("V4 READY")).count()==1);
            a.ack(Protocol.ContentKind.READY,a.tx,next.hash());a.ack(Protocol.ContentKind.READY,a.tx,next.hash());
            a.send(new Protocol.Content(Protocol.ContentKind.BEGIN,999,"0".repeat(64),100,new byte[0]));
            Thread.sleep(150);assertFalse(pending.isDone(),"ACK duplicado de A nao substitui B");assertEquals(old,target.activeHash());
            assertEquals(next.hash(),target.status().candidate());assertEquals("",managed.store().current());
            b.ack(Protocol.ContentKind.READY,b.tx,next.hash());assertTrue(pending.get(5,TimeUnit.SECONDS).accepted());
            assertTrue(target.abortReserved().get(5,TimeUnit.SECONDS).accepted());assertEquals(old,target.activeHash());
            assertEquals("",managed.store().current());assertNull(server.failure());
            System.out.println("TASK192 socketBoundAck=true duplicateCannotReplacePeer=true remoteBeginCannotPublish=true");
        }
    }
}
