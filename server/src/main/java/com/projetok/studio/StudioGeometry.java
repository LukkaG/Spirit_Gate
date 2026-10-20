package com.projetok.studio;
import com.projetok.domain.entity.*;
import com.projetok.domain.movement.*;
import java.util.*;

/** Adapta retangulos explicitos ao mesmo CollisionService do jogo. */
public final class StudioGeometry {
    private static final GameEntity PROBE=new GameEntity(1,"Previa",new Position(0,0)){};
    private StudioGeometry(){}
    /** Areas transitaveis sao anotacoes; visibilidade da camada nao desativa colisao. */
    public static CollisionService.Space space(WorldDocument d){return new CollisionService.Space(d.width(),d.height(),d.hitboxes().stream().filter(WorldDocument.Hitbox::solid).map(h->new CollisionService.Obstacle(h.x(),h.y(),h.width(),h.height())).toList(),java.util.List.of());}
    /** Previa fisica real do circulo do jogador, sem simulacao alternativa. */
    public static boolean free(WorldDocument d,float x,float y,float radius){return new CollisionService().isFree(PROBE,x,y,radius,space(d));}
    /** Consulta espaco imutavel residente para evitar reconstrucao em cada repaint. */
    public static boolean free(CollisionService.Space space,float x,float y,float radius){return new CollisionService().isFree(PROBE,x,y,radius,space);}
    /** Sobreposicoes nao sao corrigidas silenciosamente. */
    public static java.util.List<String> overlaps(WorldDocument d){var result=new ArrayList<String>();for(int i=0;i<d.hitboxes().size();i++)for(int j=i+1;j<d.hitboxes().size();j++){var a=d.hitboxes().get(i);var b=d.hitboxes().get(j);if(a.solid()&&b.solid()&&a.x()<b.x()+b.width()&&a.x()+a.width()>b.x()&&a.y()<b.y()+b.height()&&a.y()+a.height()>b.y()){result.add(a.id()+" / "+b.id());if(result.size()==1000){result.add("Diagnostico truncado em 1000 sobreposicoes; refine os solidos");return java.util.List.copyOf(result);}}}return java.util.List.copyOf(result);}
}
