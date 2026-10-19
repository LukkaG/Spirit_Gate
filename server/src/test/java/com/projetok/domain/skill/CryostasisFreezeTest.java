package com.projetok.domain.skill;

import com.projetok.domain.combat.CombatService;
import com.projetok.domain.effect.StatusEffectService;
import com.projetok.domain.effect.StatusEffectType;
import com.projetok.domain.entity.Mob;
import com.projetok.domain.entity.Player;
import com.projetok.domain.entity.Position;
import com.projetok.domain.entity.component.HealthComponent;
import com.projetok.domain.entity.component.RaceComponent;
import com.projetok.domain.entity.component.StaminaComponent;
import com.projetok.domain.entity.component.StatusEffectsComponent;
import com.projetok.domain.event.GameEventBus;
import com.projetok.domain.item.LootTable;
import com.projetok.domain.network.PlayerConnection;
import com.projetok.domain.probability.ProbabilityService;
import com.projetok.domain.race.DamageType;
import com.projetok.domain.race.Race;
import com.projetok.domain.race.RaceRegistry;
import com.projetok.domain.resource.StaminaService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

/** Regressões do Freeze aplicado por dano real, com sorteios inteiramente controlados. */
class CryostasisFreezeTest {
    private final StatusEffectService effects = new StatusEffectService();
    private final Player player = player();

    // --- Fixtures sem interface e sem adaptações do caminho de dano ---

    /** Cria um lançador mínimo com o mesmo recurso consumido na arena. */
    private static Player player() {
        Player player = new Player(1, "Lançador", new Position(100, 100), new PlayerConnection() {
            @Override public void send(Object packet) { }
            @Override public void disconnect() { }
        });
        player.attach(new HealthComponent(100));
        player.attach(new StaminaComponent());
        return player;
    }

    /** Cria um alvo neutro a gelo; a vida permite observar os cinco pulsos. */
    private static Mob mob(int id, int health) {
        Mob mob = new Mob(id, "Alvo " + id, new Position(200, 200), LootTable.of());
        mob.attach(new HealthComponent(health));
        mob.attach(new RaceComponent(RaceRegistry.withDefaults().get("demon")));
        return mob;
    }

    /** Injeta uma sequência que falha imediatamente se houver sorteios inesperados. */
    private CryostasisService service(Sequence random) {
        return new CryostasisService(new CombatService(new GameEventBus()), new StaminaService(),
                effects, new ProbabilityService(random));
    }

    /** Converte mobs em hitboxes iguais às usadas no protótipo. */
    private static List<CryostasisService.Target> targets(Mob... mobs) {
        return java.util.Arrays.stream(mobs).map(mob -> new CryostasisService.Target(mob, 19)).toList();
    }

    /** Executa o pulso imediato pelo contrato público da habilidade. */
    private CryostasisService.Result cast(CryostasisService service, List<CryostasisService.Target> targets) {
        return service.cast(player, new SkillCastCommand(CryostasisService.ID, 200, 200), 960, 640, targets);
    }

    /** Consulta efeitos ausentes como duração zero, sem exigir componente prévio na fixture. */
    private static float remaining(Mob mob) {
        return mob.get(StatusEffectsComponent.class)
                .map(component -> component.remaining(StatusEffectType.FREEZE)).orElse(0f);
    }

    /** Gerador padrão injetável com amostras predeterminadas e contagem de consumo. */
    private static final class Sequence implements RandomGenerator {
        private final double[] samples;
        private int count;

        /** Copia as amostras para impedir alteração posterior do roteiro do teste. */
        private Sequence(double... samples) { this.samples = samples.clone(); }

        /** O serviço deve usar nextDouble; outra distribuição denunciaria regressão. */
        @Override public long nextLong() { throw new AssertionError("Sorteio inesperado por nextLong"); }

        /** Retorna exatamente uma amostra por tentativa elegível. */
        @Override public double nextDouble() {
            if (count >= samples.length) throw new AssertionError("Sorteio além da sequência prevista");
            return samples[count++];
        }
    }

    // --- Elegibilidade, independência e limite estrito da probabilidade ---

    /** A mesma chance usa comparador estrito: 0,29 passa e 0,30 falha. */
    @Test void limiarDeTrintaPorCentoEhEstritoPorMob() {
        Mob first = mob(10, 120), second = mob(11, 120);
        Sequence random = new Sequence(0.29, 0.30);
        CryostasisService service = service(random);
        var result = cast(service, targets(first, second));

        assertEquals(CryostasisService.Status.CAST, result.status());
        assertEquals(2, result.hits().size());
        assertEquals(2, random.count);
        assertEquals(1f, remaining(first));
        assertEquals(0f, remaining(second));
        assertEquals(113, first.require(HealthComponent.class).current());
        assertEquals(113, second.require(HealthComponent.class).current());
        assertEquals(45f, player.require(StaminaComponent.class).current());
        assertEquals(3f, service.cooldown());
    }

