package com.projetok.rpg.recovery;

import com.projetok.rpg.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static com.projetok.rpg.recovery.RecoverableCharacterSnapshot.*;
import static com.projetok.rpg.recovery.CharacterSnapshotValidator.*;

/** Contrato sintético determinístico, sem conexão, instalação, credencial ou save pessoal. */
class CharacterSnapshotValidatorTest {
    private static final CharacterId ID=CharacterId.parse("01234567-89ab-cdef-0123-456789abcdef");
    private static final ContentHash HASH=new ContentHash("a".repeat(64));
    private static final RulesVersion RULES=new RulesVersion("rpg-current");
    private static final WorldEpoch EPOCH=new WorldEpoch(7);
    private static final Instant CAPTURED=Instant.parse("2026-11-10T03:00:00Z");
    // Carregamento explícito da fixture: nunca ocorre dentro de validate.
    private static final RpgProgression CURVE=RpgProgression.builtin();
    private static final ItemCatalog ITEMS=new ItemCatalog(RpgContent.builtin());
    private static final QuestKey QUEST=new QuestKey(QuestNamespace.BUILTIN,"first_steps","v1");
    private static final QuestService.Definition DEFINITION=new QuestService(RpgContent.builtin()).definition("first_steps");
    private final CharacterSnapshotValidator validator=new CharacterSnapshotValidator();

    /** Monta contexto de catálogos carregados sem alegar autenticação ou ocupação do mundo. */
    private SnapshotValidationContext context() {
        return new SnapshotValidationContext(CURVE,ITEMS,Map.of(QUEST,DEFINITION),
                Map.of("village",new SnapshotValidationContext.RegionBounds(18,18,942,622)),
                Set.of("none"),HASH,RULES,EPOCH,9L,CAPTURED.plusSeconds(1),Set.of());
    }

    /** Verifica códigos, preservando diagnóstico estruturado em vez de mensagens incidentais. */
    private void invalid(Draft draft,Code code) {
        var result=validator.validate(draft.build(),context());
        assertFalse(result.structurallyValid(),result.toString());
        assertTrue(result.violations().stream().anyMatch(v->v.code()==code),result.toString());
        assertFalse(result.readyForActivation());
    }

    @Test void completeSnapshotIsStructurallyValidButNeverAuthorizesActivation() {
        var result=validator.validate(new Draft().build(),context());
        assertTrue(result.structurallyValid(),result.toString());
        assertEquals(Set.of(PendingCheck.AUTHORIZATION,PendingCheck.WORLD_PLACEMENT,
                PendingCheck.DURABLE_COMMIT,PendingCheck.CLOCK_POLICY),result.pendingChecks());
        assertFalse(result.readyForActivation());
    }

    @Test void collectionsAndResultsAreImmutable() {
        var s=new Draft().build();var result=validator.validate(s,context());
        assertThrows(UnsupportedOperationException.class,()->s.inventory().slots().clear());
        assertThrows(UnsupportedOperationException.class,()->s.quests().get(0).objectives().clear());
        assertThrows(UnsupportedOperationException.class,()->s.receipts().clear());
        assertThrows(UnsupportedOperationException.class,()->s.cooldowns().deadlines().clear());
        assertThrows(UnsupportedOperationException.class,()->result.violations().clear());
        assertThrows(UnsupportedOperationException.class,()->result.pendingChecks().clear());
    }

    @Test void externalListsAndContextMapsCannotChangeConstructedData() {
        var d=new Draft();var slots=new ArrayList<>(d.inventory.slots());d.inventory=new Inventory(20,slots);
        var objectives=new ArrayList<>(d.quests.get(0).objectives());
        d.quests=new ArrayList<>(List.of(new QuestProgress(QUEST,QuestStatus.DELIVERED,objectives,"quest1")));
        d.receipts=new ArrayList<>(d.receipts);d.deadlines=new ArrayList<>(d.deadlines);
        var before=d.build();slots.clear();objectives.clear();d.quests.clear();d.receipts.clear();d.deadlines.clear();
        assertEquals(2,before.inventory().slots().size());assertFalse(before.quests().get(0).objectives().isEmpty());
        assertEquals(2,before.receipts().size());assertEquals(9,before.cooldowns().deadlines().size());
        var definitions=new HashMap<>(Map.of(QUEST,DEFINITION));var c=context();
        var copied=new SnapshotValidationContext(CURVE,ITEMS,definitions,c.regions(),c.cosmeticIds(),HASH,RULES,EPOCH,9L,c.observedAt(),Set.of());
        definitions.clear();assertEquals(1,copied.quests().size());
        assertTrue(validator.validate(before,copied).structurallyValid());
    }

