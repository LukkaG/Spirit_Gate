package com.projetok.multiplayer;

import com.projetok.domain.entity.*;
import com.projetok.domain.movement.*;
import com.projetok.prototype.*;
import com.projetok.prototype.rpg.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Task178CollisionTest {
    final CollisionService collision=new CollisionService();
    GameEntity body(int id,float x,float y){return new GameEntity(id,"Player",new Position(x,y)){};}
    void free(GameEntity b,CollisionService.Space s){assertTrue(collision.isFree(b,b.getPosition().x(),b.getPosition().y(),12,s),b.getPosition().toString());}
    @Test void reportedSideCaseHasVisualOverlapWithoutPhysicalPenetration()throws Exception{
        var b=body(1,368,240);free(b,RpgSessionGeometry.raw());
        var sprites=RpgPlayerSprites.load(Path.of("../assets/dev/player-base"),250,.5);
        var bounds=sprites.visualBounds(DirectionalAnimation.Direction.LEFT,2,true,368,240);
        assertEquals(7.5,bounds.getMaxX()-380,.001);
        assertFalse(collision.isFree(b,368,240,12,RpgSessionGeometry.player()));
        b.setPosition(new Position(358,240));free(b,RpgSessionGeometry.player());assertTrue(sprites.visualBounds(DirectionalAnimation.Direction.LEFT,2,true,358,240).getMaxX()<380);
    }
    @Test void fourApproachesStopAtCategorySpecificContact(){
        for(float[] c:new float[][]{{300,240,200,0,358,240},{600,240,-200,0,522,240},{440,100,0,300,440,168},{440,400,0,-300,440,312}}){
            var b=body(1,c[0],c[1]);collision.resolveContinuous(b,c[2],c[3],12,RpgSessionGeometry.player());assertEquals(c[4],b.getPosition().x(),.001);assertEquals(c[5],b.getPosition().y(),.001);free(b,RpgSessionGeometry.player());
        }
    }
    @Test void diagonalAlongWallSlidesWithoutSpeedGain(){var b=body(1,358,220);var direction=new MovementService(collision).direction(1,1);collision.resolveContinuous(b,direction.x()*215/30,direction.y()*215/30,12,RpgSessionGeometry.player());assertEquals(358,b.getPosition().x());assertEquals(220+215/30d/Math.sqrt(2),b.getPosition().y(),.0001);free(b,RpgSessionGeometry.player());}
    @Test void cornerCanBeRoundedWithoutEnteringSolid(){var b=body(1,358,312);collision.resolveContinuous(b,164,0,12,RpgSessionGeometry.player());collision.resolveContinuous(b,0,-200,12,RpgSessionGeometry.player());assertEquals(522,b.getPosition().x());assertEquals(112,b.getPosition().y());free(b,RpgSessionGeometry.player());}
    @Test void capsuleFitsExactCorridorAndBlocksNarrowerOne(){
        var b=body(1,142,100);
        var corridor=RpgCollisionProfile.scenery(new CollisionService.Space(960,640,List.of(new CollisionService.Obstacle(100,0,20,300),new CollisionService.Obstacle(164,0,20,300)),List.of()));
        free(b,corridor);collision.resolveContinuous(b,0,100,12,corridor);assertEquals(200,b.getPosition().y());assertFalse(collision.isFree(b,141.9f,100,12,corridor));
        var narrow=RpgCollisionProfile.scenery(new CollisionService.Space(960,640,List.of(new CollisionService.Obstacle(100,0,20,300),new CollisionService.Obstacle(163,0,20,300)),List.of()));assertFalse(collision.isFree(b,141.5f,100,12,narrow));
    }
    @Test void playersStillTouchAtTwentyFourBesideTheSameWall(){var a=body(1,358,240);var b=body(2,358,264);var p=RpgSessionGeometry.player();var space=new CollisionService.Space(960,640,p.obstacles(),List.of(new CollisionService.Body(b,12)));free(a,space);collision.resolveContinuous(a,0,100,12,space);assertEquals(240,a.getPosition().y());assertEquals(24,Math.hypot(a.getPosition().x()-b.getPosition().x(),a.getPosition().y()-b.getPosition().y()));}
    @Test void allDirectionsAndWalkFramesHaveStableClearance()throws Exception{
        var sprites=RpgPlayerSprites.load(Path.of("../assets/dev/player-base"),250,.5);var b=body(1,358,240);var image=new BufferedImage(700,500,2);var g=image.createGraphics();
        try{for(var direction:DirectionalAnimation.Direction.values())for(int frame=0;frame<5;frame++){boolean walking=frame!=0;int walk=Math.max(0,frame-1);assertTrue(sprites.drawFrame(g,direction,walk,walking,358,240));assertTrue(sprites.visualBounds(direction,walk,walking,358,240).getMaxX()<380);assertEquals(new Position(358,240),b.getPosition());free(b,RpgSessionGeometry.player());}}finally{g.dispose();}
    }
    DebugSnapshot.ActorView actor(float y){return new DebugSnapshot.ActorView(1,"Player",440,y,12,100,100,true,true);}
    @Test void tallObjectOccludesBehindAndCharacterOccludesInFront(){
        for(float y:new float[]{168,312,300}){
            var a=actor(y);var image=new BufferedImage(960,640,2);var g=image.createGraphics();
            StructureRenderer.draw(g,RpgSessionGeometry.structures(),List.of(a),v->{g.setColor(Color.RED);g.fillRect(430,Math.round(v.y()-70),20,70);});g.dispose();
            if(y==168)assertNotEquals(Color.RED.getRGB(),image.getRGB(440,150));else assertEquals(Color.RED.getRGB(),image.getRGB(440,270));
        }
        assertEquals(300,RpgSessionGeometry.CENTRAL.supportY());assertEquals(132,RpgSessionGeometry.CENTRAL.visualTop());assertEquals(180,RpgSessionGeometry.CENTRAL.footprint().y());
    }
    @Test void cornerInterpolationUsesValidAuthorityWithoutRubberBanding(){
        var diagnostics=new NetworkDiagnostics();var t=new RemoteTimeline(diagnostics);
        var old=new Protocol.State(0,List.of(new Protocol.Actor(1,"Local",140,300,100,100,100,0,false),new Protocol.Actor(2,"Remote",358,290,100,100,100,0,false)));
        var next=new Protocol.State(3,List.of(old.actors().get(0),new Protocol.Actor(2,"Remote",380,312,100,100,100,0,false)));
        diagnostics.received(1_000_000_000L);t.offer(old,1_000_000_000L,"forest");diagnostics.received(1_100_000_000L);t.offer(next,1_100_000_000L,"forest");
        for(int ms=0;ms<=120;ms+=10){var visual=t.sample(1_100_000_000L+ms*1_000_000L,1).actors().get(1);assertEquals(380,visual.x());assertEquals(312,visual.y());assertTrue(collision.isFree(body(3,140,300),visual.x(),visual.y(),12,RpgSessionGeometry.player()));}
        assertEquals(1,t.obstacleCorrections);assertEquals(380,next.actors().get(1).x());
    }
    @Test void serverMovementStillValidatesInputAndUsesNewSceneryContract(){var w=new RpgSessionWorld();w.join(1,"Player A");assertFalse(w.accept(1,new Protocol.Intent(1,Protocol.Action.MOVE,8,0)));for(int i=2;i<=80;i++){w.accept(1,new Protocol.Intent(i,Protocol.Action.MOVE,1,0));w.step();}var a=w.snapshot().actors().get(0);assertEquals(358,a.x(),.001);assertEquals(300,a.y());assertEquals(100,a.maxHp());}
    @Test void thinObstacleCannotBeSkippedWithSceneryCapsule(){var b=body(1,100,200);var space=RpgCollisionProfile.scenery(new CollisionService.Space(960,640,List.of(new CollisionService.Obstacle(200,100,1,200)),List.of()));collision.resolveContinuous(b,400,0,12,space);assertEquals(178,b.getPosition().x());free(b,space);}
    @Test void costumesDoNotChangeNewWallContactOrAttributes()throws Exception{
        var catalog=CosmeticCatalog.metadata(Path.of("../assets/dev/generated/corujao-iv-v1"));var dressed=new RpgSessionWorld(List.of("reaper","human"),catalog);var base=new RpgSessionWorld();
        for(var world:List.of(dressed,base))world.join(1,"Player A");assertTrue(dressed.appearance(1,new Protocol.AppearanceRequest(1,"hair_spiky","light_armor")));
        for(int i=2;i<90;i++){for(var world:List.of(dressed,base)){world.accept(1,new Protocol.Intent(i,Protocol.Action.MOVE,1,0));world.step();}assertEquals(base.snapshot(),dressed.snapshot());}
        assertEquals(358,dressed.snapshot().actors().get(0).x());
    }
    @Test void tcpClientsRemainAuthoritativeBesideTheObstacle()throws Exception{
        try(var server=new LocalSimulationServer(java.net.InetAddress.getLoopbackAddress(),0,new RpgSessionWorld())){
            server.start();try(var a=new LocalClient(java.net.InetAddress.getLoopbackAddress(),server.port(),"Player A");var b=new LocalClient(java.net.InetAddress.getLoopbackAddress(),server.port(),"Player B")){
                a.await(s->s.actors().size()==2,java.time.Duration.ofSeconds(3));
                long deadline=System.nanoTime()+4_000_000_000L;
                while(a.snapshot().actors().get(0).x()<358&&System.nanoTime()<deadline){a.send(Protocol.Action.MOVE,1,0);b.send(Protocol.Action.MOVE,1,0);Thread.sleep(50);}
                a.send(Protocol.Action.MOVE,0,0);b.send(Protocol.Action.MOVE,0,0);
                for(int i=0;i<12;i++){b.send(Protocol.Action.MOVE,0,-1);Thread.sleep(50);}b.send(Protocol.Action.MOVE,0,0);
                var state=a.await(s->s.tick()>30,java.time.Duration.ofSeconds(3));var one=state.actors().get(0);var two=state.actors().get(1);
                assertEquals(358,one.x(),.001);assertTrue(Math.hypot(one.x()-two.x(),one.y()-two.y())>=24-.001);
                for(var actor:state.actors())assertTrue(collision.isFree(body(9,100,100),actor.x(),actor.y(),12,RpgSessionGeometry.player()));
                assertTrue(a.connected()&&b.connected());assertNull(server.failure());
            }
        }
    }
}
