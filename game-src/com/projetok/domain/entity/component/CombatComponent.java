package com.projetok.domain.entity.component;

import com.projetok.domain.combat.CombatStyle;
import com.projetok.domain.combat.style.UnarmedStrike;

/**
 * Componente que guarda COMO uma entidade luta — nunca o que ela tem equipado.
 *
 * <p>Só armazena um {@link CombatStyle} (a "técnica"/estilo de luta). O dano de arma vem
 * sempre de {@link EquipmentComponent} (fonte única de verdade sobre equipamento) — isso
 * evita ter "qual é a arma atual" guardado em dois lugares diferentes e podendo
 * dessincronizar.</p>
 *
 * <p><b>Combate desarmado:</b> se nenhum estilo especial for passado ({@code style ==
 * null}), este componente cai automaticamente em {@link UnarmedStrike}. Uma entidade NUNCA
 * fica incapaz de atacar só por não ter arma ou estilo equipados — "sem estilo escolhido" é
 * um estado explícito e funcional (ataque desarmado real, com dano calculado a partir dos
 * atributos), não um null silencioso que zeraria o dano.</p>
 *
 * <p><b>Novas armas e novos estilos (incluindo golpes desarmados):</b> para uma arma nova,
 * não é preciso mexer aqui — basta criar um {@link com.projetok.domain.item.Item} via
 * {@code Item.weapon(...)} e equipá-lo (ver {@link
 * com.projetok.domain.progression.EquipmentService}). Para um estilo novo, armado ou
 * desarmado (ex.: uma técnica de luta marcial), basta criar uma classe que implemente
 * {@link CombatStyle} — nenhuma outra classe do domínio precisa mudar.</p>
 */
public final class CombatComponent implements Component {

    /**
     * Estilo usado sempre que a entidade é criada ou atualizada sem um estilo explícito.
     * Compartilhado entre todas as instâncias (imutável, sem estado próprio de combate),
     * por isso pode ser {@code static final}.
     */
    private static final CombatStyle DEFAULT_STYLE = new UnarmedStrike();

    /** Estilo de combate atual da entidade. Nunca fica {@code null} — ver construtor e {@link #setStyle}. */
    private CombatStyle style;

    /**
     * Cria o componente com um estilo inicial.
     *
     * @param style estilo desejado; pode ser {@code null} para indicar "sem estilo
     * especial" — nesse caso, {@link #DEFAULT_STYLE} ({@link UnarmedStrike}) é usado no lugar
     */
    public CombatComponent(CombatStyle style) {
        this.style = style != null ? style : DEFAULT_STYLE;
    }

    /**
     * @return o estilo de combate atualmente ativo — nunca {@code null}
     */
    public CombatStyle style() {
        return style;
    }

    /**
     * Troca o estilo de combate em tempo real (ex.: jogador alterna de estilo em combate,
     * ou desequipa a arma e volta a lutar com as próprias mãos).
     *
     * @param style novo estilo; {@code null} volta para {@link UnarmedStrike}
     */
    public void setStyle(CombatStyle style) {
        this.style = style != null ? style : DEFAULT_STYLE;
    }

    /**
     * Calcula o dano total de um ataque: dano da arma equipada (se houver) mais o dano do
     * estilo atual.
     *
     * @param stats atributos do atacante, repassados ao estilo para calcular a parcela de
     * dano dele
     * @param equipment equipamento do atacante, usado só para ler a arma equipada; pode ser
     * {@code null} (tratado como "sem equipamento", dano de arma = 0)
     * @return soma do dano da arma com o dano do estilo — nunca lança exceção nem retorna
     * dano "vazio", mesmo sem nenhuma arma equipada, porque o estilo sozinho (mesmo o
     * padrão, desarmado) já produz dano real
     */
    public int computeAttackDamage(StatsComponent stats, EquipmentComponent equipment) {
        int weaponDamage = equipment != null ? equipment.weaponDamage() : 0;
        int styleDamage = style.computeDamage(stats); // style nunca é null a partir daqui
        return weaponDamage + styleDamage;
    }
}
