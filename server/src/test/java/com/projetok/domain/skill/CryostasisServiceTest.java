package com.projetok.domain.skill;

import com.projetok.domain.combat.CombatService;
import com.projetok.domain.entity.Mob;
import com.projetok.domain.entity.Player;
import com.projetok.domain.entity.Position;
import com.projetok.domain.entity.component.HealthComponent;
import com.projetok.domain.entity.component.StaminaComponent;
import com.projetok.domain.entity.component.RaceComponent;
import com.projetok.domain.event.GameEventBus;
import com.projetok.domain.event.events.EntityDeathEvent;
import com.projetok.domain.item.LootTable;
import com.projetok.domain.network.PlayerConnection;
import com.projetok.domain.race.DamageType;
import com.projetok.domain.race.RaceRegistry;
import com.projetok.domain.resource.StaminaService;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

/** Regras da Criostase sem protótipo, interface ou banco de dados. */
class CryostasisServiceTest {
    private final GameEventBus events = new GameEventBus();
    private final CryostasisService service = new CryostasisService(new CombatService(events), new StaminaService());
    private final Player player = createPlayer();
    private final Mob mob = createMob(120, 200, 200);
    private final List<CryostasisService.Target> targets = List.of(new CryostasisService.Target(mob, 19));

    /** Monta somente os componentes exigidos pela habilidade. */
    private Player createPlayer() {
        var p = new Player(1, "Teste", new Position(100, 100), new PlayerConnection() {
            @Override public void send(Object packet) { }
            @Override public void disconnect() { }
        });
        p.attach(new HealthComponent(100)); p.attach(new StaminaComponent());
        return p;
    }

    /** Alvo mínimo: vida e raça, sem adaptadores de teste para receber dano. */
    private Mob createMob(int hp, float x, float y) {
        var m = new Mob(100, "Alvo", new Position(x, y), LootTable.of());
        m.attach(new HealthComponent(hp));
        m.attach(new RaceComponent(RaceRegistry.withDefaults().get("demon")));
        return m;
    }

    /** Executa pelo mesmo contrato de intenção usado pela interface. */
    private CryostasisService.Result cast(float x, float y) {
        return service.cast(player, new SkillCastCommand("cryostasis", x, y), 960, 640, targets);
    }

    /** Lançamento inicia cooldown, consome exatamente 55 e causa dano imediato neutro. */
    @Test void lancamentoValido() {
        var result = cast(200, 200);
        assertEquals(CryostasisService.Status.CAST, result.status());
        assertEquals(45, player.require(StaminaComponent.class).current());
        assertEquals(3, service.cooldown());
        assertEquals(113, mob.require(HealthComponent.class).current());
        assertEquals(7, result.hits().get(0).damage());
        assertEquals(1, service.zones().size());
    }

    /** Recurso insuficiente não cria zona ou cooldown nem altera saldo. */
    @Test void vigorInsuficiente() {
        new StaminaService().tryConsume(player, 50);
        assertEquals(CryostasisService.Status.STAMINA, cast(200, 200).status());
        assertEquals(50, player.require(StaminaComponent.class).current());
        assertEquals(0, service.cooldown()); assertTrue(service.zones().isEmpty());
    }

    /** Após três segundos pode lançar de novo, se houver recurso. */
    @Test void cooldownBloqueiaELibera() {
        cast(200, 200);
        assertEquals(CryostasisService.Status.COOLDOWN, cast(200, 200).status());
        assertEquals(45, player.require(StaminaComponent.class).current());
        service.update(2.99f, targets);
        assertEquals(CryostasisService.Status.COOLDOWN, cast(200, 200).status());
        service.update(0.01f, targets);
        player.require(StaminaComponent.class).restore(55);
        assertEquals(CryostasisService.Status.CAST, cast(200, 200).status());
    }

    /** Pontos no limite são preservados; pontos além são limitados sem recusa. */
    @Test void clampEAlcanceExato() {
        assertEquals(CryostasisService.Status.CAST, cast(340, 100).status());
        assertEquals(340, service.zones().get(0).x());
        service.update(3, targets); player.require(StaminaComponent.class).restore(100);
        assertEquals(CryostasisService.Status.CAST, cast(900, 600).status());
        var z = service.zones().get(0);
        assertEquals(240, Math.hypot(z.x() - 100, z.y() - 100), 0.0001);
        assertEquals(500.0 / 800, (z.y() - 100) / (z.x() - 100), 0.00001);
    }

    /** Entrada inválida nunca altera saldo, cooldown ou zonas. */
    @Test void rejeitaComandosInvalidos() {
        for (var command : List.of(new SkillCastCommand("outra", 200, 200),
                new SkillCastCommand(null, 200, 200), new SkillCastCommand("cryostasis", Float.NaN, 200),
                new SkillCastCommand("cryostasis", 200, Float.POSITIVE_INFINITY),
                new SkillCastCommand("cryostasis", Float.NEGATIVE_INFINITY, 200),
                new SkillCastCommand("cryostasis", -1, 0), new SkillCastCommand("cryostasis", 0, -1),
                new SkillCastCommand("cryostasis", 960, 0), new SkillCastCommand("cryostasis", 0, 640))) {
            assertEquals(CryostasisService.Status.INVALID, service.cast(player, command, 960, 640, targets).status());
            assertEquals(100, player.require(StaminaComponent.class).current());
            assertEquals(0, service.cooldown()); assertTrue(service.zones().isEmpty());
        }
        assertEquals(CryostasisService.Status.INVALID, service.cast(player, null, 960, 640, targets).status());
    }

