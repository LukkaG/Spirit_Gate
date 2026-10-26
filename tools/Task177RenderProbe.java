package com.projetok.multiplayer;
import com.projetok.prototype.rpg.*;
import com.projetok.prototype.DebugPanel;
import java.net.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import javax.swing.*;
/** Two real Swing windows; component captures and measured paint cost, no human approval claim. */
public class Task177RenderProbe {
    public static void main(String[] args)throws Exception{
        Path out=Path.of(args[0]);String label=args[1];Files.createDirectories(out);JFrame[] windows=new JFrame[2];
        var samples=new ArrayList<Double>();
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,new RpgSessionWorld(List.of("reaper","human"),null,true,20))){
            server.start();try(var a=LocalClient.combat(InetAddress.getLoopbackAddress(),server.port(),"Visual A",false);var b=LocalClient.combat(InetAddress.getLoopbackAddress(),server.port(),"Visual B",false)){
                a.await(s->s.actors().size()==3,Duration.ofSeconds(3));var sprites=RpgPlayerSprites.load(Path.of("assets/dev/player-base"),250,.5);
                SwingUtilities.invokeAndWait(()->{windows[0]=RpgNetworkClientLauncher.open(a,sprites,"localhost");windows[1]=RpgNetworkClientLauncher.open(b,sprites,"localhost");});
                var image=new java.awt.image.BufferedImage(1200,800,2);
                for(int i=0;i<300;i++){
                    int index=i;SwingUtilities.invokeAndWait(()->{var g=image.createGraphics();long start=System.nanoTime();windows[0].printAll(g);long elapsed=System.nanoTime()-start;g.dispose();if(index>=60)samples.add(elapsed/1e6);});Thread.sleep(16);
                    if(i==100||i==200)a.measureLatency();
                }
                SwingUtilities.invokeAndWait(()->{try{javax.imageio.ImageIO.write(image,"png",out.resolve(label+".png").toFile());}catch(Exception e){throw new RuntimeException(e);}});
                samples.sort(Double::compare);
                System.out.printf(Locale.ROOT,"%s printAllMedianMs=%.3f printAllP95Ms=%.3f samples=%d serverTick=%d connected=%s/%s%n",label,samples.get(samples.size()/2),samples.get((int)(samples.size()*.95)),samples.size(),server.snapshot().tick(),a.connected(),b.connected());
                try{var d=a.getClass().getMethod("diagnostics").invoke(a);System.out.println(d.getClass().getMethod("summary").invoke(d));}catch(NoSuchMethodException ignored){}
                if(server.failure()!=null)throw new AssertionError(server.failure());
                if(!a.connected()||!b.connected())throw new AssertionError("Disconnected");
            }finally{SwingUtilities.invokeAndWait(()->{for(var frame:windows)if(frame!=null)frame.dispose();});}
        }
    }
}
