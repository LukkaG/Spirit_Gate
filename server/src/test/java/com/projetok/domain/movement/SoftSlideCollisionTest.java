package com.projetok.domain.movement;

import com.projetok.domain.effect.StatusEffectType;
import com.projetok.domain.entity.GameEntity;
import com.projetok.domain.entity.Mob;
import com.projetok.domain.entity.Position;
import com.projetok.domain.entity.component.HealthComponent;
import com.projetok.domain.entity.component.StatusEffectsComponent;
import com.projetok.domain.item.LootTable;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Verifica o escape tangencial lento sem reduzir hitboxes ou empurrar outros atores. */
class SoftSlideCollisionTest {
    private final CollisionService collisions = new CollisionService();

    // --- Escape por corpos circulares e quinas externas ---

    /** Um contato lateral converte o bloqueio em avanço tangencial com velocidade reduzida. */
    @Test void contornaCorpoSemEmpurrarEMaisDevagarQueMovimentoLivre() {
        GameEntity actor = ator(1, 20, 20);
        GameEntity blocker = ator(2, 30, 25);
        var space = espacoCorpos(actor, blocker, 5);
        Position blockerOrigin = blocker.getPosition();

        collisions.resolveSliding(actor, 4, 0, 5, space);

        Position p = actor.getPosition();
        assertTrue(p.x() > 20, "O escape precisa permitir avanço na direção pedida");
        assertTrue(p.y() < 20, "A tangente deve afastar o ator do lado do bloqueador");
        assertTrue(distancia(new Position(20, 20), p) <= 4 * 0.45 + 0.0001);
        assertTrue(collisions.isFree(actor, p.x(), p.y(), 5, space));
        assertEquals(blockerOrigin, blocker.getPosition(), "O bloqueador não pode ser empurrado");
    }

    /** Os quatro espelhamentos evitam uma preferência artificial por baixo ou pela direita. */
    @Test void escapeCircularRespeitaEspelhamentosDosContatos() {
        for (int sx : new int[] {-1, 1}) for (int sy : new int[] {-1, 1}) {
            float x = espelhar(20, sx), y = espelhar(20, sy);
            GameEntity actor = ator(1, x, y);
            GameEntity blocker = ator(2, espelhar(30, sx), espelhar(25, sy));
            var space = espacoCorpos(actor, blocker, 5);

            collisions.resolveSliding(actor, 4 * sx, 0, 5, space);

            Position p = actor.getPosition();
            assertTrue((p.x() - x) * sx > 0);
            assertTrue((p.y() - y) * sy < 0);
            assertTrue(distancia(new Position(x, y), p) <= 4 * 0.45 + 0.0001);
            assertTrue(collisions.isFree(actor, p.x(), p.y(), 5, space));
        }
    }

    /** Uma quina externa admite escape lento nos quatro lados sem encolher o raio. */
    @Test void contornaQuinaExternaNosQuatroEspelhamentos() {
        for (int sx : new int[] {-1, 1}) for (int sy : new int[] {-1, 1}) {
            float x = espelhar(33, sx), y = espelhar(32, sy);
            GameEntity actor = ator(1, x, y);
            var space = new CollisionService.Space(100, 100,
                    List.of(new CollisionService.Obstacle(40, 40, 20, 20)), List.of());

            collisions.resolveSliding(actor, 5 * sx, 0, 10, space);

            Position p = actor.getPosition();
            assertTrue((p.x() - x) * sx > 0);
            assertTrue((p.y() - y) * sy < 0);
            assertTrue(distancia(new Position(x, y), p) <= 5 * 0.45 + 0.0001);
            assertTrue(collisions.isFree(actor, p.x(), p.y(), 10, space));
        }
    }

    /** Pressões repetidas permitem sair do contato sem cruzar o corpo intermediário. */
    @Test void variosTicksPermitemEscaparSemAtravessarOutroCorpo() {
        GameEntity actor = ator(1, 20, 20);
        GameEntity blocker = ator(2, 30, 25);
        var space = espacoCorpos(actor, blocker, 5);
        for (int tick = 0; tick < 24; tick++) {
            Position before = actor.getPosition();
            collisions.resolveSliding(actor, 4, 0, 5, space);
            Position after = actor.getPosition();
            for (int sample = 0; sample <= 40; sample++) {
                float ratio = sample / 40f;
                float x = before.x() + (after.x() - before.x()) * ratio;
                float y = before.y() + (after.y() - before.y()) * ratio;
                assertTrue(collisions.isFree(actor, x, y, 5, space),
                        "O caminho entre ticks deve preservar a separação dos corpos");
            }
        }
        assertTrue(actor.getPosition().x() > 40, "O ator deve conseguir contornar o bloqueador");
        assertEquals(new Position(30, 25), blocker.getPosition());
    }

