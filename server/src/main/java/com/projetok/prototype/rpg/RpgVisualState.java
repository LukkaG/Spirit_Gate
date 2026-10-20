package com.projetok.prototype.rpg;

import com.projetok.rpg.*;

/** Coordena estados visuais por ticks e eventos reais, sem modificar a simulacao. */
public final class RpgVisualState {
    /** RUN permanece indisponivel sem arte e regra de deslocamento correspondentes. */
    public enum State { IDLE, WALK, ATTACK, HIT, KO }
    private State state=State.IDLE;
    private long sequence,until,lastTick=-1;
    private String region="";
    /** Atualiza uma vez por snapshot; derrota e dano interrompem ataque. */
    public State update(RpgWorld.View view,boolean moving) {
        if(view.ticks()<lastTick||!view.state().region().equals(region)){
            sequence=lastTick<0?0:view.events().stream().mapToLong(RpgCombatEvent::sequence).max().orElse(0);
            until=0;state=State.IDLE;
        }
        region=view.state().region();lastTick=view.ticks();
        if(view.state().hp()==0){state=State.KO;sequence=view.events().stream().mapToLong(RpgCombatEvent::sequence).max().orElse(sequence);return state;}
        if(state==State.KO||view.ticks()>=until)state=moving?State.WALK:State.IDLE;
        for(var event:view.events())if(event.sequence()>sequence) {
            sequence=event.sequence();
            if(event.kind()==RpgCombatEvent.Kind.HIT&&event.target()==1){state=State.HIT;until=event.tick()+6;}
            else if(event.kind()==RpgCombatEvent.Kind.ATTACK&&state!=State.HIT){state=State.ATTACK;until=event.tick()+9;}
        }
        if(view.ticks()>=until&&state!=State.KO)state=moving?State.WALK:State.IDLE;
        return state;
    }
    /** Estado de apresentacao atual, independente de repaint. */
    public State state(){return state;}
}
