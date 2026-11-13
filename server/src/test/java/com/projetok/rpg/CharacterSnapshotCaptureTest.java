package com.projetok.rpg;

import com.projetok.domain.entity.Position;
import com.projetok.domain.movement.CollisionService;
import com.projetok.rpg.recovery.*;
import java.io.DataInputStream;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;
import static com.projetok.rpg.CharacterSnapshotCapture.*;
import static com.projetok.rpg.recovery.RecoverableCharacterSnapshot.*;

/** Provas R2 e adaptação R3 em objetos descartáveis, sem reflexão de campos privados. */
@Timeout(20)
class CharacterSnapshotCaptureTest {
    private final CharacterSnapshotCapture capture=new CharacterSnapshotCapture();

    /** Executa preparação, leituras e verificações na mesma thread proprietária. */
    private <T>T owner(CallableOnFixture<T> action)throws Exception {
        var executor=Executors.newSingleThreadExecutor(r->new Thread(r,"org03-r2-owner"));
        try{return executor.submit(()->action.call(new Fixture())).get(15,TimeUnit.SECONDS);}
        finally{executor.shutdownNow();assertTrue(executor.awaitTermination(3,TimeUnit.SECONDS));}
    }
    /** Ação da fixture, nunca injetada dentro do componente de captura. */
    @FunctionalInterface private interface CallableOnFixture<T>{T call(Fixture fixture)throws Exception;}

    @Test void coherentOwnerReadMatchesRuntimeAndRepeatsExactly()throws Exception {
        owner(f->{var before=f.world.networkState();var first=f.read();
            assertEquals(Outcome.INCOMPLETE_CAPTURE,first.outcome());assertEquals(before,f.world.networkState());
            assertEquals(first,f.read());assertEquals(before.ticks(),first.candidate().capture().sourceTick().value());return null;});
    }

    @Test void captureDoesNotAdvanceOrAlterAvailableAuthoritativeState()throws Exception {
        owner(f->{var state=f.world.networkState();var progress=f.world.progressView();var domain=f.world.domainProgression();
            var inventory=f.world.inventory().snapshot();var equipped=f.world.equippedItems();var quests=f.world.authoredQuestViews();
            var abilities=f.world.abilityView();var stats=f.world.manualAttributes();var events=f.engine.events();
            f.read();
            assertEquals(state,f.world.networkState());assertEquals(progress,f.world.progressView());assertEquals(domain,f.world.domainProgression());
            assertEquals(inventory,f.world.inventory().snapshot());assertEquals(equipped,f.world.equippedItems());
            assertEquals(quests,f.world.authoredQuestViews());assertEquals(abilities,f.world.abilityView());
            // A fixture já materializou os pontos no preparo; esta consulta posterior não é captura.
            assertEquals(stats,f.world.manualAttributes());assertEquals(events,f.engine.events());return null;});
    }

    @Test void capturedCopiesRemainImmutableAfterRealInventoryChanges()throws Exception {
        owner(f->{var result=f.read();var slots=result.candidate().inventory().slots();
            assertThrows(UnsupportedOperationException.class,slots::clear);
            assertThrows(UnsupportedOperationException.class,()->result.observations().authoredSummaries().clear());
            assertThrows(UnsupportedOperationException.class,()->result.gaps().clear());
            int captured=slots.stream().filter(s->s.itemId().equals("potion")).mapToInt(Slot::quantity).sum();
            assertTrue(f.world.inventory().add("potion",1));
            assertEquals(captured+1,f.world.inventory().count("potion"));
            assertEquals(captured,slots.stream().filter(s->s.itemId().equals("potion")).mapToInt(Slot::quantity).sum());return null;});
    }

    @Test void xpDomainHalfUnitsAndResetMetadataAreReadExactly()throws Exception {
        owner(f->{f.killRealSlime();var before=f.world.domainProgression();var result=f.read();
            assertEquals(f.world.progressView().xp(),result.candidate().progression().xp());
            assertEquals(before.xpHalfUnits(),result.candidate().progression().domainXpHalfUnits());
            assertEquals(before.lastReset(),result.candidate().progression().lastReset());
            assertEquals(before.resetQuestComplete(),result.candidate().progression().resetQuestComplete());
            assertEquals(3,before.xpHalfUnits());return null;});
    }

