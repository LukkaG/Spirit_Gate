package com.projetok.prototype;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.security.MessageDigest;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

/** Somente dados sinteticos: nenhuma aprovacao de arte real. */
class VisualPackageTest {
    @Test void duplicatePropertiesAndExtremeOffsetsFailSafely() throws Exception {
        Path path=fixture();Files.writeString(path,"\npivotX=0\n",StandardOpenOption.APPEND);
        assertNull(ValidatedSpriteAtlas.loadOptional(path));
        for(String value:List.of("2147483647","-2147483648","4097")) {
            path=fixture();alter(path,"offsetX",value);assertNull(ValidatedSpriteAtlas.loadOptional(path));
        }
        path=fixture();Files.writeString(path,"x".repeat(1_000_001));
        assertNull(ValidatedSpriteAtlas.loadOptional(path));
    }
    @TempDir Path temp;
    private Path fixture() throws Exception {
        var image=new BufferedImage(512,512,BufferedImage.TYPE_INT_ARGB);
        Path png=temp.resolve("synthetic.png");ImageIO.write(image,"png",png.toFile());
        String sha=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(png)));
        var p=new Properties();
        for(String[] pair:new String[][]{{"status","VALIDADO"},{"packageVersion","1"},{"assetId","synthetic.hero"},
                {"frameSize","64"},{"tileSize","32"},{"reviewOrigin","FIXTURE SINTETICA"},{"image","synthetic.png"},
                {"sha256",sha},{"approvedSha256",sha},{"pivotX","32"},{"pivotY","48"},{"offsetX","0"},{"offsetY","0"}})
            p.setProperty(pair[0],pair[1]);
        int slot=0;
        for(var state:DirectionalAnimation.State.values()) for(var d:DirectionalAnimation.Direction.values()) {
            if(state==DirectionalAnimation.State.FAINT && d!=DirectionalAnimation.Direction.FRONT) continue;
            String action=switch(state){case ATTACK->"PUNCH";case FAINT->"KO";default->state.name();};
            for(int i=0;i<DirectionalAnimation.count(state);i++,slot++) p.setProperty(action+"."+d+"."+i,(slot%8)*64+","+(slot/8)*64+",64,64");
        }
        Path path=temp.resolve("character.properties");try(var out=Files.newBufferedWriter(path)){p.store(out,"SINTETICO");}return path;
    }
    private void alter(Path path,String key,String value) throws Exception {
        var p=new Properties();try(var r=Files.newBufferedReader(path)){p.load(r);}
        if(value==null)p.remove(key);else p.setProperty(key,value);
        try(var out=Files.newBufferedWriter(path)){p.store(out,"SINTETICO");}
    }
    @Test void validLoadsOnceAndSurvivesSourceRemoval() throws Exception {
        Path p=fixture();var atlas=ValidatedSpriteAtlas.load(p);Files.delete(temp.resolve("synthetic.png"));
        var s=DebugWorld.demo(new Random(1)).snapshot();var image=new BufferedImage(960,640,2);var g=image.createGraphics();
        assertTrue(atlas.draw(g,s.actors().get(0),new DirectionalAnimation.View(1,DirectionalAnimation.State.IDLE,DirectionalAnimation.Direction.FRONT,0)));g.dispose();
    }
    @Test void invalidPackagesAlwaysFallback() throws Exception {
        for(String[] change:new String[][]{{"sha256","0"},{"approvedSha256","0"},{"IDLE.FRONT.0",null},
                {"IDLE.FRONT.0","0,0,32,64"},{"image","missing.png"},{"image","../outside.png"},
                {"pivotX","bad"},{"packageVersion","2"},{"reviewOrigin",""}}) {
            Path p=fixture();alter(p,change[0],change[1]);assertNull(ValidatedSpriteAtlas.loadOptional(p),Arrays.toString(change));
        }
        Path p=fixture();Files.write(temp.resolve("synthetic.png"),new byte[]{1,2,3});assertNull(ValidatedSpriteAtlas.loadOptional(p));
        assertNull(ValidatedSpriteAtlas.loadOptional(temp.resolve("absent.properties")));
    }
    @Test void portablePathIndependentOfWorkingDirectory() {
        String old=System.getProperty("jpackage.app-path");
        try {System.setProperty("jpackage.app-path",temp.resolve("SpiritGate.exe").toString());
            assertEquals(temp.resolve("visual/character.properties"),DebugLauncher.defaultVisualManifest());
        } finally {if(old==null)System.clearProperty("jpackage.app-path");else System.setProperty("jpackage.app-path",old);}
    }
}
