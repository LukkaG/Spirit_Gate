package com.projetok.multiplayer;

import java.util.List;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Aliados derrotados mantêm seleção e informação de revive sem mudar o tratamento de inimigos mortos. */
class SkillTargetPresentationTest {
    @Test void defeatedAllyKeepsTargetRingAndReviveInformation()throws Exception {
        var self=new Protocol.Actor(1,"Curador",140,300,100,100,1000,0,false);
        var ally=new Protocol.Actor(2,"Aliado",170,300,0,100,1000,0,false);
        var snapshot=new Protocol.State(1,List.of(self,ally));var presentation=new NetworkCombatPresentation();presentation.selection(2);presentation.update(snapshot,List.of(),1);
        assertEquals(2,presentation.target());
        SwingUtilities.invokeAndWait(()->{var panel=new NetworkTargetPanel(()->{});panel.refresh(snapshot,1,2,60);assertTrue(panel.getAccessibleContext().getAccessibleDescription().contains("Aliado derrotado"));assertTrue(panel.getAccessibleContext().getAccessibleDescription().contains("Reviver"));});
        var enemy=new Protocol.Actor(1_000_100,"Inimigo",200,300,0,40,0,0,true);presentation.selection(enemy.id());presentation.update(new Protocol.State(2,List.of(self,ally,enemy)),List.of(),1);assertEquals(0,presentation.target());
    }
    @Test void smartcastUsesFacingAndShortensOnlyAtMapBoundary(){
        var right=new Protocol.Actor(1,"Jogador",140,300,100,100,1000,0,false,1,0,0);
        var forward=NetworkSkillControls.facingPoint(right,280);assertEquals(420,forward.x,.001);assertEquals(300,forward.y,.001);
        var up=new Protocol.Actor(1,"Jogador",140,300,100,100,1000,0,false,0,-1,0);
        var upward=NetworkSkillControls.facingPoint(up,320);assertEquals(140,upward.x,.001);assertEquals(18,upward.y,.001);
        var diagonal=new Protocol.Actor(1,"Jogador",140,300,100,100,1000,0,false,.70710677f,.70710677f,0);
        var tied=NetworkSkillControls.facingPoint(diagonal,200);assertEquals(140,tied.x,.001);assertEquals(500,tied.y,.001);
        assertEquals(com.projetok.prototype.DirectionalAnimation.Direction.FRONT,com.projetok.prototype.DirectionalAnimation.facing(1,1,com.projetok.prototype.DirectionalAnimation.Direction.RIGHT));
    }
}