    @Test void absentBlocksAndCollectionsAreRejectedWithoutDefaults() {
        var d=new Draft();d.resources=null;invalid(d,Code.REQUIRED);
        d=new Draft();d.inventory=new Inventory(20,null);invalid(d,Code.REQUIRED);
        d=new Draft();d.restrictions=null;invalid(d,Code.REQUIRED);
        assertFalse(validator.validate(null,context()).structurallyValid());
    }

    @Test void incompatibleSchemaIsRejected() {var d=new Draft();d.schema=new SchemaVersion(99);invalid(d,Code.SCHEMA);}

    @Test void malformedOrMissingUuidIsRejected() {
        for(String id:Arrays.asList(null,"not-a-uuid","1-1-1-1-1"))
            assertThrows(IllegalArgumentException.class,()->CharacterId.parse(id));
        var d=new Draft();d.id=null;invalid(d,Code.IDENTITY);
        d=new Draft();d.id=new CharacterId(null);invalid(d,Code.IDENTITY);
    }

    @ParameterizedTest @ValueSource(longs={-1,9,Long.MAX_VALUE})
    void negativeStaleAndOverflowingRevisionsAreRejected(long value) {
        var d=new Draft();d.revision=new StateRevision(value);invalid(d,Code.REVISION);
    }

    @ParameterizedTest @ValueSource(longs={-1,1_000_001,Long.MAX_VALUE})
    void xpOutsideCurrentTechnicalBoundsIsRejected(long xp) {
        var d=new Draft();d.progression=new Progression(xp,"thunder",3,null,false);invalid(d,Code.PROGRESSION);
    }

    @Test void incompatibleDomainOrHalfUnitsAreRejectedWithoutChoosingOrAwarding() {
        var d=new Draft();d.progression=new Progression(CURVE.threshold(20),"light",3,null,false);invalid(d,Code.PROGRESSION);
        d=new Draft();d.progression=new Progression(CURVE.threshold(20),"none",3,null,false);invalid(d,Code.PROGRESSION);
        d=new Draft();d.progression=new Progression(CURVE.threshold(20),"thunder",-1,null,false);invalid(d,Code.PROGRESSION);
    }

    @Test void negativeAttributesAreRejected() {
        var d=new Draft();d.attributes=new Attributes(AttributeMode.MANUAL_SESSION,-1,3,2,1,66,20);invalid(d,Code.ATTRIBUTES);
    }

    @Test void budgetAndGrantCheckpointCannotConcedeExtraPoints() {
        var d=new Draft();d.attributes=new Attributes(AttributeMode.MANUAL_SESSION,4,3,2,1,62,20);invalid(d,Code.ATTRIBUTES);
        d=new Draft();d.attributes=new Attributes(AttributeMode.MANUAL_SESSION,4,3,2,1,61,19);invalid(d,Code.ATTRIBUTES);
        d=new Draft();d.attributes=new Attributes(AttributeMode.MANUAL_SESSION,Integer.MAX_VALUE,Integer.MAX_VALUE,2,1,Integer.MAX_VALUE,20);
        invalid(d,Code.ATTRIBUTES);
    }

    @Test void duplicateSlotIndicesAreRejectedEvenWithDifferentItems() {
        var d=new Draft();d.inventory=new Inventory(20,List.of(new Slot(0,"potion",1),new Slot(0,"sword",1)));
        invalid(d,Code.DUPLICATE_SLOT);
    }

    @ParameterizedTest @ValueSource(ints={0,-1,100,Integer.MAX_VALUE})
    void invalidStackQuantitiesAreRejected(int quantity) {
        var d=new Draft();d.inventory=new Inventory(20,List.of(new Slot(0,"potion",quantity)));invalid(d,Code.INVENTORY);
    }

    @Test void sameItemInTwoDifferentSlotsIsLegitimate() {
        var d=new Draft();d.inventory=new Inventory(20,List.of(new Slot(0,"potion",1),new Slot(4,"potion",1)));
        assertTrue(validator.validate(d.build(),context()).structurallyValid());
    }

