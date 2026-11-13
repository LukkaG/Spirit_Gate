package com.projetok.multiplayer;

import com.projetok.studio.WorldContent;
import java.net.InetAddress;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TargetCombatServerTest {
    @Test void newTargetOperationsRoundTripWithoutChangingOldOrdinals()throws Exception{
        assertEquals(0,GameplayProtocol.Operation.EQUIP.ordinal());assertEquals(4,GameplayProtocol.Operation.CHOOSE_DOMAIN.ordinal());
        for(var operation:List.of(GameplayProtocol.Operation.SELECT_TARGET,GameplayProtocol.Operation.BASIC_ATTACK,GameplayProtocol.Operation.SHUNPO_OFFENSIVE)){
            var request=new GameplayProtocol.Request(7,operation,"enemy_1000100");var out=new java.io.ByteArrayOutputStream();Protocol.write(out,request);assertEquals(request,Protocol.read(new java.io.ByteArrayInputStream(out.toByteArray())));
        }
        var old=new java.io.ByteArrayOutputStream();Protocol.write(old,new Protocol.Bye());assertArrayEquals(new byte[]{0,0,0,2,1,5},old.toByteArray());
    }
    @Test void twoClientsValidateDirectedTargetsAndSeeTargetedAndForwardShunpo()throws Exception{
        var world=new RpgSessionWorld(List.of("reaper","human"),SessionCharacterSelectionTest.catalog(),true,20);world.enableRevisions(WorldContent.baseline());
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world)){
            server.start();try(var a=Task207InventoryTest.connect(server,"Target Reaper","reaper");var b=Task207InventoryTest.connect(server,"Target Human","human")){
                Task201CharacterSessionTest.ready(a,b);Task201CharacterSessionTest.waitFor(()->a.playerSheet()!=null&&b.playerSheet()!=null,a,b);
                int enemy=a.snapshot().actors().stream().filter(v->v.id()>1_000_000&&v.id()<2_000_000&&v.hp()>0).findFirst().orElseThrow().id();
                assertFalse(a.selectTarget(b.id()));assertFalse(a.selectTarget(2_000_001));assertTrue(a.gameplay(GameplayProtocol.Operation.SHUNPO_OFFENSIVE,"enemy_"+enemy));long tick=a.snapshot().tick();Task201CharacterSessionTest.waitFor(()->a.snapshot().tick()>tick+3,a,b);assertEquals(0,a.playerSheet().abilities().shunpoTicks());
                assertTrue(a.gameplay(GameplayProtocol.Operation.SELECT_TARGET,"enemy_2000001"));assertTrue(a.gameplay(GameplayProtocol.Operation.SHUNPO_OFFENSIVE,"enemy_"+enemy));long invalid=a.snapshot().tick();Task201CharacterSessionTest.waitFor(()->a.snapshot().tick()>invalid+3,a,b);assertEquals(0,a.playerSheet().abilities().shunpoTicks());
                assertTrue(a.cycleTarget());assertEquals(enemy,a.selectedTarget());assertTrue(a.cycleTarget());assertEquals(enemy,a.selectedTarget());
                a.send(Protocol.Action.LIGHTNING_STEP,0,0);Task201CharacterSessionTest.waitFor(()->a.playerSheet().abilities().shunpoTicks()>0,a,b);assertTrue(a.playerSheet().abilities().shunpoTicks()<=90);assertTrue(GamePanelsIntegrationTest.self(a).stamina()<1000);
                var moved=GamePanelsIntegrationTest.self(a);Task201CharacterSessionTest.waitFor(()->b.snapshot().actors().stream().anyMatch(actor->actor.id()==a.id()&&Math.abs(actor.x()-moved.x())<.01&&Math.abs(actor.y()-moved.y())<.01),a,b);
                assertEquals(moved.facingX(),b.snapshot().actors().stream().filter(actor->actor.id()==a.id()).findFirst().orElseThrow().facingX(),.001);
                assertEquals(0,b.selectedTarget());b.send(Protocol.Action.MOVE,0,-1);b.send(Protocol.Action.MOVE,0,0);long settle=b.snapshot().tick();Task201CharacterSessionTest.waitFor(()->b.snapshot().tick()>settle+3,a,b);var before=GamePanelsIntegrationTest.self(b);assertEquals(-1,before.facingY(),.001);
                b.send(Protocol.Action.LIGHTNING_STEP,0,0);Task201CharacterSessionTest.waitFor(()->b.playerSheet().abilities().shunpoTicks()>0,a,b);var forward=GamePanelsIntegrationTest.self(b);assertEquals(before.x(),forward.x(),.01);assertEquals(before.y()-70,forward.y(),.01);assertTrue(forward.stamina()<1000);
                assertTrue(a.selectTarget(0));assertEquals(0,a.selectedTarget());assertNull(server.failure());
            }
        }
    }
}
