package com.projetok.multiplayer;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CorujaoFivePresentationTest {
    private Protocol.State state(long tick,float x){return new Protocol.State(tick,List.of(new Protocol.Actor(2,"Remoto",x,320,100,100,100,0,false)));}
    @Test void staleAnimationAndTeleportNeverMoveLogicalState(){
        var start=state(1,140);var warp=state(4,210);assertEquals(210,new RemotePresentation(start,warp).sample(.5,1).actors().get(0).x());
        var animation=new RemoteSpriteAnimation();animation.update(start,0);animation.update(state(4,145),100);assertTrue(animation.pose(2,200).walking());assertFalse(animation.pose(2,601).walking());
        animation.update(state(2,300),650);assertFalse(animation.pose(2,651).walking());
        assertEquals(140,start.actors().get(0).x());assertEquals(210,warp.actors().get(0).x());
    }
}
