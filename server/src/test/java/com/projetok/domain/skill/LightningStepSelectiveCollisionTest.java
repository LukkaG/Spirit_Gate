package com.projetok.domain.skill;

import com.projetok.domain.entity.DialogueScript;
import com.projetok.domain.entity.GameEntity;
import com.projetok.domain.entity.Mob;
import com.projetok.domain.entity.Npc;
import com.projetok.domain.entity.Position;
import com.projetok.domain.entity.component.HealthComponent;
import com.projetok.domain.entity.component.StaminaComponent;
import com.projetok.domain.entity.component.StatusEffectsComponent;
import com.projetok.domain.item.LootTable;
import com.projetok.domain.movement.CollisionCategory;
import com.projetok.domain.movement.CollisionService;
import com.projetok.domain.movement.SpatialBodyIndex;
import java.util.List;
import org.junit.jupiter.api.Test;
import static com.projetok.domain.skill.LightningStepTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

/** Passagem seletiva e recuperação autoritativa sem alterar os testes sólidos anteriores. */
class LightningStepSelectiveCollisionTest {
    // --- Montagem de comandos e corpos ---

    /** Aponta à direita sem limitar o orçamento de 150 unidades ao cursor. */
    private static SkillCastCommand right() {
        return new SkillCastCommand(LightningStepService.ID, 800f, 100f);
    }

    /** Cria um mob vivo que permite verificar dano e efeitos colaterais. */
    private static Mob mob(float x, float y) {
        var mob = new Mob(200, "Alvo de passagem", new Position(x, y), LootTable.of());
        mob.attach(new HealthComponent(120));
        mob.attach(new StatusEffectsComponent());
        return mob;
    }

    /** Cria um arbusto retangular para a mesma geometria usada pelos sólidos. */
    private static CollisionService.Obstacle brush(float x, float y, float width, float height) {
        return new CollisionService.Obstacle(x, y, width, height, CollisionCategory.SOFT_OBSTACLE);
    }

    /** Confirma a validade normal, inclusive após uma correção de pouso. */
    private static void assertNormalPosition(GameEntity entity, CollisionService.Space space) {
        var p = entity.getPosition();
        assertTrue(new CollisionService().isFree(entity, p.x(), p.y(), RADIUS, space),
                () -> "Posição final precisa estar livre na política normal: " + p);
    }

    // --- Passagem e regras preservadas ---

    /** Atores de classes distintas são atravessados sem deslocamento ou dano. */
    @Test void atravessaMobJogadorENpcSemAlterarOsAtores() {
        var mob = mob(175f, 100f);
        var otherPlayer = player(175f, 100f);
        var npc = new Npc(300, "Vendedor", new Position(175f, 100f), DialogueScript.of("Vendedor"));
        // O NPC de diálogo não precisa ganhar componentes de combate para ser atravessado.
        for (GameEntity actor : List.of(mob, otherPlayer, npc)) {
            var player = player(100f, 100f);
            var service = selectiveService();
            var space = new CollisionService.Space(960f, 640f, List.of(), List.of(
                    new CollisionService.Body(player, RADIUS), new CollisionService.Body(actor, 19f)));
            assertEquals(LightningStepService.Status.STARTED, service.cast(player, right(), RADIUS, space));
            service.update(player, 0.125f, RADIUS, space);
            assertEquals(new Position(175f, 100f), player.getPosition());
            assertTrue(service.active());
            service.update(player, 0.125f, RADIUS, space);
            assertEquals(new Position(250f, 100f), player.getPosition());
            assertEquals(new Position(175f, 100f), actor.getPosition());
            assertEquals(100, player.require(HealthComponent.class).current());
            assertEquals(65f, player.require(StaminaComponent.class).current());
            assertEquals(3.75f, service.cooldown());
            assertFalse(service.active());
            assertFalse(service.awaitingLanding());
            assertNormalPosition(player, space);
        }
        assertEquals(120, mob.require(HealthComponent.class).current());
        assertTrue(mob.require(StatusEffectsComponent.class).snapshot().isEmpty());
        assertEquals(100, otherPlayer.require(HealthComponent.class).current());
    }

    /** Arbustos usam a mesma forma retangular, mas não bloqueiam o avanço seletivo. */
    @Test void atravessaArbustoSemMudarDistanciaDuracaoOuCusto() {
        var player = player(100f, 100f);
        var service = selectiveService();
        var space = new CollisionService.Space(960f, 640f,
                List.of(brush(150f, 80f, 30f, 40f)), List.of());
        assertEquals(LightningStepService.Status.STARTED, service.cast(player, right(), RADIUS, space));
        service.update(player, 0.1f, RADIUS, space);
        assertEquals(160f, player.getPosition().x(), 0.0001f);
        service.update(player, 0.1f, RADIUS, space);
        assertEquals(220f, player.getPosition().x(), 0.0001f);
        service.update(player, 0.2f, RADIUS, space);
        assertEquals(new Position(250f, 100f), player.getPosition());
        assertFalse(service.active());
        assertEquals(0f, service.remaining());
        assertEquals(65f, player.require(StaminaComponent.class).current());
        assertEquals(3.6f, service.cooldown(), 0.00001f);
        assertNormalPosition(player, space);
    }

