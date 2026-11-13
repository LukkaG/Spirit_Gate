package com.projetok.rpg.economy;

import com.projetok.rpg.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static com.projetok.rpg.economy.EconomicLaboratory.*;

/** Falhas determinísticas e CAS reais em memória; não abre arquivo, socket ou personagem de produção. */
@Timeout(20)
class EconomicLaboratoryTest {
    static final BeneficiaryId A=new BeneficiaryId(new UUID(1,1)),B=new BeneficiaryId(new UUID(1,2));
    static final WorldEpoch EPOCH=new WorldEpoch(7);
    static final QuestIdentity QUEST=new QuestIdentity("builtin","first_steps");
    static final QuestIdentity AUTHORED=new QuestIdentity("authored","lab_delivery");
    static final GrantId Q=grant(1),Q_ALIAS=grant(2),Q_VERSION=grant(3),M1=grant(10),M2=grant(11),M_ALIAS=grant(12),M_B=grant(20),AQ=grant(30);
    static final long XP=RpgProgression.builtin().threshold(20);

    /** UUIDs declarados pelo laboratório, sem relação com peer, relógio ou identidade persistente. */
    private static GrantId grant(long number){return new GrantId(new UUID(3,number));}
    private static EconomicOperationId op(long number){return new EconomicOperationId(new UUID(2,number));}

    /** RNG controlado só na resolução inicial de loot; o ledger não recebe esta referência. */
    static final class CountingRandom implements RandomGenerator {
        int calls;
        public long nextLong(){calls++;return 0;}
    }
    /** Fixture usa os catálogos e a validação de objetivos reais, com estado de abertura explícito. */
    static final class Fixture {
        final CountingRandom random=new CountingRandom();
        final Map<GrantId,AuthorizedGrant> offers;
        final MemoryLedger ledger;
        final Coordinator coordinator;
        Fixture(){this(null,256);}
        Fixture(EconomicState initial,int limit){
            var quest=new QuestService(RpgContent.builtin()).definition("first_steps");
            var definition=new QuestService.Definition(List.of(new QuestService.Objective("talk","elder",1)),3,5,"potion",1);
            var book=new QuestBook(1,List.of(new QuestBook.Draft("lab_delivery","Entrega Lab","Missão descartável",
                    "elder","village","",1,"",false,List.of(),definition,false)));
            book.requireValid();var authored=new AuthoredQuestSession(book);assertTrue(authored.accept("lab_delivery"));
            authored.event("talk","elder","lab_npc",1);assertEquals(QuestService.State.READY,authored.state("lab_delivery"));
            var grants=new HashMap<GrantId,AuthorizedGrant>();
            for(var id:List.of(Q,Q_ALIAS,Q_VERSION)){
                var source=new EconomicSource(SourceKind.QUEST,"builtin","first_steps","first_steps",id.equals(Q_VERSION)?"builtin-v2":"builtin-v1",null,id);
                grants.put(id,new AuthorizedGrant(A,EPOCH,source,RewardTerms.quest(quest),quest));
            }
            var terms=resolveSlime(random);
            for(var id:List.of(M1,M2,M_ALIAS,M_B)){
                var source=new EconomicSource(SourceKind.MOB,"forest","lab_slime","slime","builtin-v1",id.equals(M2)?2L:1L,id);
                grants.put(id,new AuthorizedGrant(id.equals(M_B)?B:A,EPOCH,source,terms,null));
            }
            var source=new EconomicSource(SourceKind.QUEST,"authored","lab_delivery","lab_delivery","book-v1",null,AQ);
            grants.put(AQ,new AuthorizedGrant(A,EPOCH,source,RewardTerms.quest(definition),definition));
            offers=Map.copyOf(grants);ledger=new MemoryLedger(Map.of(A,initial==null?opening(A):initial,B,opening(B)),offers,EPOCH,limit);
            coordinator=ledger.coordinator(A);
        }
        /** Origem, valor e contexto pertencem à intenção; não são recalculados em retry. */
        EconomicOperation reward(long id,GrantId grant,long revision){var offered=offers.get(grant);
            return operation(id,offered.beneficiary(),revision,1,EPOCH,new GrantReward(offered.source(),offered.reward()));}
        /** Somente o controlador de teste inspeciona a imagem absoluta atual. */
        EconomicState state(){return ledger.snapshot().entries().get(A).state();}
    }

