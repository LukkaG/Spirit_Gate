package com.projetok.studio;
import com.projetok.rpg.RpgContent;
import java.util.*;

/** Validacao cruzada de destinos e chegada usando a geometria real do jogador. */
public final class PortalGraph {
    private PortalGraph(){}
    /** Nao presume que uma ligacao desenhada seja uma transicao executavel. */
    public static List<StudioProblem> validate(Map<String,WorldDocument> maps) {
        var problems=new ArrayList<StudioProblem>();
        for(var d:maps.values())for(var p:d.portals()) {
            var target=maps.get(p.destination());String reason=null;
            if(target==null)reason="Mapa de destino inexistente";
            else if(!StudioGeometry.free(target,p.arrivalX(),p.arrivalY(),18))reason="Chegada bloqueada ou fora dos limites";
            if(!StudioGeometry.free(d,p.x(),p.y(),18))reason="Entrada de portal bloqueada";
            if(!p.direction().isEmpty()&&!Set.of("FRONT","BACK","LEFT","RIGHT").contains(p.direction()))reason="Orientacao invalida";
            if(!p.quest().isEmpty())try{RpgContent.builtin().get("quest",p.quest());}catch(IllegalArgumentException error){reason="Missao inexistente";}
            if(reason!=null)problems.add(new StudioProblem(StudioProblem.Severity.ERROR,d.id(),p.id(),reason,p.destination()+" @ "+p.arrivalX()+","+p.arrivalY(),"Revise destino, chegada e condicao no editor de portal"));
        }
        return List.copyOf(problems);
    }
}
