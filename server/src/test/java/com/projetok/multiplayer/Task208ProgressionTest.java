package com.projetok.multiplayer;

import java.time.Duration;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

/** Confirma progressão pelo ledger real, mantendo os dados privados e a autoridade do servidor. */
@Timeout(25)
class Task208ProgressionTest {
    /** O cenário de catálogo permite observar morte e recompensa sem depender do piso de treino. */
    @Test void tcpKillAwardsXpOnceAndOtherCharacterCannotSendArbitraryXp()throws Exception {
        try(var server=Task225R1CombatFixture.start();
            var spirit=Task207InventoryTest.connect(server,"Progressao Spirit","spirit");
            var human=Task207InventoryTest.connect(server,"Progressao Human","human")) {
            Task201CharacterSessionTest.ready(spirit,human);
            Task201CharacterSessionTest.waitFor(()->spirit.playerSheet()!=null&&human.playerSheet()!=null,spirit,human);
            var death=Task225R1CombatFixture.defeatSlime(server,spirit,human);
            assertEquals(3,spirit.playerSheet().progress().xp());assertEquals(0,human.playerSheet().progress().xp());
            assertEquals(1,spirit.playerSheet().progress().level());assertEquals(60,spirit.playerSheet().progress().nextLevelXp());
            var before=spirit.playerSheet().progress();spirit.send(Protocol.Action.ATTACK,0,0);long tick=spirit.snapshot().tick();
            spirit.await(s->s.tick()>tick+3,Duration.ofSeconds(3));assertEquals(before,spirit.playerSheet().progress());
            // O protocolo não tem setter de XP: alvo inventado numa operação existente não cria progressão.
            assertTrue(human.gameplay(GameplayProtocol.Operation.ALLOCATE_ATTRIBUTE,"xp_999999"));
            assertTrue(spirit.gameplay(GameplayProtocol.Operation.USE,"xp_999999"));
            long rejectedAt=spirit.snapshot().tick();
            Task201CharacterSessionTest.waitFor(()->spirit.snapshot().tick()>rejectedAt+3
                    &&human.snapshot().tick()>rejectedAt+3,spirit,human);
            assertEquals(before,spirit.playerSheet().progress());assertEquals(0,human.playerSheet().progress().xp());
            assertEquals(1,Task225R1CombatFixture.events(spirit,"DEATH",death).size());
            assertEquals(1,Task225R1CombatFixture.events(spirit,"SYNTHETIC_REWARD",death).size());
            assertNull(server.failure());
        }
    }
    /** O rótulo usa a ficha recebida; a fixture de UI não concede XP em mundo de jogo. */
    @Test void levelAndAttributeDisplayUsesServerSheet()throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var label=new NetworkProgressLabel();label.refresh(null);assertTrue(label.getText().contains("aguardando"));
            label.refresh(new GameplayProtocol.Sheet(java.util.Map.of(),java.util.Map.of()));
            label.refresh(new GameplayProtocol.Sheet(java.util.Map.of(),java.util.Map.of(),new GameplayProtocol.Progress(60,2,180,21,0,"none")));
            assertTrue(label.getText().contains("Nível 2"));assertTrue(label.getText().contains("Subiu de nível"));
            assertTrue(label.getText().contains("ATQ 21"));
        });
        assertThrows(IllegalArgumentException.class,()->new GameplayProtocol.Progress(-1,1,60,18,0,"none"));
    }
}
