package com.projetok.domain.movement;

import com.projetok.domain.entity.GameEntity;
import com.projetok.domain.entity.Position;
import com.projetok.domain.entity.component.HealthComponent;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Regressões geométricas executadas sem interface gráfica. */
class MovementServiceTest {
    private final CollisionService collisions = new CollisionService();
    private final MovementService movement = new MovementService(collisions);

    private GameEntity actor(int id, float x, float y) {
        GameEntity actor = new GameEntity(id, "Actor", new Position(x, y)) { };
        actor.attach(new HealthComponent(10));
        return actor;
    }

    /** Verifica velocidade e delta de tempo nos quatro sentidos e nas diagonais. */
    @Test
    void calculaDeslocamentoHorizontalVerticalEDiagonal() {
        var space = new CollisionService.Space(1000, 1000, List.of(), List.of());
        for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) {
            var a = actor(1, 500, 500);
            var direction = movement.direction(x, y);
            movement.moveDirected(a, direction, 215f, 1f / 30f, 18f, space);
            assertEquals(500f + direction.x() * 215f * (1f / 30f), a.getPosition().x());
            assertEquals(500f + direction.y() * 215f * (1f / 30f), a.getPosition().y());
            assertEquals(x == 0 && y == 0 ? 0 : 215f / 30f,
                    Math.hypot(a.getPosition().x() - 500, a.getPosition().y() - 500), 0.0001);
        }
    }

    /** A resolução também pode ser usada diretamente pelo servidor. */
    @Test
    void resolucaoDiretaPreservaLimitesEDeslizamento() {
        var a = actor(1, 10, 10);
        var space = new CollisionService.Space(100, 100, List.of(), List.of());
        collisions.resolve(a, -6, 3, 5, space);
        assertEquals(new Position(10, 13), a.getPosition());
        collisions.resolve(a, -5, 0, 5, space);
        assertEquals(new Position(5, 13), a.getPosition());
    }

    @Test
    void normalizesAllDirectionsAndPreservesZero() {
        assertEquals(new Position(0, 0), movement.direction(0, 0));
        for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) {
            if (x == 0 && y == 0) continue;
            Position d = movement.direction(x, y);
            assertEquals(1, Math.hypot(d.x(), d.y()), 0.000001);
            assertEquals(Math.signum(x), Math.signum(d.x()));
            assertEquals(Math.signum(y), Math.signum(d.y()));
        }
    }

    @Test
    void allowsBoundaryTangencyAndRejectsOutsideEachEdge() {
        var a = actor(1, 10, 10);
        var space = new CollisionService.Space(100, 80, List.of(), List.of());
        assertTrue(collisions.isFree(a, 5, 5, 5, space));
        assertTrue(collisions.isFree(a, 95, 75, 5, space));
        assertFalse(collisions.isFree(a, 4.99f, 10, 5, space));
        assertFalse(collisions.isFree(a, 10, 4.99f, 5, space));
        assertFalse(collisions.isFree(a, 95.01f, 10, 5, space));
        assertFalse(collisions.isFree(a, 10, 75.01f, 5, space));
    }

    @Test
    void slidesAlongWallAndResolvesXBeforeY() {
        var a = actor(1, 25, 40);
        var wall = new CollisionService.Obstacle(40, 20, 20, 40);
        var space = new CollisionService.Space(100, 100, List.of(wall), List.of());
        movement.move(a, 11, 8, 5, space);
        assertEquals(new Position(25, 48), a.getPosition());
        a.setPosition(new Position(30, 10));
        movement.move(a, 10, 10, 5, space);
        assertEquals(new Position(40, 10), a.getPosition());
        assertTrue(collisions.isFree(a, 35, 40, 5, space));
        assertFalse(collisions.isFree(a, 36, 40, 5, space));
        assertTrue(collisions.isFree(a, 36, 16, 5, space));
        assertFalse(collisions.isFree(a, 37, 17, 5, space));
    }

    @Test
    void skipsSelfAndDeadBodiesAndReadsLivePositions() {
        var a = actor(1, 20, 20);
        var b = actor(2, 40, 20);
        var space = new CollisionService.Space(100, 100, List.of(), List.of(
                new CollisionService.Body(a, 5), new CollisionService.Body(b, 7)));
        assertTrue(collisions.isFree(a, 20, 20, 5, space));
        assertTrue(collisions.isFree(a, 28, 20, 5, space));
        assertFalse(collisions.isFree(a, 29, 20, 5, space));
        movement.move(a, 9, 0, 5, space);
        assertEquals(new Position(20, 20), a.getPosition());
        b.setPosition(new Position(60, 20));
        assertTrue(collisions.isFree(a, 29, 20, 5, space));
        b.setPosition(new Position(40, 20));
        b.require(HealthComponent.class).applyDamage(10);
        movement.move(a, 20, 0, 5, space);
        assertEquals(new Position(40, 20), a.getPosition());
    }
}
