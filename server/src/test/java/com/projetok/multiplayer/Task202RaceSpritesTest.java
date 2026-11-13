package com.projetok.multiplayer;

import com.projetok.prototype.DirectionalAnimation.Direction;
import com.projetok.prototype.rpg.RaceSpriteCatalog;
import com.projetok.prototype.rpg.RaceSpriteCatalog.Action;
import com.projetok.prototype.rpg.RpgPlayerSprites;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class Task202RaceSpritesTest {
    @TempDir Path root;
    private NetworkPlayerVisuals visuals(Path reaper) throws Exception {
        return new NetworkPlayerVisuals(reaper,RpgPlayerSprites.load(Path.of("../assets/dev/player-base"),250,.5));
    }
    private int[] render(RpgPlayerSprites sprites,Direction direction,int frame,boolean walking) {
        var image=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();
        try {assertTrue(sprites.drawFrame(g,direction,frame,walking,32,60));} finally {g.dispose();}
        var pixels=image.getRGB(0,0,64,64,null,0,64);
        assertTrue(Arrays.stream(pixels).anyMatch(pixel -> (pixel>>>24)!=0));
        return pixels;
    }
    @Test void catalogMatchesEveryResidentFrameAndDoesNotInventCombatArt() throws Exception {
        var visuals=visuals(Path.of("../assets/dev/reaper"));
        assertEquals(4,RaceSpriteCatalog.all().size());
        for(var definition:RaceSpriteCatalog.all()) {
            var sprites=visuals.sprites(definition.race());
            assertEquals(sprites.walkFrames(),definition.clips().get(Action.WALK).frames());
            assertEquals(4,definition.directions().size());
            assertEquals(32,definition.pivotX());assertEquals(60,definition.pivotY());
            for(var missing:List.of(Action.RUN,Action.ATTACK,Action.HIT,Action.DEATH))assertFalse(definition.supports(missing));
            for(var direction:definition.directions()) {
                render(sprites,direction,0,false);
                for(int frame=0;frame<sprites.walkFrames();frame++) {
                    render(sprites,direction,frame,true);
                    assertTrue(sprites.visualBounds(direction,frame,true,32,60).getMaxY()<=60);
                }
            }
        }
        for(String race:List.of("aetheri","spirit")) {
            assertSame(visuals.sprites("human"),visuals.sprites(race));
            assertTrue(visuals.renderInfo(race).provisional());
            assertTrue(visuals.renderInfo(race).detail().contains("provisória Human"));
        }
        assertFalse(visuals.supportsAccessories("reaper"));assertTrue(visuals.supportsAccessories("human"));
        assertThrows(IllegalArgumentException.class,()->RaceSpriteCatalog.get("unknown"));
    }
    @Test void missingReaperNeverSilentlyBorrowsHumanOrAccessories() throws Exception {
        var visuals=visuals(root.resolve("absent"));
        assertNull(visuals.sprites("reaper"));assertFalse(visuals.renderInfo("reaper").available());
        assertTrue(visuals.renderInfo("reaper").detail().contains("Fallback geométrico"));
        assertFalse(visuals.supportsAccessories("reaper"));assertNotNull(visuals.sprites("human"));
        var absent=new NetworkPlayerVisuals(root.resolve("absent"),null);
        for(var definition:RaceSpriteCatalog.all())assertFalse(absent.renderInfo(definition.race()).available());
    }
    @Test void twoTcpClientsRenderDifferentRacesUsingServerIdentity() throws Exception {
        var fixture=new Task201CharacterSessionTest();fixture.root=root;
        var visuals=visuals(Path.of("../assets/dev/reaper"));
        try(var server=fixture.server();var human=Task201CharacterSessionTest.client(server,"Human 202","human");
            var reaper=Task201CharacterSessionTest.client(server,"Reaper 202","reaper")) {
            Task201CharacterSessionTest.ready(human,reaper);
            Task201CharacterSessionTest.waitFor(()->human.appearances().size()==2&&reaper.appearances().size()==2,human,reaper);
            for(var client:List.of(human,reaper)) {
                String humanRace=client.appearances().get(human.id()).race();
                String reaperRace=client.appearances().get(reaper.id()).race();
                assertEquals("human",humanRace);assertEquals("reaper",reaperRace);
                assertFalse(Arrays.equals(render(visuals.sprites(humanRace),Direction.FRONT,0,false),
                    render(visuals.sprites(reaperRace),Direction.FRONT,0,false)));
                assertEquals(250,visuals.frameMillis(humanRace));assertEquals(125,visuals.frameMillis(reaperRace));
            }
            assertNull(server.failure());
        }
    }
}
