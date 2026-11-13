package com.projetok.studio;

import com.projetok.multiplayer.*;
import com.projetok.prototype.rpg.LauncherSession;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Confirma edição da partida original, sincronização e separação entre memória e persistência. */
class LiveMapEditsTest {
    @TempDir Path root;
    static final AssetCatalog.Result EMPTY=new AssetCatalog.Result(List.of(),List.of(),List.of());
    static void await(BooleanSupplier test)throws Exception{long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(9);while(!test.getAsBoolean()&&System.nanoTime()<end)Thread.sleep(10);assertTrue(test.getAsBoolean());}
    static LocalClient client(LocalSimulationServer server,String name)throws Exception{var c=LocalClient.revision(InetAddress.getLoopbackAddress(),server.port(),name,false,false,null,EMPTY);c.revisions().renderer(p->CompletableFuture.completedFuture(null));return c;}
    @Test void editsSameServerAndBothClientsWithoutWritingPublicationHistory()throws Exception{
        try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,root);var a=client(server,"Editor A");var b=client(server,"Jogador B")){
            await(()->!a.revisions().blocked()&&!b.revisions().blocked()&&server.players()==2);var managed=LauncherSession.managed(server);var base=managed.target().revision();int aId=a.id(),bId=b.id();
            var map=base.bundle().maps().get("forest");map=CollisionTools.setCell(map,new java.awt.Point(640,448),14);
            var next=LiveMapEdits.replace(base,map,EMPTY,1);assertTrue(LiveMapEdits.apply(managed.target(),base.hash(),next,()->false).accepted());
            await(()->!a.revisions().blocked()&&!b.revisions().blocked());assertEquals(next.hash(),a.revisions().hash());assertEquals(next.hash(),b.revisions().hash());assertEquals(aId,a.id());assertEquals(bId,b.id());assertEquals(2,server.players());
            assertFalse(Files.exists(managed.store().root()));assertEquals(14,a.revisions().active().revision().bundle().maps().get("forest").hitboxes().stream().filter(h->h.id().equals("cell_collision_640_448")).findFirst().orElseThrow().edges());
            var saved=managed.store().publish(LiveRevision.prepare(2,next.bundle().start(),next.bundle().maps(),EMPTY),"",next.hash(),managed.target(),LivePublishing.Fault.NONE);assertTrue(saved.active());assertEquals(next.hash(),managed.store().current());
        }
    }
    @Test void newBlankSceneAndVisitKeepOriginalConnectionAndOtherMaps()throws Exception{
        try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,root);var a=client(server,"Editor")){
            await(()->!a.revisions().blocked());var target=LauncherSession.managed(server).target();var base=target.revision();
            var empty=FileTools.playable(WorldDocument.empty("novo_mapa",960,640));
            var next=LiveMapEdits.replace(base,empty,EMPTY,1);assertEquals(Set.of("forest","novo_mapa"),next.bundle().maps().keySet());
            assertTrue(LiveMapEdits.apply(target,base.hash(),next,()->false).accepted());await(()->!a.revisions().blocked());
            assertTrue(target.visitEditorMap(a.id(),"novo_mapa").get(3,TimeUnit.SECONDS).accepted());await(()->a.region().equals("village"));assertEquals(1,server.players());assertTrue(a.connected());
            assertEquals("novo_mapa",next.bundle().maps().get("forest").portals().get(0).destination());assertTrue(next.bundle().maps().get("novo_mapa").elements().stream().noneMatch(e->e.kind()==WorldDocument.Kind.TILE||e.kind()==WorldDocument.Kind.OBJECT));
        }
    }
    @Test void cancellationAfterReservationRecoversSameWorld()throws Exception{
        try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,root);var a=client(server,"Editor")){
            await(()->!a.revisions().blocked());var target=LauncherSession.managed(server).target();var base=target.revision();var next=LiveMapEdits.replace(base,CollisionTools.setCell(base.bundle().maps().get("forest"),new java.awt.Point(640,448),14),EMPTY,1);
            var checks=new java.util.concurrent.atomic.AtomicInteger();assertThrows(CancellationException.class,()->LiveMapEdits.apply(target,base.hash(),next,()->checks.incrementAndGet()>1));await(()->!a.revisions().blocked());assertEquals(base.hash(),target.activeHash());assertEquals(base.hash(),a.revisions().hash());
        }
    }
    @Test void rejectedConcurrentEditDoesNotAbortAnotherReservation()throws Exception{
        try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,root);var a=client(server,"Editor")){
            await(()->!a.revisions().blocked());var target=LauncherSession.managed(server).target();var base=target.revision();var next=LiveMapEdits.replace(base,CollisionTools.setCell(base.bundle().maps().get("forest"),new java.awt.Point(640,448),14),EMPTY,1);
            assertTrue(target.reserve(base.hash(),next).get(4,TimeUnit.SECONDS).accepted());
            assertFalse(LiveMapEdits.apply(target,base.hash(),LiveRevision.prepare(2,next.bundle().start(),next.bundle().maps(),EMPTY),()->false).accepted());
            assertEquals(RevisionCoordinator.Phase.READY,target.status().phase());assertTrue(target.abortReserved().get(4,TimeUnit.SECONDS).accepted());await(()->!a.revisions().blocked());assertEquals(base.hash(),target.activeHash());
        }
    }
    @Test void occupiedPlayerRejectsCollisionWithoutChangingSharedMap()throws Exception{
        try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,root);var a=client(server,"Editor")){
            await(()->!a.revisions().blocked()&&a.snapshot()!=null);a.send(Protocol.Action.MOVE,0,-1);await(()->a.snapshot().actors().stream().anyMatch(p->p.id()==a.id()&&p.y()<250));a.send(Protocol.Action.MOVE,0,0);
            var target=LauncherSession.managed(server).target();var base=target.revision();var map=base.bundle().maps().get("forest").withHitboxes(List.of(new WorldDocument.Hitbox("occupied",110,100,60,180,true,"collision")));
            var next=LiveMapEdits.replace(base,map,EMPTY,1);assertFalse(LiveMapEdits.apply(target,base.hash(),next,()->false).accepted());assertEquals(base.hash(),target.activeHash());assertEquals(base.hash(),a.revisions().hash());assertTrue(a.connected());
        }
    }
}
