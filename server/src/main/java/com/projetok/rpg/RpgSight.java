package com.projetok.rpg;
import com.projetok.domain.entity.*;
import com.projetok.domain.movement.*;

/** Consulta compartilhada da linha de visao existente, com amostras de ate seis pixels. */
public final class RpgSight {
    private static final GameEntity PROBE=new GameEntity(1,"Visao",new Position(0,0)){};
    private static final CollisionService COLLISION=new CollisionService();
    private RpgSight(){}
    /** Usa a mesma geometria e tolerancia de um pixel do combate offline. */
    public static boolean clear(Position a,Position b,CollisionService.Space space) {
        double distance=Math.hypot(a.x()-b.x(),a.y()-b.y());int samples=Math.max(1,(int)(distance/6));
        for(int i=1;i<samples;i++)if(!COLLISION.isFree(PROBE,a.x()+(b.x()-a.x())*i/samples,a.y()+(b.y()-a.y())*i/samples,1,space))return false;
        return true;
    }
}
