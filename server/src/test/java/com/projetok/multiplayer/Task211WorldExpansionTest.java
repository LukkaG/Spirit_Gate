package com.projetok.multiplayer;

import com.projetok.studio.*;
import com.projetok.prototype.rpg.*;
import java.awt.image.BufferedImage;
import java.net.InetAddress;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class Task211WorldExpansionTest {
    @TempDir Path root;
    @Test void nativeMapsHaveReachablePortalsValidCollisionsAndExistingArt()throws Exception {
        var assets=AssetCatalog.scan(Path.of("..").toAbsolutePath(),null);
        var revision=CorujaoSevenWorld.initial(assets);
        assertEquals(2,revision.bundle().maps().size());
        var issues=StudioValidator.validate(revision.bundle().maps(),assets,true);
        assertTrue(issues.isEmpty(),issues.toString());
        var evidence=Path.of("../.task-checkpoints/TASK-211/corujao-vii");Files.createDirectories(evidence);
        var prepared=WorldContent.prepare(WorldContent.encode(revision),assets);
        for(var document:revision.bundle().maps().values()) {
            var nav=NavigationAnalysis.analyze(document);assertEquals(0,nav.isolated(),document.region());
            assertEquals(2,document.portals().size());
            for(var portal:document.portals()) {
                assertTrue(StudioGeometry.free(document,portal.x(),portal.y(),18));
                assertFalse(nav.isolated(portal.x()/nav.cell(),portal.y()/nav.cell()));
            }
            assertTrue(document.elements().stream().anyMatch(e->e.properties().getOrDefault("art_source","").equals("existing")));
            Files.write(evidence.resolve(document.id()+".sgmap"),DocumentStore.encode(document));
            var renderer=new StudioMapRenderer(revision.bundle(),prepared.images(),document::region);
            var image=new BufferedImage(960,640,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
            try {
                renderer.layers(g,false);
                RpgPlayerSprites.load(Path.of("../assets/dev/reaper"),125,1).drawPreview(g,com.projetok.prototype.DirectionalAnimation.Direction.FRONT,140,300);
                renderer.layers(g,true);renderer.portalHints(g);
                g.setColor(java.awt.Color.RED);for(var box:document.hitboxes())g.drawRect(box.x(),box.y(),box.width(),box.height());
            }finally{g.dispose();}
            ImageIO.write(image,"png",evidence.resolve(document.id()+"-navigation.png").toFile());
        }
    }
    @Test void twoTcpPlayersShareExpandedRegionAndSafeTravel()throws Exception {
        try(var server=LauncherSession.startExpanded(InetAddress.getLoopbackAddress(),0,SessionCharacterSelectionTest.catalog(),root);
            var a=Task207InventoryTest.connect(server,"Explora Spirit","spirit");var b=Task207InventoryTest.connect(server,"Explora Human","human")) {
            Task201CharacterSessionTest.ready(a,b);
            assertEquals(2,a.snapshot().actors().stream().filter(p->p.id()>1_000_000&&p.id()<2_000_000).count());
            Task210QuestJourneyTest.travel(a,b,"forest","to_village","village");
            assertEquals(2,a.snapshot().actors().stream().filter(p->p.id()>2_000_000).count());
            assertEquals("forest",b.region());assertTrue(a.snapshot().actors().stream().noneMatch(p->p.id()==b.id()));assertNull(server.failure());
        }
    }
}
