package com.projetok.multiplayer;

import java.util.ArrayDeque;

/** Tick-ordered bounded visual history. No extrapolation, no authoritative mutation. */
public final class RemoteTimeline {
    private final ArrayDeque<Protocol.State> states=new ArrayDeque<>();
    private final NetworkDiagnostics diagnostics;
    private long receipt;private String region="";
    private double targetTick=-1,bufferMillis=100;
    private final int override=Integer.getInteger("spiritgate.network.bufferMillis",0);
    public long corrections,dropped;
    public long obstacleCorrections;
    private final com.projetok.domain.movement.CollisionService collision=new com.projetok.domain.movement.CollisionService();
    private final com.projetok.domain.entity.GameEntity visualProbe=new com.projetok.domain.entity.GameEntity(1,"Presentation",new com.projetok.domain.entity.Position(140,300)){};
    private record WallCorrection(long tick,Protocol.Actor actor){}
    private final java.util.Map<Integer,WallCorrection> wallCorrections=new java.util.HashMap<>();
    public RemoteTimeline(NetworkDiagnostics diagnostics){this.diagnostics=diagnostics;if(override!=0&&(override<50||override>150))throw new IllegalArgumentException("bufferMillis: 50..150 or 0 (adaptive)");}
    public boolean offer(Protocol.State state,long received,String nextRegion){
        var last=states.peekLast();
        if(last!=null&&state.tick()<=last.tick()){dropped++;return false;}
        boolean reset=last==null||!region.equals(nextRegion)||state.tick()-last.tick()>15;
        if(reset){states.clear();wallCorrections.clear();targetTick=-1;if(last!=null)corrections++;}
        states.addLast(state);while(states.size()>20)states.removeFirst();
        receipt=received;region=nextRegion;
        double interval=diagnostics.snapshotIntervals.percentileMillis(.5);
        double p95=diagnostics.snapshotIntervals.percentileMillis(.95);
        if(override!=0)bufferMillis=override;
        else if(Double.isFinite(interval))bufferMillis=Math.max(50,Math.min(150,interval*.75+Math.min(40,Math.max(0,p95-interval))));
        return true;
    }
    public double bufferMillis(){return bufferMillis;}
    public Protocol.State sample(long now,int localId){
        var newest=states.peekLast();if(newest==null)return null;
        double requested=newest.tick()+(Math.max(0,now-receipt)/1e6-bufferMillis)*.03;
        targetTick=Math.min(newest.tick(),Math.max(targetTick,requested));
        Protocol.State before=states.peekFirst(),after=before;
        for(var state:states){after=state;if(state.tick()>=targetTick)break;before=state;}
        double alpha=after.tick()==before.tick()?1:Math.max(0,Math.min(1,(targetTick-before.tick())/(after.tick()-before.tick())));
        var visual=new RemotePresentation(before,after).sample(alpha,localId);
        wallCorrections.keySet().removeIf(id->newest.actors().stream().noneMatch(a->a.id()==id));
        // Local actor is always the newest authoritative state, even when remote history is older.
        return new Protocol.State(newest.tick(),newest.actors().stream().map(a->{
            if(a.id()==localId)return a;
            var old=visual.actors().stream().filter(v->v.id()==a.id()).findFirst().orElse(a);
            // Death, removal and teleports never wait in the history buffer.
            if(old.hp()==0||a.hp()==0||Math.hypot(old.x()-a.x(),old.y()-a.y())>50){wallCorrections.remove(a.id());return a;}
            var held=wallCorrections.get(a.id());
            if(held!=null&&targetTick<held.tick())return at(a,held.actor().x(),held.actor().y());
            wallCorrections.remove(a.id());
            if(!a.boss()&&!collision.isFree(visualProbe,old.x(),old.y(),
                    com.projetok.domain.movement.RpgCollisionProfile.PLAYER_RADIUS,
                    com.projetok.domain.movement.RpgSessionGeometry.player())){obstacleCorrections++;wallCorrections.put(a.id(),new WallCorrection(newest.tick(),a));return a;}
            return old;
        }).toList());
    }
    private static Protocol.Actor at(Protocol.Actor a,float x,float y){return new Protocol.Actor(a.id(),a.name(),x,y,a.hp(),a.maxHp(),a.stamina(),a.coins(),a.boss(),a.facingX(),a.facingY(),a.flags());}
}
