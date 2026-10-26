package com.projetok.multiplayer;

import com.projetok.domain.entity.*;
import com.projetok.domain.movement.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

class Task177GeometryTest {
    final CollisionService collision=new CollisionService();
    final float radius=CharacterFootprint.RPG_NETWORK.radius();
    GameEntity entity(int id,float x,float y){return new GameEntity(id,"Body",new Position(x,y)){};}
    CollisionService.Space space(GameEntity other,List<CollisionService.Obstacle> walls){return new CollisionService.Space(960,640,walls,other==null?List.of():List.of(new CollisionService.Body(other,radius)));}
    @Test void distanceAndLateralContactUseGroundCircles(){
        var a=entity(1,100,100);var b=entity(2,200,100);var s=space(b,List.of());
        assertTrue(collision.isFree(a,100,100,radius,s));assertTrue(collision.isFree(a,176,100,radius,s));assertFalse(collision.isFree(a,176.01f,100,radius,s));
        collision.resolveContinuous(a,200,0,radius,s);assertEquals(176,a.getPosition().x(),.001);assertEquals(100,a.getPosition().y());
        collision.resolveContinuous(a,0,30,radius,s);assertEquals(130,a.getPosition().y(),.001);
    }
    @Test void simultaneousOppositeMotionNeverOverlaps(){
        var a=entity(1,100,100);var b=entity(2,200,100);
        for(int i=0;i<30;i++){collision.resolveContinuous(a,4,0,radius,space(b,List.of()));collision.resolveContinuous(b,-4,0,radius,space(a,List.of()));assertTrue(Math.hypot(a.getPosition().x()-b.getPosition().x(),a.getPosition().y()-b.getPosition().y())>=24-.001);}
        assertEquals(24,b.getPosition().x()-a.getPosition().x(),.001);
    }
    @Test void normalizedDiagonalKeepsSpeed(){var service=new MovementService(collision);var direction=service.direction(1,1);var a=entity(1,100,100);service.moveContinuous(a,direction.x()*215/30,direction.y()*215/30,radius,space(null,List.of()));assertEquals(215d/30,Math.hypot(a.getPosition().x()-100,a.getPosition().y()-100),.0001);}
    @Test void narrowCorridorFitsAndThinObstacleCannotBeSkipped(){
        var a=entity(1,100,100);var walls=List.of(new CollisionService.Obstacle(50,0,38,300),new CollisionService.Obstacle(112,0,38,300),new CollisionService.Obstacle(88,180,24,1));var s=space(null,walls);
        assertTrue(collision.isFree(a,100,100,radius,s));collision.resolveContinuous(a,0,300,radius,s);assertEquals(168,a.getPosition().y(),.001);
    }
    @Test void wallCornerAndBoundsRemainSolid(){var a=entity(1,100,100);var s=space(null,List.of(new CollisionService.Obstacle(120,120,100,100)));collision.resolveContinuous(a,30,30,radius,s);assertTrue(collision.isFree(a,a.getPosition().x(),a.getPosition().y(),radius,s));assertTrue(a.getPosition().y()<=108);collision.resolveContinuous(a,-1000,0,radius,s);assertEquals(radius,a.getPosition().x(),.001);}
    @Test void multiplayerUsesCloserContactWithoutChangingCombatAttributes(){
        var w=new RpgSessionWorld();assertTrue(w.join(1,"Player A"));assertTrue(w.join(2,"Player B"));var initial=w.snapshot().actors().get(0);
        for(int i=1;i<=60;i++){assertTrue(w.accept(2,new Protocol.Intent(i,Protocol.Action.MOVE,0,-1)));w.step();}
        var a=w.snapshot().actors().get(0);var b=w.snapshot().actors().get(1);assertEquals(24,Math.hypot(a.x()-b.x(),a.y()-b.y()),.001);assertEquals(initial.maxHp(),a.maxHp());assertEquals(initial.coins(),a.coins());
    }
    @Test void spawnAfterMovementChoosesFreeGround(){var w=new RpgSessionWorld();w.join(1,"Player A");for(int i=1;i<=15;i++){w.accept(1,new Protocol.Intent(i,Protocol.Action.MOVE,0,1));w.step();}assertTrue(w.join(2,"Player B"));var a=w.snapshot().actors().get(0);var b=w.snapshot().actors().get(1);assertTrue(Math.hypot(a.x()-b.x(),a.y()-b.y())>=24);}
    @Test void delayedPresentationCannotChangeCollisionOrAuthoritativeState(){var w=new RpgSessionWorld();w.join(1,"Player A");w.join(2,"Player B");var old=w.snapshot();w.accept(2,new Protocol.Intent(1,Protocol.Action.MOVE,0,-1));w.step();var next=w.snapshot();new RemotePresentation(old,next).sample(0,1);assertEquals(next,w.snapshot());}
    @Test void allMapEdgesRetainHistoricalSaveAndProtocolBounds(){for(float[] direction:new float[][]{{1,0},{-1,0},{0,1},{0,-1}}){var w=new RpgSessionWorld();w.join(1,"Player A");for(int i=1;i<=200;i++){w.accept(1,new Protocol.Intent(i,Protocol.Action.MOVE,direction[0],direction[1]));w.step();}var p=w.snapshot().actors().get(0);assertTrue(p.x()>=18&&p.x()<=942&&p.y()>=18&&p.y()<=622);}}
}
