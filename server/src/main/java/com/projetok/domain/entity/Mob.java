package com.projetok.domain.entity;

import com.projetok.domain.ai.GameWorldView;
import com.projetok.domain.entity.capability.Attacker;
import com.projetok.domain.entity.capability.Damageable;
import com.projetok.domain.entity.capability.Lootable;
import com.projetok.domain.entity.component.AiComponent;
import com.projetok.domain.entity.component.CombatComponent;
import com.projetok.domain.entity.component.EquipmentComponent;
import com.projetok.domain.entity.component.HealthComponent;
import com.projetok.domain.entity.component.RaceComponent;
import com.projetok.domain.entity.component.StatsComponent;
import com.projetok.domain.item.Item;
import com.projetok.domain.item.LootTable;
import com.projetok.domain.race.DamageType;
import com.projetok.domain.race.Race;

import java.util.List;

/**
 * Uma entidade controlada por IA — hostil ou neutra, capaz de lutar e dropar itens.
 *
 * <p>Sucessora conceitual de {@code Demon.java}, mas "demônio" deixou de ser uma classe
 * Java e virou dado de raça (ver {@link com.projetok.domain.race.Race} e {@link
 * com.projetok.domain.race.RaceRegistry}). {@code Mob} é o "papel" — controlado por IA —
 * independente de qual raça ele tem. Um {@code Mob} pode ser da raça {@code "demon"},
 * {@code "spirit"}, ou qualquer raça futura, sem precisar de uma classe Java nova.</p>
 *
 * <p>Assim como {@link Player}, não guarda dado de jogo direto como campo (exceto {@link
 * #lootTable}, que é intrínseco ao "papel" de mob, não um atributo de raça/nível) — o resto
 * vive em componentes anexados via {@link GameEntity#attach}.</p>
 */
public final class Mob extends GameEntity implements Damageable, Attacker, Lootable {

    /**
     * Tabela de itens que este mob pode dropar ao morrer. Fixa na criação (via {@link
     * com.projetok.domain.factory.MobFactory}) — não muda durante a vida do mob.
     */
    private final LootTable lootTable;

    /**
     * Cria um mob. Normalmente não é chamado diretamente — use {@link
     * com.projetok.domain.factory.MobFactory#spawn} para garantir que todos os componentes
     * necessários já vêm anexados.
     *
     * @param id identificador único do mob
     * @param name nome exibido (ex.: "Demônio Menor")
     * @param spawn posição inicial no mundo
     * @param lootTable tabela de drops usada em {@link #rollLoot()}
     */
    public Mob(int id, String name, Position spawn, LootTable lootTable) {
        super(id, name, spawn);
        this.lootTable = lootTable;
    }

    /**
     * Aplica dano recebido ao {@link HealthComponent} deste mob.
     *
     * @param amount quantidade de dano a subtrair do HP atual
     */
    @Override
    public void receiveDamage(int amount) {
        require(HealthComponent.class).applyDamage(amount);
    }

    /**
     * @return {@code true} se o HP atual chegou a zero
     */
    @Override
    public boolean isDead() {
        return require(HealthComponent.class).isDead();
    }

    /**
     * @return a raça deste mob, usada por {@link
     * com.projetok.domain.combat.CombatService} para calcular a afinidade elemental ao
     * receber dano
     */
    @Override
    public Race race() {
        return require(RaceComponent.class).race();
    }

    /**
     * Calcula quanto dano este mob causa em um ataque, somando o dano da arma equipada
     * (se houver — muitos mobs lutam desarmados) com o dano do estilo de combate atual.
     *
     * @return dano total do próximo ataque deste mob
     */
    @Override
    public int computeOutgoingDamage() {
        return require(CombatComponent.class)
            .computeAttackDamage(require(StatsComponent.class), require(EquipmentComponent.class));
    }

    /**
     * @return o tipo de dano do estilo de combate atual — não precisa de fallback manual
     * aqui, porque {@link CombatComponent#style()} nunca retorna {@code null}
     */
    @Override
    public DamageType outgoingDamageType() {
        return require(CombatComponent.class).style().damageType();
    }

    /**
     * Sorteia os itens que este mob deixa cair ao morrer.
     *
     * @return lista de itens dropados (pode ser vazia, se nenhum item passar no sorteio de
     * chance de {@link #lootTable})
     */
    @Override
    public List<Item> rollLoot() {
        return lootTable.roll();
    }

    /**
     * Executa um "pensamento" de IA deste mob, delegando para o comportamento anexado (se
     * houver {@link AiComponent}).
     *
     * <p>Chamado pelo futuro {@code GameLoop}/{@code MobAiSystem} a cada tick — o loop
     * ainda não existe no projeto (ver roadmap, passo 2), então este método hoje não é
     * chamado por ninguém ainda.</p>
     *
     * @param world visão do mundo que a IA usa para tomar decisões (ex.: achar alvo hostil)
     */
    public void tick(GameWorldView world) {
        get(AiComponent.class).ifPresent(ai -> ai.tick(this, world));
    }
}