    /** Resolve uma vez a tabela verdadeira, sobre bolsa descartável vazia; sem RpgWorld ou save. */
    private static RewardTerms resolveSlime(CountingRandom random){
        var items=new ItemCatalog(RpgContent.builtin());var inventory=new InventoryService(items,20);
        assertTrue(new LootService(RpgContent.builtin(),items).award("lab_slime",1,"slime",inventory,new HashSet<>(),random));
        var result=new TreeMap<String,Integer>();for(var stack:inventory.slots())if(stack!=null)result.merge(stack.id(),stack.quantity(),Math::addExact);
        assertFalse(result.isEmpty());return new RewardTerms(3,5,result,Map.of());
    }
    /** READY builtin vem do QuestService, sem interpretar texto de UI ou inventar recibo. */
    private static EconomicState opening(BeneficiaryId id){
        var service=new QuestService(RpgContent.builtin());var progress=new HashMap<String,Integer>();var flags=new HashSet<String>();
        assertTrue(service.accept("first_steps",progress,flags));service.event("kill","slime","lab_slime",1,progress,flags);
        service.event("kill","slime","lab_slime",2,progress,flags);assertEquals(QuestService.State.READY,service.state("first_steps",progress,flags));
        return new EconomicState(id,new EconomicRevision(0),XP,DomainProgression.initial().choose("reaper",20,"thunder"),0,2,
                Map.of("0:potion",3),Map.of(),Map.of(QUEST,new QuestEntry(QuestStatus.READY,null),AUTHORED,new QuestEntry(QuestStatus.READY,null)),Map.of());
    }
    /** Deriva apenas aberturas de cenário, antes de construir o ledger, sem setter de estado confirmado. */
    private static EconomicState seed(long revision,long xp,int coins,int capacity,Map<String,Integer> inventory,
            DomainProgression domain,Map<QuestIdentity,QuestEntry> quests){
        return new EconomicState(A,new EconomicRevision(revision),xp,domain,coins,capacity,inventory,Map.of(),quests,Map.of());
    }
    private static EconomicOperation operation(long id,BeneficiaryId beneficiary,long revision,long generation,WorldEpoch epoch,EconomicAction action){
        return new EconomicOperation(op(id),beneficiary,new EconomicRevision(revision),new AuthorityGeneration(generation),epoch,action);
    }
    /** Asserção também exige ausência de recibo e preservação da imagem completa em recusas definitivas. */
    private static void refusedUnchanged(Fixture fixture,EconomicOperation operation,Fault fault,Code expected){
        var before=fixture.ledger.snapshot();var result=fixture.coordinator.execute(operation,fault);
        assertEquals(expected,result.code(),result.detail());assertFalse(result.confirmed());assertNull(result.receipt());assertSame(before,fixture.ledger.snapshot());
    }

    @Test void requestedValidatedAndConfirmedAreSeparateStates(){var f=new Fixture();var request=f.reward(1,Q,0);var before=f.ledger.snapshot();
        var prepared=f.ledger.prepare(f.coordinator,request,Fault.NONE);assertEquals(Code.VALIDATED,prepared.result().code());
        assertNull(prepared.result().receipt());assertSame(before,f.ledger.snapshot());assertEquals(new EconomicRevision(1),prepared.validated().proposedState().revision());
        var confirmed=f.ledger.commit(prepared.validated(),Fault.NONE);assertEquals(Code.COMMITTED,confirmed.code());assertTrue(confirmed.confirmed());}

    @Test void questBalancesDeliveryReceiptAndRevisionCommitTogether(){var f=new Fixture();var request=f.reward(1,Q,0);
        var result=f.coordinator.execute(request,Fault.NONE);var state=f.state();var receipt=result.receipt();
        assertTrue(result.confirmed());assertEquals(XP+80,state.xp());assertEquals(45,state.coins());assertEquals(80,state.domain().xpHalfUnits());
        assertEquals(5,state.inventory().get("0:potion"));assertEquals(QuestStatus.COMPLETED,state.quests().get(QUEST).status());
        assertEquals(op(1),state.quests().get(QUEST).deliveryReceipt());assertEquals(0,receipt.before().value());assertEquals(1,receipt.after().value());
        assertEquals(state,receipt.confirmedState());assertEquals(receipt,f.ledger.snapshot().receipts().get(op(1)));assertTrue(f.ledger.historyValid(f.ledger.snapshot()));}

    @Test void sameIdSameContentReturnsOriginalWithoutRewardOrRngAgain(){var f=new Fixture();var operation=f.reward(1,M1,0);int calls=f.random.calls;
        var first=f.coordinator.execute(operation,Fault.NONE);var committed=f.ledger.snapshot();
        for(int i=0;i<30;i++){var retry=f.coordinator.execute(operation,Fault.NONE);assertEquals(Code.ALREADY_CONFIRMED,retry.code());assertEquals(first.receipt(),retry.receipt());}
        assertSame(committed,f.ledger.snapshot());assertEquals(calls,f.random.calls);assertEquals(1,committed.receipts().size());}

    @Test void sameIdDifferentOriginIsConflict(){var f=new Fixture();f.coordinator.execute(f.reward(1,M1,0),Fault.NONE);
        refusedUnchanged(f,f.reward(1,M2,1),Fault.NONE,Code.ID_CONFLICT);}

    @Test void sameIdDifferentAmountIsConflict(){var f=new Fixture();var original=f.reward(1,M1,0);f.coordinator.execute(original,Fault.NONE);
        var grant=(GrantReward)original.action();var changed=new RewardTerms(4,5,grant.requested().items(),Map.of());
        refusedUnchanged(f,operation(1,A,0,1,EPOCH,new GrantReward(grant.source(),changed)),Fault.NONE,Code.ID_CONFLICT);}

    @Test void newOperationIdCannotClaimSameConcession(){var f=new Fixture();f.coordinator.execute(f.reward(1,M1,0),Fault.NONE);
        refusedUnchanged(f,f.reward(2,M1,1),Fault.NONE,Code.DUPLICATE_GRANT);}

    @Test void changingGrantIdDoesNotRepeatSameSpawnGeneration(){var f=new Fixture();f.coordinator.execute(f.reward(1,M1,0),Fault.NONE);
        refusedUnchanged(f,f.reward(2,M_ALIAS,1),Fault.NONE,Code.DUPLICATE_GRANT);}

