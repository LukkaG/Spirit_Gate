package com.projetok.prototype.rpg;

import com.projetok.prototype.DirectionalAnimation.Direction;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Combinações usam os mesmos corpos e pivôs, com perfil independente e fallback. */
class CosmeticLayersTest {
    @TempDir Path folder;
    @Test void everyCombinationDirectionAndWalkFrameRendersWithFallback()throws Exception{
        var catalog=CosmeticCatalog.load(Path.of("../assets/dev/generated/corujao-iv-v1"));assertTrue(catalog.warnings().isEmpty());var body=RpgPlayerSprites.load(Path.of("../assets/dev/player-base"),250,.5);var image=new BufferedImage(160,160,2);var g=image.createGraphics();try{for(var hair:catalog.all("hair"))for(var clothing:catalog.all("clothing"))for(var direction:Direction.values())for(int frame=0;frame<4;frame++){var selected=new CosmeticCatalog.Selection(hair.id(),clothing.id());catalog.validate(selected);assertTrue(CosmeticRenderer.draw(g,body,catalog,selected,direction,frame,true,80,150));assertTrue(CosmeticRenderer.draw(g,body,catalog,selected,direction,0,false,80,150));}assertTrue(CosmeticRenderer.draw(g,body,catalog,new CosmeticCatalog.Selection("missing","missing"),Direction.FRONT,0,false,80,150));}finally{g.dispose();}
    }
    @Test void profileRoundTripRejectsUnknownCosmeticsWithoutChangingFile()throws Exception {var catalog=CosmeticCatalog.metadata(Path.of("../assets/dev/generated/corujao-iv-v1"));var profile=new CosmeticProfile("preview",new CosmeticCatalog.Selection("hair_short","jacket"));Path path=folder.resolve("profile.properties");profile.save(path,catalog);assertEquals(profile,CosmeticProfile.load(path,catalog));byte[] before=Files.readAllBytes(path);assertThrows(IllegalArgumentException.class,()->new CosmeticProfile("preview",new CosmeticCatalog.Selection("../../saves","jacket")).save(path,catalog));assertArrayEquals(before,Files.readAllBytes(path));}
}
