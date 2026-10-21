package com.projetok.prototype.rpg;
import com.projetok.rpg.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AmbientFeedbackTest {
    @Test void repeatedSnapshotDoesNotReplayItemOrRejectedAbility(){
        var sounds=new java.util.ArrayList<RpgAudio.Cue>();var feedback=new AmbientFeedback(sounds::add);
        var world=new RpgWorld(CharacterIdentity.create("Místico","mystic","azure"));feedback.update(world.snapshot());assertTrue(sounds.isEmpty());
        world.inventory().add("sword",1);feedback.update(world.snapshot());assertEquals(1,sounds.size());for(int i=0;i<30;i++)feedback.update(world.snapshot());assertEquals(1,sounds.size());
        assertFalse(world.activate("rpg.ability"));feedback.update(world.snapshot());assertEquals(2,sounds.size());feedback.update(world.snapshot());assertEquals(2,sounds.size());
    }
    @Test void focusMuteAndCloseSkipBackendAndVolumeRemainsReal()throws Exception{
        var calls=new java.util.concurrent.atomic.AtomicInteger();
        try(var audio=new RpgAudio(new RpgPreferences(65,false,true),(cue,volume)->{assertEquals(65,volume);calls.incrementAndGet();})){
            audio.suspended(true);audio.play(RpgAudio.Cue.UI);audio.flush().get(2,java.util.concurrent.TimeUnit.SECONDS);assertEquals(0,calls.get());
            audio.suspended(false);audio.play(RpgAudio.Cue.UI);audio.flush().get(2,java.util.concurrent.TimeUnit.SECONDS);assertEquals(1,calls.get());
            audio.preferences(new RpgPreferences(65,true,true));audio.play(RpgAudio.Cue.UI);audio.flush().get(2,java.util.concurrent.TimeUnit.SECONDS);assertEquals(1,calls.get());
        }
    }
}