    /** O construtor antigo continua usando colisão sólida mesmo em obstáculos leves. */
    @Test void construtorAnteriorMantemDashSolidoContraArbustos() {
        var player = player(100f, 100f);
        var service = service();
        var space = new CollisionService.Space(960f, 640f,
                List.of(brush(150f, 80f, 30f, 40f)), List.of());
        service.cast(player, right(), RADIUS, space);
        service.update(player, 0.25f, RADIUS, space);
        assertEquals(new Position(132f, 100f), player.getPosition());
        assertFalse(service.active());
        assertNormalPosition(player, space);
    }

    /** O alcance diagonal continua normalizado e os limites consideram todo o raio. */
    @Test void diagonalSeletivaRespeitaAlcanceELimites() {
        var player = player(100f, 100f);
        var service = selectiveService();
        service.cast(player, new SkillCastCommand(LightningStepService.ID, 700f, 500f),
                RADIUS, emptySpace());
        service.update(player, 0.25f, RADIUS, emptySpace());
        assertEquals(150d, Math.hypot(player.getPosition().x() - 100f,
                player.getPosition().y() - 100f), 0.0002d);

        var borderPlayer = player(920f, 600f);
        var borderService = selectiveService();
        borderService.cast(borderPlayer, new SkillCastCommand(LightningStepService.ID, 959f, 639f),
                RADIUS, emptySpace());
        borderService.update(borderPlayer, 0.25f, RADIUS, emptySpace());
        assertEquals(new Position(942f, 622f), borderPlayer.getPosition());
        assertNormalPosition(borderPlayer, emptySpace());
    }

    // --- Recuperação no percurso efetivamente realizado ---

    /** Um destino dentro de arbusto recua até a tangência mais próxima no percurso. */
    @Test void destinoDentroDeArbustoRecuaAteUltimaPosicaoLivre() {
        var player = player(100f, 100f);
        var service = selectiveService();
        var space = new CollisionService.Space(960f, 640f,
                List.of(brush(240f, 80f, 30f, 40f)), List.of());
        service.cast(player, right(), RADIUS, space);
        service.update(player, 0.25f, RADIUS, space);
        assertEquals(222f, player.getPosition().x(), 0.0001f);
        assertEquals(100f, player.getPosition().y());
        assertFalse(service.active());
        assertFalse(service.awaitingLanding());
        assertNormalPosition(player, space);
    }

    /** A recuperação considera a soma dos raios e não empurra o inimigo. */
    @Test void destinoDentroDeMobRecuaConsiderandoAmbosOsRaios() {
        var player = player(100f, 100f);
        var mob = mob(250f, 100f);
        var space = new CollisionService.Space(960f, 640f, List.of(),
                List.of(new CollisionService.Body(mob, 19f)));
        var service = selectiveService();
        service.cast(player, right(), RADIUS, space);
        service.update(player, 0.25f, RADIUS, space);
        assertEquals(213f, player.getPosition().x(), 0.0001f);
        assertEquals(new Position(250f, 100f), mob.getPosition());
        assertEquals(120, mob.require(HealthComponent.class).current());
        assertNormalPosition(player, space);
    }

    /** Bloqueadores sobrepostos formam uma faixa única sem aceitar um falso espaço livre. */
    @Test void recuoConsideraUniaoDeArbustoEAtor() {
        var player = player(100f, 100f);
        var mob = mob(220f, 100f);
        var space = new CollisionService.Space(960f, 640f,
                List.of(brush(230f, 80f, 40f, 40f)),
                List.of(new CollisionService.Body(mob, 19f)));
        var service = selectiveService();
        service.cast(player, right(), RADIUS, space);
        service.update(player, 0.25f, RADIUS, space);
        assertEquals(183f, player.getPosition().x(), 0.0001f);
        assertNormalPosition(player, space);
    }

    /** A perna Y ocupada é desfeita antes de procurar uma saída na perna X real. */
    @Test void recuoSegueQuinaDoPercursoEmVezDaCordaDiagonal() {
        var player = player(100f, 100f);
        var service = selectiveService();
        var space = new CollisionService.Space(960f, 640f,
                List.of(brush(185f, 90f, 45f, 140f)), List.of());
        service.cast(player, new SkillCastCommand(LightningStepService.ID, 400f, 400f), RADIUS, space);
        service.update(player, 0.25f, RADIUS, space);
        // A corda origem/destino retornaria uma posição diagonal que não foi percorrida.
        assertEquals(167f, player.getPosition().x(), 0.0001f);
        assertEquals(100f, player.getPosition().y());
        assertFalse(service.awaitingLanding());
        assertNormalPosition(player, space);
    }

