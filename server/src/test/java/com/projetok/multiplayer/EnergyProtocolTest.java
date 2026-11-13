package com.projetok.multiplayer;

import com.projetok.studio.WorldContent;
import java.net.InetAddress;
import java.time.Duration;
import java.util.*;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EnergyProtocolTest {
    @org.junit.jupiter.api.io.TempDir java.nio.file.Path root;
    @Test void revisionChangeKeepsEnergyPendingUntilFreshPrivateMetadata()throws Exception{
        try(var server=com.projetok.prototype.rpg.LauncherSession.startExpanded(InetAddress.getLoopbackAddress(),0,SessionCharacterSelectionTest.catalog(),root);
            var a=Task207InventoryTest.connect(server,"Energy Revision","reaper")){
            Task201CharacterSessionTest.ready(a);assertTrue(a.requestEnergy());a.await(s->a.energy()!=null,Duration.ofSeconds(7));assertFalse(a.energyView().pending());
            var managed=com.projetok.prototype.rpg.LauncherSession.managed(server);var fixture=new WorldRevisionTest();var next=fixture.changed(1,500);
            assertTrue(managed.store().publish(next,"",managed.target().activeHash(),managed.target(),com.projetok.studio.LivePublishing.Fault.NONE).active());
            a.await(s->!a.revisions().blocked()&&a.revisions().hash().equals(next.hash()),Duration.ofSeconds(7));assertNull(a.energy());assertTrue(a.energyView().pending());
            assertTrue(a.requestEnergy());a.await(s->a.energy()!=null,Duration.ofSeconds(7));assertFalse(a.energyView().pending());assertEquals(next.hash(),a.energy().hash());assertNull(server.failure());
        }
    }
    @Test void energyMetadataRoundTripsAndOlderActorsRemainNormalized()throws Exception{
        var value=new GameplayProtocol.Energy("a".repeat(64),20,3,50,500,2000,1500);var bytes=new java.io.ByteArrayOutputStream();Protocol.write(bytes,value);assertEquals(value,Protocol.read(new java.io.ByteArrayInputStream(bytes.toByteArray())));assertThrows(IllegalArgumentException.class,()->new GameplayProtocol.Energy("a".repeat(64),20,3,50,2001,2000,1500));
        var old=new Protocol.State(19,List.of(new Protocol.Actor(1,"Jogador",140,300,100,100,1000,0,false)));assertEquals(500,value.currentFor(old,1));var current=new Protocol.State(21,List.of(new Protocol.Actor(1,"Jogador",140,300,100,100,125,0,false)));assertEquals(250,value.currentFor(current,1));
        SwingUtilities.invokeAndWait(()->{var bars=new NetworkCombatHud();bars.energy(value,false);bars.update(current,1,new NetworkCombatPresentation());assertEquals("Energia 25 / 200",bars.energyText());bars.energy(null,true);bars.update(current,1,new NetworkCombatPresentation());assertTrue(bars.energyText().contains("sincronizando"));});
    }
    @Test void energyCapacityAboveOneHundredIsPrivateAndDoesNotBreakTwoClients()throws Exception{
        var world=new RpgSessionWorld(List.of("reaper","human"),SessionCharacterSelectionTest.catalog(),true,20);world.enableRevisions(WorldContent.baseline());
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world)){
            server.start();try(var a=Task207InventoryTest.connect(server,"Energy Reaper","reaper");var b=Task207InventoryTest.connect(server,"Energy Human","human")){
                Task201CharacterSessionTest.ready(a,b);Task201CharacterSessionTest.waitFor(()->a.playerSheet()!=null&&b.playerSheet()!=null,a,b);a.requestAttributes();a.requestEnergy();a.await(s->a.attributes()!=null&&a.energy()!=null,Duration.ofSeconds(7));assertNull(b.energy());int attack=a.playerSheet().progress().attack();
                for(int i=1;i<=10;i++){final int expected=i;assertTrue(a.allocateAttribute("energy"));a.await(s->a.energy()!=null&&a.energy().trained()==expected&&a.attributes().points()==71-expected,Duration.ofSeconds(7));}
                assertEquals(1200,a.energy().maximum());assertEquals(1100,a.energy().regenerationPermille());assertEquals(attack,a.playerSheet().progress().attack());a.await(s->a.energy().currentFor(s,a.id())==1200,Duration.ofSeconds(7));assertEquals(1000,GamePanelsIntegrationTest.self(a).stamina());
                b.requestEnergy();b.await(s->b.energy()!=null,Duration.ofSeconds(7));assertEquals(0,b.energy().trained());assertEquals(1000,b.energy().maximum());assertTrue(a.connected()&&b.connected());
                a.send(Protocol.Action.CRYOSTASIS,0,0);a.await(s->a.playerSheet().abilities().shunpoTicks()>0,Duration.ofSeconds(7));assertTrue(a.energy().currentFor(a.snapshot(),a.id())<1200);assertNull(server.failure());
            }
        }
    }
}
