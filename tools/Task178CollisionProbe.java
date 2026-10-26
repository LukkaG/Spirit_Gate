package com.projetok.multiplayer;
import com.projetok.prototype.*;
import com.projetok.prototype.rpg.*;
import java.awt.*;
import java.nio.file.*;
import java.util.*;
/** Same art/positions before and after; component render, not human approval. */
public class Task178CollisionProbe {
    public static void main(String[] args)throws Exception{
        Path out=Path.of(args[0]);Files.createDirectories(out);String phase=args[1];
        var sprites=RpgPlayerSprites.load(Path.of("assets/dev/player-base"),250,.5);
        float side=phase.equals("before")?368:358;
        for(var entry:java.util.Map.of("left",new float[]{side,240},"right",new float[]{phase.equals("before")?512:522,240},"behind",new float[]{440,168},"front",new float[]{440,312}).entrySet()){
            var p=entry.getValue();var actor=new Protocol.Actor(1,"Regressao",p[0],p[1],100,100,1000,0,false);
            var state=new Protocol.State(1,java.util.List.of(actor));
            var panel=new DebugPanel(()->NetworkClientLauncher.adapt(state,1,DebugWorld.demo().snapshot()));panel.setSize(1260,750);
            panel.setActorRenderer((g,a)->sprites.drawFrame(g,DirectionalAnimation.Direction.LEFT,2,true,a.x(),a.y()));
            if(!phase.equals("before")){
                // Reflective opt-in keeps the probe compilable against the original checkout.
                var geometry=Class.forName("com.projetok.domain.movement.RpgSessionGeometry");
                panel.getClass().getMethod("setDepthStructures",java.util.List.class).invoke(panel,geometry.getMethod("structures").invoke(null));
            }
            var image=new java.awt.image.BufferedImage(1260,750,2);var g=image.createGraphics();panel.printAll(g);g.dispose();
            javax.imageio.ImageIO.write(image,"png",out.resolve(phase+"-"+entry.getKey()+".png").toFile());
            var bounds=sprites.visualBounds(DirectionalAnimation.Direction.LEFT,2,true,p[0],p[1]);
            System.out.printf(Locale.ROOT,"%s %s logical=(%.1f,%.1f) baseAlpha=[%.1f,%.1f..%.1f,%.1f] footprint=12%n",phase,entry.getKey(),p[0],p[1],bounds.getMinX(),bounds.getMinY(),bounds.getMaxX(),bounds.getMaxY());
        }
    }
}
