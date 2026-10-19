package com.projetok.domain.movement;

import com.projetok.domain.combat.CombatService;
import com.projetok.domain.effect.StatusEffectService;
import com.projetok.domain.effect.StatusEffectType;
import com.projetok.domain.entity.Mob;
import com.projetok.domain.entity.Player;
import com.projetok.domain.entity.Position;
import com.projetok.domain.entity.component.CombatComponent;
import com.projetok.domain.entity.component.EquipmentComponent;
import com.projetok.domain.entity.component.HealthComponent;
import com.projetok.domain.entity.component.RaceComponent;
import com.projetok.domain.entity.component.StatsComponent;
import com.projetok.domain.entity.component.StatusEffectsComponent;
import com.projetok.domain.event.GameEventBus;
import com.projetok.domain.item.LootTable;
import com.projetok.domain.network.PlayerConnection;
import com.projetok.domain.race.RaceRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Comprova imobilização autoritativa sem interferir em dano, jogadores ou validação. */
class FreezeMovementTest {
    private final MovementService movement = new MovementService(new CollisionService());
    private final StatusEffectService effects = new StatusEffectService();
    private final CollisionService.Space space = new CollisionService.Space(1000, 1000, List.of(), List.of());
    private final Mob mob = mob();

    /** Monta um mob desarmado real com vida e atributos suficientes para atacar. */
    private static Mob mob() {
        Mob mob = new Mob(10, "Mob", new Position(200, 200), LootTable.of());
        mob.attach(new HealthComponent(120));
        mob.attach(new RaceComponent(RaceRegistry.withDefaults().get("demon")));
        mob.attach(new StatsComponent(5, 5, 5, 1, 0));
        mob.attach(new EquipmentComponent());
        mob.attach(new CombatComponent(null));
        return mob;
    }

    /** Cria jogador neutro ao dano físico recebido do mob. */
    private static Player player() {
        Player player = new Player(1, "Jogador", new Position(240, 200), new PlayerConnection() {
            @Override public void send(Object packet) { }
            @Override public void disconnect() { }
        });
        player.attach(new HealthComponent(100));
        player.attach(new RaceComponent(RaceRegistry.withDefaults().get("human")));
        return player;
    }

    // --- Bloqueio nos caminhos públicos de movimentação ---

    /** Deslocamentos diretos, direcionados e contínuos não mudam a posição congelada. */
    @Test void freezeBloqueiaTodosOsCaminhosDeMovimento() {
        assertTrue(effects.apply(mob, StatusEffectType.FREEZE, 1f));
        Position original = mob.getPosition();

        movement.move(mob, 30, 20, 19, space);
        assertEquals(original, mob.getPosition());
        movement.moveDirected(mob, movement.direction(1, 1), 92, 0.5f, 19, space);
        assertEquals(original, mob.getPosition());
        movement.moveContinuous(mob, 200, 200, 19, space);
        assertEquals(original, mob.getPosition());
        assertEquals(1f, mob.require(StatusEffectsComponent.class).remaining(StatusEffectType.FREEZE));
    }

    /** O movimento é retomado apenas quando o segundo inteiro do efeito expira. */
    @Test void movimentoRetornaAposExpirarUmSegundo() {
        effects.apply(mob, StatusEffectType.FREEZE, 1f);
        effects.update(mob, 0.75f);
        movement.move(mob, 10, 0, 19, space);
        assertEquals(new Position(200, 200), mob.getPosition());
        assertEquals(0.25f, mob.require(StatusEffectsComponent.class).remaining(StatusEffectType.FREEZE));

        effects.update(mob, 0.25f);
        assertFalse(mob.require(StatusEffectsComponent.class).has(StatusEffectType.FREEZE));
        movement.move(mob, 10, 0, 19, space);
        assertEquals(new Position(210, 200), mob.getPosition());
        movement.moveDirected(mob, new Position(0, 1), 20, 0.5f, 19, space);
        assertEquals(new Position(210, 210), mob.getPosition());
        movement.moveContinuous(mob, 10, 0, 19, space);
        assertEquals(new Position(220, 210), mob.getPosition());
    }

    /** Freeze não transforma entradas inválidas em comandos aceitos silenciosamente. */
    @Test void entradasInvalidasContinuamRejeitadasDuranteFreeze() {
        effects.apply(mob, StatusEffectType.FREEZE, 1f);
        Position original = mob.getPosition();
        for (float invalid : new float[] {Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> movement.move(mob, invalid, 0, 19, space));
            assertThrows(IllegalArgumentException.class, () -> movement.move(mob, 0, invalid, 19, space));
            assertThrows(IllegalArgumentException.class, () -> movement.moveContinuous(mob, invalid, 0, 19, space));
            assertThrows(IllegalArgumentException.class,
                    () -> movement.moveDirected(mob, new Position(invalid, 0), 92, 0.1f, 19, space));
            assertThrows(IllegalArgumentException.class,
                    () -> movement.moveDirected(mob, new Position(1, 0), invalid, 0.1f, 19, space));
            assertThrows(IllegalArgumentException.class,
                    () -> movement.moveDirected(mob, new Position(1, 0), 92, invalid, 19, space));
        }
        for (float radius : new float[] {0, -1, Float.NaN, Float.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> movement.move(mob, 1, 0, radius, space));
            assertThrows(IllegalArgumentException.class,
                    () -> movement.moveContinuous(mob, 1, 0, radius, space));
        }
        assertThrows(IllegalArgumentException.class,
                () -> movement.moveDirected(mob, new Position(1, 0), -1, 0.1f, 19, space));
        assertThrows(IllegalArgumentException.class,
                () -> movement.moveDirected(mob, new Position(1, 0), 92, -1, 19, space));
        assertEquals(original, mob.getPosition());
    }

    /** A imobilização não muda o cálculo nem impede o mob de causar dano normal. */
    @Test void mobCongeladoMantemAtaqueEDano() {
        Player target = player();
        CombatService combat = new CombatService(new GameEventBus());
        int damageBefore = combat.attack(mob, target);
        assertTrue(damageBefore > 0);

        effects.apply(mob, StatusEffectType.FREEZE, 1f);
        int damageDuring = combat.attack(mob, target);

        assertEquals(damageBefore, damageDuring);
        assertEquals(100 - damageBefore - damageDuring, target.require(HealthComponent.class).current());
        assertTrue(mob.require(StatusEffectsComponent.class).has(StatusEffectType.FREEZE));
        assertEquals(new Position(200, 200), mob.getPosition());
    }

    /** Mesmo um componente fabricado não permite congelamento de jogadores nesta tarefa. */
    @Test void freezeNaoBloqueiaJogadorComComponenteAnexadoManualmente() {
        Player player = player();
        StatusEffectsComponent fabricated = new StatusEffectsComponent();
        fabricated.refresh(StatusEffectType.FREEZE, 1f);
        player.attach(fabricated);

        movement.move(player, 10, 0, 18, space);
        assertEquals(new Position(250, 200), player.getPosition());
        movement.moveDirected(player, new Position(0, 1), 20, 0.5f, 18, space);
        assertEquals(new Position(250, 210), player.getPosition());
        movement.moveContinuous(player, 10, 0, 18, space);
        assertEquals(new Position(260, 210), player.getPosition());
    }
}
