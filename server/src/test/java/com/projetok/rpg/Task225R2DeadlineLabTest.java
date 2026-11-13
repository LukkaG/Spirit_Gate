package com.projetok.rpg;

import com.projetok.domain.entity.Position;
import com.projetok.domain.movement.CollisionService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Modelo candidato exclusivo de laboratório; o engine real conserva a espera sem prazo. */
@Timeout(20)
class Task225R2DeadlineLabTest {
    /** Identifica mundo, região, revisão e execução; nenhum desses dados atravessa o wire. */
    private record End(int player, UUID character, String region, int revision, long execution) { }

    /** Câmara descartável da R1, com owner e pedido de retirada modelados somente no teste. */
    private static final class Lab {
        final RpgSkillEngine engine = new RpgSkillEngine();
        final Map<Integer, RpgWorld> players = new LinkedHashMap<>();
        final SharedRpgEncounter encounter;
        final RpgSceneSet scenes;
        final RpgWorld caster;
        RpgWorld blocker;
        long tick, execution = 1;
        int revision, removals;
        boolean candidate = true;

        Lab() {
            var chamber = List.of(new CollisionService.Obstacle(0, 0, 110, 640),
                    new CollisionService.Obstacle(170, 0, 790, 640),
                    new CollisionService.Obstacle(110, 0, 60, 280),
                    new CollisionService.Obstacle(110, 320, 60, 320));
            var space = new CollisionService.Space(960, 640, chamber, List.of());
            var definitions = Map.of("r2_target", "slime|forest|144|300");
            scenes = new RpgSceneSet("forest", Map.of("forest", new RpgSceneSet.Geometry(space, new Position(140, 300)),
                    "village", new RpgSceneSet.Geometry(space, new Position(140, 300))), definitions,
                    Map.of(), Map.of(), Map.of("escape", "forest|140|300|village|140|300"));
            encounter = new SharedRpgEncounter(Map.of("forest", space, "village", space), definitions,
                    SharedRpgEncounter.RewardPolicy.SYNTHETIC_PARTICIPANTS, 1000);
            caster = character("Lancador R2");
            caster.awardExperience(RpgProgression.builtin().threshold(15));
            assertTrue(caster.chooseDomain("thunder"));
            players.put(1, caster); encounter.join(1, caster);
            caster.skillEnemies().forEach(enemy -> enemy.skillRootUntil = 100_000);
            assertTrue(engine.cast(1, caster, new RpgSkillEngine.Cast(5, 0, 0, 0, 0), players));
            blocker = character("Apoio R2");
            players.put(2, blocker); encounter.join(2, blocker);
        }

        /** Cria identidade nova pela mesma fábrica de mundos, sem setters de progresso em execução. */
        RpgWorld character(String name) {
            var world = new RpgWorld(CharacterIdentity.create(name, "reaper", "azure"), scenes);
            world.useNetworkFootprint();
            return world;
        }

        /** Última tentativa real precede a decisão candidata, inclusive no tick relativo 92. */
        void advance(int count) {
            for (int i = 0; i < count; i++) {
                tick++;
                players.values().forEach(world -> world.tick(0, 0));
                encounter.step(tick); engine.clock(tick); engine.tick(players);
                if (candidate && tick == 92 && players.get(1) == caster && caster.skillIntangible()) end(token());
            }
        }

        /** O token é capturado antes da retirada e comparado novamente pelo owner candidato. */
        End token() { return new End(1, caster.networkState().identity().id(), "forest", revision, execution); }

        /** Retirada lógica idempotente; não conhece transporte, socket ou UI. */
        boolean end(End request) {
            var world = players.get(request.player());
            if (world == null || !world.networkState().identity().id().equals(request.character())
                    || !world.networkState().region().equals(request.region()) || request.revision() != revision
                    || request.execution() != execution) return false;
            engine.remove(request.player()); players.remove(request.player()); encounter.leave(world); removals++;
            return true;
        }

        /** Libera a câmara por APIs de encontro; a morte é preparação explícita, fora dos quatro cortes. */
        void release() {
            if (blocker != null) { players.remove(2); encounter.leave(blocker); blocker = null; }
            for (var enemy : caster.skillEnemies()) if (enemy.hp > 0) caster.skillDamage(enemy, enemy.generation, enemy.hp);
        }

