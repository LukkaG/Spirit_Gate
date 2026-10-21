package com.projetok.rpg;

import java.util.UUID;
import java.util.Objects;
import java.text.Normalizer;

/** Identidade local, sem login. Valores iniciais exclusivos do modo RPG. */
public record CharacterIdentity(UUID id, String name, String archetype, String appearance,
                                String initialRegion, int baseHp, int baseAttack) {
    public CharacterIdentity {
        Objects.requireNonNull(id);
        name = Normalizer.normalize(Objects.requireNonNull(name).strip(), Normalizer.Form.NFC);
        if (!name.matches("[\\p{L}][\\p{L}\\p{N} _-]{2,23}")) throw new IllegalArgumentException("Nome: 3 a 24 letras/numeros, espacos, _ ou -");
        archetype = CharacterRaces.normalizeLegacy(archetype);
        if (!java.util.Set.of("azure", "amber").contains(appearance) || !"village".equals(initialRegion)
                || baseHp < 1 || baseHp > 1000 || baseAttack < 1 || baseAttack > 100) throw new IllegalArgumentException("Identidade invalida");
    }
    /** Nome do componente mantido para compatibilidade do snapshot; o valor agora e uma raca. */
    public String race() { return archetype; }
    public static CharacterIdentity create(String name, String race, String appearance) {
        var definition=CharacterRaces.get(race);
        return new CharacterIdentity(UUID.randomUUID(), name, race, appearance, "village",definition.hp(),definition.attack());
    }
}
