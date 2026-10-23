package com.projetok.multiplayer;

import com.projetok.prototype.DebugPanel;
import com.projetok.prototype.rpg.RpgPlayerSprites;
import com.projetok.prototype.rpg.CosmeticCatalog;
import java.awt.image.BufferedImage;
import java.net.*;
import java.nio.file.*;
import java.time.Duration;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Dois clientes reais com janelas, movimento independente, saída e reconexão sintética. */
public final class RpgNetworkProbe {
    static void capture(JFrame frame,Path path)throws Exception {SwingUtilities.invokeAndWait(()->{var image=new BufferedImage(frame.getWidth(),frame.getHeight(),2);var g=image.createGraphics();frame.printAll(g);g.dispose();try{ImageIO.write(image,"png",path.toFile());}catch(Exception e){throw new IllegalStateException(e);}});}
    public static void main(String[] args)throws Exception {
        Path output=Path.of(args[0]);Files.createDirectory(output);var sprites=RpgPlayerSprites.load(Path.of("assets/dev/player-base"),250,.5);boolean cosmetics=args.length>1&&args[1].equals("--cosmetics");var catalog=cosmetics?CosmeticCatalog.load(Path.of("assets/dev/generated/corujao-iv-v1")):null;
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,new RpgSessionWorld(java.util.List.of("reaper","human"),catalog))){server.start();try(var a=cosmetics?LocalClient.cosmetics(InetAddress.getLoopbackAddress(),server.port(),"Jogador A",false):new LocalClient(InetAddress.getLoopbackAddress(),server.port(),"Jogador A");var b=cosmetics?LocalClient.cosmetics(InetAddress.getLoopbackAddress(),server.port(),"Jogador B",false):new LocalClient(InetAddress.getLoopbackAddress(),server.port(),"Jogador B")){
            a.await(s->s.actors().size()==2,Duration.ofSeconds(3));b.await(s->s.actors().size()==2,Duration.ofSeconds(3));JFrame[] frames=new JFrame[2];
            SwingUtilities.invokeAndWait(()->{frames[0]=RpgNetworkClientLauncher.open(a,sprites,"localhost:"+server.port(),catalog);frames[1]=RpgNetworkClientLauncher.open(b,sprites,"localhost:"+server.port(),catalog);});
            if(cosmetics){SwingUtilities.invokeAndWait(()->{for(int i=0;i<frames.length;i++){int comboIndex=0;for(java.awt.Component component:frames[i].getContentPane().getComponents())if(component instanceof JPanel controls)for(java.awt.Component c:controls.getComponents())if(c instanceof JComboBox<?> combo){combo.setSelectedIndex(i==0?(++comboIndex==1?1:2):(++comboIndex==1?3:1));}}});
                a.await(state->a.appearances().containsKey(b.id())&&a.appearances().get(b.id()).hair().equals("hair_spiky"),Duration.ofSeconds(3));b.await(state->b.appearances().containsKey(a.id())&&b.appearances().get(a.id()).clothing().equals("light_armor"),Duration.ofSeconds(3));if(a.appearances().get(a.id()).race().equals(a.appearances().get(b.id()).race()))throw new IllegalStateException("Racas nao distintas");}
            try{SwingUtilities.invokeAndWait(()->{((DebugPanel)frames[0].getRootPane().getClientProperty("networkPanel")).getActionMap().get("Dtrue").actionPerformed(null);((DebugPanel)frames[1].getRootPane().getClientProperty("networkPanel")).getActionMap().get("Wtrue").actionPerformed(null);});
                b.await(s->s.actors().stream().anyMatch(v->v.id()==a.id()&&v.x()>150),Duration.ofSeconds(3));a.await(s->s.actors().stream().anyMatch(v->v.id()==b.id()&&v.y()<400),Duration.ofSeconds(3));capture(frames[0],output.resolve("client-a.png"));capture(frames[1],output.resolve("client-b.png"));
                SwingUtilities.invokeAndWait(()->frames[0].dispose());b.await(s->s.actors().size()==1,Duration.ofSeconds(3));try(var reconnect=cosmetics?LocalClient.cosmetics(InetAddress.getLoopbackAddress(),server.port(),"Retorno",false):new LocalClient(InetAddress.getLoopbackAddress(),server.port(),"Retorno")){if(reconnect.id()==a.id())throw new IllegalStateException("ID reutilizado");b.await(s->s.actors().size()==2,Duration.ofSeconds(3));if(cosmetics)b.await(s->b.appearances().containsKey(reconnect.id())&&b.appearances().get(reconnect.id()).hair().equals("none"),Duration.ofSeconds(3));}
                if(cosmetics)Files.writeString(output.resolve("cosmetics.json"),"{\"synchronized\":true,\"racesDifferent\":true,\"reconnectAppearanceCleared\":true,\"imagesSentInSnapshots\":false}");
                Files.writeString(output.resolve("result.json"),"{\"clients\":2,\"mutualMovementObserved\":true,\"disconnectRemovedActor\":true,\"reconnectNewSession\":true,\"scale\":0.5,\"walkFrameMillis\":250,\"physicalUiApproved\":false,\"personalSavesUsed\":false}");
            }finally{SwingUtilities.invokeAndWait(()->{for(var frame:frames)frame.dispose();});}
        }if(server.failure()!=null)throw new IllegalStateException(server.failure());}
        System.exit(0);
    }
}