    @Test void rawAttributesAndGrantStateAreCapturedWithoutNewGrants()throws Exception {
        owner(f->{var stats=f.world.manualAttributes();var result=f.read();
            assertEquals(stats.agility(),result.observations().attributes().effectiveAgility());
            assertEquals(stats.energy(),result.observations().attributes().trainedEnergy());
            assertEquals(stats.strength(),result.candidate().attributes().strength());
            assertEquals(stats.unspentPoints(),result.candidate().attributes().unspentPoints());
            assertEquals(stats,f.world.manualAttributes());return null;});
    }

    @Test void allTwentySlotsAndQuantitiesKeepTheirActualIndices()throws Exception {
        owner(f->{assertTrue(f.world.inventory().move(0,19,1));var source=f.world.inventory().slots();var result=f.read();
            assertEquals(20,result.candidate().inventory().capacity());
            for(var slot:result.candidate().inventory().slots()) {
                assertEquals(source.get(slot.index()).id(),slot.itemId());assertEquals(source.get(slot.index()).quantity(),slot.quantity());
            }
            assertTrue(result.candidate().inventory().slots().stream().anyMatch(s->s.index()==19));return null;});
    }

    @Test void equippedSwordIsNotAddedBackToTheBag()throws Exception {
        owner(f->{assertEquals(0,f.world.inventory().count("sword"));var result=f.read();
            assertTrue(result.candidate().equipment().contains(new Equipment("weapon","sword")));
            assertTrue(result.candidate().inventory().slots().stream().noneMatch(s->s.itemId().equals("sword")));
            assertEquals(0,f.world.inventory().count("sword"));return null;});
    }

    @Test void actualKillCoinsAreExportedWithoutAnotherReward()throws Exception {
        owner(f->{f.killRealSlime();assertEquals(5,f.world.networkState().coins());var result=f.read();
            assertEquals(5,result.candidate().wallet().coins());assertEquals(5,f.world.networkState().coins());
            assertEquals(result,f.read());return null;});
    }

    @Test void authoredSummariesArePreservedWithoutPretendingToBeObjectiveLedgers()throws Exception {
        owner(f->{var before=f.world.authoredQuestViews();assertEquals(QuestService.State.READY,before.get(0).state());
            var result=f.read();var summary=result.observations().authoredSummaries().get(0);
            assertEquals(before.get(0).progress(),summary.progress());assertEquals(before.get(0).required(),summary.required());
            assertEquals(before.get(0).objective(),summary.description());assertNull(result.candidate().quests());
            assertGap(result,"quests.definitionVersion/deliveryProof");assertEquals(before,f.world.authoredQuestViews());return null;});
    }

    @Test void missingReceiptsAndLootRightsAreUnavailableRatherThanEmptyCollections()throws Exception {
        owner(f->{var result=f.read();assertNull(result.candidate().receipts());assertNull(result.candidate().pendingLoot());
            assertNull(result.candidate().watermarks());assertGap(result,"receipts");assertGap(result,"pendingLoot");
            assertEquals(GapKind.NOT_PRESENT,result.gaps().stream().filter(g->g.path().equals("receipts")).findFirst().orElseThrow().kind());return null;});
    }

    @Test void roundedBasicCooldownNeverBecomesAnExactDeadline()throws Exception {
        owner(f->{f.killRealSlime();double interval=f.world.basicAttackInterval();assertNotEquals(Math.floor(interval),interval);
            int remaining=f.world.abilityView().attackTicks();assertTrue(remaining>0);var result=f.read();
            assertEquals(remaining,result.observations().remaining().basicAttackTicks());
            assertNull(result.candidate().cooldowns());assertGap(result,"cooldowns.policy/clock");
            assertEquals(remaining,f.world.abilityView().attackTicks());return null;});
    }

    @Test void hpAndAbsoluteEnergyStayBelowMaximumWithoutRefill()throws Exception {
        owner(f->{var before=f.world.networkState();assertTrue(before.hp()<before.maxHp());
            assertTrue(before.stamina()<f.world.maximumEnergy());var result=f.read();
            assertEquals(before.hp(),result.observations().resources().hp());
            assertEquals(before.stamina(),result.observations().resources().absoluteEnergy());
            assertEquals(before.stamina(),result.candidate().resources().energyAbsolute());
            assertEquals(result.observations().exactState().regenerationRemainder(),result.candidate().resources().regenerationRemainder());
            assertEquals(before,f.world.networkState());return null;});
    }