    @Test void incompatibleEquipmentAndUnknownItemsAreRejected() {
        var d=new Draft();d.equipment=List.of(new Equipment("torso","sword"));invalid(d,Code.EQUIPMENT);
        d=new Draft();d.inventory=new Inventory(20,List.of(new Slot(0,"unknown_item",1)));invalid(d,Code.INVENTORY);
    }

    @ParameterizedTest @ValueSource(longs={-1,1_000_001,Long.MAX_VALUE})
    void negativeOrOverflowingCoinsAreRejected(long coins) {var d=new Draft();d.wallet=new Wallet(coins);invalid(d,Code.WALLET);}

    @Test void duplicateQuestsCannotHideBehindDifferentVersions() {
        var d=new Draft();var q=d.quests.get(0);
        d.quests=List.of(q,new QuestProgress(new QuestKey(QuestNamespace.BUILTIN,"first_steps","v2"),q.status(),q.objectives(),q.deliveryReceiptId()));
        invalid(d,Code.DUPLICATE_QUEST);
    }

    @Test void invalidDuplicateAndExcessiveObjectivesAreRejected() {
        var d=new Draft();d.quests=List.of(new QuestProgress(QUEST,QuestStatus.READY,
                List.of(new ObjectiveProgress(0,"kill","slime",100)),null));invalid(d,Code.OBJECTIVE);
        d=new Draft();var o=d.quests.get(0).objectives().get(0);
        d.quests=List.of(new QuestProgress(QUEST,QuestStatus.READY,List.of(o,o),null));invalid(d,Code.OBJECTIVE);
    }

    @Test void duplicateOrConflictingReceiptsAreRejected() {
        var d=new Draft();var receipt=d.receipts.get(0);d.receipts=List.of(receipt,receipt);invalid(d,Code.DUPLICATE_RECEIPT);
        d=new Draft();d.receipts=List.of(receipt,new RewardReceipt("otherId",receipt.kind(),ID,receipt.source(),EPOCH,new StateRevision(8),"b".repeat(64)));
        invalid(d,Code.DUPLICATE_RECEIPT);
    }

    @Test void duplicateLootAndDanglingEconomicReferencesAreRejected() {
        var d=new Draft();var pending=d.pending.get(0);d.pending=List.of(pending,pending);invalid(d,Code.ENTITLEMENT);
        d=new Draft();d.pending=List.of(new PendingEntitlement("p1","missing","potion",1));invalid(d,Code.ENTITLEMENT);
        d=new Draft();d.receipts=List.of(d.receipts.get(1));invalid(d,Code.RECEIPT);
    }

    @Test void missingCooldownCannotBecomeZero() {
        var d=new Draft();d.deadlines=d.deadlines.subList(1,d.deadlines.size());invalid(d,Code.COOLDOWN);
        d=new Draft();d.deadlines=null;invalid(d,Code.REQUIRED);
    }

    @Test void invalidDeadlinesAreRejectedAndDecimalPrecisionIsPreserved() {
        var d=new Draft();d.deadlines=new ArrayList<>(d.deadlines);
        d.deadlines.set(0,new CooldownDeadline(CooldownChannel.BASIC_ATTACK,null,new BigDecimal("-0.1")));invalid(d,Code.DEADLINE);
        var valid=new Draft().build();var ready=valid.cooldowns().deadlines().get(0).readyAtTick();
        assertEquals(new BigDecimal("900.123456789"),ready);
        assertTrue(validator.validate(valid,context()).structurallyValid());
        assertThrows(IllegalArgumentException.class,()->new CooldownDeadline(CooldownChannel.BASIC_ATTACK,null,new BigDecimal("0.0000000001")));
    }

    @Test void resourcesDoNotPermitDeadWithHpOrExcessEnergy() {
        var d=new Draft();d.resources=new Resources(100,500,0,LifeState.DEAD);invalid(d,Code.RESOURCES);
        d=new Draft();d.resources=new Resources(200,1021,0,LifeState.ALIVE);invalid(d,Code.RESOURCES);
        d=new Draft();d.resources=new Resources(10000,500,0,LifeState.ALIVE);invalid(d,Code.RESOURCES);
        d=new Draft();d.resources=new Resources(200,500,Double.NaN,LifeState.ALIVE);invalid(d,Code.RESOURCES);
    }

