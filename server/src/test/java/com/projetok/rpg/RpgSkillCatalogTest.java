package com.projetok.rpg;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Verifica os requisitos do catálogo sem alegar execução de combate ou suporte no protocolo. */
class RpgSkillCatalogTest {
    @Test void eachKitContainsExactlyFiveDistinctOrderedSlots() {
        var all = RpgSkillCatalog.all();
        assertEquals(40, all.size());
        assertEquals(40, all.stream().map(RpgSkillCatalog.Definition::id).distinct().count());
        var kits = all.stream().map(RpgSkillCatalog.Definition::kit).distinct().toList();
        assertEquals(8, kits.size());
        for (var kit : kits) {
            var definitions = RpgSkillCatalog.forKit(kit);
            assertEquals(List.of(1, 2, 3, 4, 5), definitions.stream().map(RpgSkillCatalog.Definition::slot).toList());
            IntStream.rangeClosed(1, 5).forEach(slot -> assertSame(definitions.get(slot - 1), RpgSkillCatalog.get(kit + "_" + slot)));
        }
    }

    @Test void historicalDomainsResolveWithoutDuplicatingOrGrantingSkills() {
        assertTrue(RpgSkillCatalog.forDomain("none").isEmpty());
        for (var domain : Domains.all()) assertEquals(5, RpgSkillCatalog.forDomain(domain.id()).size());
        assertEquals(RpgSkillCatalog.forKit("aetheri"), RpgSkillCatalog.forDomain("bow_long"));
        assertEquals(RpgSkillCatalog.forKit("aetheri"), RpgSkillCatalog.forDomain("bow_short"));
        assertEquals(RpgSkillCatalog.forKit("spirit"), RpgSkillCatalog.forDomain("darkness_control"));
        assertEquals(RpgSkillCatalog.forKit("spirit"), RpgSkillCatalog.forDomain("darkness_debuff"));
        assertThrows(IllegalArgumentException.class, () -> RpgSkillCatalog.forDomain("aetheri"));
        assertThrows(IllegalArgumentException.class, () -> RpgSkillCatalog.forDomain("unknown"));
        assertThrows(IllegalArgumentException.class, () -> RpgSkillCatalog.forKit("bow_long"));
        assertThrows(IllegalArgumentException.class, () -> RpgSkillCatalog.get("water_6"));
    }

    @Test void waterPrisonLastsTwoSecondsAndPreservesActions() {
        var prison = RpgSkillCatalog.get("water_3");
        var root = prison.effects().get(0);
        assertEquals(RpgSkillCatalog.EffectType.ROOT, root.type());
        assertEquals(60, root.durationTicks());
        assertTrue(root.blocksMovement());
        assertFalse(root.blocksActions());
        var stun = RpgSkillCatalog.get("thunder_4").effects().get(0);
        assertTrue(stun.blocksMovement());
        assertTrue(stun.blocksActions());
        assertFalse(RpgSkillCatalog.get("water_1").effects().get(0).blocksMovement());
    }

    @Test void windReturnRequiresTheRepulsionCheckpoint() {
        var source = RpgSkillCatalog.get("wind_4");
        var followup = RpgSkillCatalog.get("wind_5");
        assertEquals(RpgSkillCatalog.Pattern.KNOCKBACK_CHECKPOINT, source.mechanics().pattern());
        assertEquals(RpgSkillCatalog.Pattern.CHECKPOINT_PULL, followup.mechanics().pattern());
        assertEquals(RpgSkillCatalog.Targeting.LINE,source.targeting());
        assertEquals(RpgSkillCatalog.Targeting.SELF,followup.targeting());
        assertEquals(source.id(), followup.mechanics().prerequisiteSkill());
        assertTrue(source.mechanics().checkpointTicks() > 0);
        assertEquals(source.mechanics().checkpointTicks(), followup.mechanics().checkpointTicks());
        assertEquals(1, RpgSkillCatalog.all().stream().filter(s -> !s.mechanics().prerequisiteSkill().isEmpty()).count());
    }

