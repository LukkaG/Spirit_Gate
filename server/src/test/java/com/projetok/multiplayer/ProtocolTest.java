package com.projetok.multiplayer;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class ProtocolTest {
    @Test void roundTripsExplicitMessages()throws Exception{for(Protocol.Message m:List.of(new Protocol.Hello(1,"Viajante"),new Protocol.Welcome(1),new Protocol.Intent(1,Protocol.Action.MOVE,1,-1),new Protocol.State(12,List.of(new Protocol.Actor(1,"Viajante",100,100,80,100,65,20,false))),new Protocol.Bye(),new Protocol.Error("incompativel"))){var out=new ByteArrayOutputStream();Protocol.write(out,m);assertEquals(m,Protocol.read(new ByteArrayInputStream(out.toByteArray())));}}
    @Test void rejectsOversizeUnknownTruncationAndNonFinite()throws Exception{for(byte[] bytes:new byte[][]{{0,0,32,0},{0,0,0,2,1,99},{0,0,0,3,1,5},{0,0,0,2,2,5}})assertThrows(IOException.class,()->Protocol.read(new ByteArrayInputStream(bytes)));assertThrows(IllegalArgumentException.class,()->new Protocol.Intent(1,Protocol.Action.MOVE,Float.NaN,0));}
}
