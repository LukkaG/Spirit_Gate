package com.projetok.prototype.rpg;

import com.projetok.prototype.*;
import com.projetok.prototype.DirectionalAnimation.Direction;
import com.projetok.rpg.RpgWorld;
import org.junit.jupiter.api.Test;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class RpgPlayerRefinementTest {
    private final Path assets=Path.of("../assets/dev/player-base");
    private BufferedImage draw(RpgPlayerSprites sprites) {
        var image=new BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
        assertTrue(sprites.draw(g,new DebugSnapshot.ActorView(1,"Test",128,220,18,10,10,true,true)));
        g.dispose();return image;
    }
    private int[] bounds(BufferedImage image) {
        int left=image.getWidth(),top=image.getHeight(),right=-1,bottom=-1;
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)if((image.getRGB(x,y)>>>24)!=0){
            left=Math.min(left,x);top=Math.min(top,y);right=Math.max(right,x);bottom=Math.max(bottom,y);
            assertEquals(255,image.getRGB(x,y)>>>24,"nearest-neighbor preserves binary alpha");
        }
        return new int[]{left,top,right,bottom};
    }
    @Test void configuredDefaultsAreFiftyPercentAnd250Milliseconds() throws Exception {
        String enabled=System.getProperty(RpgPlayerSprites.ENABLED),scale=System.getProperty(RpgPlayerSprites.SCALE),
                duration=System.getProperty(RpgPlayerSprites.FRAME_MILLIS),exe=System.getProperty("jpackage.app-path");
        try {
            System.setProperty(RpgPlayerSprites.ENABLED,"true");System.clearProperty(RpgPlayerSprites.SCALE);
            System.clearProperty(RpgPlayerSprites.FRAME_MILLIS);
            System.setProperty("jpackage.app-path",Path.of("../isolated.exe").toAbsolutePath().toString());
            var sprites=RpgPlayerSprites.configured();assertNotNull(sprites);assertEquals(.5,sprites.scale());
            assertEquals(250,RpgPlayerSprites.DEFAULT_FRAME_MILLIS);
            System.setProperty(RpgPlayerSprites.SCALE,"0.4");System.setProperty(RpgPlayerSprites.FRAME_MILLIS,"150");
            assertEquals(.4,RpgPlayerSprites.configured().scale());
        } finally {
            restore(RpgPlayerSprites.ENABLED,enabled);restore(RpgPlayerSprites.SCALE,scale);
            restore(RpgPlayerSprites.FRAME_MILLIS,duration);restore("jpackage.app-path",exe);
        }
    }
    private void restore(String key,String value){if(value==null)System.clearProperty(key);else System.setProperty(key,value);}
    @Test void invalidScalesAndDurationsFallBackWithoutAffectingSimulation() {
        for(double value:new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,-.4,0,.09,1.01})
            assertNull(RpgPlayerSprites.loadOptional(assets,220,value));
        assertNull(RpgPlayerSprites.loadOptional(assets,0,.4));
        assertNull(RpgPlayerSprites.loadOptional(assets,2001,.4));
        String enabled=System.getProperty(RpgPlayerSprites.ENABLED),scale=System.getProperty(RpgPlayerSprites.SCALE),duration=System.getProperty(RpgPlayerSprites.FRAME_MILLIS);
        try {
            System.setProperty(RpgPlayerSprites.ENABLED,"true");System.setProperty(RpgPlayerSprites.SCALE,"invalid");
            assertNull(RpgPlayerSprites.configured());
            System.setProperty(RpgPlayerSprites.SCALE,"0.4");System.setProperty(RpgPlayerSprites.FRAME_MILLIS,"invalid");
            assertNull(RpgPlayerSprites.configured());
            System.setProperty(RpgPlayerSprites.ENABLED,"false");assertNull(RpgPlayerSprites.configured());
        } finally {restore(RpgPlayerSprites.ENABLED,enabled);restore(RpgPlayerSprites.SCALE,scale);restore(RpgPlayerSprites.FRAME_MILLIS,duration);}
    }
    @Test void allTwelveFramesKeepFootAnchorAndAvoidClippingAtAllComparisonScales() throws Exception {
        for(double scale:new double[]{.35,.4,.5})for(Direction direction:Direction.values()) {
            var sprites=RpgPlayerSprites.load(assets,220,scale);var world=new RpgWorld();sprites.update(world.snapshot());
            int x=direction==Direction.LEFT?-1:direction==Direction.RIGHT?1:0;
            int y=direction==Direction.BACK?-1:direction==Direction.FRONT?1:0;
            for(int state=0;state<3;state++) {
                world.tick(x,y);sprites.update(world.snapshot());
                if(state==1)for(int i=0;i<7;i++){world.tick(x,y);sprites.update(world.snapshot());}
                if(state==2){world.tick(0,0);sprites.update(world.snapshot());}
                assertEquals(direction+"."+(state==0?1:state==1?2:0),sprites.frameKey());
                var image=draw(sprites);int[] b=bounds(image);
                assertTrue(b[3]>=218&&b[3]<=220,"feet "+sprites.frameKey()+" at scale "+scale);
                assertTrue(Math.abs((b[0]+b[2])/2.0-128)<=1.5,"horizontal pivot");
                assertTrue(b[0]>0&&b[1]>0&&b[2]<255&&b[3]<255,"no clipping");
                assertEquals(0,image.getRGB(0,0));assertEquals(0,image.getRGB(128,224));
            }
        }
    }
    @Test void reducedScaleChangesBothAxesProportionally() throws Exception {
        var previous=bounds(draw(RpgPlayerSprites.load(assets,150,.5)));
        for(double scale:new double[]{.35,.4}) {
            int[] actual=bounds(draw(RpgPlayerSprites.load(assets,220,scale)));
            double ratio=scale/.5;
            assertEquals((previous[2]-previous[0]+1)*ratio,actual[2]-actual[0]+1,2);
            assertEquals((previous[3]-previous[1]+1)*ratio,actual[3]-actual[1]+1,2);
        }
    }
    @Test void slowerDurationsUseSimulationTicksAndKeepIdleStable() {
        for(int duration:new int[]{220,280}) {
            var animation=new RpgPlayerAnimation(duration);var world=new RpgWorld();animation.update(world.snapshot());
            int threshold=duration==220?8:10;
            for(int tick=1;tick<threshold;tick++){world.tick(1,0);assertEquals(0,animation.update(world.snapshot()).frame());}
            world.tick(1,0);assertEquals(1,animation.update(world.snapshot()).frame());
            var snapshot=world.snapshot();for(int paint=0;paint<100;paint++)assertEquals(1,animation.update(snapshot).frame());
            world.tick(0,-1);assertEquals(new RpgPlayerAnimation.Pose(Direction.BACK,true,0),animation.update(world.snapshot()));
            for(int tick=0;tick<20;tick++){world.tick(0,0);assertEquals(new RpgPlayerAnimation.Pose(Direction.BACK,false,0),animation.update(world.snapshot()));}
        }
        assertEquals(.4,DirectionalAnimation.duration(DirectionalAnimation.State.WALK));
    }
    @Test void allComparisonSettingsPreserveWorldStateAndGeometryFallback() throws Exception {
        for(double[] setting:new double[][]{{.5,150},{.4,220},{.35,220},{.4,280}}){
            var world=new RpgWorld();var control=RpgWorld.restore(world.save());
            var sprites=RpgPlayerSprites.load(assets,(int)setting[1],setting[0]);
            for(int tick=0;tick<60;tick++){
                world.tick(0,tick<30?1:-1);control.tick(0,tick<30?1:-1);
                sprites.update(world.snapshot());draw(sprites);assertEquals(control.save(),world.save());
            }
            var g=new BufferedImage(50,50,2).createGraphics();
            assertFalse(sprites.draw(g,new DebugSnapshot.ActorView(100,"NPC",25,25,18,10,10,false,true)));
            assertFalse(sprites.draw(g,new DebugSnapshot.ActorView(1,"Dead",25,25,18,0,10,true,false)));g.dispose();
        }
    }
}
