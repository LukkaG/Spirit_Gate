package com.projetok.multiplayer;

import java.awt.*;
import java.util.*;
import java.util.List;

/** Efeitos derivados de confirmações do servidor; nunca enviam comandos ou dano. */
public final class NetworkCombatPresentation {
    public enum Phase { IDLE, START, STRIKE, RECOVER, HIT, DEAD }
    private record Track(int hp,long attack,long hit,long death,int damage,float x,float y) { }
    private final Map<Integer,Track> tracks=new HashMap<>();
    private long tick=-1;
    private long eventSequence;
    private int target;
    private boolean explicitSelection;
    public void selection(int id){explicitSelection=true;target=id;}
    private boolean reducedMotion;
    public void reducedMotion(boolean value){reducedMotion=value;}
    public void update(Protocol.State state,List<Protocol.CombatEvent> events,int self) {
        if(state.tick()<=tick)return;
        for(var actor:state.actors()) {
            var old=tracks.get(actor.id());
            boolean attack=(actor.flags()&1)!=0;
            boolean start=attack&&(old==null||state.tick()-old.attack()>=12);
            int damage=old==null?0:Math.max(0,old.hp()-actor.hp());
            tracks.put(actor.id(),new Track(actor.hp(),start?state.tick():old==null?-100:old.attack(),
                damage>0?state.tick():old==null||old.hp()==0?-100:old.hit(),actor.hp()==0?(old==null||old.hp()>0?state.tick():old.death()):-100,
                damage>0?damage:old==null?0:old.damage(),actor.x(),actor.y()));
        }
        tracks.keySet().retainAll(state.actors().stream().map(Protocol.Actor::id).toList());
        for(var event:events)if(event.sequence()>eventSequence) {
            eventSequence=event.sequence();
            if(event.kind().equals("HIT")){var attacker=tracks.get(event.player());if(attacker!=null)tracks.put(event.player(),new Track(attacker.hp(),event.tick(),attacker.hit(),attacker.death(),attacker.damage(),attacker.x(),attacker.y()));}
            if(!explicitSelection&&event.player()==self&&event.kind().equals("HIT"))target=event.enemy();
        }
        if(!tracks.containsKey(target)||target>1_000_000&&tracks.get(target).hp()==0)target=0;
        tick=state.tick();
    }
    public Phase phase(int actor) {
        var t=tracks.get(actor);if(t==null)return Phase.IDLE;
        if(t.hp()==0)return Phase.DEAD;
        if(tick-t.hit()<6)return Phase.HIT;
        long elapsed=tick-t.attack();
        return elapsed<2?Phase.START:elapsed<5?Phase.STRIKE:elapsed<12?Phase.RECOVER:Phase.IDLE;
    }
    public int target(){return target;}
    /** Sobreposição em coordenadas do mundo, limitada aos atores presentes na região. */
    public void draw(Graphics2D original) {
        var g=(Graphics2D)original.create();
        try {
            g.setStroke(new BasicStroke(2));
            for(var entry:tracks.entrySet()) {
                var t=entry.getValue();int x=Math.round(t.x()),y=Math.round(t.y());
                var phase=phase(entry.getKey());
                if(phase==Phase.START||phase==Phase.STRIKE||phase==Phase.RECOVER) {
                    g.setColor(phase==Phase.STRIKE?GameHudStyle.CYAN:new Color(112,225,244,100));
                    g.drawArc(x-28,y-44,56,48,20,phase==Phase.STRIKE?260:90);
                }
                if(tick-t.hit()<24) {
                    String value="-"+t.damage();int textY=y-60-(reducedMotion?0:(int)(tick-t.hit()));g.setFont(new Font("SansSerif",Font.BOLD,13));g.setColor(new Color(3,12,25,230));g.drawString(value,x-7,textY+1);g.setColor(new Color(255,159,177));g.drawString(value,x-8,textY);
                    if(phase==Phase.HIT)g.drawOval(x-22,y-42,44,40);
                }
                if(phase==Phase.DEAD) {
                    g.setColor(new Color(180,160,170));g.drawLine(x-12,y-16,x+12,y+8);g.drawLine(x+12,y-16,x-12,y+8);
                    g.drawString("Derrotado",x-28,y-23);
                }
                if(entry.getKey()==target){g.setColor(target<=1_000_000?new Color(130,240,183):GameHudStyle.CYAN);g.drawOval(x-24,y-8,48,16);g.drawLine(x-31,y,x-26,y);g.drawLine(x+26,y,x+31,y);}
            }
        } finally {g.dispose();}
    }
}
