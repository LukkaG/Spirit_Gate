package com.projetok.multiplayer;

import com.projetok.prototype.DebugPanel;
import com.projetok.prototype.DebugWorld;
import com.projetok.rpg.RpgSkillCatalog;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Verifica a arte efetivamente importada, direção, fases e prioridade no painel real do jogo. */
class ReaSkillAssetsTest {
    private static final Path ROOT=Path.of("..").toAbsolutePath().normalize();
    private static Map<String,NetworkSkillPresentation.Clip> clips;
    @TempDir Path temporary;
    @BeforeAll static void load()throws Exception{clips=NetworkSkillPresentation.load(ROOT);}

    @Test void everySkillUsesTheNewPackAndAllPhasesKeepSourcePixels()throws Exception{
        assertEquals(219,clips.keySet().stream().filter(id->id.contains(".rea.")).count());
        for(var definition:RpgSkillCatalog.all())assertNotNull(clips.get(definition.id()).animation(),definition.id());
        for(var entry:clips.entrySet())if(entry.getKey().contains(".rea.")){
            assertFalse(entry.getKey().matches(".*\\.(body_action|body_combo|body_stomp|attacker|target)$"));
            var a=entry.getValue().animation();
            assertTrue(a.rows()==1||a.rows()==8);
            assertEquals(a.cell()*a.durations().length,a.image().getWidth());
            assertEquals(a.cell()*a.rows(),a.image().getHeight());
            assertTrue(a.image().getColorModel().hasAlpha());
        }
        assertEquals(8,clips.get("fire_5.rea.wave_1").frames(),"Fogo 5 deve usar a revisão 3");
        assertEquals(128,clips.get("martial_1.rea.contact").frameWidth(),"Impacto marcial deve usar a revisão 3");
    }
    @Test void allEightDirectionsUseExactRowsWithoutRotatingOrFlipping(){
        var a=clips.get("water_1.rea.flight").animation();int cell=a.cell(),frame=3;
        for(int row=0;row<8;row++){
            var output=new BufferedImage(cell,cell,BufferedImage.TYPE_INT_ARGB);var g=output.createGraphics();
            try{a.draw(g,frame,a.pivotX(),a.pivotY(),(row-2)*Math.PI/4);}finally{g.dispose();}
            for(int y=0;y<cell;y++)for(int x=0;x<cell;x++)assertEquals(a.image().getRGB(frame*cell+x,row*cell+y),output.getRGB(x,y),"Linha "+row);
        }
    }
    @Test void variableTimingAndOneShotEndKeepTheirSourceOrder(){
        var a=clips.get("fire_5.rea.wave_1").animation();long elapsed=0;
        int[] times=a.durations();assertTrue(Arrays.stream(times).distinct().count()>1);
        for(int i=0;i<times.length;i++){assertEquals(i,a.frame(elapsed,false));assertEquals(i,a.frame(elapsed+times[i]-1,false));elapsed+=times[i];}
        assertEquals(times.length-1,a.frame(elapsed+1000,false),"Uma onda não reinicia na formação");
        var loop=clips.get("water_4.rea.vortex_loop").animation();assertEquals(0,loop.frame(loop.duration(),false));
    }
    @Test void renderingEverySkillInEveryDirectionFinishesWithoutInvalidCrops()throws Exception{
        Path evidence=Files.createDirectories(ROOT.resolve("docs/skills-preview-rea"));
        var contact=new BufferedImage(1280,8*220,BufferedImage.TYPE_INT_RGB);var sheet=contact.createGraphics();
        sheet.setColor(new Color(20,34,48));sheet.fillRect(0,0,contact.getWidth(),contact.getHeight());
        int index=0;
        try{for(var definition:RpgSkillCatalog.all()){
            String id=definition.id();
            for(int row=0;row<8;row++){
                double angle=(row-2)*Math.PI/4;var actor=new Protocol.Actor(1,"Herói",300,300,100,100,100,0,false,(float)Math.cos(angle),(float)Math.sin(angle),0);
                var actors=Map.of(1,actor);
                for(int ms:new int[]{0,100,250,500,1000,1800}){
                    var event=new SkillProtocol.Event(1,0,1,id,300,300,300,300);
                    var output=new BufferedImage(600,600,BufferedImage.TYPE_INT_ARGB);var g=output.createGraphics();
                    try{assertTrue(ReaSkillAssets.draw(clips,g,event,ms,2000,actors,angle,false,false));}
                    finally{g.dispose();}
                    if(row==2&&ms==250){
                        int x=index%5*256,y=index/5*220;sheet.drawImage(output,x+128-300,y+120-300,null);
                        sheet.setColor(Color.WHITE);sheet.drawString(id,x+10,y+15);
                    }
                }
            }
            index++;
        }}finally{sheet.dispose();}
        ImageIO.write(contact,"png",evidence.resolve("runtime-40-skills.png").toFile());
    }
    @Test void actualPanelPaintsSkillsAfterAllActors(){
        var world=DebugWorld.demo();var order=new ArrayList<String>();
        var panel=new DebugPanel(world::snapshot);panel.setSize(1260,750);
        panel.setActorRenderer((g,actor)->{order.add("actor");return true;});
        panel.setWorldOverlay(g->{order.add("skill");clips.get("water_2.rea.cast").animation().draw(g,4,300,300,0);});
        var output=new BufferedImage(1260,750,BufferedImage.TYPE_INT_ARGB);var g=output.createGraphics();
        try{panel.paint(g);}finally{g.dispose();}
        assertTrue(order.contains("actor"));assertEquals("skill",order.get(order.size()-1));
    }
    @Test void opaqueSkillPixelsCoverTheHeroAndTransparentPixelsPreserveIt(){
        var a=clips.get("water_2.rea.cast").animation();int cell=a.cell();
        var output=new BufferedImage(cell,cell,BufferedImage.TYPE_INT_ARGB);var g=output.createGraphics();
        try{g.setColor(Color.MAGENTA);g.fillRect(0,0,cell,cell);a.draw(g,4,a.pivotX(),a.pivotY(),0);}finally{g.dispose();}
        int visible=0,empty=0;
        for(int y=0;y<cell;y++)for(int x=0;x<cell;x++){
            int pixel=a.image().getRGB(4*cell+x,2*cell+y);
            if(pixel>>>24==255){assertEquals(pixel,output.getRGB(x,y));visible++;}
            if(pixel>>>24==0){assertEquals(Color.MAGENTA.getRGB(),output.getRGB(x,y));empty++;}
        }
        assertTrue(visible>0&&empty>0);
    }
    @Test void projectileImpactKeepsItsLaunchRowWhenCasterTurns(){
        var clock=new AtomicLong();var view=new NetworkSkillPresentation(clock::get);view.clips(clips);
        view.actors(new Protocol.State(0,List.of(new Protocol.Actor(1,"Herói",300,300,100,100,100,0,false,0,1,0))));
        var launch=new SkillProtocol.Event(1,0,1,"water_1",300,300,300,100);
        view.update(state(0,launch));clock.set(100);
        var impact=new SkillProtocol.Event(2,3,1,"water_1",300,240,300,240);view.update(state(3,launch,impact));clock.set(180);
        var actual=new BufferedImage(600,600,BufferedImage.TYPE_INT_ARGB);var g=actual.createGraphics();try{view.draw(g);}finally{g.dispose();}
        var expected=new BufferedImage(600,600,BufferedImage.TYPE_INT_ARGB);g=expected.createGraphics();var a=clips.get("water_1.rea.impact").animation();
        try{a.draw(g,a.frame(80,false),300,212,-Math.PI/2);}finally{g.dispose();}
        assertArrayEquals(expected.getRGB(0,0,600,600,null,0,600),actual.getRGB(0,0,600,600,null,0,600));
    }
    @Test void nativeRainIsOneAreaCompositeInsteadOfFiveCopies(){
        var event=new SkillProtocol.Event(1,0,1,"aetheri_3",300,140,300,300);
        var actual=new BufferedImage(600,600,BufferedImage.TYPE_INT_ARGB);var g=actual.createGraphics();
        try{ReaSkillAssets.draw(clips,g,event,200,400,Map.of(),0,false,false);}finally{g.dispose();}
        var a=clips.get("aetheri_3.rea.wave_1").animation();var expected=new BufferedImage(600,600,BufferedImage.TYPE_INT_ARGB);g=expected.createGraphics();
        try{a.draw(g,a.frame(95,false),300,300,0);}finally{g.dispose();}
        assertArrayEquals(expected.getRGB(0,0,600,600,null,0,600),actual.getRGB(0,0,600,600,null,0,600));
    }
    @Test void optionalPackCanBeAbsentAndExternalPathsAreRejected()throws Exception{
        assertTrue(ReaSkillAssets.load(temporary).isEmpty());
        var folder=Files.createDirectories(temporary.resolve("assets/skills/rea-v1"));
        Files.writeString(folder.resolve("manifest.properties"),"water_1.rea.flight.path=../escape.png\n");
        assertThrows(java.io.IOException.class,()->ReaSkillAssets.load(temporary));
        Files.writeString(folder.resolve("manifest.properties"),"water_1.rea.flight.path=x\nwater_1.rea.flight.path=y\n");
        assertThrows(java.io.IOException.class,()->ReaSkillAssets.load(temporary));
    }
    @Test void everySkillAcceptsLaunchImpactReplayAndCleanupThroughTheRealPresenter(){
        for(var definition:RpgSkillCatalog.all()){
            var clock=new AtomicLong();var view=new NetworkSkillPresentation(clock::get);view.clips(clips);
            view.actors(new Protocol.State(0,List.of(new Protocol.Actor(1,"Herói",300,300,100,100,100,0,false))));
            String id=definition.id();var launch=new SkillProtocol.Event(1,0,1,id,300,300,300,200);
            var hit=new SkillProtocol.Event(2,3,1,id,300,246,300,246);
            view.update(state(0,launch));clock.set(100);view.update(state(3,launch,hit));
            for(int at:new int[]{100,150,400,900,1900,6000}){
                clock.set(at);var output=new BufferedImage(600,600,BufferedImage.TYPE_INT_ARGB);var g=output.createGraphics();
                try{view.draw(g);}finally{g.dispose();}
            }
            view.update(null);assertEquals(0,view.activeCount(),id);
        }
    }
    @Test void firstRainWaveDoesNotDependOnTheGlobalTickOrOtherPlayersEvents(){
        var time=new AtomicLong();var view=new NetworkSkillPresentation(time::get);view.clips(clips);
        var launch=new SkillProtocol.Event(451,137,1,"aetheri_3",300,140,300,300);
        view.update(state(137,launch));time.set(200);
        var actual=new BufferedImage(600,600,BufferedImage.TYPE_INT_ARGB);var g=actual.createGraphics();try{view.draw(g);}finally{g.dispose();}
        var expected=new BufferedImage(600,600,BufferedImage.TYPE_INT_ARGB);g=expected.createGraphics();
        try{ReaSkillAssets.draw(clips,g,launch,200,400,Map.of(),0,false,false,1);}finally{g.dispose();}
        assertArrayEquals(expected.getRGB(0,0,600,600,null,0,600),actual.getRGB(0,0,600,600,null,0,600));
        var hit=new SkillProtocol.Event(452,149,1,"aetheri_3",300,300,300,300);
        var second=new SkillProtocol.Event(453,149,1,"aetheri_3",300,140,300,300);
        time.set(400);view.update(state(149,launch,hit,second));time.set(600);
        actual=new BufferedImage(600,600,BufferedImage.TYPE_INT_ARGB);g=actual.createGraphics();try{view.draw(g);}finally{g.dispose();}
        expected=new BufferedImage(600,600,BufferedImage.TYPE_INT_ARGB);g=expected.createGraphics();
        try{
            ReaSkillAssets.draw(clips,g,hit,200,480,Map.of(),0,false,false,1);
            ReaSkillAssets.draw(clips,g,second,200,400,Map.of(),0,false,false,2);
        }finally{g.dispose();}
        assertArrayEquals(expected.getRGB(0,0,600,600,null,0,600),actual.getRGB(0,0,600,600,null,0,600));
    }
    @Test void eruptionIncludesChargePeakAndFadeInThisOrder(){
        var event=new SkillProtocol.Event(1,0,1,"fire_4",300,300,300,300);
        int duration=ReaSkillAssets.duration(clips,event,400,true),at=0;
        for(String stage:List.of("eruption_charge","eruption","eruption_fade")){
            var animation=clips.get("fire_4.rea."+stage).animation();
            var actual=new BufferedImage(600,600,BufferedImage.TYPE_INT_ARGB);var g=actual.createGraphics();
            try{ReaSkillAssets.draw(clips,g,event,at,duration,Map.of(),0,false,true);}finally{g.dispose();}
            var expected=new BufferedImage(600,600,BufferedImage.TYPE_INT_ARGB);g=expected.createGraphics();
            try{animation.draw(g,0,300,300,0);}finally{g.dispose();}
            assertArrayEquals(expected.getRGB(0,0,600,600,null,0,600),actual.getRGB(0,0,600,600,null,0,600),stage);
            at+=animation.duration();
        }
        assertEquals(at,duration);
    }
    /** Monta um snapshot sintético de apresentação sem tocar no executor do servidor. */
    private static SkillProtocol.State state(long tick,SkillProtocol.Event... events){
        return new SkillProtocol.State("a".repeat(64),tick,List.of(events),List.of(0,0,0,0,0),0,false);
    }
}
