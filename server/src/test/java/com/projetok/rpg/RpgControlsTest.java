package com.projetok.rpg;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class RpgControlsTest {
    @TempDir Path folder;
    @Test void rejectsConflictsPersistsAndHotbarMatchesDomain()throws Exception{var map=new TreeMap<>(RpgControls.DEFAULTS);map.put("ability","W");assertThrows(IllegalArgumentException.class,()->new RpgControls(map));map.put("ability","SPACE");assertThrows(IllegalArgumentException.class,()->new RpgControls(map));map.put("ability","E");var c=new RpgControls(map);Path file=folder.resolve("controls.properties");c.save(file);assertEquals(c,RpgControls.load(file));var a=new RpgWorld();var b=RpgWorld.restore(a.save());assertEquals(a.castAbility(),b.activate("rpg.ability"));assertEquals(a.save(),b.save());}
}
