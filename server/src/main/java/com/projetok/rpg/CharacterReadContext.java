package com.projetok.rpg;

import java.util.Objects;
import java.util.UUID;

/**
 * Escopo interno confinado ao owner que o declara, sem autenticação ou fence durável.
 * O coordenador deve criá-lo no ponto serializado e invalidá-lo ao retirar a participação.
 * Não instala captura no servidor TCP nem comprova sozinho admissão no coop-world.
 */
final class CharacterReadContext {
    private final Thread owner = Thread.currentThread();
    private final RpgWorld world;
    private final RpgSkillEngine engine;
    private final UUID identity;
    private final String region, domain;
    private boolean present = true;

    /** Vincula uma participação já conhecida pelo chamador no owner da simulação. */
    CharacterReadContext(RpgWorld world) {
        this(world, null);
    }

    /** Associa também o executor conhecido no mesmo owner, sem criá-lo ou alterar suas sessões. */
    CharacterReadContext(RpgWorld world, RpgSkillEngine engine) {
        this.world = Objects.requireNonNull(world);
        this.engine = engine;
        var state = world.networkState();
        identity = state.identity().id(); region = state.region();
        domain = world.domainProgression().domain();
    }

    /** Recusa thread incorreta antes de acessar qualquer estado do mundo. */
    void requireOwner() {
        if (Thread.currentThread() != owner) throw new IllegalStateException("Leitura fora do owner declarado");
    }

    /** Verifica identidade observada, instância, região e domínio sem materializar estado. */
    void requireWorld(RpgWorld expected) {
        requireOwner();
        if (!present) throw new IllegalStateException("Fonte removida");
        if (world != expected) throw new IllegalArgumentException("Mundo divergente");
        var state = world.networkState();
        if (!identity.equals(state.identity().id()) || !region.equals(state.region())
                || !domain.equals(world.domainProgression().domain()))
            throw new IllegalStateException("Contexto de leitura divergente");
    }

    /** Invalida apenas o escopo local; não remove sessão nem modifica gameplay. */
    void retire() { requireOwner(); present = false; }

    /** Impede misturar o timing de outro executor no mesmo contexto declarado. */
    void requireEngine(RpgSkillEngine expected) {
        requireOwner();
        if (engine != expected) throw new IllegalArgumentException("Executor divergente");
    }
}