    @Test void deadCharacterIsNotResurrectedOrRefilled()throws Exception {
        owner(f->{f.world.receiveDamage(10000);assertEquals(0,f.world.networkState().hp());var before=f.world.networkState();
            var result=f.read();assertEquals(LifeState.DEAD,result.observations().resources().life());
            assertEquals(0,result.observations().resources().hp());assertEquals(before,f.world.networkState());return null;});
    }

    @Test void actualLightningPhaseRemainsActiveButIsNotInTheCandidate()throws Exception {
        owner(f->{assertTrue(f.engine.cast(1,f.world,new RpgSkillEngine.Cast(5,0,0,0,0),Map.of(1,f.world)));
            assertTrue(f.world.skillIntangible());var before=f.world.networkState();var events=f.engine.events();
            var result=f.read();assertTrue(f.world.skillIntangible());assertEquals(before,f.world.networkState());
            assertEquals(events,f.engine.events());assertNull(result.candidate().restrictions());
            assertFalse(Arrays.stream(result.candidate().getClass().getRecordComponents()).anyMatch(c->c.getName().equals("intangible")));return null;});
    }

    @Test void activeNegativeControlIsNotClearedAndRemainsAPolicyGap()throws Exception {
        owner(f->{f.world.skillApplyControl(new RpgSkillCatalog.Effect(RpgSkillCatalog.EffectType.ROOT,30,0,0,0));
            assertTrue(f.world.skillMovementBlocked());var result=f.read();assertTrue(f.world.skillMovementBlocked());
            assertNull(result.candidate().restrictions());assertGap(result,"restrictions.externalEvidence");
            assertTrue(result.gaps().stream().anyMatch(g->g.path().equals("restrictions.policy")&&g.kind()==GapKind.POLICY_PENDING));return null;});
    }

    @Test void changedRegionOrControllerContextRefusesTheOldCaptureRequest()throws Exception {
        owner(f->{assertTrue(f.world.travel("to_forest"));var result=f.read();
            assertEquals(Outcome.INCOMPATIBLE_CONTEXT,result.outcome());assertNull(result.candidate());return null;});
        owner(f->{var old=f.context;f.source.changeContext(new LaboratoryContext(old.runtimeIdentity(),old.syntheticIdentity(),old.region(),
                    new ContentHash("b".repeat(64)),old.rules(),old.epoch(),old.revision(),old.capturedAt()));
            assertEquals(Outcome.INCOMPATIBLE_CONTEXT,f.read().outcome());return null;});
    }

    @Test void retiredLaboratoryParticipationHasNoCandidateOrZeroedSkillFallback()throws Exception {
        owner(f->{assertTrue(f.engine.cast(1,f.world,new RpgSkillEngine.Cast(5,0,0,0,0),Map.of(1,f.world)));
            assertTrue(f.engine.state(1).cooldowns().stream().anyMatch(t->t>0));
            f.engine.remove(1);f.encounter.leave(f.world);f.source.retire();
            assertTrue(f.engine.state(1).cooldowns().stream().allMatch(t->t==0));
            var result=f.read();
            assertEquals(Outcome.SOURCE_REMOVED,result.outcome());assertNull(result.candidate());
            assertNull(result.observations());return null;});
    }

    @Test void incompleteCandidateStaysRejectedByR1AndValidationRepeatsWithoutEffects()throws Exception {
        owner(f->{var result=f.read();assertFalse(result.validation().structurallyValid());assertFalse(result.validation().readyForActivation());
            assertTrue(result.validation().pendingChecks().containsAll(Set.of(CharacterSnapshotValidator.PendingCheck.AUTHORIZATION,
                    CharacterSnapshotValidator.PendingCheck.WORLD_PLACEMENT,CharacterSnapshotValidator.PendingCheck.DURABLE_COMMIT,
                    CharacterSnapshotValidator.PendingCheck.CLOCK_POLICY)));
            var before=f.world.networkState();for(int i=0;i<20;i++)assertEquals(result.validation(),new CharacterSnapshotValidator().validate(result.candidate(),f.validation));
            assertEquals(before,f.world.networkState());return null;});
    }

