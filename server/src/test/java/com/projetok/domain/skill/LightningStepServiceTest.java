package com.projetok.domain.skill;

import com.projetok.domain.combat.CombatService;
import com.projetok.domain.entity.Mob;
import com.projetok.domain.entity.Position;
import com.projetok.domain.entity.component.HealthComponent;
import com.projetok.domain.entity.component.StaminaComponent;
import com.projetok.domain.entity.component.StatusEffectsComponent;
import com.projetok.domain.event.GameEventBus;
import com.projetok.domain.item.LootTable;
import com.projetok.domain.movement.CollisionService;
import java.util.List;
import org.junit.jupiter.api.Test;
import static com.projetok.domain.skill.LightningStepTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

/** Regras e colisões do dash executadas no domínio, sem abrir uma janela. */
class LightningStepServiceTest {
    /** Monta um comando válido que aponta à direita da origem usada nos testes. */
    private static SkillCastCommand right() {
        return new SkillCastCommand(LightningStepService.ID, 800f, 100f);
    }

    /** A ativação gasta uma vez, fixa direção e não teleporta. */
    @Test void inicioConsomeUmaVezSemMover() {
        var player = player(100f, 100f);
        var service = service();
        assertEquals(100, player.require(HealthComponent.class).current());
        assertEquals(100, player.require(StaminaComponent.class).current());
        assertEquals(LightningStepService.Status.STARTED, service.cast(player, right(), RADIUS, emptySpace()));
        assertEquals(65, player.require(StaminaComponent.class).current());
        assertEquals(new Position(100f, 100f), player.getPosition());
        assertEquals(new Position(1f, 0f), service.direction());
        assertTrue(service.active());
        assertEquals(0.25f, service.remaining());
        assertEquals(4f, service.cooldown());
        assertEquals(LightningStepService.Status.COOLDOWN, service.cast(player, right(), RADIUS, emptySpace()));
        assertEquals(65, player.require(StaminaComponent.class).current());
    }

    /** A 30 Hz, sete passos inteiros e um fracionário somam 150 unidades. */
    @Test void progressoSemTeleporteComUltimoTickFracionario() {
        var player = player(100f, 100f);
        var service = service();
        var space = emptySpace();
        service.cast(player, right(), RADIUS, space);
        for (int tick = 1; tick <= 7; tick++) {
            var step = service.update(player, 1f / 30f, RADIUS, space);
            assertTrue(step.moved());
            assertEquals(20f, step.to().x() - step.from().x(), 0.0001);
            assertEquals(100f + tick * 20f, player.getPosition().x(), 0.0001);
            assertTrue(service.active());
        }
        var last = service.update(player, 1f / 30f, RADIUS, space);
        assertEquals(10f, last.to().x() - last.from().x(), 0.0001);
        assertEquals(new Position(250f, 100f), player.getPosition());
        assertFalse(service.active());
        assertEquals(0f, service.remaining());
        assertFalse(service.update(player, 1f / 30f, RADIUS, space).moved());
        assertEquals(65, player.require(StaminaComponent.class).current());
    }

    /** Deltas arbitrários preservam velocidade, duração e fração final exata. */
    @Test void duracaoDeUmQuartoDeSegundo() {
        var player = player(100f, 100f);
        var service = service();
        var space = emptySpace();
        service.cast(player, right(), RADIUS, space);
        service.update(player, 0.1f, RADIUS, space);
        assertEquals(160f, player.getPosition().x(), 0.0001);
        service.update(player, 0.1f, RADIUS, space);
        service.update(player, 0.049f, RADIUS, space);
        assertEquals(249.4f, player.getPosition().x(), 0.0001);
        assertTrue(service.active());
        service.update(player, 0.02f, RADIUS, space);
        assertEquals(250f, player.getPosition().x(), 0.0001);
        assertFalse(service.active());
    }

    /** Um delta maior que a duração não amplia o orçamento de deslocamento. */
    @Test void deltaGrandeNuncaExcedeDistanciaMaxima() {
        var player = player(100f, 100f);
        var service = service();
        var space = emptySpace();
        service.cast(player, right(), RADIUS, space);
        service.update(player, 10f, RADIUS, space);
        assertEquals(new Position(250f, 100f), player.getPosition());
        assertFalse(service.active());
        assertEquals(0f, service.cooldown());
    }

