package com.projetok.domain.movement;

import com.projetok.domain.entity.DialogueScript;
import com.projetok.domain.entity.Npc;
import com.projetok.domain.entity.Position;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Casos geométricos da saída seletiva, separados da habilidade e do teste reservado. */
class CollisionRecoveryTest {
    private static final float RADIUS = 18f;

    /** Monta um ator sólido sem exigir componentes de combate para um NPC. */
    private Npc actor(float x, float y) {
        return new Npc(1, "Trajetória", new Position(x, y), DialogueScript.of("Teste"));
    }

    /** Uma curva segura é refeita por eixos; sua corda atravessaria a construção. */
    @Test void recuperaPelaCurvaRealAoRedorDaConstrucao() {
        var actor = actor(200f, 200f);
        var space = new CollisionService.Space(960f, 640f, List.of(
                new CollisionService.Obstacle(130f, 130f, 40f, 40f),
                new CollisionService.Obstacle(190f, 80f, 40f, 140f, CollisionCategory.SOFT_OBSTACLE)),
                List.of());
        var recovery = new CollisionService().recoverAlongPath(actor, List.of(
                new Position(100f, 100f), new Position(200f, 100f), new Position(200f, 200f)),
                RADIUS, space);
        assertTrue(recovery.recovered());
        assertEquals(new Position(172f, 100f), actor.getPosition());
        assertEquals(List.of(new Position(200f, 200f), new Position(200f, 100f),
                new Position(172f, 100f)), recovery.path());
        assertThrows(UnsupportedOperationException.class, () -> recovery.path().clear());
    }

    /** Uma parede no caminho de retorno impede aproveitar uma saída livre do outro lado. */
    @Test void recuoNaoUsaSaidaAlemDeParedeQueSurgiuNoPercurso() {
        var actor = actor(250f, 100f);
        var body = new CollisionService.Body(actor, RADIUS);
        var bodies = List.of(body);
        var index = new SpatialBodyIndex(bodies, 16f);
        var space = new CollisionService.Space(960f, 640f, List.of(
                new CollisionService.Obstacle(210f, 0f, 2f, 200f),
                new CollisionService.Obstacle(240f, 80f, 30f, 40f, CollisionCategory.SOFT_OBSTACLE)),
                bodies, index);
        var recovery = new CollisionService().recoverAlongPath(actor,
                List.of(new Position(100f, 100f), actor.getPosition()), RADIUS, space);
        assertFalse(recovery.recovered());
        assertEquals(new Position(250f, 100f), actor.getPosition());
        assertEquals(List.of(actor.getPosition()), recovery.path());
        assertTrue(index.query(250, 100, 250, 100).contains(body));
    }

    /** A tangência compartilhada continua sendo uma saída válida, mesmo sem vão de um pixel. */
    @Test void encontraTangenciaEntreDoisIntervalosEncostados() {
        var actor = actor(250f, 100f);
        var space = new CollisionService.Space(960f, 640f, List.of(
                new CollisionService.Obstacle(80f, 80f, 82f, 40f, CollisionCategory.SOFT_OBSTACLE),
                new CollisionService.Obstacle(198f, 80f, 100f, 40f, CollisionCategory.SOFT_OBSTACLE)),
                List.of());
        var recovery = new CollisionService().recoverAlongPath(actor,
                List.of(new Position(50f, 100f), actor.getPosition()), RADIUS, space);
        assertTrue(recovery.recovered());
        assertEquals(new Position(180f, 100f), actor.getPosition());
        assertTrue(new CollisionService().isFree(actor, 180f, 100f, RADIUS, space));
    }

    /** Rotas incompletas, diagonais ou não finitas são rejeitadas antes de corrigir posição. */
    @Test void rejeitaRotasInvalidasSemMutacao() {
        var actor = actor(250f, 100f);
        var origin = actor.getPosition();
        var space = new CollisionService.Space(960f, 640f, List.of(), List.of());
        var collisions = new CollisionService();
        for (var path : List.of(List.<Position>of(), List.of(new Position(100f, 100f)),
                List.of(new Position(100f, 200f), origin),
                List.of(new Position(Float.NaN, 100f), origin))) {
            assertThrows(IllegalArgumentException.class,
                    () -> collisions.recoverAlongPath(actor, path, RADIUS, space));
            assertEquals(origin, actor.getPosition());
        }
    }
}
