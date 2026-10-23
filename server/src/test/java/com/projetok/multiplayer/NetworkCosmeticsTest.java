package com.projetok.multiplayer;

import com.projetok.prototype.rpg.CosmeticCatalog;
import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Capacidade negociada preserva peers V1 e aceita somente cosméticos da própria sessão. */
class NetworkCosmeticsTest {
    CosmeticCatalog catalog()throws Exception{return CosmeticCatalog.metadata(Path.of("../assets/dev/generated/corujao-iv-v1"));}
    @Test void negotiatedAppearanceRoundTripsAlongsideHistoricalV1Peer()throws Exception {
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,new RpgSessionWorld(List.of("reaper","human"),catalog()))){server.start();try(var a=LocalClient.cosmetics(InetAddress.getLoopbackAddress(),server.port(),"Jogador A",false);var old=new LocalClient(InetAddress.getLoopbackAddress(),server.port(),"Jogador B")){
            a.await(s->s.actors().size()==2&&a.appearances().size()==2,Duration.ofSeconds(3));assertTrue(a.appearance("hair_short","jacket"));a.await(s->a.appearances().get(a.id()).hair().equals("hair_short"),Duration.ofSeconds(3));assertTrue(old.connected());assertEquals("reaper",a.appearances().get(a.id()).race());assertFalse(old.appearance("hair_medium","tunic"));
            long before=a.snapshot().tick();assertTrue(a.appearance("missing","tunic"));a.await(s->s.tick()>before+6,Duration.ofSeconds(3));assertEquals("hair_short",a.appearances().get(a.id()).hair());assertEquals("jacket",a.appearances().get(a.id()).clothing());
        }}
    }
    @Test void ownSelectionCannotChangeAnotherPlayerRaceOrSurviveDisconnect()throws Exception {var world=new RpgSessionWorld(List.of("aetheri","spirit"),catalog());world.join(1,"Jogador A");world.join(2,"Jogador B");assertTrue(world.appearance(1,new Protocol.AppearanceRequest(1,"hair_spiky","light_armor")));assertFalse(world.appearance(1,new Protocol.AppearanceRequest(1,"hair_short","jacket")));assertEquals("none",world.appearances().get(1).hair());assertEquals("aetheri",world.race(1));world.leave(1);assertFalse(world.appearance(1,new Protocol.AppearanceRequest(2,"hair_short","tunic")));assertEquals(1,world.appearances().size());}
    @Test void extensionWireRoundTripAndPathsAreRejected()throws Exception {var state=new Protocol.State(1,List.of(new Protocol.Actor(1,"Teste",140,300,100,100,1000,0,false)));var extended=new Protocol.SessionState(state,List.of(new Protocol.Appearance(1,"reaper","hair_short","tunic")));var out=new ByteArrayOutputStream();Protocol.write(out,extended);assertEquals(extended,Protocol.read(new ByteArrayInputStream(out.toByteArray())));assertThrows(IllegalArgumentException.class,()->new Protocol.AppearanceRequest(1,"../../saves","jacket"));assertThrows(IllegalArgumentException.class,()->new Protocol.Appearance(1,"guardian","none","none"));}
}