    @Test void chainAndSentryKeepTheRequestedCounts() {
        var chain = RpgSkillCatalog.get("thunder_3").mechanics();
        assertEquals(RpgSkillCatalog.Pattern.CHAIN, chain.pattern());
        assertEquals(4, chain.hits());
        assertTrue(chain.repeatDamagePermille() > 0 && chain.repeatDamagePermille() < 1000);
        assertEquals(4, RpgSkillCatalog.get("thunder_5").mechanics().hits());
        var sentry = RpgSkillCatalog.get("aetheri_2").mechanics();
        assertEquals(RpgSkillCatalog.Pattern.SENTRY, sentry.pattern());
        assertEquals(120, sentry.durationTicks());
        assertEquals(RpgSkillCatalog.Targeting.SELF,RpgSkillCatalog.get("aetheri_2").targeting());
        assertTrue(sentry.intervalTicks() > 0 && sentry.intervalTicks() < RpgWorld.BASIC_ATTACK_TICKS);
    }

    @Test void harmlessControlAndMobOnlyHybridHealingRemainExplicit() {
        for (String id : List.of("spirit_4", "spirit_5")) {
            var skill = RpgSkillCatalog.get(id);
            assertEquals(0, skill.baseDamage());
            assertEquals(RpgSkillCatalog.DamageScope.NONE, skill.damageScope());
            assertTrue(skill.effects().stream().allMatch(e -> e.baseDamage() == 0));
        }
        var basic = RpgSkillCatalog.get("light_1");
        var strong = RpgSkillCatalog.get("light_5");
        for (var skill : List.of(basic, strong)) {
            assertEquals(RpgSkillCatalog.DamageScope.MOBS_ONLY, skill.damageScope());
            assertEquals(RpgSkillCatalog.Targeting.TARGET, skill.targeting());
            assertTrue(skill.healing() > 0 && skill.baseDamage() > 0);
        }
        assertTrue(strong.healing() > basic.healing());
        assertEquals(RpgSkillCatalog.Pattern.REVIVE, RpgSkillCatalog.get("light_3").mechanics().pattern());
        assertEquals(RpgSkillCatalog.Pattern.CLEANSE, RpgSkillCatalog.get("light_4").mechanics().pattern());
        assertEquals(RpgSkillCatalog.Targeting.ALLY, RpgSkillCatalog.get("light_3").targeting());
    }

    @Test void collectionsAreImmutableAndInvalidEffectsAreRefused() {
        assertThrows(UnsupportedOperationException.class, () -> RpgSkillCatalog.all().clear());
        assertThrows(UnsupportedOperationException.class, () -> RpgSkillCatalog.forKit("water").clear());
        assertThrows(UnsupportedOperationException.class, () -> RpgSkillCatalog.get("water_1").effects().clear());
        assertThrows(IllegalArgumentException.class, () -> new RpgSkillCatalog.Effect(RpgSkillCatalog.EffectType.ROOT, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new RpgSkillCatalog.Effect(RpgSkillCatalog.EffectType.ROOT, 60, 0, 30, 3));
        assertThrows(IllegalArgumentException.class, () -> new RpgSkillCatalog.Effect(RpgSkillCatalog.EffectType.BURN, 60, 0, 0, 3));
        assertThrows(IllegalArgumentException.class, () -> new RpgSkillCatalog.Effect(RpgSkillCatalog.EffectType.SLOW, 60, 1000, 0, 0));
    }

    @Test void glacialArrowAndSevenArrowFanKeepTheirRequestedIdentityWithoutInventingStun() {
        var arrow = RpgSkillCatalog.get("aetheri_4");
        assertEquals("Flecha de energia", arrow.name());
        assertEquals(RpgSkillCatalog.Pattern.LINE_PROJECTILE, arrow.mechanics().pattern());
        assertTrue(arrow.effects().isEmpty());
        assertEquals(7, RpgSkillCatalog.get("aetheri_5").mechanics().hits());
    }
}