    @Test void respawnInLaterGenerationIsAnotherAuthorizedEvent(){var f=new Fixture();assertTrue(f.coordinator.execute(f.reward(1,M1,0),Fault.NONE).confirmed());
        assertTrue(f.coordinator.execute(f.reward(2,M2,1),Fault.NONE).confirmed());assertEquals(XP+6,f.state().xp());assertEquals(10,f.state().coins());
        assertEquals(2,f.ledger.snapshot().receipts().size());assertEquals(2L,f.ledger.snapshot().watermarks().values().iterator().next());}

    @Test void unconfirmedOlderGenerationIsRejectedByWatermark(){var f=new Fixture();f.coordinator.execute(f.reward(1,M2,0),Fault.NONE);
        refusedUnchanged(f,f.reward(2,M1,1),Fault.NONE,Code.OBSOLETE_SOURCE);}

    @Test void questCannotBeDeliveredAgainByAliasGrantOrDefinitionVersion(){var f=new Fixture();f.coordinator.execute(f.reward(1,Q,0),Fault.NONE);
        refusedUnchanged(f,f.reward(2,Q_ALIAS,1),Fault.NONE,Code.ALREADY_DELIVERED);
        refusedUnchanged(f,f.reward(3,Q_VERSION,1),Fault.NONE,Code.ALREADY_DELIVERED);}

    @Test void questReadyWithoutConfirmationStillHasNoReceipt(){var f=new Fixture();assertEquals(QuestStatus.READY,f.state().quests().get(QUEST).status());
        assertTrue(f.ledger.snapshot().receipts().isEmpty());assertNull(f.state().quests().get(QUEST).deliveryReceipt());
        refusedUnchanged(f,f.reward(1,Q,0),Fault.PREPARATION,Code.PREPARATION_FAILED);assertEquals(QuestStatus.READY,f.state().quests().get(QUEST).status());}

    @Test void activeQuestIsNotDeliverable(){var state=opening(A);var quests=new HashMap<>(state.quests());quests.put(QUEST,new QuestEntry(QuestStatus.ACTIVE,null));
        var f=new Fixture(seed(0,XP,0,2,state.inventory(),state.domain(),quests),256);refusedUnchanged(f,f.reward(1,Q,0),Fault.NONE,Code.QUEST_NOT_READY);}

    @Test void authoredQuestUsesSameAtomicBoundaryWithoutReplayingService(){var f=new Fixture();var result=f.coordinator.execute(f.reward(1,AQ,0),Fault.NONE);
        assertTrue(result.confirmed());assertEquals(XP+3,f.state().xp());assertEquals(5,f.state().coins());assertEquals(op(1),f.state().quests().get(AUTHORED).deliveryReceipt());
        refusedUnchanged(f,f.reward(2,AQ,1),Fault.NONE,Code.ALREADY_DELIVERED);}

    @ParameterizedTest @EnumSource(value=Fault.class,names={"PREPARATION","BEFORE_COMMIT","DROP_RECEIPT","CORRUPT_RECEIPT"})
    void controlledFailuresNeverExposePartialReward(Fault fault){var f=new Fixture();var code=switch(fault){
        case PREPARATION->Code.PREPARATION_FAILED;case BEFORE_COMMIT->Code.BEFORE_COMMIT_FAILED;default->Code.INVALID_TRANSITION;};
        refusedUnchanged(f,f.reward(1,Q,0),fault,code);}

    @Test void lostResponseRequiresLookupAndDoesNotMakeCommitDisappear(){var f=new Fixture();var operation=f.reward(1,Q,0);
        var uncertain=f.coordinator.execute(operation,Fault.AFTER_COMMIT);assertEquals(Code.UNCERTAIN,uncertain.code());assertNull(uncertain.receipt());assertFalse(uncertain.confirmed());
        assertEquals(1,f.state().revision().value());var before=f.ledger.snapshot();var query=f.coordinator.reconcile(op(1),Fault.NONE);
        assertEquals(Code.CONFIRMED_BY_LOOKUP,query.code());assertEquals(f.state(),query.receipt().confirmedState());assertSame(before,f.ledger.snapshot());
        assertEquals(query.receipt(),f.coordinator.execute(operation,Fault.NONE).receipt());assertSame(before,f.ledger.snapshot());}

    @Test void failedReconciliationRemainsUncertainAndNeverRetriesBlindly(){var f=new Fixture();var operation=f.reward(1,M1,0);f.coordinator.execute(operation,Fault.AFTER_COMMIT);
        var before=f.ledger.snapshot();assertEquals(Code.UNCERTAIN,f.coordinator.reconcile(op(1),Fault.RECONCILIATION).code());
        assertEquals(Code.UNCERTAIN,f.coordinator.execute(operation,Fault.RECONCILIATION).code());assertSame(before,f.ledger.snapshot());
        assertTrue(f.coordinator.reconcile(op(1),Fault.NONE).confirmed());}

    @Test void knownFailureBeforeCommitCanBeRetriedWithoutSecondGrant(){var f=new Fixture();var operation=f.reward(1,Q,0);
        refusedUnchanged(f,operation,Fault.BEFORE_COMMIT,Code.BEFORE_COMMIT_FAILED);assertEquals(Code.NOT_FOUND,f.coordinator.reconcile(op(1),Fault.NONE).code());
        assertTrue(f.coordinator.execute(operation,Fault.NONE).confirmed());assertEquals(1,f.ledger.snapshot().receipts().size());}