    @Test void wrongThreadIsRejectedBeforeReadingTheWorld()throws Exception {
        var executor=Executors.newSingleThreadExecutor();
        try {var f=executor.submit(Fixture::new).get(10,TimeUnit.SECONDS);var result=f.read();
            assertEquals(Outcome.SOURCE_UNAVAILABLE,result.outcome());assertNull(result.candidate());}
        finally{executor.shutdownNow();assertTrue(executor.awaitTermination(3,TimeUnit.SECONDS));}
    }

    @Test void explicitSyntheticIdentityIsSeparateAndInvalidMetadataIsDiagnosed()throws Exception {
        owner(f->{var result=f.read();assertNotEquals(result.candidate().characterId().value(),result.observations().identity().ephemeralId());
            var old=f.context;f.context=new LaboratoryContext(old.runtimeIdentity(),old.syntheticIdentity(),old.region(),old.content(),old.rules(),
                    old.epoch(),new StateRevision(-1),old.capturedAt());f.source.changeContext(f.context);
            assertEquals(Outcome.STRUCTURALLY_INVALID,f.read().outcome());return null;});
    }

    @Test void captureBytecodeHasNoMutatingSaveLazyAttributeOrReflectionCalls()throws Exception {
        try(var input=new DataInputStream(Objects.requireNonNull(CharacterSnapshotCapture.class.getResourceAsStream("CharacterSnapshotCapture.class")))) {
            assertEquals(0xCAFEBABE,input.readInt());input.skipNBytes(4);int count=input.readUnsignedShort();var symbols=new HashSet<String>();
            for(int i=1;i<count;i++)switch(input.readUnsignedByte()) {
                case 1->symbols.add(input.readUTF());case 3,4->input.skipNBytes(4);case 5,6->{input.skipNBytes(8);i++;}
                case 7,8,16,19,20->input.skipNBytes(2);case 9,10,11,12,17,18->input.skipNBytes(4);case 15->input.skipNBytes(3);
                default->throw new AssertionError("Tag desconhecida no constant pool");
            }
            for(String forbidden:List.of("manualAttributes","save","snapshot","awardExperience","tick","cast","equip","unequip",
                    "collectLoot","completeQuest","completeAuthoredQuest","allocateAttribute","skillClearControls","getDeclaredField"))
                assertFalse(symbols.contains(forbidden),forbidden);
        }
    }

    /** Consulta uma lacuna sem depender do texto de mensagens do runtime. */
    private static void assertGap(CaptureResult result,String path) {assertTrue(result.gaps().stream().anyMatch(g->g.path().equals(path)),path);}

    @Test void exactLedgerAndResourcesStillDoNotAuthorizeEconomicActivation()throws Exception {
        owner(f->{assertTrue(f.world.completeAuthoredQuest("capture_trial"));var before=f.world.networkState();
            var result=f.read();var exact=result.observations().exactState();
            assertEquals(QuestService.State.COMPLETED,exact.authoredQuests().states().get("capture_trial"));
            assertTrue(exact.authoredQuests().flags().contains("quest-done:capture_trial"));
            assertNotNull(result.candidate().resources());assertNotNull(result.candidate().attributes());
            assertNull(result.candidate().receipts());assertNull(result.candidate().quests());
            assertFalse(result.validation().readyForActivation());assertEquals(before,f.world.networkState());return null;});
    }

    @Test void engineAbsenceAndPresentReadyTimingRemainDifferentInAdapter()throws Exception {
        owner(f->{var absent=f.read();assertEquals(RpgSkillEngine.TimingStatus.ABSENT,absent.observations().skillTiming().status());
            assertNull(absent.observations().skillTiming().deadlines());
            assertFalse(f.engine.cast(1,f.world,new RpgSkillEngine.Cast(1,0,0,280,320),Map.of(1,f.world)));
            var present=f.read();assertEquals(RpgSkillEngine.TimingStatus.PRESENT,present.observations().skillTiming().status());
            assertEquals(5,present.observations().skillTiming().deadlines().size());
            assertFalse(present.gaps().stream().anyMatch(g->g.path().equals("cooldowns.enginePresence")));
            assertNull(present.candidate().cooldowns());return null;});
    }