    /** Morte impede criação e gasto, inclusive com comando válido. */
    @Test void jogadorMortoNaoLanca() {
        player.require(HealthComponent.class).applyDamage(100);
        assertEquals(CryostasisService.Status.DEAD, cast(200, 200).status());
        assertEquals(100, player.require(StaminaComponent.class).current());
        assertEquals(0, service.cooldown());
    }

    /** Exatamente cinco pulsos em 0, 0,5, 1, 1,5 e 2; nenhum na expiração. */
    @Test void cincoPulsosSemSexto() {
        cast(200, 200);
        assertEquals(113, mob.require(HealthComponent.class).current());
        assertTrue(service.update(0.49f, targets).isEmpty());
        assertEquals(1, service.update(0.01f, targets).size());
        assertEquals(106, mob.require(HealthComponent.class).current());
        for (int i = 0; i < 3; i++) assertEquals(1, service.update(0.5f, targets).size());
        assertEquals(85, mob.require(HealthComponent.class).current());
        assertEquals(1, service.zones().size());
        assertTrue(service.update(0.5f, targets).isEmpty());
        assertTrue(service.zones().isEmpty());
        assertTrue(service.update(10, targets).isEmpty());
        assertEquals(85, mob.require(HealthComponent.class).current());
    }

    /** Deltas de 30 Hz preservam os mesmos cinco pulsos e a expiração. */
    @Test void pulsosComDeltaDaArena() {
        int hits = cast(200, 200).hits().size();
        for (int i = 0; i < 75; i++) hits += service.update(1f / 30f, targets).size();
        assertEquals(5, hits);
        assertEquals(85, mob.require(HealthComponent.class).current());
        assertTrue(service.zones().isEmpty());
    }

    /** Passo que cruza vários instantes processa cada pulso devido exatamente uma vez. */
    @Test void passoGrandeEDeltasInvalidos() {
        cast(200, 200);
        assertEquals(4, service.update(2.5f, targets).size());
        assertEquals(85, mob.require(HealthComponent.class).current());
        for (float dt : new float[] {-1, Float.NaN, Float.POSITIVE_INFINITY})
            assertThrows(IllegalArgumentException.class, () -> service.update(dt, targets));
    }

    /** Consulta a posição em cada pulso, inclui tangência e evita duplicatas no mesmo pulso. */
    @Test void entradaSaidaETangencia() {
        mob.setPosition(new Position(305, 200));
        cast(200, 200); assertEquals(120, mob.require(HealthComponent.class).current());
        mob.setPosition(new Position(304, 200));
        assertEquals(1, service.update(0.5f, List.of(targets.get(0), targets.get(0))).size());
        assertEquals(113, mob.require(HealthComponent.class).current());
        mob.setPosition(new Position(305, 200));
        assertTrue(service.update(0.5f, targets).isEmpty());
        mob.setPosition(new Position(200, 200));
        assertEquals(1, service.update(0.5f, targets).size());
        assertEquals(106, mob.require(HealthComponent.class).current());
    }

    /** CombatService publica morte uma vez; pulsos seguintes ignoram o cadáver. */
    @Test void eventosDeMorteESomenteCombatService() {
        var dying = createMob(7, 200, 200);
        var deathCount = new AtomicInteger();
        events.subscribe(EntityDeathEvent.class, event -> { assertSame(dying, event.entity()); deathCount.incrementAndGet(); });
        var victims = List.of(new CryostasisService.Target(dying, 19));
        service.cast(player, new SkillCastCommand("cryostasis", 200, 200), 960, 640, victims);
        assertEquals(1, deathCount.get()); assertTrue(dying.isDead());
        assertTrue(service.update(2.5f, victims).isEmpty());
        assertEquals(1, deathCount.get());
    }

    /** ICE é anexado ao enum e todas as raças atuais permanecem neutras. */
    @Test void afinidadeNeutraEEnumPreservado() {
        assertEquals(6, DamageType.ICE.ordinal());
        assertEquals(List.of("PHYSICAL", "FIRE", "WATER", "HOLY", "DARK", "EARTH", "ICE"),
                java.util.Arrays.stream(DamageType.values()).map(Enum::name).toList());
        for (var race : RaceRegistry.withDefaults().all()) assertEquals(1.0, race.affinityFor(DamageType.ICE));
    }

    /** A zona não acompanha o lançador e continua ativa se ele morrer. */
    @Test void zonaFixaAposMoverOuMorrer() {
        cast(200, 200);
        player.setPosition(new Position(700, 500));
        player.require(HealthComponent.class).applyDamage(100);
        service.update(0.5f, targets);
        assertEquals(200, service.zones().get(0).x());
        assertEquals(200, service.zones().get(0).y());
        assertEquals(106, mob.require(HealthComponent.class).current());
    }
}
