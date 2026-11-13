package com.projetok.studio;

import com.projetok.domain.entity.*;
import com.projetok.domain.movement.*;
import com.projetok.prototype.*;
import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Exercita fisica real, compatibilidade, revisao e profundidade sem depender de sprites pessoais. */
class DirectionalMapEditingTest {
    @TempDir Path temp;
    private static final AssetCatalog.Result EMPTY=new AssetCatalog.Result(List.of(),List.of(),List.of());
    private static WorldDocument map(int mask){return FileTools.playable(WorldDocument.empty("mapa",960,640)).withHitboxes(List.of(new WorldDocument.Hitbox("tree",300,200,96,96,true,"collision",mask)));}
    private static GameEntity actor(float x,float y){return new GameEntity(1,"Teste",new Position(x,y)){};}
    @Test void openTopAllowsEntryAndExitWhileOtherSidesStopSweptMovement(){
        var space=StudioGeometry.space(map(14));var physics=new CollisionService();var player=actor(348,170);
        physics.resolveContinuous(player,0,70,12,space);assertEquals(240,player.getPosition().y());
        physics.resolveContinuous(player,0,120,12,space);assertEquals(283,player.getPosition().y(),.001);
        physics.resolveContinuous(player,-200,0,12,space);assertEquals(313,player.getPosition().x(),.001);
        physics.resolveContinuous(player,300,0,12,space);assertEquals(383,player.getPosition().x(),.001);
        physics.resolveContinuous(player,0,-150,12,space);assertTrue(player.getPosition().y()<200);
        var outside=actor(348,340);physics.resolveContinuous(outside,0,-150,12,space);assertEquals(308,outside.getPosition().y(),.001);
    }
    @Test void eachSideBlocksInBothDirectionsAndDiagonalCannotTunnel(){
        var physics=new CollisionService();
        for(int bit:new int[]{1,2,4,8}){
            var space=StudioGeometry.space(map(bit));boolean vertical=bit==1||bit==4;float edge=bit==1?200:bit==4?295:bit==8?300:395;
            for(int sign:new int[]{-1,1}){
                var p=actor(vertical?348:edge+sign*40,vertical?edge+sign*40:248);
                physics.resolveContinuous(p,vertical?0:-sign*90,vertical?-sign*90:0,12,space);
                assertEquals(edge+sign*(sign>0?13:12),vertical?p.getPosition().y():p.getPosition().x(),.001);
            }
        }
        var p=actor(250,150);var full=StudioGeometry.space(map(15));physics.resolveContinuous(p,150,150,12,full);assertTrue(physics.isFree(p,p.getPosition().x(),p.getPosition().y(),12,full));
    }
    @Test void allSidesKeepFilledLegacyVolumeAndZeroHasNoPhysics(){
        assertEquals(1,StudioGeometry.space(map(15)).obstacles().size());assertFalse(StudioGeometry.free(map(15),348,248,12));
        assertTrue(StudioGeometry.free(map(0),348,248,12));assertEquals(0,StudioGeometry.space(map(0)).obstacles().size());
        assertThrows(IllegalArgumentException.class,()->map(16));
    }
    @Test void legacyRoundTripStaysV1AndDirectionsRequireV2(){
        assertDoesNotThrow(()->{var old=map(15);var bytes=DocumentStore.encode(old);assertTrue(new String(bytes,StandardCharsets.UTF_8).contains("schema=1"));assertEquals(old,DocumentStore.decode(bytes));});
        assertDoesNotThrow(()->{for(int mask=0;mask<16;mask++){var d=map(mask);var bytes=DocumentStore.encode(d);assertEquals(d,DocumentStore.decode(bytes));if(mask!=15)assertTrue(new String(bytes,StandardCharsets.UTF_8).contains("schema=2"));}});
    }
    @Test void masksSurviveSaveBackupMovePrefabAndPatch()throws Exception{
        var d=map(14);Path file=temp.resolve("mapa.sgmap");DocumentStore.save(file,map(15));DocumentStore.save(file,d);
        assertEquals(d,DocumentStore.load(file));assertEquals(15,DocumentStore.load(Path.of(file+".bak")).hitboxes().get(0).edges());
        assertEquals(14,GroupEditing.move(d,Set.of("tree"),32,0).hitboxes().get(0).edges());
        var element=new WorldDocument.Element("art",WorldDocument.Kind.OBJECT,"","",new WorldDocument.Crop(0,0,32,32),300,200,96,96,"objects",true,false,"","FRONT",Map.of());
        var prefab=Prefabs.capture(d.withElements(List.of(element)),Set.of("art","tree"),"prefab");assertEquals(14,prefab.hitboxes().get(0).edges());
        var inserted=Prefabs.insert(WorldDocument.empty("dest",960,640),prefab,100,100);assertEquals(14,inserted.hitboxes().get(0).edges());
        assertEquals(d,DraftPatch.between(map(15),d).apply(map(15)));
    }
    @Test void cellEditsPreserveArtSupportUndoAndRespectLockedLayer(){
        var d=FileTools.playable(WorldDocument.empty("mapa",960,640));var next=CollisionTools.setCell(d,new Point(322,222),14);
        assertEquals(d.elements(),next.elements());assertEquals(14,next.hitboxes().get(0).edges());
        var history=new EditorHistory(d);history.commit(next);assertEquals(d,history.undo());assertEquals(next,history.redo());
        assertTrue(CollisionTools.setCell(next,new Point(322,222),0).hitboxes().isEmpty());
        var locked=d.withLayers(d.layers().stream().map(l->l.role()==WorldDocument.Role.COLLISION?new WorldDocument.Layer(l.id(),l.name(),l.role(),l.order(),true,true):l).toList());
        assertThrows(IllegalArgumentException.class,()->CollisionTools.setCell(locked,new Point(322,222),14));
    }
    @Test void frozenNetworkContentCarriesDirectionalPhysics()throws Exception{
        var base=WorldContent.baseline();var maps=new TreeMap<>(base.bundle().maps());maps.put("forest",maps.get("forest").withHitboxes(map(14).hitboxes()));var revision=LiveRevision.prepare(1,"forest",maps,EMPTY);
        var decoded=WorldContent.decode(WorldContent.encode(revision),EMPTY);assertEquals(revision.hash(),decoded.hash());
        assertEquals(3,decoded.geometry().get("forest").obstacles().size());
    }
    @Test void objectsOccludeBehindActorAndYieldToActorInFront()throws Exception{
        var image=new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);var paint=image.createGraphics();paint.setColor(Color.RED);paint.fillRect(0,0,32,32);paint.dispose();Path file=temp.resolve("tree.png");ImageIO.write(image,"png",file.toFile());
        var asset=AssetCatalog.verified(file,"a".repeat(64),AssetCatalog.hash(file));var images=new AssetImages();images.load(asset);
        var e=new WorldDocument.Element("tree",WorldDocument.Kind.OBJECT,asset.id(),asset.sha(),new WorldDocument.Crop(0,0,32,32),100,100,64,64,"objects",true,false,"","FRONT",Map.of());
        var d=WorldDocument.empty("mapa",960,640).withElements(List.of(e));
        var renderer=new StudioMapRenderer(new StudioExport.Bundle(d.id(),Map.of(d.id(),d),EMPTY,null),images,()->"village");
        for(int y:new int[]{140,180}){var canvas=new BufferedImage(220,220,BufferedImage.TYPE_INT_ARGB);var g=canvas.createGraphics();renderer.layers(g,false);
            var a=new DebugSnapshot.ActorView(1,"Jogador",120,y,12,10,10,true,true);
            renderer.actors(g,List.of(a),actor->{g.setColor(Color.BLUE);g.fillRect(110,110,20,20);});renderer.layers(g,true);g.dispose();
            assertEquals((y<164?Color.RED:Color.BLUE).getRGB(),canvas.getRGB(120,120));
        }
        assertThrows(IllegalArgumentException.class,()->new WorldDocument.Element("bad",e.kind(),e.asset(),e.sha(),e.crop(),0,0,64,64,"objects",true,false,"","FRONT",Map.of("depth.pivot","999")));
    }
}

