package com.projetok.prototype.rpg;
import com.projetok.multiplayer.*;
import javax.swing.*;
import java.net.*;
import java.nio.file.*;
import java.time.Duration;
/** Package-only two-process capture barrier; never included in distribution. */
public final class Task176PackageClientProbe {
    static void barrier(Path output,String suffix)throws Exception {
        long deadline=System.nanoTime()+20_000_000_000L;
        while(!Files.exists(output.resolve("client-a"+suffix))||!Files.exists(output.resolve("client-b"+suffix))){if(System.nanoTime()>deadline)throw new IllegalStateException("Capture barrier timed out");Thread.sleep(50);}
    }
    public static void main(String[] args)throws Exception {
        Path output=Path.of(args[2]);String label=args[1];JFrame[] frame={null};
            var catalog=CosmeticCatalog.load(Path.of("assets/dev/generated/corujao-iv-v1"));
            var sprites=RpgPlayerSprites.loadOptional(Path.of("assets/dev/player-base"),250,.5);
        try(var client=LocalClient.combat(InetAddress.getLoopbackAddress(),Integer.parseInt(args[0]),"Viajante "+label,false)) {
            client.await(s->s.actors().size()==3,Duration.ofSeconds(10));
            SwingUtilities.invokeAndWait(()->frame[0]=RpgNetworkClientLauncher.open(client,sprites,"localhost",catalog));
            Files.writeString(output.resolve("client-"+label+".ready"),"ready");barrier(output,".ready");
            Thread.sleep(400);
            client.await(s->s.actors().size()==3,Duration.ofSeconds(3));
            if(client.appearances().size()!=2)throw new IllegalStateException("Missing appearances");
            SwingUtilities.invokeAndWait(()->{try{var image=new java.awt.image.BufferedImage(frame[0].getWidth(),frame[0].getHeight(),2);var g=image.createGraphics();frame[0].printAll(g);g.dispose();javax.imageio.ImageIO.write(image,"png",output.resolve("client-"+label+".png").toFile());}catch(Exception e){throw new RuntimeException(e);}});
            Files.writeString(output.resolve("client-"+label+".png.json"),"{\"session\":"+client.id()+",\"players\":"+client.snapshot().actors().size()+",\"appearances\":"+client.appearances().size()+",\"physicalUiApproved\":false}");
            Files.writeString(output.resolve("client-"+label+".captured"),"captured");barrier(output,".captured");
        }finally{if(frame[0]!=null)SwingUtilities.invokeAndWait(frame[0]::dispose);}
    }
}