    /** Direção diagonal normalizada mantém o mesmo alcance do deslocamento horizontal. */
    @Test void diagonalMantemDistanciaSemGanharVelocidade() {
        var player = player(100f, 100f);
        var service = service();
        var space = emptySpace();
        service.cast(player, new SkillCastCommand(LightningStepService.ID, 700f, 500f), RADIUS, space);
        for (int i = 0; i < 8; i++) service.update(player, 1f / 30f, RADIUS, space);
        assertEquals(150f, Math.hypot(player.getPosition().x() - 100f,
                player.getPosition().y() - 100f), 0.0002);
        assertEquals(2d / 3d, service.direction().y() / service.direction().x(), 0.000001);
        assertFalse(service.active());
    }

    /** O cursor próximo define a direção, sem transformar o dash em movimento curto. */
    @Test void cursorProximoDefineDirecaoEAlcanceCompleto() {
        var player = player(100f, 100f);
        var service = service();
        var space = emptySpace();
        service.cast(player, new SkillCastCommand(LightningStepService.ID, 101f, 100f), RADIUS, space);
        service.update(player, 0.25f, RADIUS, space);
        assertEquals(new Position(250f, 100f), player.getPosition());
    }

    /** Cooldown só libera depois de quatro segundos e não depende do fim do dash. */
    @Test void cooldownBloqueiaEAceitaDepoisDeQuatroSegundos() {
        var player = player(100f, 100f);
        var service = service();
        var space = emptySpace();
        service.cast(player, right(), RADIUS, space);
        service.update(player, 0.25f, RADIUS, space);
        assertEquals(3.75f, service.cooldown());
        service.update(player, 3.74f, RADIUS, space);
        assertEquals(LightningStepService.Status.COOLDOWN, service.cast(player, right(), RADIUS, space));
        assertEquals(65f, player.require(StaminaComponent.class).current());
        service.update(player, 0.01f, RADIUS, space);
        assertEquals(0f, service.cooldown());
        assertEquals(LightningStepService.Status.STARTED, service.cast(player, right(), RADIUS, space));
        assertEquals(30f, player.require(StaminaComponent.class).current());
    }

    /** Não há consumo parcial nem cooldown quando o saldo é insuficiente. */
    @Test void vigorInsuficienteNaoMudaEstado() {
        var player = player(100f, 100f);
        var service = service();
        player.require(StaminaComponent.class).tryConsume(66f);
        assertEquals(LightningStepService.Status.STAMINA, service.cast(player, right(), RADIUS, emptySpace()));
        assertEquals(34f, player.require(StaminaComponent.class).current());
        assertEquals(new Position(100f, 100f), player.getPosition());
        assertEquals(0f, service.cooldown());
        assertFalse(service.active());
    }

    /** Saldo exatamente igual ao custo é suficiente. */
    @Test void saldoExatoPodeSerConsumido() {
        var player = player(100f, 100f);
        var service = service();
        player.require(StaminaComponent.class).tryConsume(65f);
        assertEquals(LightningStepService.Status.STARTED, service.cast(player, right(), RADIUS, emptySpace()));
        assertEquals(0f, player.require(StaminaComponent.class).current());
    }

    /** Mortos não iniciam a habilidade, mesmo com vigor disponível. */
    @Test void jogadorMortoNaoInicia() {
        var player = player(100f, 100f);
        var service = service();
        player.require(HealthComponent.class).applyDamage(100);
        assertEquals(LightningStepService.Status.DEAD, service.cast(player, right(), RADIUS, emptySpace()));
        assertEquals(100f, player.require(StaminaComponent.class).current());
        assertEquals(0f, service.cooldown());
        assertFalse(service.active());
    }

