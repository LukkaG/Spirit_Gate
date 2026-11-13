package com.projetok.multiplayer;

import com.projetok.domain.entity.Position;
import com.projetok.domain.movement.CollisionService;
import com.projetok.rpg.*;
import com.projetok.studio.WorldContent;
import java.net.InetAddress;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AttributeAllocationTest {
    @Test void agilityReducesAuthoritativeBasicCooldownWithoutChangingDamageOrOtherPlayer()throws Exception{
        var world=new RpgSessionWorld(List.of("reaper","human"),SessionCharacterSelectionTest.catalog(),true,20);world.enableRevisions(WorldContent.baseline());
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world)){
            server.start();try(var a=Task207InventoryTest.connect(server,"Agilidade Reaper","reaper");var b=Task207InventoryTest.connect(server,"Agilidade Human","human")){
                Task201CharacterSessionTest.ready(a,b);Task201CharacterSessionTest.waitFor(()->a.playerSheet()!=null&&b.playerSheet()!=null,a,b);a.requestAttributes();b.requestAttributes();Task201CharacterSessionTest.waitFor(()->a.attributes()!=null&&b.attributes()!=null,a,b);int damage=a.playerSheet().progress().attack();
                for(int i=1;i<=50;i++){final int expected=i;assertTrue(a.allocateAttribute("agility"));a.await(snapshot->a.attributes()!=null&&a.attributes().agility()==expected,java.time.Duration.ofSeconds(7));}
                assertEquals(21,a.attributes().points());assertEquals(damage,a.playerSheet().progress().attack());assertEquals(0,b.attributes().agility());assertTrue(a.cycleTarget());a.send(Protocol.Action.ATTACK,0,0);Task201CharacterSessionTest.waitFor(()->a.playerSheet().abilities().attackTicks()>0,a,b);assertTrue(a.playerSheet().abilities().attackTicks()<=8);
                Task201CharacterSessionTest.waitFor(()->b.combatEvents().stream().anyMatch(e->e.player()==a.id()&&e.kind().equals("HIT")),a,b);assertNull(server.failure());
            }
        }
    }
    @Test void existingPointCurveIsAwardedOnceAndAllocationCannotOverdraw(){
        var world=new RpgWorld(CharacterIdentity.create("Atributos Reaper","reaper","azure"),new RpgSceneSet("forest",Map.of("forest",new RpgSceneSet.Geometry(new CollisionService.Space(960,640,List.of(),List.of()),new Position(140,300))),Map.of(),Map.of(),Map.of(),Map.of()));
        assertEquals(0,world.manualAttributes().unspentPoints());assertFalse(world.allocateAttribute("strength"));world.awardExperience(RpgProgression.builtin().threshold(20));assertEquals(71,world.manualAttributes().unspentPoints());assertEquals(71,world.manualAttributes().unspentPoints());int attack=world.attackPower();
        assertFalse(world.allocateAttribute("vitality"));assertTrue(world.allocateAttribute("strength"));assertEquals(attack,world.attackPower());assertTrue(world.allocateAttribute("strength"));assertEquals(attack+1,world.attackPower());
        for(int i=0;i<69;i++)assertTrue(world.allocateAttribute(i%2==0?"intelligence":"agility"));assertEquals(0,world.manualAttributes().unspentPoints());assertFalse(world.allocateAttribute("strength"));assertEquals(attack+1,world.attackPower());
        world.awardExperience(RpgProgression.builtin().threshold(21)-world.progressView().xp());assertEquals(1,world.manualAttributes().unspentPoints());
    }
    @Test void attributesArePrivateAndOnlyServerAllocatesAndUpdatesAttack()throws Exception{
        var world=new RpgSessionWorld(List.of("reaper","human"),SessionCharacterSelectionTest.catalog(),true,20);world.enableRevisions(WorldContent.baseline());
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world)){
            server.start();try(var a=Task207InventoryTest.connect(server,"Atributos Reaper","reaper");var b=Task207InventoryTest.connect(server,"Atributos Human","human")){
                Task201CharacterSessionTest.ready(a,b);Task201CharacterSessionTest.waitFor(()->a.playerSheet()!=null&&b.playerSheet()!=null,a,b);assertNull(a.attributes());assertNull(b.attributes());assertTrue(a.requestAttributes());Task201CharacterSessionTest.waitFor(()->a.attributes()!=null,a,b);assertNull(b.attributes());assertEquals(71,a.attributes().points());int attack=a.playerSheet().progress().attack();
                assertTrue(a.allocateAttribute("strength"));Task201CharacterSessionTest.waitFor(()->a.attributes().strength()==1,a,b);assertTrue(a.allocateAttribute("strength"));Task201CharacterSessionTest.waitFor(()->a.attributes().strength()==2&&a.playerSheet().progress().attack()==attack+1,a,b);assertEquals(69,a.attributes().points());assertTrue(b.requestAttributes());Task201CharacterSessionTest.waitFor(()->b.attributes()!=null,a,b);assertEquals(0,b.attributes().strength());assertEquals(71,b.attributes().points());assertFalse(a.allocateAttribute("self"));assertNull(server.failure());
            }
        }
    }
    @Test void privateAttributeFrameRoundTripsAndOldSheetStillRoundTrips()throws Exception{
        var state=new GameplayProtocol.Attributes("a".repeat(64),2,3,4,20,62);var out=new java.io.ByteArrayOutputStream();Protocol.write(out,state);assertEquals(state,Protocol.read(new java.io.ByteArrayInputStream(out.toByteArray())));
        assertThrows(IllegalArgumentException.class,()->new GameplayProtocol.Attributes("a".repeat(64),-1,0,0,1,0));assertThrows(IllegalArgumentException.class,()->new GameplayProtocol.Attributes("x",0,0,0,1,0));
        var sheet=new GameplayProtocol.State("a".repeat(64),new Protocol.CombatState(new Protocol.SessionState(new Protocol.State(1,List.of()),List.of()),List.of()),new GameplayProtocol.Sheet(Map.of(),Map.of()));out.reset();Protocol.write(out,sheet);assertEquals(sheet,Protocol.read(new java.io.ByteArrayInputStream(out.toByteArray())));
    }
}
