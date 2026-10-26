package com.projetok.studio;

import com.projetok.multiplayer.*;
import com.projetok.prototype.rpg.*;
import javax.swing.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Test-only cross-process harness. Files orchestrate assertions, content uses TCP. */
public final class Task180PackagedProbe {
    interface Check {boolean get()throws Exception;}
    static void waitFor(Check check)throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(45);while(!check.get()&&System.nanoTime()<until)Thread.sleep(25);if(!check.get())throw new AssertionError("Packaged condition timed out");}
    static void capture(JFrame frame,Path path)throws Exception{SwingUtilities.invokeAndWait(()->{try{var image=new java.awt.image.BufferedImage(frame.getWidth(),frame.getHeight(),2);var g=image.createGraphics();frame.printAll(g);g.dispose();javax.imageio.ImageIO.write(image,"png",path.toFile());}catch(Exception e){throw new RuntimeException(e);}});}
    static void marker(Path output,String name,String value)throws Exception{Path temporary=output.resolve(name+".tmp");Files.writeString(temporary,value);Files.move(temporary,output.resolve(name),StandardCopyOption.ATOMIC_MOVE);}
    public static void main(String[] args)throws Exception{
        String mode=args[0];int port=Integer.parseInt(args[1]);Path root=Path.of(args[2]).toAbsolutePath(),output=Path.of(args[3]).toAbsolutePath();Files.createDirectories(output);
        var assets=AssetCatalog.scan(root,null);var cosmetics=CosmeticCatalog.load(root.resolve("assets/dev/generated/corujao-iv-v1"));
        if(mode.equals("host")){
            try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),port,cosmetics,root)){
                var selected=LauncherSession.managed(server);String baseline=selected.target().activeHash();marker(output,"host-ready",baseline);
                waitFor(()->selected.target().status().clients().size()==2&&selected.target().status().clients().values().stream().allMatch(v->v.equals("V4 ACTIVE")));waitFor(()->Files.exists(output.resolve("baseline-a"))&&Files.exists(output.resolve("baseline-b")));
                var maps=new TreeMap<>(selected.initial().bundle().maps());var doc=maps.get("forest");var metadata=new TreeMap<>(doc.metadata());metadata.putAll(new com.projetok.domain.movement.SessionCollisionProfile("package",1,12,26).metadata());var asset=assets.assets().stream().filter(v->v.path().endsWith("/Main_Character_Idle_Front.png")).findFirst().orElseThrow();var elements=new ArrayList<>(doc.elements());elements.add(new WorldDocument.Element("existing_asset",WorldDocument.Kind.TILE,asset.id(),asset.sha(),new WorldDocument.Crop(0,0,asset.width(),asset.height()),700,450,64,64,"ground",true,false,"","FRONT",Map.of()));maps.put("forest",doc.withElements(elements).withHitboxes(List.of(new WorldDocument.Hitbox("central_gray",540,180,120,120,true,"collision"))).withMetadata(metadata));
                var next=LiveRevision.prepare(1,"forest",maps,assets);try{selected.store().publish(next,"",baseline,selected.target(),LivePublishing.Fault.AFTER_ACTIVATION);throw new AssertionError("Injected activation failure committed");}catch(IllegalStateException expected){if(!selected.target().activeHash().equals(baseline))throw new AssertionError("Failure did not restore baseline");}double failureRecoveryMs=selected.target().metrics().recoveryNanos()/1e6;if(failureRecoveryMs<=0)throw new AssertionError("Recovery not measured");var result=selected.store().publish(next,"",baseline,selected.target(),LivePublishing.Fault.NONE);if(!result.active())throw new AssertionError(result.message());marker(output,"committed",next.hash());waitFor(()->Files.exists(output.resolve("published-a"))&&Files.exists(output.resolve("published-b")));
                var original=LiveRevision.prepare(2,"forest",selected.initial().bundle().maps(),assets);var back=selected.store().publish(original,next.hash(),next.hash(),selected.target(),LivePublishing.Fault.NONE);if(!back.active())throw new AssertionError(back.message());marker(output,"rollback",baseline);waitFor(()->Files.exists(output.resolve("rollback-a"))&&Files.exists(output.resolve("rollback-b")));
                Files.writeString(output.resolve("host-result.json"),"{\"realServer\":true,\"application\":4,\"publication\":true,\"rollback\":true,\"failureRecoveryMs\":"+failureRecoveryMs+",\"historyEntries\":"+selected.store().history().size()+"}");
            }
            return;
        }
        String label=mode;boolean guest=Boolean.getBoolean("spiritgate.guest");var sprites=RpgPlayerSprites.loadOptional(root.resolve("assets/dev/player-base"),250,.5);
        waitFor(()->Files.exists(output.resolve("host-ready")));JFrame[] frame={null};
        try(var client=LocalClient.revision(InetAddress.getLoopbackAddress(),port,"Pacote "+label,false,false,null,assets)){
            SwingUtilities.invokeAndWait(()->frame[0]=RpgNetworkClientLauncher.open(client,sprites,"127.0.0.1:"+port,cosmetics));
            waitFor(()->!client.revisions().blocked()&&client.snapshot()!=null);if(guest){try{LauncherSession.start(InetAddress.getLoopbackAddress(),0,cosmetics,root);throw new AssertionError("Guest hosted");}catch(IllegalStateException expected){}}
            if(!Files.readString(output.resolve("host-ready")).equals(client.revisions().hash()))throw new AssertionError("Baseline mismatch");marker(output,"baseline-"+label,client.revisions().hash());
            waitFor(()->Files.exists(output.resolve("committed")));String hash=Files.readString(output.resolve("committed"));waitFor(()->!client.revisions().blocked()&&hash.equals(client.revisions().hash()));if(client.revisions().active().revision().bundle().assets().assets().size()!=1||client.revisions().active().images().bytes()==0)throw new AssertionError("Preinstalled asset not prepared");if(client.revisions().active().revision().profile().sceneryHalfWidth()!=26)throw new AssertionError("Wrong collider");capture(frame[0],output.resolve("published-"+label+".png"));marker(output,"published-"+label,hash);
            waitFor(()->Files.exists(output.resolve("rollback")));String old=Files.readString(output.resolve("rollback"));waitFor(()->!client.revisions().blocked()&&old.equals(client.revisions().hash()));capture(frame[0],output.resolve("rollback-"+label+".png"));marker(output,"rollback-"+label,old);
            Files.writeString(output.resolve("client-"+label+"-result.json"),"{\"application\":4,\"productionRenderer\":true,\"publication\":true,\"rollback\":true,\"guestAdministrativeDenied\":"+guest+"}");
        }finally{if(frame[0]!=null)SwingUtilities.invokeAndWait(()->frame[0].dispose());}
    }
}
