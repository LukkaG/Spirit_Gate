package com.projetok.multiplayer;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.net.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class NetworkRobustnessTest {
    private Protocol.State faults(long seed)throws Exception {
        record Packet(int due,int id,Protocol.Intent intent){}
        var random=new Random(seed);var world=new CoopWorld();world.join(1,"Jogador Um");world.join(2,"Jogador Dois");var pending=new ArrayList<Packet>();long sequence=0;
        for(int tick=0;tick<2500;tick++){
            if(tick==400)world.leave(1);if(tick==410)world.join(3,"Reconectado");
            if(pending.size()<100&&random.nextInt(4)!=0){int id=tick>410&&random.nextBoolean()?3:1;var action=Protocol.Action.values()[random.nextInt(4)];float x=action==Protocol.Action.MOVE?random.nextInt(-1,2):random.nextInt(960),y=action==Protocol.Action.MOVE?random.nextInt(-1,2):random.nextInt(640);var intent=new Protocol.Intent(++sequence,action,x,y);pending.add(new Packet(tick+random.nextInt(12),id,intent));if(random.nextBoolean())pending.add(new Packet(tick+random.nextInt(15),id,intent));}
            Collections.shuffle(pending,random);for(var it=pending.iterator();it.hasNext();){var packet=it.next();if(packet.due()<=tick){if(tick>400&&packet.id()==1)assertFalse(world.accept(1,packet.intent()));else world.accept(packet.id(),packet.intent());it.remove();}}
            world.step();new Protocol.State(world.snapshot().tick(),world.snapshot().actors());assertTrue(pending.size()<=102);
        }
        var encoded=new ByteArrayOutputStream();Protocol.write(encoded,world.snapshot());assertTrue(encoded.size()<=Protocol.MAX_FRAME+4);return world.snapshot();
    }
    @Test void lossDelayDuplicationReorderAndDisconnectAreDeterministic()throws Exception{assertEquals(faults(74074),faults(74074));}
    @Test void absentInputExpiresAndClosedSessionsCannotAct(){var world=new CoopWorld();world.join(1,"Jogador Um");world.accept(1,new Protocol.Intent(1,Protocol.Action.MOVE,1,0));for(int i=0;i<20;i++)world.step();float stopped=AuthoritativeCombatTest.actor(world,1).x();for(int i=0;i<30;i++)world.step();assertEquals(stopped,AuthoritativeCombatTest.actor(world,1).x());world.leave(1);assertFalse(world.accept(1,new Protocol.Intent(2,Protocol.Action.LIGHTNING_STEP,700,300)));}
    @Test void malformedConnectionCannotBreakServerAndPendingHandshakeCloses()throws Exception{
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0)){server.start();try(var malformed=new Socket(InetAddress.getLoopbackAddress(),server.port())){malformed.setSoTimeout(2500);new DataOutputStream(malformed.getOutputStream()).writeInt(Integer.MAX_VALUE);assertEquals(-1,malformed.getInputStream().read());}
            try(var silent=new Socket(InetAddress.getLoopbackAddress(),server.port());var good=new LocalClient(InetAddress.getLoopbackAddress(),server.port(),"Valido")){good.await(s->s.actors().stream().anyMatch(a->a.id()==good.id()),java.time.Duration.ofSeconds(3));assertTrue(server.connections()<=2);assertTrue(server.pendingCommands()<=64);silent.setSoTimeout(2500);server.close();assertEquals(-1,silent.getInputStream().read());assertNull(server.failure());assertThrows(IllegalStateException.class,server::start);}
        }
    }
}
