package com.projetok.rpg;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static com.projetok.rpg.Task225R3RecoveryLab.*;
import static org.junit.jupiter.api.Assertions.*;

/** Provas de segurança do modelo sintético; nenhuma recuperação é conectada ao servidor TCP. */
@Timeout(15)
class Task225R3RecoveryLabTest {
    private static final String HASH = "a".repeat(64);
    @TempDir Path temp;

    /** Fixture explícita com domínio e equipamentos reais; números extras pertencem ao envelope de teste. */
    private State state() {
        var world = new RpgWorld(CharacterIdentity.create("Mesmo Nome", "reaper", "azure"));
        world.awardExperience(RpgProgression.builtin().threshold(15));
        assertTrue(world.chooseDomain("thunder"));
        assertTrue(world.inventory().add("sword", 1)); assertTrue(world.equip("sword"));
        assertTrue(world.inventory().add("potion", 3));
        world.interact(); assertTrue(world.acceptQuest("first_steps"));
        var save = world.save();
        var progress = new TreeMap<>(save.quests()); progress.put("first_steps", 1);
        var flags = new TreeSet<>(save.flags()); flags.add("qo:first_steps:0:1");
        // Objetivo parcial é preparação declarada; não simula uma morte/recompensa real.
        var partial = new RpgSave(save.identity(), save.region(), save.x(), save.y(), save.hp() - 7,
                save.xp(), 45, save.inventory(), save.equipment(), progress, flags, save.ticks());
        return envelope(partial);
    }

    /** Recargas absolutas no relógio do owner: a técnica cinco só fica pronta no tick 900. */
    private State envelope(RpgSave save) {
        var ready = new TreeMap<String, Long>(); CLOCKS.forEach(key -> ready.put(key, 200L)); ready.put("skill_5", 900L);
        return new State(1, save, HASH, 7, 100, 430, 1000, Map.of("strength", 4, "energy", 3),
                Map.of("authored_trial", new Quest("ACTIVE", List.of(1, 0), Set.of("kill:mob_1:1"))),
                ready, Set.of("quest:completed_previous:1", "enemy:slime:1"));
    }

    /** Usa colisão real da geometria builtin para o subconjunto de saves suportado pelo harness. */
    private boolean safe(State state) { return RpgWorld.restore(state.character()).save().equals(state.character()); }

    /** Recuperação exige prova local, contexto e spawn; nada é escolhido pelo nome do personagem. */
    private Handle recover(Task225R3RecoveryLab lab, Enrollment enrollment, Ticket ticket, long now) throws IOException {
        return lab.recover(enrollment.credential(), ticket, UUID.randomUUID(), now, HASH, 7, "village", this::safe, Fault.NONE);
    }

    /** Conserva UUID/raça, domínio, XP/nível, HP, energia, inventário, equipamento, moedas e ledgers. */
    @Test void validRecoveryPreservesEntireSnapshotAndDropsBlockedPhase() throws Exception {
        var lab = new Task225R3RecoveryLab(); var before = state();
        var original = lab.enroll(before, new Transient(true, 4, 8, 2));
        var ticket = lab.withdraw(original.handle(), 100, 60);
        assertEquals(0, lab.activeCount());
        var next = recover(lab, original, ticket, 120);
        assertEquals(before, lab.snapshot(next)); assertEquals(Transient.CLEAN, lab.transientState(next));
        assertEquals(15, RpgProgression.builtin().level(lab.snapshot(next).character().xp()));
        assertEquals("thunder", DomainProgression.read(lab.snapshot(next).character().flags()).domain());
        assertEquals(430, lab.snapshot(next).energy()); assertEquals(before.character().hp(), lab.snapshot(next).character().hp());
        assertThrows(SecurityException.class, () -> lab.snapshot(original.handle())); assertEquals(1, lab.activeCount());
    }

    /** Um ticket não cria outra sessão nem soma itens/moedas em tentativas repetidas. */
    @Test void duplicateRecoveryCannotDuplicateItemsOrRewards() throws Exception {
        var lab = new Task225R3RecoveryLab(); var original = lab.enroll(state(), Transient.CLEAN);
        var ticket = lab.withdraw(original.handle(), 100, 60); var next = recover(lab, original, ticket, 110);
        var confirmed = lab.snapshot(next);
        for (int i = 0; i < 10; i++) assertThrows(SecurityException.class, () -> recover(lab, original, ticket, 110));
        assertEquals(confirmed, lab.snapshot(next)); assertEquals(1, lab.activeCount());
    }