    // --- Barreiras que não admitem um desvio tangencial válido ---

    /** Um contato frontal simétrico não escolhe arbitrariamente um lado de escape. */
    @Test void contatoFrontalExatoContinuaBloqueado() {
        GameEntity actor = ator(1, 20, 20);
        GameEntity blocker = ator(2, 30, 20);

        collisions.resolveSliding(actor, 4, 0, 5, espacoCorpos(actor, blocker, 5));

        assertEquals(new Position(20, 20), actor.getPosition());
    }

    /** Paredes planas, limites e quinas internas continuam impedindo penetração. */
    @Test void paredesLimitesECantosInternosContinuamSolidos() {
        GameEntity actor = ator(1, 35, 35);
        var wall = new CollisionService.Obstacle(40, 10, 10, 70);
        var wallSpace = new CollisionService.Space(100, 100, List.of(wall), List.of());
        collisions.resolveSliding(actor, 4, 0, 5, wallSpace);
        assertEquals(new Position(35, 35), actor.getPosition());

        var cornerSpace = new CollisionService.Space(100, 100,
                List.of(wall, new CollisionService.Obstacle(10, 40, 70, 10)), List.of());
        collisions.resolveSliding(actor, 4, 4, 5, cornerSpace);
        assertEquals(new Position(35, 35), actor.getPosition());

        actor.setPosition(new Position(5, 35));
        collisions.resolveSliding(actor, -4, 0, 5,
                new CollisionService.Space(100, 100, List.of(), List.of()));
        assertEquals(new Position(5, 35), actor.getPosition());
    }

    /** A tangente de um corpo não pode ignorar outro corpo fechando a passagem. */
    @Test void doisCorposNaoPermitemForcarUmaPassagemEstreita() {
        GameEntity actor = ator(1, 22, 20);
        GameEntity upper = ator(2, 30, 14);
        GameEntity lower = ator(3, 30, 26);
        var space = new CollisionService.Space(100, 100, List.of(), List.of(
                new CollisionService.Body(actor, 5), new CollisionService.Body(upper, 5),
                new CollisionService.Body(lower, 5)));
        assertTrue(collisions.isFree(actor, 22, 20, 5, space));

        for (int tick = 0; tick < 20; tick++) collisions.resolveSliding(actor, 4, 0, 5, space);

        assertEquals(new Position(22, 20), actor.getPosition());
        assertEquals(new Position(30, 14), upper.getPosition());
        assertEquals(new Position(30, 26), lower.getPosition());
    }

    /** Uma origem já sobreposta não dispara expulsão automática nem move o outro ator. */
    @Test void origemSobrepostaNaoProvocaExpulsaoAutomatica() {
        GameEntity actor = ator(1, 20, 20);
        GameEntity blocker = ator(2, 25, 20);
        var space = espacoCorpos(actor, blocker, 5);

        collisions.resolveSliding(actor, 4, 0, 5, space);

        assertEquals(new Position(20, 20), actor.getPosition());
        assertEquals(new Position(25, 20), blocker.getPosition());
    }

    // --- Contratos compartilhados: índice, validação e imobilização ---

    /** A grade recebe a nova posição do ator sem alterar o corpo usado no contato. */
    @Test void atualizaIndiceAposEscapeMantendoOutroAtorFixo() {
        GameEntity actor = ator(1, 20, 20);
        GameEntity blocker = ator(2, 30, 25);
        var bodies = List.of(new CollisionService.Body(actor, 5), new CollisionService.Body(blocker, 5));
        var index = new SpatialBodyIndex(bodies, 1);
        var space = new CollisionService.Space(100, 100, List.of(), bodies, index);
        assertTrue(index.query(20, 25.1, 20, 25.1).stream().anyMatch(body -> body.entity() == actor));

        collisions.resolveSliding(actor, 4, 0, 5, space);

        assertTrue(actor.getPosition().y() < 20);
        assertFalse(index.query(20, 25.1, 20, 25.1).stream().anyMatch(body -> body.entity() == actor),
                "A célula antiga deve deixar de conter o ator movido");
        var p = actor.getPosition();
        assertTrue(index.query(p.x(), p.y(), p.x(), p.y()).stream().anyMatch(body -> body.entity() == actor));
        assertEquals(new Position(30, 25), blocker.getPosition());
    }

