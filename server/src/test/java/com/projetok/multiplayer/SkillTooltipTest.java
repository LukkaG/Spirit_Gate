package com.projetok.multiplayer;

import com.projetok.rpg.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Garante ajuda útil nas quarenta habilidades e nos estados de indisponibilidade. */
class SkillTooltipTest {
    @Test void everySkillIncludesItsDescriptionControlsAndCooldownWhenUnavailable(){
        for(var skill:RpgSkillCatalog.all()){
            String text=NetworkSkillControls.tooltip(skill,"F"+(skill.slot()+4),"Energia insuficiente");
            assertTrue(text.contains(skill.name()),skill.id());assertTrue(text.contains(skill.description()),skill.id());
            assertTrue(text.contains(RpgSkillDescriptions.activation(skill)),skill.id());
            assertTrue(text.contains("Recarga:"));assertTrue(text.contains("Energia insuficiente"));
        }
    }
    @Test void advancedSkillsExplainTheirSpecificInputsAndImmunity(){
        assertTrue(RpgSkillCatalog.get("aetheri_2").description().contains("Conjurar novamente cancela"));
        assertTrue(RpgSkillCatalog.get("wind_5").description().contains("todos"));
        assertTrue(RpgSkillCatalog.get("thunder_5").description().contains("imune a dano"));
        assertTrue(RpgSkillDescriptions.activation(RpgSkillCatalog.get("light_3")).contains("derrotado"));
        assertTrue(RpgSkillDescriptions.activation(RpgSkillCatalog.get("fire_5")).contains("WASD"));
    }
    @Test void tooltipEscapesKeysAndStatusWithoutBreakingMarkup(){
        var text=NetworkSkillControls.tooltip(RpgSkillCatalog.get("water_1"),"<F5>","A & B");
        assertTrue(text.contains("&lt;F5&gt;"));assertTrue(text.contains("A &amp; B"));assertFalse(text.contains("<F5>"));
    }
    /** O texto precisa caber em uma ajuda Swing real, inclusive nas habilidades com instruções longas. */
    @Test void allTooltipLayoutsWrapWithinACompactReadableArea()throws Exception{
        javax.swing.SwingUtilities.invokeAndWait(()->{
            for(var skill:RpgSkillCatalog.all()){
                var button=new javax.swing.JButton();button.setToolTipText(NetworkSkillControls.tooltip(skill,"F9","Pronto"));
                var tip=button.createToolTip();tip.setTipText(button.getToolTipText());var size=tip.getPreferredSize();
                assertTrue(size.width>=300&&size.width<=420,skill.id()+": "+size);
                assertTrue(size.height>70&&size.height<=420,skill.id()+": "+size);
            }
        });
    }
}
