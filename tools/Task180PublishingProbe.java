package com.projetok.studio;

import com.projetok.multiplayer.*;
import com.projetok.prototype.rpg.*;
import com.projetok.domain.movement.SessionCollisionProfile;
import javax.swing.*;
import java.awt.*;
import java.nio.file.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/** Production launcher factory + production TCP client renderer, no mock windows. */
public final class Task180PublishingProbe {
    static WorldStudio studio;static JFrame first,second;
    interface Check {boolean get()throws Exception;}
    static void waitFor(Check check)throws Exception{long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(40);while(System.nanoTime()<until){boolean[] ok={false};SwingUtilities.invokeAndWait(()->{try{ok[0]=check.get();}catch(Exception e){throw new RuntimeException(e);}});if(ok[0])return;Thread.sleep(20);}throw new AssertionError("Timeout: "+(studio==null?"initial client":studio.liveTools.state.getText()+" | preview="+(studio.liveTools.preview==null?"none":studio.liveTools.preview.state().revision().hash())));}
    static void key(JFrame frame,String action)throws Exception{SwingUtilities.invokeAndWait(()->{var panel=(JComponent)frame.getRootPane().getClientProperty("networkPanel");panel.getActionMap().get(action).actionPerformed(new java.awt.event.ActionEvent(panel,0,action));});}
    static void capture(Path output,String name)throws Exception{Thread.sleep(250);SwingUtilities.invokeAndWait(()->{try{var image=new java.awt.image.BufferedImage(2800,1660,2);var g=image.createGraphics();studio.printAll(g);g.translate(1400,0);first.printAll(g);g.translate(0,830);second.printAll(g);g.translate(-1400,0);studio.liveTools.game.printAll(g);g.dispose();javax.imageio.ImageIO.write(image,"png",output.resolve(name).toFile());}catch(Exception e){throw new RuntimeException(e);}});}
    public static void main(String[] args)throws Exception{
        Path root=Path.of(args[0]).toAbsolutePath(),output=Path.of(args[1]).toAbsolutePath();Files.createDirectories(output);
        var assets=AssetCatalog.scan(root,null);var spritesA=RpgPlayerSprites.loadOptional(root.resolve("assets/dev/player-base"),250,.5);var spritesB=RpgPlayerSprites.loadOptional(root.resolve("assets/dev/player-base"),250,.5);var cosmetics=CosmeticCatalog.load(root.resolve("assets/dev/generated/corujao-iv-v1"));
        long heapBefore=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
        try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,cosmetics,root);var a=LocalClient.revision(InetAddress.getLoopbackAddress(),server.port(),"Anfitriao",false,false,null,assets);var b=LocalClient.revision(InetAddress.getLoopbackAddress(),server.port(),"Convidado",false,false,null,assets)){
            var managed=LauncherSession.managed(server);String baseline=managed.target().activeHash();
            SwingUtilities.invokeAndWait(()->{first=RpgNetworkClientLauncher.open(a,spritesA,"127.0.0.1:"+server.port(),cosmetics);second=RpgNetworkClientLauncher.open(b,spritesB,"127.0.0.1:"+server.port(),cosmetics);first.setSize(1400,830);second.setSize(1400,830);studio=LauncherSession.openStudio(server,root);studio.setSize(1400,830);});
            waitFor(()->!a.revisions().blocked()&&!b.revisions().blocked()&&studio.document.region().equals("forest"));
            var original=studio.document;SwingUtilities.invokeAndWait(()->{studio.catalog=assets;studio.liveTools.startPreview();});waitFor(()->studio.liveTools.preview!=null&&!studio.liveTools.busy);
            if(studio.liveTools.preview.port()==server.port())throw new AssertionError("Shared preview server");
            studio.liveTools.preview.pause(true).get(3,TimeUnit.SECONDS);capture(output,"before-save.png");
            var metadata=new TreeMap<>(original.metadata());metadata.putAll(new SessionCollisionProfile("live-published",1,12,26).metadata());var asset=assets.assets().stream().filter(v->v.path().endsWith("/Main_Character_Idle_Front.png")).findFirst().orElseThrow();var elements=new ArrayList<>(original.elements());elements.add(new WorldDocument.Element("existing_asset",WorldDocument.Kind.TILE,asset.id(),asset.sha(),new WorldDocument.Crop(0,0,asset.width(),asset.height()),700,450,64,64,"ground",true,false,"","FRONT",Map.of()));var edited=original.withElements(elements).withHitboxes(java.util.List.of(new WorldDocument.Hitbox("central_gray",540,180,120,120,true,"collision"))).withMetadata(metadata);
            long validationStart=System.nanoTime();var maps=new TreeMap<>(WorldContent.baseline().bundle().maps());maps.put(edited.id(),edited);var prepared=LiveRevision.prepare(1,edited.id(),maps,assets);byte[] descriptor=WorldContent.encode(prepared);long validationNanos=System.nanoTime()-validationStart;
            SwingUtilities.invokeAndWait(()->{studio.inspectorTabs.setSelectedIndex(studio.inspectorTabs.indexOfTab("Live / Colisoes"));studio.change(edited);});waitFor(()->!studio.liveTools.busy&&studio.liveTools.preview.state().revision().hash().equals(prepared.hash()));
            if(!baseline.equals(managed.target().activeHash())||!baseline.equals(a.revisions().hash())||!baseline.equals(b.revisions().hash()))throw new AssertionError("Edit changed real game");
            DocumentStore.save(root.resolve("studio-projects/draft-only.sgmap"),edited);if(!baseline.equals(managed.target().activeHash()))throw new AssertionError("Draft save activated");capture(output,"preview-only.png");
            long started=System.nanoTime();SwingUtilities.invokeAndWait(()->studio.liveTools.publish());waitFor(()->!studio.liveTools.publishing&&prepared.hash().equals(studio.liveTools.diskHash));waitFor(()->!a.revisions().blocked()&&!b.revisions().blocked());long publicationNanos=System.nanoTime()-started;
            if(!prepared.hash().equals(managed.target().activeHash())||!prepared.hash().equals(a.revisions().hash())||!prepared.hash().equals(b.revisions().hash()))throw new AssertionError("Hash mismatch after activation");
            if(a.revisions().active().revision().profile().sceneryHalfWidth()!=26||b.revisions().active().revision().geometry().get("forest").obstacles().stream().noneMatch(o->o.x()==540))throw new AssertionError("Production scene not installed");
            if(a.revisions().active().images().peek(asset.id())==null||b.revisions().active().images().peek(asset.id())==null)throw new AssertionError("Preinstalled image not prepared");var barriers=managed.target().metrics();capture(output,"published-two-clients.png");
            // Real inputs, PvE authority and a real region transition after publication.
            key(second,"Wtrue");Thread.sleep(300);key(second,"Wfalse");key(first,"Dtrue");Thread.sleep(250);key(first,"Dfalse");key(first,"SPACE");waitFor(()->a.combatEvents().stream().anyMatch(e->e.player()==a.id()&&e.kind().equals("HIT")));
            waitFor(()->b.snapshot()!=null&&b.snapshot().actors().stream().anyMatch(p->p.id()==b.id()&&p.y()<365));key(second,"P");waitFor(()->b.region().equals("village"));
            var publishedProfile=managed.target().revision().profile();long rollbackStart=System.nanoTime();SwingUtilities.invokeAndWait(()->studio.change(original));waitFor(()->!studio.liveTools.busy&&studio.liveTools.preview.state().revision().hash().equals(baseline));SwingUtilities.invokeAndWait(()->studio.liveTools.publish());waitFor(()->!studio.liveTools.publishing&&studio.liveTools.diskHash.equals(baseline));waitFor(()->!a.revisions().blocked()&&!b.revisions().blocked());long rollbackNanos=System.nanoTime()-rollbackStart;
            if(!baseline.equals(a.revisions().hash())||!baseline.equals(b.revisions().hash())||!baseline.equals(managed.store().current()))throw new AssertionError("Rollback mismatch");capture(output,"rollback-confirmed.png");
            long heapAfter=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
            String json=String.format(Locale.ROOT,"{\"realRpgServer\":true,\"productionRenderer\":true,\"tcpClients\":2,\"previewIsolated\":true,\"beforeSaveUnchanged\":true,\"draftSaveNotPublished\":true,\"activationConfirmed\":true,\"rollbackConfirmed\":true,\"portalAfterPublication\":true,\"pveHitAfterPublication\":true,\"manifestBytes\":%d,\"imageTransferBytes\":0,\"preinstalledAssetPreparedBothClients\":true,\"validationMs\":%.3f,\"serverPrepareMs\":%.3f,\"clientPrepareMs\":[%.3f,%.3f],\"publicationMs\":%.3f,\"rollbackMs\":%.3f,\"clientPaintP95Ms\":[%.3f,%.3f],\"tickP95Ms\":%.3f,\"heapBeforeBytes\":%d,\"heapAfterBytes\":%d,\"memoryGcUncontrolled\":true,\"publishedHalfWidth\":%.1f,\"historyEntries\":%d,\"radminPhysicalTest\":false,\"humanApproved\":false}",descriptor.length,validationNanos/1e6,managed.target().status().serverPrepareNanos()/1e6,a.revisions().prepareNanos()/1e6,b.revisions().prepareNanos()/1e6,publicationNanos/1e6,rollbackNanos/1e6,a.diagnostics().paints.percentileMillis(.95),b.diagnostics().paints.percentileMillis(.95),server.diagnostics().ticks.percentileMillis(.95),heapBefore,heapAfter,publishedProfile.sceneryHalfWidth(),managed.store().history().size());json=json.substring(0,json.length()-1)+String.format(Locale.ROOT,",\"descriptorPayloadToClientsBytes\":%d,\"contentQueuedWireBytes\":%d,\"waitingClientsMs\":%.3f,\"activationBarrierMs\":%.3f,\"transitionMs\":%.3f,\"recoveryMs\":%.3f}",barriers.descriptorPayloadBytes(),barriers.queuedWireBytes(),barriers.waitingClientsNanos()/1e6,barriers.activationNanos()/1e6,barriers.transitionNanos()/1e6,barriers.recoveryNanos()/1e6);Files.writeString(output.resolve("metrics.json"),json);System.out.println(json);
        }finally{SwingUtilities.invokeAndWait(()->{if(studio!=null){studio.liveTools.close();studio.dispose();}if(first!=null)first.dispose();if(second!=null)second.dispose();});}
    }
}
