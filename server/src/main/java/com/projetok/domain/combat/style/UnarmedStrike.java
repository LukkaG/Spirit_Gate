package com.projetok.domain.combat.style;

import com.projetok.domain.combat.CombatStyle;
import com.projetok.domain.entity.component.StatsComponent;
import com.projetok.domain.race.DamageType;

/**
 * Estilo de combate padrão usado quando nenhum estilo especial foi escolhido para uma entidade.
 *
 * <p>Equivale, em espírito, à intenção original de "sword null" em Human.java — a ideia de
 * que um personagem pode atacar sem estar armado. A diferença é que aqui isso é modelado
 * como um {@link CombatStyle} de verdade, e não como um campo esquecido/nulo: o dano nunca
 * some, ele só passa a vir 100% dos atributos do personagem em vez de vir de uma arma.</p>
 *
 * <p>{@link com.projetok.domain.entity.component.CombatComponent} usa esta classe
 * automaticamente como fallback sempre que é construído ou atualizado com {@code style =
 * null} — nenhuma outra classe do domínio precisa instanciar {@code UnarmedStrike}
 * manualmente, a menos que queira usá-lo explicitamente (ex.: um NPC que só briga
 * desarmado, de propósito).</p>
 */
public final class UnarmedStrike implements CombatStyle {

    /**
     * Calcula o dano do golpe desarmado.
     *
     * <p>Fórmula: metade da força do atacante, sem nenhum bônus de arma ou item —
     * propositalmente simples, para servir de "piso" de dano que qualquer personagem
     * consegue causar mesmo sem equipamento nenhum.</p>
     *
     * @param stats atributos atuais do atacante; só {@code strength()} é usado aqui
     * @return dano calculado (>= 0, assumindo strength >= 0)
     */
    @Override
    public int computeDamage(StatsComponent stats) {
        return stats.strength() / 2;
    }

    /**
     * @return {@link DamageType#PHYSICAL} — golpe desarmado nunca causa dano elemental
     */
    @Override
    public DamageType damageType() {
        return DamageType.PHYSICAL;
    }

    /**
     * @return "Ataque Desarmado" — nome exibido ao jogador (ex.: em log de combate)
     */
    @Override
    public String displayName() {
        return "Ataque Desarmado";
    }

    /**
     * @return 0 — atacar desarmado não custa recurso nenhum; é a ação "de base" que todo
     * personagem sempre tem disponível, mesmo sem stamina
     */
    @Override
    public int staminaCost() {
        return 0;
    }
}
