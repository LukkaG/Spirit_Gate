package com.projetok.multiplayer;

import com.projetok.rpg.RpgSkillEngine;
import com.projetok.rpg.SharedRpgEncounter;
import com.projetok.studio.LiveRevision;
import com.projetok.studio.WorldContent;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static com.projetok.multiplayer.RpgSessionWorld.CharacterCaptureStatus.*;

/** Ensaios TCP descartáveis: só a fila existente é acessada pela fixture, sem ler sessões privadas. */
@Timeout(30)
class OwnerCharacterCaptureTest {
    /** Mantém transporte, coordenador e owner reais, limitando todas as esperas do teste. */
    private static final class Lab implements AutoCloseable {
        final RpgSessionWorld world;
        final LocalSimulationServer server;
        final Queue<Runnable> commands;
        final List<FutureTask<?>> jobs=new ArrayList<>();

        /** Conecta a fixture à fila de capacidade 64, sem criar caminho novo na produção. */
        Lab() throws Exception {this(false);}

        /** O ensaio econômico usa HP de catálogo e nível inicial, antes de qualquer admissão. */
        @SuppressWarnings("unchecked")
        Lab(boolean catalogRewards) throws Exception {
            world=new RpgSessionWorld(List.of("reaper","human"),SessionCharacterSelectionTest.catalog(),true,catalogRewards?1:20);
            world.enableRevisions(WorldContent.baseline());
            if(catalogRewards){var revision=world.capture().content();
                var encounter=new SharedRpgEncounter(revision.geometry(),revision.bundle().scenes().enemies(),
                        SharedRpgEncounter.RewardPolicy.SYNTHETIC_PARTICIPANTS);
                world.installRevision(new RpgSessionWorld.RevisionCandidate(revision,world.capture().players(),encounter));}
            server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world);
            var field=LocalSimulationServer.class.getDeclaredField("commands");
            field.setAccessible(true);commands=(Queue<Runnable>)field.get(server);
            server.start();
        }

        /** Só enfileira tarefas de teste limitadas; cancelamento não interrompe o owner. */
        <T> FutureTask<T> submit(Callable<T> action) {
            var task=new FutureTask<T>(()->{
                assertEquals("coop-world",Thread.currentThread().getName());return action.call();
            });
            assertTrue(commands.offer(task),"Fila existente cheia");jobs.add(task);return task;
        }

        /** Toda espera ocorre no chamador, nunca dentro da operação de captura. */
        <T> T owner(Callable<T> action) throws Exception {return submit(action).get(5,TimeUnit.SECONDS);}

        /** O cliente recebe apenas mensagens dos protocolos existentes 6 e 7. */
        LocalClient connect(String name,int application) throws Exception {
            var selection=new SessionCharacterSelection(name,"reaper","none","none");
            var client=application==7?LocalClient.skillGame(InetAddress.getLoopbackAddress(),server.port(),
                    selection,false,false,null,Task201CharacterSessionTest.EMPTY):
                    LocalClient.gameplay(InetAddress.getLoopbackAddress(),server.port(),selection,
                            false,false,null,Task201CharacterSessionTest.EMPTY);
            client.revisions().renderer(p->CompletableFuture.completedFuture(null));return client;
        }