    @Test void invalidLocationsAreRejectedWithoutMovingAnyBody() {
        var d=new Draft();d.location=new Location("missing_region",140,320,null);invalid(d,Code.LOCATION);
        d=new Draft();d.location=new Location("village",Float.NaN,320,null);invalid(d,Code.LOCATION);
        d=new Draft();d.location=new Location("village",0,0,null);invalid(d,Code.LOCATION);
    }

    @Test void absentPenaltyEvidenceNeverMeansNoPenalty() {
        var d=new Draft();var c=context();
        var required=new SnapshotValidationContext(CURVE,ITEMS,c.quests(),c.regions(),c.cosmeticIds(),HASH,RULES,EPOCH,9L,c.observedAt(),Set.of(RestrictionKind.ROOT));
        assertTrue(validator.validate(d.build(),required).violations().stream().anyMatch(v->v.code()==Code.RESTRICTION));
        d.restrictions=new Restrictions(RestrictionAssessment.UNKNOWN,List.of());
        var result=validator.validate(d.build(),context());
        assertTrue(result.pendingChecks().contains(PendingCheck.RESTRICTION_EVIDENCE));assertFalse(result.readyForActivation());
    }

    @Test void intangibilityAndExecutableStateCannotBeRepresented() {
        assertThrows(IllegalArgumentException.class,()->RestrictionKind.valueOf("INTANGIBLE"));
        var names=Arrays.stream(RecoverableCharacterSnapshot.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName).toList();
        for(String forbidden:List.of("intangible","projectiles","socket","peer","commands","dash","sentry","events"))assertFalse(names.contains(forbidden));
    }

    @Test void snapshotFromOtherContentRulesOrEpochIsRejected() {
        var d=new Draft();d.hash=new ContentHash("b".repeat(64));invalid(d,Code.CONTEXT);
        d=new Draft();d.rules=new RulesVersion("other_rules");invalid(d,Code.CONTEXT);
        d=new Draft();d.capture=new Capture(new WorldEpoch(8),new SourceTick(100),CAPTURED,ClockOrigin.MONOTONIC_WORLD_TICKS,1);invalid(d,Code.CAPTURE);
    }

    @Test void repeatedValidationIsDeterministicAndDoesNotAwardOrAlterAnything() {
        var s=new Draft().build();var copy=new Draft().build();var c=context();
        var first=validator.validate(s,c);
        for(int n=0;n<30;n++)assertEquals(first,validator.validate(s,c));
        assertEquals(copy,s);assertEquals(45,s.wallet().coins());assertEquals(3,s.progression().domainXpHalfUnits());
        assertEquals(QuestStatus.DELIVERED,s.quests().get(0).status());assertEquals(200,s.resources().hp());
    }

    @Test void rejectionDoesNotPartiallyInstallOrMutateAnExistingInventory() {
        var live=new InventoryService(ITEMS,20);assertTrue(live.add("potion",3));var original=live.snapshot();
        var d=new Draft();d.inventory=new Inventory(20,List.of(new Slot(0,"sword",1),new Slot(20,"potion",1)));
        var candidate=d.build();var before=candidate.inventory().slots();
        assertFalse(validator.validate(candidate,context()).structurallyValid());
        assertEquals(original,live.snapshot());assertSame(before,candidate.inventory().slots());assertEquals(3,live.count("potion"));
    }

    @Test void unknownCatalogsClockAndAttributePolicyAreExplicitPendingChecks() {
        var d=new Draft();d.capture=new Capture(EPOCH,new SourceTick(100),CAPTURED,ClockOrigin.UNSPECIFIED,1);
        d.attributes=new Attributes(AttributeMode.UNSPECIFIED,4,3,2,1,61,20);
        var result=validator.validate(d.build(),null);
        assertTrue(result.pendingChecks().containsAll(Set.of(PendingCheck.CATALOGS,PendingCheck.CLOCK_POLICY,
                PendingCheck.CAPTURE_CONTEXT,PendingCheck.ATTRIBUTE_POLICY,PendingCheck.PREVIOUS_REVISION)));
        assertFalse(result.readyForActivation());
    }

    @Test void negativeDescriptorsRemainPendingWithoutReexecutingDamage() {
        var d=new Draft();d.restrictions=new Restrictions(RestrictionAssessment.ASSESSED,
                List.of(new Restriction(RestrictionKind.BURN,"fire_1",new BigDecimal("200"),1)));
        var result=validator.validate(d.build(),context());assertTrue(result.structurallyValid(),result.toString());
        assertTrue(result.pendingChecks().contains(PendingCheck.RESTRICTION_POLICY));assertEquals(200,d.resources.hp());
    }

