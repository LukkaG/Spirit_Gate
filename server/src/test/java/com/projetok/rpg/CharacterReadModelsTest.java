package com.projetok.rpg;

import com.projetok.domain.entity.Position;
import com.projetok.domain.movement.CollisionService;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

/** Provas de observação pura em mundos descartáveis com catálogos e operações reais. */
@Timeout(20)
class CharacterReadModelsTest {
    /** Prepara a simulação inteiramente no owner do teste, sem save, socket ou reflexão. */
    static final class Fixture {
        final RpgWorld world;
        final RpgSkillEngine engine=new RpgSkillEngine();
        final Map<Integer,RpgWorld> participants=new HashMap<>();
        CharacterReadContext context;

        /** Mantém portal longe do NPC para não viajar ao preparar missões. */
        Fixture() {
            var space=new CollisionService.Space(960,640,List.of(),List.of());
            var scenes=new RpgSceneSet("village",Map.of(
                    "village",new RpgSceneSet.Geometry(space,new Position(140,320)),
                    "forest",new RpgSceneSet.Geometry(space,new Position(140,320))),
                    Map.of("read_slime","slime|forest|220|320"),
                    Map.of("elder","village|190|320"),Map.of(),
                    Map.of("to_forest","village|140|320|forest|140|320"));
            var definition=new QuestService.Definition(List.of(new QuestService.Objective("talk","elder",1),
                    new QuestService.Objective("collect","potion",2)),3,5,"potion",1);
            var book=new QuestBook(1,List.of(new QuestBook.Draft("read_trial","Leitura","Teste de ledger",
                    "elder","village","",1,"",false,List.of(),definition,false)));
            world=new RpgWorld(CharacterIdentity.create("Leitura Teste","reaper","azure"),scenes,book);
            world.useNetworkFootprint();
            var encounter=new SharedRpgEncounter(Map.of("village",space,"forest",space),
                    scenes.enemies(),SharedRpgEncounter.RewardPolicy.SYNTHETIC_PARTICIPANTS);
            encounter.join(1,world);participants.put(1,world);renew();
        }

        /** Reabre o escopo apenas após uma mudança explícita do preparo. */
        void renew(){context=new CharacterReadContext(world,engine);}
        /** Materializa atributos somente no preparo, usando as regras existentes. */
        void train(){world.awardExperience(RpgProgression.builtin().threshold(20));
            assertTrue(world.chooseDomain("thunder"));
            for(String attribute:List.of("strength","intelligence","agility","energy"))
                assertTrue(world.allocateAttribute(attribute));
            renew();}
        /** Leitura exclusiva do estado bruto no contexto atual. */
        CharacterReadState read(){return world.readCharacterState(context);}
        /** Leitura do executor sem state(), cast ou inicialização lazy durante a observação. */
        RpgSkillEngine.TimingRead timing(){return engine.readTiming(1,world,participants,context);}
        /** Cria sessão real pelo caminho de validação, sem lançar projétil por falta de alvo. */
        void openReadySession(){train();assertFalse(engine.cast(1,world,
                new RpgSkillEngine.Cast(1,0,0,220,320),participants));}
    }

    @Test void repeatedReadDoesNotAdvanceOrMutate(){var f=new Fixture();var before=f.world.networkState();
        var first=f.read();assertEquals(first,f.read());assertEquals(before,f.world.networkState());}

    @Test void disabledManualModeStaysDisabled(){var f=new Fixture();
        f.world.awardExperience(RpgProgression.builtin().threshold(20));var state=f.read();
        assertFalse(state.manualAttributesEnabled());assertEquals(1,state.rawAttributes().level());
        assertEquals(0,state.rawAttributes().unspentPoints());assertEquals(state,f.read());}

    @Test void rawInvestmentsAreExact(){var f=new Fixture();f.train();var stats=f.world.manualAttributes();
        assertEquals(stats,f.read().rawAttributes());assertEquals(stats,f.world.manualAttributes());}

    @Test void freePointsAreNotGrantedAgain(){var f=new Fixture();f.train();
        int points=f.world.manualAttributes().unspentPoints();for(int i=0;i<30;i++)
            assertEquals(points,f.read().rawAttributes().unspentPoints());}

    @Test void grantCheckpointIsNotRecalculatedFromXp(){var f=new Fixture();f.train();
        int checkpoint=f.world.manualAttributes().level();
        f.world.awardExperience(RpgProgression.builtin().threshold(30));
        assertTrue(f.world.level()>checkpoint);assertEquals(checkpoint,f.read().rawAttributes().level());}