    /** Prepara sobre a mesma revisão antes de liberar duas threads reais para o CAS. */
    private static List<EconomicOperationResult> race(Fixture f,EconomicOperation first,EconomicOperation second)throws Exception {
        var a=f.ledger.prepare(f.coordinator,first,Fault.NONE);var b=f.ledger.prepare(f.coordinator,second,Fault.NONE);
        return racePrepared(f.ledger,a,b);
    }
    /** Permite disputar o CAS global também com dois beneficiários, sem last-write-wins entre eles. */
    private static List<EconomicOperationResult> racePrepared(MemoryLedger ledger,Preparation a,Preparation b)throws Exception {
        assertNotNull(a.validated());assertNotNull(b.validated());var start=new CountDownLatch(1);var ready=new CountDownLatch(2);
        var pool=Executors.newFixedThreadPool(2);
        try{var futures=new ArrayList<Future<EconomicOperationResult>>();for(var prepared:List.of(a,b))futures.add(pool.submit(()->{
            ready.countDown();assertTrue(start.await(3,TimeUnit.SECONDS));return ledger.commit(prepared.validated(),Fault.NONE);}));
            assertTrue(ready.await(3,TimeUnit.SECONDS));start.countDown();return List.of(futures.get(0).get(5,TimeUnit.SECONDS),futures.get(1).get(5,TimeUnit.SECONDS));
        }finally{start.countDown();pool.shutdownNow();assertTrue(pool.awaitTermination(3,TimeUnit.SECONDS));}
    }

    @Test void twoConcurrentOperationsHaveExactlyOneCasWinner()throws Exception {for(int round=0;round<12;round++){
        var f=new Fixture();var results=race(f,f.reward(1,Q,0),f.reward(2,M1,0));
        assertEquals(1,results.stream().filter(r->r.code()==Code.COMMITTED).count());assertEquals(1,results.stream().filter(r->r.code()==Code.STALE_REVISION).count());
        assertEquals(1,f.ledger.snapshot().receipts().size());assertEquals(1,f.state().revision().value());assertTrue(f.ledger.historyValid(f.ledger.snapshot()));}}

    @Test void simultaneousIdenticalRequestsShareOneConfirmedReceipt()throws Exception {var f=new Fixture();var operation=f.reward(1,M1,0);var results=race(f,operation,operation);
        assertEquals(1,results.stream().filter(r->r.code()==Code.COMMITTED).count());assertEquals(1,results.stream().filter(r->r.code()==Code.ALREADY_CONFIRMED).count());
        assertEquals(results.get(0).receipt(),results.get(1).receipt());assertEquals(1,f.ledger.snapshot().receipts().size());}

    @Test void simultaneousDifferentIdsForOneGrantCannotDuplicateEconomy()throws Exception {var f=new Fixture();var results=race(f,f.reward(1,M1,0),f.reward(2,M_ALIAS,0));
        assertEquals(1,results.stream().filter(r->r.code()==Code.COMMITTED).count());assertEquals(1,results.stream().filter(r->r.code()==Code.DUPLICATE_GRANT).count());assertEquals(5,f.state().coins());}

    @Test void latePreparedCallbackCannotOverwriteNewRevision(){var f=new Fixture();var old=f.ledger.prepare(f.coordinator,f.reward(1,Q,0),Fault.NONE);
        f.coordinator.execute(f.reward(2,M1,0),Fault.NONE);var confirmed=f.ledger.snapshot();assertEquals(Code.STALE_REVISION,f.ledger.commit(old.validated(),Fault.NONE).code());assertSame(confirmed,f.ledger.snapshot());}

    @Test void replayingOldReceiptDoesNotRollBackLaterBalances(){var f=new Fixture();var operation=f.reward(1,M1,0);var first=f.coordinator.execute(operation,Fault.NONE);
        f.coordinator.execute(f.reward(2,M2,1),Fault.NONE);var current=f.ledger.snapshot();var replay=f.coordinator.execute(operation,Fault.NONE);
        assertEquals(first.receipt(),replay.receipt());assertNotEquals(replay.receipt().confirmedState(),f.state());assertSame(current,f.ledger.snapshot());assertEquals(10,f.state().coins());}

    @Test void admissionGenerationRejectsOldCallbackEvenIfIdAndRevisionMatch(){var f=new Fixture();var prepared=f.ledger.prepare(f.coordinator,f.reward(1,Q,0),Fault.NONE);
        f.ledger.nextAdmission(A,EPOCH);var before=f.ledger.snapshot();assertEquals(Code.STALE_AUTHORITY,f.ledger.commit(prepared.validated(),Fault.NONE).code());
        assertEquals(Code.STALE_AUTHORITY,f.coordinator.reconcile(op(1),Fault.NONE).code());assertSame(before,f.ledger.snapshot());}

    @Test void newAdmissionMayReadHistoricalReceiptWithoutReapplyingOldRequest(){var f=new Fixture();var operation=f.reward(1,Q,0);f.coordinator.execute(operation,Fault.NONE);
        f.ledger.nextAdmission(A,new WorldEpoch(8));var next=f.ledger.coordinator(A);var before=f.ledger.snapshot();
        assertEquals(Code.STALE_AUTHORITY,next.execute(operation,Fault.NONE).code());assertTrue(next.reconcile(op(1),Fault.NONE).confirmed());assertSame(before,f.ledger.snapshot());}

