package com.projetok.studio;

import com.projetok.multiplayer.*;
import com.projetok.domain.movement.*;
import java.net.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class StudioLiveTest {
    @TempDir Path temporary;
    final AssetCatalog.Result empty=new AssetCatalog.Result(List.of(),List.of(),List.of());
    LiveRevision revision(long number,WorldDocument d)throws Exception{return LiveRevision.prepare(number,d.id(),Map.of(d.id(),d),empty);}
    PreviewServer.Ack apply(PreviewServer s,LiveRevision r,PreviewServer.EditKind kind,long base)throws Exception{return s.edit(new PreviewServer.Edit(s.session(),base,r.number(),kind,r.bundle().start(),r)).get(8,TimeUnit.SECONDS);}
    void state(PreviewServer s,java.util.function.Predicate<PreviewServer.State> check)throws Exception{long end=System.nanoTime()+4_000_000_000L;while(!check.test(s.state())&&System.nanoTime()<end)Thread.sleep(5);assertTrue(check.test(s.state()),s.state().toString());}
    LocalClient client(PreviewServer s,String name)throws Exception{var c=new LocalClient(InetAddress.getByName("127.0.0.1"),s.port(),name);c.await(a->!a.actors().isEmpty(),Duration.ofSeconds(3));return c;}
    @Test void task178BaselineProfileAndCanonicalHashAreStable()throws Exception{
        var d=LiveScenarios.document(0);assertEquals(revision(0,d).hash(),revision(99,d).hash());assertEquals(12,revision(0,d).profile().actorRadius());assertEquals(22,revision(0,d).profile().sceneryHalfWidth());
        assertThrows(IllegalArgumentException.class,()->new SessionCollisionProfile("bad",1,0,22));assertThrows(IllegalArgumentException.class,()->new SessionCollisionProfile("bad",1,12,10));assertThrows(IllegalArgumentException.class,()->SessionCollisionProfile.read(Map.of("collision.offsetX","1")));
    }
    @Test void previewIsIsolatedFromRealMainServerAndSaves()throws Exception{
        var d=LiveScenarios.document(0);Path save=temporary.resolve("personal/save.properties");Files.createDirectories(save.getParent());Files.writeString(save,"do not modify");
        try(var main=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,new RpgSessionWorld())){main.start();try(var mainClient=new LocalClient(InetAddress.getLoopbackAddress(),main.port(),"Main A");var preview=new PreviewServer(revision(0,d),"reaper",1);var a=client(preview,"Preview A")){
            mainClient.await(s->s.actors().size()==1,Duration.ofSeconds(3));var original=main.snapshot().actors();assertNotEquals(main.port(),preview.port());
            var wall=d.withHitboxes(List.of(new WorldDocument.Hitbox("wall",400,180,20,240,true,"collision")));assertTrue(apply(preview,revision(1,wall),PreviewServer.EditKind.OBSTACLE,0).accepted());state(preview,s->s.revision().number()==1);
            assertEquals(original,main.snapshot().actors());assertEquals("do not modify",Files.readString(save));var dir=preview.temporary();preview.close();assertFalse(Files.exists(dir));assertTrue(main.running());assertTrue(mainClient.connected());
        }}
    }
    @Test void createMoveDeleteAndLayerUpdatesKeepPlayerState()throws Exception{
        var d=LiveScenarios.document(0);try(var s=new PreviewServer(revision(0,d),"human",20);var c=client(s,"Live A")){
            s.pause(true).get();state(s,v->v.paused());var original=s.state().players().get(c.id());
            var added=d.withHitboxes(List.of(new WorldDocument.Hitbox("wall",400,180,40,240,true,"collision")));assertTrue(apply(s,revision(1,added),PreviewServer.EditKind.OBSTACLE,0).accepted());
            var moved=d.withHitboxes(List.of(new WorldDocument.Hitbox("wall",500,180,1,240,true,"collision")));assertTrue(apply(s,revision(2,moved),PreviewServer.EditKind.OBJECT,1).accepted());assertTrue(apply(s,revision(3,d),PreviewServer.EditKind.OBSTACLE,2).accepted());
            var layer=d.layers().get(0);var layers=new ArrayList<>(d.layers());layers.set(0,new WorldDocument.Layer(layer.id(),"Changed",layer.role(),layer.order(),false,layer.locked()));assertTrue(apply(s,revision(4,d.withLayers(layers)),PreviewServer.EditKind.LAYER,3).accepted());state(s,v->v.revision().number()==4);
            var after=s.state().players().get(c.id());assertEquals(original.x(),after.x());assertEquals(original.y(),after.y());assertEquals(original.level(),after.level());assertEquals(original.state().inventory(),after.state().inventory());assertEquals(original.state().hp(),after.state().hp());
        }
    }
    @Test void oldSessionAndOutOfOrderEventsCannotMutateAndFullSyncRecovers()throws Exception{
        var d=LiveScenarios.document(0);try(var s=new PreviewServer(revision(0,d),"reaper",1)){
            assertFalse(s.edit(new PreviewServer.Edit(UUID.randomUUID(),0,1,PreviewServer.EditKind.FULL_SYNC,d.id(),revision(1,d))).get().accepted());
            assertTrue(apply(s,revision(2,d),PreviewServer.EditKind.FULL_SYNC,0).accepted());assertFalse(apply(s,revision(1,d),PreviewServer.EditKind.FULL_SYNC,0).accepted());assertFalse(apply(s,revision(3,d),PreviewServer.EditKind.TILE,0).accepted());assertTrue(apply(s,revision(3,d),PreviewServer.EditKind.FULL_SYNC,0).accepted());
        }
    }
    @Test void occupiedPlayerRejectsWallAndProfileGrowthAtomically()throws Exception{
        var d=LiveScenarios.document(0);try(var s=new PreviewServer(revision(0,d),"reaper",1);var a=client(s,"Live A");var b=client(s,"Live B")){
            b.send(Protocol.Action.MOVE,-1,0);Thread.sleep(170);b.send(Protocol.Action.MOVE,0,0);s.pause(true).get();state(s,v->v.paused());var before=s.state().players();
            var metadata=new TreeMap<>(d.metadata());metadata.putAll(new SessionCollisionProfile("grow",1,18,22).metadata());var reply=apply(s,revision(1,d.withMetadata(metadata)),PreviewServer.EditKind.PROFILE,0);assertFalse(reply.accepted());assertEquals(0,s.state().revision().number());
            var wall=d.withHitboxes(List.of(new WorldDocument.Hitbox("wall",165,300,10,40,true,"collision")));assertFalse(apply(s,revision(2,wall),PreviewServer.EditKind.OBSTACLE,0).accepted());assertEquals(before,s.state().players());assertNull(s.failure());
        }
    }
    @Test void profileChangesAreLiveAndReversibleWithoutProgressionChanges()throws Exception{
        var d=LiveScenarios.document(0);try(var s=new PreviewServer(revision(0,d),"aetheri",20);var c=client(s,"Live A")){
            s.pause(true).get();state(s,v->v.paused());var before=s.state().players().get(c.id());var metadata=new TreeMap<>(d.metadata());metadata.putAll(new SessionCollisionProfile("test",1,10,24).metadata());
            assertTrue(apply(s,revision(1,d.withMetadata(metadata)),PreviewServer.EditKind.PROFILE,0).accepted());state(s,v->v.revision().number()==1);assertEquals(10,s.state().revision().profile().actorRadius());assertEquals(before.state().xp(),s.state().players().get(c.id()).state().xp());
            assertTrue(apply(s,revision(2,d),PreviewServer.EditKind.PROFILE,1).accepted());state(s,v->v.revision().number()==2);assertEquals(12,s.state().revision().profile().actorRadius());assertEquals(before.x(),s.state().players().get(c.id()).x());
        }
    }
    @Test void invalidDimensionsSpawnAndProfileDoNotStartServers()throws Exception{
        var large=LiveScenarios.document(0).resized(1920,1280);assertThrows(Exception.class,()->revision(1,large));
        var d=LiveScenarios.document(0);assertThrows(Exception.class,()->revision(1,d.withMetadata(Map.of("collision.radius","100"))));assertThrows(Exception.class,()->revision(1,d.withHitboxes(List.of(new WorldDocument.Hitbox("wall",130,310,20,20,true,"collision")))));
    }
    @Test void publicGameplaySocketHasNoEditorOrAdministrativeCommand()throws Exception{
        var d=LiveScenarios.document(0);try(var s=new PreviewServer(revision(0,d),"reaper",1);var c=client(s,"Player A")){
            var before=s.state().revision().hash();assertTrue(c.send(Protocol.Action.MOVE,8192,0));Thread.sleep(100);assertEquals(140,s.state().players().get(c.id()).x());assertEquals(before,s.state().revision().hash());
            try(var socket=new Socket("127.0.0.1",s.port())){socket.setSoTimeout(2000);Protocol.write(socket.getOutputStream(),new Protocol.Hello(1,"Bad Admin"));assertInstanceOf(Protocol.Welcome.class,Protocol.read(socket.getInputStream()));Protocol.write(socket.getOutputStream(),new Protocol.WorldRequest(1,Protocol.WorldAction.TRAVEL,"edit"));Thread.sleep(100);assertEquals(before,s.state().revision().hash());}
        }
    }
    @Test void playerEnemyShunpoPortalAndResetUseRealRpgRules()throws Exception{
        var d=LiveScenarios.document(5);try(var s=new PreviewServer(revision(0,d),"reaper",20);var c=client(s,"Live A")){
            var result=s.command(c.id(),"shunpo").get();assertTrue(result.accepted());state(s,v->v.players().get(c.id()).x()>140);assertEquals(20,s.state().players().get(c.id()).level());assertTrue(s.state().players().get(c.id()).state().flags().stream().anyMatch(f->f.startsWith("ability-until:")));
            assertFalse(s.command(c.id(),"evade").get().accepted());s.reset("spirit",1).get();state(s,v->v.players().get(c.id()).level()==1);assertFalse(s.command(c.id(),"shunpo").get().accepted());
        }
        var portals=StudioExample.document().withPortals(List.of(new WorldDocument.Portal("near",140,320,"studio_tutorial",600,400,"","FRONT")));
        try(var s=new PreviewServer(revision(0,portals),"reaper",1);var c=client(s,"Live A")){assertTrue(s.command(c.id(),"portal").get().accepted());state(s,v->v.players().get(c.id()).x()==600);assertEquals(400,s.state().players().get(c.id()).y());}
    }
    @Test void publicationIsBlockedForProductionAndNeverWrites()throws Exception{
        var d=LiveScenarios.document(0);var store=new LivePublishing(temporary.resolve("published"));var result=store.publish(revision(1,d),"","",null,LivePublishing.Fault.NONE);assertFalse(result.active());assertTrue(result.message().contains("V3"));assertFalse(Files.exists(store.root()));
    }
    @Test void saveActivatesMapAndCollisionTogetherAndRetainsHistory()throws Exception{
        var d=LiveScenarios.document(0);var initial=revision(0,d);try(var target=new PreviewServer(initial,"reaper",20)){
            var store=new LivePublishing(temporary.resolve("published"));var metadata=new TreeMap<>(d.metadata());metadata.putAll(new SessionCollisionProfile("published",1,10,24).metadata());var next=revision(1,d.withMetadata(metadata));
            var saved=store.publish(next,"",initial.hash(),target,LivePublishing.Fault.NONE);assertTrue(saved.active());assertEquals(next.hash(),store.current());state(target,s->s.revision().hash().equals(next.hash()));assertEquals(10,target.state().revision().profile().actorRadius());
            var back=store.restore(saved.revision(),2,empty);assertEquals(next.hash(),back.hash());assertEquals(1,store.history().size());assertTrue(Files.readString(saved.revision().resolve("revision.properties")).contains("CONFIRMED"));
            assertFalse(store.publish(revision(3,d),"stale",next.hash(),target,LivePublishing.Fault.NONE).active());assertEquals(next.hash(),store.current());
            assertTrue(store.publish(revision(4,d),next.hash(),next.hash(),target,LivePublishing.Fault.NONE).active());assertEquals(next.hash(),Files.readString(store.root().resolve("previous.txt")));assertEquals(2,store.history().size());
        }
    }
    @Test void stagingAndActivationFailuresRollbackFilesAndServer()throws Exception{
        for(var fault:List.of(LivePublishing.Fault.AFTER_STAGE,LivePublishing.Fault.AFTER_ACTIVATION,LivePublishing.Fault.BEFORE_POINTER)){
            var d=LiveScenarios.document(0);var original=revision(0,d);try(var target=new PreviewServer(original,"reaper",1)){
                var store=new LivePublishing(temporary.resolve(fault.name()));var changed=d.withHitboxes(List.of(new WorldDocument.Hitbox("wall",500,200,10,200,true,"collision")));assertThrows(Exception.class,()->store.publish(revision(1,changed),"",original.hash(),target,fault));
                state(target,s->s.revision().hash().equals(original.hash())&&!s.paused());assertEquals("",store.current());assertTrue(store.history().isEmpty());assertFalse(Files.exists(store.root().resolve("pending.properties")));assertNull(target.failure());
            }
        }
    }
    @Test void incompatibleClientCannotReceivePublication()throws Exception{
        var d=LiveScenarios.document(0);var initial=revision(0,d);try(var target=new PreviewServer(initial,"reaper",1);var ordinary=client(target,"Ordinary")){
            var store=new LivePublishing(temporary.resolve("published"));assertFalse(store.publish(revision(1,d),"",initial.hash(),target,LivePublishing.Fault.NONE).active());assertEquals("",store.current());
            target.registerRenderer(ordinary.id());assertTrue(store.publish(revision(2,d),"",initial.hash(),target,LivePublishing.Fault.NONE).active());
        }
    }
    @Test void disconnectAndInterruptedJournalLeaveNoFalsePublication()throws Exception{
        var d=LiveScenarios.document(0);var initial=revision(0,d);var target=new PreviewServer(initial,"reaper",1);target.close();var store=new LivePublishing(temporary.resolve("published"));assertThrows(Exception.class,()->store.publish(revision(1,d),"",initial.hash(),target,LivePublishing.Fault.NONE));assertEquals("",store.current());
        var p=new Properties();p.setProperty("before","");p.setProperty("after",initial.hash());Files.write(store.root().resolve("pending.properties"),LivePublishing.properties(p));store.recoverPreviousPointer();assertFalse(Files.exists(store.root().resolve("pending.properties")));assertEquals("",store.current());
    }
    @Test void eightCollisionScenariosAndProfileSidecarRemainSgmapV1Compatible()throws Exception{
        for(int i=0;i<8;i++){var d=LiveScenarios.document(i);var copy=DocumentStore.decode(DocumentStore.encode(d));assertEquals(d,copy);assertNotNull(revision(i,copy));}
    }
    @Test void incrementalTilesUseBoundedDeltaAndDoNotShareImageCaches()throws Exception{
        Path png=temporary.resolve("tile.png");javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(16,16,2),"png",png.toFile());String hash=AssetCatalog.hash(png),id="a".repeat(64);var asset=AssetCatalog.verified(png,id,hash);var catalog=new AssetCatalog.Result(List.of(asset),List.of(),List.of());
        var d=LiveScenarios.document(0);var tile=new WorldDocument.Element("tile",WorldDocument.Kind.TILE,id,hash,new WorldDocument.Crop(0,0,16,16),32,32,32,32,"ground",true,false,"","FRONT",Map.of());var elements=new ArrayList<>(d.elements());elements.add(tile);var changed=d.withElements(elements);
        var delta=DraftPatch.between(d,changed);assertEquals(1,delta.changedRecords());assertEquals(changed,delta.apply(d));assertThrows(Exception.class,()->delta.apply(changed));
        var initial=LiveRevision.prepare(0,d.id(),Map.of(d.id(),d),catalog);try(var server=new PreviewServer(initial,"reaper",1)){
            var ack=server.patch(server.session(),0,1,delta,catalog).get();assertTrue(ack.accepted());state(server,s->s.revision().number()==1);assertNull(server.state().revision().bundle().assets().assets().get(0).thumbnail());
            var moved=changed.withElements(List.of(changed.elements().get(0),tile.at("tile",96,32)));assertTrue(server.patch(server.session(),1,2,DraftPatch.between(changed,moved),catalog).get().accepted());state(server,s->s.revision().number()==2);assertEquals(96,server.state().revision().bundle().maps().get(d.id()).elements().get(1).x());
            assertTrue(server.patch(server.session(),2,3,DraftPatch.between(moved,d),catalog).get().accepted());assertFalse(server.patch(server.session(),0,4,delta,catalog).get().accepted());
        }
        var a=new AssetImages();var b=new AssetImages();assertNotSame(a.load(asset),b.load(asset));a.clear();assertTrue(b.bytes()>0);
    }
    @Test void reservedServerRejectsMutationAndDisconnectAbortsActivation()throws Exception{
        var d=LiveScenarios.document(0);var base=revision(0,d);try(var server=new PreviewServer(base,"reaper",1);var client=client(server,"Live A")){
            server.registerRenderer(client.id());assertTrue(server.reserve(base.hash(),revision(1,d)).get().accepted());assertFalse(server.pause(false).get().accepted());assertFalse(server.command(client.id(),"attack").get().accepted());assertFalse(server.spawn(client.id()).get().accepted());
            client.close();state(server,s->s.players().isEmpty());assertThrows(Exception.class,()->server.activateReserved().get());assertEquals(base.hash(),server.state().revision().hash());
        }
    }
    @Test void publishedFileCorruptionIsDetectedBeforeRecoveryOrActivation()throws Exception{
        var d=LiveScenarios.document(0);var base=revision(0,d);try(var target=new PreviewServer(base,"reaper",1)){
            var store=new LivePublishing(temporary.resolve("published"));var saved=store.publish(revision(1,d),"",base.hash(),target,LivePublishing.Fault.NONE);Files.writeString(saved.revision().resolve(d.id()+".sgmap"),"broken");assertThrows(Exception.class,store::current);assertThrows(Exception.class,()->store.restore(saved.revision(),2,empty));
        }
    }
}