    /** Mesmo nome/aparência e posse de outra instância não autorizam a identidade solicitada. */
    @Test void sameNameAndForeignCredentialCannotClaimTicket() throws Exception {
        var lab = new Task225R3RecoveryLab(); var first = lab.enroll(state(), Transient.CLEAN);
        var second = lab.enroll(state(), Transient.CLEAN); var ticket = lab.withdraw(first.handle(), 100, 60);
        assertThrows(SecurityException.class, () -> lab.recover(second.credential(), ticket, UUID.randomUUID(),
                110, HASH, 7, "village", this::safe, Fault.NONE));
        var other = new Task225R3RecoveryLab(); var foreign = other.enroll(state(), Transient.CLEAN);
        assertThrows(SecurityException.class, () -> lab.recover(foreign.credential(), ticket, UUID.randomUUID(),
                110, HASH, 7, "village", this::safe, Fault.NONE));
        assertNotNull(recover(lab, first, ticket, 110)); assertEquals(2, lab.activeCount());
    }

    /** Expiração é exclusiva: último tick aceito 159, recusa a partir de 160, sem renovar o prazo. */
    @ParameterizedTest @ValueSource(longs = {159, 160, 161})
    void expirationHasExplicitBoundary(long now) throws Exception {
        var lab = new Task225R3RecoveryLab(); var original = lab.enroll(state(), Transient.CLEAN);
        var ticket = lab.withdraw(original.handle(), 100, 60);
        if (now < 160) assertNotNull(recover(lab, original, ticket, now));
        else {
            assertThrows(IOException.class, () -> recover(lab, original, ticket, now));
            assertThrows(IOException.class, () -> recover(lab, original, ticket, now + 1));
            assertEquals(0, lab.activeCount());
        }
    }

    /** Dois concorrentes de verdade disputam o mesmo ticket e apenas um obtém posse ativa. */
    @Test void simultaneousRecoveriesHaveExactlyOneWinner() throws Exception {
        var lab = new Task225R3RecoveryLab(); var original = lab.enroll(state(), Transient.CLEAN);
        var ticket = lab.withdraw(original.handle(), 100, 60);
        var ready = new CountDownLatch(2); var start = new CountDownLatch(1);
        var wins = new AtomicInteger(); var refusals = new AtomicInteger(); var workers = Executors.newFixedThreadPool(2);
        try {
            var results = new ArrayList<Future<?>>();
            for (int i = 0; i < 2; i++) results.add(workers.submit(() -> {
                ready.countDown();
                try {
                    if (!start.await(3, TimeUnit.SECONDS)) throw new AssertionError("Barreira expirou");
                    recover(lab, original, ticket, 110); wins.incrementAndGet();
                } catch (SecurityException expected) { refusals.incrementAndGet(); }
                catch (IOException | InterruptedException e) { throw new AssertionError(e); }
            }));
            assertTrue(ready.await(3, TimeUnit.SECONDS)); start.countDown();
            for (var result : results) result.get(5, TimeUnit.SECONDS);
        } finally { workers.shutdownNow(); assertTrue(workers.awaitTermination(3, TimeUnit.SECONDS)); }
        assertEquals(1, wins.get()); assertEquals(1, refusals.get()); assertEquals(1, lab.activeCount());
    }

    /** Spawn ocupado recusa sem consumir; o mesmo ticket pode tentar apoio válido antes da expiração. */
    @Test void blockedSpawnDoesNotConsumeOrRestoreInsideObstacle() throws Exception {
        var lab = new Task225R3RecoveryLab(); var original = lab.enroll(state(), Transient.CLEAN);
        var ticket = lab.withdraw(original.handle(), 100, 60);
        assertThrows(IOException.class, () -> lab.recover(original.credential(), ticket, UUID.randomUUID(), 110,
                HASH, 7, "village", s -> false, Fault.NONE));
        assertEquals(0, lab.activeCount()); assertNotNull(recover(lab, original, ticket, 111));
    }