    /** Morte durante a habilidade cancela movimento, mantendo a passagem do cooldown. */
    @Test void morteInterrompeDashAtivo() {
        var player = player(100f, 100f);
        var service = service();
        service.cast(player, right(), RADIUS, emptySpace());
        player.require(HealthComponent.class).applyDamage(100);
        assertFalse(service.update(player, 0.1f, RADIUS, emptySpace()).moved());
        assertEquals(new Position(100f, 100f), player.getPosition());
        assertFalse(service.active());
        assertEquals(3.9f, service.cooldown(), 0.000001);
    }

    /** Valores externos inválidos e comandos desconhecidos não iniciam estado. */
    @Test void comandosInvalidosNaoConsomemNemIniciamCooldown() {
        var player = player(100f, 100f);
        var service = service();
        var space = emptySpace();
        for (var command : List.of(new SkillCastCommand("outra", 200f, 200f),
                new SkillCastCommand(null, 200f, 200f),
                new SkillCastCommand(LightningStepService.ID, Float.NaN, 200f),
                new SkillCastCommand(LightningStepService.ID, 200f, Float.NaN),
                new SkillCastCommand(LightningStepService.ID, Float.POSITIVE_INFINITY, 200f),
                new SkillCastCommand(LightningStepService.ID, 200f, Float.NEGATIVE_INFINITY),
                new SkillCastCommand(LightningStepService.ID, -1f, 200f),
                new SkillCastCommand(LightningStepService.ID, 200f, -1f),
                new SkillCastCommand(LightningStepService.ID, 960f, 200f),
                new SkillCastCommand(LightningStepService.ID, 200f, 640f))) {
            assertEquals(LightningStepService.Status.INVALID, service.cast(player, command, RADIUS, space));
            assertEquals(100f, player.require(StaminaComponent.class).current());
            assertEquals(0f, service.cooldown());
            assertFalse(service.active());
            assertEquals(new Position(100f, 100f), player.getPosition());
        }
        assertEquals(LightningStepService.Status.INVALID, service.cast(player, null, RADIUS, space));
        assertEquals(LightningStepService.Status.INVALID, service.cast(player, right(), RADIUS, null));
        assertEquals(LightningStepService.Status.INVALID, service.cast(null, right(), RADIUS, space));
    }

    /** Geometria inicial inválida é rejeitada antes de consumir o recurso. */
    @Test void geometriaInvalidaRecusaLancamento() {
        var player = player(100f, 100f);
        var service = service();
        for (float radius : new float[] {0f, -1f, Float.NaN, Float.POSITIVE_INFINITY}) {
            assertEquals(LightningStepService.Status.INVALID, service.cast(player, right(), radius, emptySpace()));
            assertEquals(100f, player.require(StaminaComponent.class).current());
        }
        player.setPosition(new Position(Float.NaN, 100f));
        assertEquals(LightningStepService.Status.INVALID, service.cast(player, right(), RADIUS, emptySpace()));
        player.setPosition(new Position(5f, 100f));
        assertEquals(LightningStepService.Status.INVALID, service.cast(player, right(), RADIUS, emptySpace()));
        player.setPosition(new Position(100f, 100f));
        var blocked = new CollisionService.Space(960f, 640f,
                List.of(new CollisionService.Obstacle(90f, 90f, 20f, 20f)), List.of());
        assertEquals(LightningStepService.Status.INVALID, service.cast(player, right(), RADIUS, blocked));
        assertEquals(100f, player.require(StaminaComponent.class).current());
        assertEquals(0f, service.cooldown());
    }

    /** Delta inválido não altera relógios, posição ou recurso; zero é inerte. */
    @Test void deltaInvalidoNaoMudaEstadoEZeroEhPermitido() {
        var player = player(100f, 100f);
        var service = service();
        var space = emptySpace();
        service.cast(player, right(), RADIUS, space);
        for (float dt : new float[] {-1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> service.update(player, dt, RADIUS, space));
            assertEquals(new Position(100f, 100f), player.getPosition());
            assertEquals(4f, service.cooldown());
            assertEquals(0.25f, service.remaining());
            assertEquals(65f, player.require(StaminaComponent.class).current());
        }
        assertFalse(service.update(player, 0f, RADIUS, space).moved());
        assertEquals(4f, service.cooldown());
        assertEquals(0.25f, service.remaining());
    }

