package com.projetok.studio;
import com.projetok.domain.entity.*;
import com.projetok.domain.movement.*;
import java.util.*;

/** Adapta retangulos explicitos ao mesmo CollisionService do jogo. */
public final class StudioGeometry {
    private static final GameEntity PROBE=new GameEntity(1,"Previa",new Position(0,0)){};
    private StudioGeometry(){}
    /** Areas transitaveis sao anotacoes; visibilidade da camada nao desativa colisao. */
    public static CollisionService.Space space(WorldDocument d){return new CollisionService.Space(d.width(),d.height(),d.hitboxes().stream().flatMap(h->obstacles(h).stream()).toList(),java.util.List.of());}
    /** Quatro lados mantêm o volume antigo; lados parciais viram bordas de um pixel. */
    public static List<CollisionService.Obstacle> obstacles(WorldDocument.Hitbox h){
        if(!h.solid()||h.edges()==0)return List.of();
        if(h.edges()==WorldDocument.Hitbox.ALL)return List.of(new CollisionService.Obstacle(h.x(),h.y(),h.width(),h.height()));
        var result=new ArrayList<CollisionService.Obstacle>();
        if(h.blocks(WorldDocument.Hitbox.UP))result.add(new CollisionService.Obstacle(h.x(),h.y(),h.width(),1));
        if(h.blocks(WorldDocument.Hitbox.DOWN))result.add(new CollisionService.Obstacle(h.x(),h.y()+h.height()-1,h.width(),1));
        if(h.blocks(WorldDocument.Hitbox.LEFT))result.add(new CollisionService.Obstacle(h.x(),h.y(),1,h.height()));
        if(h.blocks(WorldDocument.Hitbox.RIGHT))result.add(new CollisionService.Obstacle(h.x()+h.width()-1,h.y(),1,h.height()));
        return List.copyOf(result);
    }
    /** Previa fisica real do circulo do jogador, sem simulacao alternativa. */
    public static boolean free(WorldDocument d,float x,float y,float radius){return new CollisionService().isFree(PROBE,x,y,radius,space(d));}
    /** Consulta espaco imutavel residente para evitar reconstrucao em cada repaint. */
    public static boolean free(CollisionService.Space space,float x,float y,float radius){return new CollisionService().isFree(PROBE,x,y,radius,space);}
    /** Sobreposicoes nao sao corrigidas silenciosamente. */
    public static java.util.List<String> overlaps(WorldDocument d){var result=new ArrayList<String>();for(int i=0;i<d.hitboxes().size();i++)for(int j=i+1;j<d.hitboxes().size();j++){var a=d.hitboxes().get(i);var b=d.hitboxes().get(j);if(a.solid()&&b.solid()&&a.x()<b.x()+b.width()&&a.x()+a.width()>b.x()&&a.y()<b.y()+b.height()&&a.y()+a.height()>b.y()){result.add(a.id()+" / "+b.id());if(result.size()==1000){result.add("Diagnostico truncado em 1000 sobreposicoes; refine os solidos");return java.util.List.copyOf(result);}}}return java.util.List.copyOf(result);}
}