    @Test void boundsAreTechnicalRejectionsAndNeverTruncateLedgers() {
        var d=new Draft();d.receipts=Collections.nCopies(MAX_RECEIPTS+1,d.receipts.get(0));
        assertThrows(IllegalArgumentException.class,d::build);
        assertThrows(IllegalArgumentException.class,()->new RulesVersion("x".repeat(MAX_TEXT+1)));
        assertThrows(NullPointerException.class,()->new Inventory(20,Arrays.asList((Slot)null)));
    }

    @Test void malformedHashesCaptureDatesAndWatermarksAreRejected() {
        var d=new Draft();d.hash=new ContentHash("invalid");invalid(d,Code.CONTEXT);
        d=new Draft();d.capture=new Capture(EPOCH,new SourceTick(-1),CAPTURED,ClockOrigin.MONOTONIC_WORLD_TICKS,1);invalid(d,Code.CAPTURE);
        d=new Draft();d.capture=new Capture(EPOCH,new SourceTick(100),CAPTURED.plusSeconds(2),ClockOrigin.MONOTONIC_WORLD_TICKS,1);invalid(d,Code.CAPTURE);
        d=new Draft();d.progression=new Progression(CURVE.threshold(20),"thunder",3,LocalDate.MAX,false);invalid(d,Code.PROGRESSION);
        d=new Draft();d.watermarks=List.of(d.watermarks.get(0),d.watermarks.get(0));invalid(d,Code.WATERMARK);
    }

    @Test void nullNestedIdentifiersAreRejectedWithoutThrowing() {
        var d=new Draft();d.identity=new Identity("Personagem Sintetico","reaper","azure",null,"none");
        invalid(d,Code.IDENTITY);
        d=new Draft();d.location=new Location(null,140,320,null);invalid(d,Code.LOCATION);
    }

    @Test void validDeadStateRemainsDeadAndReceiptsMustBelongToCharacter() {
        var d=new Draft();d.resources=new Resources(0,500,0,LifeState.DEAD);
        assertTrue(validator.validate(d.build(),context()).structurallyValid());assertEquals(0,d.resources.hp());
        var r=d.receipts.get(0);d.receipts=List.of(new RewardReceipt(r.id(),r.kind(),CharacterId.parse("11234567-89ab-cdef-0123-456789abcdef"),r.source(),EPOCH,r.confirmedRevision(),r.resultHash()));
        invalid(d,Code.RECEIPT);
    }

    @Test void everyRequiredTopLevelBlockIsRejectedWhenAbsent() throws Exception {
        var s=new Draft().build();var components=RecoverableCharacterSnapshot.class.getRecordComponents();
        var types=Arrays.stream(components).map(java.lang.reflect.RecordComponent::getType).toArray(Class<?>[]::new);
        for(int missing=0;missing<components.length;missing++) {
            Object[] values=new Object[components.length];
            for(int n=0;n<components.length;n++)values[n]=n==missing?null:components[n].getAccessor().invoke(s);
            var candidate=RecoverableCharacterSnapshot.class.getConstructor(types).newInstance(values);
            assertFalse(validator.validate(candidate,context()).structurallyValid(),components[missing].getName());
        }
    }

    @Test void deadlineOverflowNumericSkillKeysAndDuplicateEquipmentAreRejected() {
        var d=new Draft();d.deadlines=new ArrayList<>(d.deadlines);
        d.deadlines.set(0,new CooldownDeadline(CooldownChannel.BASIC_ATTACK,null,BigDecimal.valueOf(Long.MAX_VALUE)));invalid(d,Code.DEADLINE);
        d=new Draft();d.deadlines=new ArrayList<>(d.deadlines);
        d.deadlines.set(4,new CooldownDeadline(CooldownChannel.DOMAIN_SKILL,"1",BigDecimal.TEN));invalid(d,Code.COOLDOWN);
        d=new Draft();d.equipment=List.of(new Equipment("weapon","sword"),new Equipment("weapon","sword"));invalid(d,Code.EQUIPMENT);
    }

