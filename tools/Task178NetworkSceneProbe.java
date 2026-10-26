package com.projetok.multiplayer;
import com.projetok.prototype.rpg.*;
import com.projetok.prototype.DebugPanel;
import java.awt.event.ActionEvent;
import java.net.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import javax.swing.*;

/** Real server/two clients, input bindings only; no authoritative position injection. */
public class Task178NetworkSceneProbe {
    static void key(JFrame frame,String action)throws Exception{SwingUtilities.invokeAndWait(()->{var p=(DebugPanel)frame.getRootPane().getClientProperty("networkPanel");p.getActionMap().get(action).actionPerformed(new ActionEvent(p,0,action));});}
    public static void main(String[] args)throws Exception{
        Path out=Path.of(args[0]);Files.createDirectories(out);JFrame[] frames=new JFrame[2];
        var sprites=RpgPlayerSprites.load(Path.of("assets/dev/player-base"),250,.5);
        var catalog=CosmeticCatalog.load(Path.of("assets/dev/generated/corujao-iv-v1"));
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,new RpgSessionWorld(List.of("reaper","human"),catalog,true,20))){
            server.start();try(var a=LocalClient.combat(InetAddress.getLoopbackAddress(),server.port(),"Scene A",false);var b=LocalClient.combat(InetAddress.getLoopbackAddress(),server.port(),"Scene B",false)){
                a.await(s->s.actors().size()==3,Duration.ofSeconds(5));
                SwingUtilities.invokeAndWait(()->{frames[0]=RpgNetworkClientLauncher.open(a,sprites,"localhost",catalog);frames[1]=RpgNetworkClientLauncher.open(b,sprites,"localhost",catalog,new CosmeticCatalog.Selection("hair_spiky","light_armor"));});
                key(frames[0],"SPACE");a.await(s->s.actors().stream().anyMatch(v->v.boss()&&v.hp()==0),Duration.ofSeconds(3));
                key(frames[0],"Wtrue");Thread.sleep(250);key(frames[0],"Wfalse");
                key(frames[0],"Dtrue");key(frames[1],"Dtrue");Thread.sleep(1400);key(frames[0],"Dfalse");key(frames[1],"Dfalse");
                key(frames[1],"Wtrue");Thread.sleep(600);key(frames[1],"Wfalse");Thread.sleep(250);
                var one=a.snapshot().actors().stream().filter(v->v.id()==a.id()).findFirst().orElseThrow();
                if(Math.abs(one.x()-358)>.01)throw new AssertionError("Expected authoritative side contact at 358, got "+one);
                if(server.failure()!=null)throw new AssertionError(server.failure());
                SwingUtilities.invokeAndWait(()->{try{for(int i=0;i<2;i++){var frame=frames[i];var image=new java.awt.image.BufferedImage(frame.getWidth(),frame.getHeight(),2);var g=image.createGraphics();frame.printAll(g);g.dispose();javax.imageio.ImageIO.write(image,"png",out.resolve("network-debug-"+i+".png").toFile());}}catch(Exception error){throw new RuntimeException(error);}});
                Files.writeString(out.resolve("network-scene.json"),"{\"players\":2,\"authorityX\":"+one.x()+",\"authorityY\":"+one.y()+",\"footprintRadius\":12,\"sceneryHalfWidth\":22,\"cosmetics\":true,\"serverFailure\":false,\"radminRemoteTested\":false,\"humanApproved\":false}");
                System.out.println("NETWORK_SCENE "+one+" | "+a.diagnostics().summary());
            }finally{SwingUtilities.invokeAndWait(()->{for(var frame:frames)if(frame!=null)frame.dispose();});}
        }
    }
}