    @Test void regenerationFractionKeepsExactDoubleBits(){var f=new Fixture();f.train();
        assertTrue(f.world.skillSpend(7));f.world.tick(0,0);var first=f.read();
        assertTrue(first.regenerationRemainder()>0);assertTrue(first.regenerationRemainder()<1);
        assertEquals(Double.doubleToRawLongBits(first.regenerationRemainder()),
                Double.doubleToRawLongBits(f.read().regenerationRemainder()));}

    @Test void resourcesAreNotHealedOrRegenerated(){var f=new Fixture();f.train();
        f.world.receiveDamage(17);assertTrue(f.world.skillSpend(7));var before=f.world.networkState();
        for(int i=0;i<20;i++){var state=f.read();assertEquals(before.hp(),state.hp());assertEquals(before.stamina(),state.energy());}
        assertEquals(before,f.world.networkState());}

    @Test void slotsAreImmutableAndPreserveNullIndices(){var f=new Fixture();var state=f.read();
        assertEquals(20,state.slots().size());assertNull(state.slots().get(19));
        assertThrows(UnsupportedOperationException.class,()->state.slots().set(0,null));}

    @Test void equipmentIsNotDuplicatedIntoInventory(){var f=new Fixture();
        assertTrue(f.world.inventory().add("sword",1));assertTrue(f.world.equip("sword"));var state=f.read();
        assertEquals("sword",state.equipment().get("weapon"));
        assertTrue(state.slots().stream().filter(Objects::nonNull).noneMatch(s->s.id().equals("sword")));
        assertThrows(UnsupportedOperationException.class,()->state.equipment().clear());}

    @Test void xpAndDomainStayExact(){var f=new Fixture();f.train();var state=f.read();
        assertEquals(f.world.progressView().xp(),state.xp());assertEquals(f.world.domainProgression(),state.domain());
        assertEquals(state,f.read());}

    @Test void initialNaNIsExplicitRatherThanInventedDeadline(){var f=new Fixture();
        assertTrue(Double.isNaN(f.read().preciseAttackReady()));assertEquals(f.read(),f.read());}

    @Test void fractionalBasicDeadlineKeepsExactBits(){var f=new Fixture();f.train();
        assertTrue(f.world.travel("to_forest"));f.renew();assertTrue(f.world.attack());var state=f.read();
        assertTrue(Double.isFinite(state.preciseAttackReady()));
        assertNotEquals(Math.floor(state.preciseAttackReady()),state.preciseAttackReady());
        assertEquals(Double.doubleToRawLongBits(state.preciseAttackReady()),
                Double.doubleToRawLongBits(f.read().preciseAttackReady()));
        assertEquals((long)Math.ceil(state.preciseAttackReady()),state.attackReadyTick());}

    @Test void shunpoPotionAndTravelDeadlinesDoNotMove(){var f=new Fixture();f.train();
        f.world.receiveDamage(17);assertTrue(f.world.use("potion"));
        assertTrue(f.world.travel("to_forest"));f.renew();assertTrue(f.world.castForwardAbility());var state=f.read();
        assertTrue(state.shunpoReadyTick()>state.tick());assertTrue(state.potionReadyTick()>state.tick());
        assertTrue(state.travelReadyTick()>state.tick());assertEquals(state,f.read());}

    @Test void fiveSkillDeadlinesUseRealCatalogIds(){var f=new Fixture();f.train();
        assertTrue(f.engine.cast(1,f.world,new RpgSkillEngine.Cast(5,0,0,280,320),f.participants));
        var timing=f.timing();assertEquals(RpgSkillEngine.TimingStatus.PRESENT,timing.status());
        assertEquals(5,timing.deadlines().size());var kit=RpgSkillCatalog.forDomain("thunder");
        for(int i=0;i<5;i++){assertEquals(kit.get(i).id(),timing.deadlines().get(i).skillId());
            assertEquals(i==4?f.world.skillTick()+kit.get(i).cooldownTicks():0,timing.deadlines().get(i).readyAtTick());}
        assertEquals(timing,f.timing());assertThrows(UnsupportedOperationException.class,()->timing.deadlines().clear());}

    @Test void absentSkillSessionIsNotFiveZeroes(){var f=new Fixture();f.train();
        assertEquals(RpgSkillEngine.TimingStatus.ABSENT,f.timing().status());assertNull(f.timing().deadlines());}

    @Test void presentReadySessionIsDistinguishedFromAbsent(){var f=new Fixture();f.openReadySession();
        var timing=f.timing();assertEquals(RpgSkillEngine.TimingStatus.PRESENT,timing.status());
        assertTrue(timing.deadlines().stream().allMatch(d->d.readyAtTick()==0));}

