package com.projetok.multiplayer;

import java.util.List;
import com.projetok.prototype.DirectionalAnimation.Direction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Orientação e frame remoto são apresentação e não alteram o snapshot original. */
class RemoteSpriteAnimationTest {
    Protocol.State state(long tick,float x){return new Protocol.State(tick,List.of(new Protocol.Actor(1,"Teste",x,100,100,100,1000,0,false,1,0,0)));}
    @Test void fourFrameWalkUses250MillisecondsAndRemovesDisconnectedActor(){var animation=new RemoteSpriteAnimation();animation.update(state(1,100),0);assertFalse(animation.pose(1,0).walking());var moved=state(4,110);animation.update(moved,100);assertTrue(animation.pose(1,349).walking());assertEquals(0,animation.pose(1,349).frame());assertEquals(1,animation.pose(1,350).frame());assertEquals(Direction.RIGHT,animation.pose(1,350).direction());assertEquals(110,moved.actors().get(0).x());animation.update(new Protocol.State(5,List.of()),500);assertFalse(animation.pose(1,500).walking());}
}
