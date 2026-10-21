package com.projetok.rpg;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LegacyRaceMigrationTest {
    @Test void oldArchetypesMigrateWithoutLosingCharacterOrGrantingDomain() throws Exception {
        for(String old:new String[]{"guardian","mystic"}) {
            var world=new RpgWorld();world.awardExperience(500);
            world.inventory().add("crystal",3);var before=world.save();
            var properties=new Properties();properties.load(new ByteArrayInputStream(LocalSaveStore.encode(before)));
            properties.setProperty("schema","1");properties.remove("race");properties.setProperty("archetype",old);
            properties.setProperty("baseHp",old.equals("guardian")?"140":"100");properties.setProperty("baseAttack",old.equals("guardian")?"18":"24");
            properties.remove("flag.domain:none");properties.remove("flag.mastery-half:0");
            var bytes=new ByteArrayOutputStream();properties.store(bytes,"legacy fixture");
            var migrated=LocalSaveStore.decode(bytes.toByteArray());
            assertEquals(before.identity().id(),migrated.identity().id());assertEquals(before.identity().name(),migrated.identity().name());
            assertEquals(old.equals("guardian")?"reaper":"human",migrated.identity().race());
            assertEquals(old.equals("guardian")?140:100,migrated.identity().baseHp());
            assertEquals(old.equals("guardian")?18:24,migrated.identity().baseAttack());
            assertEquals(before.xp(),migrated.xp());assertEquals(before.inventory(),migrated.inventory());
            assertEquals(before.equipment(),migrated.equipment());assertFalse(DomainProgression.read(migrated.flags()).chosen());
            var loaded=RpgWorld.restore(migrated).save();
            assertEquals(loaded,LocalSaveStore.decode(LocalSaveStore.encode(loaded)));
        }
    }
}
