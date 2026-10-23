package com.projetok.multiplayer;

import com.projetok.prototype.rpg.CosmeticCatalog;
import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Snapshot forjado não é comando e nunca altera outra sessão. */
class FinalNetworkIsolationTest {
    @Test void forgedSnapshotDisconnectsSenderAndPreservesOtherPlayer()throws Exception {
        var catalog=CosmeticCatalog.metadata(Path.of("../assets/dev/generated/corujao-iv-v1"));try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,new RpgSessionWorld(List.of("reaper","human"),catalog))){server.start();try(var victim=LocalClient.cosmetics(InetAddress.getLoopbackAddress(),server.port(),"Jogador A",false);var attacker=new Socket(InetAddress.getLoopbackAddress(),server.port())){
            attacker.setSoTimeout(2500);Protocol.write(attacker.getOutputStream(),new Protocol.Hello(2,"Jogador B"));assertInstanceOf(Protocol.Welcome.class,Protocol.read(attacker.getInputStream()));var fake=new Protocol.State(1,List.of(new Protocol.Actor(victim.id(),"Forjado",900,600,100,100,1000,999999,false)));Protocol.write(attacker.getOutputStream(),new Protocol.SessionState(fake,List.of(new Protocol.Appearance(victim.id(),"spirit","hair_spiky","light_armor"))));victim.await(s->s.tick()>6&&s.actors().size()==1,Duration.ofSeconds(3));var appearance=victim.appearances().get(victim.id());assertEquals("reaper",appearance.race());assertEquals("none",appearance.hair());assertEquals(0,victim.snapshot().actors().get(0).coins());assertNull(server.failure());
        }}
    }
    @Test void nestedExtensionAndUnsafeCliOptionsAreRejected()throws Exception {
        var inner=new ByteArrayOutputStream();Protocol.write(inner,new Protocol.SessionState(new Protocol.State(1,List.of()),List.of()));var payload=new ByteArrayOutputStream();var data=new DataOutputStream(payload);data.writeByte(1);data.writeByte(8);data.write(inner.toByteArray());data.writeByte(0);var framed=new ByteArrayOutputStream();var frame=new DataOutputStream(framed);frame.writeInt(payload.size());frame.write(payload.toByteArray());assertThrows(IOException.class,()->Protocol.read(new ByteArrayInputStream(framed.toByteArray())));
        assertThrows(IllegalArgumentException.class,()->RpgNetworkClientLauncher.options(new String[]{"localhost","54557","Teste","--hair","../../saves","--cosmetics"}));assertThrows(IllegalArgumentException.class,()->RpgNetworkClientLauncher.options(new String[]{"localhost","54557","Teste","--hair","hair_short"}));
    }
}
