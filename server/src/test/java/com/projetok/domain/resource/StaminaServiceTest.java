package com.projetok.domain.resource;

import com.projetok.domain.entity.GameEntity;
import com.projetok.domain.entity.Position;
import com.projetok.domain.entity.component.HealthComponent;
import com.projetok.domain.entity.component.StaminaComponent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Regressões do recurso sem Swing, rede ou mundo de prototipagem. */
class StaminaServiceTest {
    private final StaminaService service = new StaminaService();

    /** Cria uma entidade mínima com vida e vigor. */
    private GameEntity actor() {
        GameEntity entity = new GameEntity(1, "Teste", new Position(0, 0)) { };
        entity.attach(new HealthComponent(100));
        entity.attach(new StaminaComponent());
        return entity;
    }

    /** Valores iniciais e máximo configurável permanecem válidos. */
    @Test
    void valoresIniciais() {
        var stamina = new StaminaComponent();
        assertEquals(100f, stamina.current());
        assertEquals(100f, stamina.max());
        assertEquals(50f, new StaminaComponent(50f).current());
    }

    /** Custo zero é aceito inclusive sem saldo; recusa não modifica o recurso. */
    @Test
    void consumoValidoZeroEInsuficiente() {
        var entity = actor();
        var stamina = entity.require(StaminaComponent.class);
        assertTrue(service.tryConsume(entity, 20));
        assertEquals(80, stamina.current());
        assertFalse(service.tryConsume(entity, 81));
        assertEquals(80, stamina.current());
        assertTrue(service.tryConsume(entity, 80));
        assertTrue(service.tryConsume(entity, 0));
        assertFalse(service.tryConsume(entity, 1));
        assertEquals(0, stamina.current());
    }

    /** Regeneração fracionária, delta zero e saturação no máximo. */
    @Test
    void regeneracaoPorTempoELimite() {
        var entity = actor();
        var stamina = entity.require(StaminaComponent.class);
        service.tryConsume(entity, 100);
        service.regenerate(entity, 0.5f);
        assertEquals(7.5f, stamina.current());
        service.regenerate(entity, 0);
        assertEquals(7.5f, stamina.current());
        service.regenerate(entity, Float.MAX_VALUE);
        assertEquals(100, stamina.current());
        stamina.restore(Float.MAX_VALUE);
        assertEquals(100, stamina.current());
    }

    /** Toda entrada numérica inválida falha antes de modificar o componente. */
    @Test
    void rejeitaInvalidosSemMutacao() {
        var entity = actor();
        var stamina = entity.require(StaminaComponent.class);
        for (float invalid : new float[] {-1, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new StaminaComponent(invalid));
            assertThrows(IllegalArgumentException.class, () -> stamina.tryConsume(invalid));
            assertThrows(IllegalArgumentException.class, () -> stamina.restore(invalid));
            assertThrows(IllegalArgumentException.class, () -> service.tryConsume(entity, invalid));
            assertThrows(IllegalArgumentException.class, () -> service.regenerate(entity, invalid));
            assertEquals(100, stamina.current());
        }
        assertThrows(IllegalArgumentException.class, () -> new StaminaComponent(0));
    }

    /** Mortos não consomem nem regeneram, mesmo que haja saldo ou custo zero. */
    @Test
    void mortoNaoExecutaNemRegenera() {
        var entity = actor();
        service.tryConsume(entity, 20);
        entity.require(HealthComponent.class).applyDamage(100);
        assertFalse(service.tryConsume(entity, 20));
        assertFalse(service.tryConsume(entity, 0));
        service.regenerate(entity, 10);
        assertEquals(80, entity.require(StaminaComponent.class).current());
        assertThrows(IllegalArgumentException.class, () -> service.tryConsume(entity, Float.NaN));
    }
}