    /** A nova política mantém a rejeição de números inválidos antes de qualquer mutação. */
    @Test void rejeitaEntradasInvalidasSemAlterarPosicao() {
        GameEntity actor = ator(1, 20, 20);
        GameEntity blocker = ator(2, 30, 25);
        var space = espacoCorpos(actor, blocker, 5);
        for (float invalid : new float[] {Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class,
                    () -> collisions.resolveSliding(actor, invalid, 0, 5, space));
            assertThrows(IllegalArgumentException.class,
                    () -> collisions.resolveSliding(actor, 0, invalid, 5, space));
            assertThrows(IllegalArgumentException.class,
                    () -> collisions.resolveSliding(actor, 4, 0, invalid, space));
            assertEquals(new Position(20, 20), actor.getPosition());
        }
        for (float radius : new float[] {0, -1}) {
            assertThrows(IllegalArgumentException.class,
                    () -> collisions.resolveSliding(actor, 4, 0, radius, space));
            assertEquals(new Position(20, 20), actor.getPosition());
        }
    }

    /** Freeze intercepta todos os caminhos do serviço antes do escape tangencial. */
    @Test void freezeBloqueiaMovimentoMesmoComPoliticaDeDeslizamento() {
        Mob mob = new Mob(1, "Mob", new Position(20, 20), LootTable.of());
        mob.attach(new HealthComponent(10));
        var effects = new StatusEffectsComponent();
        effects.refresh(StatusEffectType.FREEZE, 1);
        mob.attach(effects);
        GameEntity blocker = ator(2, 30, 25);
        var space = espacoCorpos(mob, blocker, 5);
        var movement = new MovementService(collisions, MovementService.Response.SOFT_SLIDE);

        movement.move(mob, 4, 0, 5, space);
        movement.moveDirected(mob, new Position(1, 0), 40, 0.1f, 5, space);
        movement.moveContinuous(mob, 4, 0, 5, space);

        assertEquals(new Position(20, 20), mob.getPosition());
        assertEquals(1f, effects.remaining(StatusEffectType.FREEZE));
        assertThrows(IllegalArgumentException.class,
                () -> movement.move(mob, Float.NaN, 0, 5, space));

        effects.advance(1);
        movement.move(mob, 4, 0, 5, space);
        assertTrue(mob.getPosition().x() > 20);
        assertTrue(mob.getPosition().y() < 20);
    }

    /** A política opcional preserva posições e velocidade do movimento livre em todas as direções. */
    @Test void movimentoLivreEVelocidadeDiagonalPermanecemIguais() {
        var strict = new MovementService(collisions);
        var sliding = new MovementService(collisions, MovementService.Response.SOFT_SLIDE);
        var space = new CollisionService.Space(1000, 1000, List.of(), List.of());
        for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) {
            GameEntity original = ator(1, 500, 500);
            GameEntity actor = ator(2, 500, 500);
            Position direction = strict.direction(x, y);

            strict.moveDirected(original, direction, 215, 1f / 30f, 18, space);
            sliding.moveDirected(actor, direction, 215, 1f / 30f, 18, space);

            assertEquals(original.getPosition(), actor.getPosition());
            assertEquals(x == 0 && y == 0 ? 0 : 215f / 30f,
                    distancia(new Position(500, 500), actor.getPosition()), 0.0001);
        }
    }

    /** A resolução por eixos permanece intacta quando ao menos um eixo pode avançar. */
    @Test void preservaDeslizamentoAnteriorQuandoUmEixoEstaLivre() {
        var strict = new MovementService(collisions);
        var sliding = new MovementService(collisions, MovementService.Response.SOFT_SLIDE);
        var space = new CollisionService.Space(100, 100,
                List.of(new CollisionService.Obstacle(40, 20, 20, 40)), List.of());
        GameEntity original = ator(1, 25, 40);
        GameEntity actor = ator(2, 25, 40);

        strict.move(original, 11, 8, 5, space);
        sliding.move(actor, 11, 8, 5, space);

        assertEquals(new Position(25, 48), actor.getPosition());
        assertEquals(original.getPosition(), actor.getPosition());
    }

    // --- Cenários mínimos sem Swing ou dependências externas ---

    /** Cria um ator com o componente de vida exigido pelas consultas de colisão. */
    private static GameEntity ator(int id, float x, float y) {
        GameEntity entity = new GameEntity(id, "Ator", new Position(x, y)) { };
        entity.attach(new HealthComponent(10));
        return entity;
    }

    /** Monta um cenário com o ator móvel e um bloqueador de mesmo raio. */
    private static CollisionService.Space espacoCorpos(GameEntity actor, GameEntity blocker, float radius) {
        return new CollisionService.Space(100, 100, List.of(), List.of(
                new CollisionService.Body(actor, radius), new CollisionService.Body(blocker, radius)));
    }

    /** Espelha uma coordenada em torno do centro do cenário de cem unidades. */
    private static float espelhar(float coordinate, int sign) {
        return sign > 0 ? coordinate : 100 - coordinate;
    }

    /** Mede deslocamento efetivo sem pressupor arredondamentos da projeção tangencial. */
    private static double distancia(Position a, Position b) {
        return Math.hypot(a.x() - b.x(), a.y() - b.y());
    }
}
