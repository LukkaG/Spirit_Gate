package com.projetok.multiplayer;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Um keepalive não pode ser expulso por controles preenchidos pelo próprio produtor de conteúdo. */
class Task190ControlFairnessTest {
    @Test void onePingDuringBulkContentHasReservedControlCapacity() throws Exception {
        var world=new RpgSessionWorld(List.of("reaper","human"),null,true);
        world.enableRevisions(Task187BackpressureTest.padded());
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world);var socket=new Socket()) {
            server.start();socket.setReceiveBufferSize(1024);socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(),server.port()));socket.setSoTimeout(3000);
            @SuppressWarnings("unchecked")var peers=(Map<Integer,?>)Task187BackpressureTest.field(server,"connections");
            WorldRevisionTest.waitFor(()->!peers.isEmpty());Object peer=peers.values().iterator().next();
            ((Socket)Task187BackpressureTest.field(peer,"socket")).setSendBufferSize(1024);
            Protocol.write(socket.getOutputStream(),new Protocol.Hello(4,"Keepalive unico"));
            assertInstanceOf(Protocol.Welcome.class,Protocol.read(socket.getInputStream()));
            var controls=(ArrayBlockingQueue<?>)Task187BackpressureTest.field(peer,"controls");
            WorldRevisionTest.waitFor(()->controls.size()>=3);
            Thread.sleep(200); // Estabilizar o writer retido, sem drenar o socket.
            int before=controls.size();Protocol.write(socket.getOutputStream(),new Protocol.Ping(1));Thread.sleep(100);
            assertEquals(1,server.players(),"Ping único deve coexistir com a transferência; controles anteriores="+before);
            boolean pong=false;
            for(int i=0;i<400&&!pong;i++)pong=Protocol.read(socket.getInputStream()) instanceof Protocol.Pong p&&p.nonce()==1;
            assertTrue(pong);assertNull(server.failure());
            assertEquals(4,controls.size()+controls.remainingCapacity());
            System.out.println("TASK190 bulkContentControlsBeforePing="+before+" singlePingAccepted=true pongReceived=true controlCapacity=4");
        }
    }
}
