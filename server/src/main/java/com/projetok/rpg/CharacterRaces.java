package com.projetok.rpg;

import java.util.List;
import java.util.Objects;

/** Origens disponiveis na criacao. Valores de combate sao provisórios e comuns a todas. */
public final class CharacterRaces {
    public record Definition(String id, String name, int hp, int attack, String startingAttack,
                             String startingAttackName, String description) { }
    private static final List<Definition> VALUES = List.of(
        new Definition("reaper", "Reaper", 100, 18, "sword", "Espada pequena",
            "Aprendiz de ceifador. Comeca com uma espada pequena, sem elemento."),
        new Definition("human", "Human", 100, 18, "unarmed", "Socos comuns",
            "Aprendiz humano. Comeca com socos comuns, sem luz ou especializacao marcial."),
        new Definition("aetheri", "Aetheri", 100, 18, "unarmed", "Socos comuns",
            "Aprendiz Aetheri. Comeca desarmado; os caminhos de arco chegam na iniciacao."),
        new Definition("spirit", "Spirit", 100, 18, "beam", "Feixe espiritual luminoso",
            "Espirito iniciante. Seus feixes luminosos basicos nao pertencem ao dominio Luz.")
    );
    private CharacterRaces() { }
    public static List<Definition> all() { return VALUES; }
    public static Definition get(String id) {
        return VALUES.stream().filter(r -> r.id().equals(id)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Raca desconhecida: " + id));
    }
    /** Somente leitura de identidades antigas; a criacao exige um ID atual. */
    public static String normalizeLegacy(String id) {
        return switch (Objects.requireNonNull(id, "raca")) {
            case "guardian" -> "reaper";
            case "mystic" -> "human";
            default -> get(id).id();
        };
    }
}
