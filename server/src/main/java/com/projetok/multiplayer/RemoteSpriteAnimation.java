package com.projetok.multiplayer;

import com.projetok.prototype.DirectionalAnimation;
import java.util.*;

/** Idle/Walk a 250 ms, determinado por snapshots; não usa animação como tick de simulação. */
public final class RemoteSpriteAnimation {
    public record Pose(DirectionalAnimation.Direction direction,boolean walking,int frame) { }
    private record Track(Protocol.Actor actor,long tick,long started,boolean walking,DirectionalAnimation.Direction direction) { }
    private final Map<Integer,Track> tracks=new HashMap<>();
    public void update(Protocol.State state,long millis){
        for(var actor:state.actors()){
            var old=tracks.get(actor.id());if(old!=null&&old.tick()==state.tick())continue;
            boolean walking=old!=null&&Math.hypot(actor.x()-old.actor().x(),actor.y()-old.actor().y())>.001;
            var direction=DirectionalAnimation.facing(actor.facingX(),actor.facingY(),old==null?DirectionalAnimation.Direction.FRONT:old.direction());
            long started=old!=null&&old.walking()&&walking&&old.direction()==direction?old.started():millis;
            tracks.put(actor.id(),new Track(actor,state.tick(),started,walking,direction));
        }
        tracks.keySet().retainAll(state.actors().stream().map(Protocol.Actor::id).toList());
    }
    public Pose pose(int id,long millis){var track=tracks.get(id);if(track==null)return new Pose(DirectionalAnimation.Direction.FRONT,false,0);return new Pose(track.direction(),track.walking(),(int)(Math.max(0,millis-track.started())/250%4));}
}
