package com.projetok.studio;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.function.*;

/** Visao geral de dados reais, com navegacao pelo clique e analise opcional aproximada. */
final class MinimapPanel extends JPanel {
    final Supplier<WorldDocument> source;final Consumer<Point> center;boolean collisions=true;NavigationAnalysis.Result navigation;
    MinimapPanel(Supplier<WorldDocument> source,Consumer<Point> center){this.source=source;this.center=center;setPreferredSize(new Dimension(640,420));addMouseListener(new MouseAdapter(){public void mousePressed(MouseEvent e){var d=source.get();double scale=Math.min(getWidth()/(double)d.width(),getHeight()/(double)d.height());int x=(int)(e.getX()/scale),y=(int)(e.getY()/scale);if(x<d.width()&&y<d.height())center.accept(new Point(x,y));}});}
    @Override protected void paintComponent(Graphics original){super.paintComponent(original);var d=source.get();Graphics2D g=(Graphics2D)original.create();try{double scale=Math.min(getWidth()/(double)d.width(),getHeight()/(double)d.height());g.scale(scale,scale);g.setColor(new Color(43,70,59));g.fillRect(0,0,d.width(),d.height());if(navigation!=null){g.setColor(new Color(220,100,35,120));for(int y=0;y<navigation.rows();y++)for(int x=0;x<navigation.cols();x++)if(navigation.isolated(x,y))g.fillRect(x*navigation.cell(),y*navigation.cell(),navigation.cell(),navigation.cell());}if(collisions){g.setColor(Color.RED);for(var b:d.hitboxes())if(b.solid())g.fillRect(b.x(),b.y(),b.width(),b.height());}for(var e:d.elements()){g.setColor(switch(e.kind()){case PLAYER->Color.CYAN;case ENEMY->Color.ORANGE;case NPC->Color.YELLOW;default->Color.GRAY;});g.fillRect(e.x(),e.y(),Math.max(8,e.width()),Math.max(8,e.height()));}g.setColor(Color.MAGENTA);for(var p:d.portals())g.fillOval(p.x()-10,p.y()-10,20,20);}finally{g.dispose();}}
}