    /** Posição realmente obstruída é recusada pelo motor; snapshot retido não é publicado como personagem. */
    @Test void realGeometryRejectsObstructedSavedPosition() throws Exception {
        var lab = new Task225R3RecoveryLab(); var s = state(); var old = s.character();
        var bad = new RpgSave(old.identity(), old.region(), 400, 200, old.hp(), old.xp(), old.coins(),
                old.inventory(), old.equipment(), old.quests(), old.flags(), old.ticks());
        var original = lab.enroll(envelope(bad), Transient.CLEAN); var ticket = lab.withdraw(original.handle(), 100, 60);
        assertThrows(IllegalArgumentException.class, () -> recover(lab, original, ticket, 110)); assertEquals(0, lab.activeCount());
    }

    /** Revisão, época e região são compatibilidade, não instruções para teletransportar ou converter estado. */
    @ParameterizedTest @ValueSource(strings = {"revision", "epoch", "region"})
    void changedContextIsRejectedWithoutConsuming(String changed) throws Exception {
        var lab = new Task225R3RecoveryLab(); var original = lab.enroll(state(), Transient.CLEAN);
        var ticket = lab.withdraw(original.handle(), 100, 60);
        assertThrows(IOException.class, () -> lab.recover(original.credential(), ticket, UUID.randomUUID(), 110,
                changed.equals("revision") ? "b".repeat(64) : HASH, changed.equals("epoch") ? 8 : 7,
                changed.equals("region") ? "forest" : "village", this::safe, Fault.NONE));
        assertNotNull(recover(lab, original, ticket, 111));
    }

    /** Falha pré-commit não altera posse nem ticket; nova tentativa instala exatamente o snapshot anterior. */
    @Test void failureBeforeCommitCanRetryWithoutPartialState() throws Exception {
        var lab = new Task225R3RecoveryLab(); var before = state(); var original = lab.enroll(before, Transient.CLEAN);
        var ticket = lab.withdraw(original.handle(), 100, 60);
        assertThrows(IOException.class, () -> lab.recover(original.credential(), ticket, UUID.randomUUID(), 110,
                HASH, 7, "village", this::safe, Fault.BEFORE_COMMIT));
        assertEquals(0, lab.activeCount()); assertEquals(before, lab.snapshot(recover(lab, original, ticket, 111)));
    }

    /** Resposta perdida após commit é reconciliada por recibo; não cria outra admissão no retry. */
    @Test void lostResponseAfterCommitReturnsReceiptWithoutReapplying() throws Exception {
        var lab = new Task225R3RecoveryLab(); var before = state(); var original = lab.enroll(before, Transient.CLEAN);
        var ticket = lab.withdraw(original.handle(), 100, 60); var operation = UUID.randomUUID();
        assertThrows(IOException.class, () -> lab.recover(original.credential(), ticket, operation, 110,
                HASH, 7, "village", this::safe, Fault.AFTER_COMMIT));
        assertEquals(1, lab.activeCount()); var receipt = lab.receipt(original.credential(), operation);
        assertEquals(before, receipt.state()); assertEquals(2, receipt.generation());
        assertThrows(SecurityException.class, () -> recover(lab, original, ticket, 110));
        assertEquals(receipt, lab.receipt(original.credential(), operation));
        assertThrows(SecurityException.class, () -> lab.receipt(original.credential(), UUID.randomUUID()));
    }

    /** XP, baú e missão realmente recompensados pelo harness 196 continuam sem reaplicação. */
    @Test void previouslyGrantedRealCatalogRewardsAreNotReplayed() throws Exception {
        try (var storage = new SyntheticSessionLaboratory(temp.resolve("online-synthetic-r3"))) {
            var owner = storage.create("spirit"); var original = storage.snapshot(owner);
            var earned = Task196ResumeHarness.rewardedFixture(original.state());
            var confirmed = storage.save(owner, 1, earned); storage.disconnect(owner);
            var lab = new Task225R3RecoveryLab(); var enrollment = lab.enroll(envelope(confirmed.state()), Transient.CLEAN);
            var ticket = lab.withdraw(enrollment.handle(), 100, 60); var next = recover(lab, enrollment, ticket, 110);
            var recovered = lab.snapshot(next); Task196ResumeHarness.assertNoReplay(recovered.character());
            assertEquals(earned, recovered.character()); assertEquals(2, confirmed.revision());
            assertThrows(SecurityException.class, () -> recover(lab, enrollment, ticket, 110));
        }
    }

