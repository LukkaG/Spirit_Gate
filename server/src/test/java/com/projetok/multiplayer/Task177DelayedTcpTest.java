package com.projetok.multiplayer;
import java.net.*;
import java.io.*;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Task177DelayedTcpTest {
    /** Opaque length-prefixed TCP frames, ordered delivery, delay/jitter/holds; no UDP loss model. */
    static final class Relay implements AutoCloseable {
        final ServerSocket listener;volatile Socket front,back;final Thread accept;
        Relay(int upstream)throws IOException {
            listener=new ServerSocket(0,1,InetAddress.getLoopbackAddress());
            accept=new Thread(()->{try{front=listener.accept();back=new Socket(InetAddress.getLoopbackAddress(),upstream);front.setTcpNoDelay(true);back.setTcpNoDelay(true);pump(front,back,false);pump(back,front,true);}catch(IOException ignored){}},"task177-relay");accept.setDaemon(true);accept.start();
        }
        void pump(Socket from,Socket to,boolean jitter){var thread=new Thread(()->{try{var in=new DataInputStream(from.getInputStream());var out=new DataOutputStream(to.getOutputStream());int count=0;while(!from.isClosed()){int size=in.readInt();if(size<1||size>4096)throw new IOException("frame");byte[] bytes=in.readNBytes(size);if(bytes.length!=size)throw new EOFException();int delay=jitter?new int[]{20,40,80}[count%3]:20;if(jitter&&++count%8==0)delay+=120;Thread.sleep(delay);out.writeInt(size);out.write(bytes);out.flush();}}catch(IOException|InterruptedException ignored){}},"task177-ordered-pump");thread.setDaemon(true);thread.start();}
        public void close()throws IOException{listener.close();if(front!=null)front.close();if(back!=null)back.close();accept.interrupt();}
    }
    @Test void twoClientsMoveThroughDelayedOrderedTcpAndKeepSessionAlive()throws Exception {
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,new RpgSessionWorld(List.of("reaper","human"),null,true,20))){
            server.start();try(var relay=new Relay(server.port());var a=LocalClient.combat(InetAddress.getLoopbackAddress(),relay.listener.getLocalPort(),"Delayed A",false);var b=LocalClient.combat(InetAddress.getLoopbackAddress(),server.port(),"Direct B",false)){
                var first=a.await(s->s.actors().size()==3,Duration.ofSeconds(3));float start=first.actors().get(0).x();long tick=first.tick();
                for(int i=0;i<12;i++){assertTrue(a.send(Protocol.Action.MOVE,-1,0));assertTrue(b.send(Protocol.Action.MOVE,1,0));a.measureLatency();Thread.sleep(200);var next=a.snapshot();assertTrue(next.tick()>=tick);tick=next.tick();}
                assertTrue(a.connected(),a.problem());assertTrue(b.connected(),b.problem());assertNull(server.failure());assertTrue(a.latencyMillis()>=35,"Measured RTT "+a.latencyMillis());assertTrue(a.snapshot().actors().get(0).x()<start-50,"Movement under TCP jitter");assertTrue(server.snapshot().tick()>first.tick()+45);
            }
        }
    }
}
