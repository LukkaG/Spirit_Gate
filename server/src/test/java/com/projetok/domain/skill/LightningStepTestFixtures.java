package com.projetok.domain.skill;

import com.projetok.domain.entity.Player;
import com.projetok.domain.entity.Position;
import com.projetok.domain.factory.PlayerArchetype;
import com.projetok.domain.factory.PlayerFactory;
import com.projetok.domain.movement.CollisionService;
import com.projetok.domain.movement.CollisionPolicy;
import com.projetok.domain.movement.MovementService;
import com.projetok.domain.network.PlayerConnection;
import com.projetok.domain.progression.VitalityService;
import com.projetok.domain.race.RaceRegistry;
import com.projetok.domain.resource.StaminaService;
import java.util.List;

/** Montagem real e reutilizável dos testes de domínio do Passo Relâmpago. */
final class LightningStepTestFixtures {
    /** Mesmo raio físico usado pelo jogador na arena. */
    static final float RADIUS = 18f;

    /** Impede instanciação da coleção de fixtures. */
    private LightningStepTestFixtures() { }

    /** Cria um jogador humano pela fábrica real, com vida e vigor iniciais de 100. */
    static Player player(float x, float y) {
        var archetype = new PlayerArchetype(RaceRegistry.withDefaults().get("human"),
                30, 8, 14, null, null);
        return new PlayerFactory(new VitalityService()).create(1, "Teste de dash", archetype,
                new Position(x, y), new PlayerConnection() {
                    @Override public void send(Object packet) { /* canal local sem rede */ }
                    @Override public void disconnect() { /* nenhuma conexão aberta */ }
                });
    }

    /** Cria uma arena vazia de 960 por 640 unidades, sem janela ou banco de dados. */
    static CollisionService.Space emptySpace() {
        return new CollisionService.Space(960f, 640f, List.of(), List.of());
    }

    /** Compartilha a mesma detecção de colisões entre o serviço e a movimentação. */
    static LightningStepService service() {
        var collisions = new CollisionService();
        return new LightningStepService(new MovementService(collisions), new StaminaService(), collisions);
    }

    /** Monta a política seletiva sem mudar os cenários sólidos anteriores. */
    static LightningStepService selectiveService() {
        var collisions = new CollisionService();
        return new LightningStepService(new MovementService(collisions), new StaminaService(),
                collisions, CollisionPolicy.PHASE_THROUGH_ACTORS_AND_SOFT);
    }
}