    /** Tempo offline consome a mesma linha de ticks, sem zerar recargas nem aceitar relógio do cliente. */
    @Test void cooldownAndResourcePolicyHasNoFreeResetOrHealing() throws Exception {
        var lab = new Task225R3RecoveryLab(); var before = state(); var original = lab.enroll(before, Transient.CLEAN);
        var ticket = lab.withdraw(original.handle(), 100, 1800); var next = recover(lab, original, ticket, 150);
        var recovered = lab.snapshot(next);
        assertEquals(750, recovered.remaining("skill_5", 150)); assertEquals(50, recovered.remaining("attack", 150));
        assertEquals(0, recovered.remaining("skill_5", 901)); assertEquals(before.energy(), recovered.energy());
        assertEquals(before.character().hp(), recovered.character().hp());
        var nextTicket = lab.withdraw(next, 150, 60);
        assertThrows(IllegalArgumentException.class, () -> recover(lab, original, nextTicket, 149));
        assertNotNull(recover(lab, original, nextTicket, 151));
    }

    /** Missões parciais e recibos são cópias profundas; não se executa evento para reconstruir objetivos. */
    @Test void partialQuestAndCopiesRemainUnchanged() throws Exception {
        var lab = new Task225R3RecoveryLab(); var before = state(); var original = lab.enroll(before, Transient.CLEAN);
        var ticket = lab.withdraw(original.handle(), 100, 60); var next = recover(lab, original, ticket, 110);
        var s = lab.snapshot(next);
        assertEquals(1, s.character().quests().get("first_steps"));
        assertEquals(new Quest("ACTIVE", List.of(1, 0), Set.of("kill:mob_1:1")), s.authoredQuests().get("authored_trial"));
        assertEquals(before.rewards(), s.rewards());
        assertThrows(UnsupportedOperationException.class, () -> s.character().inventory().clear());
        assertThrows(UnsupportedOperationException.class, () -> s.authoredQuests().get("authored_trial").objectives().clear());
        assertThrows(UnsupportedOperationException.class, () -> s.readyAt().clear());
    }

    /** Ticket de geração anterior não retoma nem remove a geração seguinte. */
    @Test void oldTicketCannotAffectAnotherAdmissionGeneration() throws Exception {
        var lab = new Task225R3RecoveryLab(); var original = lab.enroll(state(), Transient.CLEAN);
        var first = lab.withdraw(original.handle(), 100, 60); var next = recover(lab, original, first, 110);
        var second = lab.withdraw(next, 111, 60);
        assertThrows(SecurityException.class, () -> recover(lab, original, first, 112));
        assertNotNull(recover(lab, original, second, 112)); assertEquals(1, lab.activeCount());
    }

    /** O envelope não aceita schema desconhecido nem omissão de recarga, evitando default zero silencioso. */
    @Test void unsupportedVersionAndMissingCooldownAreRejected() {
        var s = state();
        assertThrows(IllegalArgumentException.class, () -> new State(2, s.character(), HASH, 7, 100,
                s.energy(), s.maximumEnergy(), s.attributes(), s.authoredQuests(), s.readyAt(), s.rewards()));
        var incomplete = new TreeMap<>(s.readyAt()); incomplete.remove("skill_5");
        assertThrows(IllegalArgumentException.class, () -> new State(1, s.character(), HASH, 7, 100,
                s.energy(), s.maximumEnergy(), s.attributes(), s.authoredQuests(), incomplete, s.rewards()));
    }

    /** Limite de dois registros e autoridade perdida após restart são recusas, sem fallback por nome/UUID. */
    @Test void capacityAndRestartCannotInventAuthenticatedRecovery() throws Exception {
        var lab = new Task225R3RecoveryLab(); var original = lab.enroll(state(), Transient.CLEAN);
        lab.enroll(state(), Transient.CLEAN); assertThrows(IllegalStateException.class, () -> lab.enroll(state(), Transient.CLEAN));
        var ticket = lab.withdraw(original.handle(), 100, 60); var restarted = new Task225R3RecoveryLab();
        assertThrows(SecurityException.class, () -> recover(restarted, original, ticket, 110));
        assertNotNull(recover(lab, original, ticket, 110));
    }
}