    @Test void wrongBeneficiaryIsRefusedBeforeAnyEconomicChange(){var f=new Fixture();refusedUnchanged(f,f.reward(1,M_B,0),Fault.NONE,Code.WRONG_BENEFICIARY);}

    @Test void sameOperationIdCannotCrossBeneficiaries(){var f=new Fixture();f.coordinator.execute(f.reward(1,M1,0),Fault.NONE);var before=f.ledger.snapshot();
        assertEquals(Code.ID_CONFLICT,f.ledger.coordinator(B).execute(f.reward(1,M_B,0),Fault.NONE).code());assertSame(before,f.ledger.snapshot());
        assertEquals(Code.WRONG_BENEFICIARY,f.ledger.coordinator(B).reconcile(op(1),Fault.NONE).code());}

    @Test void sameSpawnGenerationCanRewardDifferentAuthorizedBeneficiaries(){var f=new Fixture();assertTrue(f.coordinator.execute(f.reward(1,M1,0),Fault.NONE).confirmed());
        assertTrue(f.ledger.coordinator(B).execute(f.reward(2,M_B,0),Fault.NONE).confirmed());assertEquals(5,f.state().coins());assertEquals(5,f.ledger.snapshot().entries().get(B).state().coins());}

    @Test void wrongEpochAndRevisionAreRefused(){var f=new Fixture();var offered=f.offers.get(M1);
        refusedUnchanged(f,operation(1,A,0,1,new WorldEpoch(8),new GrantReward(offered.source(),offered.reward())),Fault.NONE,Code.WRONG_EPOCH);
        refusedUnchanged(f,f.reward(1,M1,9),Fault.NONE,Code.STALE_REVISION);}

    @Test void oldOfferCannotBecomeNewEpochLootByChangingRequestContext(){var f=new Fixture();f.ledger.nextAdmission(A,new WorldEpoch(8));var next=f.ledger.coordinator(A);var offered=f.offers.get(M1);
        var before=f.ledger.snapshot();var operation=operation(1,A,0,2,new WorldEpoch(8),new GrantReward(offered.source(),offered.reward()));
        assertEquals(Code.WRONG_EPOCH,next.execute(operation,Fault.NONE).code());assertSame(before,f.ledger.snapshot());}

    @Test void uuidDoesNotAdmitUnknownBeneficiaryOrForeignValidatedPlan(){var f=new Fixture();assertThrows(IllegalArgumentException.class,()->f.ledger.coordinator(new BeneficiaryId(new UUID(99,99))));
        var other=new Fixture();var prepared=f.ledger.prepare(f.coordinator,f.reward(1,Q,0),Fault.NONE);var before=other.ledger.snapshot();
        assertEquals(Code.FOREIGN_VALIDATION,other.ledger.commit(prepared.validated(),Fault.NONE).code());assertSame(before,other.ledger.snapshot());}

    @Test void revisionOverflowRejectsWholeGrant(){var state=opening(A);var f=new Fixture(seed(Long.MAX_VALUE,XP,0,2,state.inventory(),state.domain(),state.quests()),256);
        refusedUnchanged(f,f.reward(1,Q,Long.MAX_VALUE),Fault.NONE,Code.LIMIT_EXCEEDED);}

    @ParameterizedTest @ValueSource(strings={"coins","xp","domain"})
    void resourceOverflowDoesNotPartiallyCompleteQuest(String component){var state=opening(A);
        var domain=component.equals("domain")?new DomainProgression("thunder",Long.MAX_VALUE,null,false):state.domain();
        var f=new Fixture(seed(0,component.equals("xp")?1_000_000:XP,component.equals("coins")?1_000_000:0,2,state.inventory(),domain,state.quests()),256);
        refusedUnchanged(f,f.reward(1,Q,0),Fault.NONE,Code.LIMIT_EXCEEDED);}

    @ParameterizedTest @ValueSource(strings={"xp","coins"})
    void numericOverflowInAuthorizedTermsFailsClosed(String component){var initial=opening(A);
        var state=seed(0,XP,component.equals("coins")?1_000_000:0,2,initial.inventory(),initial.domain(),initial.quests());
        var source=new EconomicSource(SourceKind.MOB,"forest","overflow_slime","slime","builtin-v1",1L,M1);
        var terms=new RewardTerms(component.equals("xp")?Long.MAX_VALUE:0,component.equals("coins")?Integer.MAX_VALUE:0,Map.of(),Map.of());var grant=new AuthorizedGrant(A,EPOCH,source,terms,null);
        var ledger=new MemoryLedger(Map.of(A,state),Map.of(M1,grant),EPOCH,20);var before=ledger.snapshot();
        var operation=operation(1,A,0,1,EPOCH,new GrantReward(source,terms));assertEquals(Code.LIMIT_EXCEEDED,ledger.coordinator(A).execute(operation,Fault.NONE).code());assertSame(before,ledger.snapshot());}

    /** Bolsa cheia não elimina loot: o direito entra junto com a recompensa e seu recibo. */
    private static Fixture full(){var state=opening(A);return new Fixture(seed(0,XP,0,1,Map.of("0:sword",1),state.domain(),state.quests()),256);}

