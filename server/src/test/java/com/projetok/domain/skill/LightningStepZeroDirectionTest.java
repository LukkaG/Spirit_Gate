package com.projetok.domain.skill;

import com.projetok.domain.entity.component.StaminaComponent;
import org.junit.jupiter.api.Test;

import static com.projetok.domain.skill.LightningStepTestFixtures.RADIUS;
import static com.projetok.domain.skill.LightningStepTestFixtures.emptySpace;
import static com.projetok.domain.skill.LightningStepTestFixtures.player;
import static com.projetok.domain.skill.LightningStepTestFixtures.service;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Verifica a recusa por direção zero sem alterar o estado do jogador. */
class LightningStepZeroDirectionTest {

    /** Cursor sobre o jogador não inicia dash, recarga ou consumo de vigor. */
    @Test
    void deveRejeitarDashSemDirecao() {
        var jogador = player(100f, 100f);
        var servico = service();
        var espaco = emptySpace();
        var vigor = jogador.require(StaminaComponent.class);

        var posicaoInicial = jogador.getPosition();
        var vigorInicial = vigor.current();

        // A mira acompanha a posição da fixture para garantir direção zero.
        var comando = new SkillCastCommand(
            LightningStepService.ID, posicaoInicial.x(), posicaoInicial.y()
        );

        var resultado = servico.cast(jogador, comando, RADIUS, espaco);

        assertEquals(LightningStepService.Status.ZERO_DIRECTION, resultado);
        assertEquals(posicaoInicial, jogador.getPosition());
        assertEquals(vigorInicial, vigor.current(), 0.0001f);
        assertEquals(0f, servico.cooldown(), 0.0001f);
        assertFalse(servico.active());
    }
}
