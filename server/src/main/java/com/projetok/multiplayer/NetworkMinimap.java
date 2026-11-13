package com.projetok.multiplayer;

import com.projetok.studio.WorldContent;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

/** Projeção passiva da região recebida, sem descobrir entidades ou calcular física local. */
public final class NetworkMinimap extends JComponent {
    private Protocol.State snapshot;
    private WorldContent.Prepared content;
    private String region="forest";
    private int self;
    private Rectangle2D visible;
    public NetworkMinimap(){setName("hudMinimap");setToolTipText("Mapa da região atual — M para mostrar ou ocultar");getAccessibleContext().setAccessibleName("Minimapa da região recebida do servidor");}
    @Override public javax.accessibility.AccessibleContext getAccessibleContext(){if(accessibleContext==null)accessibleContext=new AccessibleJComponent(){};return accessibleContext;}
    public void refresh(Protocol.State snapshot,int self,String region,WorldContent.Prepared content,Rectangle2D visible){this.snapshot=snapshot;this.self=self;this.region=region;this.content=content;this.visible=visible==null?null:(Rectangle2D)visible.clone();repaint();}
    public static Point2D project(double x,double y,int width,int height,int mapWidth,int mapHeight){
        if(mapWidth<1||mapHeight<1||width<1||height<1)throw new IllegalArgumentException("Projeção inválida");
        return new Point2D.Double(x*width/mapWidth,y*height/mapHeight);
    }
    @Override protected void paintComponent(Graphics original){var g=GameHudStyle.graphics(original);try{
        GameHudStyle.panel(g,getWidth(),getHeight(),14);
        g.setFont(new Font("SansSerif",Font.BOLD,10));g.setColor(GameHudStyle.MUTED);g.drawString("MAPA DA REGIÃO",14,21);
        g.setColor(GameHudStyle.CYAN);g.drawString("N",getWidth()-23,21);
        var document=content==null?null:content.revision().bundle().maps().values().stream().filter(d->d.region().equals(region)).findFirst().orElse(null);
        if(document==null){g.setFont(new Font("SansSerif",Font.PLAIN,11));g.drawString("Aguardando mapa recebido",14,58);return;}
        int availableW=Math.max(1,getWidth()-26),availableH=Math.max(1,getHeight()-80);
        double scale=Math.min(availableW/(double)document.width(),availableH/(double)document.height());
        int w=Math.max(1,(int)Math.floor(document.width()*scale)),h=Math.max(1,(int)Math.floor(document.height()*scale));
        int top=33+(availableH-h)/2,left=(getWidth()-w)/2;
        var map=(Graphics2D)g.create();try {
            map.translate(left,top);map.clipRect(0,0,w,h);
            map.setColor(new Color(10,31,43));map.fillRect(0,0,w,h);
            double sx=w/(double)document.width(),sy=h/(double)document.height();
            map.setColor(new Color(70,112,128,50));for(int i=1;i<6;i++){map.drawLine(w*i/6,0,w*i/6,h);map.drawLine(0,h*i/6,w,h*i/6);}
            map.setColor(new Color(59,92,110));for(var hitbox:document.hitboxes())if(hitbox.solid())map.fill(new Rectangle2D.Double(hitbox.x()*sx,hitbox.y()*sy,hitbox.width()*sx,hitbox.height()*sy));
            if(visible!=null){map.setColor(new Color(112,225,244,18));map.fill(new Rectangle2D.Double(visible.getX()*sx,visible.getY()*sy,visible.getWidth()*sx,visible.getHeight()*sy));map.setColor(new Color(112,225,244,90));map.draw(new Rectangle2D.Double(visible.getX()*sx,visible.getY()*sy,visible.getWidth()*sx,visible.getHeight()*sy));}
            map.setColor(GameHudStyle.CYAN);for(var portal:document.portals())map.draw(new Ellipse2D.Double(portal.x()*sx-3,portal.y()*sy-3,6,6));
            if(snapshot!=null)for(var actor:snapshot.actors())if(actor.hp()>0){
                double x=actor.x()*sx,y=actor.y()*sy;
                if(actor.id()==self){map.setColor(new Color(112,225,244,40));map.fill(new Ellipse2D.Double(x-8,y-8,16,16));map.setColor(GameHudStyle.CYAN);map.fill(new Ellipse2D.Double(x-3,y-3,6,6));map.draw(new Line2D.Double(x,y,x+actor.facingX()*7,y+actor.facingY()*7));}
                else if(!actor.boss()){map.setColor(Color.WHITE);map.fill(new Ellipse2D.Double(x-2,y-2,4,4));}
                else if(actor.id()>=2_000_000){map.setColor(new Color(230,203,145));map.fill(new Rectangle2D.Double(x-2,y-2,4,4));}
                else {map.setColor(new Color(235,129,143));map.fill(new Ellipse2D.Double(x-2,y-2,4,4));}
            }
        }finally{map.dispose();}
        g.setFont(new Font("SansSerif",Font.PLAIN,10));g.setColor(GameHudStyle.TEXT);String name=document.name();
        while(name.length()>3&&g.getFontMetrics().stringWidth(name)>getWidth()-30)name=name.substring(0,name.length()-2);
        g.drawString(name,14,getHeight()-25);
        g.setFont(new Font("SansSerif",Font.PLAIN,9));g.setColor(GameHudStyle.CYAN);g.fillOval(14,getHeight()-16,4,4);g.setColor(GameHudStyle.MUTED);g.drawString("Você",22,getHeight()-11);g.setColor(Color.WHITE);g.fillOval(57,getHeight()-16,4,4);g.setColor(GameHudStyle.MUTED);g.drawString("Aliado",65,getHeight()-11);g.setColor(new Color(235,129,143));g.fillOval(110,getHeight()-16,4,4);g.setColor(GameHudStyle.MUTED);g.drawString("Inimigo",118,getHeight()-11);
    }finally{g.dispose();}}
}
