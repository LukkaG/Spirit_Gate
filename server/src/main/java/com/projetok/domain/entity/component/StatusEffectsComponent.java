package com.projetok.domain.entity.component;

import com.projetok.domain.effect.StatusEffectType;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Guarda os efeitos e seus segundos restantes por entidade, sem regras de habilidade.
 * Somente a thread proprietária da entidade modifica este componente.
 */
public final class StatusEffectsComponent implements Component {
    private static final double EXPIRATION_TOLERANCE = 0.000001;
    private final EnumMap<StatusEffectType, Double> remaining = new EnumMap<>(StatusEffectType.class);

    /** Cria armazenamento sem efeitos ativos. */
    public StatusEffectsComponent() { }

    /** Informa se o tipo está ativo, sem modificar seu temporizador. */
    public boolean has(StatusEffectType type) {
        return remaining.containsKey(Objects.requireNonNull(type, "type"));
    }

    /** Retorna segundos restantes, ou zero quando o efeito não está ativo. */
    public float remaining(StatusEffectType type) {
        return remaining.getOrDefault(Objects.requireNonNull(type, "type"), 0.0).floatValue();
    }

    /** Publica uma cópia imutável dos temporizadores, sem expor o mapa interno. */
    public Map<StatusEffectType, Double> snapshot() {
        return Map.copyOf(remaining);
    }

    // --- Armazenamento e avanço do tempo ---

    /** Substitui a duração por um valor finito positivo, sem acumular o tempo anterior. */
    public void refresh(StatusEffectType type, float duration) {
        Objects.requireNonNull(type, "type");
        if (!Float.isFinite(duration) || duration <= 0f)
            throw new IllegalArgumentException("Duração do efeito deve ser finita e positiva");
        remaining.put(type, (double) duration);
    }

    /** Avança todos os efeitos; tolerância de um microssegundo evita resíduos de deltas float. */
    public void advance(float dt) {
        if (!Float.isFinite(dt) || dt < 0f)
            throw new IllegalArgumentException("Delta do efeito deve ser finito e não negativo");
        remaining.replaceAll((type, seconds) -> seconds - dt);
        remaining.values().removeIf(seconds -> seconds <= EXPIRATION_TOLERANCE);
    }

    /** Remove todos os efeitos, por exemplo após a morte do portador. */
    public void clear() {
        remaining.clear();
    }
}
