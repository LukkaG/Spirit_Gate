package com.projetok.multiplayer;
import org.junit.jupiter.api.Test;
import java.net.*;
import static org.junit.jupiter.api.Assertions.*;
class LocalServerTest {
    @Test void realLoopbackHandshakeMovementAndShutdown()throws Exception{
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0)){server.start();try(var socket=new Socket(InetAddress.getLoopbackAddress(),server.port())){socket.setSoTimeout(2500);Protocol.write(socket.getOutputStream(),new Protocol.Hello(1,"Jogador Um"));int id=((Protocol.Welcome)Protocol.read(socket.getInputStream())).id();Protocol.write(socket.getOutputStream(),new Protocol.Intent(1,Protocol.Action.MOVE,1,0));Protocol.State state;do{state=(Protocol.State)Protocol.read(socket.getInputStream());}while(state.actors().stream().noneMatch(a->a.id()==id&&a.x()>140));assertNull(server.failure());}server.close();assertFalse(server.running());}
    }
    @Test void rejectsPublicBindAndStaleCommands()throws Exception{assertThrows(IllegalArgumentException.class,()->new LocalSimulationServer(InetAddress.getByName("0.0.0.0"),0));var world=new CoopWorld();assertTrue(world.join(1,"Jogador Um"));assertTrue(world.accept(1,new Protocol.Intent(2,Protocol.Action.MOVE,1,0)));assertFalse(world.accept(1,new Protocol.Intent(1,Protocol.Action.MOVE,-1,0)));world.leave(1);assertFalse(world.accept(1,new Protocol.Intent(3,Protocol.Action.MOVE,1,0)));}
}
