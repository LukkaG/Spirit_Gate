package com.projetok.prototype.rpg;

import java.nio.file.*;
import java.util.*;
import java.security.MessageDigest;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Confere todos os PNGs temporários contra dimensões, pivôs e hashes declarados. */
class GeneratedCosmeticsTest {
    @Test void layersMatchActualBaseFramesAndHaveBinaryTransparency()throws Exception {
        Path root=Path.of("../assets/dev/generated/corujao-iv-v1");Properties manifest=new Properties();try(var in=Files.newInputStream(root.resolve("manifest.properties"))){manifest.load(in);}
        Properties original=new Properties();try(var in=getClass().getResourceAsStream("/rpg/experimental-player.properties")){original.load(in);}
        assertEquals("true",manifest.getProperty("temporary"));assertEquals(6,manifest.stringPropertyNames().stream().filter(k->k.startsWith("cosmetic.")).count());
        int count=Integer.parseInt(manifest.getProperty("count"));assertEquals(180,count);Set<String> combinations=new HashSet<>();
        for(int i=0;i<count;i++){
            String[] v=manifest.getProperty("frame."+i).split("\\|");assertEquals(10,v.length);assertTrue(combinations.add(v[0]+"/"+v[1]+"/"+v[2]));assertEquals("TEMPORARIO",v[9]);
            Path path=root.resolve(v[3]).normalize();assertTrue(path.startsWith(root.normalize()));byte[] bytes=Files.readAllBytes(path);assertEquals(v[4],HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
            var image=ImageIO.read(path.toFile());String[] base=original.getProperty(v[1]).split(",");assertEquals(Integer.parseInt(base[2]),image.getWidth());assertEquals(Integer.parseInt(base[3]),image.getHeight());assertEquals(base[4],v[7]);assertEquals(base[5],v[8]);
            boolean transparent=false,opaque=false;for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){int alpha=image.getRGB(x,y)>>>24;assertTrue(alpha==0||alpha==255);transparent|=alpha==0;opaque|=alpha==255;}assertTrue(transparent);assertTrue(opaque);
        }
        for(String key:original.stringPropertyNames()){String[] v=original.getProperty(key).split(",");Path path=Path.of("../assets/dev/player-base").resolve(v[0]);assertEquals(v[1],HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))));}
    }
}