    /** Cada mob recebe outro sorteio no pulso seguinte, sem compartilhar sucesso. */
    @Test void sorteiosSaoIndependentesPorMobEPorPulso() {
        Mob first = mob(10, 120), second = mob(11, 120);
        Sequence random = new Sequence(0.29, 0.80, 0.80, 0.29);
        CryostasisService service = service(random);
        var targets = targets(first, second);

        cast(service, targets);
        effects.update(first, 0.5f);
        effects.update(second, 0.5f);
        assertEquals(2, service.update(0.5f, targets).size());

        assertEquals(4, random.count);
        assertEquals(0.5f, remaining(first));
        assertEquals(1f, remaining(second));
        assertEquals(106, first.require(HealthComponent.class).current());
        assertEquals(106, second.require(HealthComponent.class).current());
    }

    /** Uma vítima duplicada na lista continua elegível uma única vez por pulso. */
    @Test void alvoDuplicadoEForaDaZonaNaoGeramSorteiosExtras() {
        Mob inside = mob(10, 120), outside = mob(11, 120);
        outside.setPosition(new Position(500, 500));
        Sequence random = new Sequence(0.29);
        var insideTarget = new CryostasisService.Target(inside, 19);
        var result = cast(service(random), List.of(insideTarget, insideTarget,
                new CryostasisService.Target(outside, 19)));

        assertEquals(1, result.hits().size());
        assertEquals(1, random.count);
        assertEquals(113, inside.require(HealthComponent.class).current());
        assertEquals(120, outside.require(HealthComponent.class).current());
        assertEquals(0f, remaining(outside));
    }

    /** Mortos e danos letais não abrem tentativas nem recebem novos efeitos. */
    @Test void mortosEDanoLetalNaoSorteiamNemAplicamFreeze() {
        Mob dead = mob(10, 120), dying = mob(11, 7);
        dead.require(HealthComponent.class).applyDamage(120);
        Sequence random = new Sequence();
        CryostasisService service = service(random);
        var targets = targets(dead, dying);

        var result = cast(service, targets);
        assertEquals(1, result.hits().size());
        assertEquals(7, result.hits().get(0).damage());
        assertTrue(dying.isDead());
        assertEquals(0f, remaining(dead));
        assertEquals(0f, remaining(dying));
        assertTrue(service.update(2.5f, targets).isEmpty());
        assertEquals(0, random.count);
    }

    /** Afinidade de gelo zero continua dano zero e não oferece controle gratuito. */
    @Test void danoZeroNaoSorteiaNemAplicaFreeze() {
        Mob immune = mob(10, 120);
        Race base = immune.race();
        immune.attach(new RaceComponent(new Race("gelo-zero", "Alvo sem dano de gelo", base.hpGrowth(),
                base.growthBonus(), Map.of(DamageType.ICE, 0.0), List.of())));
        Sequence random = new Sequence();
        CryostasisService service = service(random);
        var targets = targets(immune);

        assertEquals(0, cast(service, targets).hits().get(0).damage());
        assertEquals(4, service.update(2.5f, targets).size());
        assertEquals(120, immune.require(HealthComponent.class).current());
        assertEquals(0f, remaining(immune));
        assertEquals(0, random.count);
    }

    // --- Relógio do efeito separado do relógio da habilidade ---

    /** Novo sucesso renova um segundo; uma falha posterior preserva o tempo restante. */
    @Test void sucessoRenovaSemAcumularEFalhaNaoAlteraDuracao() {
        Mob mob = mob(10, 120);
        Sequence random = new Sequence(0.29, 0.29, 0.30);
        CryostasisService service = service(random);
        var targets = targets(mob);

        cast(service, targets);
        effects.update(mob, 0.5f);
        assertEquals(0.5f, remaining(mob));
        service.update(0.5f, targets);
        assertEquals(1f, remaining(mob));
        effects.update(mob, 0.5f);
        service.update(0.5f, targets);
        assertEquals(0.5f, remaining(mob));
        effects.update(mob, 0.5f);

        assertEquals(0f, remaining(mob));
        assertFalse(mob.require(StatusEffectsComponent.class).has(StatusEffectType.FREEZE));
        assertEquals(3, random.count);
        assertEquals(99, mob.require(HealthComponent.class).current());
    }

    /** Cinco sucessos não acrescentam dano e a expiração da zona não cria sexto sorteio. */
    @Test void cincoPulsosNormaisSemDanoExtraNemSextaTentativa() {
        Mob mob = mob(10, 120);
        Sequence random = new Sequence(0, 0, 0, 0, 0);
        CryostasisService service = service(random);
        var targets = targets(mob);
        int hits = cast(service, targets).hits().size();

        for (int i = 0; i < 4; i++) {
            effects.update(mob, 0.5f);
            var pulse = service.update(0.5f, targets);
            assertEquals(1, pulse.size());
            assertEquals(7, pulse.get(0).damage());
            assertEquals(1f, remaining(mob));
            hits += pulse.size();
        }
        effects.update(mob, 0.5f);
        assertTrue(service.update(0.5f, targets).isEmpty());
        assertTrue(service.zones().isEmpty());
        assertEquals(0.5f, remaining(mob));
        effects.update(mob, 0.5f);
        assertTrue(service.update(10f, targets).isEmpty());

        assertEquals(5, hits);
        assertEquals(5, random.count);
        assertEquals(85, mob.require(HealthComponent.class).current());
        assertEquals(0f, remaining(mob));
    }
}
