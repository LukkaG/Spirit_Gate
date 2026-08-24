package com.projetok.domain.race;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Conjunto de raças padrão para desenvolvimento/testes locais. Em produção, isto deve ser
 * substituído por carga a partir da tabela `races` no Postgres (ver Pilar 2 do documento de
 * arquitetura) — nenhuma outra classe do domínio depende de como esses dados chegam até
 * aqui, só desta interface (RaceRegistry.get(id)).
 */
public final class RaceRegistry {

    private final Map<String, Race> races;

    private RaceRegistry(Map<String, Race> races) {
        this.races = races;
    }

    public static RaceRegistry of(Collection<Race> races) {
        Map<String, Race> byId = races.stream()
            .collect(Collectors.toMap(Race::id, race -> race));
        return new RaceRegistry(byId);
    }

    public static RaceRegistry withDefaults() {
        return of(List.of(
            new Race(
                "human", "Humano",
                new HpGrowth(100, 10),
                AttributeGrowth.NONE,
                Map.of(),
                List.of()
            ),
            new Race(
                "demon", "Demônio",
                new HpGrowth(120, 14),
                new AttributeGrowth(2, 0, 0),
                Map.of(DamageType.HOLY, 1.5, DamageType.WATER, 1.3),
                List.of()
            ),
            new Race(
                "spirit", "Espírito",
                new HpGrowth(80, 8),
                new AttributeGrowth(0, 3, 1),
                Map.of(DamageType.PHYSICAL, 1.4, DamageType.HOLY, 0.6),
                List.of()
            )
        ));
    }

    public Race get(String id) {
        Race race = races.get(id);
        if (race == null) {
            throw new IllegalArgumentException("Raça desconhecida: " + id);
        }
        return race;
    }

    public Collection<Race> all() {
        return races.values();
    }
}