    @Test void immutableContractClassesHaveNoLinksToRuntimeOrExternalIo() throws Exception {
        var queue=new ArrayDeque<Class<?>>();
        queue.add(RecoverableCharacterSnapshot.class);queue.add(CharacterSnapshotValidator.class);queue.add(SnapshotValidationContext.class);
        while(!queue.isEmpty()) {
            var type=queue.remove();queue.addAll(Arrays.asList(type.getDeclaredClasses()));
            try(var input=type.getResourceAsStream("/"+type.getName().replace('.','/')+".class")) {
                assertNotNull(input);
                var bytecode=new String(input.readAllBytes(),java.nio.charset.StandardCharsets.ISO_8859_1);
                for(String forbidden:List.of("java/awt/","javax/swing/","java/net/","java/sql/","java/io/","java/nio/file/",
                        "com/projetok/rpg/RpgWorld","com/projetok/multiplayer/","SyntheticOnlineRepository"))
                    assertFalse(bytecode.contains(forbidden),type.getName()+": "+forbidden);
            }
        }
    }

    /** Montagem mutável exclusiva da fixture; os objetos entregues ao validador são records copiados. */
    private static final class Draft {
        CharacterId id=ID;SchemaVersion schema=new SchemaVersion(1);StateRevision revision=new StateRevision(10);
        ContentHash hash=HASH;RulesVersion rules=RULES;
        Capture capture=new Capture(EPOCH,new SourceTick(100),CAPTURED,ClockOrigin.MONOTONIC_WORLD_TICKS,1);
        Identity identity=new Identity("Personagem Sintetico","reaper","azure","none","none");
        Progression progression=new Progression(CURVE.threshold(20),"thunder",3,null,false);
        Attributes attributes=new Attributes(AttributeMode.MANUAL_SESSION,4,3,2,1,61,20);
        Inventory inventory=new Inventory(20,List.of(new Slot(0,"potion",2),new Slot(1,"potion",1)));
        List<Equipment> equipment=List.of(new Equipment("weapon","sword"));Wallet wallet=new Wallet(45);
        List<QuestProgress> quests=List.of(new QuestProgress(QUEST,QuestStatus.DELIVERED,objectives(),"quest1"));
        List<RewardReceipt> receipts=List.of(
                new RewardReceipt("quest1",RewardKind.QUEST,ID,"builtin:first_steps:v1",EPOCH,new StateRevision(8),"b".repeat(64)),
                new RewardReceipt("loot1",RewardKind.LOOT,ID,"forest:slime:1",EPOCH,new StateRevision(9),"c".repeat(64)));
        List<EventWatermark> watermarks=List.of(new EventWatermark(QuestEventType.KILL,"coop_slime",EPOCH,1));
        List<PendingEntitlement> pending=List.of(new PendingEntitlement("p1","loot1","potion",2));
        Resources resources=new Resources(200,500,.125,LifeState.ALIVE);
        List<CooldownDeadline> deadlines=deadlines();
        Location location=new Location("village",140,320,null);
        Restrictions restrictions=new Restrictions(RestrictionAssessment.ASSESSED,List.of());

        /** Produz todos os objetivos da definição real sem emitir eventos de missão. */
        static List<ObjectiveProgress> objectives() {
            var list=new ArrayList<ObjectiveProgress>();
            for(int n=0;n<DEFINITION.objectives().size();n++) {var o=DEFINITION.objectives().get(n);list.add(new ObjectiveProgress(n,o.type(),o.target(),o.count()));}
            return list;
        }
        /** Canais reais e prazos declarados, sem simular a execução de skills. */
        static List<CooldownDeadline> deadlines() {
            var list=new ArrayList<CooldownDeadline>();
            for(var channel:List.of(CooldownChannel.BASIC_ATTACK,CooldownChannel.SHUNPO,CooldownChannel.POTION,CooldownChannel.TRAVEL))
                list.add(new CooldownDeadline(channel,null,new BigDecimal("900.123456789")));
            RpgSkillCatalog.forDomain("thunder").forEach(s->list.add(new CooldownDeadline(CooldownChannel.DOMAIN_SKILL,s.id(),new BigDecimal("200"))));
            return list;
        }
        /** Constrói um documento novo, sem adaptador ou instalação no runtime. */
        RecoverableCharacterSnapshot build() {
            return new RecoverableCharacterSnapshot(id,schema,revision,hash,rules,capture,identity,progression,attributes,
                    inventory,equipment,wallet,quests,receipts,watermarks,pending,resources,new Cooldowns(1,deadlines),location,restrictions);
        }
    }
}