    @Test void worldBindingMismatchIsRejected(){var f=new Fixture();f.openReadySession();var other=new Fixture();
        var timing=f.engine.readTiming(1,other.world,Map.of(1,other.world),new CharacterReadContext(other.world,f.engine));
        assertEquals(RpgSkillEngine.TimingStatus.INCOMPATIBLE,timing.status());assertNull(timing.deadlines());}

    @Test void changedDomainRefusesOldContext(){var f=new Fixture();var old=f.context;f.train();
        assertThrows(IllegalStateException.class,()->f.world.readCharacterState(old));}

    @Test void changedRegionRejectsStaleSkillSessionWithoutCleaning(){var f=new Fixture();f.openReadySession();
        assertTrue(f.world.travel("to_forest"));f.renew();
        assertEquals(RpgSkillEngine.TimingStatus.INCOMPATIBLE,f.timing().status());
        assertEquals(RpgSkillEngine.TimingStatus.INCOMPATIBLE,f.timing().status());}

    @Test void removedEngineSessionRemainsAbsent(){var f=new Fixture();f.openReadySession();f.engine.remove(1);
        assertEquals(RpgSkillEngine.TimingStatus.ABSENT,f.timing().status());assertNull(f.timing().deadlines());}

    @Test void absentQueriesDoNotCreateLazySession(){var f=new Fixture();f.train();
        for(int i=0;i<40;i++){assertEquals(RpgSkillEngine.TimingStatus.ABSENT,f.timing().status());assertNull(f.timing().deadlines());}}

    @Test void participantRemovalIsRejectedEvenWithRetainedWorld(){var f=new Fixture();f.openReadySession();
        f.participants.clear();assertEquals(RpgSkillEngine.TimingStatus.INCOMPATIBLE,f.timing().status());
        f.context.retire();assertThrows(IllegalStateException.class,f::read);}

    @Test void builtinLedgerKeepsCountersFlagsAndDefinitions(){var f=new Fixture();
        assertTrue(f.world.interactSyntheticNpc());assertTrue(f.world.acceptQuest("first_steps"));
        var state=f.read();var ledger=state.builtinQuests();assertTrue(ledger.progress().containsKey("first_steps"));
        assertTrue(ledger.flags().stream().anyMatch(flag->flag.startsWith("qe:talk:")));
        assertEquals(new QuestService(RpgContent.builtin()).definition("first_steps"),ledger.definitions().get("first_steps"));
        assertEquals(state,f.read());assertThrows(UnsupportedOperationException.class,()->ledger.flags().clear());
        assertThrows(UnsupportedOperationException.class,()->ledger.progress().clear());}

    @Test void authoredLedgerDoesNotAwardAgain(){var f=new Fixture();assertTrue(f.world.interactSyntheticNpc());
        assertTrue(f.world.acceptAuthoredQuest("read_trial"));assertTrue(f.world.completeAuthoredQuest("read_trial"));
        var state=f.read();assertEquals(QuestService.State.COMPLETED,state.authoredQuests().states().get("read_trial"));
        assertTrue(state.authoredQuests().flags().contains("quest-done:read_trial"));
        for(int i=0;i<20;i++)assertEquals(state,f.read());
        assertEquals(state.coins(),f.world.networkState().coins());assertEquals(state.xp(),f.world.progressView().xp());}

    @Test void historicalFlagsStayLocalAndUnchanged(){var f=new Fixture();
        assertTrue(f.world.interactSyntheticNpc());assertTrue(f.world.acceptAuthoredQuest("read_trial"));
        assertTrue(f.world.completeAuthoredQuest("read_trial"));var ledger=f.read().authoredQuests();
        assertTrue(ledger.flags().contains("quest-done:read_trial"));
        int coins=f.world.networkState().coins();
        assertTrue(ledger.flags().stream().anyMatch(flag->flag.startsWith("qe:")));
        assertFalse(ledger.flags().stream().anyMatch(flag->flag.startsWith("receipt:")));
        assertEquals(ledger,f.read().authoredQuests());assertEquals(coins,f.world.networkState().coins());}

    @Test void adverseControlsAreObservedWithoutCleanup(){var f=new Fixture();f.train();
        f.world.skillApplyControl(new RpgSkillCatalog.Effect(RpgSkillCatalog.EffectType.ROOT,37,0,0,0));
        var state=f.read();assertTrue(f.world.skillMovementBlocked());
        assertEquals(f.world.skillTick()+37,state.restrictions().rootUntil());assertFalse(state.restrictions().externalEffectsKnown());
        assertEquals(state,f.read());assertTrue(f.world.skillMovementBlocked());}

