package com.projetok.domain.entity;

import com.projetok.domain.entity.capability.Attacker;
import com.projetok.domain.entity.capability.Damageable;
import com.projetok.domain.entity.component.CombatComponent;
import com.projetok.domain.entity.component.EquipmentComponent;
import com.projetok.domain.entity.component.HealthComponent;
import com.projetok.domain.entity.component.RaceComponent;
import com.projetok.domain.entity.component.StatsComponent;
import com.projetok.domain.network.PlayerConnection;
import com.projetok.domain.race.DamageType;
import com.projetok.domain.race.Race;

import java.util.Objects;

/**
 * Uma entidade controlada por uma conexão de rede — um jogador humano de verdade, do outro
 * lado do socket.
 *
 * <p>Sucessora do {@code Player.java} original, que era {@code abstract}, não estendia
 * {@code Entity}, e não tinha nenhuma subclasse concreta em todo o projeto (nunca era
 * instanciado). Esta versão é concreta, sempre criada por {@link
 * com.projetok.domain.factory.PlayerFactory} (que garante que todos os componentes
 * necessários — stats, vida, combate, equipamento, inventário, raça — já estão anexados
 * antes do jogador entrar em jogo), e efetivamente ligada a uma conexão de rede real.</p>
 *
 * <p>Não guarda nenhum dado de jogo diretamente como campo — tudo isso vive em {@link
 * com.projetok.domain.entity.component.Component}s anexados via {@link GameEntity#attach}.
 * Esta classe só implementa os contratos de "papel": {@link Damageable} (pode receber dano)
 * e {@link Attacker} (pode causar dano), delegando os cálculos reais para os componentes.</p>
 */
public final class Player extends GameEntity implements Damageable, Attacker {

    /**
     * Canal de comunicação com o cliente deste jogador. Nunca {@code null} — um {@code
     * Player} sem conexão não faz sentido (é a própria definição de "controlado por
     * jogador").
     */
    private final PlayerConnection connection;

    /**
     * Cria um jogador. Normalmente não é chamado diretamente pelo resto do código — use
     * {@link com.projetok.domain.factory.PlayerFactory#create} para garantir que todos os
     * componentes necessários já vêm anexados.
     *
     * @param id identificador único do jogador
     * @param username nome de usuário, também usado como {@link GameEntity#getName()}
     * @param spawn posição inicial no mundo
     * @param connection conexão de rede associada a este jogador; não pode ser {@code null}
     */
    public Player(int id, String username, Position spawn, PlayerConnection connection) {
        super(id, username, spawn);
        this.connection = Objects.requireNonNull(connection, "connection");
    }

    /**
     * Aplica dano recebido ao {@link HealthComponent} deste jogador.
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
     * @return a raça deste jogador, usada por {@link
     * com.projetok.domain.combat.CombatService} para calcular a afinidade elemental ao
     * receber dano
     */
    @Override
    public Race race() {
        return require(RaceComponent.class).race();
    }

    /**
     * Calcula quanto dano este jogador causa em um ataque, somando o dano da arma equipada
     * (se houver) com o dano do estilo de combate atual.
     *
     * @return dano total do próximo ataque deste jogador
     */
    @Override
    public int computeOutgoingDamage() {
        return require(CombatComponent.class)
            .computeAttackDamage(require(StatsComponent.class), require(EquipmentComponent.class));
    }

    /**
     * @return o tipo de dano do estilo de combate atual (ex.: {@code WATER} para {@code
     * WaterStyle}, {@code PHYSICAL} para ataque desarmado) — não precisa de fallback manual
     * aqui, porque {@link CombatComponent#style()} nunca retorna {@code null}
     */
    @Override
    public DamageType outgoingDamageType() {
        return require(CombatComponent.class).style().damageType();
    }

    /**
     * @return a conexão de rede associada a este jogador, usada pela camada de
     * infraestrutura para enviar pacotes de resposta
     */
    public PlayerConnection connection() {
        return connection;
    }
}
