package com.projetok.domain.movement;

import com.projetok.domain.entity.GameEntity;
import com.projetok.domain.entity.Position;
import com.projetok.domain.entity.component.HealthComponent;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Entradas inválidas e varredura sem tunelamento, executadas sem interface. */
class MovementHardeningTest {
    private final CollisionService collisions = new CollisionService();
    private final MovementService movement = new MovementService(collisions);

    /** Cria um ator mínimo. */
    private GameEntity actor(float x, float y) {
        var entity = new GameEntity(1, "Ator", new Position(x, y)) { };
        entity.attach(new HealthComponent(10));
        return entity;
    }

    /** Cenário sem corpos adicionais. */
    private CollisionService.Space space(CollisionService.Obstacle... obstacles) {
        return new CollisionService.Space(200, 200, List.of(obstacles), List.of());
    }

    /** Nenhum eixo pode ser aplicado antes da validação de todos os números. */
    @Test
    void rejeitaNaoFinitosSemMutacaoParcial() {
        var a = actor(20, 20);
        for (float invalid : new float[] {Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> collisions.isFree(a, invalid, 20, 5, space()));
            assertThrows(IllegalArgumentException.class, () -> collisions.isFree(a, 20, invalid, 5, space()));
            assertThrows(IllegalArgumentException.class, () -> collisions.isFree(a, 20, 20, invalid, space()));
            assertThrows(IllegalArgumentException.class, () -> movement.direction(invalid, 1));
            assertThrows(IllegalArgumentException.class, () -> movement.move(a, 5, invalid, 5, space()));
            assertThrows(IllegalArgumentException.class, () -> movement.moveContinuous(a, 5, invalid, 5, space()));
            assertThrows(IllegalArgumentException.class, () -> movement.moveDirected(a,
                    new Position(1, 0), invalid, 0.1f, 5, space()));
            assertThrows(IllegalArgumentException.class, () -> movement.moveDirected(a,
                    new Position(1, 0), 10, invalid, 5, space()));
            assertEquals(new Position(20, 20), a.getPosition());
        }
    }

    /** Overflow de posição e de velocidade também falha antes da resolução. */
    @Test
    void rejeitaOverflowENegativos() {
        var a = actor(Float.MAX_VALUE, 20);
        assertThrows(IllegalArgumentException.class, () -> movement.move(a, Float.MAX_VALUE, 5, 5, space()));
        assertEquals(new Position(Float.MAX_VALUE, 20), a.getPosition());
        var b = actor(20, 20);
        assertThrows(IllegalArgumentException.class, () -> movement.moveDirected(b,
                new Position(1, 0), Float.MAX_VALUE, 2, 5, space()));
        assertThrows(IllegalArgumentException.class, () -> movement.moveDirected(b,
                new Position(1, 0), -1, 1, 5, space()));
        assertThrows(IllegalArgumentException.class, () -> movement.moveDirected(b,
                new Position(1, 0), 1, -1, 5, space()));
        assertThrows(IllegalArgumentException.class, () -> collisions.resolve(b, 1, 1, 0, space()));
        assertEquals(new Position(20, 20), b.getPosition());
    }

