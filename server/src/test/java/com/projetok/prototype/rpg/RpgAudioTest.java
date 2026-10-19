package com.projetok.prototype.rpg;
import com.projetok.rpg.RpgPreferences;
import org.junit.jupiter.api.Test;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
class RpgAudioTest {
    @Test void unavailableDeviceFallsBackAndMuteSkipsOutput()throws Exception{
        try(var audio=new RpgAudio(RpgPreferences.defaults(),(cue,volume)->{throw new javax.sound.sampled.LineUnavailableException("sem dispositivo");})){
            audio.play(RpgAudio.Cue.HIT);audio.flush().get(2,TimeUnit.SECONDS);assertEquals(1,audio.unavailableCount());audio.preferences(new RpgPreferences(40,true,true));audio.play(RpgAudio.Cue.UI);audio.flush().get(2,TimeUnit.SECONDS);assertEquals(1,audio.unavailableCount());
        }
    }
}
