package com.projetok.multiplayer;

import com.projetok.studio.WorldContent;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Task212GameplayIntegrityTest {
    @Test void repeatedSequencesCannotDuplicateEquipmentOrConsumePotionTwice()throws Exception {
        var world=new RpgSessionWorld(List.of("reaper","human"),SessionCharacterSelectionTest.catalog(),true);
        world.enableRevisions(WorldContent.baseline());
        assertTrue(world.join(1,new SessionCharacterSelection("Integridade Reaper","reaper","none","none"),6));
        assertTrue(world.join(2,new SessionCharacterSelection("Integridade Human","human","none","none"),6));
        // Fixture isolado da thread de simulação; provas TCP estão nas TASKs 207–211.
        world.freeze(false);
        var unequip=new GameplayProtocol.Request(1,GameplayProtocol.Operation.UNEQUIP,"weapon");
        assertTrue(world.gameplay(1,unequip));var first=world.gameplaySnapshotFor(1).sheet();
        assertFalse(world.gameplay(1,unequip));assertEquals(first,world.gameplaySnapshotFor(1).sheet());
        assertFalse(world.gameplay(2,new GameplayProtocol.Request(1,GameplayProtocol.Operation.EQUIP,"small_sword")));
        assertTrue(world.gameplay(1,new GameplayProtocol.Request(2,GameplayProtocol.Operation.EQUIP,"small_sword")));
        assertFalse(world.gameplay(1,new GameplayProtocol.Request(3,GameplayProtocol.Operation.EQUIP,"missing")));
        for(int i=0;i<100&&world.snapshot().actors().get(0).hp()==100;i++)world.step();
        assertTrue(world.snapshot().actors().get(0).hp()<100);
        var potion=new GameplayProtocol.Request(4,GameplayProtocol.Operation.USE,"potion");assertTrue(world.gameplay(1,potion));
        assertEquals(2,Task207InventoryTest.count(world.gameplaySnapshotFor(1).sheet(),"potion"));
        assertFalse(world.gameplay(1,potion));assertFalse(world.gameplay(1,new GameplayProtocol.Request(5,GameplayProtocol.Operation.USE,"potion")));
        assertEquals(2,Task207InventoryTest.count(world.gameplaySnapshotFor(1).sheet(),"potion"));
        world.freeze(true);assertFalse(world.gameplay(1,new GameplayProtocol.Request(6,GameplayProtocol.Operation.UNEQUIP,"weapon")));
    }
}
