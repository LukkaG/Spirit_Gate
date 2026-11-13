package com.projetok.multiplayer;

import com.projetok.prototype.rpg.*;
import com.projetok.studio.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Provas TCP reais de escolha, admissao, reconexao e revisao do mundo compartilhado. */
class Task201CharacterSessionTest {
    @TempDir Path root;
    static final AssetCatalog.Result EMPTY=new AssetCatalog.Result(List.of(),List.of(),List.of());
    interface Check {boolean get() throws Exception;}
    static void waitFor(Check check,LocalClient... clients) throws Exception {
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(7);
        while(!check.get()&&System.nanoTime()<until){for(var c:clients)c.measureLatency();Thread.sleep(10);}
        assertTrue(check.get(),"Condicao TCP nao atingida no prazo");
    }
    static LocalClient client(LocalSimulationServer server,String name,String race) throws Exception {
        var client=LocalClient.character(InetAddress.getLoopbackAddress(),server.port(),new SessionCharacterSelection(name,race,"none","none"),false,false,null,EMPTY);
        client.revisions().renderer(p->CompletableFuture.completedFuture(null));return client;
    }
    static void ready(LocalClient... clients) throws Exception {
        waitFor(()->Arrays.stream(clients).allMatch(c->!c.revisions().blocked()&&c.snapshot()!=null),clients);
    }
    LocalSimulationServer server() throws Exception {
        return LauncherSession.start(InetAddress.getLoopbackAddress(),0,SessionCharacterSelectionTest.catalog(),root);
    }

    @ParameterizedTest @ValueSource(strings={"reaper","human","aetheri","spirit"})
    void bothRealClientsCanChooseSameRaceAndName(String race) throws Exception {
        try(var server=server();var a=client(server,"Mesmo Nome",race);var b=client(server,"Mesmo Nome",race)) {
            ready(a,b);waitFor(()->a.appearances().size()==2&&b.appearances().size()==2,a,b);
            assertNotEquals(a.id(),b.id());assertEquals(2,server.players());
            for(var c:List.of(a,b)) {
                assertEquals(race,c.appearances().get(a.id()).race());assertEquals(race,c.appearances().get(b.id()).race());
                assertTrue(c.snapshot().actors().stream().filter(actor->!actor.boss()).allMatch(actor->actor.name().equals("Mesmo Nome")));
            }
            assertEquals(race,a.characterSelection().race());assertNull(server.failure());
        }
    }

    @Test void reverseOrderAndReconnectPreserveChoiceWithNewSession() throws Exception {
        try(var server=server();var human=client(server,"Primeiro Humano","human");var reaper=client(server,"Segundo Reaper","reaper")) {
            ready(human,reaper);waitFor(()->reaper.appearances().size()==2,human,reaper);
            assertEquals("human",reaper.appearances().get(human.id()).race());assertEquals("reaper",reaper.appearances().get(reaper.id()).race());
            int old=human.id();human.close();waitFor(()->server.players()==1,reaper);
            try(var next=human.reconnect()) {
                next.revisions().renderer(p->CompletableFuture.completedFuture(null));ready(next,reaper);
                waitFor(()->reaper.appearances().containsKey(next.id()),next,reaper);
                assertNotEquals(old,next.id());assertEquals(human.characterSelection(),next.characterSelection());
                assertEquals("human",reaper.appearances().get(next.id()).race());assertFalse(reaper.appearances().containsKey(old));
            }
            assertNull(server.failure());
        }
    }

    @Test void invalidAndMissingInitialProfileReleaseConnectionWithoutCreatingActor() throws Exception {
        try(var server=server()) {
            try(var socket=new Socket(InetAddress.getLoopbackAddress(),server.port())) {
                socket.setSoTimeout(3000);Protocol.write(socket.getOutputStream(),new Protocol.Hello(5,"Invalido"));
                Protocol.write(socket.getOutputStream(),new Protocol.CharacterCreate("human","missing","none"));
                assertInstanceOf(Protocol.Error.class,Protocol.read(socket.getInputStream()));
            }
            waitFor(()->server.connections()==0);assertTrue(server.snapshot().actors().isEmpty());
            try(var socket=new Socket(InetAddress.getLoopbackAddress(),server.port())) {
                socket.setSoTimeout(3000);Protocol.write(socket.getOutputStream(),new Protocol.Hello(5,"Sem Perfil"));
                Protocol.write(socket.getOutputStream(),new Protocol.Intent(1,Protocol.Action.MOVE,0,0));
                var error=assertInstanceOf(Protocol.Error.class,Protocol.read(socket.getInputStream()));assertTrue(error.reason().contains("personagem"));
            }
            waitFor(()->server.connections()==0);assertTrue(server.snapshot().actors().isEmpty());
            try(var a=client(server,"Valido A","spirit");var b=client(server,"Valido B","human")) {ready(a,b);assertEquals(2,server.players());}
            assertNull(server.failure());
        }
    }

