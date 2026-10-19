package com.projetok.multiplayer;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class RemotePresentationTest {
    @Test void interpolationCannotModifyAuthoritativePhysics(){var old=new Protocol.State(1,List.of(new Protocol.Actor(1,"Local",100,100,100,100,100,0,false),new Protocol.Actor(2,"Remoto",200,100,100,100,100,0,false)));var next=new Protocol.State(4,List.of(new Protocol.Actor(1,"Local",120,100,100,100,100,0,false),new Protocol.Actor(2,"Remoto",240,100,100,100,100,0,false)));var visual=new RemotePresentation(old,next).sample(0.5,1);assertEquals(120,visual.actors().get(0).x());assertEquals(220,visual.actors().get(1).x());assertEquals(240,next.actors().get(1).x());assertThrows(IllegalArgumentException.class,()->new RemotePresentation(old,next).sample(Double.NaN,1));}
}
