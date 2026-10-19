package com.projetok.prototype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Sequenciador visual exclusivo da simulacao; nao altera entidades nem usa relogio real. */
public final class DirectionalAnimation {
    public enum State { IDLE, WALK, RUN, ATTACK, HIT, FAINT }
    public enum Direction { FRONT, BACK, LEFT, RIGHT }
    /** Sinal de combate emitido no ponto real da simulacao, nunca inferido da cor. */
    public record Signal(int actorId, State state) { }
    /** Apenas valores imutaveis sao entregues ao renderizador. */
    public record View(int actorId, State state, Direction direction, int frame) { }
    /** Publicacao atomica do mundo e de seus estados visuais correspondentes. */
    public record Presentation(DebugSnapshot world, List<View> animations) {
        public Presentation { animations = List.copyOf(animations); }
    }
    private record Track(DebugSnapshot.ActorView actor, State state, Direction direction, double elapsed) { }
    private final Map<Integer, Track> tracks = new HashMap<>();

    /** Descarta facing e tempos ao recriar o mundo por R. */
    public void reset() { tracks.clear(); }

    /** Avanca exclusivamente pelo dt recebido; dt zero pausa a apresentacao. */
    public Presentation advance(DebugSnapshot world, List<Signal> signals, double dt) {
        if (!Double.isFinite(dt) || dt < 0 || dt > .1) throw new IllegalArgumentException("dt visual invalido");
        List<View> result = new ArrayList<>(world.actors().size());
        for (var actor : world.actors()) {
            Track old = tracks.get(actor.id());
            double dx = old == null ? 0 : actor.x()-old.actor().x();
            double dy = old == null ? 0 : actor.y()-old.actor().y();
            Direction direction = facing(dx, dy, old == null ? Direction.FRONT : old.direction());
            if (actor.player()) direction = facing(world.facingX(), world.facingY(), direction);
            boolean frozen = world.statuses().stream().anyMatch(s -> s.actorId()==actor.id() && s.freezeRemaining()>0);
            State state = dx*dx+dy*dy > .000001 ? State.WALK : State.IDLE;
            if (old != null && (old.state()==State.HIT || old.state()==State.ATTACK) && old.elapsed()+dt < duration(old.state())) state=old.state();
            boolean triggered = false;
            for (Signal signal : signals) if (signal.actorId()==actor.id()) {
                if (signal.state()==State.HIT || state!=State.HIT) state=signal.state();
                triggered=true;
            }
            if (!actor.alive()) { state=State.FAINT; direction=Direction.FRONT; }
            else if (frozen && old != null && !triggered) state=old.state();
            double elapsed = old == null || old.state()!=state || old.direction()!=direction || triggered ? 0 : old.elapsed()+(frozen ? 0 : dt);
            Track next = new Track(actor,state,direction,elapsed);
            tracks.put(actor.id(),next);
            result.add(new View(actor.id(),state,direction,frame(state,elapsed)));
        }
        tracks.keySet().retainAll(world.actors().stream().map(DebugSnapshot.ActorView::id).toList());
        return new Presentation(world,result);
    }

    /** Eixo dominante, empate vertical estavel e ultimo facing quando parado. */
    public static Direction facing(double x, double y, Direction previous) {
        if (x*x+y*y < .000001) return previous;
        return Math.abs(x)>Math.abs(y) ? (x>0 ? Direction.RIGHT : Direction.LEFT) : (y>0 ? Direction.FRONT : Direction.BACK);
    }
    public static int count(State state) {
        return switch(state) { case IDLE -> 1; case WALK, RUN -> 4; case ATTACK, FAINT -> 3; case HIT -> 2; };
    }
    public static double duration(State state) { return count(state)*.1; }
    private static int frame(State state, double elapsed) {
        int index=(int)Math.floor((elapsed+1e-8)/.1);
        return state==State.FAINT || state==State.HIT || state==State.ATTACK ? Math.min(count(state)-1,index) : index%count(state);
    }
}
