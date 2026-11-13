package com.projetok.domain.mob;

/** Dados imutáveis de uma espécie; vida atual, posição e respawn pertencem à instância. */
public record MobDefinition(String id, int hp, int attack, int speed, int aggro,
                            int leash, int respawn, int level) {
    /** Valida os limites já usados pelo runtime, sem conceder recompensas novas. */
    public MobDefinition {
        if (id == null || !id.matches("[a-z][a-z0-9_-]{0,39}")
                || hp < 1 || hp > 10000 || attack < 1 || attack > 100
                || speed < 1 || speed > 200 || aggro < 40 || aggro > 800
                || leash < aggro || leash > 900 || respawn < 0 || respawn > 9000)
            throw new IllegalArgumentException("Parâmetros de mob inválidos");
        if (level != 1)
            throw new IllegalArgumentException("Recompensas de mobs acima do nível 1 ainda não definidas");
    }
}
