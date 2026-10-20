package com.projetok.prototype.rpg;

import com.projetok.prototype.DirectionalAnimation.Direction;
import com.projetok.prototype.DirectionalAnimation;
import com.projetok.rpg.RpgWorld;

/** Presentation only: consumes published positions, never input or mutable world state. */
public final class RpgPlayerAnimation {
    public record Pose(Direction direction, boolean walking, int frame) { }
    private final int frameMillis;
    private RpgWorld.View previous;
    private Pose pose=new Pose(Direction.FRONT,false,0);
    private long stateTick;

    public RpgPlayerAnimation(int frameMillis) {
        if(frameMillis<30 || frameMillis>2000) throw new IllegalArgumentException("frameMillis: 30..2000");
        this.frameMillis=frameMillis;
    }
    public Pose pose() { return pose; }
    public Pose update(RpgWorld.View next) {
        if(previous!=null && next.ticks()==previous.ticks() && next.state().equals(previous.state())) return pose;
        boolean reset=previous==null || next.ticks()<=previous.ticks()
                || !next.state().region().equals(previous.state().region());
        float dx=reset?0:next.x()-previous.x(),dy=reset?0:next.y()-previous.y();
        boolean walking=next.state().hp()>0 && dx*dx+dy*dy>0.000001;
        Direction direction=walking?DirectionalAnimation.facing(dx,dy,pose.direction()):pose.direction();
        if(reset || walking!=pose.walking() || direction!=pose.direction()) stateTick=next.ticks();
        int frame=walking?(int)(((next.ticks()-stateTick)*1000L/(30L*frameMillis))%2):0;
        pose=new Pose(direction,walking,frame);previous=next;return pose;
    }
}
