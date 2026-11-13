package com.projetok.multiplayer;

import com.projetok.prototype.rpg.*;
import java.io.*;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class Task207InventoryTest {
    @TempDir Path root;
    static LocalClient connect(LocalSimulationServer server,String name,String race)throws Exception {
        var client=LocalClient.gameplay(InetAddress.getLoopbackAddress(),server.port(),new SessionCharacterSelection(name,race,"none","none"),false,false,null,Task201CharacterSessionTest.EMPTY);
        client.revisions().renderer(p->CompletableFuture.completedFuture(null));return client;
    }
    @Test void twoTcpInventoriesArePrivateAndServerValidatesOperations()throws Exception {
        try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,SessionCharacterSelectionTest.catalog(),root);
            var reaper=connect(server,"Inventario Reaper","reaper");var human=connect(server,"Inventario Human","human")) {
            Task201CharacterSessionTest.ready(reaper,human);
            Task201CharacterSessionTest.waitFor(()->reaper.playerSheet()!=null&&human.playerSheet()!=null,reaper,human);
            assertEquals("small_sword",reaper.playerSheet().equipment().get("weapon"));assertTrue(human.playerSheet().equipment().isEmpty());
            var other=human.playerSheet();
            reaper.gameplay(GameplayProtocol.Operation.UNEQUIP,"weapon");
            Task201CharacterSessionTest.waitFor(()->reaper.playerSheet().equipment().isEmpty(),reaper,human);
            assertEquals(1,count(reaper.playerSheet(),"small_sword"));assertEquals(other,human.playerSheet());
            reaper.gameplay(GameplayProtocol.Operation.UNEQUIP,"weapon");
            reaper.gameplay(GameplayProtocol.Operation.EQUIP,"small_sword");
            Task201CharacterSessionTest.waitFor(()->reaper.playerSheet().equipment().containsKey("weapon"),reaper,human);
            assertEquals(0,count(reaper.playerSheet(),"small_sword"));
            human.gameplay(GameplayProtocol.Operation.EQUIP,"small_sword");
            human.gameplay(GameplayProtocol.Operation.USE,"crystal");
            long tick=human.snapshot().tick();
            Task201CharacterSessionTest.waitFor(()->human.snapshot().tick()>tick+3,reaper,human);
            assertEquals(other,human.playerSheet());assertNull(server.failure());
        }
    }
    static int count(GameplayProtocol.Sheet sheet,String id){return sheet.inventory().entrySet().stream().filter(e->e.getKey().endsWith(":"+id)).mapToInt(Map.Entry::getValue).sum();}
    @Test void extensionRoundTripsAndRejectsOversizeMalformedAndNestedStates()throws Exception {
        var sheet=new GameplayProtocol.Sheet(Map.of("0:potion",3),Map.of("weapon","small_sword"));
        var state=new GameplayProtocol.State("a".repeat(64),new Protocol.CombatState(new Protocol.SessionState(new Protocol.State(1,List.of()),List.of()),List.of()),sheet);
        var bytes=new ByteArrayOutputStream();Protocol.write(bytes,state);assertTrue(bytes.size()<Protocol.MAX_FRAME);assertEquals(state,Protocol.read(new ByteArrayInputStream(bytes.toByteArray())));
        var request=new Protocol.RevisionCommand("a".repeat(64),new GameplayProtocol.Request(1,GameplayProtocol.Operation.USE,"potion"));
        bytes.reset();Protocol.write(bytes,request);assertEquals(request,Protocol.read(new ByteArrayInputStream(bytes.toByteArray())));
        assertThrows(IllegalArgumentException.class,()->new GameplayProtocol.Sheet(Map.of("20:potion",1),Map.of()));
        assertThrows(IllegalArgumentException.class,()->new GameplayProtocol.Sheet(Map.of("0:potion",-1),Map.of()));
        assertThrows(IllegalArgumentException.class,()->new GameplayProtocol.Sheet(Map.of("0:potion",1,"0:crystal",1),Map.of()));
        assertThrows(IllegalArgumentException.class,()->new GameplayProtocol.Request(1,GameplayProtocol.Operation.EQUIP,"../save"));
        assertThrows(IllegalArgumentException.class,()->new Protocol.RevisionCommand("a".repeat(64),state));
    }
}