        /** O ledger real distingue os quatro cortes e as recompensas posteriores. */
        long cuts() { return encounter.events().stream().filter(e -> e.kind().equals("HIT") && e.amount() == 14).count(); }
    }

    /** Exercita todas as bordas, pedido duplicado, ausência de recompensa tardia e liberação após saída. */
    @Test void blockedDeadlineHasOneRemovalAndNoLateDamageOrReward() {
        var lab = new Lab(); var token = lab.token();
        var xp = lab.caster.progressView(); var inventory = lab.caster.inventory().snapshot();
        lab.advance(31); assertTrue(lab.caster.skillIntangible()); assertEquals(0, lab.cuts());
        lab.advance(1); assertEquals(4, lab.cuts()); assertTrue(lab.caster.skillIntangible());
        lab.advance(59); assertEquals(91, lab.tick); assertEquals(0, lab.removals);
        lab.advance(1); assertEquals(1, lab.removals); assertFalse(lab.players.containsKey(1));
        assertFalse(lab.caster.skillIntangible()); assertFalse(lab.caster.skillInDash());
        assertTrue(lab.engine.events().isEmpty()); assertFalse(lab.end(token));
        lab.advance(1); assertEquals(93, lab.tick); assertEquals(1, lab.removals); assertEquals(4, lab.cuts());
        lab.release(); lab.advance(10);
        assertEquals(1, lab.removals); assertEquals(4, lab.cuts());
        assertEquals(xp, lab.caster.progressView()); assertEquals(inventory, lab.caster.inventory().snapshot());
        assertTrue(lab.encounter.events().stream().noneMatch(e -> e.kind().equals("SYNTHETIC_REWARD")));
    }

    /** Apoio liberado na borda ganha prioridade sobre qualquer encerramento. */
    @ParameterizedTest @ValueSource(ints = {32, 91, 92})
    void supportReleasedBeforeLastAttemptMaterializes(int at) {
        var lab = new Lab(); lab.advance(at - 1); lab.release(); lab.advance(1);
        assertFalse(lab.caster.skillIntangible()); assertTrue(lab.players.containsKey(1));
        long expectedHits = at == 32 ? 0 : 4; // Aos 32, a preparação já retirou o alvo antes dos cortes.
        assertEquals(0, lab.removals); assertEquals(expectedHits, lab.cuts());
        lab.advance(94 - at); assertEquals(0, lab.removals); assertEquals(expectedHits, lab.cuts());
    }

    /** Sem o coordenador candidato, o runtime continua esperando aos 92/93: produção preservada. */
    @Test void unchangedRuntimeStillWaitsPastCandidateDeadline() {
        var lab = new Lab(); lab.candidate = false; lab.advance(93);
        assertTrue(lab.caster.skillIntangible()); assertEquals(0, lab.removals); assertEquals(4, lab.cuts());
    }

    /** Desconexão espontânea usa a limpeza existente e invalida o pedido antes do prazo. */
    @Test void spontaneousDepartureInvalidatesPendingEnd() {
        var lab = new Lab(); lab.advance(60); var token = lab.token();
        assertTrue(lab.end(token)); lab.advance(33);
        assertFalse(lab.end(token)); assertEquals(1, lab.removals); assertEquals(4, lab.cuts());
    }

    /** Região, revisão, execução e identidade substituídas não aceitam resultados atrasados. */
    @ParameterizedTest @ValueSource(strings = {"region", "revision", "execution", "identity", "other-player"})
    void staleRequestCannotRemoveAnotherContext(String change) {
        var lab = new Lab(); lab.advance(60); var token = lab.token();
        switch (change) {
            case "region" -> { assertTrue(lab.caster.travel("escape")); lab.engine.tick(lab.players); }
            case "revision" -> { lab.revision++; lab.engine.clear(); }
            case "execution" -> lab.execution++;
            case "identity" -> {
                lab.engine.remove(1); lab.encounter.leave(lab.caster);
                var replacement = lab.character("Novo R2"); lab.players.put(1, replacement); lab.encounter.join(1, replacement);
            }
            case "other-player" -> token = new End(2, token.character(), token.region(), token.revision(), token.execution());
            default -> fail(change);
        }
        assertFalse(lab.end(token)); assertEquals(0, lab.removals);
        assertTrue(lab.players.containsKey(1)); assertTrue(lab.players.containsKey(2));
    }
}
