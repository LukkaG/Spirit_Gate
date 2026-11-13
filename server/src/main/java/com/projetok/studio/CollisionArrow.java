package com.projetok.studio;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

/** Seta vetorial de edição: desenho estável entre fontes, sem modificar os pixels dos tiles. */
final class CollisionArrow implements Icon {
    private final int side;
    private final int size;
    CollisionArrow(int side,int size){this.side=side;this.size=size;}
    @Override public int getIconWidth(){return size;}
    @Override public int getIconHeight(){return size;}
    @Override public void paintIcon(Component c,Graphics graphics,int x,int y){
        draw((Graphics2D)graphics,x+size/2d,y+size/2d,size,side,c instanceof AbstractButton b&&b.isSelected());
    }
    /** Desenha uma ponta larga e haste curta, com sombra, borda clara e estado por preenchimento. */
    static void draw(Graphics2D original,double cx,double cy,double size,int side,boolean blocked){
        var g=(Graphics2D)original.create();
        try{
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.translate(cx,cy);g.rotate(side==WorldDocument.Hitbox.RIGHT?Math.PI/2:side==WorldDocument.Hitbox.DOWN?Math.PI:side==WorldDocument.Hitbox.LEFT?-Math.PI/2:0);
            g.scale(size,size);
            var path=new Path2D.Double();path.moveTo(0,-.4);path.lineTo(.34,-.04);path.lineTo(.14,-.04);path.lineTo(.14,.34);path.quadTo(.14,.4,.08,.4);path.lineTo(-.08,.4);path.quadTo(-.14,.4,-.14,.34);path.lineTo(-.14,-.04);path.lineTo(-.34,-.04);path.closePath();
            g.setStroke(new BasicStroke(.13f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setColor(new Color(2,12,23,blocked?205:90));g.draw(path);
            g.setPaint(blocked?new GradientPaint(0,-.4f,new Color(255,196,136),0,.4f,new Color(244,94,85)):new Color(24,71,86,40));g.fill(path);
            g.setStroke(new BasicStroke(.055f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.setColor(blocked?new Color(255,236,200):new Color(116,226,243,110));g.draw(path);
        }finally{g.dispose();}
    }
}