    /** A posição atual do mob, e não a posição de lançamento, decide o pouso e a grade. */
    @Test void corpoMovelEIndiceEspacialSaoAtualizadosAposRecuo() {
        var player = player(100f, 100f);
        var mob = mob(400f, 100f);
        var playerBody = new CollisionService.Body(player, RADIUS);
        var bodies = List.of(playerBody, new CollisionService.Body(mob, 19f));
        var index = new SpatialBodyIndex(bodies, 16f);
        var space = new CollisionService.Space(960f, 640f, List.of(), bodies, index);
        var service = selectiveService();
        service.cast(player, right(), RADIUS, space);
        service.update(player, 0.1f, RADIUS, space);
        mob.setPosition(new Position(250f, 100f));
        index.update(mob);
        service.update(player, 0.15f, RADIUS, space);
        assertEquals(213f, player.getPosition().x(), 0.0001f);
        assertTrue(index.query(213d, 100d, 213d, 100d).stream().anyMatch(body -> body == playerBody));
        assertFalse(index.query(250d, 100d, 250d, 100d).stream().anyMatch(body -> body == playerBody));
        assertNormalPosition(player, space);
    }

    /** Sem saída possível, o relógio termina e a recuperação aguarda sem outro gasto. */
    @Test void todaRotaOcupadaAguardaLiberacaoSemEstenderDash() {
        var player = player(100f, 100f);
        var mob = mob(400f, 100f);
        var bodies = List.of(new CollisionService.Body(player, RADIUS),
                new CollisionService.Body(mob, 100f));
        var index = new SpatialBodyIndex(bodies, 16f);
        var space = new CollisionService.Space(960f, 640f, List.of(), bodies, index);
        var service = selectiveService();
        service.cast(player, right(), RADIUS, space);
        service.update(player, 0.1f, RADIUS, space);
        mob.setPosition(new Position(175f, 100f));
        index.update(mob);
        service.update(player, 0.15f, RADIUS, space);
        assertEquals(new Position(250f, 100f), player.getPosition());
        assertFalse(service.active());
        assertEquals(0f, service.remaining());
        assertTrue(service.awaitingLanding());
        service.update(player, 0.25f, RADIUS, space);
        assertEquals(new Position(250f, 100f), player.getPosition());
        assertEquals(3.5f, service.cooldown(), 0.00001f);
        assertEquals(65f, player.require(StaminaComponent.class).current());
        assertNotEquals(LightningStepService.Status.STARTED, service.cast(player, right(), RADIUS, space));

        mob.setPosition(new Position(400f, 100f));
        index.update(mob);
        service.update(player, 0.01f, RADIUS, space);
        assertFalse(service.awaitingLanding());
        assertFalse(service.active());
        assertEquals(new Position(250f, 100f), player.getPosition());
        assertEquals(65f, player.require(StaminaComponent.class).current());
        assertNormalPosition(player, space);
    }

    /** Um alvo morto na posição final não exige recuo nem recebe novos efeitos. */
    @Test void corpoMortoNaoBloqueiaPouso() {
        var player = player(100f, 100f);
        var mob = mob(250f, 100f);
        mob.require(HealthComponent.class).applyDamage(120);
        var space = new CollisionService.Space(960f, 640f, List.of(),
                List.of(new CollisionService.Body(mob, 19f)));
        var service = selectiveService();
        service.cast(player, right(), RADIUS, space);
        service.update(player, 0.25f, RADIUS, space);
        assertEquals(new Position(250f, 100f), player.getPosition());
        assertTrue(mob.require(StatusEffectsComponent.class).snapshot().isEmpty());
        assertFalse(service.awaitingLanding());
        assertNormalPosition(player, space);
    }

    @Test void devePreservarBloqueioDeParedeNoDashSeletivo(){
            var jogador = player(100f, 100f);
            var servico = selectiveService();

            // Parede sólida
            var parede = new CollisionService.Obstacle(
                200f, 60f, 20f, 100f
            );

            var espaco = new CollisionService.Space(
                960f,
                640f,
                List.of(parede),
                List.of()
            );
            var resultado = servico.cast(
                jogador,
                new SkillCastCommand(LightningStepService.ID, 300f, 100f),
                RADIUS,
                espaco
            );

            servico.update(jogador, 0.25f, RADIUS, espaco);

            // O personagem avançou?
            assertTrue(jogador.getPosition().x() > 100f);

            // O personagem parou antes da parede?
            assertTrue(jogador.getPosition().x() <= 182f);

            // O dash foi iniciado corretamente?
            assertEquals(LightningStepService.Status.STARTED, resultado);
    }
}
