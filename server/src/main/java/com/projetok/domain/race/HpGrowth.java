package com.projetok.domain.race;

/**
 * As DUAS únicas fontes de HP máximo do jogo: nível (hpPerLevel) e itens (itemBonusHp).
 * Não existe (e não deve existir) nenhum outro caminho de código que altere HP máximo.
 */
public record HpGrowth(int baseHp, int hpPerLevel) {

    public HpGrowth {
        if (baseHp < 1) throw new IllegalArgumentException("baseHp deve ser >= 1");
        if (hpPerLevel < 0) throw new IllegalArgumentException("hpPerLevel não pode ser negativo");
    }

    public int computeMaxHp(int level, int itemBonusHp) {
        return baseHp + hpPerLevel * (level - 1) + Math.max(0, itemBonusHp);
    }
}