    /** Parede fina detém a varredura e o tempo bloqueado não acumula distância futura. */
    @Test void paredeFinaImpedeTunelamentoESemCompensacao() {
        var player = player(100f, 100f);
        var service = service();
        var space = new CollisionService.Space(960f, 640f,
                List.of(new CollisionService.Obstacle(140f, 0f, 2f, 200f)), List.of());
        assertEquals(LightningStepService.Status.STARTED, service.cast(player, right(), RADIUS, space));
        service.update(player, 0.1f, RADIUS, space);
        assertEquals(122f, player.getPosition().x(), 0.0001);
        service.update(player, 0.15f, RADIUS, space);
        assertEquals(122f, player.getPosition().x(), 0.0001);
        assertFalse(service.active());
        assertFalse(service.update(player, 0.1f, RADIUS, emptySpace()).moved());
    }

    /** Um eixo bloqueado preserva o deslocamento do eixo livre na direção fixada. */
    @Test void colisaoPreservaDeslizamentoPorEixos() {
        var player = player(100f, 100f);
        var service = service();
        var space = new CollisionService.Space(960f, 640f,
                List.of(new CollisionService.Obstacle(140f, 0f, 2f, 640f)), List.of());
        service.cast(player, new SkillCastCommand(LightningStepService.ID, 400f, 300f), RADIUS, space);
        service.update(player, 0.25f, RADIUS, space);
        assertEquals(122f, player.getPosition().x(), 0.0001);
        assertEquals(100f + 150f * service.direction().y(), player.getPosition().y(), 0.0001);
        assertTrue(new CollisionService().isFree(player, player.getPosition().x(),
                player.getPosition().y(), RADIUS, space));
    }

    /** A varredura respeita a borda da arena considerando todo o raio do personagem. */
    @Test void dashNaoUltrapassaLimitesDaArena() {
        var player = player(920f, 600f);
        var service = service();
        var space = emptySpace();
        service.cast(player, new SkillCastCommand(LightningStepService.ID, 959f, 639f), RADIUS, space);
        service.update(player, 0.25f, RADIUS, space);
        assertEquals(new Position(942f, 622f), player.getPosition());
        assertTrue(new CollisionService().isFree(player, 942f, 622f, RADIUS, space));
    }

    /** Um mob vivo bloqueia sem receber dano, status ou deslocamento do dash. */
    @Test void mobBloqueiaSemReceberDanoOuEfeito() {
        var player = player(100f, 100f);
        var mob = new Mob(100, "Bloqueio", new Position(160f, 100f), LootTable.of());
        mob.attach(new HealthComponent(120));
        mob.attach(new StatusEffectsComponent());
        var space = new CollisionService.Space(960f, 640f, List.of(), List.of(
                new CollisionService.Body(player, RADIUS), new CollisionService.Body(mob, 19f)));
        var service = service();
        service.cast(player, right(), RADIUS, space);
        service.update(player, 0.25f, RADIUS, space);
        assertEquals(123f, player.getPosition().x(), 0.0001);
        assertEquals(new Position(160f, 100f), mob.getPosition());
        assertEquals(120, mob.require(HealthComponent.class).current());
        assertTrue(mob.require(StatusEffectsComponent.class).snapshot().isEmpty());
        assertEquals(100, player.require(HealthComponent.class).current());
        assertFalse(service.active());
    }

    /** A habilidade não impede CombatService de causar dano ao jogador durante o dash. */
    @Test void jogadorContinuaRecebendoDanoDuranteDash() {
        var player = player(100f, 100f);
        var attacker = player(200f, 100f);
        var service = service();
        service.cast(player, right(), RADIUS, emptySpace());
        int before = player.require(HealthComponent.class).current();
        int damage = new CombatService(new GameEventBus()).attack(attacker, player);
        assertTrue(service.active());
        assertTrue(damage > 0);
        assertEquals(before - damage, player.require(HealthComponent.class).current());
        assertTrue(service.update(player, 0.05f, RADIUS, emptySpace()).moved());
    }
}
