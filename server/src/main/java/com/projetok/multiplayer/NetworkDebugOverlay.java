package com.projetok.multiplayer;

import com.projetok.domain.movement.CharacterFootprint;
import com.projetok.prototype.rpg.RpgPlayerSprites;
import java.awt.*;
import java.awt.geom.*;

/** Presentation only; resident geometry, no IO and no collision recalculation. */
public final class NetworkDebugOverlay {
    private NetworkDebugOverlay(){}
    public static void draw(Graphics2D original,Protocol.State authoritative,Protocol.State display,
                            RpgPlayerSprites sprites,RemoteSpriteAnimation animation){draw(original,authoritative,display,sprites,animation,com.projetok.domain.movement.SessionCollisionProfile.DEFAULT,com.projetok.domain.movement.RpgSessionGeometry.structures());}
    public static void draw(Graphics2D original,Protocol.State authoritative,Protocol.State display,RpgPlayerSprites sprites,RemoteSpriteAnimation animation,com.projetok.domain.movement.SessionCollisionProfile profile,java.util.List<com.projetok.domain.movement.RpgSessionGeometry.Structure> structures){
        if(authoritative==null)return;var g=(Graphics2D)original.create();
        try{
            g.setStroke(new BasicStroke(1));
            for(var structure:structures){
                var raw=structure.footprint();float padding=profile.sceneryHalfWidth()-profile.actorRadius();var effective=new com.projetok.domain.movement.CollisionService.Obstacle(raw.x()-padding,raw.y(),raw.width()+2*padding,raw.height());
                g.setColor(Color.ORANGE);g.draw(new Rectangle2D.Float(raw.x(),raw.y(),raw.width(),raw.height()));
                g.setColor(Color.YELLOW);g.draw(new Rectangle2D.Float(effective.x(),effective.y(),effective.width(),effective.height()));
                g.setColor(Color.BLUE);g.draw(new Line2D.Float(raw.x(),structure.supportY(),raw.x()+raw.width(),structure.supportY()));
                g.drawString("depth="+structure.supportY(),raw.x(),structure.supportY()+16);
            }
            for(var a:authoritative.actors()){
                if(a.boss())continue;
                var v=display.actors().stream().filter(p->p.id()==a.id()).findFirst().orElse(a);
                double r=profile.actorRadius();
                g.setColor(Color.RED);g.draw(new Ellipse2D.Double(a.x()-r,a.y()-r,r*2,r*2));
                g.draw(new Line2D.Float(a.x(),a.y(),v.x(),v.y()));
                g.setColor(Color.GREEN);g.draw(new Ellipse2D.Double(v.x()-r,v.y()-r,r*2,r*2));
                double halfSegment=profile.sceneryHalfWidth()-profile.actorRadius();
                g.setColor(new Color(255,165,0));g.draw(new RoundRectangle2D.Double(v.x()-r-halfSegment,v.y()-r,2*(r+halfSegment),2*r,2*r,2*r));
                g.setColor(Color.CYAN);g.draw(new Line2D.Float(v.x()-4,v.y(),v.x()+4,v.y()));g.draw(new Line2D.Float(v.x(),v.y()-4,v.x(),v.y()+4));
                if(sprites!=null){var pose=animation.pose(a.id(),System.nanoTime()/1_000_000);g.setColor(Color.MAGENTA);g.draw(sprites.visualBounds(pose.direction(),pose.frame(),pose.walking(),v.x(),v.y()));}
            }
            g.setColor(Color.WHITE);g.drawString("F2 | sprite: magenta | pivot: cyan | body: green/red | scenery: orange | padded: yellow | depth: blue",12,22);
        }finally{g.dispose();}
    }
}
