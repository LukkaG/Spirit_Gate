package com.projetok.prototype;

import com.projetok.domain.movement.RpgSessionGeometry.Structure;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/** Opt-in depth pass for session structures. The old map/layer pipelines remain default. */
public final class StructureRenderer {
    public record Entry(float supportY,Structure structure,DebugSnapshot.ActorView actor){
        public boolean object(){return structure!=null;}
    }
    private StructureRenderer(){}
    public static List<Entry> order(List<Structure> structures,List<DebugSnapshot.ActorView> actors){
        var entries=new ArrayList<Entry>();
        structures.forEach(s->entries.add(new Entry(s.supportY(),s,null)));
        actors.forEach(a->entries.add(new Entry(a.y(),null,a)));
        entries.sort(Comparator.comparingDouble(Entry::supportY).thenComparingInt(e->e.object()?0:1)
                .thenComparingInt(e->e.actor()==null?0:e.actor().id()));
        return List.copyOf(entries);
    }
    public static void draw(Graphics2D original,List<Structure> structures,List<DebugSnapshot.ActorView> actors,
                            Consumer<DebugSnapshot.ActorView> drawActor){
        var g=(Graphics2D)original.create();
        try{
            for(var s:structures){var p=s.footprint();g.setColor(new Color(0,0,0,65));g.fillRoundRect(Math.round(p.x()+8),Math.round(p.y()+10),Math.round(p.width()),Math.round(p.height()),18,18);}
            for(var entry:order(structures,actors)){
                if(entry.object())drawObject(g,entry.structure());else drawActor.accept(entry.actor());
            }
        }finally{g.dispose();}
    }
    private static void drawObject(Graphics2D g,Structure structure){
        var p=structure.footprint();int x=Math.round(p.x()),y=Math.round(structure.visualTop());
        int w=Math.round(p.width()),h=Math.round(p.height()+structure.projectionHeight());
        g.setPaint(new GradientPaint(x,y,new Color(86,107,116),x+w,y+h,new Color(39,60,78)));
        g.fillRoundRect(x,y,w,h,18,18);g.setColor(new Color(127,172,172));g.setStroke(new BasicStroke(3));g.drawRoundRect(x,y,w,h,18,18);
        g.setColor(new Color(192,226,218,56));for(int yy=y+22;yy<y+h-10;yy+=32)g.drawLine(x+18,yy,x+w-18,yy);
    }
}