    @Test void fullQuestInventoryRejectsRewardDeliveryAndReceiptTogether(){var f=full();refusedUnchanged(f,f.reward(1,Q,0),Fault.NONE,Code.INVENTORY_FULL);}

    @Test void fullMobInventoryCreatesRightWithConfirmedOrigin(){var f=full();var result=f.coordinator.execute(f.reward(1,M1,0),Fault.NONE);
        assertTrue(result.confirmed());assertEquals(Map.of("0:sword",1),f.state().inventory());assertEquals(5,f.state().coins());
        var right=f.state().pending().get(M1);assertEquals(op(1),right.grantingReceipt());assertEquals(f.offers.get(M1).source(),right.source());
        assertEquals(f.offers.get(M1).reward().items(),right.items());assertEquals(right,result.receipt().confirmedState().pending().get(M1));}

    @Test void pendingRightCannotBeGrantedAgain(){var f=full();f.coordinator.execute(f.reward(1,M1,0),Fault.NONE);
        refusedUnchanged(f,f.reward(2,M_ALIAS,1),Fault.NONE,Code.DUPLICATE_GRANT);assertEquals(1,f.state().pending().size());}

    @Test void claimWithoutSpacePreservesWholePendingRight(){var f=full();f.coordinator.execute(f.reward(1,M1,0),Fault.NONE);
        refusedUnchanged(f,operation(2,A,1,1,EPOCH,new ClaimPending(M1,op(1))),Fault.NONE,Code.INVENTORY_FULL);}

    @Test void pendingTransferConsumeAndRetryDoNotDuplicateLootOrXp(){var f=full();f.coordinator.execute(f.reward(1,M1,0),Fault.NONE);
        assertTrue(f.coordinator.execute(operation(2,A,1,1,EPOCH,new ConsumeItem("sword",1)),Fault.NONE).confirmed());
        var claim=operation(3,A,2,1,EPOCH,new ClaimPending(M1,op(1)));assertTrue(f.coordinator.execute(claim,Fault.NONE).confirmed());
        assertTrue(f.state().pending().isEmpty());assertEquals(op(3),f.state().claimed().get(M1));assertEquals(XP+3,f.state().xp());assertEquals(5,f.state().coins());
        var before=f.ledger.snapshot();assertEquals(Code.ALREADY_CONFIRMED,f.coordinator.execute(claim,Fault.NONE).code());assertSame(before,f.ledger.snapshot());
        refusedUnchanged(f,operation(4,A,3,1,EPOCH,new ClaimPending(M1,op(1))),Fault.NONE,Code.DUPLICATE_GRANT);
        refusedUnchanged(f,f.reward(5,M_ALIAS,3),Fault.NONE,Code.DUPLICATE_GRANT);assertTrue(f.ledger.historyValid(f.ledger.snapshot()));}

    @Test void pendingClaimMustReferenceExactGrantingReceipt(){var f=full();f.coordinator.execute(f.reward(1,M1,0),Fault.NONE);
        refusedUnchanged(f,operation(2,A,1,1,EPOCH,new ClaimPending(M1,op(99))),Fault.NONE,Code.WRONG_ORIGIN);}

    @Test void pendingWithoutSourceOrReceiptCannotBeConstructedOrImported(){var f=new Fixture();var source=f.offers.get(M1).source();
        assertThrows(NullPointerException.class,()->new PendingEntitlement(M1,op(1),null,Map.of("crystal",1)));
        assertThrows(NullPointerException.class,()->new PendingEntitlement(M1,null,source,Map.of("crystal",1)));
        var state=opening(A);var corrupt=new EconomicState(A,state.revision(),XP,state.domain(),0,2,state.inventory(),
                Map.of(M1,new PendingEntitlement(M1,op(99),source,Map.of("crystal",1))),state.quests(),Map.of());
        assertThrows(IllegalArgumentException.class,()->new MemoryLedger(Map.of(A,corrupt),f.offers,EPOCH,20));}

    @Test void inventoryMoveHasReceiptAndPreservesTotalItems(){var f=new Fixture();var operation=operation(1,A,0,1,EPOCH,new MoveItem(0,1,2));
        assertTrue(f.coordinator.execute(operation,Fault.NONE).confirmed());assertEquals(Map.of("0:potion",1,"1:potion",2),f.state().inventory());
        var before=f.ledger.snapshot();assertEquals(Code.ALREADY_CONFIRMED,f.coordinator.execute(operation,Fault.NONE).code());assertSame(before,f.ledger.snapshot());}

    @Test void invalidMoveOrConsumptionDoesNotCreateReceipt(){var f=new Fixture();
        refusedUnchanged(f,operation(1,A,0,1,EPOCH,new MoveItem(0,99,1)),Fault.NONE,Code.INVALID_OPERATION);
        refusedUnchanged(f,operation(2,A,0,1,EPOCH,new ConsumeItem("potion",99)),Fault.NONE,Code.INVALID_OPERATION);}

    @Test void receiptCapacityRejectsNewCommandsWithoutEviction(){var f=new Fixture(null,1);var original=f.reward(1,M1,0);f.coordinator.execute(original,Fault.NONE);
        refusedUnchanged(f,f.reward(2,M2,1),Fault.NONE,Code.RETENTION_LIMIT);assertEquals(Code.ALREADY_CONFIRMED,f.coordinator.execute(original,Fault.NONE).code());assertEquals(1,f.ledger.snapshot().receipts().size());}

