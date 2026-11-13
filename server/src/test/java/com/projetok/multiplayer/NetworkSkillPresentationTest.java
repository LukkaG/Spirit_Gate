package com.projetok.multiplayer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Verifica tempo, deduplicação e integridade dos assets sem executar combate na apresentação. */
class NetworkSkillPresentationTest {
    private static final String HASH="a".repeat(64);
    @TempDir Path temporary;
    private SkillProtocol.Event event(long sequence,long tick,String id){return new SkillProtocol.Event(sequence,tick,1,id,140,300,140,300);}
    private SkillProtocol.State state(String hash,long tick,SkillProtocol.Event... events){return new SkillProtocol.State(hash,tick,List.of(events),List.of(0,0,0,0,0),0,false);}

    @Test void repeatedSnapshotsNeverRestartEffectsAndPortalsClearTheOldRegion(){
        var time=new AtomicLong();var view=new NetworkSkillPresentation(time::get);
        var event=event(1,10,"water_4");var snapshot=state(HASH,10,event);
        view.update(snapshot);view.update(snapshot);assertEquals(1,view.activeCount());
        time.set(1500);assertEquals(0,view.activeCount());view.update(snapshot);assertEquals(0,view.activeCount());
        view.update(state(HASH,30,event(2,30,"water_4")));assertEquals(1,view.activeCount());
        view.region("village");assertEquals(0,view.activeCount());view.update(state(HASH,30,event(2,30,"water_4")));assertEquals(0,view.activeCount());
        view.update(state("b".repeat(64),1,event(1,1,"water_4")));assertEquals(1,view.activeCount());
        view.update(null);assertEquals(0,view.activeCount());
    }
    @Test void prisonLastsTwoSecondsAndEventsTooOldToShowAreDiscarded(){
        var time=new AtomicLong();var view=new NetworkSkillPresentation(time::get);
        view.update(state(HASH,1,event(1,1,"water_3")));time.set(1999);assertEquals(1,view.activeCount());
        time.set(2000);assertEquals(0,view.activeCount());
        view.update(state(HASH,1000,event(2,1,"water_3")));assertEquals(0,view.activeCount());
    }
    @Test void selfCastFollowsAuthoritativePlayerPosition(){
        var pixels=new BufferedImage(8,8,BufferedImage.TYPE_INT_ARGB);for(int y=0;y<8;y++)for(int x=0;x<8;x++)pixels.setRGB(x,y,0xff00ffff);
        var view=new NetworkSkillPresentation(()->0L);view.clips(Map.of("water_2",new NetworkSkillPresentation.Clip(pixels,8,8,1,1,100,8,8,4,4)));
        view.update(state(HASH,1,event(1,1,"water_2")));
        view.actors(new Protocol.State(1,List.of(new Protocol.Actor(1,"Jogador",300,400,100,100,1000,0,false))));
        var output=new BufferedImage(500,500,BufferedImage.TYPE_INT_ARGB);var graphics=output.createGraphics();try{view.draw(graphics);}finally{graphics.dispose();}
        assertEquals(0xff00ffff,output.getRGB(300,372));assertEquals(0,output.getRGB(140,272));
    }
    @Test void allFortyLocalClipsHaveUsableFramesAndAlpha()throws Exception {
        Path root=Path.of("..").toAbsolutePath().normalize();var clips=NetworkSkillPresentation.load(root);assertEquals(40,clips.keySet().stream().filter(id->!id.contains(".")).count());
        for(var definition:com.projetok.rpg.RpgSkillCatalog.all()){
            var clip=clips.get(definition.id());assertNotNull(clip,definition.id());assertTrue(clip.image().getColorModel().hasAlpha(),definition.id());
            int visible=0,transparent=0;for(int y=0;y<clip.image().getHeight();y+=2)for(int x=0;x<clip.image().getWidth();x+=2){if(clip.image().getRGB(x,y)>>>24==0)transparent++;else visible++;}
            assertTrue(visible>0&&transparent>0,definition.id());
        }
    }
    @Test void externalPathsAndDuplicateMetadataAreRejected()throws Exception {
        var folder=Files.createDirectories(temporary.resolve("assets/skills"));var manifest=folder.resolve("manifest.properties");
        Files.writeString(manifest,"water_1.path=../outside.png\n");assertThrows(IOException.class,()->NetworkSkillPresentation.load(temporary));
        Files.writeString(manifest,"water_1.path=assets/skills/a.png\nwater_1.path=assets/skills/b.png\n");assertThrows(IOException.class,()->NetworkSkillPresentation.load(temporary));
    }
    @Test void lineProjectileStartsAtCasterAndTravelsBeforeImpact(){
        var pixels=new BufferedImage(8,8,BufferedImage.TYPE_INT_ARGB);for(int y=0;y<8;y++)for(int x=0;x<8;x++)pixels.setRGB(x,y,0xff00ffff);
        var time=new AtomicLong();var view=new NetworkSkillPresentation(time::get);view.clips(Map.of("aetheri_4",new NetworkSkillPresentation.Clip(pixels,8,8,1,1,100,8,8,4,4)));
        var launch=new SkillProtocol.Event(1,0,1,"aetheri_4",100,100,280,100);view.update(state(HASH,0,launch));
        var start=paint(view);assertEquals(0xff00ffff,start.getRGB(100,72));assertEquals(0,start.getRGB(280,72));
        time.set(167);var middle=paint(view);assertEquals(0xff00ffff,middle.getRGB(190,72));assertEquals(0,middle.getRGB(280,72));
        var impact=new SkillProtocol.Event(2,6,1,"aetheri_4",190,100,190,100);view.update(state(HASH,6,launch,impact));
        assertEquals(1,view.activeCount());assertEquals(0xff00ffff,paint(view).getRGB(190,72));
    }
    @Test void fissureGrowsFromCasterAndEruptionWaitsForConfirmedEndpoint(){
        var time=new AtomicLong();var view=new NetworkSkillPresentation(time::get);
        view.clips(Map.of("fire_4",solid(0xffff0000,8,4),"fire_4.travel",solid(0xff00ff00,8,4),"fire_4.ground",solid(0xff00ffff,200,0)));
        var launch=new SkillProtocol.Event(1,0,1,"fire_4",100,100,280,100);view.update(state(HASH,0,launch));
        time.set(167);var traveling=paint(view);assertEquals(0xff00ffff,traveling.getRGB(140,100));assertEquals(0,traveling.getRGB(280,100),"Erupção não pode aparecer no alvo antes do percurso");
        time.set(334);var impact=new SkillProtocol.Event(2,10,1,"fire_4",280,100,280,100);view.update(state(HASH,10,launch,impact));
        assertEquals(0xffff0000,paint(view).getRGB(280,100));
    }
    @Test void firstImpactStopsTheRemainingProjectile(){
        var time=new AtomicLong();var view=new NetworkSkillPresentation(time::get);view.clips(Map.of("thunder_2",solid(0xff00ffff,8,4)));
        var launch=new SkillProtocol.Event(1,0,1,"thunder_2",100,100,280,100);view.update(state(HASH,0,launch));
        time.set(167);var impact=new SkillProtocol.Event(2,5,1,"thunder_2",190,100,190,100);view.update(state(HASH,5,launch,impact));
        time.set(250);assertEquals(0,paint(view).getRGB(235,72),"A lança não deve continuar além da primeira vítima");
    }
    @Test void volleyImpactStopsItsOwnRayInsteadOfTheFirstRay(){
        var time=new AtomicLong();var view=new NetworkSkillPresentation(time::get);var clip=solid(0xff00ffff,8,4);
        view.clips(Map.of("aetheri_5",clip,"aetheri_5.impact",clip));
        var upper=new SkillProtocol.Event(1,0,1,"aetheri_5",100,100,280,60);var lower=new SkillProtocol.Event(2,0,1,"aetheri_5",100,100,280,140);
        view.update(state(HASH,0,upper,lower));time.set(167);
        var impact=new SkillProtocol.Event(3,5,1,"aetheri_5",190,120,190,120);view.update(state(HASH,5,upper,lower,impact));
        time.set(200);var output=paint(view);assertEquals(0xff00ffff,output.getRGB(205,49),"O raio superior precisa continuar após o impacto do inferior");assertEquals(0,output.getRGB(205,95));
    }
    @ParameterizedTest @ValueSource(strings={"aetheri_4","thunder_2"})
    void projectileImpactFallbackKeepsTheUpwardLaunchDirection(String skill){
        var bitmap=new BufferedImage(12,4,BufferedImage.TYPE_INT_ARGB);for(int y=0;y<4;y++)for(int x=0;x<12;x++)bitmap.setRGB(x,y,0xff00ffff);
        var clip=new NetworkSkillPresentation.Clip(bitmap,12,4,1,1,100,12,4,6,2);var time=new AtomicLong();var view=new NetworkSkillPresentation(time::get);view.clips(Map.of(skill,clip));
        var launch=new SkillProtocol.Event(1,0,1,skill,100,100,100,20);view.update(state(HASH,0,launch));
        time.set(100);var impact=new SkillProtocol.Event(2,3,1,skill,100,46,100,46);view.update(state(HASH,3,launch,impact));time.set(225);
        var output=paint(view);assertEquals(0xff00ffff,output.getRGB(100,13),"O impacto deve continuar apontando para cima");assertEquals(0,output.getRGB(105,18),"A flecha não deve voltar a apontar para a direita");
    }
    @ParameterizedTest @ValueSource(ints={0,1,2,3})
    void cardinalProjectilesStartAtTheCenterInsteadOfTheFeet(int direction){
        int[] dx={1,0,-1,0},dy={0,1,0,-1};var view=new NetworkSkillPresentation(()->0L);
        view.clips(Map.of("aetheri_4",solid(0xff00ffff,8,4)));
        var launch=new SkillProtocol.Event(1,0,1,"aetheri_4",250,250,250+dx[direction]*180,250+dy[direction]*180);
        view.update(state(HASH,0,launch));var output=paint(view);
        assertEquals(0xff00ffff,output.getRGB(250,222));assertEquals(0,output.getRGB(250,250));
    }
    private NetworkSkillPresentation.Clip solid(int color,int width,int pivot){var bitmap=new BufferedImage(width,8,BufferedImage.TYPE_INT_ARGB);for(int y=0;y<8;y++)for(int x=0;x<width;x++)bitmap.setRGB(x,y,color);return new NetworkSkillPresentation.Clip(bitmap,width,8,1,1,100,width,8,pivot,4);}
    @Test void contactReplacesTheProjectileWithAnImpactThatDoesNotLoop(){
        var time=new AtomicLong();var view=new NetworkSkillPresentation(time::get);
        view.clips(Map.of("aetheri_4",solid(0xff00ff00,8,4),"aetheri_4.impact",solid(0xffff0000,8,4)));
        var launch=new SkillProtocol.Event(1,0,1,"aetheri_4",100,100,280,100);view.update(state(HASH,0,launch));
        time.set(100);var hit=new SkillProtocol.Event(2,3,1,"aetheri_4",154,100,154,100);view.update(state(HASH,3,launch,hit));
        assertEquals(1,view.activeCount());assertEquals(0xffff0000,paint(view).getRGB(154,72));assertEquals(0,paint(view).getRGB(190,72));
        time.set(200);assertEquals(0,view.activeCount());view.update(state(HASH,3,launch,hit));assertEquals(0,view.activeCount());
    }
    @Test void rainFallsToTheSelectedGroundAndOnlyThenDisplaysDissipation(){
        var time=new AtomicLong();var view=new NetworkSkillPresentation(time::get);
        view.clips(Map.of("aetheri_3",solid(0xff00ff00,8,4),"aetheri_3.travel",solid(0xff00ff00,8,4),"aetheri_3.impact",solid(0xffff0000,8,4)));
        var launch=new SkillProtocol.Event(1,0,1,"aetheri_3",250,140,250,300);view.update(state(HASH,0,launch));
        assertEquals(0xff00ff00,paint(view).getRGB(250,140));assertEquals(0,paint(view).getRGB(250,300));
        time.set(200);assertEquals(0xff00ff00,paint(view).getRGB(250,220));
        time.set(400);var hit=new SkillProtocol.Event(2,12,1,"aetheri_3",250,300,250,300);view.update(state(HASH,12,launch,hit));
        assertEquals(0xffff0000,paint(view).getRGB(250,300));assertEquals(0,paint(view).getRGB(250,220));
        time.set(500);assertEquals(0,view.activeCount());
    }
    @Test void sentryArrowDoesNotInheritTheFourSecondToggleDuration(){
        var time=new AtomicLong();var view=new NetworkSkillPresentation(time::get);view.clips(Map.of("aetheri_2",solid(0xff00ffff,8,4)));
        view.update(state(HASH,0,new SkillProtocol.Event(1,0,1,"aetheri_2",100,100,280,100)));
        time.set(384);assertEquals(0,view.activeCount());
    }
    @Test void fissureSectionsOpenAtTheirOwnArrivalTimeInsteadOfAppearingAllAtOnce(){
        var bitmap=new BufferedImage(64,8,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<8;y++)for(int x=0;x<64;x++)bitmap.setRGB(x,y,x<32?0xffff0000:0xff00ff00);
        var ground=new NetworkSkillPresentation.Clip(bitmap,32,8,2,2,100,32,8,0,4);
        var time=new AtomicLong();var view=new NetworkSkillPresentation(time::get);view.clips(Map.of("fire_4",solid(0xff0000ff,8,4),"fire_4.ground",ground));
        var launch=new SkillProtocol.Event(1,0,1,"fire_4",100,100,280,100);view.update(state(HASH,0,launch));time.set(167);
        var image=paint(view);assertEquals(0xff00ff00,image.getRGB(110,100));assertEquals(0xffff0000,image.getRGB(160,100));assertEquals(0,image.getRGB(210,100));
    }
    @ParameterizedTest @ValueSource(strings={"fire_3","water_4"})
    void vortexUsesItsWholeLifetimeWithoutLoopingOrRestartingFromRepeatedSnapshots(String skill){
        var bitmap=new BufferedImage(96,8,BufferedImage.TYPE_INT_ARGB);
        for(int frame=0;frame<12;frame++)for(int y=0;y<8;y++)for(int x=0;x<8;x++)bitmap.setRGB(frame*8+x,y,0xff000000|frame+1);
        var clip=new NetworkSkillPresentation.Clip(bitmap,8,8,12,12,30,8,8,4,4);
        var time=new AtomicLong();var view=new NetworkSkillPresentation(time::get);view.clips(Map.of(skill,clip));
        var snapshot=state(HASH,0,event(1,0,skill));view.update(snapshot);
        assertEquals(0xff000001,paint(view).getRGB(140,300));
        time.set(666);view.update(snapshot);assertEquals(0xff000006,paint(view).getRGB(140,300));
        time.set(1222);assertEquals(0xff00000c,paint(view).getRGB(140,300));
        time.set(1333);assertEquals(0,view.activeCount());assertEquals(0,paint(view).getRGB(140,300));
    }
    @ParameterizedTest @ValueSource(strings={"fire_4","fire_3","water_4"})
    void shippedLifecycleStartsSmallPeaksAndDissipatesOnTheGround(String skill)throws Exception{
        var root=Path.of("..").toAbsolutePath().normalize();var clips=NetworkSkillPresentation.load(root);var clip=clips.get(skill);
        assertEquals(12,clip.frames());assertEquals(4,clip.columns());
        int duration=skill.equals("fire_4")?clip.frames()*clip.frameMillis():1333;
        var time=new AtomicLong();var view=new NetworkSkillPresentation(time::get);view.clips(clips);
        var snapshot=state(HASH,0,event(1,0,skill));view.update(snapshot);
        var first=paint(view);time.set(duration/2);var peak=paint(view);
        time.set(duration-20);view.update(snapshot);var last=paint(view);
        assertTrue(visible(first)*10<visible(peak),"O início deve ter menos de 10% dos pixels do ápice");
        assertTrue(visible(last)*10<visible(peak),"A dissipação deve terminar com poucos pixels");
        // Movimento reduzido também deve conservar a entrada e a saída, sem congelar no ápice.
        view.reducedMotion(true);assertEquals(visible(last),visible(paint(view)));
        time.set(duration);assertEquals(0,visible(paint(view)));
        var folder=Files.createDirectories(root.resolve("docs/skills-preview-v5/runtime"));
        javax.imageio.ImageIO.write(first,"png",folder.resolve(skill+"-inicio.png").toFile());
        javax.imageio.ImageIO.write(peak,"png",folder.resolve(skill+"-apice.png").toFile());
        javax.imageio.ImageIO.write(last,"png",folder.resolve(skill+"-fim.png").toFile());
    }
    /** A ida não antecipa contato; a volta conserva o sentido do checkpoint ao lançador. */
    @Test void windRepulsionTravelsStopsAtContactAndReturnMovesFromCheckpointTowardCaster(){
        var time=new AtomicLong();var view=new NetworkSkillPresentation(time::get);
        view.clips(Map.of("wind_4",solid(0xff00ff00,8,4),"wind_4.impact",solid(0xffff0000,8,4),"wind_5",solid(0xff0000ff,8,4)));
        var launch=new SkillProtocol.Event(1,0,1,"wind_4",100,100,280,100);view.update(state(HASH,0,launch));
        assertEquals(0xff00ff00,paint(view).getRGB(100,72));assertEquals(0,paint(view).getRGB(280,72));
        time.set(100);var hit=new SkillProtocol.Event(2,3,1,"wind_4",154,100,154,100);view.update(state(HASH,3,launch,hit));
        assertEquals(1,view.activeCount());assertEquals(0xffff0000,paint(view).getRGB(154,72));
        var returned=new SkillProtocol.Event(3,3,1,"wind_5",280,100,100,100);view.update(state(HASH,3,launch,hit,returned));
        time.set(200);assertEquals(0xff0000ff,paint(view).getRGB(226,72));assertEquals(0,paint(view).getRGB(100,72));
    }
    @Test void fireRainGrowsAndDissipatesEachCrossBeforeTheNextWave(){
        var root=Path.of("..").toAbsolutePath().normalize();
        Map<String,NetworkSkillPresentation.Clip> clips;
        try{clips=NetworkSkillPresentation.load(root);}catch(IOException error){throw new AssertionError(error);}
        var time=new AtomicLong();var view=new NetworkSkillPresentation(time::get);view.clips(clips);view.update(state(HASH,0,event(1,0,"fire_5")));
        int first=visible(paint(view));time.set(180);int peak=visible(paint(view));assertTrue(peak>first*5);
        time.set(390);assertEquals(0,visible(paint(view)),"As cinco dissipações terminam antes da próxima onda");
        time.set(580);assertTrue(visible(paint(view))>first*5);time.set(2000);assertEquals(0,visible(paint(view)));
    }
    @Test void upwardWindImpactKeepsItsDirectionalBladesFacingUp(){
        var bitmap=new BufferedImage(12,4,BufferedImage.TYPE_INT_ARGB);for(int y=0;y<4;y++)for(int x=0;x<12;x++)bitmap.setRGB(x,y,0xff00ffff);
        var clip=new NetworkSkillPresentation.Clip(bitmap,12,4,1,1,100,12,4,6,2);
        var time=new AtomicLong();var view=new NetworkSkillPresentation(time::get);view.clips(Map.of("wind_4",clip,"wind_4.impact",clip));
        var launch=new SkillProtocol.Event(1,0,1,"wind_4",100,100,100,20);view.update(state(HASH,0,launch));
        time.set(100);var hit=new SkillProtocol.Event(2,3,1,"wind_4",100,46,100,46);view.update(state(HASH,3,launch,hit));
        assertEquals(0xff00ffff,paint(view).getRGB(100,13));assertEquals(0,paint(view).getRGB(105,18));
    }
    @Test void waterSpinLeavesTheBodyCenterClearAndSeismicDoesNotFillTheGround()throws Exception{
        var clips=NetworkSkillPresentation.load(Path.of("..").toAbsolutePath().normalize());
        var water=clips.get("water_2");assertEquals(12,water.frames());
        int sx=5%water.columns()*water.frameWidth()+water.pivotX(),sy=5/water.columns()*water.frameHeight()+water.pivotY();
        assertTrue((water.image().getRGB(sx,sy)>>>24)<16,"O centro do giro deve deixar o corpo legível");
        var ground=clips.get("martial_3");int count=0;
        for(int y=0;y<ground.frameHeight();y++)for(int x=0;x<ground.frameWidth();x++)if((ground.image().getRGB(ground.frameWidth()+x,ground.frameHeight()+y)>>>24)>=16)count++;
        assertTrue(count<ground.frameWidth()*ground.frameHeight()/5,"O chão conserva espaço vazio entre rachaduras");
    }
    @ParameterizedTest @ValueSource(strings={"water_5","wind_3","thunder_4"})
    void dashOnlyPaintsTheObservedPathBehindTheBodyAndFadesAfterStopping(String skill){
        var time=new AtomicLong();var view=new NetworkSkillPresentation(time::get);view.clips(Map.of(skill,solid(0xff00ffff,8,4)));
        var launch=new SkillProtocol.Event(1,0,1,skill,100,100,280,100);view.update(state(HASH,0,launch));view.actors(actorState(0,100,100));
        assertEquals(0,visible(paint(view)),"Não desenhar o destino antes do primeiro passo");
        time.set(40);view.actors(actorState(1,140,100));assertEquals(0xff00ffff,paint(view).getRGB(115,72));
        assertEquals(0,paint(view).getRGB(140,72),"O corpo fica livre de sobreposição");assertEquals(0,paint(view).getRGB(200,72));assertEquals(0,paint(view).getRGB(280,72));
        time.set(80);view.actors(actorState(2,180,100));assertEquals(0xff00ffff,paint(view).getRGB(155,72));
        var stop=new SkillProtocol.Event(2,2,1,skill,180,100,180,100);view.update(state(HASH,2,launch,stop));view.actors(actorState(2,180,100));
        time.set(1000);assertEquals(0,visible(paint(view)));assertEquals(0,view.activeCount());
    }
    @Test void lightningFieldHidesCasterUntilConfirmedEndThenDisplaysFourCuts(){
        var time=new AtomicLong();var view=new NetworkSkillPresentation(time::get);view.clips(Map.of("thunder_5",solid(0xff00ffff,8,4)));
        var start=new SkillProtocol.Event(1,0,1,"thunder_5",100,100,210,100);
        view.update(state(HASH,0,start));view.actors(actorState(0,100,100));
        assertTrue(view.hidden(1));assertFalse(view.hidden(2));assertTrue(visible(paint(view))>0);
        time.set(1067);var end=new SkillProtocol.Event(2,32,1,"thunder_5",100,100,100,100);
        view.update(state(HASH,32,start,end));assertFalse(view.hidden(1));
        time.set(1300);assertTrue(visible(paint(view))>0);
        time.set(1800);assertEquals(0,visible(paint(view)));assertEquals(0,view.activeCount());
        view.region("village");assertFalse(view.hidden(1));
    }
    @Test void delayedDashTerminationClampsAtTheConfirmedEndInsteadOfFollowingNormalWalking(){
        var time=new AtomicLong();var view=new NetworkSkillPresentation(time::get);view.clips(Map.of("water_5",solid(0xff00ffff,8,4)));
        var launch=new SkillProtocol.Event(1,0,1,"water_5",100,100,280,100);view.update(state(HASH,0,launch));view.actors(actorState(0,100,100));
        time.set(80);view.actors(actorState(2,160,100));
        var end=new SkillProtocol.Event(2,8,1,"water_5",180,100,180,100);view.update(state(HASH,8,launch,end));view.actors(actorState(6,160,100));
        time.set(120);view.actors(actorState(9,200,100));
        assertEquals(0xff00ffff,paint(view).getRGB(170,72));assertEquals(0,paint(view).getRGB(184,72));
        view.update(null);assertEquals(0,view.activeCount());
    }
    @Test void dashNeverInventsAPathForUnknownFinishOrTeleport(){
        var time=new AtomicLong();var view=new NetworkSkillPresentation(time::get);view.clips(Map.of("water_5",solid(0xff00ffff,8,4)));
        view.update(state(HASH,8,new SkillProtocol.Event(1,8,1,"water_5",180,100,180,100)));view.actors(actorState(8,180,100));assertEquals(0,visible(paint(view)));
        view.update(state(HASH,10,new SkillProtocol.Event(2,10,1,"water_5",180,100,300,100)));view.actors(actorState(10,180,100));
        time.set(30);view.actors(actorState(11,400,100));assertEquals(0,visible(paint(view)),"Correção grande não liga dois pontos por uma faixa falsa");
    }
    @Test void bodySlamShowsGrippingHandsBeforeTheConfirmedGroundImpactWithoutRestarting(){
        var time=new AtomicLong();var view=new NetworkSkillPresentation(time::get);
        view.clips(Map.of("martial_4",solid(0xff00ffff,8,4),"martial_4.travel",solid(0xff00ffff,8,4),"martial_4.impact",solid(0xffff0000,8,4)));
        var grip=new SkillProtocol.Event(1,0,1,"martial_4",100,100,140,100);var impact=new SkillProtocol.Event(2,0,1,"martial_4",140,100,140,100);
        var snapshot=state(HASH,0,grip,impact);view.update(snapshot);
        assertEquals(0xff00ffff,paint(view).getRGB(140,72));assertEquals(0,paint(view).getRGB(140,100));
        time.set(200);view.update(snapshot);assertEquals(0,paint(view).getRGB(140,72));assertEquals(0xffff0000,paint(view).getRGB(140,100));
        time.set(300);assertEquals(0,view.activeCount());view.update(snapshot);assertEquals(0,view.activeCount());
    }
    @Test void allLightningPhasesUseWarmColorsAndHotbarIconsStillLoad()throws Exception{
        var clips=NetworkSkillPresentation.load(Path.of("..").toAbsolutePath().normalize());
        for(String name:List.of("thunder_1","thunder_2","thunder_3","thunder_4","thunder_5","thunder_1.impact","thunder_2.impact","thunder_3.impact","thunder_5.travel")){
            var clip=clips.get(name);assertNotNull(clip,name);assertNotNull(clip.icon());int warm=0,cold=0;
            for(int y=0;y<clip.image().getHeight();y+=2)for(int x=0;x<clip.image().getWidth();x+=2){int pixel=clip.image().getRGB(x,y);if((pixel>>>24)<128)continue;int r=pixel>>16&255,g=pixel>>8&255,b=pixel&255;
                if(r>b+30)warm++;if(b>r+12&&b>g)cold++;
            }
            assertTrue(warm>0,name);
            assertTrue(cold<=Math.max(1,warm/1000),name+": a paleta deve ser predominantemente quente, tolerando resíduo isolado de geração");
        }
    }
    @ParameterizedTest @ValueSource(strings={"water_5","wind_3","thunder_4"})
    void nativeDashFragmentsRemainVisibleAcrossShortSnapshotSegments(String skill)throws Exception{
        var root=Path.of("..").toAbsolutePath().normalize();var time=new AtomicLong();var view=new NetworkSkillPresentation(time::get);view.clips(NetworkSkillPresentation.load(root));
        view.update(state(HASH,0,new SkillProtocol.Event(1,0,1,skill,100,100,300,100)));view.actors(actorState(0,100,100));
        for(int step=1;step<=6;step++){time.set(step*30);view.actors(actorState(step,100+step*15,100));}
        var image=paint(view);assertTrue(visible(image)>30,"Fragmentos reais precisam permanecer legíveis entre snapshots: "+skill);
        assertEquals(0,image.getRGB(300,72));
        var folder=Files.createDirectories(root.resolve("docs/skills-preview-v8/runtime"));javax.imageio.ImageIO.write(image,"png",folder.resolve(skill+"-fragmentos.png").toFile());
    }
    private Protocol.State actorState(long tick,float x,float y){return new Protocol.State(tick,List.of(new Protocol.Actor(1,"Jogador",x,y,100,100,1000,0,false)));}
    /** Conta cobertura visível, independente da identidade dos frames escolhidos. */
    private int visible(BufferedImage image){int count=0;for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)if(image.getRGB(x,y)>>>24>=128)count++;return count;}
    private BufferedImage paint(NetworkSkillPresentation view){var output=new BufferedImage(500,500,BufferedImage.TYPE_INT_ARGB);var graphics=output.createGraphics();try{view.draw(graphics);}finally{graphics.dispose();}return output;}
}
