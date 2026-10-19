package com.projetok.domain.movement;

import com.projetok.domain.entity.GameEntity;
import com.projetok.domain.entity.Position;
import com.projetok.domain.entity.component.HealthComponent;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

/** Equivalência da busca espacial com busca linear e atualização incremental. */
class SpatialBodyIndexTest {
    /** Cria um corpo vivo com raio fixo. */
    private CollisionService.Body body(int id, float x, float y) {
        var entity = new GameEntity(id, "Corpo", new Position(x, y)) { };
        entity.attach(new HealthComponent(10));
        return new CollisionService.Body(entity, 5);
    }

    /** Mil consultas com 400 corpos coincidem com a geometria sem índice. */
    @Test
    void equivaleABuscaLinearEReduzCandidatos() {
        List<CollisionService.Body> bodies = new ArrayList<>();
        for (int i = 0; i < 400; i++) bodies.add(body(i, 20 + i % 20 * 40, 20 + i / 20 * 40));
        var index = new SpatialBodyIndex(bodies, 32);
        var indexed = new CollisionService.Space(1000, 1000, List.of(), bodies, index);
        var linear = new CollisionService.Space(1000, 1000, List.of(), bodies);
        var probe = body(500, 10, 10).entity();
        var service = new CollisionService();
        Random random = new Random(1024);
        for (int i = 0; i < 1000; i++) {
            float x = random.nextFloat() * 1000, y = random.nextFloat() * 1000;
            assertEquals(service.isFree(probe, x, y, 7, linear), service.isFree(probe, x, y, 7, indexed));
        }
        assertTrue(index.query(400, 400, 420, 420).size() < 20);
    }

    /** Resolução atualiza a célula; teleporte externo exige atualização explícita. */
    @Test
    void atualizaMovimentoETeleporteEMorte() {
        var body = body(1, 20, 20);
        var index = new SpatialBodyIndex(List.of(body), 32);
        var context = new CollisionService.Space(1000, 1000, List.of(), List.of(body), index);
        var collisions = new CollisionService();
        collisions.resolve(body.entity(), 200, 0, 5, context);
        assertFalse(index.query(10, 10, 30, 30).contains(body));
        assertTrue(index.query(210, 10, 230, 30).contains(body));
        body.entity().setPosition(new Position(400, 400));
        index.update(body.entity());
        var probe = body(2, 10, 10).entity();
        assertFalse(collisions.isFree(probe, 400, 400, 5, context));
        body.entity().require(HealthComponent.class).applyDamage(10);
        assertTrue(collisions.isFree(probe, 400, 400, 5, context));
    }

    /** A varredura consulta o trajeto inteiro e atualiza o índice ao terminar. */
    @Test
    void varreduraComIndiceEquivaleALinear() {
        var a = body(1, 20, 20);
        var b = body(2, 150, 20);
        var bodies = List.of(a, b);
        var index = new SpatialBodyIndex(bodies, 32);
        var indexed = new CollisionService.Space(1000, 1000, List.of(), bodies, index);
        var collisions = new CollisionService();
        collisions.resolveContinuous(a.entity(), 500, 0, 5, indexed);
        assertEquals(140, a.entity().getPosition().x());
        assertTrue(index.query(135, 15, 145, 25).contains(a));
    }

    /** Corpos grandes em células distantes ainda são candidatos à colisão. */
    @Test
    void raioGrandeEFronteirasDeCelulas() {
        var small = body(1, 20, 20);
        var large = new CollisionService.Body(body(2, 150, 150).entity(), 100);
        var bodies = List.of(small, large);
        var indexed = new CollisionService.Space(1000, 1000, List.of(), bodies,
                new SpatialBodyIndex(bodies, 16));
        var collisions = new CollisionService();
        assertFalse(collisions.isFree(small.entity(), 50, 150, 5, indexed));
        assertTrue(collisions.isFree(small.entity(), 45, 150, 5, indexed));
    }

    /** Consultas extremas não percorrem bilhões de células e mantêm candidatos. */
    @Test
    void fallbackEValidacaoDoIndice() {
        var body = body(1, 20, 20);
        var index = new SpatialBodyIndex(List.of(body), 32);
        assertEquals(List.of(body), index.query(-Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> new SpatialBodyIndex(List.of(body), Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> new SpatialBodyIndex(List.of(body, body), 32));
        assertThrows(IllegalArgumentException.class, () -> index.query(Double.NaN, 0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new CollisionService.Space(100, 100, List.of(), List.of(), index));
    }
}