    @Test void losingReceiptOrDedupIndexMakesHistoryIncomplete(){var f=new Fixture();f.coordinator.execute(f.reward(1,M1,0),Fault.NONE);var before=f.ledger.snapshot();
        assertFalse(f.ledger.historyValid(new LedgerImage(before.entries(),Map.of(),before.concessions(),before.watermarks())));
        assertFalse(f.ledger.historyValid(new LedgerImage(before.entries(),before.receipts(),Map.of(),before.watermarks())));
        assertFalse(f.ledger.historyValid(new LedgerImage(before.entries(),before.receipts(),before.concessions(),Map.of())));assertSame(before,f.ledger.snapshot());}

    @Test void conflictingReceiptFingerprintIsNotAcceptedAsHistory(){var f=new Fixture();var receipt=f.coordinator.execute(f.reward(1,Q,0),Fault.NONE).receipt();var before=f.ledger.snapshot();
        var corrupt=new EconomicReceipt(receipt.operation(),receipt.before(),receipt.after(),receipt.beforeHash(),receipt.confirmedState(),"0".repeat(64));
        assertFalse(f.ledger.historyValid(new LedgerImage(before.entries(),Map.of(op(1),corrupt),before.concessions(),before.watermarks())));assertSame(before,f.ledger.snapshot());}

    @Test void coordinatorReinitializationOnlyRetainsSameInMemoryLedger(){var f=new Fixture();var operation=f.reward(1,Q,0);f.coordinator.execute(operation,Fault.AFTER_COMMIT);
        var next=f.ledger.coordinator(A);var before=f.ledger.snapshot();assertTrue(next.reconcile(op(1),Fault.NONE).confirmed());assertEquals(Code.ALREADY_CONFIRMED,next.execute(operation,Fault.NONE).code());assertSame(before,f.ledger.snapshot());}

    @Test void lossOfLedgerMemoryIsNotDurabilityProof(){var f=new Fixture();f.coordinator.execute(f.reward(1,Q,0),Fault.NONE);var fresh=new Fixture();
        assertEquals(Code.NOT_FOUND,fresh.coordinator.reconcile(op(1),Fault.NONE).code());assertNotEquals(f.state(),fresh.state());assertTrue(fresh.ledger.snapshot().receipts().isEmpty());}

    @Test void immutableSnapshotsAndReceiptsStayDetachedFromLaterCommits(){var f=new Fixture();var opening=f.ledger.snapshot();var first=f.coordinator.execute(f.reward(1,M1,0),Fault.NONE).receipt();
        var state=first.confirmedState();f.coordinator.execute(f.reward(2,M2,1),Fault.NONE);assertEquals(0,opening.receipts().size());assertEquals(XP+3,state.xp());
        assertThrows(UnsupportedOperationException.class,()->state.inventory().clear());assertThrows(UnsupportedOperationException.class,()->state.pending().clear());
        assertThrows(UnsupportedOperationException.class,()->f.ledger.snapshot().receipts().clear());assertThrows(UnsupportedOperationException.class,()->f.offers.get(M1).reward().items().clear());}

    @Test void fingerprintsIgnoreMapIterationOrderButIncludeExactSlots(){var state=opening(A);var first=new LinkedHashMap<String,Integer>();first.put("0:potion",1);first.put("1:crystal",2);
        var second=new LinkedHashMap<String,Integer>();second.put("1:crystal",2);second.put("0:potion",1);
        assertEquals(stateHash(seed(0,XP,0,2,first,state.domain(),state.quests())),stateHash(seed(0,XP,0,2,second,state.domain(),state.quests())));
        assertNotEquals(stateHash(seed(0,XP,0,2,first,state.domain(),state.quests())),stateHash(seed(0,XP,0,2,Map.of("0:potion",2,"1:crystal",1),state.domain(),state.quests())));}

    @Test void unknownCatalogItemAndIncompleteQuestOpeningAreRefused(){var state=opening(A);var f=new Fixture();
        assertThrows(IllegalArgumentException.class,()->new MemoryLedger(Map.of(A,seed(0,XP,0,2,Map.of("0:unknown",1),state.domain(),state.quests())),f.offers,EPOCH,20));
        var quests=new HashMap<>(state.quests());quests.put(QUEST,new QuestEntry(QuestStatus.COMPLETED,op(99)));
        assertThrows(IllegalArgumentException.class,()->new MemoryLedger(Map.of(A,seed(0,XP,0,2,state.inventory(),state.domain(),quests)),f.offers,EPOCH,20));}

    @Test void malformedIdsAndNullReferencesFailBeforeAnyCommit(){var f=new Fixture();var before=f.ledger.snapshot();
        assertThrows(NullPointerException.class,()->new EconomicOperationId(null));assertThrows(IllegalArgumentException.class,()->new EconomicRevision(-1));
        assertThrows(IllegalArgumentException.class,()->new EconomicSource(SourceKind.MOB,"forest","lab_slime","slime","v1",null,M1));
        assertThrows(IllegalArgumentException.class,()->new QuestEntry(QuestStatus.COMPLETED,null));
        assertEquals(Code.INVALID_OPERATION,f.coordinator.execute(null,Fault.NONE).code());assertSame(before,f.ledger.snapshot());}

