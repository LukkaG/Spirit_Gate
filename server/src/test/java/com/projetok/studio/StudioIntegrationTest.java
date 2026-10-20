package com.projetok.studio;
import com.projetok.rpg.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class StudioIntegrationTest {
    @TempDir Path folder;
    static WorldDocument map(String id,String region){var d=WorldDocument.empty(id,960,640);var spawn=new WorldDocument.Element("player",WorldDocument.Kind.PLAYER,"","",new WorldDocument.Crop(0,0,32,32),140,320,32,32,"interaction",true,false,"","FRONT",Map.of());return new WorldDocument(1,id,id,region,960,640,32,d.layers(),List.of(spawn),List.of(),List.of(),List.of(),Map.of());}
    @Test void exportedCollisionAndPortalRunInRealWorldWithoutChangingLegacy()throws Exception{var a=map("novo","village").withHitboxes(List.of(new WorldDocument.Hitbox("wall",200,280,32,100,true,"collision"))).withPortals(List.of(new WorldDocument.Portal("link",140,320,"bosque",140,320,"","FRONT")));var b=map("bosque","forest");var path=StudioExport.write(folder.resolve("export"),a.id(),Map.of(a.id(),a,b.id(),b),new AssetCatalog.Result(List.of(),List.of(),List.of()));var bundle=StudioExport.load(path);var w=new RpgWorld(CharacterIdentity.create("Teste","guardian","azure"),bundle.scenes());for(int i=0;i<25;i++)w.tick(1,0);assertTrue(w.snapshot().x()<=182.01);assertTrue(w.travel(StudioExport.key("portal_",a.id()+"/link")));assertEquals("forest",w.save().region());assertEquals(140,w.snapshot().x());var legacy=new RpgWorld();assertEquals(140,legacy.snapshot().x());assertTrue(legacy.snapshot().enemies().isEmpty());}
    @Test void unsupportedSizeAndTamperedBundleAreRejected()throws Exception{var a=map("novo","village");var out=StudioExport.write(folder.resolve("ok"),a.id(),Map.of(a.id(),a),new AssetCatalog.Result(List.of(),List.of(),List.of()));Files.writeString(out.resolve("novo.sgmap"),"alterado");assertThrows(java.io.IOException.class,()->StudioExport.load(out));var big=WorldDocument.empty("grande",1920,1280);assertThrows(java.io.IOException.class,()->StudioExport.write(folder.resolve("bad"),big.id(),Map.of(big.id(),big),new AssetCatalog.Result(List.of(),List.of(),List.of())));assertFalse(Files.exists(folder.resolve("bad")));}
}