        /** Cancela qualquer pedido de teste restante antes de fechar o servidor descartável. */
        public void close(){for(var job:jobs)job.cancel(false);server.close();}
    }

    /** Compara todos os snapshots existentes no mesmo ponto, sem APIs lazy de atributos/energia. */
    private static List<Object> views(Lab lab,int a,int b) {
        return List.of(lab.world.snapshot(),lab.world.gameplaySnapshotFor(a),lab.world.skillsSnapshotFor(a),
                lab.world.gameplaySnapshotFor(b),lab.world.combatSnapshotFor(b));
    }

    @ParameterizedTest @ValueSource(ints={6,7})
    void realOwnerCopiesBothParticipantsWithoutChangingPrivateOrObserverState(int application) throws Exception {
        try(var lab=new Lab();var caster=lab.connect("Captura Owner",7);var observer=lab.connect("Observador Owner",application)) {
            Task201CharacterSessionTest.ready(caster,observer);
            Task201CharacterSessionTest.waitFor(()->caster.skills()!=null&&observer.playerSheet()!=null,caster,observer);
            lab.owner(()->{
                var expected=views(lab,caster.id(),observer.id());
                var request=lab.world.characterCaptureRequest(caster.id());
                var first=lab.world.captureCharacter(request);
                assertEquals(CAPTURED,first.status());assertEquals(first,lab.world.captureCharacter(request));
                var other=lab.world.captureCharacter(lab.world.characterCaptureRequest(observer.id()));
                assertEquals(CAPTURED,other.status());assertNotEquals(first.participant(),other.participant());
                assertNotEquals(first.observation().character().identity().id(),other.observation().character().identity().id());
                assertEquals(caster.id(),first.observation().timing().participant());
                assertEquals(first.observation().character().identity().id(),first.observation().timing().identity());
                assertEquals(first.metadata().contentHash(),lab.world.contentHash());
                assertEquals(first.metadata().worldTick(),first.observation().character().tick());
                assertEquals(first.metadata().skillEngineClock(),first.observation().timing().engineClock());
                assertEquals(first.metadata().sessionTick(),lab.world.snapshot().tick());
                assertNull(first.metadata().durableWorldEpoch());assertFalse(first.economicHistoryComplete());
                assertFalse(first.activationAuthorized());assertEquals(expected,views(lab,caster.id(),observer.id()));
                assertThrows(UnsupportedOperationException.class,()->first.observation().character().slots().clear());
                return true;
            });
            long before=observer.snapshot().tick();
            Task201CharacterSessionTest.waitFor(()->observer.snapshot().tick()>before+2,caster,observer);
            assertTrue(caster.connected());assertTrue(observer.connected());assertNull(lab.server.failure());
        }
    }

    @Test void absentReadyActiveAndRemovedSkillSessionsRemainDistinct() throws Exception {
        try(var lab=new Lab();var caster=lab.connect("Timing Owner",7);var observer=lab.connect("Timing Testemunha",6)) {
            Task201CharacterSessionTest.ready(caster,observer);
            assertTrue(caster.gameplay(GameplayProtocol.Operation.CHOOSE_DOMAIN,"thunder"));
            Task201CharacterSessionTest.waitFor(()->caster.playerSheet()!=null&&caster.playerSheet().progress().domain().equals("thunder"),caster,observer);
            lab.owner(()->{
                var request=lab.world.characterCaptureRequest(caster.id());
                var absent=lab.world.captureCharacter(request);
                assertEquals(RpgSkillEngine.TimingStatus.ABSENT,absent.observation().timing().status());
                assertNull(absent.observation().timing().deadlines());
                // Validação real cria Session antes de recusar ausência de alvo, sem lançar a skill.
                assertFalse(lab.world.skill(caster.id(),new SkillProtocol.Request(1000,1,0,0,280,320)));
                var ready=lab.world.captureCharacter(request);
                assertEquals(RpgSkillEngine.TimingStatus.PRESENT,ready.observation().timing().status());
                assertTrue(ready.observation().timing().deadlines().stream().allMatch(d->d.readyAtTick()==0));
                assertTrue(lab.world.skill(caster.id(),new SkillProtocol.Request(1001,5,0,0,0,0)));
                var expected=views(lab,caster.id(),observer.id());var active=lab.world.captureCharacter(request);
                assertTrue(active.observation().character().restrictions().intangible());
                assertTrue(active.observation().timing().deadlines().get(4).readyAtTick()>active.metadata().worldTick());
                assertEquals(expected,views(lab,caster.id(),observer.id()));assertFalse(active.activationAuthorized());
                // A instalação em memória da mesma revisão troca os mundos e limpa skills pelo fluxo existente.
                lab.world.installRevision(lab.world.prepareRevision(lab.world.capture().content()));
                assertEquals(REPLACED,lab.world.captureCharacter(request).status());
                var next=lab.world.captureCharacter(lab.world.characterCaptureRequest(caster.id()));
                assertEquals(CAPTURED,next.status());assertEquals(RpgSkillEngine.TimingStatus.ABSENT,next.observation().timing().status());
                assertNull(next.observation().timing().deadlines());return true;
            });
            assertNull(lab.server.failure());
        }
    }

    @Test void wrongThreadMissingSourceAndInvalidRequestReturnOnlyDiagnostics() throws Exception {
        var unbound=new RpgSessionWorld();assertEquals(WRONG_OWNER,unbound.captureCharacter(null).status());
        try(var lab=new Lab();var client=lab.connect("Owner Restrito",6)) {
            Task201CharacterSessionTest.ready(client);
            var request=lab.owner(()->lab.world.characterCaptureRequest(client.id()));
            var wrong=lab.world.captureCharacter(request);assertEquals(WRONG_OWNER,wrong.status());
            assertNull(wrong.observation());assertNull(wrong.metadata());
            assertThrows(IllegalStateException.class,()->lab.world.characterCaptureRequest(client.id()));
            lab.owner(()->{
                assertEquals(INVALID_REQUEST,lab.world.captureCharacter(null).status());
                var missing=lab.world.captureCharacter(lab.world.characterCaptureRequest(999999));
                assertEquals(ABSENT,missing.status());assertNull(missing.observation());return true;
            });
        }
    }

    @Test void domainRegionFreezeAndContentRevisionRejectStaleRequests() throws Exception {
        try(var lab=new Lab();var client=lab.connect("Contexto Owner",7)) {
            Task201CharacterSessionTest.ready(client);
            lab.owner(()->{
                var before=lab.world.characterCaptureRequest(client.id());
                assertTrue(lab.world.gameplay(client.id(),new GameplayProtocol.Request(1000,GameplayProtocol.Operation.CHOOSE_DOMAIN,"thunder")));
                assertEquals(DOMAIN_CHANGED,lab.world.captureCharacter(before).status());
                var chosen=lab.world.characterCaptureRequest(client.id());
                lab.world.freeze(true);
                try{assertEquals(TRANSITION,lab.world.captureCharacter(chosen).status());}
                finally{lab.world.freeze(false);}
                assertTrue(lab.world.visitEditorRegion(client.id(),"village"));
                assertEquals(REGION_CHANGED,lab.world.captureCharacter(chosen).status());
                assertTrue(lab.world.visitEditorRegion(client.id(),"forest"));
                var original=lab.world.capture().content();
                var next=new LiveRevision(original.number()+1,original.hash(),original.bundle(),original.profile());
                lab.world.installRevision(lab.world.prepareRevision(next));
                var changed=lab.world.captureCharacter(chosen);assertEquals(CONTENT_CHANGED,changed.status());
                assertNull(changed.observation());assertNull(changed.metadata());
                var current=lab.world.captureCharacter(lab.world.characterCaptureRequest(client.id()));
                assertEquals(CAPTURED,current.status());assertEquals(next.number(),current.metadata().contentRevision());
                return true;
            });
            assertNull(lab.server.failure());
        }
    }

    @Test void disconnectBetweenDepartureDrainAndCaptureIsRejectedThenRemoved() throws Exception {
        try(var lab=new Lab();var caster=lab.connect("Desconexao Owner",7);var observer=lab.connect("Desconexao Observa",6)) {
            Task201CharacterSessionTest.ready(caster,observer);
            var request=lab.owner(()->lab.world.characterCaptureRequest(caster.id()));
            var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
            var pending=lab.submit(()->{
                entered.countDown();assertTrue(release.await(3,TimeUnit.SECONDS));
                return lab.world.captureCharacter(request);
            });
            try{
                assertTrue(entered.await(3,TimeUnit.SECONDS));caster.close();
                Task201CharacterSessionTest.waitFor(()->lab.server.players()==1&&lab.server.connections()==1);
            }finally{release.countDown();}
            var result=pending.get(5,TimeUnit.SECONDS);assertEquals(DISCONNECTING,result.status());
            assertNull(result.observation());
            Task201CharacterSessionTest.waitFor(()->observer.snapshot().actors().stream().noneMatch(a->a.id()==caster.id()),observer);
            assertEquals(REMOVED,lab.owner(()->lab.world.captureCharacter(request)).status());
            try(var next=lab.connect("Outro Participante",7)){
                Task201CharacterSessionTest.ready(next,observer);
                lab.owner(()->{
                    assertEquals(REMOVED,lab.world.captureCharacter(request).status());
                    var fresh=lab.world.captureCharacter(lab.world.characterCaptureRequest(next.id()));
                    assertEquals(CAPTURED,fresh.status());assertNotEquals(caster.id(),fresh.participant());return true;
                });
            }
            assertTrue(observer.connected());assertNull(lab.server.failure());
        }
    }

    @Test void sameNumericIdWithAnotherSessionCannotReuseOldRequestInLaboratory() {
        var world=new RpgSessionWorld();world.bindCaptureOwner(Thread.currentThread(),id->true);
        assertTrue(world.join(1,"Primeiro Local"));var request=world.characterCaptureRequest(1);
        var old=world.captureCharacter(request);world.leave(1);assertTrue(world.join(1,"Segundo Local"));
        var refused=world.captureCharacter(request);assertEquals(REPLACED,refused.status());assertNull(refused.observation());
        var next=world.captureCharacter(world.characterCaptureRequest(1));assertEquals(CAPTURED,next.status());
        // UUID legado deriva de instancia:id e pode coincidir; o token compara o objeto Session real.
        assertEquals(old.observation().character().identity().id(),next.observation().character().identity().id());
        assertNotEquals(old.observation().character().identity().name(),next.observation().character().identity().name());
    }

    @Test void commandOrderSnapshotsAndReadCostAreMeasuredWithoutAdvancingSimulation() throws Exception {
        try(var lab=new Lab();var client=lab.connect("Custo Captura",7);var other=lab.connect("Custo Observador",6)) {
            Task201CharacterSessionTest.ready(client,other);
            var measurement=lab.owner(()->{
                var request=lab.world.characterCaptureRequest(client.id());
                var second=lab.world.characterCaptureRequest(other.id());
                long tick=lab.world.snapshot().tick();var before=List.of(lab.world.captureCharacter(request),lab.world.captureCharacter(second));
                for(int i=0;i<20;i++)assertEquals(before,List.of(lab.world.captureCharacter(request),lab.world.captureCharacter(second)));
                long[] nanos=new long[100];
                for(int i=0;i<nanos.length;i++){
                    long start=System.nanoTime();var value=List.of(lab.world.captureCharacter(request),lab.world.captureCharacter(second));nanos[i]=System.nanoTime()-start;
                    assertEquals(before,value);
                }
                assertEquals(tick,lab.world.snapshot().tick());Arrays.sort(nanos);
                return List.of(tick,nanos[0],nanos[49],nanos[94],nanos[99]);
            });
            System.out.println("ORG03R4 capture pairedParticipants=2 samples=100 warmup=20 sessionTick="+measurement.get(0)
                    +" minNs="+measurement.get(1)+" medianNs="+measurement.get(2)+" p95Ns="+measurement.get(3)+" maxNs="+measurement.get(4));
            Task201CharacterSessionTest.waitFor(()->client.snapshot().tick()>measurement.get(0),client);
            assertNull(lab.server.failure());
        }
    }

    @Test void consecutiveCommandsCaptureBeforeStepAndSnapshotReflectsMovementAfterStep() throws Exception {
        try(var lab=new Lab();var client=lab.connect("Ordem Captura",6)) {
            Task201CharacterSessionTest.ready(client);
            var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
            var gate=lab.submit(()->{entered.countDown();assertTrue(release.await(3,TimeUnit.SECONDS));return true;});
            assertTrue(entered.await(3,TimeUnit.SECONDS));
            FutureTask<RpgSessionWorld.CharacterCaptureResult> first,second;
            try{
                first=lab.submit(()->{
                    var request=lab.world.characterCaptureRequest(client.id());
                    var result=lab.world.captureCharacter(request);
                    assertTrue(lab.world.accept(client.id(),new Protocol.Intent(1000,Protocol.Action.MOVE,1,0)));
                    return result;
                });
                second=lab.submit(()->lab.world.captureCharacter(lab.world.characterCaptureRequest(client.id())));
            }finally{release.countDown();}
            gate.get(5,TimeUnit.SECONDS);var before=first.get(5,TimeUnit.SECONDS);var after=second.get(5,TimeUnit.SECONDS);
            assertEquals(before,after,"A intenção não avança movimento entre comandos");
            Task201CharacterSessionTest.waitFor(()->client.snapshot().tick()>after.metadata().sessionTick(),client);
            var moved=lab.owner(()->lab.world.captureCharacter(lab.world.characterCaptureRequest(client.id())));
            assertTrue(moved.observation().character().x()>after.observation().character().x());
            assertEquals(before.observation().character().x(),after.observation().character().x());assertNull(lab.server.failure());
        }
    }

    @Test void staleSkillRegionIsRefusedWithoutCleanupUntilNormalStepRemovesIt() throws Exception {
        try(var lab=new Lab();var client=lab.connect("Binding Recarga",7)) {
            Task201CharacterSessionTest.ready(client);
            long tick=lab.owner(()->{
                assertTrue(lab.world.gameplay(client.id(),new GameplayProtocol.Request(1000,GameplayProtocol.Operation.CHOOSE_DOMAIN,"thunder")));
                assertFalse(lab.world.skill(client.id(),new SkillProtocol.Request(1001,1,0,0,280,320)));
                assertTrue(lab.world.visitEditorRegion(client.id(),"village"));
                var request=lab.world.characterCaptureRequest(client.id());
                assertEquals(SKILL_BINDING_MISMATCH,lab.world.captureCharacter(request).status());
                assertEquals(SKILL_BINDING_MISMATCH,lab.world.captureCharacter(request).status());
                return lab.world.snapshot().tick();
            });
            Task201CharacterSessionTest.waitFor(()->client.snapshot().tick()>tick,client);
            var current=lab.owner(()->lab.world.captureCharacter(lab.world.characterCaptureRequest(client.id())));
            assertEquals(CAPTURED,current.status());assertEquals(RpgSkillEngine.TimingStatus.ABSENT,current.observation().timing().status());
            assertNull(current.observation().timing().deadlines());assertNull(lab.server.failure());
        }
    }

    @Test void lateParticipantKeepsSeparateWorldSessionAndSkillClocks() throws Exception {
        try(var lab=new Lab();var first=lab.connect("Relogio Antigo",7)) {
            Task201CharacterSessionTest.ready(first);long start=first.snapshot().tick();
            Task201CharacterSessionTest.waitFor(()->first.snapshot().tick()>start+5,first);
            try(var next=lab.connect("Relogio Novo",7)){
                Task201CharacterSessionTest.ready(first,next);
                var result=lab.owner(()->{
                    assertTrue(lab.world.gameplay(next.id(),new GameplayProtocol.Request(1000,GameplayProtocol.Operation.CHOOSE_DOMAIN,"thunder")));
                    assertTrue(lab.world.skill(next.id(),new SkillProtocol.Request(1001,5,0,0,0,0)));
                    return lab.world.captureCharacter(lab.world.characterCaptureRequest(next.id()));
                });
                assertEquals(CAPTURED,result.status());assertTrue(result.metadata().sessionTick()>result.metadata().worldTick());
                assertEquals(result.observation().timing().worldTick(),result.metadata().worldTick());
                assertEquals(result.observation().timing().engineClock(),result.metadata().skillEngineClock());
                long duration=com.projetok.rpg.RpgSkillCatalog.forDomain("thunder").get(4).cooldownTicks();
                long deadline=result.observation().timing().deadlines().get(4).readyAtTick();
                assertEquals(result.metadata().worldTick()+duration,deadline);
                assertNotEquals(result.metadata().sessionTick()+duration,deadline);
                assertNull(result.metadata().durableWorldEpoch());assertNotNull(result.metadata().sessionWorldInstance());
            }
        }
    }

    @Test void realRewardBalancesAndLedgersAreReadWithoutAnotherGrant() throws Exception {
        try(var lab=new Lab(true);var caster=lab.connect("Economia Captura",6);var observer=lab.connect("Economia Observa",7)) {
            Task201CharacterSessionTest.ready(caster,observer);
            Task225R1CombatFixture.defeatSlime(lab.server,caster,observer);
            lab.owner(()->{
                var request=lab.world.characterCaptureRequest(caster.id());var value=lab.world.captureCharacter(request);
                assertEquals(CAPTURED,value.status());assertTrue(value.observation().character().coins()>0);
                assertTrue(value.observation().character().xp()>0);var expected=lab.world.gameplaySnapshotFor(caster.id());
                for(int i=0;i<20;i++)assertEquals(value,lab.world.captureCharacter(request));
                assertEquals(expected,lab.world.gameplaySnapshotFor(caster.id()));
                assertFalse(value.economicHistoryComplete());assertFalse(value.activationAuthorized());return true;
            });
            assertTrue(observer.connected());assertNull(lab.server.failure());
        }
    }

    @Test void disappearanceAtFinalPresenceCheckDiscardsWholeObservationInLaboratory() {
        var world=new RpgSessionWorld();var checks=new java.util.concurrent.atomic.AtomicInteger();
        world.bindCaptureOwner(Thread.currentThread(),id->checks.incrementAndGet()==1);
        assertTrue(world.join(1,"Presenca Local"));var before=world.snapshot();
        var result=world.captureCharacter(world.characterCaptureRequest(1));
        assertEquals(DISCONNECTING,result.status());assertNull(result.observation());assertNull(result.metadata());
        assertEquals(before,world.snapshot());assertEquals(2,checks.get());
    }
}
