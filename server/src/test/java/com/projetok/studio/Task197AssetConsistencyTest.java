package com.projetok.studio;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class Task197AssetConsistencyTest {
    @TempDir Path temp;
    Path image(Path root,int rgb)throws Exception {
        Path path=root.resolve("assets/fixture.png");Files.createDirectories(path.getParent());
        var bitmap=new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);bitmap.setRGB(0,0,rgb);assertTrue(ImageIO.write(bitmap,"png",path.toFile()));return path;
    }
    LiveRevision revision(AssetCatalog.Result catalog)throws Exception {
        var a=catalog.assets().get(0);var maps=new TreeMap<>(WorldContent.baseline().bundle().maps());var forest=maps.get("forest");
        var elements=new ArrayList<>(forest.elements());elements.add(new WorldDocument.Element("fixture",WorldDocument.Kind.TILE,a.id(),a.sha(),
            new WorldDocument.Crop(0,0,32,32),700,450,32,32,"ground",true,false,"","FRONT",Map.of()));
        maps.put("forest",forest.withElements(elements));return LiveRevision.prepare(1,"forest",maps,catalog);
    }
    @Test void pairedLocalInstallsPrepareSamePixelsAndMissingFileNeverUsesPlaceholder()throws Exception {
        Path host=temp.resolve("host"),guest=temp.resolve("guest");Path source=image(host,0xff00ff00);
        Path copy=guest.resolve("assets/fixture.png");Files.createDirectories(copy.getParent());Files.copy(source,copy);
        var a=AssetCatalog.scan(host,null);var b=AssetCatalog.scan(guest,null);assertEquals(a.assets().get(0).id(),b.assets().get(0).id());
        assertEquals(a.assets().get(0).sha(),b.assets().get(0).sha());byte[] descriptor=WorldContent.encode(revision(a));
        var left=WorldContent.prepare(descriptor,a);var right=WorldContent.prepare(descriptor,b);String id=a.assets().get(0).id();
        assertEquals(left.revision().hash(),right.revision().hash());assertEquals(0xff00ff00,right.images().peek(id).getRGB(0,0));assertEquals(4096,right.images().bytes());
        Files.delete(copy);assertThrows(Exception.class,()->WorldContent.prepare(descriptor,b));
        var missing=AssetCatalog.scan(guest,null);var error=assertThrows(Exception.class,()->WorldContent.prepare(descriptor,missing));
        assertTrue(error.getMessage().contains("Sprite inexistente"));assertNotNull(left.images().peek(id));
        System.out.println("TASK197 pairedHash="+left.revision().hash()+" missingRejected=true placeholderSuccess=false imageTransferBytes=0");
    }
    @Test void changedLocalAssetWithStablePathIsRejectedBeforeActivation()throws Exception {
        Path host=temp.resolve("host"),guest=temp.resolve("guest");image(host,0xff00ff00);image(guest,0xff0000ff);
        var a=AssetCatalog.scan(host,null);var b=AssetCatalog.scan(guest,null);assertEquals(a.assets().get(0).id(),b.assets().get(0).id());
        assertNotEquals(a.assets().get(0).sha(),b.assets().get(0).sha());byte[] descriptor=WorldContent.encode(revision(a));
        var error=assertThrows(Exception.class,()->WorldContent.prepare(descriptor,b));assertTrue(error.getMessage().contains("Asset alterado"));
        assertThrows(IOException.class,()->AssetCatalog.verified(b.assets().get(0).file(),a.assets().get(0).id(),a.assets().get(0).sha()));
        System.out.println("TASK197 adulterationRejected=true stableIdDifferentSha=true");
    }
    @Test void declaredHugePngAndOversizedFileAreRejectedBeforePixelDecode()throws Exception {
        Path root=temp.resolve("huge");Path png=image(root,0xff00ff00);byte[] bytes=Files.readAllBytes(png);
        ByteBuffer.wrap(bytes).putInt(16,9000);var crc=new CRC32();crc.update(bytes,12,17);ByteBuffer.wrap(bytes).putInt(29,(int)crc.getValue());Files.write(png,bytes);
        var indexed=AssetCatalog.scan(root,null);assertTrue(indexed.assets().isEmpty());assertTrue(indexed.warnings().stream().anyMatch(s->s.contains("Dimensoes")));
        assertThrows(IOException.class,()->AssetCatalog.verified(png,"a".repeat(64),AssetCatalog.hash(png)));
        Path large=root.resolve("assets/oversized.png");try(var file=new RandomAccessFile(large.toFile(),"rw")){file.setLength(32_000_001);}
        assertThrows(IOException.class,()->AssetCatalog.hash(large));assertTrue(AssetCatalog.scan(root,null).assets().isEmpty());
        System.out.println("TASK197 width9000Rejected=true bytes32000001Rejected=true decodeLimitPreserved=true");
    }
}