    @Test void intangibilityRemainsObservationOnly(){var f=new Fixture();f.train();
        assertTrue(f.engine.cast(1,f.world,new RpgSkillEngine.Cast(5,0,0,280,320),f.participants));
        var state=f.read();var events=f.engine.events();assertTrue(state.restrictions().intangible());
        assertEquals(state,f.read());assertEquals(events,f.engine.events());}

    @Test void wrongThreadIsRejectedBeforeRead()throws Exception {var f=new Fixture();var executor=Executors.newSingleThreadExecutor();
        try {executor.submit(()->{assertThrows(IllegalStateException.class,f::read);
            assertThrows(IllegalStateException.class,f::timing);return null;}).get(5,TimeUnit.SECONDS);}
        finally{executor.shutdownNow();assertTrue(executor.awaitTermination(3,TimeUnit.SECONDS));}}

    @Test void copiedStateDoesNotFollowFutureWorldChanges(){var f=new Fixture();var state=f.read();
        int old=state.slots().get(0).quantity();assertTrue(f.world.inventory().add("potion",1));
        f.world.receiveDamage(7);f.world.tick(0,0);
        assertEquals(old,state.slots().get(0).quantity());assertNotEquals(state,f.read());
        assertThrows(UnsupportedOperationException.class,()->state.authoredBook().quests().clear());
        assertThrows(UnsupportedOperationException.class,()->state.builtinQuests().definitions().clear());
        assertThrows(UnsupportedOperationException.class,()->state.authoredQuests().definitions().get("read_trial").objectives().clear());}

    @Test void failedReadDoesNotPartiallyMutate(){var f=new Fixture();var state=f.read();var other=new Fixture();
        assertThrows(IllegalArgumentException.class,()->f.world.readCharacterState(other.context));
        assertThrows(NullPointerException.class,()->f.world.readCharacterState(null));
        assertEquals(state,f.read());}

    @Test void nullMapInvalidParticipantAndWrongEngineFailSafely(){var f=new Fixture();f.openReadySession();var state=f.read();
        assertThrows(NullPointerException.class,()->f.engine.readTiming(1,f.world,null,f.context));
        assertEquals(RpgSkillEngine.TimingStatus.INCOMPATIBLE,f.engine.readTiming(0,f.world,f.participants,f.context).status());
        assertThrows(IllegalArgumentException.class,()->new RpgSkillEngine().readTiming(1,f.world,f.participants,f.context));
        assertEquals(state,f.read());}

    @Test void clocksAreIndependentExactAndNeverAdvancedByRead(){var f=new Fixture();f.openReadySession();
        f.engine.clock(9876543210L);f.world.tick(0,0);var timing=f.timing();
        assertEquals(9876543210L,timing.engineClock());assertEquals(1,timing.worldTick());assertEquals(timing,f.timing());}

    @Test void authoredAbsenceIsNotEmptyLedger(){var world=new RpgWorld(CharacterIdentity.create("Sem Autoria","reaper","azure"));
        var state=world.readCharacterState(new CharacterReadContext(world));assertNull(state.authoredQuests());assertNull(state.authoredBook());}

    @Test void objectiveIndicesKeepUnknownCollectCounterRatherThanZero(){var f=new Fixture();
        assertTrue(f.world.interactSyntheticNpc());assertTrue(f.world.acceptAuthoredQuest("read_trial"));
        var ledger=f.read().authoredQuests();var objectives=ledger.objectives().get("read_trial");
        assertEquals(0,objectives.get(0).index());assertEquals(1,objectives.get(0).storedCounter());
        assertEquals(1,objectives.get(1).index());assertEquals("collect",objectives.get(1).definition().type());
        assertNull(objectives.get(1).storedCounter());assertEquals(3,ledger.progress().get("read_trial"));
        assertThrows(UnsupportedOperationException.class,()->objectives.clear());}

    @Test void malformedLedgerReadFailsWithoutChangingInput(){var f=new Fixture();
        var service=new QuestService(RpgContent.builtin());var progress=new TreeMap<String,Integer>();
        progress.put("first_steps",0);var flags=new TreeSet<>(Set.of("qo:first_steps:0:invalid"));
        var before=f.read();var copy=Set.copyOf(flags);
        assertThrows(NumberFormatException.class,()->service.readLedger(progress,flags,f.context,f.world));
        assertEquals(copy,flags);assertEquals(Map.of("first_steps",0),progress);assertEquals(before,f.read());}

    @Test void deadResourcesAreNotRevived(){var f=new Fixture();f.world.receiveDamage(f.world.networkState().hp());
        var state=f.read();assertEquals(0,state.hp());assertEquals(state,f.read());}
}