    @Test void concurrentBeneficiariesBothCommitWithoutOverwritingAnotherImage()throws Exception {var f=new Fixture();
        var a=f.ledger.prepare(f.coordinator,f.reward(1,M1,0),Fault.NONE);
        var b=f.ledger.prepare(f.ledger.coordinator(B),f.reward(2,M_B,0),Fault.NONE);
        var results=racePrepared(f.ledger,a,b);assertTrue(results.stream().allMatch(EconomicOperationResult::confirmed));
        assertEquals(5,f.ledger.snapshot().entries().get(A).state().coins());assertEquals(5,f.ledger.snapshot().entries().get(B).state().coins());
        assertEquals(2,f.ledger.snapshot().receipts().size());assertTrue(f.ledger.historyValid(f.ledger.snapshot()));}

    @Test void requestedValueMustMatchTrustedResolvedOffer(){var f=new Fixture();var offered=f.offers.get(M1);
        var inflated=new RewardTerms(1000,500,offered.reward().items(),Map.of());
        refusedUnchanged(f,operation(1,A,0,1,EPOCH,new GrantReward(offered.source(),inflated)),Fault.NONE,Code.INVALID_OPERATION);}

    @Test void unchosenDomainDoesNotGainSyntheticMastery(){var state=opening(A);
        var f=new Fixture(seed(0,0,0,2,state.inventory(),DomainProgression.initial(),state.quests()),256);
        assertTrue(f.coordinator.execute(f.reward(1,Q,0),Fault.NONE).confirmed());assertEquals(80,f.state().xp());assertEquals(DomainProgression.initial(),f.state().domain());}

    @ParameterizedTest @ValueSource(ints={1,3})
    void collectionQuestConsumesOnlyOnConfirmedCompleteTransition(int stock){var initial=opening(A);
        var definition=new QuestService.Definition(List.of(new QuestService.Objective("collect","potion",2)),3,5,"crystal",1);
        var book=new QuestBook(1,List.of(new QuestBook.Draft("lab_collect","Coleta Lab","Coleta descartável",
                "elder","village","",1,"",false,List.of(),definition,false)));
        var authored=new AuthoredQuestSession(book);assertTrue(authored.accept("lab_collect"));
        var bag=InventoryService.restore(new ItemCatalog(RpgContent.builtin()),2,Map.of("0:potion",3));
        authored.refresh(bag,"village");assertEquals(QuestService.State.READY,authored.state("lab_collect"));
        var identity=new QuestIdentity("authored","lab_collect");var quests=new HashMap<>(initial.quests());quests.put(identity,new QuestEntry(QuestStatus.READY,null));
        var state=seed(0,XP,0,2,Map.of("0:potion",stock),initial.domain(),quests);
        var source=new EconomicSource(SourceKind.QUEST,"authored","lab_collect","lab_collect","book-v1",null,AQ);
        var terms=RewardTerms.quest(definition);var grant=new AuthorizedGrant(A,EPOCH,source,terms,definition);
        var ledger=new MemoryLedger(Map.of(A,state),Map.of(AQ,grant),EPOCH,20);var coordinator=ledger.coordinator(A);
        var operation=operation(1,A,0,1,EPOCH,new GrantReward(source,terms));var before=ledger.snapshot();var result=coordinator.execute(operation,Fault.NONE);
        if(stock==1){assertEquals(Code.QUEST_NOT_READY,result.code());assertSame(before,ledger.snapshot());}
        else{assertTrue(result.confirmed());var after=result.receipt().confirmedState();assertEquals(Map.of("0:potion",1,"1:crystal",1),after.inventory());
            assertEquals(op(1),after.quests().get(identity).deliveryReceipt());assertEquals(result.receipt(),coordinator.execute(operation,Fault.NONE).receipt());}
    }

    @Test void partiallyFittingSyntheticBundleIsEntirelyPendingAndClaimRemainsAtomic(){
        var initial=opening(A);var items=new ItemCatalog(RpgContent.builtin());int stock=items.get("crystal").maxStack()-1;
        var state=seed(0,XP,0,1,Map.of("0:crystal",stock),initial.domain(),initial.quests());
        var source=new EconomicSource(SourceKind.MOB,"forest","lab_bundle","slime","lab-v1",1L,M1);
        // Pacote sintético de dois itens de catálogo: testa atomicidade, não modifica tabela de drops.
        var terms=new RewardTerms(3,5,Map.of("crystal",1,"potion",1),Map.of());
        var ledger=new MemoryLedger(Map.of(A,state),Map.of(M1,new AuthorizedGrant(A,EPOCH,source,terms,null)),EPOCH,20);
        var coordinator=ledger.coordinator(A);var operation=operation(1,A,0,1,EPOCH,new GrantReward(source,terms));
        assertTrue(coordinator.execute(operation,Fault.NONE).confirmed());var before=ledger.snapshot();
        assertEquals(Map.of("0:crystal",stock),before.entries().get(A).state().inventory());
        assertEquals(terms.items(),before.entries().get(A).state().pending().get(M1).items());
        assertEquals(Code.INVENTORY_FULL,coordinator.execute(operation(2,A,1,1,EPOCH,new ClaimPending(M1,op(1))),Fault.NONE).code());
        assertSame(before,ledger.snapshot());
    }
}
