package com.projetok.prototype.rpg;

import com.projetok.rpg.*;
import java.awt.*;
import java.util.*;

/** Efeitos provisórios limitados, derivados de eventos reais e independentes do repaint. */
public final class RpgCombatFeedback {
    private final java.util.List<RpgCombatEvent> active=new ArrayList<>();
    private long sequence,tick=-1;
    private String region="";
    private record Ray(float x,float y,boolean bow) { }
    private final Map<Long,Ray> rays=new HashMap<>();
    /** Consome cada sequencia uma vez; ignora eventos antigos e limpa transicoes. */
    public void update(RpgWorld.View view) {
        if(tick>=0&&(view.ticks()<tick||!region.equals(view.state().region()))) {
            active.clear();rays.clear();sequence=view.events().stream().mapToLong(RpgCombatEvent::sequence).max().orElse(0);
        }
        tick=view.ticks();region=view.state().region();
        for(var event:view.events())if(event.sequence()>sequence) {
            sequence=event.sequence();if(tick-event.tick()<30){
                active.add(event);
                var domain=DomainProgression.read(view.state().flags());
                boolean bow=domain.domain().startsWith("bow_");
                if(event.kind()==RpgCombatEvent.Kind.ATTACK&&(bow||view.identity().race().equals("spirit")))
                    rays.put(event.sequence(),new Ray(view.x(),view.y(),bow));
            }
        }
        active.removeIf(e->tick-e.tick()>=30);
        while(active.size()>64)active.remove(0);
        rays.keySet().retainAll(active.stream().map(RpgCombatEvent::sequence).toList());
    }
    /** Copia imutavel para validacao e apresentacao. */
    public java.util.List<RpgCombatEvent> active(){return java.util.List.copyOf(active);}
    /** Desenha somente feedback; nao acessa nem altera o mundo. */
    public void draw(Graphics2D original) {
        Graphics2D g=(Graphics2D)original.create();
        try {
            g.setFont(new Font("SansSerif",Font.BOLD,14));
            for(var e:active) {
                int age=(int)(tick-e.tick()),x=Math.round(e.x()),y=Math.round(e.y());
                g.setColor(e.kind()==RpgCombatEvent.Kind.HEAL?new Color(90,240,150):e.kind()==RpgCombatEvent.Kind.REJECT?new Color(230,210,150):new Color(255,120,105));
                String text=switch(e.kind()){case HIT->"-"+e.amount();case HEAL->"+"+e.amount();case KO->"Derrotado";case REJECT->"Indisponivel";case ATTACK,PUNCH->"";};
                if(!text.isEmpty())g.drawString(text,x-12,y-50-age);
                if(age<6&&e.kind()==RpgCombatEvent.Kind.HIT){g.setStroke(new BasicStroke(2));g.drawOval(x-24,y-24,48,48);}
                var ray=rays.get(e.sequence());
                if(age<6&&ray!=null){
                    g.setColor(ray.bow()?new Color(120,220,255,210):new Color(255,250,200,220));
                    g.setStroke(new BasicStroke(ray.bow()?2:5));
                    g.draw(new java.awt.geom.Line2D.Float(ray.x(),ray.y()-12,x,y-12));
                }
            }
        } finally {g.dispose();}
    }
}
