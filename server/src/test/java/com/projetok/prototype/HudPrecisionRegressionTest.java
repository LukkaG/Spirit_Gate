package com.projetok.prototype;

import com.projetok.domain.skill.CryostasisService;
import com.projetok.domain.skill.LightningStepService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Impede que arredondamentos no aviso contradigam a elegibilidade real. */
class HudPrecisionRegressionTest {
    /** O float imediatamente abaixo do custo continua insuficiente, sem mostrar saldo igual ao custo. */
    @Test void avisoNaoArredondaVigorInsuficienteParaSaldoSuficiente() {
        float dashBalance = Math.nextDown(LightningStepService.COST);
        var dash = SkillHudMapper.lightningStep(false, dashBalance, 0, false, false, SkillHudMapper.none());
        assertEquals(DebugSnapshot.SkillState.INSUFFICIENT_STAMINA, dash.state());
        assertEquals(dashBalance, dash.stamina());
        assertFalse(dash.stateText().contains("35/35"), "O aviso não pode apresentar saldo suficiente para uma ação recusada");

        float cryoBalance = Math.nextDown(CryostasisService.COST);
        var cryo = SkillHudMapper.cryostasis(false, cryoBalance, 0, SkillHudMapper.none());
        assertEquals(DebugSnapshot.SkillState.INSUFFICIENT_STAMINA, cryo.state());
        assertEquals(cryoBalance, cryo.stamina());
        assertFalse(cryo.stateText().contains("55/55"), "O aviso não deve arredondar a falta de vigor para cima");
    }
}
