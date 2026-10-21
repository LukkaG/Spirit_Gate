package com.projetok.prototype.rpg;
import com.projetok.prototype.LocalArenaMap;
import com.projetok.rpg.*;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/** Navegação visual dos mapas já suportados, sem inferir rotas ou revelar recursos secretos. */
final class NavigationMap extends JComponent {
    private final RpgRuntime runtime;
    private final java.util.Map<String,LocalArenaMap> maps=java.util.Map.of("village",RpgMaps.region("village"),"forest",RpgMaps.region("forest"),"ruins",RpgMaps.region("ruins"));
    private double zoom=1;
    NavigationMap(RpgRuntime runtime){this.runtime=runtime;setPreferredSize(new Dimension(190,160));getAccessibleContext().setAccessibleName("Mapa da região atual");}
    /** Expõe identificação do mapa às tecnologias assistivas do Swing. */
    @Override public javax.accessibility.AccessibleContext getAccessibleContext(){if(accessibleContext==null)accessibleContext=new AccessibleJComponent(){};return accessibleContext;}
    /** Zoom modifica apenas a projeção; colisão e movimento permanecem no mundo. */
    void zoom(double value){if(!Double.isFinite(value)||value<1||value>3)throw new IllegalArgumentException("Zoom: 1 a 3");zoom=value;repaint();}
    /** Projeta a coordenada usando os limites reais do descritor local. */
    static Point2D project(float x,float y,int width,int height,LocalArenaMap map){return new Point2D.Double(x/map.width()*width,y/map.height()*height);}
    @Override protected void paintComponent(Graphics original){
        Graphics2D g=(Graphics2D)original.create();try{
            g.setColor(SpiritTheme.BACKGROUND);g.fillRect(0,0,getWidth(),getHeight());g.setColor(SpiritTheme.ACCENT);
            var view=runtime.snapshot();var map=maps.get(view.state().region());
            if(map==null){g.drawString("Mapa não suportado",6,20);return;}
            g.drawString(RpgContent.builtin().get("region",view.state().region()).name(),6,18);
            g.drawString("● Você  ○ Portal  ◇ NPC",6,Math.max(38,getHeight()-6));
            double scale=Math.max(.001,Math.min((getWidth()-12.0)/map.width(),(getHeight()-50.0)/map.height()))*zoom;
            Graphics2D layer=(Graphics2D)g.create();try{
                layer.clipRect(0,24,getWidth(),Math.max(0,getHeight()-48));
                layer.translate(zoom==1?(getWidth()-map.width()*scale)/2:getWidth()/2.0-view.x()*scale,zoom==1?26+(getHeight()-50-map.height()*scale)/2:getHeight()/2.0-view.y()*scale);
                layer.scale(scale,scale);layer.setColor(new Color(21,60,66));layer.fillRect(0,0,map.width(),map.height());
                layer.setColor(new Color(75,107,123));for(var obstacle:map.physical())if(obstacle.category()==com.projetok.domain.movement.CollisionCategory.SOLID_OBSTACLE)layer.fill(new Rectangle2D.Float(obstacle.x(),obstacle.y(),obstacle.width(),obstacle.height()));
                layer.setColor(SpiritTheme.ACCENT);for(var portal:view.portals())layer.draw(new Ellipse2D.Float(portal.x()-15,portal.y()-15,30,30));
                layer.setColor(new Color(250,210,120));for(var npc:view.npcs())layer.fill(new Rectangle2D.Float(npc.x()-9,npc.y()-9,18,18));
                layer.setColor(Color.WHITE);layer.fill(new Ellipse2D.Float(view.x()-12,view.y()-12,24,24));
            }finally{layer.dispose();}
        }finally{g.dispose();}
    }
}