    /** Geometria inválida não pode entrar no contexto. */
    @Test
    void validaGeometriaEOrigem() {
        assertThrows(IllegalArgumentException.class, () -> new CollisionService.Space(Float.NaN, 100, List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new CollisionService.Space(100, -1, List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new CollisionService.Obstacle(0, 0, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> new CollisionService.Obstacle(Float.MAX_VALUE, 0, Float.MAX_VALUE, 1));
        assertThrows(IllegalArgumentException.class, () -> new CollisionService.Body(actor(0, 0), -1));
        var a = actor(Float.NaN, 10);
        assertThrows(IllegalArgumentException.class, () -> collisions.resolve(a, 1, 0, 5, space()));
        assertTrue(Float.isNaN(a.getPosition().x()));
    }

    /** Normalização extrema não produz vetor zero por overflow. */
    @Test
    void normalizaMagnitudeExtrema() {
        var d = movement.direction(Float.MAX_VALUE, Float.MAX_VALUE);
        assertEquals(1, Math.hypot(d.x(), d.y()), 0.000001);
        var tiny = movement.direction(Float.MIN_VALUE, Float.MIN_VALUE);
        assertEquals(1, Math.hypot(tiny.x(), tiny.y()), 0.000001);
    }

    /** Amostra o percurso inteiro, incluindo cantos arredondados e contatos fracionários. */
    @Test
    void trajetosContinuosNaoPenetramGeometria() {
        var context = space(new CollisionService.Obstacle(40, 40, 20, 20),
                new CollisionService.Obstacle(100, 10, 1, 170));
        var random = new java.util.Random(412);
        for (int trial = 0; trial < 100; trial++) {
            float x = 10 + random.nextFloat() * 180, y = 10 + random.nextFloat() * 180;
            var a = actor(x, y);
            if (!collisions.isFree(a, x, y, 5, context)) continue;
            collisions.resolveContinuous(a, random.nextFloat() * 300 - 150,
                    random.nextFloat() * 300 - 150, 5, context);
            var end = a.getPosition();
            for (int i = 0; i <= 100; i++) {
                float progress = i / 100f;
                // Interpolação em double e saturação evitam ultrapassar o contato por arredondamento.
                float sampledX = (float) ((double) x + ((double) end.x() - x) * progress);
                float sampledY = (float) ((double) y + ((double) end.y() - y) * progress);
                assertTrue(collisions.isFree(a, sampledX, y, 5, context));
                assertTrue(collisions.isFree(a, end.x(), sampledY, 5, context));
            }
        }
    }

    /** O caminho discreto fica compatível, enquanto o contínuo para na parede fina. */
    @Test
    void impedeTunelamentoNosDoisSentidos() {
        var wall = new CollisionService.Obstacle(50, 0, 1, 100);
        var a = actor(20, 30);
        collisions.resolve(a, 100, 0, 5, space(wall));
        assertEquals(120, a.getPosition().x());
        a.setPosition(new Position(20, 30));
        movement.moveContinuous(a, 100, 0, 5, space(wall));
        assertEquals(45, a.getPosition().x());
        a.setPosition(new Position(120, 30));
        movement.moveContinuous(a, -100, 0, 5, space(wall));
        assertEquals(56, a.getPosition().x());
    }

    /** Varredura vertical e deslizamento preservam a prioridade X antes de Y. */
    @Test
    void varreVerticalEDesliza() {
        var a = actor(20, 20);
        collisions.resolveContinuous(a, 0, 100, 5, space(new CollisionService.Obstacle(0, 50, 100, 1)));
        assertEquals(new Position(20, 45), a.getPosition());
        a.setPosition(new Position(20, 20));
        collisions.resolveContinuous(a, 100, 40, 5, space(new CollisionService.Obstacle(50, 0, 1, 100)));
        assertEquals(new Position(45, 60), a.getPosition());
    }

    /** Cantos são arredondados pela geometria circular; trajetória tangente é livre. */
    @Test
    void respeitaCantoETangencia() {
        var wall = new CollisionService.Obstacle(40, 40, 20, 20);
        var a = actor(20, 36);
        collisions.resolveContinuous(a, 70, 0, 5, space(wall));
        assertEquals(37, a.getPosition().x(), 0.0001);
        a.setPosition(new Position(20, 35));
        collisions.resolveContinuous(a, 70, 0, 5, space(wall));
        assertEquals(90, a.getPosition().x());
    }

    /** Entidades vivas bloqueiam o trajeto, mortas não bloqueiam. */
    @Test
    void varreCirculosSemTunelamento() {
        var a = actor(20, 30);
        var b = actor(70, 30);
        var context = new CollisionService.Space(200, 200, List.of(), List.of(new CollisionService.Body(b, 7)));
        collisions.resolveContinuous(a, 100, 0, 5, context);
        assertEquals(58, a.getPosition().x());
        b.require(HealthComponent.class).applyDamage(10);
        collisions.resolveContinuous(a, 100, 0, 5, context);
        assertEquals(158, a.getPosition().x());
    }

    /** O contato mais próximo prevalece, independentemente da ordem dos obstáculos. */
    @Test
    void contatoMaisProximoELimites() {
        var a = actor(20, 30);
        collisions.resolveContinuous(a, 150, 0, 5, space(
                new CollisionService.Obstacle(100, 0, 1, 100), new CollisionService.Obstacle(50, 0, 1, 100)));
        assertEquals(45, a.getPosition().x());
        a.setPosition(new Position(20, 30));
        collisions.resolveContinuous(a, 1000, -1000, 5, space());
        assertEquals(new Position(195, 5), a.getPosition());
    }

    /** A varredura não tenta corrigir uma origem já penetrada. */
    @Test
    void origemPenetradaFalhaSemMutacao() {
        var a = actor(45, 45);
        assertThrows(IllegalStateException.class, () -> collisions.resolveContinuous(a, 10, 10, 5,
                space(new CollisionService.Obstacle(40, 40, 20, 20))));
        assertEquals(new Position(45, 45), a.getPosition());
    }
}
