package com.projetok.studio;

import com.projetok.rpg.RpgWorld;
import java.awt.*;
import java.awt.geom.*;
import java.util.function.Supplier;
import javax.swing.*;

/** Minimapa exclusivamente do descritor autorado atual; nunca desenha o mapa legado. */
final class StudioMiniMap extends JComponent {
    private final Supplier<WorldDocument> document;
    private final Supplier<RpgWorld.View> world;
    StudioMiniMap(Supplier<WorldDocument> document,Supplier<RpgWorld.View> world){this.document=document;this.world=world;setPreferredSize(new Dimension(200,180));setToolTipText("Mapa autorado: jogador, sólidos e portais desta região");}
    static Point2D.Double project(double x,double y,int width,int height,WorldDocument d){return new Point2D.Double(x/d.width()*width,y/d.height()*height);}
    @Override protected void paintComponent(Graphics original){
        var g=(Graphics2D)original.create();try{
            g.setColor(StudioTheme.FIELD);g.fillRect(0,0,getWidth(),getHeight());var d=document.get();var v=world.get();g.setColor(StudioTheme.TEXT);
            if(d==null||!d.region().equals(v.state().region())){g.drawString("Descritor indisponível",8,20);return;}
            g.drawString(d.name(),8,18);g.drawString(d.region(),8,35);double scale=Math.max(.001,Math.min((getWidth()-16d)/d.width(),(getHeight()-58d)/d.height()));
            g.translate(8,45);g.scale(scale,scale);g.setColor(new Color(30,65,63));g.fillRect(0,0,d.width(),d.height());
            g.setColor(new Color(122,145,158));for(var h:d.hitboxes())if(h.solid())g.fillRect(h.x(),h.y(),h.width(),h.height());
            g.setStroke(new BasicStroke((float)(1/scale)));g.setColor(Color.MAGENTA);for(var p:d.portals())g.draw(new Ellipse2D.Double(p.x()-10/scale,p.y()-10/scale,20/scale,20/scale));
            g.setColor(StudioTheme.ACCENT);g.fill(new Ellipse2D.Double(v.x()-3/scale,v.y()-3/scale,6/scale,6/scale));
        }finally{g.dispose();}
    }
}