    /** Mundo sintético real com catálogos, bolsa20, recompensa real e comandos anteriores à captura. */
    private final class Fixture {
        final RpgWorld world;final SharedRpgEncounter encounter;final RpgSkillEngine engine=new RpgSkillEngine();
        final Source source;final SnapshotValidationContext validation;LaboratoryContext context;

        /** Prepara conteúdo e operações reais em memória antes de entregar a fonte ao capturador. */
        Fixture() {
            var space=new CollisionService.Space(960,640,List.of(),List.of());
            var maps=Map.of("village",new RpgSceneSet.Geometry(space,new Position(140,320)),"forest",new RpgSceneSet.Geometry(space,new Position(140,320)));
            var enemies=Map.of("capture_slime","slime|forest|220|320");
            var scenes=new RpgSceneSet("village",maps,enemies,Map.of("elder","village|190|320"),Map.of(),
                    Map.of("to_forest","village|140|320|forest|140|320"));
            var definition=new QuestService.Definition(List.of(new QuestService.Objective("talk","elder",1),new QuestService.Objective("collect","potion",2)),3,5,"potion",1);
            var book=new QuestBook(1,List.of(new QuestBook.Draft("capture_trial","Captura","Missão descartável","elder","village","",1,"",false,List.of(),definition,false)));
            var race=CharacterRaces.get("reaper");
            var identity=new CharacterIdentity(UUID.fromString("11234567-89ab-cdef-0123-456789abcdef"),
                    "Captura Laboratorio",race.id(),"azure","village",race.hp(),race.attack());
            world=new RpgWorld(identity,scenes,book);
            world.useNetworkFootprint();encounter=new SharedRpgEncounter(Map.of("village",space,"forest",space),enemies,SharedRpgEncounter.RewardPolicy.SYNTHETIC_PARTICIPANTS);
            encounter.join(1,world);world.awardExperience(RpgProgression.builtin().threshold(20));assertTrue(world.chooseDomain("thunder"));
            for(int i=0;i<4;i++)assertTrue(world.allocateAttribute("strength"));
            for(int i=0;i<3;i++)assertTrue(world.allocateAttribute("intelligence"));
            for(int i=0;i<2;i++)assertTrue(world.allocateAttribute("agility"));
            for(int i=0;i<3;i++)assertTrue(world.allocateAttribute("energy"));
            assertTrue(world.inventory().add("sword",1));assertTrue(world.equip("sword"));
            assertTrue(world.interactSyntheticNpc());assertTrue(world.acceptQuest("first_steps"));assertTrue(world.acceptAuthoredQuest("capture_trial"));
            world.receiveDamage(17);assertTrue(world.skillSpend(7));world.tick(0,0);
            var hash=new ContentHash("c".repeat(64));var rules=new RulesVersion("lab-capture-v1");var epoch=new WorldEpoch(7);
            var now=Instant.parse("2026-11-11T03:00:00Z");
            context=new LaboratoryContext(world.networkState().identity().id(),CharacterId.parse("01234567-89ab-cdef-0123-456789abcdef"),
                    "village",hash,rules,epoch,new StateRevision(10),now);
            source=new Source(world,context);source.bindSkills(engine,1);
            validation=new SnapshotValidationContext(RpgProgression.builtin(),new ItemCatalog(RpgContent.builtin()),null,
                    Map.of("village",new SnapshotValidationContext.RegionBounds(18,18,942,622),"forest",new SnapshotValidationContext.RegionBounds(18,18,942,622)),
                    Set.of("none"),hash,rules,epoch,9L,now,null);
        }
        /** Encaminha apenas uma leitura; nunca normaliza ou completa dados indisponíveis. */
        CaptureResult read(){return capture.capture(source,context,validation);}
        /** Viagem e morte real do slime de40HP para obter XP/moedas/ledger sem editar suas regras. */
        void killRealSlime() {
            assertTrue(world.travel("to_forest"));assertTrue(world.attack());
            assertTrue(world.skillEnemies().stream().allMatch(e->e.hp==0));
            var old=context;context=new LaboratoryContext(old.runtimeIdentity(),old.syntheticIdentity(),"forest",old.content(),old.rules(),old.epoch(),old.revision(),old.capturedAt());
            source.changeContext(context);
        }
    }
}
