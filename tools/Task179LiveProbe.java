package com.projetok.studio;

import com.projetok.prototype.DebugPanel;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;

/** Real editor + two separately owned game servers. Only synthetic project paths. */
public class Task179LiveProbe {
    static WorldStudio ui;
    interface Checked{boolean get()throws Exception;}
    static void await(Checked condition)throws Exception{long end=System.nanoTime()+30_000_000_000L;while(System.nanoTime()<end){boolean[] ok={false};SwingUtilities.invokeAndWait(()->{try{ok[0]=condition.get();}catch(Exception error){throw new RuntimeException(error);}});if(ok[0])return;Thread.sleep(20);}throw new AssertionError("Probe timed out: "+ui.liveTools.state.getText());}
    static void capture(Path output,String name)throws Exception{Thread.sleep(200);SwingUtilities.invokeAndWait(()->{try{var image=new java.awt.image.BufferedImage(2800,950,2);var g=image.createGraphics();ui.printAll(g);g.translate(1400,0);ui.liveTools.game.printAll(g);g.dispose();javax.imageio.ImageIO.write(image,"png",output.resolve(name).toFile());}catch(Exception error){throw new RuntimeException(error);}});}
    static void key(String action)throws Exception{SwingUtilities.invokeAndWait(()->{var p=(DebugPanel)ui.liveTools.game.getRootPane().getClientProperty("livePanel");p.getActionMap().get(action).actionPerformed(new ActionEvent(p,0,action));});}
    static double percentile(java.util.List<Double> values,double q){var sorted=new ArrayList<>(values);sorted.sort(Double::compare);return sorted.get(Math.min(sorted.size()-1,(int)Math.ceil(q*sorted.size())-1));}
    public static void main(String[] args)throws Exception{
        Path project=Path.of(args[0]).toAbsolutePath(),output=Path.of(args[1]).toAbsolutePath();Files.createDirectories(output);
        Path tile=project.resolve("assets/dev/live-fixture.png");Files.createDirectories(tile.getParent());var image=new java.awt.image.BufferedImage(32,32,2);var graphics=image.createGraphics();graphics.setColor(new Color(50,160,90));graphics.fillRect(0,0,32,32);graphics.setColor(Color.WHITE);graphics.drawRect(1,1,29,29);graphics.dispose();javax.imageio.ImageIO.write(image,"png",tile.toFile());
        var asset=AssetCatalog.verified(tile,"a".repeat(64),AssetCatalog.hash(tile));var catalog=new AssetCatalog.Result(java.util.List.of(asset),java.util.List.of(),java.util.List.of());
        var doc=LiveScenarios.document(0);var tileElement=new WorldDocument.Element("tile",WorldDocument.Kind.TILE,asset.id(),asset.sha(),new WorldDocument.Crop(0,0,32,32),64,64,32,32,"ground",true,false,"","FRONT",Map.of());var elements=new ArrayList<>(doc.elements());elements.add(tileElement);var initial=doc.withElements(elements);
        long heapBefore=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();long starting=System.nanoTime();
        try{
            SwingUtilities.invokeAndWait(()->{ui=new WorldStudio(project);ui.document=ui.history.reset(initial);ui.liveTools.publishedDocument=initial;ui.catalog=catalog;ui.filter();ui.warmImages();ui.setVisible(true);ui.liveTools.startPreview();});
            await(()->ui.liveTools.preview!=null&&!ui.liveTools.busy);long startup=System.nanoTime()-starting;
            SwingUtilities.invokeAndWait(()->ui.liveTools.startTarget());await(()->ui.liveTools.target!=null);String targetBaseline=ui.liveTools.target.state().revision().hash();
            key("Dtrue");key("RIGHTtrue");Thread.sleep(200);key("Dfalse");key("RIGHTfalse");
            capture(output,"before-edit.png");var latencies=new ArrayList<Double>();var validations=new ArrayList<Double>();var ticks=new ArrayList<Double>();WorldDocument current=initial;
            for(int i=1;i<=20;i++){
                var updatedElements=new ArrayList<>(initial.elements());updatedElements.set(1,tileElement.at("tile",64+(i%6)*32,64));var next=initial.withElements(updatedElements).withHitboxes(java.util.List.of(new WorldDocument.Hitbox("live_wall",450+(i%5)*20,180,20,240,true,"collision")));
                if(i%5==0){var metadata=new TreeMap<>(next.metadata());metadata.putAll(new com.projetok.domain.movement.SessionCollisionProfile("bench",1,10,24).metadata());next=next.withMetadata(metadata);}
                var frozen=next;long start=System.nanoTime();SwingUtilities.invokeAndWait(()->ui.change(frozen));await(()->!ui.liveTools.busy&&ui.liveTools.preview.state().revision().bundle().maps().get(frozen.id()).equals(frozen));
                latencies.add((System.nanoTime()-start)/1e6);validations.add(ui.liveTools.lastValidationNanos/1e6);ticks.add(ui.liveTools.preview.state().tickNanos()/1e6);current=next;
                if(!targetBaseline.equals(ui.liveTools.target.state().revision().hash()))throw new AssertionError("Editing mutated target before Save");
            }
            if(Files.exists(ui.liveTools.store.root()))throw new AssertionError("Editing created a published namespace");
            DocumentStore.save(project.resolve("studio-projects/draft-only.sgmap"),current);if(!targetBaseline.equals(ui.liveTools.target.state().revision().hash()))throw new AssertionError("Draft save mutated game");
            SwingUtilities.invokeAndWait(()->ui.inspectorTabs.setSelectedIndex(ui.inspectorTabs.indexOfTab("Live / Colisoes")));capture(output,"live-edit-unpublished.png");SwingUtilities.invokeAndWait(()->ui.liveTools.publish());await(()->!ui.liveTools.publishing&&!ui.liveTools.diskHash.isEmpty());String firstHash=ui.liveTools.diskHash;Path firstVersion=ui.liveTools.store.history().get(0);
            if(!firstHash.equals(ui.liveTools.target.state().revision().hash()))throw new AssertionError("No activation confirmation");
            var second=current.withHitboxes(java.util.List.of(new WorldDocument.Hitbox("live_wall",650,180,20,240,true,"collision")));SwingUtilities.invokeAndWait(()->ui.change(second));await(()->!ui.liveTools.busy&&ui.liveTools.preview.state().revision().bundle().maps().get(second.id()).equals(second));SwingUtilities.invokeAndWait(()->ui.liveTools.publish());await(()->!ui.liveTools.publishing&&!ui.liveTools.diskHash.equals(firstHash));String secondHash=ui.liveTools.diskHash;
            var rollback=ui.liveTools.store.restore(firstVersion,999,catalog);var original=rollback.bundle().maps().get(rollback.bundle().start());SwingUtilities.invokeAndWait(()->ui.change(original));await(()->!ui.liveTools.busy&&ui.liveTools.preview.state().revision().bundle().maps().get(original.id()).equals(original));SwingUtilities.invokeAndWait(()->ui.liveTools.publish());await(()->!ui.liveTools.publishing&&ui.liveTools.diskHash.equals(firstHash));
            if(!firstHash.equals(ui.liveTools.store.current())||!firstHash.equals(ui.liveTools.target.state().revision().hash()))throw new AssertionError("Rollback mismatch");
            Thread.sleep(300);capture(output,"saved-and-rollback.png");
            long heapAfter=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
            String result=String.format(Locale.ROOT,"{\"updates\":20,\"editMedianMs\":%.3f,\"editP95Ms\":%.3f,\"validationMedianMs\":%.3f,\"validationP95Ms\":%.3f,\"tickMedianMs\":%.3f,\"tickP95Ms\":%.3f,\"paintMedianMs\":%.3f,\"paintP95Ms\":%.3f,\"startupMs\":%.3f,\"heapBeforeBytes\":%d,\"heapAfterBytes\":%d,\"memoryIsGcUncontrolled\":true,\"separatePorts\":true,\"beforeSaveTargetUnchanged\":true,\"draftSaveNotPublished\":true,\"syntheticActivationConfirmed\":true,\"rollbackConfirmed\":true,\"historyEntries\":%d,\"productionActivated\":false,\"radminTested\":false,\"humanApproved\":false}",percentile(latencies,.5),percentile(latencies,.95),percentile(validations,.5),percentile(validations,.95),percentile(ticks,.5),percentile(ticks,.95),ui.liveTools.game.diagnostics.paints.percentileMillis(.5),ui.liveTools.game.diagnostics.paints.percentileMillis(.95),startup/1e6,heapBefore,heapAfter,ui.liveTools.store.history().size());
            Files.writeString(output.resolve("benchmark.json"),result);System.out.println(result);
        }finally{if(ui!=null)SwingUtilities.invokeAndWait(()->{ui.liveTools.close();ui.dispose();});}
    }
}
