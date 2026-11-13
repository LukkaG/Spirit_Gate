package com.projetok.rpg;

import java.util.Map;
import java.util.Objects;

/**
 * Ponte Java entre pacotes da simulação: transporta somente os valores imutáveis da R3.
 * Não autentica participante, expõe endpoint ou cria um snapshot recuperável.
 */
public record RpgCharacterObservation(CharacterReadState character, RpgSkillEngine.TimingRead timing) {
    /**
     * O coordenador confiável fornece seu owner e mapa atual, após validar a admissão.
     * A guarda ocorre antes de construir o contexto ou ler qualquer objeto de gameplay.
     */
    public static RpgCharacterObservation read(Thread owner, int participant, RpgWorld world,
            RpgSkillEngine engine, Map<Integer,RpgWorld> participants) {
        if (owner == null || Thread.currentThread() != owner)
            throw new IllegalStateException("Leitura fora do owner do coordenador");
        Objects.requireNonNull(world); Objects.requireNonNull(engine); Objects.requireNonNull(participants);
        if (participant < 1 || participants.get(participant) != world)
            throw new IllegalArgumentException("Participação divergente");
        var context = new CharacterReadContext(world, engine);
        var timing = engine.readTiming(participant, world, participants, context);
        if (timing.status() == RpgSkillEngine.TimingStatus.INCOMPATIBLE)
            return new RpgCharacterObservation(null, timing);
        return new RpgCharacterObservation(world.readCharacterState(context), timing);
    }
}
