package com.projetok.domain.effect;

import com.projetok.domain.entity.Mob;
import com.projetok.domain.entity.Player;
import com.projetok.domain.entity.Position;
import com.projetok.domain.entity.component.HealthComponent;
import com.projetok.domain.entity.component.StatusEffectsComponent;
import com.projetok.domain.item.LootTable;
import com.projetok.domain.network.PlayerConnection;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Regressões do armazenamento, aplicação e relógio de efeitos sem interface ou habilidade. */
class StatusEffectServiceTest {
    private final StatusEffectService service = new StatusEffectService();

    /** Cria um mob mínimo sem depender da fábrica ou de componentes de combate. */
    private Mob mob() {
        var mob = new Mob(10, "Alvo", new Position(20, 30), LootTable.of());
        mob.attach(new HealthComponent(100));
        return mob;
    }

    /** Cria jogador com vida para verificar a restrição de congelamento. */
    private Player player() {
        var player = new Player(1, "Jogador", new Position(0, 0), new PlayerConnection() {
            @Override public void send(Object packet) { }
            @Override public void disconnect() { }
        });
        player.attach(new HealthComponent(100));
        return player;
    }

    /** Componente novo não inventa efeitos; a atualização sem componente não o anexa. */
    @Test void inicialmenteSemEfeitos() {
        var effects = new StatusEffectsComponent();
        assertFalse(effects.has(StatusEffectType.FREEZE));
        assertEquals(0f, effects.remaining(StatusEffectType.FREEZE));
        assertTrue(effects.snapshot().isEmpty());
        var mob = mob();
        service.update(mob, 1f);
        assertTrue(mob.get(StatusEffectsComponent.class).isEmpty());
    }

    /** Aplicação cria armazenamento compatível com entidades antigas e dura um segundo. */
    @Test void aplicacaoEDuracaoDeUmSegundo() {
        var mob = mob();
        assertTrue(service.apply(mob, StatusEffectType.FREEZE, 1f));
        var effects = mob.require(StatusEffectsComponent.class);
        assertTrue(effects.has(StatusEffectType.FREEZE));
        assertEquals(1f, effects.remaining(StatusEffectType.FREEZE));
        service.update(mob, 0f);
        assertEquals(1f, effects.remaining(StatusEffectType.FREEZE));
        service.update(mob, 0.4f);
        assertEquals(0.6f, effects.remaining(StatusEffectType.FREEZE), 0.000001f);
        service.update(mob, 0.59f);
        assertTrue(effects.has(StatusEffectType.FREEZE));
        service.update(mob, 0.01f);
        assertFalse(effects.has(StatusEffectType.FREEZE));
        assertEquals(0f, effects.remaining(StatusEffectType.FREEZE));
    }

    /** Novo sucesso restaura exatamente um segundo, sem somar à duração anterior. */
    @Test void renovacaoNaoAcumulaTempo() {
        var mob = mob();
        service.apply(mob, StatusEffectType.FREEZE, 1f);
        service.update(mob, 0.4f);
        var effects = mob.require(StatusEffectsComponent.class);
        assertEquals(0.6f, effects.remaining(StatusEffectType.FREEZE), 0.0001f);
        service.apply(mob, StatusEffectType.FREEZE, 1f);
        assertEquals(1f, effects.remaining(StatusEffectType.FREEZE));
        service.update(mob, 0.9f);
        assertTrue(effects.has(StatusEffectType.FREEZE));
        service.update(mob, 0.1f);
        assertFalse(effects.has(StatusEffectType.FREEZE));
    }

    /** Trinta passos da arena expiram o efeito sem deixar resíduos do arredondamento float. */
    @Test void expiraComDeltaDeTrintaHertz() {
        var mob = mob();
        service.apply(mob, StatusEffectType.FREEZE, 1f);
        for (int i = 0; i < 29; i++) service.update(mob, 1f / 30f);
        assertTrue(mob.require(StatusEffectsComponent.class).has(StatusEffectType.FREEZE));
        service.update(mob, 1f / 30f);
        assertFalse(mob.require(StatusEffectsComponent.class).has(StatusEffectType.FREEZE));
    }