    @Test void malformedReaperChoiceAndSecondCreationCannotMutateAnotherActor() throws Exception {
        try(var server=server();var good=client(server,"Correto","human")) {
            ready(good);int stable=good.id();
            try(var socket=new Socket(InetAddress.getLoopbackAddress(),server.port())) {
                socket.setSoTimeout(3000);Protocol.write(socket.getOutputStream(),new Protocol.Hello(5,"Reaper Invalido"));
                var bytes=new ByteArrayOutputStream();var data=new DataOutputStream(bytes);data.writeByte(1);data.writeByte(16);
                for(String value:List.of("reaper","hair_short","jacket")){byte[] encoded=value.getBytes(StandardCharsets.UTF_8);data.writeByte(encoded.length);data.write(encoded);}
                var output=new DataOutputStream(socket.getOutputStream());output.writeInt(bytes.size());output.write(bytes.toByteArray());output.flush();
                assertThrows(IOException.class,()->Protocol.read(socket.getInputStream()));
            }
            waitFor(()->server.connections()==1,good);assertEquals(1,server.players());assertEquals(stable,good.id());
            try(var socket=new Socket(InetAddress.getLoopbackAddress(),server.port())) {
                socket.setSoTimeout(3000);Protocol.write(socket.getOutputStream(),new Protocol.Hello(5,"Criacao Repetida"));
                Protocol.write(socket.getOutputStream(),new Protocol.CharacterCreate("human","none","none"));
                assertInstanceOf(Protocol.Welcome.class,Protocol.read(socket.getInputStream()));
                Protocol.write(socket.getOutputStream(),new Protocol.CharacterCreate("spirit","none","none"));
                waitFor(()->server.connections()==1,good);
            }
            assertEquals(1,server.players());assertTrue(good.connected());
            assertEquals("human",good.appearances().get(stable).race());assertNull(server.failure());
        }
    }

    @Test void selectedClientAndHistoricalV4PublishAndRollbackWithoutLosingRace() throws Exception {
        try(var server=server();var selected=client(server,"Escolhido","spirit");
            var historical=LocalClient.revision(InetAddress.getLoopbackAddress(),server.port(),"Historico V4",false,false,null,EMPTY)) {
            historical.revisions().renderer(p->CompletableFuture.completedFuture(null));ready(selected,historical);
            assertNull(historical.characterSelection());var managed=LauncherSession.managed(server);String before=managed.target().activeHash();
            var next=new WorldRevisionTest().changed(1,500);
            var published=managed.store().publish(next,"",before,managed.target(),LivePublishing.Fault.NONE);assertTrue(published.active(),published.message());
            ready(selected,historical);assertEquals(next.hash(),selected.revisions().hash());assertEquals(next.hash(),historical.revisions().hash());
            waitFor(()->historical.appearances().containsKey(selected.id()),selected,historical);assertEquals("spirit",historical.appearances().get(selected.id()).race());
            var baseline=WorldContent.baseline();var back=LiveRevision.prepare(2,"forest",baseline.bundle().maps(),EMPTY);
            assertTrue(managed.store().publish(back,next.hash(),next.hash(),managed.target(),LivePublishing.Fault.NONE).active());
            ready(selected,historical);assertEquals(before,selected.revisions().hash());assertEquals("spirit",selected.appearances().get(selected.id()).race());assertNull(server.failure());
        }
    }

    @Test void historicalV1StillBlocksLivePublicationAlongsideSelectedCharacter() throws Exception {
        try(var server=server();var selected=client(server,"Atualizado","aetheri");var historical=new LocalClient(InetAddress.getLoopbackAddress(),server.port(),"Historico V1")) {
            ready(selected);historical.await(s->s.actors().stream().filter(a->!a.boss()).count()==2,Duration.ofSeconds(3));
            var managed=LauncherSession.managed(server);String before=managed.target().activeHash();
            var denied=managed.store().publish(new WorldRevisionTest().changed(1,500),"",before,managed.target(),LivePublishing.Fault.NONE);
            assertFalse(denied.active());assertTrue(historical.connected());assertEquals(before,managed.target().activeHash());assertFalse(Files.exists(managed.store().root().resolve("active.txt")));assertNull(server.failure());
        }
    }

    @Test void unsupportedServerGivesUpgradeErrorWithoutSilentlyJoiningLegacyMode() throws Exception {
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0)) {
            server.start();var error=assertThrows(IOException.class,()->client(server,"Novo Jogador","human"));
            assertTrue(error.getMessage().toLowerCase(Locale.ROOT).contains("servidor"));
            assertTrue(error.getMessage().toLowerCase(Locale.ROOT).contains("atualiz"));waitFor(()->server.connections()==0);assertEquals(0,server.players());
            try(var old=new LocalClient(InetAddress.getLoopbackAddress(),server.port(),"Antigo Jogador")) {old.await(s->!s.actors().isEmpty(),Duration.ofSeconds(3));assertNull(old.characterSelection());}
            assertNull(server.failure());
        }
    }
}
