package com.projetok.domain.movement;

import com.projetok.domain.entity.DialogueScript;
import com.projetok.domain.entity.Npc;
import com.projetok.domain.entity.Position;
import com.projetok.domain.entity.component.HealthComponent;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Políticas filtram categorias sem duplicar a geometria normal ou a varredura contínua. */
class CollisionPolicyTest {
    private static final float RADIUS = 18f;
    private static final CollisionPolicy PHASE = CollisionPolicy.PHASE_THROUGH_ACTORS_AND_SOFT;

    /** Cria um corpo com vida sem depender de fábricas de habilidades. */
    private static Npc actor(int id, float x, float y) {
        var actor = new Npc(id, "Corpo de teste", new Position(x, y), DialogueScript.of("Corpo de teste"));
        actor.attach(new HealthComponent(100));
        return actor;
    }

    /** Obstáculos antigos conservam a classificação sólida do cenário original. */
    @Test void obstaculoSemCategoriaContinuaSolido() {
        var obstacle = new CollisionService.Obstacle(180f, 60f, 30f, 80f);
        assertEquals(CollisionCategory.SOLID_OBSTACLE, obstacle.category());
        var player = actor(1, 100f, 100f);
        var space = new CollisionService.Space(960f, 640f, List.of(obstacle), List.of());
        assertFalse(new CollisionService().isFree(player, 190f, 100f, RADIUS, space, PHASE));
    }

    /** A mesma posição é livre para passagem seletiva e ocupada para a física normal. */
    @Test void consultaSeletivaIgnoraAtoresEArbustosMasPreservaLimites() {
        var player = actor(1, 100f, 100f);
        var blocker = actor(2, 200f, 100f);
        var space = new CollisionService.Space(960f, 640f,
                List.of(new CollisionService.Obstacle(180f, 80f, 40f, 40f, CollisionCategory.SOFT_OBSTACLE)),
                List.of(new CollisionService.Body(blocker, 19f)));
        var collisions = new CollisionService();
        assertFalse(collisions.isFree(player, 200f, 100f, RADIUS, space));
        assertFalse(collisions.isFree(player, 200f, 100f, RADIUS, space, CollisionPolicy.SOLID));
        assertTrue(collisions.isFree(player, 200f, 100f, RADIUS, space, PHASE));
        for (Position outside : List.of(new Position(17f, 100f), new Position(943f, 100f),
                new Position(100f, 17f), new Position(100f, 623f))) {
            assertFalse(collisions.isFree(player, outside.x(), outside.y(), RADIUS, space, PHASE));
        }
    }

    /** Paredes finas ou espessas detêm até deslocamentos que atravessariam toda a forma. */
    @Test void varreduraSeletivaMantemParedesFinasEEspessas() {
        var collisions = new CollisionService();
        for (float thickness : new float[] {1f, 80f}) {
            var player = actor(1, 100f, 100f);
            var space = new CollisionService.Space(960f, 640f,
                    List.of(new CollisionService.Obstacle(200f, 50f, thickness, 100f)), List.of());
            collisions.resolveContinuous(player, 700f, 0f, RADIUS, space, PHASE);
            assertEquals(new Position(182f, 100f), player.getPosition());
            assertTrue(collisions.isFree(player, 182f, 100f, RADIUS, space));
        }
    }

    /** A política seletiva preserva o deslizamento por eixo ao atingir um sólido. */
    @Test void varreduraSeletivaDeslizaNoEixoLivre() {
        var player = actor(1, 100f, 100f);
        var space = new CollisionService.Space(960f, 640f,
                List.of(new CollisionService.Obstacle(160f, 0f, 2f, 640f)), List.of());
        new MovementService(new CollisionService()).moveContinuous(player, 100f, 100f, RADIUS, space, PHASE);
        assertEquals(new Position(142f, 200f), player.getPosition());
    }

    /** As quatro bordas continuam sólidas, inclusive em deslocamento diagonal contínuo. */
    @Test void varreduraSeletivaMantemLimitesNosDoisSentidos() {
        var player = actor(1, 900f, 580f);
        var space = new CollisionService.Space(960f, 640f, List.of(), List.of());
        var movement = new MovementService(new CollisionService());
        movement.moveContinuous(player, 300f, 300f, RADIUS, space, PHASE);
        assertEquals(new Position(942f, 622f), player.getPosition());
        movement.moveContinuous(player, -2000f, -2000f, RADIUS, space, PHASE);
        assertEquals(new Position(18f, 18f), player.getPosition());
    }

    /** O caminho normal utilizado pelo WASD continua tratando arbustos como bloqueadores. */
    @Test void movimentoNormalContinuaBloqueadoPorObstaculosLeves() {
        var player = actor(1, 100f, 100f);
        var space = new CollisionService.Space(960f, 640f,
                List.of(new CollisionService.Obstacle(140f, 80f, 30f, 40f, CollisionCategory.SOFT_OBSTACLE)),
                List.of());
        var movement = new MovementService(new CollisionService());
        movement.move(player, 50f, 0f, RADIUS, space);
        assertEquals(new Position(100f, 100f), player.getPosition());
        movement.moveContinuous(player, 150f, 0f, RADIUS, space);
        assertEquals(new Position(122f, 100f), player.getPosition());
    }
}