    /** Mortos não recebem efeito novo e a atualização limpa um efeito anterior à morte. */
    @Test void mortoNaoRecebeELimpaEfeitoAnterior() {
        var mob = mob();
        service.apply(mob, StatusEffectType.FREEZE, 1f);
        service.update(mob, 0.3f);
        mob.receiveDamage(100);
        assertFalse(service.apply(mob, StatusEffectType.FREEZE, 1f));
        assertEquals(0.7f, mob.require(StatusEffectsComponent.class).remaining(StatusEffectType.FREEZE), 0.000001f);
        service.update(mob, 0f);
        assertTrue(mob.require(StatusEffectsComponent.class).snapshot().isEmpty());
        var alreadyDead = mob();
        alreadyDead.receiveDamage(100);
        assertFalse(service.apply(alreadyDead, StatusEffectType.FREEZE, 1f));
        assertTrue(alreadyDead.get(StatusEffectsComponent.class).isEmpty());
    }

    /** Congelamento de jogadores fica fora do escopo e não anexa componente. */
    @Test void jogadorNaoRecebeCongelamento() {
        var player = player();
        assertFalse(service.apply(player, StatusEffectType.FREEZE, 1f));
        assertTrue(player.get(StatusEffectsComponent.class).isEmpty());
        assertEquals(100, player.require(HealthComponent.class).current());
    }

    /** Cópias do mapa não aceitam alterações e não acompanham futuras mutações. */
    @Test void snapshotImutavelEIndependente() {
        var effects = new StatusEffectsComponent();
        effects.refresh(StatusEffectType.FREEZE, 1f);
        var snapshot = effects.snapshot();
        assertThrows(UnsupportedOperationException.class, () -> snapshot.clear());
        effects.advance(0.5f);
        assertEquals(1.0, snapshot.get(StatusEffectType.FREEZE));
        assertEquals(0.5f, effects.remaining(StatusEffectType.FREEZE));
        effects.clear();
        assertTrue(effects.snapshot().isEmpty());
        assertEquals(1, snapshot.size());
    }

    /** Durações inválidas falham antes de anexar ou renovar o armazenamento. */
    @Test void rejeitaDuracoesInvalidasSemMutacao() {
        var mob = mob();
        var component = new StatusEffectsComponent();
        for (float invalid : new float[] {0f, -1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> service.apply(mob, StatusEffectType.FREEZE, invalid));
            assertThrows(IllegalArgumentException.class, () -> component.refresh(StatusEffectType.FREEZE, invalid));
            assertTrue(mob.get(StatusEffectsComponent.class).isEmpty());
            assertTrue(component.snapshot().isEmpty());
        }
        service.apply(mob, StatusEffectType.FREEZE, 1f);
        assertThrows(IllegalArgumentException.class, () -> service.apply(mob, StatusEffectType.FREEZE, Float.NaN));
        assertEquals(1f, mob.require(StatusEffectsComponent.class).remaining(StatusEffectType.FREEZE));
    }

    /** Deltas inválidos não removem efeitos nem alteram seu tempo restante. */
    @Test void rejeitaDeltasInvalidosSemMutacao() {
        var mob = mob();
        service.apply(mob, StatusEffectType.FREEZE, 1f);
        var effects = mob.require(StatusEffectsComponent.class);
        for (float invalid : new float[] {-1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> service.update(mob, invalid));
            assertThrows(IllegalArgumentException.class, () -> effects.advance(invalid));
            assertEquals(1f, effects.remaining(StatusEffectType.FREEZE));
        }
        service.update(mob, Float.MAX_VALUE);
        assertFalse(effects.has(StatusEffectType.FREEZE));
    }
}
