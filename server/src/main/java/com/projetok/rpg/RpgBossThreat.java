package com.projetok.rpg;
import com.projetok.domain.entity.Position;
import com.projetok.domain.movement.CollisionService;

/** Aviso imutavel do ataque em preparacao; geometria e tempo vem do dominio. */
public record RpgBossThreat(float x,float y,int radius,long remainingTicks,boolean enraged,CollisionService.Space geometry) {
    /** Predicado de perigo coincide com alcance e linha de visao usados no golpe. */
    public boolean contains(float px,float py){return Math.hypot(px-x,py-y)<=radius&&RpgSight.clear(new Position(px,py),new Position(x,y),geometry);}
}
