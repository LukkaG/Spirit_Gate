package com.projetok.multiplayer;
import java.util.*;
/** Interpolacao exclusivamente visual; estados autoritativos nunca sao modificados. */
public record RemotePresentation(Protocol.State previous,Protocol.State current) {
    public RemotePresentation{Objects.requireNonNull(previous);Objects.requireNonNull(current);if(previous.tick()>current.tick())throw new IllegalArgumentException("Frames fora de ordem");}
    public Protocol.State sample(double alpha,int localId){
        if(!Double.isFinite(alpha)||alpha<0||alpha>1)throw new IllegalArgumentException("Interpolacao invalida");Map<Integer,Protocol.Actor> before=new HashMap<>();previous.actors().forEach(a->before.put(a.id(),a));
        return new Protocol.State(current.tick(),current.actors().stream().map(a->{var p=before.get(a.id());if(a.id()==localId||p==null||p.hp()==0||a.hp()==0||current.tick()-previous.tick()>15||Math.hypot(a.x()-p.x(),a.y()-p.y())>50)return a;return new Protocol.Actor(a.id(),a.name(),(float)(p.x()+(a.x()-p.x())*alpha),(float)(p.y()+(a.y()-p.y())*alpha),a.hp(),a.maxHp(),a.stamina(),a.coins(),a.boss(),a.facingX(),a.facingY(),a.flags());}).toList());
    }
}
