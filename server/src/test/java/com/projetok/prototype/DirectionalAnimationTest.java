package com.projetok.prototype;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;
import java.util.HexFormat;
import java.security.MessageDigest;
import java.awt.image.BufferedImage;
import java.awt.Color;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class DirectionalAnimationTest {
    @Test void variableDeltasFullCyclesAndLargeValuesRespectExistingContract() {
        var animation=new DirectionalAnimation();visual(animation,100,100,0);
        visual(animation,101,100,.02);
        assertEquals(0,visual(animation,102,100,.03).frame());
        assertEquals(1,visual(animation,103,100,.07).frame());
        for(int i=0;i<30;i++) {
            var value=visual(animation,104+i,100,.1);
            assertTrue(value.frame()>=0 && value.frame()<4);
            assertEquals(DirectionalAnimation.State.WALK,value.state());
        }
        for(double dt:new double[]{-.1,.10001,1,Double.POSITIVE_INFINITY,Double.NaN})
            assertThrows(IllegalArgumentException.class,()->visual(animation,150,100,dt));
    }
    @Test void attackWhileWalkingHitThenDeathAndDirectionTransition() {
        var animation=new DirectionalAnimation();visual(animation,100,100,0);
        var attack=new DirectionalAnimation.Signal(1,DirectionalAnimation.State.ATTACK);
        var hit=new DirectionalAnimation.Signal(1,DirectionalAnimation.State.HIT);
        var a=animation.advance(snapshot(101,100,true),List.of(attack),.03).animations().get(0);
        assertEquals(DirectionalAnimation.State.ATTACK,a.state());assertEquals(DirectionalAnimation.Direction.RIGHT,a.direction());
        assertEquals(1,animation.advance(snapshot(102,100,true),List.of(),.1).animations().get(0).frame());
        a=animation.advance(snapshot(102,99,true),List.of(hit,attack),.03).animations().get(0);
        assertEquals(DirectionalAnimation.State.HIT,a.state());assertEquals(0,a.frame());
        a=animation.advance(snapshot(102,98,false),List.of(attack,hit),.03).animations().get(0);
        assertEquals(DirectionalAnimation.State.FAINT,a.state());assertEquals(DirectionalAnimation.Direction.FRONT,a.direction());
        animation.reset();assertEquals(DirectionalAnimation.State.IDLE,visual(animation,100,100,0).state());
    }
    @TempDir Path temp;
    private final DebugSnapshot base=DebugWorld.demo(new java.util.Random(12)).snapshot();

    private DebugSnapshot snapshot(float x,float y,boolean alive) {
        var a=new DebugSnapshot.ActorView(1,"fixture",x,y,18,alive?100:0,100,false,alive);
        return new DebugSnapshot(List.of(a),base.obstacles(),List.of(),100,100,0,false,1,"",1,0,0,null,
                100,100,0,List.of(),List.of(),false,0,0,List.of(),false,base.softObstacles(),base.cryostasisHud(),base.lightningStepHud());
    }
    private DirectionalAnimation.View visual(DirectionalAnimation a, float x,float y,double dt) {
        return a.advance(snapshot(x,y,true),List.of(),dt).animations().get(0);
    }
    @Test void fourDirectionsAndStableFacing() {
        var a=new DirectionalAnimation();
        visual(a,100,100,0);
        assertEquals(DirectionalAnimation.Direction.RIGHT,visual(a,101,100,.1).direction());
        assertEquals(DirectionalAnimation.Direction.LEFT,visual(a,100,100,.1).direction());
        assertEquals(DirectionalAnimation.Direction.BACK,visual(a,100,99,.1).direction());
        assertEquals(DirectionalAnimation.Direction.FRONT,visual(a,100,100,.1).direction());
        assertEquals(DirectionalAnimation.Direction.FRONT,visual(a,100,100,.1).direction());
    }
    @Test void timingLoopPauseReturnAndReset() {
        var a=new DirectionalAnimation();
        visual(a,100,100,0);
        assertEquals(0,visual(a,101,100,.1).frame());
        assertEquals(1,visual(a,102,100,.1).frame());
        assertEquals(2,visual(a,103,100,.1).frame());
        assertEquals(3,visual(a,104,100,.1).frame());
        assertEquals(0,visual(a,105,100,.1).frame());
        assertEquals(DirectionalAnimation.State.IDLE,visual(a,105,100,.1).state());
        a.reset();
        assertEquals(DirectionalAnimation.Direction.FRONT,visual(a,100,100,0).direction());
        assertThrows(IllegalArgumentException.class,()->visual(a,100,100,Double.NaN));
    }
    @Test void attackHitDeathPriorityAndClamp() {
        var a=new DirectionalAnimation();
        var attack=new DirectionalAnimation.Signal(1,DirectionalAnimation.State.ATTACK);
        var hit=new DirectionalAnimation.Signal(1,DirectionalAnimation.State.HIT);
        assertEquals(DirectionalAnimation.State.ATTACK,a.advance(snapshot(100,100,true),List.of(attack),.1).animations().get(0).state());
        assertEquals(1,a.advance(snapshot(100,100,true),List.of(),.1).animations().get(0).frame());
        assertEquals(DirectionalAnimation.State.HIT,a.advance(snapshot(100,100,true),List.of(hit,attack),.1).animations().get(0).state());
        assertEquals(0,a.advance(snapshot(100,100,true),List.of(),0).animations().get(0).frame());
        a.advance(snapshot(100,100,true),List.of(),.1);
        assertEquals(DirectionalAnimation.State.IDLE,a.advance(snapshot(100,100,true),List.of(),.1).animations().get(0).state());
        for(int i=0;i<20;i++) a.advance(snapshot(100,100,false),List.of(),.1);
        var ko=a.advance(snapshot(100,100,false),List.of(),.1).animations().get(0);
        assertEquals(DirectionalAnimation.State.FAINT,ko.state());
        assertEquals(2,ko.frame());
        assertEquals(DirectionalAnimation.Direction.FRONT,ko.direction());
    }
    @Test void realRuntimeAttackRestartAndNoInventedRun() {
        try(var runtime=new DebugRuntime(()->DebugWorld.demo(new java.util.Random(2)))) {
            runtime.setInput(new DebugInput(false,false,false,false,true));
            runtime.advanceOneTick();
            assertEquals(DirectionalAnimation.State.ATTACK,runtime.presentation().animations().get(0).state());
            runtime.setInput(DebugInput.NONE);
            runtime.restart();runtime.advanceOneTick();
            assertEquals(DirectionalAnimation.State.IDLE,runtime.presentation().animations().get(0).state());
            assertEquals(135,runtime.snapshot().actors().get(0).x());
            assertTrue(runtime.presentation().animations().stream().noneMatch(v->v.state()==DirectionalAnimation.State.RUN));
        }
    }
    @Test void realDamageAndDeathDriveVisuals() {
        var world=DebugWorld.demo(new java.util.Random(3));var animations=new DirectionalAnimation();
        boolean hit=false,death=false;
        for(int i=0;i<5000 && !death;i++) {
            var s=world.snapshot();var p=s.actors().get(0);var mob=s.actors().get(1);
            float dx=mob.x()-p.x(),dy=mob.y()-p.y();
            world.tick(new DebugInput(dy<-4,dy>4,dx<-4,dx>4,false),1f/30f);
            var signals=world.animationSignals();
            var v=animations.advance(world.snapshot(),signals,1d/30).animations().get(0);
            if(signals.stream().anyMatch(e->e.actorId()==1 && e.state()==DirectionalAnimation.State.HIT)) {
                hit=true;assertTrue(v.state()==DirectionalAnimation.State.HIT || v.state()==DirectionalAnimation.State.FAINT);
            }
            if(world.snapshot().gameOver()) {death=true;assertEquals(DirectionalAnimation.State.FAINT,v.state());}
        }
        assertTrue(hit);assertTrue(death);
    }
    @Test void frozenActorHoldsFrameThenResumes() {
        var a=new DirectionalAnimation();var s=snapshot(100,100,true);
        var frozen=new DebugSnapshot(s.actors(),s.obstacles(),s.effects(),100,100,0,false,1,"",1,0,0,null,
                100,100,0,List.of(),List.of(new DebugSnapshot.StatusView(1,1)),false,0,0,List.of(),false,
                s.softObstacles(),s.cryostasisHud(),s.lightningStepHud());
        a.advance(s,List.of(new DirectionalAnimation.Signal(1,DirectionalAnimation.State.HIT)),.1);
        assertEquals(0,a.advance(frozen,List.of(),.1).animations().get(0).frame());
        assertEquals(0,a.advance(frozen,List.of(),.1).animations().get(0).frame());
        assertEquals(1,a.advance(s,List.of(),.1).animations().get(0).frame());
    }
    private Path atlas() throws Exception {
        var image=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();g.setColor(Color.MAGENTA);g.fillRect(20,10,24,40);g.dispose();
        Path png=temp.resolve("synthetic.png");ImageIO.write(image,"png",png.toFile());
        Properties p=new Properties();p.setProperty("status","VALIDADO");p.setProperty("image","synthetic.png");
        p.setProperty("sha256",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(png))));
        p.setProperty("pivotX","32");p.setProperty("pivotY","48");p.setProperty("offsetX","0");p.setProperty("offsetY","0");
        p.setProperty("IDLE.FRONT.0","0,0,64,64");
        Path manifest=temp.resolve("synthetic.properties");try(var out=Files.newBufferedWriter(manifest)){p.store(out,"FIXTURE SINTETICA");}
        return manifest;
    }
    @Test void syntheticAtlasPivotFallbackAndNoMutation() throws Exception {
        var atlas=ValidatedSpriteAtlas.load(atlas());
        var a=new DirectionalAnimation();var presentation=a.advance(snapshot(100,100,true),List.of(),0);
        var image=new BufferedImage(DebugPanel.PANEL_WIDTH,DebugPanel.PANEL_HEIGHT,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();
        assertTrue(atlas.draw(g,presentation.world().actors().get(0),presentation.animations().get(0)));
        assertEquals(Color.MAGENTA.getRGB(),image.getRGB(88,62));
        assertFalse(atlas.draw(g,presentation.world().actors().get(0),new DirectionalAnimation.View(1,DirectionalAnimation.State.HIT,DirectionalAnimation.Direction.FRONT,0)));
        var before=presentation.toString();var panel=new DebugPanel(()->presentation.world());
        panel.setPresentation(()->presentation,atlas);panel.render(g,presentation);panel.render(g,presentation);
        assertEquals(before,presentation.toString());g.dispose();
        Path capture=Path.of("../docs/TASK-014-sintetico.png");ImageIO.write(image,"png",capture.toFile());
    }
    @Test void rejectsUnapprovedAndAlteredAsset() throws Exception {
        Path p=atlas();Files.writeString(p,Files.readString(p).replace("VALIDADO","PENDENTE"));
        Path unapproved=p;assertThrows(java.io.IOException.class,()->ValidatedSpriteAtlas.load(unapproved));
        p=atlas();Files.write(temp.resolve("synthetic.png"),new byte[]{1,2,3});
        Path finalPath=p;assertThrows(java.io.IOException.class,()->ValidatedSpriteAtlas.load(finalPath));
    }
}
