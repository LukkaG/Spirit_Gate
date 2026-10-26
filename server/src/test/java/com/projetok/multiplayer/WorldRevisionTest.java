package com.projetok.multiplayer;

import com.projetok.studio.*;
import com.projetok.prototype.rpg.*;
import com.projetok.domain.movement.SessionCollisionProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;

class WorldRevisionTest {
    @TempDir Path root;
    static final AssetCatalog.Result EMPTY=new AssetCatalog.Result(List.of(),List.of(),List.of());
    static final class RawPeer implements AutoCloseable {
        final Socket socket;final Thread reader;volatile boolean initial=true,resumed,ready=true,activate=true;volatile long tx;volatile String hash;
        RawPeer(LocalSimulationServer server)throws Exception{socket=new Socket(InetAddress.getLoopbackAddress(),server.port());socket.setSoTimeout(4000);send(new Protocol.Hello(4,"Falha controlada"));assertInstanceOf(Protocol.Welcome.class,Protocol.read(socket.getInputStream()));reader=new Thread(()->{try{while(!socket.isClosed()){var message=Protocol.read(socket.getInputStream());if(message instanceof Protocol.Content c){if(c.kind()==Protocol.ContentKind.BEGIN){tx=c.transaction();hash=c.hash();}if(c.kind()==Protocol.ContentKind.END&&(initial||ready))ack(Protocol.ContentKind.READY,c.transaction(),c.hash());if(c.kind()==Protocol.ContentKind.ACTIVATE&&(initial||activate))ack(Protocol.ContentKind.ACTIVE,c.transaction(),c.hash());if(c.kind()==Protocol.ContentKind.ABORT)ack(Protocol.ContentKind.ACTIVE,c.transaction(),c.hash());if(c.kind()==Protocol.ContentKind.RESUME){initial=false;resumed=true;}}}}catch(Exception ignored){}});reader.setDaemon(true);reader.start();}
        synchronized void send(Protocol.Message message)throws Exception{Protocol.write(socket.getOutputStream(),message);}
        void ack(Protocol.ContentKind kind,long tx,String hash)throws Exception{send(new Protocol.Content(kind,tx,hash,0,new byte[0]));}
        public void close()throws Exception{socket.close();reader.interrupt();}
    }
    interface Check {boolean get()throws Exception;}
    static void waitFor(Check check,LocalClient... clients)throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(7);while(!check.get()&&System.nanoTime()<until){for(var c:clients)c.measureLatency();Thread.sleep(10);}assertTrue(check.get(),"Condition timed out");}
    LocalClient client(LocalSimulationServer server,String name)throws Exception{var c=LocalClient.revision(InetAddress.getLoopbackAddress(),server.port(),name,false,false,null,EMPTY);c.revisions().renderer(p->CompletableFuture.completedFuture(null));return c;}
    LiveRevision changed(long number,int x)throws Exception{var base=WorldContent.baseline();var maps=new TreeMap<>(base.bundle().maps());var d=maps.get("forest");maps.put(d.id(),d.withHitboxes(List.of(new WorldDocument.Hitbox("central_gray",x,180,120,120,true,"collision"))).withMetadata(new SessionCollisionProfile("published",1,10,24).metadata()));return LiveRevision.prepare(number,"forest",maps,EMPTY);}
    @Test void negotiatedTcpClientsPublishAndRollbackRealRpgWorld()throws Exception{
        try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,root);var a=client(server,"Jogador A");var b=client(server,"Jogador B")){
            var managed=LauncherSession.managed(server);var target=managed.target();String before=target.activeHash();waitFor(()->!a.revisions().blocked()&&!b.revisions().blocked(),a,b);
            var next=changed(1,500);assertEquals(before,target.activeHash());assertEquals(before,a.revisions().hash());assertFalse(Files.exists(managed.store().root()));
            var result=managed.store().publish(next,"",before,target,LivePublishing.Fault.NONE);assertTrue(result.active(),result.message());waitFor(()->!a.revisions().blocked()&&!b.revisions().blocked(),a,b);
            assertEquals(next.hash(),target.activeHash());assertEquals(next.hash(),a.revisions().hash());assertEquals(next.hash(),b.revisions().hash());assertEquals(10,a.revisions().active().revision().profile().actorRadius());
            waitFor(()->a.snapshot()!=null&&b.snapshot()!=null,a,b);assertTrue(a.send(Protocol.Action.MOVE,0,1));assertTrue(b.send(Protocol.Action.ATTACK,0,0));
            var baseline=WorldContent.baseline();var back=LiveRevision.prepare(2,"forest",baseline.bundle().maps(),EMPTY);var rollback=managed.store().publish(back,next.hash(),next.hash(),target,LivePublishing.Fault.NONE);assertTrue(rollback.active());waitFor(()->!a.revisions().blocked()&&!b.revisions().blocked(),a,b);assertEquals(before,a.revisions().hash());assertEquals(before,b.revisions().hash());assertEquals(3,managed.store().history().size());assertNull(server.failure());
        }
    }
    @Test void activationFailureRecoversBothClientRenderSlotsAndFiles()throws Exception{
        try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,root);var a=client(server,"Jogador A");var b=client(server,"Jogador B")){
            var m=LauncherSession.managed(server);waitFor(()->!a.revisions().blocked()&&!b.revisions().blocked(),a,b);String old=m.target().activeHash();
            assertThrows(Exception.class,()->m.store().publish(changed(1,500),"",old,m.target(),LivePublishing.Fault.AFTER_ACTIVATION));waitFor(()->!a.revisions().blocked()&&!b.revisions().blocked(),a,b);
            assertEquals(old,m.target().activeHash());assertEquals(old,a.revisions().hash());assertEquals(old,b.revisions().hash());assertEquals("",m.store().current());assertEquals(1,m.store().history().size());
        }
    }
    @Test void historicalV1AndV3BlockWithoutBeingKicked()throws Exception{
        for(int application:List.of(1,3))try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,Files.createDirectories(root.resolve("v"+application)));var legacy=application==1?new LocalClient(InetAddress.getLoopbackAddress(),server.port(),"Antigo"):LocalClient.combat(InetAddress.getLoopbackAddress(),server.port(),"Antigo",false)){
            legacy.await(s->!s.actors().isEmpty(),java.time.Duration.ofSeconds(3));var m=LauncherSession.managed(server);var result=m.store().publish(changed(1,500),"",m.target().activeHash(),m.target(),LivePublishing.Fault.NONE);assertFalse(result.active());assertTrue(result.message().contains("Cliente"));assertTrue(legacy.connected());assertFalse(Files.exists(m.store().root().resolve("active.txt")));
        }
    }
    @Test void historicalV2BlocksAndKeepsItsOriginalMessageType()throws Exception{
        var world=new RpgSessionWorld(List.of("reaper","human"),CosmeticCatalog.metadata(Path.of("../assets/dev/generated/corujao-iv-v1")),true);world.enableRevisions(WorldContent.baseline());
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world)){server.start();try(var old=LocalClient.cosmetics(InetAddress.getLoopbackAddress(),server.port(),"Antigo",false)){old.await(s->!s.actors().isEmpty(),java.time.Duration.ofSeconds(3));var result=world.revisions().reserve(world.contentHash(),changed(1,500)).get(5,TimeUnit.SECONDS);assertFalse(result.accepted());assertTrue(old.connected());}}
    }
    @Test void publicationRejectsOccupiedPlayerAndStaleBase()throws Exception{
        try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,root);var a=client(server,"Jogador A")){
            waitFor(()->!a.revisions().blocked()&&a.snapshot()!=null,a);var m=LauncherSession.managed(server);var old=m.target().activeHash();assertFalse(m.target().reserve("0".repeat(64),changed(1,500)).get(3,TimeUnit.SECONDS).accepted());
            var base=WorldContent.baseline();var d=base.bundle().maps().get("forest");var player=server.snapshot().actors().get(0);var maps=new TreeMap<>(base.bundle().maps());maps.put("forest",d.withHitboxes(List.of(new WorldDocument.Hitbox("occupied",(int)player.x()-4,(int)player.y()-4,8,8,true,"collision"))));
            assertThrows(Exception.class,()->LiveRevision.prepare(1,"forest",maps,EMPTY));assertEquals(old,m.target().activeHash());
        }
    }
    @Test void reservedContentCannotBeMutatedAndConcurrentPublicationIsRejected()throws Exception{
        try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,root);var a=client(server,"Jogador A")){
            waitFor(()->!a.revisions().blocked(),a);var target=LauncherSession.managed(server).target();assertTrue(target.reserve(target.activeHash(),changed(1,500)).get(5,TimeUnit.SECONDS).accepted());
            assertFalse(target.reserve(target.activeHash(),changed(2,550)).get(5,TimeUnit.SECONDS).accepted());assertEquals(RevisionCoordinator.Phase.READY,target.status().phase());assertTrue(target.abortReserved().get(5,TimeUnit.SECONDS).accepted());waitFor(()->!a.revisions().blocked(),a);
        }
    }
    @Test void guestCannotObtainAdministrativeCapability()throws Exception{
        try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,root)){System.setProperty("spiritgate.guest","true");try{assertThrows(IllegalStateException.class,()->LauncherSession.managed(server));assertThrows(IllegalStateException.class,()->LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,root));}finally{System.clearProperty("spiritgate.guest");}}
    }
    @Test void canonicalDocumentsRoundTripWithoutSessionDataAndRejectTampering()throws Exception{
        var revision=WorldContent.baseline();byte[] bytes=WorldContent.encode(revision);assertEquals(revision.hash(),WorldContent.decode(bytes,EMPTY).hash());assertArrayEquals(bytes,WorldContent.encode(WorldContent.baseline()));bytes[14]=bytes[14]=='a'?(byte)'b':(byte)'a';assertThrows(Exception.class,()->WorldContent.decode(bytes,EMPTY));assertThrows(Exception.class,()->WorldContent.decode(new byte[WorldContent.LIMIT+1],EMPTY));
    }
    @Test void historicalEnvelopeAndNewFramesAreStrictlyBounded()throws Exception{
        var buffer=new ByteArrayOutputStream();Protocol.write(buffer,new Protocol.State(7,List.of()));assertEquals("0000000b0104000000000000000700",HexFormat.of().formatHex(buffer.toByteArray()));
        var content=new Protocol.Content(Protocol.ContentKind.CHUNK,1,"a".repeat(64),0,new byte[2800]);buffer.reset();Protocol.write(buffer,content);assertTrue(buffer.size()<Protocol.MAX_FRAME);assertInstanceOf(Protocol.Content.class,Protocol.read(new ByteArrayInputStream(buffer.toByteArray())));byte[] truncated=Arrays.copyOf(buffer.toByteArray(),buffer.size()-1);assertThrows(IOException.class,()->Protocol.read(new ByteArrayInputStream(truncated)));assertThrows(IllegalArgumentException.class,()->new Protocol.Content(Protocol.ContentKind.CHUNK,1,"a".repeat(64),0,new byte[2801]));
    }
    @Test void outOfOrderTransferNeverCreatesFilesOrPreparesRenderer()throws Exception{
        try(var sync=new ClientRevisionSync(EMPTY,m->{},()->{})){var begin=new Protocol.Content(Protocol.ContentKind.BEGIN,1,"a".repeat(64),100,new byte[0]);sync.receive(begin);assertThrows(IOException.class,()->sync.receive(new Protocol.Content(Protocol.ContentKind.CHUNK,1,"a".repeat(64),20,new byte[10])));assertNull(sync.active());assertTrue(sync.blocked());}
    }
    @Test void disconnectAfterReadyAndDuringActivationRecoversRemainingPlayer()throws Exception{
        for(boolean activated:List.of(false,true))try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,Files.createDirectories(root.resolve("disconnect"+activated)));var a=client(server,"Jogador A");var b=client(server,"Jogador B")){
            waitFor(()->!a.revisions().blocked()&&!b.revisions().blocked(),a,b);var target=LauncherSession.managed(server).target();String old=target.activeHash();assertTrue(target.reserve(old,changed(1,500)).get(5,TimeUnit.SECONDS).accepted());if(activated)assertTrue(target.activateReserved().get(5,TimeUnit.SECONDS).accepted());a.close();
            waitFor(()->target.status().phase()==RevisionCoordinator.Phase.ABORTED&&!b.revisions().blocked(),b);assertEquals(old,target.activeHash());assertEquals(old,b.revisions().hash());assertEquals(1,server.players());
        }
    }
    @Test void missingClientAssetRejectsCandidateWithoutFallbackAndKeepsDraft()throws Exception{
        Path image=root.resolve("assets/fixture.png");Files.createDirectories(image.getParent());javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(32,32,2),"png",image.toFile());var assets=AssetCatalog.scan(root,null);var asset=assets.assets().get(0);
        try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,root);var a=client(server,"Jogador A")){
            waitFor(()->!a.revisions().blocked(),a);var m=LauncherSession.managed(server);String old=m.target().activeHash();var maps=new TreeMap<>(WorldContent.baseline().bundle().maps());var d=maps.get("forest");var elements=new ArrayList<>(d.elements());elements.add(new WorldDocument.Element("fixture",WorldDocument.Kind.TILE,asset.id(),asset.sha(),new WorldDocument.Crop(0,0,32,32),64,64,32,32,"ground",true,false,"","FRONT",Map.of()));maps.put("forest",d.withElements(elements));var next=LiveRevision.prepare(1,"forest",maps,assets);
            var result=m.store().publish(next,"",old,m.target(),LivePublishing.Fault.NONE);assertFalse(result.active());waitFor(()->!a.revisions().blocked(),a);assertEquals(old,a.revisions().hash());assertEquals(old,m.target().activeHash());assertEquals("",m.store().current());assertEquals(3,maps.get("forest").elements().size());
        }
    }
    @Test void restartLoadsOnlyConfirmedRevisionAndOldClientCannotJoinAuthoredWorld()throws Exception{
        String hash;try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,root);var a=client(server,"Jogador A")){
            waitFor(()->!a.revisions().blocked(),a);var m=LauncherSession.managed(server);var result=m.store().publish(changed(1,500),"",m.target().activeHash(),m.target(),LivePublishing.Fault.NONE);assertTrue(result.active());hash=result.hash();
        }
        try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,root)){
            assertEquals(hash,LauncherSession.managed(server).target().activeHash());assertThrows(IOException.class,()->LocalClient.combat(InetAddress.getLoopbackAddress(),server.port(),"Antigo",false));try(var a=client(server,"Atualizado")){waitFor(()->!a.revisions().blocked(),a);assertEquals(hash,a.revisions().hash());}
        }
    }
    @Test void stagingFailurePreservesPreviousPointerAndClients()throws Exception{
        try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,root);var a=client(server,"Jogador A")){
            waitFor(()->!a.revisions().blocked(),a);var m=LauncherSession.managed(server);String old=m.target().activeHash();assertThrows(Exception.class,()->m.store().publish(changed(1,500),"",old,m.target(),LivePublishing.Fault.AFTER_STAGE));waitFor(()->!a.revisions().blocked(),a);assertEquals(old,m.target().activeHash());assertEquals(old,a.revisions().hash());assertFalse(Files.exists(m.store().root().resolve("pending.properties")));assertEquals("",m.store().current());
        }
    }
    @Test void ordinaryGameplaySocketCannotStartPublicationEvenWithContentOpcodes()throws Exception{
        try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,root);var a=client(server,"Jogador A");var socket=new Socket(InetAddress.getLoopbackAddress(),server.port())){
            waitFor(()->!a.revisions().blocked(),a);Protocol.write(socket.getOutputStream(),new Protocol.Hello(4,"Invasor"));assertInstanceOf(Protocol.Welcome.class,Protocol.read(socket.getInputStream()));String hash=LauncherSession.managed(server).target().activeHash();
            Protocol.write(socket.getOutputStream(),new Protocol.Content(Protocol.ContentKind.BEGIN,999,hash,100,new byte[0]));Thread.sleep(100);assertEquals(hash,LauncherSession.managed(server).target().activeHash());assertFalse(Files.exists(LauncherSession.managed(server).store().root()));assertNull(server.failure());
        }
    }
    @Test void recursiveAndTruncatedDescriptorsAreRejectedWithoutFiles()throws Exception{
        byte[] bytes=WorldContent.encode(WorldContent.baseline());for(int n:List.of(0,1,12,bytes.length-1))assertThrows(Exception.class,()->WorldContent.decode(Arrays.copyOf(bytes,n),EMPTY));
        try(var sync=new ClientRevisionSync(EMPTY,m->{},()->{})){sync.receive(new Protocol.Content(Protocol.ContentKind.BEGIN,1,"a".repeat(64),10,new byte[0]));assertThrows(IOException.class,()->sync.receive(new Protocol.Content(Protocol.ContentKind.END,1,"a".repeat(64),0,new byte[0])));assertNull(sync.active());}
    }
    @Test void preparationAndActivationTimeoutsRecoverOverTcp()throws Exception{
        for(boolean preparation:List.of(true,false)){
            System.setProperty("spiritgate.content.timeoutMillis","1000");LocalSimulationServer started;try{started=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,Files.createDirectories(root.resolve("timeout"+preparation)));}finally{System.clearProperty("spiritgate.content.timeoutMillis");}
            try(var server=started;var raw=new RawPeer(server)){
                waitFor(()->raw.resumed);raw.ready=!preparation;raw.activate=preparation;var target=LauncherSession.managed(server).target();String old=target.activeHash();var reserved=target.reserve(old,changed(1,500)).get(6,TimeUnit.SECONDS);
                if(preparation)assertFalse(reserved.accepted());else{assertTrue(reserved.accepted());assertFalse(target.activateReserved().get(6,TimeUnit.SECONDS).accepted());}
                waitFor(()->target.status().phase()==RevisionCoordinator.Phase.ABORTED);assertEquals(old,target.activeHash());assertTrue(target.status().detail().contains("Timeout"));assertNull(server.failure());
            }
        }
    }
    @Test void delayedWrongAndDuplicateAcksNeverAuthorizeAnotherRevision()throws Exception{
        try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,root);var raw=new RawPeer(server)){
            waitFor(()->raw.resumed);raw.ready=false;var target=LauncherSession.managed(server).target();String old=target.activeHash();long initial=raw.tx;var next=changed(1,500);var pending=target.reserve(old,next);waitFor(()->target.status().phase()==RevisionCoordinator.Phase.WAITING_CLIENTS&&raw.tx>initial);
            raw.ack(Protocol.ContentKind.READY,initial,old);raw.ack(Protocol.ContentKind.READY,raw.tx,"0".repeat(64));Thread.sleep(100);assertFalse(pending.isDone());assertEquals(old,target.activeHash());
            raw.ack(Protocol.ContentKind.READY,raw.tx,next.hash());raw.ack(Protocol.ContentKind.READY,raw.tx,next.hash());assertTrue(pending.get(4,TimeUnit.SECONDS).accepted());assertTrue(target.abortReserved().get(4,TimeUnit.SECONDS).accepted());assertEquals(old,target.activeHash());
        }
    }
    @Test void newJoinIsBlockedDuringPublicationAndServerShutdownFailsPendingTransaction()throws Exception{
        try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,root);var raw=new RawPeer(server)){
            waitFor(()->raw.resumed);raw.ready=false;var target=LauncherSession.managed(server).target();var pending=target.reserve(target.activeHash(),changed(1,500));waitFor(()->target.status().phase()==RevisionCoordinator.Phase.WAITING_CLIENTS);assertThrows(IOException.class,()->new LocalClient(InetAddress.getLoopbackAddress(),server.port(),"Recem chegado"));server.close();assertThrows(ExecutionException.class,()->pending.get(2,TimeUnit.SECONDS));
        }
    }
    @Test void incompleteJournalBlocksStartupUntilExplicitRecovery()throws Exception{
        var store=new LivePublishing(LauncherSession.revisionRoot(root,0));Files.createDirectories(store.root());Files.writeString(store.root().resolve("pending.properties"),"before=\nafter="+"a".repeat(64)+"\n");assertThrows(IllegalStateException.class,()->LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,root));store.recoverPreviousPointer();assertEquals("",store.current());try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,root)){assertTrue(server.running());}try(var files=Files.list(store.root())){assertTrue(files.anyMatch(p->p.getFileName().toString().startsWith("recovered-")));}
    }
    @Test void movingEnemyCannotBeCoveredByANewWall()throws Exception{
        try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,root);var a=client(server,"Jogador A")){
            waitFor(()->!a.revisions().blocked()&&a.snapshot()!=null,a);a.send(Protocol.Action.MOVE,1,0);Thread.sleep(100);for(int i=0;i<5;i++){a.send(Protocol.Action.MOVE,-1,0);Thread.sleep(50);a.send(Protocol.Action.MOVE,0,0);Thread.sleep(100);}waitFor(()->a.snapshot().actors().stream().anyMatch(e->e.boss()&&e.id()<2_000_000&&e.x()<200),a);var enemy=a.snapshot().actors().stream().filter(e->e.boss()&&e.id()<2_000_000).findFirst().orElseThrow();
            var maps=new TreeMap<>(WorldContent.baseline().bundle().maps());var forest=maps.get("forest");var markers=new ArrayList<>(forest.elements());markers.set(0,markers.get(0).at("player",320,500));maps.put("forest",forest.withElements(markers).withPortals(List.of(new WorldDocument.Portal("to_village",320,500,"village",140,320,"","FRONT"))).withHitboxes(List.of(new WorldDocument.Hitbox("enemy_occupied",(int)enemy.x()-2,(int)enemy.y()-2,4,4,true,"collision"))));maps.put("village",maps.get("village").withPortals(List.of(new WorldDocument.Portal("to_forest",140,320,"forest",320,500,"","FRONT"))));var next=LiveRevision.prepare(1,"forest",maps,EMPTY);var target=LauncherSession.managed(server).target();String old=target.activeHash();var result=target.reserve(old,next).get(4,TimeUnit.SECONDS);assertFalse(result.accepted());assertEquals(old,target.activeHash());
        }
    }
    @Test void historicalV3PortalAliasStillWorksOnLauncherBaseline()throws Exception{
        try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,root);var old=LocalClient.combat(InetAddress.getLoopbackAddress(),server.port(),"Antigo",false)){
            old.await(s->!s.actors().isEmpty(),java.time.Duration.ofSeconds(3));assertTrue(old.request(Protocol.WorldAction.TRAVEL,"to_village"));waitFor(()->old.region().equals("village"),old);assertTrue(old.connected());
        }
    }
    @Test void lateGameplayCommandsAreRejectedByTheirRevisionTag()throws Exception{
        try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,root);var raw=new RawPeer(server)){
            waitFor(()->raw.resumed);var target=LauncherSession.managed(server).target();String old=target.activeHash();var next=changed(1,500);assertTrue(target.reserve(old,next).get(5,TimeUnit.SECONDS).accepted());assertTrue(target.activateReserved().get(5,TimeUnit.SECONDS).accepted());assertTrue(target.confirmReserved().get(5,TimeUnit.SECONDS).accepted());float x=server.snapshot().actors().get(0).x();
            raw.send(new Protocol.RevisionCommand(old,new Protocol.Intent(1,Protocol.Action.MOVE,1,0)));Thread.sleep(150);assertEquals(x,server.snapshot().actors().get(0).x());raw.send(new Protocol.RevisionCommand(next.hash(),new Protocol.Intent(2,Protocol.Action.MOVE,1,0)));waitFor(()->server.snapshot().actors().get(0).x()>x);assertThrows(IllegalArgumentException.class,()->new Protocol.RevisionCommand(next.hash(),new Protocol.Bye()));
        }
    }
    @Test void traversalNamesCannotResolveAssetsOrWriteOutsideTheRevisionStore()throws Exception{
        assertThrows(Exception.class,()->DocumentStore.decode(DocumentStore.encode(WorldDocument.empty("../escape",960,640))));
        assertThrows(Exception.class,()->{var base=WorldContent.baseline();var maps=new TreeMap<>(base.bundle().maps());var d=maps.get("forest");var elements=new ArrayList<>(d.elements());elements.add(new WorldDocument.Element("outside",WorldDocument.Kind.TILE,"../../escape.png","a".repeat(64),new WorldDocument.Crop(0,0,32,32),64,64,32,32,"ground",true,false,"","FRONT",Map.of()));maps.put("forest",d.withElements(elements));LiveRevision.prepare(1,"forest",maps,EMPTY);});assertFalse(Files.exists(root.resolve("escape.png")));
    }
    @Test void studioCancellationBeforeReserveAfterReadyAndAfterActivationCannotCommit()throws Exception{
        for(String phase:List.of("BEFORE","READY","VERIFYING"))try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,Files.createDirectories(root.resolve(phase)));var a=client(server,"Jogador A")){
            waitFor(()->!a.revisions().blocked(),a);var managed=LauncherSession.managed(server);String old=managed.target().activeHash();
            assertThrows(CancellationException.class,()->managed.store().publish(changed(1,500),"",old,managed.target(),LivePublishing.Fault.NONE,()->phase.equals("BEFORE")||managed.target().status().phase().name().equals(phase)));
            waitFor(()->!a.revisions().blocked(),a);assertEquals(old,managed.target().activeHash());assertEquals(old,a.revisions().hash());assertEquals("",managed.store().current());assertFalse(Files.exists(managed.store().root().resolve("active.txt")));
        }
    }
    @Test void rollbackToSameHashCannotReplayAnOlderSnapshot()throws Exception{
        var baseline=WorldContent.baseline();var next=changed(1,500);var fresh=new CountDownLatch(1);var oldSent=new CountDownLatch(1);var failure=new java.util.concurrent.atomic.AtomicReference<Throwable>();
        try(var listener=new ServerSocket(0,2,InetAddress.getLoopbackAddress())){
            var peer=new Thread(()->{try(var socket=listener.accept()){
                assertEquals(4,((Protocol.Hello)Protocol.read(socket.getInputStream())).version());Protocol.write(socket.getOutputStream(),new Protocol.Welcome(1));
                for(int round=1;round<=3;round++){
                    var revision=round==2?next:baseline;byte[] bytes=WorldContent.encode(revision);String base=round==1?"":round==2?baseline.hash():next.hash();
                    Protocol.write(socket.getOutputStream(),new Protocol.Content(Protocol.ContentKind.BEGIN,round,revision.hash(),bytes.length,base.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                    for(int offset=0;offset<bytes.length;offset+=2800)Protocol.write(socket.getOutputStream(),new Protocol.Content(Protocol.ContentKind.CHUNK,round,revision.hash(),offset,Arrays.copyOfRange(bytes,offset,Math.min(bytes.length,offset+2800))));
                    Protocol.write(socket.getOutputStream(),new Protocol.Content(Protocol.ContentKind.END,round,revision.hash(),0,new byte[0]));assertEquals(Protocol.ContentKind.READY,((Protocol.Content)Protocol.read(socket.getInputStream())).kind());
                    Protocol.write(socket.getOutputStream(),new Protocol.Content(Protocol.ContentKind.ACTIVATE,round,revision.hash(),0,new byte[0]));assertEquals(Protocol.ContentKind.ACTIVE,((Protocol.Content)Protocol.read(socket.getInputStream())).kind());Protocol.write(socket.getOutputStream(),new Protocol.Content(Protocol.ContentKind.RESUME,round,revision.hash(),0,new byte[0]));
                    var actor=new Protocol.Actor(1,"Viajante",round==2?150:140,300,100,100,0,0,false);var state=new Protocol.State(round==2?101:100,List.of(actor));Protocol.write(socket.getOutputStream(),new Protocol.RevisionState(revision.hash(),new Protocol.CombatState(new Protocol.SessionState(state,List.of()),List.of())));
                    if(round==3){oldSent.countDown();assertTrue(fresh.await(3,TimeUnit.SECONDS));Protocol.write(socket.getOutputStream(),new Protocol.RevisionState(revision.hash(),new Protocol.CombatState(new Protocol.SessionState(new Protocol.State(102,List.of(actor)),List.of()),List.of())));Thread.sleep(200);}else Thread.sleep(150);
                }
            }catch(Throwable e){failure.set(e);oldSent.countDown();}});peer.setDaemon(true);peer.start();
            try(var client=LocalClient.revision(InetAddress.getLoopbackAddress(),listener.getLocalPort(),"Viajante",false,false,null,EMPTY)){
                client.revisions().renderer(p->CompletableFuture.completedFuture(null));assertTrue(oldSent.await(5,TimeUnit.SECONDS));Thread.sleep(50);assertNull(client.snapshot(),"Old tick with reused content hash must stay hidden after renderer reset");fresh.countDown();client.await(s->s.tick()==102,java.time.Duration.ofSeconds(3));peer.join(1000);assertNull(failure.get());
            }
        }
    }
}
