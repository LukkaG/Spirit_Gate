package com.projetok.studio;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
class AssetCatalogTest {
    @TempDir Path project;
    @Test void stableIdDetectsChangedImagesAndRejectsInvalidFiles()throws Exception {
        var assets=Files.createDirectories(project.resolve("assets"));Path p=assets.resolve("alpha.png");ImageIO.write(new BufferedImage(65,65,2),"png",p.toFile());Files.writeString(assets.resolve("bad.png"),"not an image");
        var first=AssetCatalog.scan(project,null);assertEquals(1,first.assets().size());assertEquals(1,first.warnings().size());assertTrue(first.assets().get(0).alpha());
        ImageIO.write(new BufferedImage(66,66,2),"png",p.toFile());var next=AssetCatalog.scan(project,first);assertEquals(first.assets().get(0).id(),next.assets().get(0).id());assertNotEquals(first.assets().get(0).sha(),next.assets().get(0).sha());assertTrue(next.changes().get(0).startsWith("Alterado"));
        var cache=new AssetImages();assertNotNull(cache.load(next.assets().get(0)));assertTrue(cache.bytes()<64L*1024*1024);assertThrows(java.io.IOException.class,()->new AssetImages().load(first.assets().get(0)));
    }
    @Test void tilesetUsesMarginsSpacingAndBoundsWithoutChangingOriginal()throws Exception {
        Path p=Files.createDirectories(project.resolve("assets")).resolve("tiles.png");ImageIO.write(new BufferedImage(70,70,2),"png",p.toFile());var a=AssetCatalog.scan(project,null).assets().get(0);String sha=AssetCatalog.hash(p);
        assertEquals(4,AssetCatalog.tiles(a,32,32,2,2,new WorldDocument.Crop(0,0,70,70)).size());assertEquals(sha,AssetCatalog.hash(p));assertThrows(IllegalArgumentException.class,()->AssetCatalog.tiles(a,100,100,0,0,new WorldDocument.Crop(0,0,70,70)));
    }
}
